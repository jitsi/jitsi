/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "StringUtils.h"

LPWSTR StringUtils::MultiByteToWideChar(LPCSTR str)
{
    int wsize = ::MultiByteToWideChar(CP_ACP, 0, str, -1, NULL, 0);
    LPWSTR wstr;

    if (wsize)
    {
        wstr = (LPWSTR) ::malloc(wsize * sizeof(WCHAR));
        if (str && !::MultiByteToWideChar(CP_ACP, 0, str, -1, wstr, wsize))
        {
            ::free(wstr);
            wstr = NULL;
        }
    }
    else
        wstr = NULL;
    return wstr;
}

LPSTR StringUtils::WideCharToMultiByte(LPCWSTR wstr)
{
    int size = ::WideCharToMultiByte(CP_ACP, 0, wstr, -1, NULL, 0, NULL, NULL);
    LPSTR str;

    if (size)
    {
        str = (LPSTR) ::malloc(size);
        if (str
                && !::WideCharToMultiByte(
                        CP_ACP,
                        0,
                        wstr,
                        -1,
                        str,
                        size,
                        NULL,
                        NULL))
        {
            ::free(str);
            str = NULL;
        }
    }
    else
        str = NULL;
    return str;
}
