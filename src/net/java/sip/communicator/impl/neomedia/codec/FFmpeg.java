/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec;

/**
 * Provides the interface to the native FFmpeg library.
 *
 * @author Lubomir Marinov
 * @author Sebastien Vincent
 */
public class FFmpeg
{
    public static final long AV_NOPTS_VALUE = 0x8000000000000000L;

    public static final int CODEC_FLAG_LOOP_FILTER = 0x00000800;

    public static final int CODEC_ID_H264 = 28;

    public static final int CODEC_ID_MJPEG = 8;

    public static final int CODEC_ID_MP3 = 0x15000 + 1;

    public static final int FF_BUG_AUTODETECT = 1;

    public static final int FF_CMP_CHROMA = 256;

    public static final int FF_INPUT_BUFFER_PADDING_SIZE = 8;

    public static final int FF_MB_DECISION_SIMPLE = 0;

    /** The minimum encoding buffer size defined by libavcodec. */
    public static final int FF_MIN_BUFFER_SIZE = 16384;

    public static final int PIX_FMT_ARGB = 27;

    /**
     * BGR32 format handled in endian specific manner.
     * It is stored as ABGR on big-endian and RGBA on little-endian.
     */
    public static final int PIX_FMT_BGR32;

    /**
     * BGR32_1 format handled in endian specific manner.
     * It is stored as BGRA on big-endian and ARGB on little-endian.
     */
    public static final int PIX_FMT_BGR32_1;

    public static final int PIX_FMT_NONE = -1;

    public static final int PIX_FMT_NV12 = 25;

    /**
     * RGB24 format handled in endian specific manner.
     * It is stored as RGB on big-endian and BGR on little-endian.
     */
    public static final int PIX_FMT_RGB24;

    /**
     * RGB32 format handled in endian specific manner.
     * It is stored as ARGB on big-endian and BGRA on little-endian.
     */
    public static final int PIX_FMT_RGB32;

    /**
     * RGB32_1 format handled in endian specific manner.
     * It is stored as RGBA on big-endian and ABGR on little-endian.
     */
    public static final int PIX_FMT_RGB32_1;

    public static final int PIX_FMT_UYVY422 = 17;

    public static final int PIX_FMT_UYYVYY411 = 18;

    public static final int PIX_FMT_YUV420P = 0;

    public static final int PIX_FMT_YUYV422 = 1;

    public static final int PIX_FMT_YUVJ422P = 13;

    public static final int SWS_BICUBIC = 4;

    public static final int X264_RC_ABR = 2;

    public static native void av_free(long ptr);

    public static native long av_malloc(int size);

    public static native void av_register_all();

    public static native long avcodec_alloc_context();

    public static native long avcodec_alloc_frame();

    public static native int avcodec_close(long avctx);

    public static native int avcodec_decode_video(long avctx, long frame,
        boolean[] got_picture, byte[] buf, int buf_size);

    public static native int avcodec_decode_video(long avcontext,
            long avframe, long src, int src_length);

    /**
     * Encodes an audio frame from <tt>samples</tt> into <tt>buf</tt>.
     *
     * @param avctx the codec context
     * @param buf the output buffer
     * @param buf_offset the output buffer offset
     * @param buf_size the output buffer size
     * @param samples the input buffer containing the samples. The number of
     * samples read from this buffer is <tt>frame_size</tt>*<tt>channels</tt>,
     * both of which are defined in <tt>avctx</tt>. For PCM audio the number of
     * samples read from samples is equal to
     * <tt>buf_size</tt>*<tt>input_sample_size</tt>/<tt>output_sample_size</tt>.
     * @param samples_offset the offset in the input buffer containing the
     * samples
     * @return on error a negative value is returned, on success zero or the
     * number of bytes used to encode the data read from the input buffer
     */
    public static native int avcodec_encode_audio(
            long avctx,
            byte[] buf, int buf_offset, int buf_size,
            byte[] samples, int samples_offset);

    public static native int avcodec_encode_video(long avctx, byte[] buff,
        int buf_size, long frame);

    public static native long avcodec_find_decoder(int id);

    /**
     * Finds a registered encoder with a matching codec ID.
     *
     * @param id <tt>CodecID</tt> of the requested encoder
     * @return an <tt>AVCodec</tt> encoder if one was found; <tt>0</tt>,
     * otherwise
     */
    public static native long avcodec_find_encoder(int id);

    public static native void avcodec_init();

    /**
     * Initializes the specified <tt>AVCodecContext</tt> to use the specified
     * <tt>AVCodec</tt>.
     *
     * @param avctx the <tt>AVCodecContext</tt> which will be set up to use the
     * specified <tt>AVCodec</tt>
     * @param codec the <tt>AVCodec</tt> to use within the
     * <tt>AVCodecContext</tt>
     * @return zero on success, a negative value on error
     */
    public static native int avcodec_open(long avctx, long codec);

    public static native void avcodeccontext_add_flags(long avctx, int flags);

    public static native void avcodeccontext_add_partitions(long avctx,
        int partitions);

    /**
     * Gets the samples per packet of the specified <tt>AVCodecContext</tt>. The
     * property is set by libavcodec upon {@link #avcodec_open(long, long)}.
     *
     * @param avctx the <tt>AVCodecContext</tt> to get the samples per packet of
     * @return the samples per packet of the specified <tt>AVCodecContext</tt>
     */
    public static native int avcodeccontext_get_frame_size(long avctx);

    public static native int avcodeccontext_get_height(long avctx);

    public static native int avcodeccontext_get_pix_fmt(long avctx);

    public static native int avcodeccontext_get_width(long avctx);

    public static native void avcodeccontext_set_b_frame_strategy(long avctx,
        int b_frame_strategy);

