/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "MsOutlookMAPIHResultException.h"

void
MsOutlookMAPIHResultException_throwNew
    (JNIEnv *jniEnv, HRESULT hResult, LPCTSTR file, ULONG line)
{
    jclass clazz;

    clazz
        = jniEnv->FindClass(
                "net/java/sip/communicator/plugin/addrbook/msoutlook/MsOutlookMAPIHResultException");
    if (clazz)
    {
        LPCTSTR message;

        switch (hResult)
        {
        case MAPI_E_LOGON_FAILED:
            message = _T("MAPI_E_LOGON_FAILED");
            break;
        case MAPI_E_NO_ACCESS:
            message = _T("MAPI_E_NO_ACCESS");
            break;
        case MAPI_E_NO_SUPPORT:
            message = _T("MAPI_E_NO_SUPPORT");
            break;
        case MAPI_E_NOT_ENOUGH_MEMORY:
            message = _T("MAPI_E_NOT_ENOUGH_MEMORY");
            break;
        case MAPI_E_NOT_FOUND:
            message = _T("MAPI_E_NOT_FOUND");
            break;
        case MAPI_E_NOT_INITIALIZED:
            message = _T("MAPI_E_NOT_INITIALIZED");
            break;
        case MAPI_E_TIMEOUT:
            message = _T("MAPI_E_TIMEOUT");
            break;
        case MAPI_E_UNKNOWN_ENTRYID:
            message = _T("MAPI_E_UNKNOWN_ENTRYID");
            break;
        case MAPI_E_USER_CANCEL:
            message = _T("MAPI_E_USER_CANCEL");
            break;
        case MAPI_W_ERRORS_RETURNED:
            message = _T("MAPI_W_ERRORS_RETURNED");
            break;
        case S_OK:
            message = _T("S_OK");
            break;
        default:
            message = NULL;
            break;
        }

        if (message)
        {
            jmethodID methodID;

            methodID
                = jniEnv->GetMethodID(
                        clazz,
                        "<init>",
                        "(JLjava/lang/String;)V");
            if (methodID)
            {
                jobject t;

                t = jniEnv->NewObject(clazz, methodID, hResult, message);
                if (t)
                    jniEnv->Throw((jthrowable) t);
                return;
            }
        }

        {
            jmethodID methodID;

            methodID = jniEnv->GetMethodID(clazz, "<init>", "(J)V");
            if (methodID)
            {
                jobject t;

                t = jniEnv->NewObject(clazz, methodID, hResult);
                if (t)
                    jniEnv->Throw((jthrowable) t);
                return;
            }
        }

        jniEnv->ThrowNew(clazz, message);
    }
}
