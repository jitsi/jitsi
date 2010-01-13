#include "net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer.h"

#import <CoreVideo/CVPixelBuffer.h>

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getBytes
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    CVPixelBufferRef pixelBuffer;
    size_t byteCount;
    jbyteArray bytes;

    pixelBuffer = (CVPixelBufferRef) ptr;

    byteCount
        = CVPixelBufferGetBytesPerRow(pixelBuffer)
            * CVPixelBufferGetHeight(pixelBuffer);
    bytes = (*jniEnv)->NewByteArray(jniEnv, byteCount);
    if (!bytes)
        return NULL;

    if (kCVReturnSuccess == CVPixelBufferLockBaseAddress(pixelBuffer, 0))
    {
        jbyte *cBytes;

        cBytes = CVPixelBufferGetBaseAddress(pixelBuffer);
        (*jniEnv)->SetByteArrayRegion(jniEnv, bytes, 0, byteCount, cBytes);
        CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    }
    return bytes;
}
