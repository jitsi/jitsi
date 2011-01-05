/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPIHRESULTEXCEPTION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPIHRESULTEXCEPTION_H_

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

#endif /* _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPIHRESULTEXCEPTION_ */