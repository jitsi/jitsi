/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKADDRBOOKCONTACTQUERY_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKADDRBOOKCONTACTQUERY_H_

#include <mapidefs.h>

int MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp
    (long propId, const char * nativeEntryId);

long MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
        const char* nativeEntryId,
        int propIdCount,
        long * propIds,
        long flags,
        void ** props,
        unsigned long* propsLength,
        char * propsType);

int MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString
    (long propId, const wchar_t* nativeValue, const char* nativeEntryId);

char* MsOutlookAddrBookContactQuery_createContact(void);

int MsOutlookAddrBookContactQuery_deleteContact(const char * nativeEntryId);

void MsOutlookAddrBookContactQuery_foreachMailUser
    (const char * query, void * callback, void * callbackObject);

char* MsOutlookAddrBookContactQuery_getStringUnicodeProp
    (LPUNKNOWN entry, ULONG propId);

int MsOutlookAddrBookContactQuery_compareEntryIds
    (LPSTR id1, LPSTR id2);

#endif
