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
#include "MsOutlookUtils.h"
#include "MsOutlookAddrBookContactQuery.h"
#include "MsOutlookCalendar.h"
#include "MAPINotification.h"
#include "MsOutlookAddrBookContactSourceService.h"

#include "MAPISession.h"
#include "StringUtils.h"
#include <Mapidefs.h>
#include <jni.h>

static ULONG MsOutlookCalendar_rdOpenEntryUlFlags = 0x0;

static void* callbackObject = NULL;

jboolean MsOutlookCalendar_foreachCalendarItemCallback(
        LPSTR iUnknown,
        void * object);

static jboolean
MsOutlookCalendar_onForeachCalendarInMsgStoresTableRow
    (LPUNKNOWN mapiSession,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    const char * query, void * callbackMethod, void * callbackClient,
    long callbackAddress);

void MsOutlookCalendar_setCallbackObject(void *callback)
{
	callbackObject = callback;
}

void MsOutlookCalendar_getAllCalendarItems(
	     void * callbackMethod,
	     void * callbackClient,
	     long callbackAddress)
{
	MAPISession_lock();
	LPMAPISESSION mapiSession = MAPISession_getMapiSession();
	if (mapiSession == NULL)
	{
		MAPISession_unlock();
		return;
	}
	HRESULT hResult;
	LPMAPITABLE msgStoresTable = NULL;
	hResult = mapiSession->GetMsgStoresTable(0, &msgStoresTable);
	if (HR_SUCCEEDED(hResult) && msgStoresTable)
	{
		MsOutlookAddrBookContactQuery_foreachRowInTable(
			msgStoresTable,
			MsOutlookCalendar_onForeachCalendarInMsgStoresTableRow,
			(LPUNKNOWN) mapiSession,
			NULL,callbackMethod,
            callbackClient,
            callbackAddress);
		msgStoresTable->Release();
	}

	 MAPISession_unlock();
}

jboolean MsOutlookCalendar_foreachCalendarItemCallback(
        LPSTR iUnknown,
        long callbackObject)
{

	LPWSTR iUnknownW = StringUtils::MultiByteToWideChar(iUnknown);
	BSTR res = SysAllocString(iUnknownW);

	char * charId = StringUtils::WideCharToMultiByte(res);
    MAPINotification_callCallbackMethod(charId, callbackObject);
    free(charId);

	SysFreeString(res);
	free(iUnknownW);

    return true;
}


static jboolean
MsOutlookCalendar_onForeachCalendarInMsgStoresTableRow
    (LPUNKNOWN mapiSession,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    const char * query,
    void * callbackMethod,
	void * callbackClient,
	long callbackAddress)
{
    HRESULT hResult;
    LPMDB msgStore;
    // In case, that we've failed but other parts of the hierarchy may still
    // succeed.
    jboolean proceed = JNI_TRUE;

    hResult = ((LPMAPISESSION) mapiSession)->OpenMsgStore(
                0,
                entryIDByteCount, entryID,
                NULL,
                MDB_NO_MAIL | MsOutlookCalendar_rdOpenEntryUlFlags,
                &msgStore);
    if (HR_SUCCEEDED(hResult))
    {
        LPENTRYID receiveFolderEntryID = NULL;
        ULONG calendarFolderEntryIDByteCount = 0;
        LPENTRYID calendarFolderEntryID = NULL;

        hResult = msgStore->GetReceiveFolder(
                    NULL,
                    0,
                    &entryIDByteCount,
                    &receiveFolderEntryID,
                    NULL);
        if (HR_SUCCEEDED(hResult))
        {
            hResult = MsOutlookCalendar_getCalendarFolderEntryID(
                        msgStore,
                        entryIDByteCount,
                        receiveFolderEntryID,
                        &calendarFolderEntryIDByteCount,
                        &calendarFolderEntryID,
                        MsOutlookCalendar_rdOpenEntryUlFlags);
            MAPIFreeBuffer(receiveFolderEntryID);
        }
        if (HR_FAILED(hResult))
        {
            hResult = MsOutlookCalendar_getCalendarFolderEntryID(
                        msgStore,
                        0,
                        NULL,
                        &calendarFolderEntryIDByteCount,
                        &calendarFolderEntryID,
                        MsOutlookCalendar_rdOpenEntryUlFlags);
        }
        if (HR_SUCCEEDED(hResult))
        {
            ULONG calendarFolderObjType;
            LPUNKNOWN calendarFolder;

            hResult = msgStore->OpenEntry(
            		calendarFolderEntryIDByteCount,
            		calendarFolderEntryID,
                    NULL,
                    MsOutlookCalendar_rdOpenEntryUlFlags,
                    &calendarFolderObjType,
                    &calendarFolder);
            if (HR_SUCCEEDED(hResult))
            {
                proceed = MsOutlookAddrBookContactQuery_foreachMailUser(
                			calendarFolderObjType,
                			calendarFolder,
                            query,
                            callbackMethod,
							callbackClient,
							callbackAddress);
                calendarFolder->Release();
            }
            MAPIFreeBuffer(calendarFolderEntryID);
        }
        msgStore->Release();
    }

    return proceed;
}

HRESULT
MsOutlookCalendar_getCalendarFolderEntryID
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *calendarFolderEntryIDByteCount, LPENTRYID *calendarFolderEntryID,
    ULONG flags)
{
	return MsOutlookUtils_getFolderEntryIDByType(
                    msgStore,
                    folderEntryIDByteCount,
                    folderEntryID,
                    calendarFolderEntryIDByteCount,
                    calendarFolderEntryID,
                    flags,
                    CALENDAR_FOLDER_TYPE);
}
