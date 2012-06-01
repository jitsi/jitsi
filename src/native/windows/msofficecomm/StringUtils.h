/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_STRINGUTILS_H_
#define _JMSOFFICECOMM_STRINGUTILS_H_

#include <tchar.h>
#include <windows.h>

class StringUtils
{
public:
    static LPWSTR MultiByteToWideChar(LPCSTR str);
    static LPSTR WideCharToMultiByte(LPCWSTR wstr);
};

#endif /* #ifndef _JMSOFFICECOMM_STRINGUTILS_H_ */
