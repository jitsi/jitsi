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

#include "nls.h"

#include <stdlib.h>

LPWSTR
NLS_str2wstr(LPCSTR str)
{
    int wstrSize = MultiByteToWideChar(CP_THREAD_ACP, 0, str, -1, NULL, 0);
    LPWSTR wstr;

    if (wstrSize)
    {
        wstr = malloc(wstrSize * sizeof(WCHAR));
        if (wstr)
        {
            wstrSize
                = MultiByteToWideChar(
                        CP_THREAD_ACP,
                        0,
                        str, -1,
                        wstr, wstrSize);
            if (!wstrSize)
            {
                free(wstr);
                wstr = NULL;
            }
        }
    }
    else
        wstr = NULL;
    return wstr;
}

LPSTR
NLS_wstr2str(LPCWSTR wstr)
{
    int strSize
        = WideCharToMultiByte(
                CP_THREAD_ACP,
                WC_NO_BEST_FIT_CHARS,
                wstr, -1,
                NULL, 0,
                NULL, NULL);
    LPSTR str;

    if (strSize)
    {
        str = malloc(strSize);
        if (str)
        {
            strSize
                = WideCharToMultiByte(
                        CP_THREAD_ACP,
                        WC_NO_BEST_FIT_CHARS,
                        wstr, -1,
                        str, strSize,
                        NULL, NULL);
            if (!strSize)
            {
                free(str);
                str = NULL;
            }
        }
    }
    else
        str = NULL;
    return str;
}
