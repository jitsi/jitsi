/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

/**
 * @author Lubomir Marinov
 */
public class FFMPEG
{
    public static final int CODEC_FLAG_LOOP_FILTER = 0x00000800;

    public static final int CODEC_ID_H264 = 28;

    public static final int FF_BUG_AUTODETECT = 1;

    public static final int FF_CMP_CHROMA = 256;

    public static final int FF_INPUT_BUFFER_PADDING_SIZE = 8;

    public static final int FF_MB_DECISION_SIMPLE = 0;

    public static final int PIX_FMT_RGBA;

    public static final int PIX_FMT_RGB24;

    public static final int PIX_FMT_YUV420P;

    public static final int X264_RC_ABR = 2;

    public static native int getRGBAFormat();

    public static native int getRGB24Format();

    public static native int getYUV420PFormat();

    public static native void av_free(long ptr);

    public static native long av_malloc(int size);

    public static native void av_register_all();

    public static native long avcodec_alloc_context();

    public static native long avcodec_alloc_frame();

    public static native int avcodec_close(long avctx);

    public static native int avcodec_decode_video(long avctx, long frame,
        boolean[] got_picture, byte[] buf, int buf_size);

    public static native int avcodec_encode_video(long avctx, byte[] buff,
        int buf_size, long frame);

    public static native long avcodec_find_decoder(int id);

    public static native long avcodec_find_encoder(int id);

    public static native void avcodec_init();

    public static native int avcodec_open(long avctx, long codec);

    public static native void avcodeccontext_add_flags(long avctx, int flags);

    public static native void avcodeccontext_add_partitions(long avctx,
        int partitions);

    public static native int avcodeccontext_get_height(long avctx);

    public static native int avcodeccontext_get_pix_fmt(long avctx);

    public static native int avcodeccontext_get_width(long avctx);

    public static native void avcodeccontext_set_bit_rate(long avctx,
        int bit_rate);

    public static native void avcodeccontext_set_bit_rate_tolerance(long avctx,
        int bit_rate_tolerance);

    public static native void avcodeccontext_set_crf(long avctx, float crf);

    public static native void avcodeccontext_set_gop_size(long avctx,
        int gop_size);

    public static native void avcodeccontext_set_i_quant_factor(long avctx,
        float i_quant_factor);

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

    public static native void avcodeccontext_set_sample_aspect_ratio(
        long avctx, int num, int den);

    public static native void avcodeccontext_set_scenechange_threshold(
        long avctx, int scenechange_threshold);

    public static native void avcodeccontext_set_size(long avctx, int width,
        int height);

    public static native void avcodeccontext_set_thread_count(long avctx,
        int thread_count);

    public static native void avcodeccontext_set_time_base(long avctx, int num,
        int den);

    public static native void avcodeccontext_set_workaround_bugs(long avctx,
        int workaround_bugs);

    public static native void avcodeccontext_set_max_b_frames(long avctx, 
        int max_b_frames);
    
    public static native void avcodeccontext_set_b_frame_strategy(long avctx, 
        int b_frame_strategy);

    public static native void avcodeccontext_set_trellis(long avctx, 
        int trellis);
    
    public static native void avcodeccontext_set_refs(long avctx, 
        int refs);
    
    public static native void avcodeccontext_set_chromaoffset(long avctx, 
        int chromaoffset);
    
    public static native void avcodeccontext_set_deblockbeta(long avctx, 
        int deblockbeta);
    
    public static native void avcodeccontext_set_ticks_per_frame(long avctx, 
        int ticks_per_frame);

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

    public static native int img_convert(long dst, int dst_pix_fmt, long src,
        int pix_fmt, int width, int height);
    
    /**
     * Convert image bytes (scale/format).
     * 
     * @param dst destination image. Its type must be an array (int[], byte[] 
     * or short[])
     * @param dst_pix_fmt destination format
     * @param src source image. Its type must be an array (int[], byte[] or short[])
     * @param width original width
     * @param height original height
     * @param newWidth new width
     * @param newHeight new height
     */
    public static native int img_convert(Object dst, int dst_pix_fmt, 
            Object src, int pix_fmt, int width, int height, int newWidth, 
            int newHeight);
    
    public static native void memcpy(int[] dst, int dst_offset, int dst_length,
        long src);

    public static native void memcpy(long dst, byte[] src, int src_offset,
        int src_length);

    static
    {
        System.loadLibrary("ffmpeg");

        av_register_all();
        avcodec_init();
        PIX_FMT_RGB24 = getRGB24Format();
        PIX_FMT_RGBA = getRGBAFormat();
        PIX_FMT_YUV420P = getYUV420PFormat();
    }
}
