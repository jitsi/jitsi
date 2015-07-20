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

HRESULT
MsOutlookCalendar_getCalendarFolderEntryID
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *calendarFolderEntryIDByteCount, LPENTRYID *calendarFolderEntryID,
    ULONG flags);

#define MsOutlookCalendar_UUID_Address (UUID){0x00062002, 0x0000, 0x0000, {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}}
#endif
