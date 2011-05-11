/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_quicktime_QTFormatDescription.h"

#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSGeometry.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>
#import <QTKit/QTFormatDescription.h>

JNIEXPORT jobject JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTFormatDescription_sizeForKey
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jstring key)
{
    const char *cKey;
    jobject size = NULL;

    cKey = (const char *) (*jniEnv)->GetStringUTFChars(jniEnv, key, NULL);
    if (cKey)
    {
        QTFormatDescription *formatDescription;
        NSAutoreleasePool *autoreleasePool;
        NSString *oKey;
        NSValue *attribute;

        formatDescription = (QTFormatDescription *) ptr;
        autoreleasePool = [[NSAutoreleasePool alloc] init];

        oKey = [NSString stringWithUTF8String:cKey];
        (*jniEnv)->ReleaseStringUTFChars(jniEnv, key, cKey);

        attribute = [formatDescription attributeForKey:oKey];
        if (attribute)
        {
            NSSize oSize;
            jclass dimensionClass;

            oSize = [attribute sizeValue];

            dimensionClass = (*jniEnv)->FindClass(jniEnv, "java/awt/Dimension");
            if (dimensionClass)
            {
                jmethodID dimensionCtorMethodID;

                dimensionCtorMethodID
                    = (*jniEnv)
                        ->GetMethodID(
                            jniEnv,
                            dimensionClass,
                            "<init>",
                            "(II)V");
                if (dimensionCtorMethodID)
                    size
                        = (*jniEnv)
                            ->NewObject(
                                jniEnv,
                                dimensionClass,
                                dimensionCtorMethodID,
                                (jint) oSize.width,
                                (jint) oSize.height);
            }
        }

        [autoreleasePool release];
    }
    return size;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_QTFormatDescription_VideoEncodedPixelsSizeAttribute
    (JNIEnv *jniEnv, jclass clazz)
{
    NSAutoreleasePool *autoreleasePool;
    jstring jstr;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    jstr
        = (*jniEnv)
            ->NewStringUTF(
                jniEnv,
                [QTFormatDescriptionVideoEncodedPixelsSizeAttribute
                    UTF8String]);

    [autoreleasePool release];
    return jstr;
}
