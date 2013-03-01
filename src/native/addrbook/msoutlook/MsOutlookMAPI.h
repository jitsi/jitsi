/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPI_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPI_H_

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#ifndef __in
#define __in
#endif /* #ifndef __in */
#ifndef __in_opt
#define __in_opt
#endif /* #ifndef __in_opt */
#ifndef __out
#define __out
#endif /* #ifndef __out */

#if defined(_WINBASE_H) && !defined(_WINBASE_)
#define _tagCY_DEFINED
#define _WINBASE_
#endif
#include <mapitags.h>
#include <mapix.h>

WINBOOL MsOutlookAddrBook_fBinFromHex(LPTSTR lpsz, LPBYTE lpb);
#define FBinFromHex MsOutlookAddrBook_fBinFromHex

void MsOutlookAddrBook_freeProws(LPSRowSet lpRows);
#define FreeProws MsOutlookAddrBook_freeProws

void MsOutlookAddrBook_hexFromBin(LPBYTE pb, int cb, LPTSTR sz);
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
    LPSTR profileName, LPSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession);
#define MAPILogonEx MsOutlookAddrBook_mapiLogonEx

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPI_H_ */
