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

#include "MsOutlookMAPIHResultException.h"

void
MsOutlookMAPIHResultException_throwNew
    (JNIEnv *jniEnv, HRESULT hResult, LPCSTR file, ULONG line)
{
    jclass clazz;

    clazz
        = jniEnv->FindClass(
                "net/java/sip/communicator/plugin/addrbook/msoutlook/MsOutlookMAPIHResultException");
    if (clazz)
    {
        LPCSTR message;

        switch (hResult)
        {
        case MAPI_E_LOGON_FAILED:
            message = "MAPI_E_LOGON_FAILED";
            break;
        case MAPI_E_NO_ACCESS:
            message = "MAPI_E_NO_ACCESS";
            break;
        case MAPI_E_NO_SUPPORT:
            message = "MAPI_E_NO_SUPPORT";
            break;
        case MAPI_E_NOT_ENOUGH_MEMORY:
            message = "MAPI_E_NOT_ENOUGH_MEMORY";
            break;
        case MAPI_E_NOT_FOUND:
            message = "MAPI_E_NOT_FOUND";
            break;
        case MAPI_E_NOT_INITIALIZED:
            message = "MAPI_E_NOT_INITIALIZED";
            break;
        case MAPI_E_TIMEOUT:
            message = "MAPI_E_TIMEOUT";
            break;
        case MAPI_E_UNKNOWN_ENTRYID:
            message = "MAPI_E_UNKNOWN_ENTRYID";
            break;
        case MAPI_E_USER_CANCEL:
            message = "MAPI_E_USER_CANCEL";
            break;
        case MAPI_W_ERRORS_RETURNED:
            message = "MAPI_W_ERRORS_RETURNED";
            break;
        case S_OK:
            message = "S_OK";
            break;
        default:
            message = NULL;
            break;
        }

        if (message)
        {
            jmethodID methodID
                = jniEnv->GetMethodID(
                        clazz,
                        "<init>",
                        "(JLjava/lang/String;)V");

            if (methodID)
            {
                jstring jmessage = jniEnv->NewStringUTF(message);

                if (jmessage)
                {
                    jobject t
                        = jniEnv->NewObject(
                                clazz,
                                methodID,
                                (jlong) hResult, jmessage);

                    if (t)
                    {
                        jniEnv->Throw((jthrowable) t);

                        jniEnv->DeleteLocalRef(t);
                    }
                    jniEnv->DeleteLocalRef(jmessage);
                }
                return;
            }
        }

        {
            jmethodID methodID = jniEnv->GetMethodID(clazz, "<init>", "(J)V");

            if (methodID)
            {
                jobject t = jniEnv->NewObject(clazz, methodID, hResult);

                if (t)
                {
                    jniEnv->Throw((jthrowable) t);

                    jniEnv->DeleteLocalRef(t);
                }
                return;
            }
        }

        jniEnv->ThrowNew(clazz, message);

        jniEnv->DeleteLocalRef(clazz);
    }
}
