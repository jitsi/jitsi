/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

