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
#import <Foundation/NSData.h>

static void MacOSXAddrBookContactQuery_idToJObject
    (JNIEnv *jniEnv, id o, jobjectArray jos, jint i, jclass objectClass);

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery_ABPerson_1imageData
    (JNIEnv *jniEnv, jclass clazz, jlong person)
{
    NSData *imageData = [((ABPerson *) person) imageData];
    jbyteArray jImageData;

    if (imageData)
    {
        NSUInteger length = [imageData length];

        if (length)
        {
            jImageData = (*jniEnv)->NewByteArray(jniEnv, length);
            if (jImageData)
            {
                (*jniEnv)->SetByteArrayRegion(
                    jniEnv,
                    jImageData, 0, length,
                    [imageData bytes]);
            }
        }
        else
            jImageData = NULL;
    }
    else
        jImageData = NULL;
    return jImageData;
}

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

    addressBook = [ABAddressBook sharedAddressBook];
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
DEFINE_ABPERSON_PROPERTY_GETTER(kABOrganizationProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABPersonFlags)
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
            /*
             * We changed our minds after the initial implementation and decided
             * that we want to display not only the values but the labels as
             * well. In order to minimize the scope of the modifications, we'll
             * be returning each label in the same array right after its
             * corresponding value.
             */
            ABMultiValue *mv = (ABMultiValue *) o;
            NSUInteger mvCount = [mv count];
            jobjectArray joArray
                = (*jniEnv)->NewObjectArray(
                        jniEnv,
                        mvCount * 2 /* value, label */, objectClass, NULL);

            jo = joArray;
            if (joArray)
            {
                NSUInteger j, j2;

                for (j = 0; j < mvCount; j++)
                {
                    j2 = j * 2;
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [mv valueAtIndex:j],
                        joArray, j2,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [mv labelAtIndex:j],
                        joArray, j2 + 1,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }
                }
            }
        }
        else if ([o isKindOfClass:[NSNumber class]])
        {
            jclass longClass = (*jniEnv)->FindClass(jniEnv, "java/lang/Long");

            jo = NULL;
            if (longClass)
            {
                jmethodID longMethodID
                    = (*jniEnv)->GetMethodID(
                            jniEnv,
                            longClass, "<init>", "(J)V");

                if (longMethodID)
                {
                    jo
                        = (*jniEnv)->NewObject(
                                jniEnv,
                                longClass, longMethodID,
                                (jlong) ([((NSNumber *) o) longValue]));
                }
            }
        }
        else
            jo = NULL;
        if (jo)
            (*jniEnv)->SetObjectArrayElement(jniEnv, jos, i, jo);
    }
}
