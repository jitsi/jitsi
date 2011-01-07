/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery.h"

#include "AddrBookContactQuery.h"

#import <AddressBook/AddressBook.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSAutoreleasePool.h>

static void MacOSXAddrBookContactQuery_idToJObject
    (JNIEnv *jniEnv, id o, jobjectArray jos, jint i, jclass objectClass);

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery_ABRecord_1valuesForProperties
    (JNIEnv *jniEnv, jclass clazz, jlong record, jlongArray properties)
{
    jsize propertyCount;
    jobjectArray values = NULL;

    propertyCount = (*jniEnv)->GetArrayLength(jniEnv, properties);
    if (propertyCount)
    {
        jclass objectClass;

        objectClass = (*jniEnv)->FindClass(jniEnv, "java/lang/Object");
        if (objectClass)
        {
            values
                = (*jniEnv)->NewObjectArray(
                    jniEnv,
                    propertyCount, objectClass, NULL);
            if (values)
            {
                jint i;
                ABRecord *r = (ABRecord *) record;

                for (i = 0; i < propertyCount; i++)
                {
                    jlong property;

                    (*jniEnv)->GetLongArrayRegion(
                            jniEnv,
                            properties, i, 1, &property);
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [r valueForProperty:(NSString *)property],
                        values, i,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                        break;
                }
            }
        }
    }
    return values;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery_foreachPerson
    (JNIEnv *jniEnv, jclass clazz, jstring query, jobject callback)
{
    jmethodID callbackMethodID;
    NSAutoreleasePool *autoreleasePool;
    ABAddressBook *addressBook;
    NSArray *people;
    NSUInteger peopleCount;
    NSUInteger i;

    callbackMethodID
        = AddrBookContactQuery_getPtrCallbackMethodID(jniEnv, callback);
    if (!callbackMethodID || (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv)))
        return;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook addressBook];
    people = [addressBook people];
    peopleCount = [people count];
    for (i = 0; i < peopleCount; i++)
    {
        jboolean proceed;
        ABPerson *person = [people objectAtIndex:i];

        proceed
            = (*jniEnv)->CallBooleanMethod(
                jniEnv,
                callback, callbackMethodID,
                person);
        if ((JNI_FALSE == proceed)
                || (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv)))
            break;
    }
    [addressBook release];

    [autoreleasePool release];
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

static void
MacOSXAddrBookContactQuery_idToJObject
    (JNIEnv *jniEnv,
    id o,
    jobjectArray jos, jint i,
    jclass objectClass)
{
    if (o)
    {
        jobject jo;

        if ([o isKindOfClass:[NSString class]])
        {
            jo = (*jniEnv)->NewStringUTF(jniEnv, [((NSString *) o) UTF8String]);
        }
        else if ([o isKindOfClass:[ABMultiValue class]])
        {
            ABMultiValue *mv = (ABMultiValue *) o;
            NSUInteger mvCount = [mv count];
            jobjectArray joArray
                = (*jniEnv)->NewObjectArray(jniEnv, mvCount, objectClass, NULL);

            jo = joArray;
            if (joArray)
            {
                NSUInteger j;

                for (j = 0; j < mvCount; j++)
                {
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [mv valueAtIndex:j],
                        joArray, j,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }
                }
            }
        }
        else
            jo = NULL;
        if (jo)
            (*jniEnv)->SetObjectArrayElement(jniEnv, jos, i, jo);
    }
}
