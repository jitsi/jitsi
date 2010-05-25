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
    /**
     * Plugin name.
     */
    private static final String PLUGIN_NAME = "H.264 Decoder";

    /**
     * The default output <tt>VideoFormat</tt>.
     */
    private static final VideoFormat[] DEFAULT_OUTPUT_FORMATS
        = new VideoFormat[] { new AVFrameFormat() };

    /**
     *  The codec context native pointer we will use.
     */
    private long avcontext;

    /**
     *  The decoded data is stored in avpicture in native ffmpeg format (YUV).
     */
    private long avframe;

    /**
     * If decoder has got a picture.
     */
    private final boolean[] got_picture = new boolean[1];

    /**
     * The last known height of {@link #avcontext} i.e. the video output by this
     * <tt>JNIDecoder</tt>. Used to detect changes in the output size.
     */
    private int height;

    /**
     * Array of output <tt>VideoFormat</tt>s.
     */
    private final VideoFormat[] outputFormats;

    /**
     * The last known width of {@link #avcontext} i.e. the video output by this
     * <tt>JNIDecoder</tt>. Used to detect changes in the output size.
     */
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
                        ensureFrameRate(Format.NOT_SPECIFIED),
                        FFmpeg.PIX_FMT_YUV420P)
            };

        Dimension outputSize = outputFormats[0].getSize();

        width = outputSize.width;
        height = outputSize.height;
    }

    /**
     * Check <tt>Format</tt>.
     *
     * @param format <tt>Format</tt> to check
     * @return true if <tt>Format</tt> is H264_RTP
     */
    public boolean checkFormat(Format format)
    {
        return format.getEncoding().equals(Constants.H264_RTP);
    }

    /**
     * Close <tt>Codec</tt>.
     */
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

    /**
     * Ensure frame rate.
     *
     * @param frameRate frame rate
     * @return frame rate
     */
    private float ensureFrameRate(float frameRate)
    {
        return frameRate;
    }

    /**
     * Get matching outputs for a specified input <tt>Format</tt>.
     *
     * @param in input <tt>Format</tt>
     * @return array of matching outputs or null if there are no matching
     * outputs.
     */
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
                new AVFrameFormat(
                        outSize,
                        ensureFrameRate(ivf.getFrameRate()),
                        FFmpeg.PIX_FMT_YUV420P)
            };
    }

    /**
     * Get plugin name.
     *
     * @return "H.264 Decoder"
     */
    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Get all supported output <tt>Format</tt>s.
     *
     * @param in input <tt>Format</tt> to determine corresponding output
     * <tt>Format/tt>s
     * @return array of supported <tt>Format</tt>
     */
    public Format[] getSupportedOutputFormats(Format in)
    {
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

    /**
     * Decodes H.264 media data read from a specific input <tt>Buffer</tt> into
     * a specific output <tt>Buffer</tt>.
     *
     * @param inBuffer input <tt>Buffer</tt>
     * @param outBuffer output <tt>Buffer</tt>
     * @return <tt>BUFFER_PROCESSED_OK</tt> if <tt>inBuffer</tt> has been
     * successfully processed
     */
    public synchronized int process(Buffer inBuffer, Buffer outBuffer)
    {
        if (!checkInputBuffer(inBuffer))
            return BUFFER_PROCESSED_FAILED;
        if (isEOM(inBuffer) || !opened)
        {
            propagateEOM(outBuffer);
            return BUFFER_PROCESSED_OK;
        }
        if (inBuffer.isDiscard())
        {
            outBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        // Ask FFmpeg to decode.
        got_picture[0] = false;
        // TODO Take into account the offset of inputBuffer.
        FFmpeg.avcodec_decode_video(
                avcontext,
                avframe,
                got_picture,
                (byte[]) inBuffer.getData(), inBuffer.getLength());

        if (!got_picture[0])
        {
            outBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        // format
        int width = FFmpeg.avcodeccontext_get_width(avcontext);
        int height = FFmpeg.avcodeccontext_get_height(avcontext);

        if ((width > 0)
                && (height > 0)
                && ((this.width != width) || (this.height != height)))
        {
            this.width = width;
            this.height = height;

            // Output in same size and frame rate as input.
            Dimension outSize = new Dimension(this.width, this.height);
            VideoFormat inFormat = (VideoFormat) inBuffer.getFormat();
            float outFrameRate = ensureFrameRate(inFormat.getFrameRate());

            outputFormat
                = new AVFrameFormat(
                        outSize,
                        outFrameRate,
                        FFmpeg.PIX_FMT_YUV420P);
        }
        outBuffer.setFormat(outputFormat);

        // data
        Object out = outBuffer.getData();

        if (!(out instanceof AVFrame) || (((AVFrame) out).getPtr() != avframe))
            outBuffer.setData(new AVFrame(avframe));

        // timeStamp
        long pts = FFmpeg.AV_NOPTS_VALUE; // TODO avframe_get_pts(avframe);

        if (pts == FFmpeg.AV_NOPTS_VALUE)
            outBuffer.setTimeStamp(Buffer.TIME_UNKNOWN);
        else
        {
            outBuffer.setTimeStamp(pts);

            int outFlags = outBuffer.getFlags();

            outFlags |= Buffer.FLAG_RELATIVE_TIME;
            outFlags &= ~(Buffer.FLAG_RTP_TIME | Buffer.FLAG_SYSTEM_TIME);
            outBuffer.setFlags(outFlags);
        }

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Sets the <tt>Format</tt> of the media data to be input for processing in
     * this <tt>Codec</tt>.
     *
     * @param format the <tt>Format</tt> of the media data to be input for
     * processing in this <tt>Codec</tt>
     * @return the <tt>Format</tt> of the media data to be input for processing
     * in this <tt>Codec</tt> if <tt>format</tt> is compatible with this
     * <tt>Codec</tt>; otherwise, <tt>null</tt>
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
