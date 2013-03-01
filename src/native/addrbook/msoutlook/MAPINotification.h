/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPINOTIFICATION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPINOTIFICATION_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "MsOutlookMAPI.h"

#include <jni.h>
#include <mapidefs.h>

LONG
STDAPICALLTYPE MAPINotification_onNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications);

void
MAPINotification_registerNotificationsDelegate
    (JNIEnv *jniEnv, LPMAPISESSION, jobject);

ULONG MAPINotification_registerNotifyMessageDataBase (LPMDB iUnknown);

void MAPINotification_unregisterNotificationsDelegate (JNIEnv *jniEnv);

#ifdef __cplusplus
}
#endif

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPINOTIFICATION_H_ */
