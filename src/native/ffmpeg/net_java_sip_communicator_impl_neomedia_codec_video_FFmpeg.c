/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg.h"

#include <string.h>

#include <libavutil/avutil.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_av_1free (
        JNIEnv *jniEnv, jclass clazz, jlong ptr) {
    av_free ((void *) ptr);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_av_1malloc (
        JNIEnv *jniEnv, jclass clazz, jint size) {
    return (jlong) av_malloc ((unsigned int) size);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_av_1register_1all (
        JNIEnv *jniEnv, jclass clazz) {
    av_register_all ();
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1alloc_1context (
        JNIEnv *jniEnv, jclass clazz) {
    return (jlong) avcodec_alloc_context ();
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1alloc_1frame (
        JNIEnv *jniEnv, jclass clazz) {
    return (jlong) avcodec_alloc_frame ();
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1close (
        JNIEnv *jniEnv, jclass clazz, jlong avctx) {
    return (jint) avcodec_close ((AVCodecContext *) avctx);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1decode_1video (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jlong frame,
        jbooleanArray got_picture, jbyteArray buf, jint buf_size) {
    jint ret;
    int n_got_picture;

    if (buf) {
        jbyte *buf_ptr = (*jniEnv)->GetByteArrayElements (jniEnv, buf, NULL);

        if (buf_ptr) {
            AVPacket avpkt;

            av_init_packet(&avpkt);
            avpkt.data = (uint8_t *) buf_ptr;
            avpkt.size = (int) buf_size;

            ret
                = avcodec_decode_video2(
                    (AVCodecContext *) avctx,
                    (AVFrame *) frame,
                    &n_got_picture,
                    &avpkt);

            (*jniEnv)->ReleaseByteArrayElements (jniEnv, buf, buf_ptr, 0);

            if (got_picture) {
                jboolean j_got_picture = n_got_picture ? JNI_TRUE : JNI_FALSE;

                (*jniEnv)->SetBooleanArrayRegion (jniEnv, got_picture, 0, 1,
                    &j_got_picture);
            }
        } else
            ret = -1;
    } else
        ret = -1;
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1encode_1video (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jbyteArray buf,
        jint buf_size, jlong frame) {
    jint ret;

    if (buf) {
        jbyte *buf_ptr = (*jniEnv)->GetByteArrayElements (jniEnv, buf, NULL);

        if (buf_ptr) {
            ret = (jint)
                avcodec_encode_video ((AVCodecContext *) avctx,
                    (uint8_t *) buf_ptr, (int) buf_size,
                    (const AVFrame *) frame);
            (*jniEnv)->ReleaseByteArrayElements (jniEnv, buf, buf_ptr, 0);
        } else
            ret = -1;
    } else
        ret = -1;
    return ret;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1find_1decoder (
        JNIEnv *jniEnv, jclass clazz, jint id) {
    return (jlong) avcodec_find_decoder ((enum CodecID) id);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1find_1encoder (
        JNIEnv *jniEnv, jclass clazz, jint id) {
    return (jlong) avcodec_find_encoder ((enum CodecID) id);
}

/* Empty log function to skip all logs coming out onto the terminal. */
static void log_callback_help(void* ptr, int level, const char* fmt, va_list vl)
{
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1init (
        JNIEnv *jniEnv, jclass clazz) {
    avcodec_init ();
    av_log_set_callback(log_callback_help);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodec_1open (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jlong codec) {
    return (jint) avcodec_open ((AVCodecContext *) avctx, (AVCodec *) codec);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1add_1flags (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint flags) {
    ((AVCodecContext *) avctx)->flags |= (int) flags;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1add_1partitions (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint partitions) {
    ((AVCodecContext *) avctx)->partitions |= (int) partitions;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1get_1height (
        JNIEnv *jniEnv, jclass clazz, jlong avctx) {
    return (jint) (((AVCodecContext *) avctx)->height);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1get_1pix_1fmt (
        JNIEnv *jniEnv, jclass clazz, jlong avctx) {
    return (jint) (((AVCodecContext *) avctx)->pix_fmt);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1get_1width (
        JNIEnv *jniEnv, jclass clazz, jlong avctx) {
    return (jint) (((AVCodecContext *) avctx)->width);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1b_1frame_1strategy (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint b_frame_strategy) {
    ((AVCodecContext *) avctx)->b_frame_strategy = (int) b_frame_strategy;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1bit_1rate (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint bit_rate) {
    ((AVCodecContext *) avctx)->bit_rate = (int) bit_rate;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1bit_1rate_1tolerance (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint bit_rate_tolerance) {
    ((AVCodecContext *) avctx)->bit_rate_tolerance = (int) bit_rate_tolerance;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1chromaoffset (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint chromaoffset) {
    ((AVCodecContext *) avctx)->chromaoffset = (int) chromaoffset;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1crf (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jfloat crf) {
    ((AVCodecContext *) avctx)->crf = (float) crf;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1deblockbeta (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint deblockbeta) {
    ((AVCodecContext *) avctx)->deblockbeta = (int) deblockbeta;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1gop_1size (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint gop_size) {
    ((AVCodecContext *) avctx)->gop_size = (int) gop_size;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1i_1quant_1factor (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jfloat i_quant_factor) {
    ((AVCodecContext *) avctx)->i_quant_factor = (float) i_quant_factor;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1max_1b_1frames (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint max_b_frames) {
    ((AVCodecContext *) avctx)->max_b_frames = (int) max_b_frames;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1mb_1decision (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint mb_decision) {
    ((AVCodecContext *) avctx)->mb_decision = (int) mb_decision;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1me_1cmp (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint me_cmp) {
    ((AVCodecContext *) avctx)->me_cmp = (int) me_cmp;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1me_1method (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint me_method) {
    ((AVCodecContext *) avctx)->me_method = (int) me_method;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1me_1range (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint me_range) {
    ((AVCodecContext *) avctx)->me_range = (int) me_range;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1me_1subpel_1quality (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint me_subpel_quality) {
    ((AVCodecContext *) avctx)->me_subpel_quality = (int) me_subpel_quality;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1pix_1fmt (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint pix_fmt) {
    ((AVCodecContext *) avctx)->pix_fmt = (int) pix_fmt;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1qcompress (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jfloat qcompress) {
    ((AVCodecContext *) avctx)->qcompress = (float) qcompress;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1quantizer (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint qmin, jint qmax,
        jint max_qdiff) {
    AVCodecContext *n_avctx = (AVCodecContext *) avctx;

    n_avctx->qmin = (int) qmin;
    n_avctx->qmax = (int) qmax;
    n_avctx->max_qdiff = (int) max_qdiff;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1rc_1buffer_1size (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint rc_buffer_size) {
    ((AVCodecContext *) avctx)->rc_buffer_size = (int) rc_buffer_size;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1rc_1eq (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jstring rc_eq) {
    char *n_rc_eq;

    if (rc_eq) {
        const char * j_rc_eq =
            (*jniEnv)->GetStringUTFChars (jniEnv, rc_eq, NULL);

        if (j_rc_eq) {
            n_rc_eq = strdup (j_rc_eq);
            (*jniEnv)->ReleaseStringUTFChars (jniEnv, rc_eq, j_rc_eq);
        } else
            n_rc_eq = NULL;
    } else
        n_rc_eq = NULL;
    ((AVCodecContext *) avctx)->rc_eq = n_rc_eq;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1rc_1max_1rate (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint rc_max_rate) {
    ((AVCodecContext *) avctx)->rc_max_rate = (int) rc_max_rate;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1refs (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint refs) {
    ((AVCodecContext *) avctx)->refs = (int) refs;
}
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1rtp_1payload_1size (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint rtp_payload_size) {
    ((AVCodecContext *) avctx)->rtp_payload_size = (int) rtp_payload_size;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1sample_1aspect_1ratio (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint num, jint den) {
    AVRational *sample_aspect_ratio =
        &(((AVCodecContext *) avctx)->sample_aspect_ratio);

    sample_aspect_ratio->num = (int) num;
    sample_aspect_ratio->den = (int) den;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1scenechange_1threshold (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint scenechange_threshold) {
    ((AVCodecContext *) avctx)->scenechange_threshold =
        (int) scenechange_threshold;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1size (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint width, jint height) {
    AVCodecContext *n_avctx = (AVCodecContext *) avctx;

    n_avctx->width = (int) width;
    n_avctx->height = (int) height;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1thread_1count (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint thread_count) {
    ((AVCodecContext *) avctx)->thread_count = (int) thread_count;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1ticks_1per_1frame (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint ticks_per_frame) {
    ((AVCodecContext *) avctx)->ticks_per_frame = (int) ticks_per_frame;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1time_1base (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint num, jint den) {
    AVRational *time_base = &(((AVCodecContext *) avctx)->time_base);

    time_base->num = (int) num;
    time_base->den = (int) den;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1trellis (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint trellis) {
    ((AVCodecContext *) avctx)->trellis = (int) trellis;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avcodeccontext_1set_1workaround_1bugs (
        JNIEnv *jniEnv, jclass clazz, jlong avctx, jint workaround_bugs) {
    ((AVCodecContext *) avctx)->workaround_bugs = (int) workaround_bugs;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avframe_1get_1pts (
        JNIEnv *jniEnv, jclass clazz, jlong frame) {
    return (jlong) (((AVFrame *) frame)->pts);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avframe_1set_1data (
        JNIEnv *jniEnv, jclass clazz, jlong frame, jlong data0, jlong offset1,
        jlong offset2) {
    AVFrame *n_frame = (AVFrame *) frame;

    n_frame->data[0] = (uint8_t *) data0;
    n_frame->data[1] = n_frame->data[0] + offset1;
    n_frame->data[2] = n_frame->data[1] + offset2;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avframe_1set_1key_1frame (
        JNIEnv *jniEnv, jclass clazz, jlong frame, jboolean key_frame) {
    ((AVFrame *) frame)->key_frame = (JNI_TRUE == key_frame) ? 1 : 0;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avframe_1set_1linesize (
        JNIEnv *jniEnv, jclass clazz, jlong frame, jint linesize0,
        jint linesize1, jint linesize2) {
    AVFrame *n_frame = (AVFrame *) frame;

    n_frame->linesize[0] = (int) linesize0;
    n_frame->linesize[1] = (int) linesize1;
    n_frame->linesize[2] = (int) linesize2;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avpicture_1fill (
        JNIEnv *jniEnv, jclass clazz, jlong picture, jlong ptr, jint pix_fmt,
        jint width, jint height) {
    return (jint)
        avpicture_fill ((AVPicture *) picture, (uint8_t *) ptr, (int) pix_fmt,
            (int) width, (int) height);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avpicture_1get_1data0 (
        JNIEnv *jniEnv, jclass clazz, jlong picture) {
    return (jlong) (((AVPicture *) picture)->data[0]);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_avpicture_1get_1size (
        JNIEnv *jniEnv, jclass clazz, jint pix_fmt, jint width, jint height) {
    return (jint) avpicture_get_size ((int) pix_fmt, (int) width, (int) height);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_memcpy___3IIIJ (
        JNIEnv *jniEnv, jclass clazz, jintArray dst, jint dst_offset,
        jint dst_length, jlong src) {
    (*jniEnv)
        ->SetIntArrayRegion (jniEnv, dst, dst_offset, dst_length, (jint *) src);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_memcpy__J_3BII (
        JNIEnv *jniEnv, jclass clazz, jlong dst, jbyteArray src,
        jint src_offset, jint src_length) {
    (*jniEnv)->GetByteArrayRegion (jniEnv, src, src_offset, src_length,
            (jbyte *) dst);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_PIX_1FMT_1BGR32 (
        JNIEnv *env, jclass clazz) {
    return PIX_FMT_BGR32;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_PIX_1FMT_1BGR32_11 (
        JNIEnv *env, jclass clazz) {
    return PIX_FMT_BGR32_1;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_PIX_1FMT_1RGB24 (
        JNIEnv *jniEnv, jclass clazz) {
    uint32_t test = 1;
    int little_endian = *((uint8_t*)&test);

    return little_endian ? PIX_FMT_BGR24 : PIX_FMT_RGB24;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_PIX_1FMT_1RGB32 (
        JNIEnv *env, jclass clazz) {
    return PIX_FMT_RGB32;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_PIX_1FMT_1RGB32_11 (
        JNIEnv *env, jclass clazz) {
    return PIX_FMT_RGB32_1;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_PIX_1FMT_1YUV420P (
        JNIEnv *env, jclass clazz) {
    return PIX_FMT_YUV420P;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_sws_1freeContext (
        JNIEnv *jniEnv, jclass clazz, jlong context) {
    sws_freeContext ((struct SwsContext *) context);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_sws_1getCachedContext (
        JNIEnv *jniEnv, jclass clazz, jlong context, jint srcW, jint srcH,
        jint srcFormat, jint dstW, jint dstH, jint dstFormat, jint flags) {
    return
        (jlong)
            sws_getCachedContext (
                (struct SwsContext *) context,
                (int) srcW, (int) srcH, (enum PixelFormat) srcFormat,
                (int) dstW, (int) dstH, (enum PixelFormat) dstFormat,
                (int) flags,
                NULL, NULL, NULL);
}

JNIEXPORT jint JNICALL Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_sws_1scale__JJIILjava_lang_Object_2III (
        JNIEnv *jniEnv, jclass clazz, jlong context, jlong src, jint srcSliceY,
        jint srcSliceH, jobject dst, jint dstFormat, jint dstW, jint dstH) {
    AVPicture *srcPicture;
    uint8_t *dstPtr;
    int ret;

    srcPicture = (AVPicture *) src;
    dstPtr = (*jniEnv)->GetPrimitiveArrayCritical (jniEnv, dst, NULL);
    if (dstPtr) {
        AVPicture dstPicture;

        /* Turn the bytes into an AVPicture. */
        avpicture_fill (
            &dstPicture, dstPtr, (int) dstFormat, (int) dstW, (int) dstH);
        ret
            = sws_scale (
                (struct SwsContext *) context,
                (const uint8_t * const *) srcPicture->data, (int *) srcPicture->linesize,
                (int) srcSliceY, (int) srcSliceH,
                (uint8_t **) dstPicture.data,
                (int *) dstPicture.linesize);
        (*jniEnv)->ReleasePrimitiveArrayCritical (jniEnv, dst, dstPtr, 0);
    }
    else
        ret = -1;
    return (jint) ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_sws_1scale__JLjava_lang_Object_2IIIIILjava_lang_Object_2III (
        JNIEnv *jniEnv, jclass class, jlong context, jobject src,
        jint srcFormat, jint srcW, jint srcH, jint srcSliceY, jint srcSliceH,
        jobject dst, jint dstFormat, jint dstW, jint dstH) {
    uint8_t *srcPtr;
    jint ret;

    srcPtr = (*jniEnv)->GetPrimitiveArrayCritical (jniEnv, src, NULL);
    if (srcPtr) {
        AVPicture srcPicture;

        avpicture_fill (
            &srcPicture, srcPtr, (int) srcFormat, (int) srcW, (int) srcH);
        ret
            = Java_net_java_sip_communicator_impl_neomedia_codec_video_FFmpeg_sws_1scale__JJIILjava_lang_Object_2III (
                jniEnv, class,
                context,
                (jlong) &srcPicture, srcSliceY, srcSliceH,
                dst, dstFormat, dstW, dstH);
        (*jniEnv)->ReleasePrimitiveArrayCritical (jniEnv, src, srcPtr, 0);
    }
    else
        ret = -1;
    return ret;
}
