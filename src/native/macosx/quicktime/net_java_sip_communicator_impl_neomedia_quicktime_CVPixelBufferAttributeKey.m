/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBufferAttributeKey.h"

#import <CoreVideo/CVPixelBuffer.h>

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBufferAttributeKey_kCVPixelBufferHeightKey
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) kCVPixelBufferHeightKey;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBufferAttributeKey_kCVPixelBufferPixelFormatTypeKey
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) kCVPixelBufferPixelFormatTypeKey;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBufferAttributeKey_kCVPixelBufferWidthKey
    (JNIEnv *jniEnv, jclass clazz)
{
    return (jlong) kCVPixelBufferWidthKey;
}