    /**
     * Sets the average bit rate of the specified <tt>AVCodecContext</tt>. The
     * property is to be set by the user when encoding and is unused for
     * constant quantizer encoding. It is set by libavcodec when decoding and
     * its value is <tt>0</tt> or some bitrate if this info is available in the
     * stream. 
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the average bit rate of
     * @param bit_rate the average bit rate to be set to the specified
     * <tt>AVCodecContext</tt>
     */
    public static native void avcodeccontext_set_bit_rate(long avctx,
        int bit_rate);

    public static native void avcodeccontext_set_bit_rate_tolerance(long avctx,
        int bit_rate_tolerance);

    /**
     * Sets the number of channels of the specified <tt>AVCodecContext</tt>. The
     * property is audio only.
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the number of channels of
     * @param channels the number of channels to set to the specified
     * <tt>AVCodecContext</tt>
     */
    public static native void avcodeccontext_set_channels(
            long avctx, int channels);

    public static native void avcodeccontext_set_chromaoffset(long avctx,
        int chromaoffset);

    public static native void avcodeccontext_set_crf(long avctx, float crf);

    public static native void avcodeccontext_set_deblockbeta(long avctx,
        int deblockbeta);

    public static native void avcodeccontext_set_gop_size(long avctx,
        int gop_size);

    public static native void avcodeccontext_set_i_quant_factor(long avctx,
        float i_quant_factor);

    public static native void avcodeccontext_set_max_b_frames(long avctx,
        int max_b_frames);

    public static native void avcodeccontext_set_mb_decision(long avctx,
        int mb_decision);

    public static native void avcodeccontext_set_me_cmp(long avctx, int me_cmp);

    public static native void avcodeccontext_set_me_method(long avctx,
        int me_method);

    public static native void avcodeccontext_set_me_range(long avctx,
        int me_range);

    public static native void avcodeccontext_set_me_subpel_quality(long avctx,
        int me_subpel_quality);

    public static native void avcodeccontext_set_pix_fmt(long avctx, int pix_fmt);

    public static native void avcodeccontext_set_qcompress(long avctx,
        float qcompress);

    public static native void avcodeccontext_set_quantizer(long avctx,
        int qmin, int qmax, int max_qdiff);

    public static native void avcodeccontext_set_rc_buffer_size(long avctx,
        int rc_buffer_size);

    public static native void avcodeccontext_set_rc_eq(long avctx, String rc_eq);

    public static native void avcodeccontext_set_rc_max_rate(long avctx,
        int rc_max_rate);

    public static native void avcodeccontext_set_refs(long avctx,
        int refs);

    public static native void avcodeccontext_set_rtp_payload_size(long avctx,
        int rtp_payload_size);

    public static native void avcodeccontext_set_sample_aspect_ratio(
        long avctx, int num, int den);

    /**
     * Sets the samples per second of the specified <tt>AVCodecContext</tt>. The
     * property is audio only.
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the samples per second of
     * @param sample_rate the samples per second to set to the specified
     * <tt>AVCodecContext</tt>
     */
    public static native void avcodeccontext_set_sample_rate(
            long avctx, int sample_rate);

    public static native void avcodeccontext_set_scenechange_threshold(
        long avctx, int scenechange_threshold);

    public static native void avcodeccontext_set_size(long avctx, int width,
        int height);

    public static native void avcodeccontext_set_thread_count(long avctx,
        int thread_count);

    public static native void avcodeccontext_set_ticks_per_frame(long avctx,
        int ticks_per_frame);

    public static native void avcodeccontext_set_time_base(long avctx, int num,
        int den);

    public static native void avcodeccontext_set_trellis(long avctx,
        int trellis);

    public static native void avcodeccontext_set_workaround_bugs(long avctx,
        int workaround_bugs);

    public static native long avframe_get_pts(long frame);

    public static native void avframe_set_data(long frame, long data0,
        long offset1, long offset2);

    public static native void avframe_set_key_frame(long frame,
        boolean key_frame);

    public static native void avframe_set_linesize(long frame, int linesize0,
        int linesize1, int linesize2);

    public static native int avpicture_fill(long picture, long ptr,
        int pix_fmt, int width, int height);

    public static native long avpicture_get_data0(long picture);

    public static native int avpicture_get_size(int pix_fmt, int width,
        int height);

    public static native void memcpy(int[] dst, int dst_offset, int dst_length,
        long src);

    public static native void memcpy(long dst, byte[] src, int src_offset,
        int src_length);

    private static native int PIX_FMT_BGR32();

    private static native int PIX_FMT_BGR32_1();

    private static native int PIX_FMT_RGB24();

    private static native int PIX_FMT_RGB32();

    private static native int PIX_FMT_RGB32_1();

    public static native void sws_freeContext(long context);

    public static native long sws_getCachedContext(
        long context,
        int srcW, int srcH, int srcFormat,
        int dstW, int dstH, int dstFormat,
        int flags);

    public static native int sws_scale(
        long context,
        long src, int srcSliceY, int srcSliceH,
        Object dst, int dstFormat, int dstW, int dstH);

    public static native int sws_scale(
        long context,
        Object src, int srcFormat, int srcW, int srcH,
        int srcSliceY, int srcSliceH,
        Object dst, int dstFormat, int dstW, int dstH);

    static
    {
        System.loadLibrary("ffmpeg");

        av_register_all();
        avcodec_init();

        PIX_FMT_BGR32 = PIX_FMT_BGR32();
        PIX_FMT_BGR32_1 = PIX_FMT_BGR32_1();
        PIX_FMT_RGB24 = PIX_FMT_RGB24();
        PIX_FMT_RGB32 = PIX_FMT_RGB32();
        PIX_FMT_RGB32_1 = PIX_FMT_RGB32_1();
    }
}
