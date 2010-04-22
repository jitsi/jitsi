/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec.video.h264;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.codec.*;
import net.java.sip.communicator.impl.media.codec.video.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Decodes incoming rtp data of type h264 and returns the result frames in RGB
 * format.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
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

    // supported sizes by the codec
    private final Dimension[] supportedSizes = new Dimension[]
    {
    // P720
        new Dimension(720, 480),
        // CIF4
        new Dimension(704, 576),
        // CIF
        new Dimension(352, 288), new Dimension(320, 240),
        // QCIF
        new Dimension(176, 144),
        // SQCIF
        new Dimension(128, 96) };

    // index of default size (output format)
    private static int defaultSizeIx = 2;

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

        outputFormats = new VideoFormat[supportedSizes.length];

        Dimension targetVideoSize =
            new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);

        for (int i = 0; i < supportedSizes.length; i++)
        {
            Dimension size = supportedSizes[i];

            if (size.equals(targetVideoSize))
                defaultSizeIx = i;

            outputFormats[i] =
            // PIX_FMT_RGB32
                new RGBFormat(size, -1, Format.intArray,
                    ensureFrameRate(Format.NOT_SPECIFIED), 32, RED_MASK,
                    GREEN_MASK, BLUE_MASK, 1, size.width, Format.FALSE,
                    Format.NOT_SPECIFIED);
        }

        currentVideoWidth = outputFormats[defaultSizeIx].getSize().getWidth();
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
            ovf = outputFormats[defaultSizeIx];
        }
        else
        {
            ovf = null;
            for (VideoFormat vf : outputFormats)
            {
                if (vf.getSize().equals(inSize))
                {
                    ovf = vf;
                    break;
                }
            }
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
    public synchronized void open() throws ResourceUnavailableException
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

        if (avctxWidth != 0 && currentVideoWidth != avctxWidth)
        {
            currentVideoWidth = avctxWidth;

            VideoFormat ivf = (VideoFormat) inputBuffer.getFormat();
            VideoFormat ovf =
                getVideoFormat(currentVideoWidth, ivf.getFrameRate());
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
        int avctxHeight = FFMPEG.avcodeccontext_get_height(avcontext);
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

    private VideoFormat getVideoFormat(double width, float frameRate)
    {
        for (VideoFormat vf : outputFormats)
        {
            Dimension size = vf.getSize();

            if (size.getWidth() == width)
            {
                return new RGBFormat(size, -1, Format.intArray,
                    ensureFrameRate(frameRate), 32, RED_MASK, GREEN_MASK,
                    BLUE_MASK, 1, size.width, Format.FALSE,
                    Format.NOT_SPECIFIED);
            }
        }
        return null;
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

    @Override
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
     *
     * @param in input format
     * @param outs array of format
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
