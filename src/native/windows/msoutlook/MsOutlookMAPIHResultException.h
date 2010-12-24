#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_MSOUTLOOK_MSOUTLOOKMAPIHRESULTEXCEPTION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_MSOUTLOOK_MSOUTLOOKMAPIHRESULTEXCEPTION_H_

#include <jni.h>
#include "MsOutlookMAPI.h"
#include <tchar.h>

#ifdef __cplusplus
extern "C" {
#endif /* #ifdef __cplusplus */

void MsOutlookMAPIHResultException_throwNew
    (JNIEnv *jniEnv, HRESULT hResult, LPCTSTR file, ULONG line);

#ifdef __cplusplus
}
#endif /* #ifdef __cplusplus */

#endif /* _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_MSOUTLOOK_MSOUTLOOKMAPIHRESULTEXCEPTION_ */