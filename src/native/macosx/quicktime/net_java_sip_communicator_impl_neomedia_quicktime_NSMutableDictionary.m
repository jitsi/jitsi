/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_quicktime_NSMutableDictionary.h"

#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSValue.h>

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_NSMutableDictionary_allocAndInit
    (JNIEnv *jniEnv, jclass clazz)
{
    NSAutoreleasePool *autoreleasePool;
    NSMutableDictionary *mutableDictionary;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    mutableDictionary = [[NSMutableDictionary alloc] init];

    [autoreleasePool release];
    return (jlong) mutableDictionary;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_NSMutableDictionary_setIntForKey
    (JNIEnv *jniEnv, jclass clazz, jlong ptr, jint value, jlong key)
{
    NSMutableDictionary *mutableDictionary;
    NSAutoreleasePool *autoreleasePool;

    mutableDictionary = (NSMutableDictionary *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    [mutableDictionary setObject:[NSNumber numberWithInt:value] forKey:(id)key];

    [autoreleasePool release];
}
