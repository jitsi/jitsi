/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKADDRBOOKCONTACTSOURCESERVICE_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKADDRBOOKCONTACTSOURCESERVICE_H_

#include <jni.h>
#include <mapix.h>

BOOL MsOutlookAddrBook_fBinFromHex(LPSTR lpsz, LPBYTE lpb);
#define FBinFromHex MsOutlookAddrBook_fBinFromHex

void MsOutlookAddrBook_freeProws(LPSRowSet lpRows);
#define FreeProws MsOutlookAddrBook_freeProws

void MsOutlookAddrBook_hexFromBin(LPBYTE pb, int cb, LPSTR sz);
#define HexFromBin MsOutlookAddrBook_hexFromBin

void MsOutlookAddrBook_hrAllocAdviseSink
    (LPNOTIFCALLBACK lpfnCallback, LPVOID lpvContext,
     LPMAPIADVISESINK* lppAdviseSink);
#define HrAllocAdviseSink MsOutlookAddrBook_hrAllocAdviseSink

HRESULT MsOutlookAddrBook_hrQueryAllRows
    (LPMAPITABLE lpTable, LPSPropTagArray lpPropTags,
     LPSRestriction lpRestriction, LPSSortOrderSet lpSortOrderSet,
     LONG crowsMax, LPSRowSet* lppRows);
#define HrQueryAllRows MsOutlookAddrBook_hrQueryAllRows

SCODE MsOutlookAddrBook_mapiAllocateBuffer(ULONG size, LPVOID FAR *buffer);
#define MAPIAllocateBuffer MsOutlookAddrBook_mapiAllocateBuffer

ULONG MsOutlookAddrBook_mapiFreeBuffer(LPVOID buffer);
#define MAPIFreeBuffer MsOutlookAddrBook_mapiFreeBuffer

HRESULT MsOutlookAddrBook_mapiLogonEx
    (ULONG_PTR uiParam,
    LPTSTR profileName, LPTSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession);
#define MAPILogonEx MsOutlookAddrBook_mapiLogonEx


HRESULT MsOutlookAddrBookContactSourceService_MAPIInitialize
    (jlong version, jlong flags);

HRESULT MsOutlookAddrBookContactSourceService_MAPIInitializeCOMServer(void);

void MsOutlookAddrBookContactSourceService_MAPIUninitialize(void);

void MsOutlookAddrBookContactSourceService_MAPIUninitializeCOMServer(void);

HRESULT MsOutlookAddrBookContactSourceService_NativeMAPIInitialize
    (jlong version, jlong flags,
     void * deletedMethod, void * insertedMethod, void * updatedMethod);

void MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize(void);

#endif
