/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKCALENDAR_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKCALENDAR_H_

void MsOutlookCalendar_getAllCalendarItems(
	     void * callbackMethod,
	     void * callbackClient,
	     long callbackAddress);
jboolean MsOutlookCalendar_foreachCalendarItemCallback(
        LPSTR iUnknown,
        long callbackAddress);

void MsOutlookCalendar_setCallbackObject(void *callback);

#define MsOutlookCalendar_UUID_Address (UUID){0x00062002, 0x0000, 0x0000, {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}}
#endif
