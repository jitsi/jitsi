/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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

