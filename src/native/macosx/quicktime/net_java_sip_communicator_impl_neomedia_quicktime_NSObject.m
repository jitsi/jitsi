/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_quicktime_NSObject.h"

#include "common.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_NSObject_release
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    NSObject_performSelector((id) ptr, @"release");
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_NSObject_retain
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    NSObject_performSelector((id) ptr, @"retain");
}
