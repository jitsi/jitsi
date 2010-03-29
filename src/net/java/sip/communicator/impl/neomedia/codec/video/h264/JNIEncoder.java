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
 * Encodes supplied data in h264
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class JNIEncoder
    extends AbstractCodec
{
    private static final String PLUGIN_NAME = "H.264 Encoder";

    private static final int INPUT_BUFFER_PADDING_SIZE = 8;

    private static final Format[] defOutputFormats =
    { new VideoFormat(Constants.H264) };

    // the frame rate we will use
    private static final int TARGET_FRAME_RATE = 15;

    // The codec we will use
    private long avcontext;

    // the encoded data is stored in avpicture
    private long avframe;

    // we use this buffer to supply data to encoder
    private byte[] encFrameBuffer;

    // the supplied data length
    private int encFrameLen;

    private long rawFrameBuffer;

    // key frame every 300 frames
    private static final int IFRAME_INTERVAL = 300;

    private int framesSinceLastIFrame = IFRAME_INTERVAL + 1;

    /**
     * Constructor
     */
    public JNIEncoder()
    {
        float sourceFrameRate = TARGET_FRAME_RATE;

        inputFormats = new Format[1];

        inputFormats[0] = new YUVFormat(null, -1, Format.byteArray,
                sourceFrameRate, YUVFormat.YUV_420, -1, -1, 0, -1, -1);

        inputFormat = null;
        outputFormat = null;
    }

    private Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat videoIn = (VideoFormat) in;
        Dimension inSize = videoIn.getSize();

        return
            new VideoFormat[]
            { new VideoFormat(Constants.H264, inSize, Format.NOT_SPECIFIED,
                Format.byteArray, videoIn.getFrameRate()) };
    }

    /**
     * Return the list of formats supported at the output.
     */
    public Format[] getSupportedOutputFormats(Format in)
    {
        // null input format
        if (in == null)
            return defOutputFormats;

        // mismatch input format
        if (!(in instanceof VideoFormat)
                || (null == AbstractCodecExt.matches(in, inputFormats)))
            return new Format[0];

        return getMatchingOutputFormats(in);
    }

    @Override
    public Format setInputFormat(Format in)
    {
        // mismatch input format
        if (!(in instanceof VideoFormat)
                || null == AbstractCodecExt.matches(in, inputFormats))
            return null;

        VideoFormat videoIn = (VideoFormat) in;
        Dimension inSize = videoIn.getSize();

        if (inSize == null)
        {
            /* XXX code reached ? */
            inSize = new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);
        }

        YUVFormat yuv = (YUVFormat) videoIn;

        if (yuv.getOffsetU() > yuv.getOffsetV())
            return null;

        int strideY = inSize.width;
        int strideUV = strideY / 2;
        int offsetU = strideY * inSize.height;
        int offsetV = offsetU + strideUV * inSize.height / 2;

        int inputYuvLength = (strideY + strideUV) * inSize.height;
        float sourceFrameRate = videoIn.getFrameRate();

        inputFormat =
            new YUVFormat(inSize, inputYuvLength + INPUT_BUFFER_PADDING_SIZE,
                Format.byteArray, sourceFrameRate, YUVFormat.YUV_420, strideY,
                strideUV, 0, offsetU, offsetV);

        // Return the selected inputFormat
        return inputFormat;
    }

    @Override
    public Format setOutputFormat(Format out)
    {
        // mismatch output format
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
            if (inSize == null)
            {
                /* XXX code reached ? */
                outSize = new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);
            }
            else
                outSize = inSize;
        }

        outputFormat =
            new VideoFormat(videoOut.getEncoding(), outSize, outSize.width
                * outSize.height, Format.byteArray, videoOut.getFrameRate());

        // Return the selected outputFormat
        return outputFormat;
    }

    public synchronized int process(Buffer inBuffer, Buffer outBuffer)
    {
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
        if (inFormat != inputFormat && !(inFormat.matches(inputFormat)))
            setInputFormat(inFormat);

        if (inBuffer.getLength() < 10)
        {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        // copy data to avframe
        FFmpeg.memcpy(rawFrameBuffer, (byte[]) inBuffer.getData(), inBuffer
            .getOffset(), encFrameLen);

        if (framesSinceLastIFrame >= IFRAME_INTERVAL)
        {
            FFmpeg.avframe_set_key_frame(avframe, true);
            framesSinceLastIFrame = 0;
        }
        else
        {
            framesSinceLastIFrame++;
            FFmpeg.avframe_set_key_frame(avframe, false);
        }

        // encode data
        int encLen =
            FFmpeg.avcodec_encode_video(avcontext, encFrameBuffer, encFrameLen,
                avframe);

        byte[] r = new byte[encLen];
        System.arraycopy(encFrameBuffer, 0, r, 0, r.length);

        outBuffer.setData(r);
        outBuffer.setLength(r.length);
        outBuffer.setOffset(0);
        //outBuffer.setTimeStamp(inBuffer.getTimeStamp());

        return BUFFER_PROCESSED_OK;
    }

    @Override
    public synchronized void open()
        throws ResourceUnavailableException
    {
        int width = 0;
        int height = 0;

        if (opened)
            return;

        if (inputFormat == null)
            throw new ResourceUnavailableException("No input format selected");
        if (outputFormat == null)
            throw new ResourceUnavailableException("No output format selected");

        width = (int)((VideoFormat)outputFormat).getSize().getWidth();
        height = (int)((VideoFormat)outputFormat).getSize().getHeight();

        long avcodec = FFmpeg.avcodec_find_encoder(FFmpeg.CODEC_ID_H264);

        avcontext = FFmpeg.avcodec_alloc_context();

        FFmpeg.avcodeccontext_set_pix_fmt(avcontext, FFmpeg.PIX_FMT_YUV420P);
        FFmpeg.avcodeccontext_set_size(avcontext, width, height);

        FFmpeg.avcodeccontext_set_qcompress(avcontext, 0.6f);

        //int _bitRate = 768000;
        int _bitRate = 128000;
        // average bit rate
        FFmpeg.avcodeccontext_set_bit_rate(avcontext, _bitRate);
        // so to be 1 in x264
        FFmpeg.avcodeccontext_set_bit_rate_tolerance(avcontext, _bitRate);
        FFmpeg.avcodeccontext_set_rc_max_rate(avcontext, _bitRate);
        FFmpeg.avcodeccontext_set_sample_aspect_ratio(avcontext, 0, 0);
        FFmpeg.avcodeccontext_set_thread_count(avcontext, 1);
        /* time base should be 1 / frame rate */
        FFmpeg.avcodeccontext_set_time_base(avcontext, 1, TARGET_FRAME_RATE);
        FFmpeg.avcodeccontext_set_quantizer(avcontext, 22, 30, 4);

        // avcontext.chromaoffset = -2;

        FFmpeg.avcodeccontext_add_partitions(avcontext, 0x111);
        // X264_PART_I4X4 0x001
        // X264_PART_P8X8 0x010
        // X264_PART_B8X8 0x100

        FFmpeg.avcodeccontext_set_mb_decision(avcontext,
            FFmpeg.FF_MB_DECISION_SIMPLE);

        FFmpeg.avcodeccontext_set_rc_eq(avcontext, "blurCplx^(1-qComp)");

        FFmpeg.avcodeccontext_add_flags(avcontext,
            FFmpeg.CODEC_FLAG_LOOP_FILTER);
        FFmpeg.avcodeccontext_set_me_method(avcontext, 7);
        FFmpeg.avcodeccontext_set_me_subpel_quality(avcontext, 6);
        FFmpeg.avcodeccontext_set_me_range(avcontext, 16);
        FFmpeg.avcodeccontext_set_me_cmp(avcontext, FFmpeg.FF_CMP_CHROMA);
        FFmpeg.avcodeccontext_set_scenechange_threshold(avcontext, 40);
        // Constant quality mode (also known as constant ratefactor)
        /* FFmpeg.avcodeccontext_set_crf(avcontext, 0); */
        FFmpeg.avcodeccontext_set_rc_buffer_size(avcontext, 0);
        FFmpeg.avcodeccontext_set_gop_size(avcontext, IFRAME_INTERVAL);
        FFmpeg.avcodeccontext_set_i_quant_factor(avcontext, 1f / 1.4f);

        FFmpeg.avcodeccontext_set_refs(avcontext, 4);
        FFmpeg.avcodeccontext_set_trellis(avcontext, 2);

        if (FFmpeg.avcodec_open(avcontext, avcodec) < 0)
            throw new RuntimeException("Could not open codec");

        encFrameLen = (width * height * 3) / 2;

        rawFrameBuffer = FFmpeg.av_malloc(encFrameLen);

        avframe = FFmpeg.avcodec_alloc_frame();
        int size = width * height;
        FFmpeg.avframe_set_data(avframe, rawFrameBuffer, size, size / 4);
        FFmpeg.avframe_set_linesize(avframe, width, width / 2, width / 2);

        encFrameBuffer = new byte[encFrameLen];

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

            FFmpeg.avcodec_close(avcontext);
            FFmpeg.av_free(avcontext);
            avcontext = 0;

            FFmpeg.av_free(avframe);
            avframe = 0;
            FFmpeg.av_free(rawFrameBuffer);
            rawFrameBuffer = 0;

            encFrameBuffer = null;
        }
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }
}
