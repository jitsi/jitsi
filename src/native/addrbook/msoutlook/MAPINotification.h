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

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPINOTIFICATION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPINOTIFICATION_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include <mapidefs.h>
#include <mapix.h>

/**
 * Manages notification for the message data base (used to get the list of
 * contact).
 *
 * @author Vincent Lucas
 */

boolean MAPINotification_callCallbackMethod(LPSTR iUnknown, long objectAddr);

void MAPINotification_jniCallDeletedMethod(LPSTR iUnknown);
void MAPINotification_jniCallInsertedMethod(LPSTR iUnknown);
void MAPINotification_jniCallUpdatedMethod(LPSTR iUnknown);

void MAPINotification_jniCallCalendarDeletedMethod(LPSTR iUnknown);
void MAPINotification_jniCallCalendarInsertedMethod(LPSTR iUnknown);
void MAPINotification_jniCallCalendarUpdatedMethod(LPSTR iUnknown);

LONG
STDAPICALLTYPE MAPINotification_onNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications,
    ULONG type);

void
MAPINotification_registerJniNotificationsDelegate
    (JNIEnv *jniEnv, jobject notificationsDelegate);

void
MAPINotification_registerCalendarJniNotificationsDelegate
    (JNIEnv *jniEnv, jobject notificationsDelegate);
void
MAPINotification_registerNativeNotificationsDelegate
    (void * deletedMethod, void * insertedMethod, void *updatedMethod);

void
MAPINotification_registerCalendarNativeNotificationsDelegate
    (void * deletedMethod, void * insertedMethod, void *updatedMethod);

void MAPINotification_registerNotifyAllMsgStores(LPMAPISESSION mapiSession);

void MAPINotification_unregisterJniNotificationsDelegate(JNIEnv *jniEnv);
void MAPINotification_unregisterNativeNotificationsDelegate();
void MAPINotification_unregisterNotifyAllMsgStores(void);

void MAPINotification_unregisterJniCalendarNotificationsDelegate(JNIEnv *jniEnv);
void MAPINotification_unregisterNativeCalendarNotificationsDelegate();


#ifdef __cplusplus
}
#endif

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPINOTIFICATION_H_ */
