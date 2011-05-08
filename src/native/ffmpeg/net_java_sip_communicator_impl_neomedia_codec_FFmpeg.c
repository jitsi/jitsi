/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_codec_FFmpeg.h"

#include <string.h>

#include <libavutil/avutil.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/vsrc_buffer.h>
#include <libswscale/swscale.h>

#define DEFINE_AVCODECCONTEXT_F_PROPERTY_SETTER(name, property) \
    JNIEXPORT void JNICALL \
    Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1##name \
        (JNIEnv *jniEnv, jclass clazz, jlong avctx, jfloat property) \
    { \
        ((AVCodecContext *) avctx)->property = (float) property; \
    }
#define DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(name, property) \
    JNIEXPORT void JNICALL \
    Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1##name \
        (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint property) \
    { \
        ((AVCodecContext *) avctx)->property = (int) property; \
    }

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_av_1free
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    av_free ((void *) ptr);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_av_1malloc
    (JNIEnv *jniEnv, jclass clazz, jint size)
{
    return (jlong) av_malloc ((unsigned int) size);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_av_1register_1all
    (JNIEnv *jniEnv, jclass clazz)
{
    av_register_all ();
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1alloc_1context
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) avcodec_alloc_context ();
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1alloc_1frame
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) avcodec_alloc_frame ();
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1close
    (JNIEnv *jniEnv, jclass clazz, jlong avctx)
{
    return (jint) avcodec_close ((AVCodecContext *) avctx);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1decode_1video__JJ_3Z_3BI
    (JNIEnv *jniEnv, jclass clazz,
    jlong avctx,
    jlong frame, jbooleanArray got_picture, jbyteArray buf, jint buf_size)
{
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
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1decode_1video__JJJI
    (JNIEnv *jniEnv, jclass clazz,
    jlong avctx, jlong avframe, jlong src, jint src_length)
{
    AVPacket avpkt;
    int got_picture = 0;
    int ret = -1;

    av_init_packet(&avpkt);
    avpkt.data = (uint8_t*)src;
    avpkt.size = (int)src_length;

    ret
        = avcodec_decode_video2(
                (AVCodecContext *) avctx,
                (AVFrame *)avframe, &got_picture, &avpkt);

    return got_picture ? ret : -1;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1encode_1audio
    (JNIEnv *jniEnv, jclass clazz,
    jlong avctx,
    jbyteArray buf, jint buf_offset, jint buf_size,
    jbyteArray samples, jint samples_offset)
{
    jint ret;

    if (buf) {
        jbyte *buf_ptr = (*jniEnv)->GetByteArrayElements (jniEnv, buf, NULL);

        if (buf_ptr) {
            jbyte *samples_ptr
                = (*jniEnv)->GetByteArrayElements (jniEnv, samples, NULL);

            if (samples_ptr) {
                ret = (jint) avcodec_encode_audio(
                        (AVCodecContext *) avctx,
                        (uint8_t *) (buf_ptr + buf_offset), (int) buf_size,
                        (const short *) (samples_ptr + samples_offset));
                (*jniEnv)->ReleaseByteArrayElements(
                        jniEnv,
                        samples, samples_ptr,
                        JNI_ABORT);
            } else
                ret = -1;
            (*jniEnv)->ReleaseByteArrayElements (jniEnv, buf, buf_ptr, 0);
        } else
            ret = -1;
    } else
        ret = -1;
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1encode_1video
    (JNIEnv *jniEnv, jclass clazz,
    jlong avctx, jbyteArray buf, jint buf_size, jlong frame)
{
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
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1find_1decoder
    (JNIEnv *jniEnv, jclass clazz, jint id)
{
    return (jlong) avcodec_find_decoder ((enum CodecID) id);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1find_1encoder
    (JNIEnv *jniEnv, jclass clazz, jint id)
{
    return (jlong) avcodec_find_encoder ((enum CodecID) id);
}

/**
 * Implements a log callback that does not log anything in order to prevent logs
 * from appearing on stdout and/or stderr.
 */
static void
null_log_callback(void* ptr, int level, const char* fmt, va_list vl)
{
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1init
    (JNIEnv *jniEnv, jclass clazz)
{
    avcodec_init ();
    av_log_set_callback(null_log_callback);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodec_1open
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jlong codec)
{
    return (jint) avcodec_open ((AVCodecContext *) avctx, (AVCodec *) codec);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1add_1flags
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint flags)
{
    ((AVCodecContext *) avctx)->flags |= (int) flags;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1add_1flags2
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint flags2)
{
    ((AVCodecContext *) avctx)->flags2 |= (int) flags2;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1add_1partitions
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint partitions)
{
    ((AVCodecContext *) avctx)->partitions |= (int) partitions;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1get_1frame_1size
    (JNIEnv *jniEnv, jclass clazz, jlong avctx)
{
    return (jint) (((AVCodecContext *) avctx)->frame_size);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1get_1height
    (JNIEnv *jniEnv, jclass clazz, jlong avctx)
{
    return (jint) (((AVCodecContext *) avctx)->height);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1get_1pix_1fmt
    (JNIEnv *jniEnv, jclass clazz, jlong avctx)
{
    return (jint) (((AVCodecContext *) avctx)->pix_fmt);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1get_1width
    (JNIEnv *jniEnv, jclass clazz, jlong avctx)
{
    return (jint) (((AVCodecContext *) avctx)->width);
}

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(b_1frame_1strategy, b_frame_strategy)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(bit_1rate, bit_rate)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(bit_1rate_1tolerance, bit_rate_tolerance)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(channels, channels)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(chromaoffset, chromaoffset)

DEFINE_AVCODECCONTEXT_F_PROPERTY_SETTER(crf, crf)

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(deblockbeta, deblockbeta)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(gop_1size, gop_size)

DEFINE_AVCODECCONTEXT_F_PROPERTY_SETTER(i_1quant_1factor, i_quant_factor)

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(keyint_1min, keyint_min)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(max_1b_1frames, max_b_frames)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(mb_1decision, mb_decision)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(me_1cmp, me_cmp)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(me_1method, me_method)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(me_1range, me_range)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(me_1subpel_1quality, me_subpel_quality)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(pix_1fmt, pix_fmt)

DEFINE_AVCODECCONTEXT_F_PROPERTY_SETTER(qcompress, qcompress)

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1quantizer
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint qmin, jint qmax,
        jint max_qdiff)
{
    AVCodecContext *n_avctx = (AVCodecContext *) avctx;

    n_avctx->qmin = (int) qmin;
    n_avctx->qmax = (int) qmax;
    n_avctx->max_qdiff = (int) max_qdiff;
}

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(rc_1buffer_1size, rc_buffer_size)

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1rc_1eq
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jstring rc_eq)
{
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

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(rc_1max_1rate, rc_max_rate)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(refs, refs)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(rtp_1payload_1size, rtp_payload_size)

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1sample_1aspect_1ratio
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint num, jint den)
{
    AVRational *sample_aspect_ratio =
        &(((AVCodecContext *) avctx)->sample_aspect_ratio);

    sample_aspect_ratio->num = (int) num;
    sample_aspect_ratio->den = (int) den;
}

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(sample_1rate, sample_rate)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(scenechange_1threshold, scenechange_threshold)

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1size
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint width, jint height)
{
    AVCodecContext *n_avctx = (AVCodecContext *) avctx;

    n_avctx->width = (int) width;
    n_avctx->height = (int) height;
}

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(thread_1count, thread_count)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(ticks_1per_1frame, ticks_per_frame)

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avcodeccontext_1set_1time_1base
    (JNIEnv *jniEnv, jclass clazz, jlong avctx, jint num, jint den)
{
    AVRational *time_base = &(((AVCodecContext *) avctx)->time_base);

    time_base->num = (int) num;
    time_base->den = (int) den;
}

DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(trellis, trellis)
DEFINE_AVCODECCONTEXT_I_PROPERTY_SETTER(workaround_1bugs, workaround_bugs)

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avframe_1get_1pts
    (JNIEnv *jniEnv, jclass clazz, jlong frame)
{
    return (jlong) (((AVFrame *) frame)->pts);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avframe_1set_1data
    (JNIEnv *jniEnv, jclass clazz, jlong frame, jlong data0, jlong offset1,
        jlong offset2)
{
    AVFrame *n_frame = (AVFrame *) frame;

    n_frame->data[0] = (uint8_t *) data0;
    n_frame->data[1] = n_frame->data[0] + offset1;
    n_frame->data[2] = n_frame->data[1] + offset2;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avframe_1set_1key_1frame
    (JNIEnv *jniEnv, jclass clazz, jlong frame, jboolean key_frame)
{
    ((AVFrame *) frame)->key_frame = (JNI_TRUE == key_frame) ? 1 : 0;
    ((AVFrame *) frame)->pict_type = (JNI_TRUE == key_frame) ? FF_I_TYPE : 0;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avframe_1set_1linesize
    (JNIEnv *jniEnv, jclass clazz, jlong frame, jint linesize0,
        jint linesize1, jint linesize2)
{
    AVFrame *n_frame = (AVFrame *) frame;

    n_frame->linesize[0] = (int) linesize0;
    n_frame->linesize[1] = (int) linesize1;
    n_frame->linesize[2] = (int) linesize2;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avpicture_1fill
    (JNIEnv *jniEnv, jclass clazz, jlong picture, jlong ptr, jint pix_fmt,
        jint width, jint height)
{
    return (jint)
        avpicture_fill ((AVPicture *) picture, (uint8_t *) ptr, (int) pix_fmt,
            (int) width, (int) height);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avpicture_1get_1data0
    (JNIEnv *jniEnv, jclass clazz, jlong picture)
{
    return (jlong) (((AVPicture *) picture)->data[0]);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avpicture_1get_1size
    (JNIEnv *jniEnv, jclass clazz, jint pix_fmt, jint width, jint height)
{
    return (jint) avpicture_get_size ((int) pix_fmt, (int) width, (int) height);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_memcpy___3IIIJ
    (JNIEnv *jniEnv, jclass clazz, jintArray dst, jint dst_offset,
        jint dst_length, jlong src)
{
    (*jniEnv)
        ->SetIntArrayRegion (jniEnv, dst, dst_offset, dst_length, (jint *) src);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_memcpy__J_3BII
    (JNIEnv *jniEnv, jclass clazz, jlong dst, jbyteArray src,
        jint src_offset, jint src_length)
{
    (*jniEnv)->GetByteArrayRegion (jniEnv, src, src_offset, src_length,
            (jbyte *) dst);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_PIX_1FMT_1BGR32
    (JNIEnv *env, jclass clazz)
{
    return PIX_FMT_BGR32;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_PIX_1FMT_1BGR32_11
    (JNIEnv *env, jclass clazz)
{
    return PIX_FMT_BGR32_1;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_PIX_1FMT_1RGB24
    (JNIEnv *jniEnv, jclass clazz)
{
    uint32_t test = 1;
    int little_endian = *((uint8_t*)&test);

    return little_endian ? PIX_FMT_BGR24 : PIX_FMT_RGB24;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_PIX_1FMT_1RGB32
    (JNIEnv *env, jclass clazz)
{
    return PIX_FMT_RGB32;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_PIX_1FMT_1RGB32_11
    (JNIEnv *env, jclass clazz)
{
    return PIX_FMT_RGB32_1;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_sws_1freeContext
    (JNIEnv *jniEnv, jclass clazz, jlong context)
{
    sws_freeContext ((struct SwsContext *) context);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_sws_1getCachedContext
    (JNIEnv *jniEnv, jclass clazz, jlong context, jint srcW, jint srcH,
        jint srcFormat, jint dstW, jint dstH, jint dstFormat, jint flags)
{
    return
        (jlong)
            sws_getCachedContext(
                (struct SwsContext *) context,
                (int) srcW, (int) srcH, (enum PixelFormat) srcFormat,
                (int) dstW, (int) dstH, (enum PixelFormat) dstFormat,
                (int) flags,
                NULL, NULL, NULL);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_sws_1scale__JJIILjava_lang_Object_2III
    (JNIEnv *jniEnv, jclass clazz, jlong context, jlong src, jint srcSliceY,
        jint srcSliceH, jobject dst, jint dstFormat, jint dstW, jint dstH)
{
    AVPicture *srcPicture;
    uint8_t *dstPtr;
    int ret;

    srcPicture = (AVPicture *) src;
    dstPtr = (*jniEnv)->GetPrimitiveArrayCritical (jniEnv, dst, NULL);
    if (dstPtr) {
        AVPicture dstPicture;

        /* Turn the bytes into an AVPicture. */
        avpicture_fill(
            &dstPicture, dstPtr, (int) dstFormat, (int) dstW, (int) dstH);
        ret
            = sws_scale(
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
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_sws_1scale__JLjava_lang_Object_2IIIIILjava_lang_Object_2III
    (JNIEnv *jniEnv, jclass class, jlong context, jobject src,
        jint srcFormat, jint srcW, jint srcH, jint srcSliceY, jint srcSliceH,
        jobject dst, jint dstFormat, jint dstW, jint dstH)
{
    uint8_t *srcPtr;
    jint ret;

    srcPtr = (*jniEnv)->GetPrimitiveArrayCritical (jniEnv, src, NULL);
    if (srcPtr) {
        AVPicture srcPicture;

        avpicture_fill(
            &srcPicture, srcPtr, (int) srcFormat, (int) srcW, (int) srcH);
        ret
            = Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_sws_1scale__JJIILjava_lang_Object_2III(
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

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1graph_1alloc
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) avfilter_graph_alloc();
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1graph_1config
    (JNIEnv *jniEnv, jclass clazz, jlong graph, jlong log_ctx)
{
    return
        (jint)
            avfilter_graph_config((AVFilterGraph *) graph, (AVClass *) log_ctx);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1graph_1free
    (JNIEnv *jniEnv, jclass clazz, jlong graph)
{
    AVFilterGraph *graph_ = (AVFilterGraph *) graph;

    avfilter_graph_free(&graph_);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1graph_1get_1filter
    (JNIEnv *jniEnv, jclass clazz, jlong graph, jstring name)
{
    const char *name_ = (*jniEnv)->GetStringUTFChars(jniEnv, name, NULL);
    AVFilterContext *filter;

    if (name_)
    {
        filter = avfilter_graph_get_filter((AVFilterGraph *) graph, name_);
        (*jniEnv)->ReleaseStringUTFChars(jniEnv, name, name_);
    }
    else
        filter = NULL;
    return (jlong) filter;
}

static int
ffsink_query_formats(AVFilterContext *ctx)
{
    int err;

    /* Make ffsink output in the format in which buffer inputs. */
    if (ctx->priv)
    {
        AVFilterContext *src = ctx->priv;
        const int pix_fmts[] = { src->outputs[0]->in_formats->formats[0], -1 };

        avfilter_set_common_formats(ctx, avfilter_make_format_list(pix_fmts));
        err = 0;
    }
    else
        err = avfilter_default_query_formats(ctx);
    return err;
}

static void
ffsink_uninit(AVFilterContext *ctx)
{
    /*
     * Do not let FFmpeg libavfilter erroneously free the buffer video source
     * thinking that it is the priv allocated to this ffsink video sink via
     * priv_size.
     */
    ctx->priv = NULL;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1graph_1parse
    (JNIEnv *jniEnv, jclass clazz,
    jlong graph, jstring filters, jlong inputs, jlong outputs, jlong log_ctx)
{
    const char *filters_ = (*jniEnv)->GetStringUTFChars(jniEnv, filters, NULL);
    int ret;

    if (filters_)
    {
        AVFilterGraph *graph_ = (AVFilterGraph *) graph;

        ret
            = avfilter_graph_parse(
                    graph_,
                    filters_,
                    (AVFilterInOut *) inputs, (AVFilterInOut *) outputs,
                    (AVClass *) log_ctx);

        /*
         * FIXME The implementation at the time of this writing presumes that
         * the first filter is buffer, the last filter is nullsink meant to be
         * ffsink and the ffsink is expected to output in the format in which
         * the buffer inputs.
         */
        if (0 == ret)
        {
            /* Turn nullsink into ffsink. */
            unsigned filterCount = graph_->filter_count;

            if (filterCount)
            {
                AVFilterContext *ffsink = (graph_->filters)[filterCount - 1];
                AVFilterContext *buffer = (graph_->filters)[0];

                /*
                 * Make sure query_format of ffsink outputs in the format in
                 * which buffer inputs. Otherwise, the output format may end up
                 * different on the C and Java sides.
                 */
                ffsink->filter->uninit = ffsink_uninit;
                ffsink->priv = buffer;
                ffsink->filter->query_formats = ffsink_query_formats;

                ffsink->input_pads->min_perms = AV_PERM_READ;
                ffsink->input_pads->start_frame = NULL;
            }
        }

        (*jniEnv)->ReleaseStringUTFChars(jniEnv, filters, filters_);
    }
    else
        ret = AVERROR(ENOMEM);
    return (jint) ret;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1register_1all
    (JNIEnv *jniEnv, jclass clazz)
{
    avfilter_register_all();
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_avfilter_1unref_1buffer
    (JNIEnv *jniEnv, jclass clazz, jlong ref)
{
    avfilter_unref_buffer((AVFilterBufferRef *) ref);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_codec_FFmpeg_get_1filtered_1video_1frame
    (JNIEnv *jniEnv, jclass clazz,
    jlong input, jlong buffer, jlong ffsink, jlong output)
{
    AVFilterContext *buffer_ = (AVFilterContext *) buffer;
    AVFilterBufferRef *ref = NULL;

    if (av_vsrc_buffer_add_frame(buffer_, (AVFrame *) input) == 0)
    {
        AVFilterContext *ffsink_ = (AVFilterContext *) ffsink;
        AVFilterLink *ffsinkLink = (ffsink_->inputs)[0];

        if (avfilter_request_frame(ffsinkLink) == 0)
        {
            ref = ffsinkLink->cur_buf;
            if (ref)
            {
                AVFrame *output_ = (AVFrame *) output;

                /*
                 * The data of cur_buf will be returned into output so it needs
                 * to exist at least while output needs it. So take ownership of
                 * cur_buf and the user of output will unref it when they are
                 * done with output.
                 */
                ffsinkLink->cur_buf = NULL;

                memcpy(output_->data, ref->data, sizeof(output_->data));
                memcpy(
                    output_->linesize,
                    ref->linesize,
                    sizeof(output_->linesize));
                output_->interlaced_frame = ref->video->interlaced;
                output_->top_field_first = ref->video->top_field_first;
            }
        }
    }
    return (jlong) ref;
}
