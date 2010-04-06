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

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.util.*;

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
     * @return
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
        int outOffset = outBuffer.getOffset();
        int newOutLength = inLength;
        int octet;

        if (start_bit)
        {
            /*
             * The Start bit and End bit MUST NOT both be set to one in the same
             * FU header.
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

    private int dePacketizeSingleNALUnitPacket(
            byte[] in,
            int inOffset,
            int inLength,
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

        return BUFFER_PROCESSED_OK;
    }

    protected void doClose()
    {
    }

    protected void doOpen()
        throws ResourceUnavailableException
    {
        fuaStartedAndNotEnded = false;
        lastSequenceNumber = -1;
        lastTimeStamp = -1;
    }

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

        if ((lastSequenceNumber != -1)
                && ((sequenceNumber - lastSequenceNumber) != 1))
        {
            if (logger.isTraceEnabled())
                logger.trace(
                        "Dropping RTP packets upto sequenceNumber "
                            + lastSequenceNumber
                            + " and continuing with sequenceNumber "
                            + sequenceNumber);

            fuaStartedAndNotEnded = false;
            if (sequenceNumber <= lastSequenceNumber)
            {
                // Drop the input Buffer.
                outBuffer.setDiscard(true);
                return BUFFER_PROCESSED_OK;
            }
            else
                outBuffer.setLength(0); // Reset.
        }
        lastSequenceNumber = sequenceNumber;

        // If the RTP time stamp changes, we're receiving a new NAL unit.
        long timeStamp = inBuffer.getTimeStamp();

        if(timeStamp != lastTimeStamp)
        {
            // Reset.
            fuaStartedAndNotEnded = false;
            outBuffer.setLength(0);
        }
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
        int ret;

        // Single NAL Unit Packet
        if ((nal_unit_type >= 1) && (nal_unit_type <= 23))
        {
            fuaStartedAndNotEnded = false;
            ret
                = dePacketizeSingleNALUnitPacket(
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
}
