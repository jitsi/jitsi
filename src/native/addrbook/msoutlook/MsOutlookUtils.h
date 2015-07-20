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
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKUTILS_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKUTILS_H_

#include <mapidefs.h>
#include <jni.h>

void MsOutlookUtils_createLogger(const char* logFile, const char* logPath,
		int logLevel);

void MsOutlookUtils_log(const char* message);

void MsOutlookUtils_logInfo(const char* message);

void MsOutlookUtils_deleteLogger();

char* MsOutlookUtils_getLoggerPath();

int MsOutlookUtils_getLoggerLevel();

bool MsOutlookUtils_isOutlookDefaultMailClient();

HRESULT
MsOutlookUtils_getFolderEntryIDByType
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID,
    ULONG flags, ULONG type);

HRESULT
MsOutlookUtils_HrGetOneProp(
        LPMAPIPROP mapiProp,
        ULONG propTag,
        LPSPropValue *prop);

jobjectArray
MsOutlookUtils_IMAPIProp_GetProps(
        JNIEnv *jniEnv,
        jclass clazz,
        jstring entryId,
        jlongArray propIds,
        jlong flags,
        UUID UUID_Address);

#define CALENDAR_FOLDER_TYPE 0x36D00102
#define CONTACTS_FOLDER_TYPE 0x36D10102
#endif
