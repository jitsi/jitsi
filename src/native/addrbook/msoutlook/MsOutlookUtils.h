/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
