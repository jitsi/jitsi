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
