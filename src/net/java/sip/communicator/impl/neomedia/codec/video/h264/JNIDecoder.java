/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Decodes incoming rtp data of type h264 and returns the result frames in RGB
 * format.
 * 
 * @author Damian Minkov
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class JNIDecoder
    extends AbstractCodec
    implements Codec
{
    private final Logger logger = Logger.getLogger(JNIDecoder.class);

    private static final String PLUGIN_NAME = "H.264 Decoder";

    private static final int RED_MASK = 0xff0000;
    private static final int GREEN_MASK = 0x00ff00;
    private static final int BLUE_MASK = 0x0000ff;

    private final VideoFormat[] outputFormats;

    private final VideoFormat[] defaultOutputFormats = new VideoFormat[]
    { new RGBFormat() };

    // The codec we will use
    private long avcontext;

    // The decoded data is stored in avpicture in native ffmpeg format (YUV)
    private long avframe;

    // Used to convert decoded data to RGB
    private long frameRGB;

    // The parser used to parse rtp content
    private final H264Parser parser =
        new H264Parser(FFMPEG.FF_INPUT_BUFFER_PADDING_SIZE);
    
    // current width of video, so we can detect changes in video size
    private double currentVideoWidth;

    // keep track of last received sequence in order to avoid inconsistent data
    private long lastReceivedSeq = -1;

    // in case of inconsistent data drop all data till a marker is received
    private boolean waitingForMarker = false;

    private final boolean[] got_picture = new boolean[1];

    /**
     * Constructs new h264 decoder
     */
    public JNIDecoder()
    {
        inputFormats = new VideoFormat[]
        { new VideoFormat(Constants.H264_RTP) };

        outputFormats = new VideoFormat[1];

        /* default output format */
        outputFormats[0] = new RGBFormat(
                new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT), 
                -1, Format.intArray,
                ensureFrameRate(Format.NOT_SPECIFIED), 32, RED_MASK,
                GREEN_MASK, BLUE_MASK, 1, -1, Format.FALSE,
                Format.NOT_SPECIFIED);

        currentVideoWidth = outputFormats[0].getSize().getWidth();
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat ivf = (VideoFormat) in;
        Dimension inSize = ivf.getSize();

        VideoFormat ovf;
        // return the default size/currently decoder and encoder
        // set to transmit/receive at this size
        if (inSize == null)
        {
            ovf = outputFormats[0];
        }
        else
        {
            /* output in same size as input */
            ovf = ivf;
        }

        if (ovf == null)
            return null;

        Dimension outSize = ovf.getSize();
        return new Format[]
        { new RGBFormat(outSize, -1, Format.intArray,
            ensureFrameRate(ivf.getFrameRate()), 32, RED_MASK, GREEN_MASK,
            BLUE_MASK, 1, outSize.width, Format.FALSE, Format.NOT_SPECIFIED) };
    }

    /**
     * Set the data input format.
     *
     * @return false if the format is not supported.
     */
    @Override
    public Format setInputFormat(Format format)
    {
        if (super.setInputFormat(format) != null)
        {
            reset();
            return format;
        }
        else
            return null;
    }

    @Override
    public Format setOutputFormat(Format format)
    {
        return super.setOutputFormat(format);
    }

    /**
     * Init the codec instances.
     */
    @Override
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (opened)
            return;

        long avcodec = FFMPEG.avcodec_find_decoder(FFMPEG.CODEC_ID_H264);

        avcontext = FFMPEG.avcodec_alloc_context();
        FFMPEG.avcodeccontext_set_workaround_bugs(avcontext,
            FFMPEG.FF_BUG_AUTODETECT);

        if (FFMPEG.avcodec_open(avcontext, avcodec) < 0)
            throw new RuntimeException("Could not open codec");

        avframe = FFMPEG.avcodec_alloc_frame();
        frameRGB = FFMPEG.avcodec_alloc_frame();

        opened = true;
        super.open();
    }

    @Override
    public synchronized void close()
    {
        if (opened)
        {
            opened = false;
            super.close();

            FFMPEG.avcodec_close(avcontext);
            FFMPEG.av_free(avcontext);
            avcontext = 0;

            FFMPEG.av_free(avframe);
            avframe = 0;
        }
    }

    public synchronized int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer) || !opened)
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        if (inputBuffer.isDiscard())
        {
            inputBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        if(waitingForMarker)
        {
            lastReceivedSeq = inputBuffer.getSequenceNumber();
            if((inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
            {
                waitingForMarker = false;
                outputBuffer.setDiscard(true);
                return BUFFER_PROCESSED_OK;
            }
            else
                return OUTPUT_BUFFER_NOT_FILLED;
        }

        if (lastReceivedSeq != -1
            && inputBuffer.getSequenceNumber() - lastReceivedSeq > 1)
        {
            long oldRecv = lastReceivedSeq;
            lastReceivedSeq = inputBuffer.getSequenceNumber();
            waitingForMarker = true;
            logger.trace("DROP rtp data! " + oldRecv + "/" + lastReceivedSeq);
            parser.reset();
            reset();
            return OUTPUT_BUFFER_NOT_FILLED;
        }
        else if (!parser.pushRTPInput(inputBuffer))
        {
            lastReceivedSeq = inputBuffer.getSequenceNumber();
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        lastReceivedSeq = inputBuffer.getSequenceNumber();

        // decodes the data
        got_picture[0] = false;
        FFMPEG.avcodec_decode_video(avcontext, avframe, got_picture, parser
            .getEncodedFrame(), parser.getEncodedFrameLen());

        int avctxWidth = FFMPEG.avcodeccontext_get_width(avcontext);
        int avctxHeight = FFMPEG.avcodeccontext_get_height(avcontext);

        if (avctxWidth != 0 && currentVideoWidth != avctxWidth)
        {
            currentVideoWidth = avctxWidth;

            VideoFormat ivf = (VideoFormat) inputBuffer.getFormat();
            /* output format with same size as input */
            VideoFormat ovf = new RGBFormat(new Dimension(avctxWidth, 
                        avctxHeight), -1, Format.intArray,
                        ensureFrameRate(ivf.getFrameRate()), 32, RED_MASK, GREEN_MASK,
                        BLUE_MASK, 1, avctxWidth, Format.FALSE, 
                        Format.NOT_SPECIFIED);

            if (ovf != null)
            {
                outputFormat = ovf;
            }
        }
        outputBuffer.setFormat(outputFormat);

        if (!got_picture[0])
        {
            outputBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        // convert the picture in RGB Format
        int numBytes =
            FFMPEG.avpicture_get_size(FFMPEG.PIX_FMT_RGB32, avctxWidth,
                avctxHeight);
        long buffer = FFMPEG.av_malloc(numBytes);

        FFMPEG.avpicture_fill(frameRGB, buffer, FFMPEG.PIX_FMT_RGB32,
            avctxWidth, avctxHeight);

        // Convert the image from its native format to RGB
        FFMPEG.img_convert(frameRGB, FFMPEG.PIX_FMT_RGB32, avframe, FFMPEG
            .avcodeccontext_get_pix_fmt(avcontext), avctxWidth, avctxHeight);

        Object outData = outputBuffer.getData();
        int dataLength = numBytes / 4;
        int[] data;
        if ((outData instanceof int[])
            && ((int[]) outData).length >= dataLength)
            data = (int[]) outData;
        else
            data = new int[dataLength];

        FFMPEG
            .memcpy(data, 0, dataLength, FFMPEG.avpicture_get_data0(frameRGB));

        outputBuffer.setOffset(0);
        outputBuffer.setLength(dataLength);
        outputBuffer.setData(data);

        FFMPEG.av_free(buffer);
        return BUFFER_PROCESSED_OK;
    }

    public boolean checkFormat(Format format)
    {
        return format.getEncoding().equals(Constants.H264_RTP);
    }

    private float ensureFrameRate(float frameRate)
    {
        return frameRate;
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    public Format[] getSupportedOutputFormats(Format in)
    {
        // null input format
        if (in == null)
        {
            return defaultOutputFormats;
        }

        // mismatch input format
        if (!(in instanceof VideoFormat) || (matches(in, inputFormats) == null))
        {
            return new Format[0];
        }

        // match input format
        return getMatchingOutputFormats(in);
    }

    /**
     * Utility to perform format matching.
     */
    public static Format matches(Format in, Format outs[])
    {
        for (Format out : outs)
        {
            if (in.matches(out))
                return out;
        }
        return null;
    }
}
