/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.util.*;

/**
 * Parses H264 rtp headers and extracts the data in the format
 * the decoder expects it. RFC3984.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class H264Parser
{
    private final Logger logger = Logger.getLogger(H264Parser.class);

    // allocate enough space for the incoming data
    private static final int MAX_FRAME_SIZE = 128 * 1024;

    // every NAL starts with this start sequence
    private static final byte[] startSequence = { 0, 0, 1};

    // the timestamp of the last received rtp packet
    private long lastTimestamp = -1;

    // the result data is collected in this buffer
    private final byte[] encodedFrame;

    // the size of the result data
    private int encodedFrameLen;

    private final int encodedFramePaddingSize;

    public H264Parser()
    {
        this(0);
    }

    public H264Parser(int encodedFramePaddingSize)
    {
        this.encodedFramePaddingSize = encodedFramePaddingSize;
        this.encodedFrame =
            new byte[MAX_FRAME_SIZE + this.encodedFramePaddingSize];
    }

    /**
     * New rtp packet is received. We push it to the parser to extract the data.
     * @param inputBuffer the data from the rtp packet
     * @return true if the result data must be passed to the decoder.
     */
    public boolean pushRTPInput(Buffer inputBuffer)
    {
        long currentStamp = inputBuffer.getTimeStamp();

        // the rtp marker field points that this is the last packet of
        // the received frame
        boolean hasMarker =
            (inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0;

        // if the timestamp changes we are starting receiving a new frame
        if(!(currentStamp == lastTimestamp))
        {
            reset();
        }
        // the new frame timestamp
        lastTimestamp = currentStamp;

        byte[] inData = (byte[]) inputBuffer.getData();
        int inputOffset = inputBuffer.getOffset();
        byte fByte = inData[inputOffset];
        int type = fByte & 0x1f;
        int nri = (fByte & 0x60) >> 5;

        if(nri == 0)
            return false;

        try
        {
            // types from 1 to 23 are treated the same way
            if (type >= 1 && type <= 23)
            {
                System.arraycopy(startSequence, 0, encodedFrame, encodedFrameLen, startSequence.length);
                encodedFrameLen += startSequence.length;
                int len = inputBuffer.getLength();
                System.arraycopy(inData, inputOffset, encodedFrame, encodedFrameLen, len);
                encodedFrameLen += len;
                ensureEncodedFramePaddingSize();
            }
            //else if (type == 24)
            //{
                //return deencapsulateSTAP(inputBuffer);
            //}
            else if (type == 28)
            {
                deencapsulateFU(fByte, inputBuffer);
            }
            else
            {
                logger.warn("Skipping unsupported NAL unit type");
                return false;
            }
        }
        catch(Exception ex)
        {
            logger.warn("Cannot parse incoming " + ex.getMessage());
            reset();
            return false;
        }

        return hasMarker;
    }

    /**
     * Extract data from FU packet. This are packets across several rtp packets,
     * the first has a start bit set, we store all data and don't care about end
     * bit.
     *
     * @param nal
     * @param inputBuffer
     */
    private void deencapsulateFU (byte nal, Buffer inputBuffer)
    {
        byte[] buf = (byte[])inputBuffer.getData();
        int len = inputBuffer.getLength();
        int offset = inputBuffer.getOffset();

        offset++;
        len--;

        byte fu_indicator = nal;
        byte fu_header = buf[offset];
        boolean start_bit = (fu_header >> 7) != 0;
        //boolean end_bit = ((fu_header & 0x40) >> 6) != 0;
        int nal_type = (fu_header & 0x1f);
        byte reconstructed_nal;
        //reconstruct this packet's true nal; only the data follows..
        //the original nal forbidden bit and NRI are stored in this packet's nal;
        reconstructed_nal = (byte)(fu_indicator & (byte)0xe0);
        reconstructed_nal |= nal_type;

        // skip the fu_header...
        offset++;
        len--;

        if (start_bit)
        {
            // copy in the start sequence, and the reconstructed nal....
            System.arraycopy(startSequence, 0, encodedFrame, encodedFrameLen,
                startSequence.length);
            encodedFrameLen += startSequence.length;
            encodedFrame[encodedFrameLen] = reconstructed_nal;
            encodedFrameLen++;
        }
        System.arraycopy(buf, offset, encodedFrame, encodedFrameLen, len);
        encodedFrameLen += len;
        ensureEncodedFramePaddingSize();
    }

    /**
     * Returns the result data extracted from one ore more rtp packest.
     * @return the result data.
     */
    public byte[] getEncodedFrame()
    {
        return encodedFrame;
    }

    /**
     * Returns the result data length.
     * @return the result length.
     */
    public int getEncodedFrameLen()
    {
        return encodedFrameLen;
    }

    void reset()
    {
        encodedFrameLen = 0;
        ensureEncodedFramePaddingSize();
    }

    private void ensureEncodedFramePaddingSize()
    {
        Arrays.fill(encodedFrame, encodedFrameLen, encodedFrameLen
            + encodedFramePaddingSize, (byte) 0);
    }
}
