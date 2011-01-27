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
    /**
     * No pts value.
     */
    public static final long AV_NOPTS_VALUE = 0x8000000000000000L;

    /**
     * Loop filter flag.
     */
    public static final int CODEC_FLAG_LOOP_FILTER = 0x00000800;

    /**
     * H263+ UMV flag.
     */
    public static final int CODEC_FLAG_H263P_UMV = 0x01000000 ;

    /**
     * AC pred flag.
     */
    public static final int CODEC_FLAG_AC_PRED = 0x02000000;

    /**
     * H263+ slice struct flag.
     */
    public static final int CODEC_FLAG_H263P_SLICE_STRUCT = 0x10000000;

    /**
     * Intra refresh flag2.
     */
    public static final int CODEC_FLAG2_INTRA_REFRESH = 0x00200000;

    /**
     * Allow to pass incomplete frame to decoder.
     */
    public static final int CODEC_FLAG2_CHUNKS = 0x00008000;

    /**
     * H264 codec ID.
     */
    public static final int CODEC_ID_H264 = 28;

    /**
     * H263 codec ID.
     */
    public static final int CODEC_ID_H263 = 5;

    /**
     * H263+ codec ID.
     */
    public static final int CODEC_ID_H263P = 20;

    /**
     * MJPEG codec ID.
     */
    public static final int CODEC_ID_MJPEG = 8;

    /**
     * MP3 codec ID.
     */
    public static final int CODEC_ID_MP3 = 0x15000 + 1;

    /**
     * Work around bugs in encoders which sometimes cannot be detected
     * automatically.
     */
    public static final int FF_BUG_AUTODETECT = 1;

    public static final int FF_CMP_CHROMA = 256;

    /**
     * Padding size for FFmpeg input buffer.
     */
    public static final int FF_INPUT_BUFFER_PADDING_SIZE = 8;

    public static final int FF_MB_DECISION_SIMPLE = 0;

    /**
     * The minimum encoding buffer size defined by libavcodec.
     */
    public static final int FF_MIN_BUFFER_SIZE = 16384;

    /**
     * ARGB format.
     */
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

    /**
     * "NONE" format.
     */
    public static final int PIX_FMT_NONE = -1;

    /**
     * NV12 format.
     */
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

    /**
     * UYVY422 format.
     */
    public static final int PIX_FMT_UYVY422 = 17;

    /**
     * UYYVYY411 format.
     */
    public static final int PIX_FMT_UYYVYY411 = 18;

    /**
     * YUV420P format.
     */
    public static final int PIX_FMT_YUV420P = 0;

    /**
     * YUYV422 format.
     */
    public static final int PIX_FMT_YUYV422 = 1;

    /**
     * YUVJ422P format.
     */
    public static final int PIX_FMT_YUVJ422P = 13;

    /**
     * BICUBIC type for libswscale conversion.
     */
    public static final int SWS_BICUBIC = 4;

    //public static final int X264_RC_ABR = 2;

    /**
     * Free a native pointer allocated by av_malloc.
     *
     * @param ptr native pointer to free
     */
    public static native void av_free(long ptr);

    /**
     * Allocate memory.
     *
     * @param size size to allocate
     * @return native pointer or 0 if av_malloc failed
     */
    public static native long av_malloc(int size);

    /**
     * Initialize libavformat and register all the muxers, demuxers and
     * protocols.
     */
    public static native void av_register_all();

    /**
     * Allocate a AVContext.
     *
     * @return native pointer to AVContext
     */
    public static native long avcodec_alloc_context();

    /**
     * Allocate a AVFrame.
     *
     * @return native pointer to AVFrame
     */
    public static native long avcodec_alloc_frame();

    /**
     * Close an AVCodecContext
     *
     * @param avctx pointer to AVCodecContex
     * @return 0 if success, -1 otherwise
     */
    public static native int avcodec_close(long avctx);

    /**
     * Decode a video frame.
     *
     * @param avctx codec context
     * @param frame frame decoded
     * @param got_picture if the decoding has produced a valid picture
     * @param buf the input buffer
     * @param buf_size input buffer size
     * @return number of bytes written to buff if success
     */
    public static native int avcodec_decode_video(long avctx, long frame,
        boolean[] got_picture, byte[] buf, int buf_size);

    /**
     * Decode a video frame.
     *
     * @param avcontext codec context
     * @param avframe frame decoded
     * @param src input buffer
     * @param src_length input buffer size
     * @return number of bytes written to buff if success
     */
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

    /**
     * Encode a video frame.
     *
     * @param avctx codec context
     * @param buff the output buffer
     * @param buf_size output buffer size
     * @param frame frame to encode
     * @return number of bytes written to buff if success
     */
    public static native int avcodec_encode_video(long avctx, byte[] buff,
        int buf_size, long frame);

    /**
     * Find a registered decoder with a matching ID.
     *
     * @param id <tt>CodecID</tt> of the requested encoder
     * @return an <tt>AVCodec</tt> encoder if one was found; <tt>0</tt>,
     * otherwise
     */
    public static native long avcodec_find_decoder(int id);

    /**
     * Finds a registered encoder with a matching codec ID.
     *
     * @param id <tt>CodecID</tt> of the requested encoder
     * @return an <tt>AVCodec</tt> encoder if one was found; <tt>0</tt>,
     * otherwise
     */
    public static native long avcodec_find_encoder(int id);

    /**
     * Initializes FFmpeg's avcodec.
     */
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

    /**
     * Add specific flags to AVCodecContext's flags member.
     *
     * @param avctx pointer to AVCodecContext
     * @param flags flags to add
     */
    public static native void avcodeccontext_add_flags(long avctx, int flags);

    /**
     * Add specific flags to AVCodecContext's flags2 member.
     *
     * @param avctx pointer to AVCodecContext
     * @param flags flags to add
     */
    public static native void avcodeccontext_add_flags2(long avctx, int flags);

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

    /**
     * Get height of the video.
     *
     * @param avctx pointer to AVCodecContext
     * @return video height
     */
    public static native int avcodeccontext_get_height(long avctx);

    /**
     * Get pixel format.
     *
     * @param avctx pointer to AVCodecContext
     * @return pixel format
     */
    public static native int avcodeccontext_get_pix_fmt(long avctx);

    /**
     * Get width of the video.
     *
     * @param avctx pointer to AVCodecContext
     * @return video width
     */
    public static native int avcodeccontext_get_width(long avctx);

    /**
     * Set the B-Frame strategy.
     *
     * @param avctx AVCodecContext pointer
     * @param b_frame_strategy strategy
     */
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

    /**
     * Set the bit rate tolerance
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the bit rate of
     * @param bit_rate_tolerance bit rate tolerance
     */
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

    /**
     * Set the gop size (key frame interval).
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the gop size of
     * @param gop_size key frame interval
     */
    public static native void avcodeccontext_set_gop_size(long avctx,
        int gop_size);

    public static native void avcodeccontext_set_i_quant_factor(long avctx,
        float i_quant_factor);

    /**
     * Set the maximum B frames.
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the maximum B frames of
     * @param max_b_frames maximum B frames
     */
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

    /**
     * Set the pixel format.
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the pixel format of
     * @param pix_fmt pixel format
     */
    public static native void avcodeccontext_set_pix_fmt(long avctx,
            int pix_fmt);

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

    /**
     * Set the RTP payload size.
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the RTP payload size of
     * @param rtp_payload_size RTP payload size
     */
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

    /**
     * Set the scene change threshold (in percent).
     *
     * @param avctx AVCodecContext pointer
     * @param scenechange_threshold value between 0 and 100
     */
    public static native void avcodeccontext_set_scenechange_threshold(
        long avctx, int scenechange_threshold);

    /**
     * Set the size of the video.
     *
     * @param avctx pointer to AVCodecContext
     * @param width video width
     * @param height video height
     */
    public static native void avcodeccontext_set_size(long avctx, int width,
        int height);

    /**
     * Set the number of thread.
     *
     * @param avctx the <tt>AVCodecContext</tt> to set the number of thread of
     * @param thread_count number of thread to set
     */
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

    /**
     * Get BGR32 pixel format.
     *
     * @return BGR32 pixel format
     */
    private static native int PIX_FMT_BGR32();

    /**
     * Get BGR32_1 pixel format.
     *
     * @return BGR32_1 pixel format
     */
    private static native int PIX_FMT_BGR32_1();

    /**
     * Get RGB24 pixel format.
     *
     * @return RGB24 pixel format
     */
    private static native int PIX_FMT_RGB24();

    /**
     * Get RGB32 pixel format.
     *
     * @return RGB32 pixel format
     */
    private static native int PIX_FMT_RGB32();

    /**
     * Get RGB32_1 pixel format.
     *
     * @return RGB32_1 pixel format
     */
    private static native int PIX_FMT_RGB32_1();

    /**
     * Free an SwsContext.
     *
     * @param context SwsContext native pointer
     */
    public static native void sws_freeContext(long context);

    /**
     * Get a SwsContext pointer.
     *
     * @param context SwsContext
     * @param srcW width of source image
     * @param srcH height of source image
     * @param srcFormat  image format
     * @param dstW width of destination image
     * @param dstH height destination image
     * @param dstFormat destination format
     * @param flags flags
     * @return cached SwsContext pointer
     */
    public static native long sws_getCachedContext(
        long context,
        int srcW, int srcH, int srcFormat,
        int dstW, int dstH, int dstFormat,
        int flags);

    /**
     * Scale an image.
     *
     * @param context SwsContext native pointer
     * @param src source image (native pointer)
     * @param srcSliceY slice Y of source image
     * @param srcSliceH slice H of source image
     * @param dst destination image (java type)
     * @param dstFormat destination format
     * @param dstW width of destination image
     * @param dstH height destination image
     * @return 0 if success, -1 otherwise
     */
    public static native int sws_scale(
        long context,
        long src, int srcSliceY, int srcSliceH,
        Object dst, int dstFormat, int dstW, int dstH);

    /**
     * Scale image an image.
     *
     * @param context SwsContext native pointer
     * @param src source image (java type)
     * @param srcFormat image format
     * @param srcW width of source image
     * @param srcH height of source image
     * @param srcSliceY slice Y of source image
     * @param srcSliceH slice H of source image
     * @param dst destination image (java type)
     * @param dstFormat destination format
     * @param dstW width of destination image
     * @param dstH height destination image
     * @return 0 if success, -1 otherwise
     */
    public static native int sws_scale(
        long context,
        Object src, int srcFormat, int srcW, int srcH,
        int srcSliceY, int srcSliceH,
        Object dst, int dstFormat, int dstW, int dstH);

    /**
     * Register all filter from libavfilter.
     */
    public static native void avfilter_register_all();

    /**
     * Allocate a AVFilterGraph.
     *
     * @return pointer to graph
     */
    public static native long avfilter_alloc_filtergraph();

    /**
     * Free a AVFilterGraph.
     *
     * @param ptr native pointer
     */
    public static native void avfilter_free_filtergraph(long ptr);

    /**
     * Allocate a AVInputStream.
     *
     * @return native pointer or 0 if failure
     */
    public static native long avfilter_alloc_inputstream();

    /**
     * Free a AVInputStream.
     *
     * @param ptr native pointer
     */
    public static native void avfilter_free_inputstream(long ptr);

    /**
     * Allocate a AVOutputStream.
     *
     * @return native pointer or 0 if failure
     */
    public static native long avfilter_alloc_outputstream();

    /**
     * Free a AVOutputStream.
     *
     * @param ptr native pointer
     */
    public static native void avfilter_free_outputstream(long ptr);

    /**
     * Configure the filters.
     *
     * @param filters filters list
     * @param avinputstream AVInputStream pointer
     * @param pix_fmt pixel format
     * @param width width of the video
     * @param height height of the video
     * @param graph AVFilterGraph pointer
     * @return 0 if success, -1 otherwise
     */
    public static native int avfilter_configure_filters(String filters,
            long avinputstream, int pix_fmt, int width, int height, long graph);

    /**
     * Add the <tt>picture</tt> to the <tt>inputstream</tt> to be filtered.
     *
     * @param avinputstream AVInputStream pointer
     * @param avframe AVFrame pointer
     */
    public static native void av_vsrc_buffer_add_frame(long avinputstream,
            long avframe);

    /**
     * Get the filtered video frame.
     *
     * @param avinputstream AVInputStream pointer
     * @param avframe AVFrame pointer
     * @return 0 if success, -1 otherwise
     */
    public static native int av_get_filtered_video_frame(long avinputstream,
            long avframe);

    static
    {
        System.loadLibrary("ffmpeg");

        av_register_all();
        avfilter_register_all();
        avcodec_init();

        PIX_FMT_BGR32 = PIX_FMT_BGR32();
        PIX_FMT_BGR32_1 = PIX_FMT_BGR32_1();
        PIX_FMT_RGB24 = PIX_FMT_RGB24();
        PIX_FMT_RGB32 = PIX_FMT_RGB32();
        PIX_FMT_RGB32_1 = PIX_FMT_RGB32_1();
    }
}
