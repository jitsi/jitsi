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

#include <shellapi.h>
#include <stdlib.h>
#include <tchar.h>

#define CLEANSWEEP_ALLUSERS 0

static BOOL CleanSweep_isAdmin();
static void CleanSweep_rm(LPCTSTR path);

static BOOL
CleanSweep_isAdmin()
{
    BOOL admin = FALSE;
/*
    BYTE admins[SECURITY_MAX_SID_SIZE];
    DWORD adminsSize = sizeof(admins);

    if (CreateWellKnownSid(
            WinBuiltinAdministratorsSid,
            NULL,
            (PSID) admins,
            &adminsSize))
        CheckTokenMembership(NULL, (PSID) admins, &admin))
*/
    return admin;
}

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
WinMain(
        HINSTANCE instance, HINSTANCE prevInstance,
        LPSTR cmdLine,
        int cmdShow)
{
    LPCTSTR productName = PRODUCTNAME;
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
            LPTSTR dir
                = (LPTSTR)
                    malloc(
                            sizeof(TCHAR)
                                * (appDataCapacity
                                    + 1
                                    + productNameLength
                                    + 1));

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
                    _tcsncpy(str, productName, productNameLength);
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

    /*
     * If administrative privileges are available, try to clean sweep the
     * private product data and preferences of all users.
     */
#ifdef CLEANSWEEP_ALLUSERS
    if (CLEANSWEEP_ALLUSERS)
    {
        LPCTSTR systemDriveName = _T("SystemDrive");
        DWORD systemDriveCapacity
            = GetEnvironmentVariable(systemDriveName, NULL, 0);

        if (systemDriveCapacity)
        {
            LPCTSTR users = _T("\\Users\\");
            size_t usersLength = _tcslen(users);
            LPTSTR path
                = (LPTSTR)
                    malloc(
                            sizeof(TCHAR)
                                * (systemDriveCapacity + usersLength + 1));

            if (path)
            {
                DWORD systemDriveLength
                    = GetEnvironmentVariable(
                            systemDriveName,
                            path,
                            systemDriveCapacity);

                if (systemDriveLength
                        && (systemDriveLength < systemDriveCapacity))
                {
                    LPTSTR str = path + systemDriveLength;

                    HANDLE findFile;
                    WIN32_FIND_DATA findFileData;

                    _tcsncpy(str, users, usersLength);
                    str += usersLength;
                    *str = _T('*');
                    str++;
                    *str = 0;

                    findFile = FindFirstFile(path, &findFileData);
                    if (INVALID_HANDLE_VALUE != findFile)
                    {
                        size_t pathLength = systemDriveLength + usersLength;
                        LPCTSTR appData = _T("\\AppData\\Roaming\\");
                        size_t appDataLength = _tcslen(appData);

                        do
                        {
                            LPCTSTR fileName = findFileData.cFileName;

                            if (((findFileData.dwFileAttributes
                                            & FILE_ATTRIBUTE_DIRECTORY)
                                        != 0)
                                    && (_tcsicmp(fileName, _T(".")) != 0)
                                    && (_tcsicmp(fileName, _T("..")) != 0))
                            {
                                size_t fileNameLength = _tcslen(fileName);
                                LPTSTR dir
                                    = (LPTSTR)
                                        malloc(
                                                sizeof(TCHAR)
                                                    * (pathLength
                                                        + fileNameLength
                                                        + appDataLength
                                                        + productNameLength
                                                        + 2));

                                if (dir)
                                {
                                    str = dir;
                                    _tcsncpy(str, path, pathLength);
                                    str += pathLength;
                                    _tcsncpy(str, fileName, fileNameLength);
                                    str += fileNameLength;
                                    _tcsncpy(str, appData, appDataLength);
                                    str += appDataLength;
                                    _tcsncpy(
                                            str,
                                            productName,
                                            productNameLength);
                                    str += productNameLength;
                                    *str = 0;
                                    str++;
                                    *str = 0;

                                    CleanSweep_rm(dir);
                                    free(dir);
                                }
                            }
                        }
                        while (FindNextFile(findFile, &findFileData));
                        FindClose(findFile);
                    }
                }
                free(path);
            }
        }
    }
#endif /* #ifdef CLEANSWEEP_ALLUSERS */

    return 0;
}
