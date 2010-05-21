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

import net.java.sip.communicator.service.neomedia.event.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.sf.fmj.media.*;

/**
 * Implements a H.264 encoder.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class JNIEncoder
    extends AbstractCodec
    implements RTCPFeedbackListener
{
    /**
     * Default output formats.
     */
    private static final Format[] DEFAULT_OUTPUT_FORMATS
        = { new VideoFormat(Constants.H264) };

    /**
     * Key frame every 300 frames.
     */
    private static final int IFRAME_INTERVAL = 300;

    /**
     * Padding size.
     */
    private static final int INPUT_BUFFER_PADDING_SIZE = 8;

    /**
     * Name of the code.
     */
    private static final String PLUGIN_NAME = "H.264 Encoder";

    /**
     * Minimum interval between two PLI request processing (in milliseconds).
     */
    private static final long PLI_INTERVAL = 3000;

    /**
     * The frame rate we will use
     */
    private static final int TARGET_FRAME_RATE = 15;

    /**
     * The codec we will use.
     */
    private long avcontext;

    /**
     * The encoded data is stored in avpicture.
     */
    private long avframe;

    /**
     * We use this buffer to supply data to encoder.
     */
    private byte[] encFrameBuffer;

    /**
     * The supplied data length.
     */
    private int encFrameLen;

    /**
     * Next interval for an automatic keyframe.
     */
    private int framesSinceLastIFrame = IFRAME_INTERVAL + 1;

    /**
     * The raw frame buffer.
     */
    private long rawFrameBuffer;

    /**
     * Force encoder to send a key frame.
     */
    private boolean forceKeyFrame = false;

    /**
     * Last keyframe request time.
     */
    private long lastKeyframeRequestTime = System.currentTimeMillis();

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        inputFormats
            = new Format[]
            {
                new YUVFormat(
                        null,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        TARGET_FRAME_RATE,
                        YUVFormat.YUV_420,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        0, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED)
            };

        inputFormat = null;
        outputFormat = null;
    }

    /**
     * Close the <tt>Codec</tt>.
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
            FFmpeg.av_free(rawFrameBuffer);
            rawFrameBuffer = 0;

            encFrameBuffer = null;
        }
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

        return
            new VideoFormat[]
            {
                new VideoFormat(
                        Constants.H264,
                        videoIn.getSize(),
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
     *
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
            return new Format[0];

        return getMatchingOutputFormats(in);
    }

    /**
     * Inits <tt>Codec</tt> instance.
     */
    @Override
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (opened)
            return;

        if (inputFormat == null)
            throw new ResourceUnavailableException("No input format selected");
        if (outputFormat == null)
            throw new ResourceUnavailableException("No output format selected");

        Dimension outputFormatSize = ((VideoFormat) outputFormat).getSize();
        int width = outputFormatSize.width;
        int height = outputFormatSize.height;

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
        // time_base should be 1 / frame rate
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
        // FFmpeg.avcodeccontext_set_crf(avcontext, 0);
        FFmpeg.avcodeccontext_set_rc_buffer_size(avcontext, 0);
        FFmpeg.avcodeccontext_set_gop_size(avcontext, IFRAME_INTERVAL);
        FFmpeg.avcodeccontext_set_i_quant_factor(avcontext, 1f / 1.4f);

        FFmpeg.avcodeccontext_set_refs(avcontext, 4);
        FFmpeg.avcodeccontext_set_trellis(avcontext, 2);

        /*
         * AVCodecContext's rtp_payload_size is supposed to try to provide an
         * alternative to the "Fragmentation Units (FUs)" created by Packetizer
         * but the former does not seem to be guaranteed and the video looks
         * better at the time of this writing when only Packetizer takes care of
         * ensuring MAX_PAYLOAD_SIZE.
         */
        //FFmpeg.avcodeccontext_set_rtp_payload_size(avcontext,
        //        Packetizer.MAX_PAYLOAD_SIZE);

        if (FFmpeg.avcodec_open(avcontext, avcodec) < 0)
        {
            throw
                new ResourceUnavailableException(
                        "Could not open codec. (size= "
                            + width + "x" + height
                            + ")");
        }

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

    /**
     * Processes (encode) a buffer.
     *
     * @param inBuffer input buffer
     * @param outBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     */
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

        if ((inFormat != inputFormat) && !(inFormat.matches(inputFormat)))
            setInputFormat(inFormat);

        if (inBuffer.getLength() < 10)
        {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        // copy data to avframe
        FFmpeg.memcpy(
                rawFrameBuffer,
                (byte[]) inBuffer.getData(), inBuffer.getOffset(),
                encFrameLen);

        if (framesSinceLastIFrame >= IFRAME_INTERVAL || forceKeyFrame)
        {
            FFmpeg.avframe_set_key_frame(avframe, true);
            framesSinceLastIFrame = 0;
            forceKeyFrame = false;
        }
        else
        {
            framesSinceLastIFrame++;
            FFmpeg.avframe_set_key_frame(avframe, false);
        }

        // encode data
        int encLen
            = FFmpeg.avcodec_encode_video(
                    avcontext,
                    encFrameBuffer, encFrameLen,
                    avframe);

        /*
         * Do not always allocate a new data array for outBuffer, try to reuse
         * the existing one if it is suitable.
         */
        Object outData = outBuffer.getData();
        byte[] out;

        if (outData instanceof byte[])
        {
            out = (byte[]) outData;
            if (out.length < encLen)
                out = null;
        }
        else
            out = null;
        if (out == null)
            out = new byte[encLen];

        System.arraycopy(encFrameBuffer, 0, out, 0, encLen);

        outBuffer.setData(out);
        outBuffer.setLength(encLen);
        outBuffer.setOffset(0);
        outBuffer.setTimeStamp(inBuffer.getTimeStamp());
/*
        // flags
        int inFlags = inBuffer.getFlags();
        int outFlags = outBuffer.getFlags();

        if ((inFlags & Buffer.FLAG_LIVE_DATA) != 0)
            outFlags |= Buffer.FLAG_LIVE_DATA;
        if ((inFlags & Buffer.FLAG_RELATIVE_TIME) != 0)
            outFlags |= Buffer.FLAG_RELATIVE_TIME;
        if ((inFlags & Buffer.FLAG_RTP_TIME) != 0)
            outFlags |= Buffer.FLAG_RTP_TIME;
        if ((inFlags & Buffer.FLAG_SYSTEM_TIME) != 0)
            outFlags |= Buffer.FLAG_SYSTEM_TIME;
        outBuffer.setFlags(outFlags);
*/
        return BUFFER_PROCESSED_OK;
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
        // mismatch input format
        if (!(in instanceof VideoFormat)
                || (null == AbstractCodecExt.matches(in, inputFormats)))
            return null;

        YUVFormat yuv = (YUVFormat) in;
        Dimension inSize = yuv.getSize();

        if (inSize == null)
            inSize
                = new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);

        if (yuv.getOffsetU() > yuv.getOffsetV())
            return null;

        int strideY = inSize.width;
        int strideUV = strideY / 2;
        int offsetU = strideY * inSize.height;
        int offsetV = offsetU + strideUV * inSize.height / 2;

        int inYUVLength = (strideY + strideUV) * inSize.height;

        inputFormat
            = new YUVFormat(
                    inSize,
                    inYUVLength + INPUT_BUFFER_PADDING_SIZE,
                    Format.byteArray,
                    yuv.getFrameRate(),
                    YUVFormat.YUV_420,
                    strideY, strideUV,
                    0, offsetU, offsetV);

        // Return the selected inputFormat
        return inputFormat;
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

        // Return the selected outputFormat
        return outputFormat;
    }

    /**
     * Event fired when RTCP feedback message is received.
     *
     * @param event <tt>RTCPFeedbackEvent</tt>
     */
    public void feedbackReceived(RTCPFeedbackEvent event)
    {
        /* if RTCP message is a Picture Loss Indication (PLI)
         * or a Full Intra-frame Request (FIR) the encoder will
         * force the next frame to be a keyframe
         */
        if(event.getPayloadType() == RTCPFeedbackEvent.PT_PS)
        {
            switch(event.getFeedbackMessageType())
            {
                case RTCPFeedbackEvent.FMT_PLI:
                case RTCPFeedbackEvent.FMT_FIR:
                    if(System.currentTimeMillis() > lastKeyframeRequestTime
                            + PLI_INTERVAL)
                    {
                        lastKeyframeRequestTime = System.currentTimeMillis();
                        forceKeyFrame = true;
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
