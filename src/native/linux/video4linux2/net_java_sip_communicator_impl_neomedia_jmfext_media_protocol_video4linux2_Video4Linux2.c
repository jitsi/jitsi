/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2.h"

#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/select.h>
#include <unistd.h>

#include <linux/videodev2.h>

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_close
    (JNIEnv *jniEnv, jclass clazz, jint fd)
{
    return close(fd);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_free
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    free((void *) ptr);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_ioctl
    (JNIEnv *jniEnv, jclass clazz, jint fd, jint request, jlong argp)
{
    return ioctl(fd, request, (void *) argp);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_memcpy
    (JNIEnv *jniEnv, jclass clazz, jlong dest, jlong src, jint n)
{
    return (jlong) memcpy((void *) dest, (const void *) src, n);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_mmap
    (JNIEnv *jniEnv, jclass clazz, jlong start, jint length, jint prot,
     jint flags, jint fd, jlong offset)
{
    return (jlong) mmap((void *) start, length, prot, flags, fd, offset);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_munmap
    (JNIEnv *jniEnv, jclass clazz, jlong start, jint length)
{
    return munmap((void *) start, length);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_open
    (JNIEnv *jniEnv, jclass clazz, jstring deviceName, jint flags)
{
    const char *deviceNameChars;
    jint fd;

    deviceNameChars
        = (const char *) (*jniEnv)->GetStringUTFChars(jniEnv, deviceName, NULL);
    if (deviceNameChars)
    {
        fd = open(deviceNameChars, flags);
        (*jniEnv)->ReleaseStringUTFChars(jniEnv, deviceName, deviceNameChars);
    }
    else
        fd = -1;
    return fd;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1alloc
    (JNIEnv *jniEnv, jclass clazz, jint type)
{
    struct v4l2_buffer *v4l2_buffer;

    v4l2_buffer = malloc(sizeof(struct v4l2_buffer));
    if (v4l2_buffer)
        v4l2_buffer->type = type;
    return (jlong) v4l2_buffer;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1getBytesused
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_buffer)
{
    return ((struct v4l2_buffer *) v4l2_buffer)->bytesused;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1getIndex
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_buffer)
{
    return ((struct v4l2_buffer *) v4l2_buffer)->index;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1getLength
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_buffer)
{
    return ((struct v4l2_buffer *) v4l2_buffer)->length;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1getMOffset
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_buffer)
{
    return ((struct v4l2_buffer *) v4l2_buffer)->m.offset;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1setIndex
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_buffer, jint index)
{
    ((struct v4l2_buffer *) v4l2_buffer)->index = index;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buffer_1setMemory
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_buffer, jint memory)
{
    ((struct v4l2_buffer *) v4l2_buffer)->memory = memory;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1buf_1type_1alloc
    (JNIEnv *jniEnv, jclass clazz, jint type)
{
    enum v4l2_buf_type *v4l2_buf_type;

    v4l2_buf_type = malloc(sizeof(enum v4l2_buf_type));
    if (v4l2_buf_type)
        (*v4l2_buf_type) = type;
    return (jlong) v4l2_buf_type;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1capability_1alloc
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) malloc(sizeof(struct v4l2_capability));
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1capability_1getCapabilities
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_capability)
{
    return ((struct v4l2_capability *) v4l2_capability)->capabilities;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1capability_1getCard
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_capability)
{
    return
        (*jniEnv)->NewStringUTF(
                jniEnv,
                (const char *)
                    (((struct v4l2_capability *) v4l2_capability)->card));
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1format_1alloc
    (JNIEnv *jniEnv, jclass clazz, jint type)
{
    struct v4l2_format *v4l2_format;

    v4l2_format = malloc(sizeof(struct v4l2_format));
    if (v4l2_format)
    {
        v4l2_format->type = type;
        if (V4L2_BUF_TYPE_VIDEO_CAPTURE == type)
            v4l2_format->fmt.pix.priv = 0;
    }
    return (jlong) v4l2_format;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1format_1getFmtPix
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_format)
{
    return (jlong) &(((struct v4l2_format *) v4l2_format)->fmt.pix);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1getHeight
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_pix_format)
{
    return ((struct v4l2_pix_format *) v4l2_pix_format)->height;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1getPixelformat
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_pix_format)
{
    return ((struct v4l2_pix_format *) v4l2_pix_format)->pixelformat;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1getWidth
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_pix_format)
{
    return ((struct v4l2_pix_format *) v4l2_pix_format)->width;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1setBytesperline
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_pix_format, jint bytesperline)
{
    ((struct v4l2_pix_format *) v4l2_pix_format)->bytesperline = bytesperline;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1setField
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_pix_format, jint field)
{
    ((struct v4l2_pix_format *) v4l2_pix_format)->field = field;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1setPixelformat
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_pix_format, jint pixelformat)
{
    ((struct v4l2_pix_format *) v4l2_pix_format)->pixelformat = pixelformat;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1pix_1format_1setWidthAndHeight
    (JNIEnv *jniEnv, jclass clazz,
     jlong v4l2_pix_format,
     jint width, jint height)
{
    struct v4l2_pix_format *ptr;

    ptr = (struct v4l2_pix_format *) v4l2_pix_format;
    ptr->width = width;
    ptr->height = height;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1requestbuffers_1alloc
    (JNIEnv *jniEnv, jclass clazz, jint type)
{
    struct v4l2_requestbuffers *v4l2_requestbuffers;

    v4l2_requestbuffers = malloc(sizeof(struct v4l2_requestbuffers));
    if (v4l2_requestbuffers)
        v4l2_requestbuffers->type = type;
    return (jlong) v4l2_requestbuffers;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1requestbuffers_1getCount
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_requestbuffers)
{
    return ((struct v4l2_requestbuffers *) v4l2_requestbuffers)->count;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1requestbuffers_1setCount
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_requestbuffers, jint count)
{
    ((struct v4l2_requestbuffers *) v4l2_requestbuffers)->count = count;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1requestbuffers_1setMemory
    (JNIEnv *jniEnv, jclass clazz, jlong v4l2_requestbuffers, jint memory)
{
    ((struct v4l2_requestbuffers *) v4l2_requestbuffers)->memory = memory;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1streamparm_1alloc
  (JNIEnv *jniEnv, jclass clazz, jint type)
{
    struct v4l2_streamparm* v4l2_streamparm = (struct v4l2_streamparm *)malloc(sizeof(struct v4l2_streamparm));

    if(v4l2_streamparm)
      v4l2_streamparm->type = type;

    return (jlong)v4l2_streamparm;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_v4l2_1streamparm_1setFps
  (JNIEnv *jniEnv, jclass clazz, jlong v4l2_streamparm, jint fps)
{
    ((struct v4l2_streamparm*)v4l2_streamparm)->parm.capture.timeperframe.numerator =  1;
    ((struct v4l2_streamparm*)v4l2_streamparm)->parm.capture.timeperframe.denominator = fps;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1DQBUF
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_DQBUF;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1G_1FMT
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_G_FMT;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1QBUF
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_QBUF;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1QUERYBUF
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_QUERYBUF;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1QUERYCAP
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_QUERYCAP;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1REQBUFS
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_REQBUFS;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1S_1FMT
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_S_FMT;
}

JNIEXPORT jint JNICALL Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1S_1PARM
  (JNIEnv *jniEnv, jclass clazz)
{
  return VIDIOC_S_PARM;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1STREAMOFF
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_STREAMOFF;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_protocol_video4linux2_Video4Linux2_VIDIOC_1STREAMON
    (JNIEnv *jniEnv, jclass clazz)
{
    return VIDIOC_STREAMON;
}
