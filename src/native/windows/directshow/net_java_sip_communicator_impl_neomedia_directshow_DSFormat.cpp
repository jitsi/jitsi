
#include "net_java_sip_communicator_impl_neomedia_directshow_DSFormat.h"

#include <windows.h>
#include <dshow.h>
#include <wmcodecdsp.h>

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getRGB24PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_RGB24.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getRGB32PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_RGB32.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getARGBPixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_ARGB32.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getAYUVPixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_AYUV.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getYUY2PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_YUY2.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getUYVYPixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_UYVY.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getIMC1PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_IMC1.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getIMC2PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_IMC2.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getIMC3PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_IMC3.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getIMC4PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_IMC4.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getYV12PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_YV12.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getNV12PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_NV12.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getIF09PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_IF09.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getIYUVPixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_IYUV.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getY211PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_Y211.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getY411PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_Y411.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getY41PPixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_Y41P.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getYVU9PixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_YVU9.Data1;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSFormat_getYVYUPixelFormat
  (JNIEnv *, jclass)
{
    return MEDIASUBTYPE_YVYU.Data1;
}

