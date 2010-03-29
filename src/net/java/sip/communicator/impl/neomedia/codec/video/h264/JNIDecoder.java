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
import net.sf.fmj.media.*;

/**
 * Decodes H.264 NAL units and returns the resulting frames as FFmpeg
 * <tt>AVFrame</tt>s (i.e. in YUV format).
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class JNIDecoder
    extends AbstractCodec
{
    private static final String PLUGIN_NAME = "H.264 Decoder";

    private static final int RED_MASK = 0xff0000;
    private static final int GREEN_MASK = 0x00ff00;
    private static final int BLUE_MASK = 0x0000ff;

    private static final VideoFormat[] DEFAULT_OUTPUT_FORMATS
        = new VideoFormat[] { new AVFrameFormat() };

    // The codec we will use
    private long avcontext;

    // The decoded data is stored in avpicture in native ffmpeg format (YUV)
    private long avframe;

    private final boolean[] got_picture = new boolean[1];

    private final VideoFormat[] outputFormats;

    // current width of video, so we can detect changes in video size
    private int width;

    /**
     * Initializes a new <tt>JNIDecoder</tt> instance which is to decode H.264
     * NAL units into frames in YUV format.
     */
    public JNIDecoder()
    {
        inputFormats
            = new VideoFormat[] { new VideoFormat(Constants.H264) };
        outputFormats
            = new VideoFormat[]
            {
                new AVFrameFormat(
                        new Dimension(
                                Constants.VIDEO_WIDTH,
                                Constants.VIDEO_HEIGHT),
                        ensureFrameRate(Format.NOT_SPECIFIED))
            };

        width = outputFormats[0].getSize().width;
    }

    public boolean checkFormat(Format format)
    {
        return format.getEncoding().equals(Constants.H264_RTP);
    }

    @Override
    public synchronized void close()
    {
        if (opened)
        {
            opened = false;
            super.close();

            FFmpeg.avcodec_close(avcontext);
            FFmpeg.av_free(avcontext);
            avcontext = 0;

            FFmpeg.av_free(avframe);
            avframe = 0;
        }
    }

    private float ensureFrameRate(float frameRate)
    {
        return frameRate;
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat ivf = (VideoFormat) in;
        Dimension inSize = ivf.getSize();
        Dimension outSize;

        // return the default size/currently decoder and encoder
        // set to transmit/receive at this size
        if (inSize == null)
        {
            VideoFormat ovf = outputFormats[0];

            if (ovf == null)
                return null;
            else
                outSize = ovf.getSize();
        }
        else
            outSize = inSize; // Output in same size as input.

        return
            new Format[]
            {
                new AVFrameFormat(outSize, ensureFrameRate(ivf.getFrameRate()))
            };
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
            return DEFAULT_OUTPUT_FORMATS;

        // mismatch input format
        if (!(in instanceof VideoFormat)
                || (AbstractCodecExt.matches(in, inputFormats) == null))
            return new Format[0];

        // match input format
        return getMatchingOutputFormats(in);
    }

    /**
     * Inits the codec instances.
     */
    @Override
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (opened)
            return;

        long avcodec = FFmpeg.avcodec_find_decoder(FFmpeg.CODEC_ID_H264);

        avcontext = FFmpeg.avcodec_alloc_context();
        FFmpeg.avcodeccontext_set_workaround_bugs(avcontext,
            FFmpeg.FF_BUG_AUTODETECT);

        if (FFmpeg.avcodec_open(avcontext, avcodec) < 0)
            throw new RuntimeException("Could not open codec CODEC_ID_H264");

        avframe = FFmpeg.avcodec_alloc_frame();

        opened = true;
        super.open();
    }

    public synchronized int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
            return BUFFER_PROCESSED_FAILED;
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

        // decodes the data
        got_picture[0] = false;
        // TODO Take into account the offset of inputBuffer.
        FFmpeg.avcodec_decode_video(
                avcontext,
                avframe,
                got_picture,
                (byte[]) inputBuffer.getData(), inputBuffer.getLength());

        if (!got_picture[0])
        {
            outputBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        int avctxWidth = FFmpeg.avcodeccontext_get_width(avcontext);
        int avctxHeight = FFmpeg.avcodeccontext_get_height(avcontext);

        if ((avctxWidth != 0) && (avctxWidth != width))
        {
            width = avctxWidth;

            // Output in same size and frame rate as input.
            Dimension outSize = new Dimension(avctxWidth, avctxHeight);
            VideoFormat ivf = (VideoFormat) inputBuffer.getFormat();
            float outFrameRate = ensureFrameRate(ivf.getFrameRate());

            outputFormat = new AVFrameFormat(outSize, outFrameRate);
        }
        outputBuffer.setFormat(outputFormat);

        Object outputData = outputBuffer.getData();

        if (!(outputData instanceof AVFrame)
                || (((AVFrame) outputData).getPtr() != avframe))
            outputBuffer.setData(new AVFrame(avframe));

        //outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
        return BUFFER_PROCESSED_OK;
    }

    /**
     * Set the data input format.
     *
     * @return false if the format is not supported.
     */
    @Override
    public Format setInputFormat(Format format)
    {
        Format setFormat = super.setInputFormat(format);

        if (setFormat != null)
            reset();
        return setFormat;
    }
}
