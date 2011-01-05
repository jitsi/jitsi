/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery.h"

#import <AddressBook/AddressBook.h>

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery_ABRecord_1valuesForProperties
    (JNIEnv *jniEnv, jclass clazz, jlong record, jlongArray properties)
{
    /* TODO Auto-generated method stub */
    return NULL;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery_foreachPerson
    (JNIEnv *jniEnv, jclass clazz, jstring query, jobject callback)
{
    /* TODO Auto-generated method stub */
}

#define DEFINE_ABPERSON_PROPERTY_GETTER(property) \
    JNIEXPORT jlong JNICALL \
    Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery_##property \
        (JNIEnv *jniEnv, jclass clazz) \
    { \
        return (jlong) property; \
    }

DEFINE_ABPERSON_PROPERTY_GETTER(kABAIMInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABEmailProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABFirstNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABFirstNamePhoneticProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABICQInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABJabberInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABLastNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABLastNamePhoneticProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMiddleNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMiddleNamePhoneticProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMSNInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABNicknameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABPhoneProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABYahooInstantProperty)
