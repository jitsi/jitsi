/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer.h"

#import <CoreVideo/CVPixelBuffer.h>
#include <string.h>

static size_t
CVPixelBuffer_getByteCount(CVPixelBufferRef pixelBuffer, size_t planeCount)
{
    size_t byteCount;

    if (planeCount)
    {
        size_t planeIndex;

        byteCount = 0;
        for (planeIndex = 0; planeIndex < planeCount; planeIndex++)
        {
            byteCount
                += CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, planeIndex)
                    * CVPixelBufferGetHeightOfPlane(pixelBuffer, planeIndex);
        }
    }
    else
    {
        byteCount
            = CVPixelBufferGetBytesPerRow(pixelBuffer)
                * CVPixelBufferGetHeight(pixelBuffer);
    }
    return byteCount;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getByteCount
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    CVPixelBufferRef pixelBuffer;
    size_t planeCount;

    pixelBuffer = (CVPixelBufferRef) ptr;

    planeCount = CVPixelBufferGetPlaneCount(pixelBuffer);
    return (jint) CVPixelBuffer_getByteCount(pixelBuffer, planeCount);
}

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getBytes__J
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    CVPixelBufferRef pixelBuffer;
    size_t planeCount;
    size_t byteCount;
    jbyteArray bytes;

    pixelBuffer = (CVPixelBufferRef) ptr;

    planeCount = CVPixelBufferGetPlaneCount(pixelBuffer);
    byteCount = CVPixelBuffer_getByteCount(pixelBuffer, planeCount);
    bytes = (*jniEnv)->NewByteArray(jniEnv, byteCount);
    if (!bytes)
        return NULL;

    if (kCVReturnSuccess == CVPixelBufferLockBaseAddress(pixelBuffer, 0))
    {
        jbyte *cBytes;

        if (planeCount)
        {
            size_t byteOffset;
            size_t planeIndex;

            byteOffset = 0;
            for (planeIndex = 0; planeIndex < planeCount; planeIndex++)
            {
                cBytes
                    = CVPixelBufferGetBaseAddressOfPlane(
                        pixelBuffer,
                        planeIndex);
                byteCount
                    += CVPixelBufferGetBytesPerRowOfPlane(
                            pixelBuffer,
                            planeIndex)
                        * CVPixelBufferGetHeightOfPlane(
                                pixelBuffer,
                                planeIndex);
                (*jniEnv)
                    ->SetByteArrayRegion(
                        jniEnv,
                        bytes,
                        byteOffset,
                        byteCount,
                        cBytes);
                byteOffset += byteCount;
            }
        }
        else
        {
            cBytes = CVPixelBufferGetBaseAddress(pixelBuffer);
            (*jniEnv)->SetByteArrayRegion(jniEnv, bytes, 0, byteCount, cBytes);
        }
        CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    }
    return bytes;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getBytes__JJI
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jlong buf, jint bufLength)
{
    CVPixelBufferRef pixelBuffer;
    size_t byteCount;

    pixelBuffer = (CVPixelBufferRef) ptr;

    if (kCVReturnSuccess == CVPixelBufferLockBaseAddress(pixelBuffer, 0))
    {
        size_t planeCount;
        jbyte *cBytes;

        planeCount = CVPixelBufferGetPlaneCount(pixelBuffer);
        byteCount = CVPixelBuffer_getByteCount(pixelBuffer, planeCount);

        if (planeCount)
        {
            size_t byteOffset;
            size_t planeIndex;

            byteOffset = 0;
            for (planeIndex = 0; planeIndex < planeCount; planeIndex++)
            {
                cBytes
                    = CVPixelBufferGetBaseAddressOfPlane(
                        pixelBuffer,
                        planeIndex);
                byteCount
                    += CVPixelBufferGetBytesPerRowOfPlane(
                            pixelBuffer,
                            planeIndex)
                        * CVPixelBufferGetHeightOfPlane(
                                pixelBuffer,
                                planeIndex);
                memcpy((void *) buf, cBytes, byteCount);
                byteOffset += byteCount;
            }
            byteCount = byteOffset;
        }
        else
        {
            cBytes = CVPixelBufferGetBaseAddress(pixelBuffer);
            memcpy(buf, cBytes, byteCount);
        }
        CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    }
    else
        byteCount = 0;
    return byteCount;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getHeight
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    return (jint) CVPixelBufferGetHeight((CVPixelBufferRef) ptr);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getWidth
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    return (jint) CVPixelBufferGetWidth((CVPixelBufferRef) ptr);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_memcpy
    (JNIEnv *jniEnv, jclass clazz,
        jbyteArray dst, jint dstOffset, jint dstLength,
        jlong src)
{
    (*jniEnv)->SetByteArrayRegion(
            jniEnv,
            dst, dstOffset, dstLength,
            (jbyte *) src);
}
