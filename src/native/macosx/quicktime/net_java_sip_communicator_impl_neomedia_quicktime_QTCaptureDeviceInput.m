#include "net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDeviceInput.h"

#import <Foundation/NSException.h>
#import <QTKit/QTCaptureDevice.h>
#import <QTKit/QTCaptureDeviceInput.h>

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTCaptureDeviceInput_deviceInputWithDevice
    (JNIEnv *jniEnv, jclass clazz, jlong devicePtr)
{
    QTCaptureDevice *device;
    NSAutoreleasePool *autoreleasePool;
    id deviceInput;

    device = (QTCaptureDevice *) devicePtr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    @try
    {
        deviceInput = [QTCaptureDeviceInput deviceInputWithDevice:device];
    }
    @catch (NSException *ex)
    {
        deviceInput = nil;
    }
    if (deviceInput)
        [deviceInput retain];

    [autoreleasePool release];
    return (jlong) deviceInput;
}
