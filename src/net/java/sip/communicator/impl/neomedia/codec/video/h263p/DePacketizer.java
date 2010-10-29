/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h263p;

import java.util.Arrays;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.util.Logger;
import net.sf.fmj.media.*;

/**
 * Depacketizes H.263+ RTP packets in in accord with RFC 4529 "RTP Payload
 * Format for ITU-T Rec. H.263 Video".
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
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
     * The size of the padding at the end of the output data of this
     * <tt>DePacketizer</tt> expected by the H.263+ decoder.
     */
    private final int outputPaddingSize = FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;

    /**
     * Keeps track of last (input) sequence number in order to avoid
     * inconsistent data.
     */
    private long lastSequenceNumber = -1;

    /**
     * The indicator which determines whether incomplete buffer packets are
     * output from the H.263+ <tt>DePacketizer</tt> to the decoder.
     */
    private static final boolean OUTPUT_INCOMPLETE_BUFFER = true;

    /**
     * Initializes a new <tt>DePacketizer</tt> instance which is to depacketize
     * H.263+ RTP packet.
     */
    public DePacketizer()
    {
        super(
            "H263+ DePacketizer",
            VideoFormat.class,
            new VideoFormat[] { new VideoFormat(Constants.H263P) });

        inputFormats
            = new VideoFormat[] { new VideoFormat(Constants.H263P_RTP) };
    }

    /**
     * Close the <tt>Codec</tt>.
     */
    protected void doClose()
    {
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
    }

    /**
     * Processes (depacketizes) a buffer.
     *
     * @param inBuffer input buffer
     * @param outBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     */
    protected int doProcess(Buffer inBuffer, Buffer outBuffer)
    {
        long sequenceNumber = inBuffer.getSequenceNumber();

        if ((lastSequenceNumber != -1)
                && ((sequenceNumber - lastSequenceNumber) != 1))
        {
            int ret = 0;

            /* maybe we lost a frame somewhere or the sequence number reach
             * its maximum number
             */
            if (logger.isTraceEnabled())
                logger.trace(
                        "Dropped RTP packets upto sequenceNumber "
                            + lastSequenceNumber
                            + " and continuing with sequenceNumber "
                            + sequenceNumber);

            ret = reset(outBuffer);

            if ((ret & OUTPUT_BUFFER_NOT_FILLED) == 0)
            {
                lastSequenceNumber = -1;
                return ret;
            }
        }

        lastSequenceNumber = sequenceNumber;

        byte[] in = (byte[]) inBuffer.getData();
        int inLength = inBuffer.getLength();
        int inOffset = inBuffer.getOffset();
        int outOffset = outBuffer.getOffset();

        if(inLength < 3)
        {
            return BUFFER_PROCESSED_FAILED;
        }

        boolean pBit = ((in[inOffset] & 0x04) > 0);
        boolean vBit = ((in[inOffset] & 0x02) > 0);;
        int plen = ((in[inOffset] & 0x01) << 5) +
            ((in[inOffset + 1] & 0xF8) >> 3);
        int dataLength = inLength - plen - (vBit ? 1 : 0) - (pBit ? 0 : 2);

        byte out[] = validateByteArraySize(outBuffer, outOffset + dataLength +
                outputPaddingSize);

        if(pBit)
        {
            out[0] = 0x00;
            out[1] = 0x00;
        }

        if(vBit)
        {
            /* ignore VRC */
        }

        if(plen > 0)
        {
            if(logger.isInfoEnabled())
            {
                logger.info("Extra picture header present PLEN=" + plen);
            }
        }

        System.arraycopy(in, inOffset + 2 + (vBit ? 1 : 0) + plen,
                out, outOffset + (pBit ? 2 : 0), dataLength - (pBit ? 2 : 0));

        padOutput(out, outOffset + dataLength);

        outBuffer.setLength(outOffset + dataLength);
        outBuffer.setSequenceNumber(sequenceNumber);

        /*
         * The RTP marker bit is set for the very last packet of the access unit
         * indicated by the RTP time stamp to allow an efficient playout buffer
         * handling. Consequently, we have to output it as well.
         */
        if ((inBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
        {
            outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_RTP_MARKER);
            outBuffer.setOffset(0);
            return BUFFER_PROCESSED_OK;
        }
        else
        {
            outBuffer.setOffset(outOffset + dataLength);
            return OUTPUT_BUFFER_NOT_FILLED;
        }
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
     * process input RTP payloads.
     *
     * @param outBuffer the output <tt>Buffer</tt> to be reset
     * @return the flags such as <tt>BUFFER_PROCESSED_OK</tt> and
     * <tt>OUTPUT_BUFFER_NOT_FILLED</tt> to be returned by
     * {@link #process(Buffer, Buffer)}
     */
    private int reset(Buffer outBuffer)
    {
        if (OUTPUT_INCOMPLETE_BUFFER && outBuffer.getLength() > 0)
        {
            Object outData = outBuffer.getData();

            if (outData instanceof byte[])
            {
                return (BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED);
            }
        }

        outBuffer.setLength(0);
        return OUTPUT_BUFFER_NOT_FILLED;
    }
}
