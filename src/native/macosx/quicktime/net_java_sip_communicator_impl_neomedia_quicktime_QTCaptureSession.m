#include "net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureSession.h"

#include "common.h"

#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSError.h>
#import <QTKit/QTCaptureInput.h>
#import <QTKit/QTCaptureOutput.h>
#import <QTKit/QTCaptureSession.h>

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureSession_addInput
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jlong inputPtr)
{
    QTCaptureSession *captureSession;
    QTCaptureInput *input;
    NSAutoreleasePool *autoreleasePool;
    BOOL ret;
    NSError *error;

    captureSession = (QTCaptureSession *) ptr;
    input = (QTCaptureInput *) inputPtr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    ret = [captureSession addInput:input error:&error];

    [autoreleasePool release];
    return (YES == ret) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureSession_addOutput
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jlong outputPtr)
{
    QTCaptureSession *captureSession;
    QTCaptureOutput *output;
    NSAutoreleasePool *autoreleasePool;
    BOOL ret;
    NSError *error;

    captureSession = (QTCaptureSession *) ptr;
    output = (QTCaptureOutput *) outputPtr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    ret = [captureSession addOutput:output error:&error];

    [autoreleasePool release];
    return (YES == ret) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureSession_allocAndInit
    (JNIEnv *jniEnv, jclass clazz)
{
    NSAutoreleasePool *autoreleasePool;
    QTCaptureSession *captureSession;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    captureSession = [[QTCaptureSession alloc] init];

    [autoreleasePool release];
    return (jlong) captureSession;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureSession_startRunning
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    NSObject_performSelector((id) ptr, @"startRunning");
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureSession_stopRunning
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    NSObject_performSelector((id) ptr, @"stopRunning");
}
