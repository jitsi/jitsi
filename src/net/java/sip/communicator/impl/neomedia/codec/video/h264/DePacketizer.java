/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Implements <tt>Codec</tt> to represent a depacketizer of H.264 RTP packets
 * into NAL units.
 *
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class DePacketizer
    extends AbstractCodecExt
{

    /**
     * The <tt>Logger</tt> used by the <tt>DePacketizer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DePacketizer.class);

    /**
     * The bytes to prefix any NAL unit to be output by this
     * <tt>DePacketizer</tt> and given to a H.264 decoder. Includes
     * start_code_prefix_one_3bytes. According to "B.1 Byte stream NAL unit
     * syntax and semantics" of "ITU-T Rec. H.264 Advanced video coding for
     * generic audiovisual services", zero_byte "shall be present" when "the
     * nal_unit_type within the nal_unit() is equal to 7 (sequence parameter
     * set) or 8 (picture parameter set)" or "the byte stream NAL unit syntax
     * structure contains the first NAL unit of an access unit in decoding
     * order".
     */
    private static final byte[] NAL_PREFIX = { 0, 0, 1 };

    /**
     * The indicator which determines whether incomplete NAL units are output
     * from the H.264 <tt>DePacketizer</tt> to the decoder. It is advisable to
     * output incomplete NAL units because the FFmpeg H.264 decoder is able to
     * decode them. If <tt>false</tt>, incomplete NAL units will be discarded
     * and, consequently, the video quality will be worse (e.g. if the last RTP
     * packet of a fragmented NAL unit carrying a keyframe does not arrive from
     * the network, the whole keyframe will be discarded and thus all NAL units
     * upto the next keyframe will be useless).
     */
    private static final boolean OUTPUT_INCOMPLETE_NAL_UNITS = true;

    /**
     * Interval between a PLI request and its reemission (in milliseconds).
     */
    private static final long PLI_INTERVAL = 200;

    /**
     * Interval between a PLI and last keyframe received (in milliseconds).
     */
    private static final long PLI_KEYFRAME_INTERVAL = 4000;

    /**
     * The indicator which determines whether this <tt>DePacketizer</tt> has
     * successfully processed an RTP packet with payload representing a
     * "Fragmentation Unit (FU)" with its Start bit set and has not encountered
     * one with its End bit set.
     */
    private boolean fuaStartedAndNotEnded = false;

    /**
     * Keeps track of last (input) sequence number in order to avoid
     * inconsistent data.
     */
    private long lastSequenceNumber = -1;

    /**
     * The timestamp of the last received RTP packet.
     */
    private long lastTimeStamp = -1;

    /**
     * The size of the padding at the end of the output data of this
     * <tt>DePacketizer</tt> expected by the H.264 decoder.
     */
    private final int outputPaddingSize
        = FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;

    /**
     * RTCP output stream to send RTCP feedback message
     * back to sender in case of packet loss for example.
     */
    private RTPConnectorOutputStream rtcpOutputStream = null;

    /**
     * Local SSRC.
     */
    private long localSSRC = -1;

    /**
     * Remote SSRC.
     */
    private long remoteSSRC = -1;

    /**
     * The indicator which determines whether RTCP PLI is to be used when this
     * <tt>DePacketizer</tt> detects that video data has been lost.
     */
    private boolean usePLI = false;

    /**
     * Last timestamp of keyframe request.
     *
     * This is used to retransmit keyframe request in case previous get lost
     * and codec doesn't receive keyframe.
     */
    private long lastPLIRequestTime = -1;

    /**
     * Last timestamp of received keyframe.
     */
    private long lastReceivedKeyframeTime = -1;

    /**
     * If <tt>Codec</tt> has detected frames lost.
     */
    private boolean missFrame = false;

    /**
     * State of PLISendThread.
     */
    private boolean isPLIThreadRunning = false;

    /**
     * Thread that will monitor time between PLI request, keyframes received
     * and will send or not a PLI.
     */
    private PLISendThread pliSendThread = new PLISendThread();

    /**
     * Initializes a new <tt>DePacketizer</tt> instance which is to depacketize
     * H.264 RTP packets into NAL units.
     */
    public DePacketizer()
    {
        super(
            "H264 DePacketizer",
            VideoFormat.class,
            new VideoFormat[] { new VideoFormat(Constants.H264) });

        inputFormats
            = new VideoFormat[] { new VideoFormat(Constants.H264_RTP) };
    }

    /**
     * Extracts a fragment of a NAL unit from a specific FU-A RTP packet
     * payload.
     *
     * @param in the payload of the RTP packet from which a FU-A fragment of a
     * NAL unit is to be extracted
     * @param inOffset the offset in <tt>in</tt> at which the payload begins
     * @param inLength the length of the payload in <tt>in</tt> beginning at
     * <tt>inOffset</tt>
     * @param outBuffer the <tt>Buffer</tt> which is to receive the extracted
     * FU-A fragment of a NAL unit
     * @return the flags such as <tt>BUFFER_PROCESSED_OK</tt> and
     * <tt>OUTPUT_BUFFER_NOT_FILLED</tt> to be returned by
     * {@link #process(Buffer, Buffer)}
     */
    private int dePacketizeFUA(
            byte[] in, int inOffset, int inLength,
            Buffer outBuffer)
    {
        byte fu_indicator = in[inOffset];

        inOffset++;
        inLength--;

        byte fu_header = in[inOffset];

        inOffset++;
        inLength--;

        boolean start_bit = (fu_header & 0x80) != 0;
        boolean end_bit = (fu_header & 0x40) != 0;
        /* key frame (IDR) type = 5 */
        boolean keyframe = ((fu_header & 0x1F) == 5);
        int outOffset = outBuffer.getOffset();
        int newOutLength = inLength;
        int octet;

        /* waiting for a keyframe ? */
        if(keyframe)
        {
            /* keyframe received */
            missFrame = false;
            lastReceivedKeyframeTime = System.currentTimeMillis();
        }

        if (start_bit)
        {
            /*
             * The Start bit and End bit MUST NOT both be set in the same FU
             * header.
             */
            if (end_bit)
            {
                outBuffer.setDiscard(true);
                return BUFFER_PROCESSED_OK;
            }

            fuaStartedAndNotEnded = true;

            newOutLength += NAL_PREFIX.length + 1 /* octet */;
            octet
                = (fu_indicator & 0xE0) /* forbidden_zero_bit & NRI */
                    | (fu_header & 0x1F) /* nal_unit_type */;
        }
        else if (!fuaStartedAndNotEnded)
        {
            outBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }
        else
        {
            int outLength = outBuffer.getLength();

            outOffset += outLength;
            newOutLength += outLength;
            octet = 0; // Ignored later on.
        }

        byte[] out
            = validateByteArraySize(
                outBuffer,
                outBuffer.getOffset() + newOutLength + outputPaddingSize);

        if (start_bit)
        {
            // Copy in the NAL start sequence and the (reconstructed) octet.
            System.arraycopy(NAL_PREFIX, 0, out, outOffset, NAL_PREFIX.length);
            outOffset += NAL_PREFIX.length;

            out[outOffset] = (byte) (octet & 0xFF);
            outOffset++;
        }
        System.arraycopy(in, inOffset, out, outOffset, inLength);
        outOffset += inLength;

        padOutput(out, outOffset);

        outBuffer.setLength(newOutLength);

        if (end_bit)
        {
            fuaStartedAndNotEnded = false;
            return BUFFER_PROCESSED_OK;
        }
        else
            return OUTPUT_BUFFER_NOT_FILLED;
    }

    /**
     * Extract a single (complete) NAL unit from RTP payload.
     *
     * @param nal_unit_type unit type of NAL
     * @param in the payload of the RTP packet
     * @param inOffset the offset in <tt>in</tt> at which the payload begins
     * @param inLength the length of the payload in <tt>in</tt> beginning at
     * <tt>inOffset</tt>
     * @param outBuffer the <tt>Buffer</tt> which is to receive the extracted
     * NAL unit
     * @return the flags such as <tt>BUFFER_PROCESSED_OK</tt> and
     * <tt>OUTPUT_BUFFER_NOT_FILLED</tt> to be returned by
     * {@link #process(Buffer, Buffer)}
     */
    private int dePacketizeSingleNALUnitPacket(
            int nal_unit_type,
            byte[] in, int inOffset, int inLength,
            Buffer outBuffer)
    {
        int outOffset = outBuffer.getOffset();
        int newOutLength = NAL_PREFIX.length + inLength;
        byte[] out
            = validateByteArraySize(
                outBuffer,
                outOffset + newOutLength + outputPaddingSize);

        System.arraycopy(NAL_PREFIX, 0, out, outOffset, NAL_PREFIX.length);
        outOffset += NAL_PREFIX.length;

        System.arraycopy(in, inOffset, out, outOffset, inLength);
        outOffset += inLength;

        padOutput(out, outOffset);

        outBuffer.setLength(newOutLength);

        /* coded slice of an IDR picture (keyframe) */
        if(nal_unit_type == 5)
        {
            /* keyframe received */
            missFrame = false;
            lastReceivedKeyframeTime = System.currentTimeMillis();
        }

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Close the <tt>Codec</tt>.
     */
    protected void doClose()
    {
        /* will end PLISendThread loop */
        isPLIThreadRunning = false;
        pliSendThread = null;
    }

    /**
     * Opens this <tt>Codec</tt> and acquires the resources that it needs to
     * operate. A call to {@link PlugIn#open()} on this instance will result in
     * a call to <tt>doOpen</tt> only if {@link AbstractCodec#opened} is
     * <tt>false</tt>. All required input and/or output formats are assumed to
     * have been set on this <tt>Codec</tt> before <tt>doOpen</tt> is called.
     *
     * @throws ResourceUnavailableException if any of the resources that this
     * <tt>Codec</tt> needs to operate cannot be acquired
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
        fuaStartedAndNotEnded = false;
        lastSequenceNumber = -1;
        lastTimeStamp = -1;
    }
    /**
     * Processes (depacketize) a buffer.
     *
     * @param inBuffer input buffer
     * @param outBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     */
    protected int doProcess(Buffer inBuffer, Buffer outBuffer)
    {
        /*
         * We'll only be depacketizing, we'll not act as an H.264 parser.
         * Consequently, we'll only care about the rules of
         * packetizing/depacketizing. For example, we'll have to make sure that
         * no packets are lost and no other packets are received when
         * depacketizing FU-A Fragmentation Units (FUs).
         */
        long sequenceNumber = inBuffer.getSequenceNumber();
        int ret;

        if ((lastSequenceNumber != -1)
                && ((sequenceNumber - lastSequenceNumber) != 1))
        {
            /*
             * Even if (the new) sequenceNumber is less than lastSequenceNumber,
             * we have to use it because the received sequence numbers may have
             * reached their maximum value and wrapped around starting from
             * their minimum value again.
             */
            if (logger.isTraceEnabled())
                logger.trace(
                        "Dropped RTP packets upto sequenceNumber "
                            + lastSequenceNumber
                            + " and continuing with sequenceNumber "
                            + sequenceNumber);

            /* detection of missed frames */
            if(usePLI)
            {
                missFrame = true;

                /* if the PLI thread is not started, start it! */
                if(!isPLIThreadRunning)
                {
                    isPLIThreadRunning = true;
                    pliSendThread.start();
                }
            }

            ret = reset(outBuffer);

            if ((ret & OUTPUT_BUFFER_NOT_FILLED) == 0)
                return ret;
        }

        long timeStamp = inBuffer.getTimeStamp();

        /*
         * Ignore the RTP time stamp reported by JMF because it is not the
         * actual RTP packet time stamp send by the remote peer but some locally
         * calculated JMF value.
         */
/*
        // If the RTP time stamp changes, we're receiving a new NAL unit.
        if (timeStamp != lastTimeStamp)
        {
            ret = reset(outBuffer);
            if ((ret & OUTPUT_BUFFER_NOT_FILLED) == 0)
                return ret;
        }
*/

        lastSequenceNumber = sequenceNumber;
        lastTimeStamp = timeStamp;

        byte[] in = (byte[]) inBuffer.getData();
        int inOffset = inBuffer.getOffset();
        byte octet = in[inOffset];

        /*
         * NRI equal to the binary value 00 indicates that the content of the
         * NAL unit is not used to reconstruct reference pictures for inter
         * picture prediction. Such NAL units can be discarded without risking
         * the integrity of the reference pictures. However, it is not the place
         * of the DePacketizer to take the decision to discard them but of the
         * H.264 decoder.
         */

        int nal_unit_type = octet & 0x1F;

        // Single NAL Unit Packet
        if ((nal_unit_type >= 1) && (nal_unit_type <= 23))
        {
            fuaStartedAndNotEnded = false;
            ret
                = dePacketizeSingleNALUnitPacket(
                    nal_unit_type,
                    in, inOffset, inBuffer.getLength(),
                    outBuffer);
        }
        else if (nal_unit_type == 28) // FU-A Fragmentation unit (FU)
        {
            ret = dePacketizeFUA(in, inOffset, inBuffer.getLength(), outBuffer);
            if (outBuffer.isDiscard())
                fuaStartedAndNotEnded = false;
        }
        else
        {
            logger.warn(
                    "Dropping NAL unit of unsupported type " + nal_unit_type);
            fuaStartedAndNotEnded = false;
            outBuffer.setDiscard(true);
            ret = BUFFER_PROCESSED_OK;
        }

        outBuffer.setSequenceNumber(sequenceNumber);
        //outBuffer.setTimeStamp(timeStamp);

        /*
         * The RTP marker bit is set for the very last packet of the access unit
         * indicated by the RTP time stamp to allow an efficient playout buffer
         * handling. Consequently, we have to output it as well.
         */
        if ((inBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
            outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_RTP_MARKER);

        return ret;
    }

    /**
     * Appends {@link #outputPaddingSize} number of bytes to <tt>out</tt>
     * beginning at index <tt>outOffset</tt>. The specified <tt>out</tt> is
     * expected to be large enough to accommodate the mentioned number of bytes.
     *
     * @param out the buffer in which <tt>outputPaddingSize</tt> number of bytes
     * are to be written
     * @param outOffset the index in <tt>outOffset</tt> at which the writing of
     * <tt>outputPaddingSize</tt> number of bytes is to begin
     */
    private void padOutput(byte[] out, int outOffset)
    {
        Arrays.fill(out, outOffset, outOffset + outputPaddingSize, (byte) 0);
    }

    /**
     * Resets the states of this <tt>DePacketizer</tt> and a specific output
     * <tt>Buffer</tt> so that they are ready to have this <tt>DePacketizer</tt>
     * process input RTP payloads. If the specified output <tt>Buffer</tt>
     * contains an incomplete NAL unit, its forbidden_zero_bit will be turned on
     * and the NAL unit in question will be output by this
     * <tt>DePacketizer</tt>.
     *
     * @param outBuffer the output <tt>Buffer</tt> to be reset
     * @return the flags such as <tt>BUFFER_PROCESSED_OK</tt> and
     * <tt>OUTPUT_BUFFER_NOT_FILLED</tt> to be returned by
     * {@link #process(Buffer, Buffer)}
     */
    private int reset(Buffer outBuffer)
    {
        /*
         * We need the octet at the very least. Additionally, it does not make
         * sense to output a NAL unit with zero payload because such NAL units
         * are only given meaning for the purposes of the network and not the
         * H.264 decoder.
         */
        if (OUTPUT_INCOMPLETE_NAL_UNITS
                && fuaStartedAndNotEnded
                && (outBuffer.getLength() >= (NAL_PREFIX.length + 1 + 1)))
        {
            Object outData = outBuffer.getData();

            if (outData instanceof byte[])
            {
                byte[] out = (byte[]) outData;
                int octetIndex = outBuffer.getOffset() + NAL_PREFIX.length;

                out[octetIndex] |= 0x80; // Turn on the forbidden_zero_bit.
                fuaStartedAndNotEnded = false;
                return (BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED);
            }
        }

        fuaStartedAndNotEnded = false;
        outBuffer.setLength(0);
        return OUTPUT_BUFFER_NOT_FILLED;
    }

    /**
     * Send RTCP Feedback PLI message.
     */
    private void sendRTCPFeedbackPLI()
    {
        RTCPFeedbackPacket packet = new RTCPFeedbackPacket(1, 206,
                localSSRC, remoteSSRC);
        packet.writeTo(rtcpOutputStream);
    }

    /**
     * Set <tt>OutputDataStream</tt>.
     *
     * @param rtcpOutputStream RTCP <tt>OutputDataStream</tt>
     */
    public void setConnector(RTPConnectorOutputStream rtcpOutputStream)
    {
        this.rtcpOutputStream = rtcpOutputStream;
    }

    /**
     * Set local and remote SSRC. It will be used
     * to send RTCP messages.
     *
     * @param localSSRC local SSRC
     * @param remoteSSRC remote SSRC
     */
    public void setSSRC(long localSSRC, long remoteSSRC)
    {
        this.localSSRC = localSSRC;
        this.remoteSSRC = remoteSSRC;
    }

    /**
     * Use or not RTCP feedback PLI.
     *
     * @param use use or not RTCP PLI message
     */
    public void setRtcpFeedbackPLI(boolean use)
    {
        usePLI = use;
    }

    /**
     * Thread that will handle Picture Loss Indication (PLI) transmission.
     * Note that PLI will be sent with a delay if recently we received a
     * keyframe or sent a PLI.
     *
     * @author Sebastien Vincent
     */
    private class PLISendThread extends Thread
    {
        /**
         * Represents the entry point of <tt>PLISendThread</tt>.
         */
        @Override
        public void run()
        {
            while(isPLIThreadRunning)
            {
                long time = System.currentTimeMillis();

                if(missFrame)
                {
                    /* we keep some delay with the latest keyframe */
                    if(time > (lastReceivedKeyframeTime +
                            PLI_KEYFRAME_INTERVAL))
                    {
                        /* we keep delay between latest PLI to not spam sender
                         * with a lot of PLI request
                         */
                        if(lastPLIRequestTime == -1 ||
                                time > lastPLIRequestTime + PLI_INTERVAL)
                        {
                            lastPLIRequestTime = time;
                            sendRTCPFeedbackPLI();
                        }
                    }
                }

                try
                {
                    Thread.sleep(100);
                }
                catch(Exception e)
                {
                }
            }
        }
    }
}
