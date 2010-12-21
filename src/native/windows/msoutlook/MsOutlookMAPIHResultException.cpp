#include "MsOutlookMAPIHResultException.h"

#include <tchar.h>

void
MsOutlookMAPIHResultException_throwNew(JNIEnv *jniEnv, HRESULT hResult)
{
    jclass clazz;

    clazz
        = jniEnv->FindClass(
                "net/java/sip/communicator/plugin/msoutlook/MsOutlookMAPIHResultException");
    if (clazz)
    {
        LPCTSTR message;

        switch (hResult)
        {
        case MAPI_E_NOT_INITIALIZED:
            message = _T("MAPI_E_NOT_INITIALIZED");
            break;
        case S_OK:
            message = _T("S_OK");
            break;
        default:
            message = NULL;
            break;
        }

        /*
         * TODO Use MsOutlookMAPIHResultException(long, String) in order to
         * communicate hResult as a HRESULT value.
         */
        jniEnv->ThrowNew(clazz, message);
    }
}
