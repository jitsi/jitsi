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

#include "cleansweep.h"

#include <windows.h>
#include <shellapi.h>
#include <stdlib.h>
#include <tchar.h>

static void CleanSweep_rm(LPCTSTR path);

static void
CleanSweep_rm(LPCTSTR path)
{
    DWORD fileAttributes = GetFileAttributes(path);

    if ((INVALID_FILE_ATTRIBUTES != fileAttributes)
            && ((fileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0))
    {
        SHFILEOPSTRUCT fileOp;

        ZeroMemory(&fileOp, sizeof(fileOp));
        fileOp.wFunc = FO_DELETE;
        fileOp.pFrom = path;
        fileOp.fFlags
            = FOF_NOCONFIRMATION
                | FOF_NOCONFIRMMKDIR
                | FOF_NOERRORUI
                | FOF_SILENT;

        SHFileOperation(&fileOp);
    }
}

int CALLBACK
_tWinMain(
        HINSTANCE instance, HINSTANCE prevInstance,
        LPTSTR cmdLine,
        int cmdShow)
{
    LPCTSTR productName = _T(PRODUCTNAME);
    size_t productNameLength = _tcslen(productName);

    /* If we do not have the product name, we cannot really delete anything. */
    if (!productNameLength)
        return 0;

    /* Delete the private product data and preferences of the user. */
    {
        LPCTSTR appDataName = _T("APPDATA");
        DWORD appDataCapacity = GetEnvironmentVariable(appDataName, NULL, 0);

        if (appDataCapacity)
        {
            /*
             * For the purposes of convenience in CleanSweep_rm, we will
             * double-null terminate the directory path.
             */
            size_t dir_size = appDataCapacity + 1 + productNameLength + 1;
            LPTSTR dir = (LPTSTR)malloc(sizeof(TCHAR) * dir_size);
            if (dir)
            {
                DWORD appDataLength
                    = GetEnvironmentVariable(
                            appDataName,
                            dir,
                            appDataCapacity);

                if (appDataLength && (appDataLength < appDataCapacity))
                {
                    LPTSTR str = dir + appDataLength;

                    *str = _T('\\');
                    str++;
                    _tcsncpy_s(str, dir_size - appDataLength, productName, productNameLength);
                    str += productNameLength;
                    *str = 0;
                    str++;
                    *str = 0;

                    CleanSweep_rm(dir);
                }
                free(dir);
            }
        }
    }

    return 0;
}
