/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPI_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPI_H_

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#ifndef __in_opt
#define __in_opt
#endif /* #ifndef __in_opt */
#if defined(_WINBASE_H) && !defined(_WINBASE_)
#define _tagCY_DEFINED
#define _WINBASE_
#endif
#include <mapitags.h>
#include <mapix.h>

SCODE MsOutlookAddrBook_MAPIAllocateBuffer(ULONG size, LPVOID FAR *buffer);
#define MAPIAllocateBuffer MsOutlookAddrBook_MAPIAllocateBuffer
ULONG MsOutlookAddrBook_MAPIFreeBuffer(LPVOID buffer);
#define MAPIFreeBuffer MsOutlookAddrBook_MAPIFreeBuffer
HRESULT MsOutlookAddrBook_MAPILogonEx
    (ULONG_PTR uiParam,
    LPSTR profileName, LPSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession);
#define MAPILogonEx MsOutlookAddrBook_MAPILogonEx

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MSOUTLOOKMAPI_H_ */
