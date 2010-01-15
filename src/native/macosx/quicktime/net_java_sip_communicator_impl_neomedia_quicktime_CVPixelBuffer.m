#include "net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer.h"

#import <CoreVideo/CVPixelBuffer.h>

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getBytes
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    CVPixelBufferRef pixelBuffer;
    size_t planeCount;
    size_t byteCount;
    jbyteArray bytes;

    pixelBuffer = (CVPixelBufferRef) ptr;

    planeCount = CVPixelBufferGetPlaneCount(pixelBuffer);
    if (planeCount)
    {
        size_t planeIndex;

        byteCount = 0;
        for (planeIndex = 0; planeIndex < planeCount; planeIndex++)
            byteCount
                += CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, planeIndex)
                    * CVPixelBufferGetHeightOfPlane(pixelBuffer, planeIndex);
    }
    else
        byteCount
            = CVPixelBufferGetBytesPerRow(pixelBuffer)
                * CVPixelBufferGetHeight(pixelBuffer);
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
