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

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_calendar_CalendarServiceImpl.h"
#include "MsOutlookUtils.h"
#include "MsOutlookCalendar.h"
#include "MAPINotification.h"
#include "com/ComClient.h"
#include "com/MsOutlookAddrBookServer.h"

JNIEXPORT void JNICALL Java_net_java_sip_communicator_plugin_addrbook_msoutlook_calendar_CalendarServiceImpl_getAllCalendarItems(
        JNIEnv *jniEnv,
        jclass clazz,
        jobject callback)
{
	MAPINotification_registerCalendarJniNotificationsDelegate(
	      jniEnv,
	      callback);
	MsOutlookCalendar_setCallbackObject(callback);
	IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
	if(iServer)
	{
		iServer->getAllCalendarItems((long)(intptr_t)callback);
	}

}



JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_calendar_CalendarServiceImpl_IMAPIProp_1GetProps(
        JNIEnv *jniEnv,
        jclass clazz,
        jstring entryId,
        jlongArray propIds,
        jlong flags)
{
	return MsOutlookUtils_IMAPIProp_GetProps(jniEnv, clazz, entryId, propIds, flags, MsOutlookCalendar_UUID_Address);
}

