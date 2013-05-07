/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_STRINGUTILS_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_STRINGUTILS_H_

#include <tchar.h>
#include <windows.h>

class StringUtils
{
public:
    static LPWSTR MultiByteToWideChar(LPCSTR str);
    static LPSTR WideCharToMultiByte(LPCWSTR wstr);
};

#endif
