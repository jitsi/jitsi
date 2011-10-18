/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h263p;

import java.awt.*;
import java.util.*;
import java.util.List; // disambiguation

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.sf.fmj.media.*;

/**
 * Packetizes H.263+ encoded data into RTP packets in accord with RFC 4529
 * "RTP Payload Format for ITU-T Rec. H.263 Video".
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 */
public class Packetizer
    extends AbstractPacketizer
{
       /**
     * Array of default output formats.
     */
    private static final Format[] DEFAULT_OUTPUT_FORMATS
        = { new VideoFormat(Constants.H263P_RTP) };

    /**
     * Maximum payload size without the headers.
     */
    public static final int MAX_PAYLOAD_SIZE = 1024;

    /**
     * Name of the plugin.
     */
    private static final String PLUGIN_NAME = "H263+ Packetizer";

    /**
     * The sequence number of the next RTP packet to be output by this
     * <tt>Packetizer</tt>.
     */
    private int sequenceNumber = 0;

    /**
     * The list of H263+ "Start code" video packets to be sent as payload in RTP
     * packets.
     */
    private final List<byte[]> videoPkts = new LinkedList<byte[]>();

    /**
     * The timeStamp of the RTP packets in which H263+ packets are to be sent.
     */
    private long timeStamp = 0;

    /**
     * Initializes a new <tt>Packetizer</tt> instance which is to packetize
     * H.263+ encoded data into RTP packets in accord with
     * RFC 4529 "RTP Payload Format for ITU-T Rec. H.263 Video".
     */
    public Packetizer()
    {
        inputFormats = new Format[] { new VideoFormat(Constants.H263P) };

        inputFormat = null;
        outputFormat = null;
    }

    /**
     * Get the matching output formats for a specific format.
     *
     * @param in input format
     * @return array for formats matching input format
     */
    private Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat videoIn = (VideoFormat) in;
        Dimension inSize = videoIn.getSize();

        return
            new VideoFormat[]
            {
                new VideoFormat(
                        Constants.H263P_RTP,
                        inSize,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        videoIn.getFrameRate())
            };
    }

    /**
     * Get codec name.
     *
     * @return codec name
     */
    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Return the list of formats supported at the output.
     * @param in input <tt>Format</tt> to determine corresponding output
     * <tt>Format/tt>s
     * @return array of formats supported at output
     */
    public Format[] getSupportedOutputFormats(Format in)
    {
        // null input format
        if (in == null)
            return DEFAULT_OUTPUT_FORMATS;

        // mismatch input format
        if (!(in instanceof VideoFormat)
                || (null == AbstractCodecExt.matches(in, inputFormats)))
        {
            return new Format[0];
        }

        return getMatchingOutputFormats(in);
    }

    /**
     * Open this <tt>Packetizer</tt>.
     *
     * @throws ResourceUnavailableException if something goes wrong during
     * initialization of the Packetizer.
     */
    @Override
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (!opened)
        {
            videoPkts.clear();
            sequenceNumber = 0;

            super.open();
            opened = true;
        }
    }

    /**
     * Close this <tt>Packetizer</tt>.
     */
    @Override
    public synchronized void close()
    {
        if (opened)
        {
            videoPkts.clear();
            opened = false;
            super.close();
        }
    }

    /**
     * Sets the input format.
     *
     * @param in format to set
     * @return format
     */
    @Override
    public Format setInputFormat(Format in)
    {
        /*
         * Return null if the specified input Format is incompatible with this
         * Packetizer.
         */
        if (!(in instanceof VideoFormat)
                || null == AbstractCodecExt.matches(in, inputFormats))
            return null;

        inputFormat = in;
        return in;
    }

    /**
     * Sets the <tt>Format</tt> in which this <tt>Codec</tt> is to output media
     * data.
     *
     * @param out the <tt>Format</tt> in which this <tt>Codec</tt> is to
     * output media data
     * @return the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to output media data or <tt>null</tt> if <tt>format</tt> was
     * found to be incompatible with this <tt>Codec</tt>
     */
    @Override
    public Format setOutputFormat(Format out)
    {
        /*
         * Return null if the specified output Format is incompatible with this
         * Packetizer.
         */
        if (!(out instanceof VideoFormat)
                || (null
                        == AbstractCodecExt.matches(
                                out,
                                getMatchingOutputFormats(inputFormat))))
            return null;

        VideoFormat videoOut = (VideoFormat) out;
        Dimension outSize = videoOut.getSize();

        if (outSize == null)
        {
            Dimension inSize = ((VideoFormat) inputFormat).getSize();

            outSize
                = (inSize == null)
                    ? new Dimension(
                            Constants.VIDEO_WIDTH,
                            Constants.VIDEO_HEIGHT)
                    : inSize;
        }

        outputFormat
            = new VideoFormat(
                    videoOut.getEncoding(),
                    outSize,
                    outSize.width * outSize.height,
                    Format.byteArray,
                    videoOut.getFrameRate());

        // Return the outputFormat which is actually set.
        return outputFormat;
    }

    /**
     * Processes (packetize) a buffer.
     *
     * @param inBuffer input buffer
     * @param outBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     */
    @Override
    public int process(Buffer inBuffer, Buffer outBuffer)
    {
        int inLength = inBuffer.getLength();
        byte inData[] = (byte[])inBuffer.getData();
        int inOffset = inBuffer.getOffset();
        boolean pktAdded = false;

        if (videoPkts.size() > 0)
        {
            byte[] pktData = videoPkts.remove(0);

            // Send the packet.
            outBuffer.setData(pktData);
            outBuffer.setLength(pktData.length);
            outBuffer.setOffset(0);
            outBuffer.setTimeStamp(timeStamp);
            outBuffer.setSequenceNumber(sequenceNumber++);

            // If there are other packets, send them as well.
            if(videoPkts.size() > 0)
            {
                return (BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED);
            }
            else
            {
                // It's the last packet of the current frame so mark it.
                outBuffer.setFlags(
                    outBuffer.getFlags() | Buffer.FLAG_RTP_MARKER);

                return BUFFER_PROCESSED_OK;
            }
        }

        if (isEOM(inBuffer))
        {
            propagateEOM(outBuffer);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        if (inBuffer.isDiscard())
        {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        Format inFormat = inBuffer.getFormat();

        if ((inFormat != inputFormat) && !inFormat.matches(inputFormat))
            setInputFormat(inFormat);

        int endIndex = inOffset + inLength;
        int beginIndex = findStartcode(inData, inOffset, endIndex);

        if (beginIndex < endIndex)
        {
            for (int nextBeginIndex;
                    beginIndex < endIndex;
                    beginIndex = nextBeginIndex + 3)
            {
                nextBeginIndex = findStartcode(inData, beginIndex + 3,
                        endIndex);
                int length = nextBeginIndex - beginIndex;

                if (length > 0)
                {
                    pktAdded
                        = packetize(inData, beginIndex, length)
                            || pktAdded;
                    beginIndex += length;
                }
            }
        }

        timeStamp = inBuffer.getTimeStamp();

        if(pktAdded)
        {
            return process(inBuffer, outBuffer);
        }
        else
        {
            /* first frame is not a synchronization point, discard ?*/
            return BUFFER_PROCESSED_FAILED;
        }
     }

    /**
     * Packetizes H.263+ encoded data so that it becomes ready to be sent as the
     * payload of RTP packets.
     *
     * @param data the bytes which contain the H.263+ encoded data to be
     * packetized
     * @param offset the offset of H.263+ encoded data to be packetized begins
     * @param length the length  of the H.263+ encoded data starting at offset
     * @return <tt>true</tt> if at least one RTP packet payload has been
     * packetized i.e. prepared for sending; otherwise, <tt>false</tt>
     */
    private boolean packetize(byte[] data, int offset, int length)
    {
        boolean pktAdded = false;

        while(length > 0)
        {
            boolean isPsc = false;
            int pos = 0;
            int maxPayloadLength = MAX_PAYLOAD_SIZE;
            byte pkt[] = null;
            int payloadLength = 0;

            /* is we are at synchronization point (PSC, GSBC, EOS, EOSBS) */
            if(data.length > 3 && data[offset] == 0x00 &&
                    data[offset + 1] == 0x00)
            {
                isPsc = true;
                pos = 2;
            }
            else
            {
                maxPayloadLength -= 2;
            }

            if(length > maxPayloadLength)
            {
                payloadLength = maxPayloadLength;
            }
            else
            {
                payloadLength = length;
            }

            pkt = new byte[payloadLength + (isPsc ? 0 : 2)];

            /* add H263+ payload header */
            /* no VRC and no extra picture header */
            pkt[0] = (byte)(isPsc ? 0x04 : 0x00);
            pkt[1] = 0x00;

            System.arraycopy(data, offset + pos, pkt, 2, payloadLength - pos);
            pktAdded = videoPkts.add(pkt) || pktAdded;

            offset += payloadLength;
            length -= payloadLength;
        }

        return pktAdded;
    }

    /**
     * Finds the index in <tt>byteStream</tt> at which a Picture Start code
     * begins.
     *
     * @param byteStream the H.263+ encoded byte stream
     * @param beginIndex the inclusive index in <tt>byteStream</tt> at which the
     * search is to begin
     * @param endIndex the exclusive index in <tt>byteStream</tt> at which the
     * search is to end
     * @return the index in <tt>byteStream</tt> at which the Picture Start code
     * begins, otherwise, <tt>endIndex</tt>
     */
    private static int findStartcode(byte[] byteStream, int beginIndex,
            int endIndex)
    {
        for (; beginIndex < (endIndex - 3); beginIndex++)
            if((byteStream[beginIndex] == 0)
                    && (byteStream[beginIndex + 1] == 0)
                    && ((byteStream[beginIndex + 2] & (byte)0x80) == -128))
            {
                return beginIndex;
            }
        return endIndex;
    }
}
