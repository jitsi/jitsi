/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKADDRBOOKCONTACTQUERY_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKADDRBOOKCONTACTQUERY_H_

#include <mapidefs.h>
#include <jni.h>

typedef
    jboolean (*MsOutlookAddrBookContactQuery_ForeachRowInTableCallback)
        (LPUNKNOWN iUnknown,
        ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
        const char * query,
        void * callbackMethod,
        void * callbackClient,
        long callbackAddress);

jboolean MsOutlookAddrBookContactQuery_foreachRowInTable
    (LPMAPITABLE mapiTable,
    MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
    LPUNKNOWN iUnknown,
    const char * query,
    void * callbackMethod,
    void * callbackClient,
    long callbackAddress);

jboolean MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
     const char * query,
     void * callbackMethod,
     void * callbackClient,
     long callbackAddress);

int MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp
    (long propId, const char * nativeEntryId);

long MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
        const char* nativeEntryId,
        int propIdCount,
        long * propIds,
        long flags,
        void ** props,
        unsigned long* propsLength,
        char * propsType,
        UUID UUID_Address);

int MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString
    (long propId, const wchar_t* nativeValue, const char* nativeEntryId);

char* MsOutlookAddrBookContactQuery_createContact(void);

int MsOutlookAddrBookContactQuery_deleteContact(const char * nativeEntryId);

HRESULT MsOutlookAddrBookContactQuery_foreachMailUser
    (const char * query,
     void * callbackMethod,
     void * callbackClient,
     long callbackAddress);

char* MsOutlookAddrBookContactQuery_getStringUnicodeProp
    (LPUNKNOWN entry, ULONG propId);

int MsOutlookAddrBookContactQuery_compareEntryIds
    (LPSTR id1, LPSTR id2);

#define MsOutlookAddrBookContactQuery_UUID_Address (UUID){0x00062004, 0x0000, 0x0000, {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}}

#endif
