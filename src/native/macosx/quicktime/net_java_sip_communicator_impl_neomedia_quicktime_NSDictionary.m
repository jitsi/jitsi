#include "net_java_sip_communicator_impl_neomedia_quicktime_NSDictionary.h"

#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSValue.h>

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_NSDictionary_intForKey
  (JNIEnv *jniEnv, jclass clazz, jlong ptr, jlong key)
{
    NSDictionary *dictionary;
    NSAutoreleasePool *autoreleasePool;
    NSNumber *value;
    jint jvalue;

    dictionary = (NSDictionary *) ptr;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    value = [dictionary objectForKey:(id)key];
    jvalue = value ? [value intValue] : 0;

    [autoreleasePool release];
    return jvalue;
}
