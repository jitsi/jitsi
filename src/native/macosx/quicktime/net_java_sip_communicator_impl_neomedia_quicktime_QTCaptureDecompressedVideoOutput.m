#include "net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDecompressedVideoOutput.h"

#import <CoreVideo/CVImageBuffer.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSObject.h>
#import <QTKit/QTCaptureDecompressedVideoOutput.h>
#import <QTKit/QTCaptureConnection.h>
#import <QTKit/QTCaptureOutput.h>
#import <QTKit/QTSampleBuffer.h>

@interface QTCaptureDecompressedVideoOutputDelegate : NSObject
{
@private
    jobject delegate;
    JavaVM *vm;
}

- (void)captureOutput:(QTCaptureOutput *)captureOutput
        didOutputVideoFrame:(CVImageBufferRef *)videoFrame
        withSampleBuffer:(QTSampleBuffer *)sampleBuffer
        fromConnection:(QTCaptureConnection *)connection;
- (void)dealloc;
- (id)init;
- (void)setDelegate:(jobject)delegate inJNIEnv:(JNIEnv *)jniEnv;

@end

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDecompressedVideoOutput_allocAndInit
    (JNIEnv *jniEnv, jclass clazz)
{
    NSAutoreleasePool *autoreleasePool;
    QTCaptureDecompressedVideoOutput *captureDecompressedVideoOutput;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    captureDecompressedVideoOutput
        = [[QTCaptureDecompressedVideoOutput alloc] init];

    [autoreleasePool release];
    return (jlong) captureDecompressedVideoOutput;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDecompressedVideoOutput_pixelBufferAttributes
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    QTCaptureDecompressedVideoOutput *captureDecompressedVideoOutput;
    NSAutoreleasePool *autoreleasePool;
    NSDictionary *pixelBufferAttributes;

    captureDecompressedVideoOutput = (QTCaptureDecompressedVideoOutput *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    pixelBufferAttributes
        = [captureDecompressedVideoOutput pixelBufferAttributes];
    if (pixelBufferAttributes)
        [pixelBufferAttributes retain];

    [autoreleasePool release];
    return (jlong) pixelBufferAttributes;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDecompressedVideoOutput_setAutomaticallyDropsLateVideoFrames
    (JNIEnv *jniEnv, jclass clazz, jlong ptr,
        jboolean automaticallyDropsLateVideoFrames)
{
    QTCaptureDecompressedVideoOutput *captureDecompressedVideoOutput;
    NSAutoreleasePool *autoreleasePool;

    captureDecompressedVideoOutput = (QTCaptureDecompressedVideoOutput *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    if ([captureDecompressedVideoOutput
            respondsToSelector:@selector(setAutomaticallyDropsLateVideoFrames)])
        [captureDecompressedVideoOutput
            setAutomaticallyDropsLateVideoFrames:
                ((JNI_TRUE == automaticallyDropsLateVideoFrames) ? YES : NO)];

    [autoreleasePool release];
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDecompressedVideoOutput_setDelegate
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jobject delegate)
{
    QTCaptureDecompressedVideoOutput *captureDecompressedVideoOutput;
    NSAutoreleasePool *autoreleasePool;
    QTCaptureDecompressedVideoOutputDelegate *oDelegate;
    id oPrevDelegate;

    captureDecompressedVideoOutput = (QTCaptureDecompressedVideoOutput *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    if (delegate)
    {
        oDelegate = [[QTCaptureDecompressedVideoOutputDelegate alloc] init];
        [oDelegate setDelegate:delegate inJNIEnv:jniEnv];
    }
    else
        oDelegate = nil;
    oPrevDelegate = [captureDecompressedVideoOutput delegate];
    if (oDelegate != oPrevDelegate)
    {
        [captureDecompressedVideoOutput setDelegate:oDelegate];
        if (oPrevDelegate)
            [oPrevDelegate release];
    }

    [autoreleasePool release];
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDecompressedVideoOutput_setPixelBufferAttributes
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jlong pixelBufferAttributesPtr)
{
    QTCaptureDecompressedVideoOutput *captureDecompressedVideoOutput;
    NSDictionary *pixelBufferAttributes;
    NSAutoreleasePool *autoreleasePool;

    captureDecompressedVideoOutput = (QTCaptureDecompressedVideoOutput *) ptr;
    pixelBufferAttributes = (NSDictionary *) pixelBufferAttributesPtr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    [captureDecompressedVideoOutput
        setPixelBufferAttributes:pixelBufferAttributes];

    [autoreleasePool release];
}

@implementation QTCaptureDecompressedVideoOutputDelegate

- (void)captureOutput:(QTCaptureOutput *)captureOutput
        didOutputVideoFrame:(CVImageBufferRef *)videoFrame
        withSampleBuffer:(QTSampleBuffer *)sampleBuffer
        fromConnection:(QTCaptureConnection *)connection;
{
    jobject delegate;
    JavaVM *vm;
    JNIEnv *jniEnv;
    jclass delegateClass;

    delegate = self->delegate;
    if (!delegate)
        return;

    vm = self->vm;
    if (0 != (*vm)->AttachCurrentThreadAsDaemon(vm, &jniEnv, NULL))
        return;

    delegateClass = (*jniEnv)->GetObjectClass(jniEnv, delegate);
    if (delegateClass)
    {
        jmethodID didOutputVideoFrameWithSampleBufferMethodID;

        didOutputVideoFrameWithSampleBufferMethodID
            = (*jniEnv)
                ->GetMethodID(
                    jniEnv,
                    delegateClass,
                    "outputVideoFrameWithSampleBuffer",
                    "(JJ)V");
        if (didOutputVideoFrameWithSampleBufferMethodID)
            (*jniEnv)
                ->CallVoidMethod(
                    jniEnv,
                    delegate,
                    didOutputVideoFrameWithSampleBufferMethodID,
                    (jlong) videoFrame,
                    (jlong) sampleBuffer);
    }
    (*jniEnv)->ExceptionClear(jniEnv);
}

- (void)dealloc
{
    [self setDelegate:NULL inJNIEnv:NULL];
    [super dealloc];
}

- (id)init
{
    if ((self = [super init]))
    {
        self->delegate = NULL;
        self->vm = NULL;
    }
    return self;
}

- (void)setDelegate:(jobject) delegate inJNIEnv:(JNIEnv *)jniEnv
{
    if (self->delegate)
    {
        if (!jniEnv)
            (*(self->vm))->AttachCurrentThread(self->vm, &jniEnv, NULL);
        (*jniEnv)->DeleteGlobalRef(jniEnv, self->delegate);
        self->delegate = NULL;
        self->vm = NULL;
    }
    if (delegate)
    {
        delegate = (*jniEnv)->NewGlobalRef(jniEnv, delegate);
        if (delegate)
        {
            (*jniEnv)->GetJavaVM(jniEnv, &(self->vm));
            self->delegate = delegate;
        }
    }
}

@end
