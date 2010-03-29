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
     * The start sequence of every NAL.
     */
    private static final byte[] NAL_START_SEQUENCE = { 0, 0, 1 };

    /**
     * If last processed packet has a marker (indicate end of frame).
     */
    private boolean lastHasMarker = false;

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
     * <tt>DePpacketizer</tt> expected by the H.264 decoder.
     */
    private final int outputPaddingSize
        = FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;

    /**
     * In case of inconsistent input drop all data until a marker is received.
     */
    private boolean waitingForMarker = false;

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
     * @param input the payload of the RTP packet from which a FU-A fragment of
     * a NAL unit is to be extracted
     * @param inputOffset the offset in <tt>input</tt> at which the payload
     * starts
     * @param inputLength the length of the payload in <tt>input</tt> starting
     * at <tt>inputOffset</tt>
     * @param outputBuffer the <tt>Buffer</tt> which is to receive the extracted
     * FU-A fragment of a NAL unit
     */
    private void deencapsulateFU(
            byte[] input, int inputOffset, int inputLength,
            Buffer outputBuffer)
    {
        byte fu_indicator = input[inputOffset];

        // Skip fu_indicator.
        inputOffset++;
        inputLength--;

        byte fu_header = input[inputOffset];
        boolean start_bit = (fu_header >> 7) != 0;
        //boolean end_bit = ((fu_header & 0x40) >> 6) != 0;
        int nal_type = (fu_header & 0x1f);
        byte reconstructed_nal;

        //reconstruct this packet's true nal; only the data follows..
        //the original nal forbidden bit and NRI are stored in this packet's nal;
        reconstructed_nal = (byte)(fu_indicator & (byte)0xe0);
        reconstructed_nal |= nal_type;

        // Skip fu_header.
        inputOffset++;
        inputLength--;

        int outputOffset = outputBuffer.getOffset();
        int outputLength = outputBuffer.getLength();
        int newOutputLength = outputLength + inputLength;

        if (start_bit)
            newOutputLength += NAL_START_SEQUENCE.length + 1;

        byte[] output
            = validateByteArraySize(
                outputBuffer,
                outputOffset + newOutputLength + outputPaddingSize);

        outputOffset += outputLength;

        if (start_bit)
        {
            // Copy in the start sequence and the reconstructed NAL.
            System.arraycopy(
                    NAL_START_SEQUENCE, 0,
                    output, outputOffset,
                    NAL_START_SEQUENCE.length);
            outputOffset += NAL_START_SEQUENCE.length;

            output[outputOffset] = reconstructed_nal;
            outputOffset++;
        }
        System.arraycopy(
                input, inputOffset,
                output, outputOffset,
                inputLength);
        outputOffset += inputLength;

        padOutput(output, outputOffset);

        outputBuffer.setLength(newOutputLength);
    }

    protected void doClose()
    {
    }

    protected void doOpen()
        throws ResourceUnavailableException
    {
        lastHasMarker = false;
        lastSequenceNumber = -1;
        lastTimeStamp = -1;
        waitingForMarker = false;
    }

    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (waitingForMarker)
        {
            lastSequenceNumber = inputBuffer.getSequenceNumber();
            if ((inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
            {
                waitingForMarker = false;
                discardOutputBuffer(outputBuffer);
                return BUFFER_PROCESSED_OK;
            }
            else
                return OUTPUT_BUFFER_NOT_FILLED;
        }

        long inputSequenceNumber = inputBuffer.getSequenceNumber();

        // Detect inconsistent input drop.
        if ((lastSequenceNumber != -1)
                && (inputSequenceNumber - lastSequenceNumber > 1))
        {
            if (logger.isTraceEnabled())
                logger.trace(
                        "Dropping RTP data! "
                            + lastSequenceNumber + "/" + inputSequenceNumber);

            lastSequenceNumber = inputSequenceNumber;
            waitingForMarker = true;
            outputBuffer.setLength(0);
            return OUTPUT_BUFFER_NOT_FILLED;
        }
        else
            lastSequenceNumber = inputSequenceNumber;

        // if the timestamp changes we are starting receiving a new frame
        // this is also the case when last processed packet has marker
        long timeStamp = inputBuffer.getTimeStamp();

        if((timeStamp != lastTimeStamp) || lastHasMarker)
            outputBuffer.setLength(0); // reset
        // the new frame timestamp
        lastTimeStamp = timeStamp;

        byte[] input = (byte[]) inputBuffer.getData();
        int inputOffset = inputBuffer.getOffset();
        byte fByte = input[inputOffset];

        /*
         * A value of 00 indicates that the content of the NAL unit is not used
         * to reconstruct reference pictures for inter picture prediction. Such
         * NAL units can be discarded without risking the integrity of the
         * reference pictures.
         */
        int nri = (fByte & 0x60) >> 5;

        if(nri == 0)
            return OUTPUT_BUFFER_NOT_FILLED;

        int type = fByte & 0x1f;

        try
        {
            if ((type >= 1) && (type <= 23)) // Single NAL unit packet per H.264
            {
                int outputOffset = outputBuffer.getOffset();
                int outputLength = outputBuffer.getLength();
                int inputLength = inputBuffer.getLength();
                int newOutputLength
                    = outputLength + NAL_START_SEQUENCE.length + inputLength;
                byte[] output
                    = validateByteArraySize(
                        outputBuffer,
                        outputOffset + newOutputLength + outputPaddingSize);

                outputOffset += outputLength;

                System.arraycopy(
                        NAL_START_SEQUENCE, 0,
                        output, outputOffset,
                        NAL_START_SEQUENCE.length);
                outputOffset += NAL_START_SEQUENCE.length;

                System.arraycopy(
                        input, inputOffset,
                        output, outputOffset,
                        inputLength);
                outputOffset += inputLength;

                padOutput(output, outputOffset);

                outputBuffer.setLength(newOutputLength);
            }
            else if (type == 28) // FU-A Fragmentation unit
            {
                deencapsulateFU(
                    input, inputOffset, inputBuffer.getLength(),
                    outputBuffer);
            }
            else
            {
                logger.warn("Skipping unsupported NAL unit type");
                return OUTPUT_BUFFER_NOT_FILLED;
            }
        }
        catch (Exception ex)
        {
            logger.warn("Cannot parse incoming packet", ex);
            outputBuffer.setLength(0); // reset
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        outputBuffer.setTimeStamp(timeStamp);

        // the rtp marker field points that this is the last packet of
        // the received frame
        boolean hasMarker
            = (inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0;

        lastHasMarker = hasMarker;

        return hasMarker ? BUFFER_PROCESSED_OK : OUTPUT_BUFFER_NOT_FILLED;
    }

    private void padOutput(byte[] output, int outputOffset)
    {
        Arrays.fill(
                output,
                outputOffset,
                outputOffset + outputPaddingSize,
                (byte) 0);
    }
}
