/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_quicktime_QTSampleBuffer.h"

#import <Foundation/NSAutoreleasePool.h>
#import <QTKit/QTFormatDescription.h>
#import <QTKit/QTSampleBuffer.h>

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTSampleBuffer_bytesForAllSamples
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    QTSampleBuffer *sampleBuffer;
    NSAutoreleasePool *autoreleasePool;
    NSUInteger lengthForAllSamples;
    jbyteArray jBytesForAllSamples;

    sampleBuffer = (QTSampleBuffer *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    lengthForAllSamples = [sampleBuffer lengthForAllSamples];
    if (lengthForAllSamples)
    {
        jBytesForAllSamples
            = (*jniEnv)->NewByteArray(jniEnv, lengthForAllSamples);
        if (jBytesForAllSamples)
        {
            jbyte *bytesForAllSamples = [sampleBuffer bytesForAllSamples];

            (*jniEnv)
                ->SetByteArrayRegion(
                    jniEnv,
                    jBytesForAllSamples,
                    0,
                    lengthForAllSamples,
                    bytesForAllSamples);
        }
    }
    else
        jBytesForAllSamples = NULL;

    [autoreleasePool release];
    return jBytesForAllSamples;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTSampleBuffer_formatDescription
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    QTSampleBuffer *sampleBuffer;
    NSAutoreleasePool *autoreleasePool;
    QTFormatDescription *formatDescription;

    sampleBuffer = (QTSampleBuffer *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    formatDescription = [sampleBuffer formatDescription];
    if (formatDescription)
        [formatDescription retain];

    [autoreleasePool release];
    return (jlong) formatDescription;
}
