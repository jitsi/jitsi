/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "AddrBookContactQuery.h"

static void Exception_throwNew
    (JNIEnv *jniEnv, const char *className, const char *message);

jmethodID
AddrBookContactQuery_getPtrCallbackMethodID(JNIEnv *jniEnv, jobject callback)
{
    jclass callbackClass;
    jmethodID callbackMethodID = 0;

    /*
     * Make sure that the specified arguments are valid. For example, check
     * whether callback exists and has the necessary signature.
     */
    if (callback)
    {
        callbackClass = (*jniEnv)->GetObjectClass(jniEnv, callback);
        if (callbackClass)
        {
            callbackMethodID
                = (*jniEnv)->GetMethodID(
                        jniEnv,
                        callbackClass, "callback", "(J)Z");
            if (!callbackMethodID)
            {
                Exception_throwNew(
                    jniEnv, "java/lang/IllegalArgumentException", "callback");
            }
        }
    }
    else
    {
        Exception_throwNew(
            jniEnv, "java/lang/NullPointerException", "callback");
    }
    return callbackMethodID;
}

static void
Exception_throwNew(JNIEnv *jniEnv, const char *className, const char *message)
{
    jclass clazz;

    clazz = (*jniEnv)->FindClass(jniEnv, className);
    if (clazz)
        (*jniEnv)->ThrowNew(jniEnv, clazz, message);
}

