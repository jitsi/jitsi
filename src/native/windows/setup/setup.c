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

#include "lasterror.h"
#include "nls.h"
#include "registry.h"
#include "setup.h"

#include <ctype.h> /* isspace */
#include <stdint.h> /* intptr_t */
#include <stdlib.h>
#include <string.h>
#include <tchar.h>

#include <objbase.h>
#ifndef ERROR_RESOURCE_ENUM_USER_STOP
#define ERROR_RESOURCE_ENUM_USER_STOP 0x3B02
#endif /* #ifndef ERROR_RESOURCE_ENUM_USER_STOP */
#include <shellapi.h>
#ifndef SEE_MASK_NOASYNC
#define SEE_MASK_NOASYNC 0x00000100
#endif /* #ifndef SEE_MASK_NOASYNC */
#include <tlhelp32.h> /* CreateToolhelp32Snapshot */

#include <bspatch.h>
#include <lzma.h>

#define SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR_PROPERTY_BEGIN \
    L"SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR=\""

static LPWSTR Setup_commandLine = NULL;
static LPTSTR Setup_fileName = NULL;

/**
 * The indicator which determines whether this setup is to execute as msiexec
 * only and is to just execute an MSI specified on the command line.
 */
static BOOL Setup_msiexec_ = FALSE;
static LPTSTR Setup_productName = NULL;

/**
 * The indicator which determines whether this setup is to display no user
 * interface such as error message dialogs and the error status of the
 * application is to be reported as its exit code.
 */
static BOOL Setup_quiet = FALSE;
static BOOL Setup_waitForParentProcess_ = FALSE;

/**
 * The indicator which determines whether this setup is to execute as xzdec only
 * and is to just extract its payload in the current directory.
 */
static BOOL Setup_xzdec_ = FALSE;

BOOL CALLBACK Setup_enumResNameProc(HMODULE module, LPCTSTR type, LPTSTR name, LONG_PTR param);
static DWORD Setup_executeBspatch(LPCTSTR path);
static DWORD Setup_executeMsiA(LPCSTR path);
static DWORD Setup_executeMsiW(LPCWSTR path);
static DWORD Setup_extractAndExecutePayload(LPVOID ptr, DWORD size);
#ifdef PACKAGECODE
static LONG Setup_findLocalPackageByProductId(LPCTSTR productId, LPTSTR *localPackage);
static LONG Setup_findProductIdByPackageCode(LPCTSTR packageCode, LPTSTR *productId);
#endif /* #ifdef PACKAGECODE */
static void Setup_fixCommandLineQuotes();
static LPWSTR Setup_getArgW(LPWSTR commandLine, LPWSTR *value);
static LPSTR Setup_getBoolArgA(LPCSTR argName, LPSTR commandLine, BOOL *boolValue);
static LPWSTR Setup_getBoolArgW(LPCWSTR argName, LPWSTR commandLine, BOOL *boolValue);
static LPCTSTR Setup_getFileName();
static DWORD Setup_getParentProcess(DWORD *ppid, LPTSTR *fileName);
static LPCTSTR Setup_getProductName();
static DWORD Setup_getWinMainCmdLine(LPTSTR *winMainCmdLine);
static int Setup_isWow64Acceptable();
LRESULT CALLBACK Setup_isWow64AcceptableMessageBoxCallWndRetProc(int code, WPARAM wParam, LPARAM lParam);
static DWORD Setup_msiexec();
static DWORD Setup_parseCommandLine(LPTSTR cmdLine);
static LPSTR Setup_skipWhitespaceA(LPSTR str);
static LPWSTR Setup_skipWhitespaceW(LPWSTR str);
static DWORD Setup_terminateUp2DateExe();
static DWORD Setup_waitForParentProcess();
static DWORD Setup_xzdec(LPVOID ptr, DWORD size, HANDLE file);

#ifdef _UNICODE
#define Setup_executeMsi(path) \
    Setup_executeMsiW(path)
#define Setup_getBoolArg(argName, commandLine, boolValue) \
    Setup_getBoolArgW(argName, commandLine, boolValue)
#define Setup_skipWhitespace(str) \
    Setup_skipWhitespaceW(str)
#else /* #ifdef _UNICODE */
#define Setup_executeMsi(path) \
    Setup_executeMsiA(path)
#define Setup_getBoolArg(argName, commandLine, boolValue) \
    Setup_getBoolArgA(argName, commandLine, boolValue)
#define Setup_skipWhitespace(str) \
    Setup_skipWhitespaceA(str)
#endif /* #ifdef _UNICODE */

BOOL CALLBACK
Setup_enumResNameProc(
        HMODULE module,
        LPCTSTR type, LPTSTR name,
        LONG_PTR param)
{
    HRSRC rsrc = FindResource(module, name, type);
    BOOL proceed = TRUE;
    DWORD error = ERROR_SUCCESS;

    if (rsrc)
    {
        DWORD size = SizeofResource(module, rsrc);

        if (size)
        {
            HGLOBAL global = LoadResource(module, rsrc);

            if (global)
            {
                LPVOID ptr = LockResource(global);

                if (ptr)
                {
                    proceed = FALSE;
                    error = Setup_extractAndExecutePayload(ptr, size);
                }
                else
                {
                    error = GetLastError();
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                }
            }
            else
            {
                error = GetLastError();
                LastError_setLastError(error, _T(__FILE__), __LINE__);
            }
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
    }
    else
    {
        error = GetLastError();
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
    if (param)
        *((DWORD *) param) = error;
    return proceed;
}

static DWORD
Setup_executeBspatch(LPCTSTR path)
{
    DWORD error;

#ifdef PACKAGECODE
    LPTSTR packageCode = _tcsdup(PACKAGECODE);

    if (packageCode)
    {
        /*
         * Strip the display characters from the GUID, only its bytes are
         * important.
         */
        size_t i;
        size_t j;
        size_t packageCodeLength = _tcslen(packageCode);

        for (i = 0, j = 0; i < packageCodeLength; i++)
        {
            TCHAR c = packageCode[i];

            if ((_T('{') != c) && (_T('}') != c) && (_T('-') != c))
                packageCode[j++] = c;
        }
        packageCode[j] = 0;
        packageCodeLength = j;

        if (32 == packageCodeLength)
        {
            TCHAR swap;
            LPTSTR pc = packageCode;
            LPTSTR productId;

            /* 8 */
            swap = pc[7]; pc[7] = pc[0]; pc[0] = swap;
            swap = pc[6]; pc[6] = pc[1]; pc[1] = swap;
            swap = pc[5]; pc[5] = pc[2]; pc[2] = swap;
            swap = pc[4]; pc[4] = pc[3]; pc[3] = swap;
            /* 4 */
            swap = pc[11]; pc[11] = pc[8]; pc[8] = swap;
            swap = pc[10]; pc[10] = pc[9]; pc[9] = swap;
            /* 4 */
            swap = pc[15]; pc[15] = pc[12]; pc[12] = swap;
            swap = pc[14]; pc[14] = pc[13]; pc[13] = swap;
            /* 4 */
            swap = pc[17]; pc[17] = pc[16]; pc[16] = swap;
            swap = pc[19]; pc[19] = pc[18]; pc[18] = swap;
            /* 12 */
            swap = pc[21]; pc[21] = pc[20]; pc[20] = swap;
            swap = pc[23]; pc[23] = pc[22]; pc[22] = swap;
            swap = pc[25]; pc[25] = pc[24]; pc[24] = swap;
            swap = pc[27]; pc[27] = pc[26]; pc[26] = swap;
            swap = pc[29]; pc[29] = pc[28]; pc[28] = swap;
            swap = pc[31]; pc[31] = pc[30]; pc[30] = swap;

            error = Setup_findProductIdByPackageCode(packageCode, &productId);
            if (ERROR_SUCCESS == error)
            {
                LPTSTR localPackage;

                error
                    = Setup_findLocalPackageByProductId(
                            productId,
                            &localPackage);
#ifdef PACKAGESIZE
                /*
                 * Windows Installer on Windows XP caches the MSI database only
                 * so the localPackage cannot really be used with bspatch as the
                 * old file to produce the new file. Unfortunately, bspatch will
                 * report that it has successfully produced the new file from
                 * the old file in this scenario but the resulting MSI will be
                 * malformed. As a workaround to detect this error, make sure
                 * that the localPackage is with the expected size in bytes.
                 */
                if (ERROR_SUCCESS == error)
                {
                    HANDLE hLocalPackage
                        = CreateFile(
                                localPackage,
                                GENERIC_READ,
                                FILE_SHARE_READ,
                                NULL,
                                OPEN_EXISTING,
                                0,
                                NULL);

                    if (INVALID_HANDLE_VALUE == hLocalPackage)
                    {
                        error = GetLastError();
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                    }
                    else
                    {
                        LARGE_INTEGER packageSize;

                        if (GetFileSizeEx(hLocalPackage, &packageSize))
                        {
                            if (PACKAGESIZE != packageSize.QuadPart)
                            {
                                error = ERROR_FILE_NOT_FOUND;
                                LastError_setLastError(
                                        error,
                                        _T(__FILE__), __LINE__);
                            }
                        }
                        else
                        {
                            error = GetLastError();
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                        CloseHandle(hLocalPackage);
                    }
                }
#endif /* #ifdef PACKAGESIZE */
                if (ERROR_SUCCESS == error)
                {
                    /*
                     * The path to the new file to be produced by bspatch.exe is
                     * optional. If it is not specified on the command line,
                     * default to a path derived from the path to the .bspatch
                     * file.
                     */
                    LPWSTR wNewPath;
                    LPTSTR newPath;

                    Setup_commandLine
                        = Setup_getArgW(Setup_commandLine, &wNewPath);
                    if (wNewPath)
                    {
#ifdef _UNICODE
                        newPath = wNewPath;
#else /* #ifdef _UNICODE */
                        newPath = NLS_wstr2str(wNewPath);
#endif /* #ifdef _UNICODE */
                    }
                    else
                    {
                        size_t pathLength = _tcslen(path);
                        LPCTSTR extension = _T(".msi");
                        size_t extensionLength = _tcslen(extension);

                        newPath
                            = malloc(
                                    sizeof(TCHAR)
                                        * (pathLength + extensionLength + 1));
                        if (newPath)
                        {
                            LPTSTR str;

                            str = newPath;
                            _tcsncpy(str, path, pathLength);
                            str += pathLength;
                            _tcsncpy(str, extension, extensionLength);
                            str += extensionLength;
                            *str = 0;
                        }
                    }
                    /*
                     * Execute bspatch.exe (or rather the function it has been
                     * compiled into).
                     */
                    if (newPath)
                    {
                        LPCTSTR argv[]
                            = {
                                _T("bspatch.exe"),
                                localPackage, newPath, path,
                                NULL
                            };

                        if (bspatch_main(
                                (sizeof(argv) / sizeof(LPCTSTR)) - 1,
                                argv))
                        {
                            error = ERROR_GEN_FAILURE;
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                        if (((LPVOID) newPath) != ((LPVOID) wNewPath))
                            free(newPath);
                    }
                    else
                    {
                        error = ERROR_NOT_ENOUGH_MEMORY;
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                    }
                    free(localPackage);
                }
                free(productId);
            }
        }
        else
        {
            error = ERROR_INVALID_PARAMETER;
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
        free(packageCode);
    }
    else
    {
        error = ERROR_NOT_ENOUGH_MEMORY;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
#else /* #ifdef PACKAGECODE */
    error = ERROR_CALL_NOT_IMPLEMENTED;
    LastError_setLastError(error, _T(__FILE__), __LINE__);
#endif /* #ifdef PACKAGECODE */
    return error;
}

static DWORD
Setup_executeMsiA(LPCSTR path)
{
    LPWSTR wpath = NLS_str2wstr(path);
    DWORD error;

    if (wpath)
    {
        error = Setup_executeMsiW(wpath);
        free(wpath);
    }
    else
    {
        error = ERROR_OUTOFMEMORY;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
    return error;
}

static DWORD
Setup_executeMsiW(LPCWSTR path)
{
    DWORD error = ERROR_SUCCESS;
    LPCWSTR p0, p1, p2, p3;
    size_t p0Length, p1Length, p2Length, p3Length;
    LPWSTR parameters;

    p0 = L"/i \"";
    p0Length = wcslen(p0);
    p1 = path;
    if (p1)
        p1Length = wcslen(p1);
    else
    {
        error = ERROR_INVALID_PARAMETER;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
        return error;
    }
    p2 = L"\" REINSTALLMODE=amus ";
    p2Length = wcslen(p2);
    p3 = Setup_commandLine;
    p3Length = p3 ? wcslen(p3) : 0;

    parameters
        = (LPWSTR)
            malloc(
                    sizeof(wchar_t)
                        * (p0Length + p1Length + p2Length + p3Length + 1));
    if (parameters)
    {
        LPWSTR str = parameters;
        SHELLEXECUTEINFOW sei;

        wcsncpy(str, p0, p0Length);
        str += p0Length;
        wcsncpy(str, p1, p1Length);
        str += p1Length;
        wcsncpy(str, p2, p2Length);
        str += p2Length;
        if (p3Length)
        {
            wcsncpy(str, p3, p3Length);
            str += p3Length;
        }
        *str = 0;

        ZeroMemory(&sei, sizeof(sei));
        sei.cbSize = sizeof(sei);
        sei.fMask
            = SEE_MASK_NOCLOSEPROCESS | SEE_MASK_NOASYNC | SEE_MASK_FLAG_NO_UI;
        sei.lpVerb = L"open";
        sei.lpFile = L"msiexec.exe";
        sei.lpParameters = parameters;
        sei.nShow = SW_SHOWNORMAL;

        /*
         * MSDN says it is good practice to always initialize COM before using
         * ShellExecuteEx.
         */
        CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);

        if (ShellExecuteExW(&sei) && (((intptr_t) (sei.hInstApp)) > 32))
        {
            if (sei.hProcess)
            {
                DWORD event;

                do
                {
                    event = WaitForSingleObject(sei.hProcess, INFINITE);
                    if (WAIT_FAILED == event)
                    {
                        error = GetLastError();
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                        break;
                    }
                }
                while (WAIT_TIMEOUT == event);
                CloseHandle(sei.hProcess);
            }
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }

        free(parameters);
    }
    else
    {
        error = ERROR_OUTOFMEMORY;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
    return error;
}

static DWORD
Setup_extractAndExecutePayload(LPVOID ptr, DWORD size)
{
    TCHAR path[MAX_PATH + 1];
    DWORD pathSize = sizeof(path) / sizeof(TCHAR);
    DWORD tempPathLength;
    DWORD error = ERROR_SUCCESS;

    /*
     * When this application is to execute in the fashion of xzdec only, it does
     * not sound like a nice idea to extract its payload in the TEMP directory
     * and the current directory sounds like an acceptable compromise (given
     * that it is far more complex to accept the path to extract to as a command
     * line argument).
     */
    if (Setup_xzdec_)
    {
        path[0] = _T('.');
        path[1] = _T('\\');
        /*
         * It is not necessary to null-terminate path because it will
         * automatically be done later in accord with the value of
         * tempPathLength.
         */
        tempPathLength = 2;
    }
    else
        tempPathLength = GetTempPath(pathSize, path);
    if (tempPathLength)
    {
        if (tempPathLength > pathSize)
        {
            error = ERROR_NOT_ENOUGH_MEMORY;
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
        else
        {
            LPCTSTR fileName = Setup_getFileName();
            HANDLE file = INVALID_HANDLE_VALUE;

            if (fileName)
            {
                size_t fileNameLength = _tcslen(fileName);
                size_t freePathSize;
#ifdef PACKAGECODE
                LPCTSTR fileType = _T("bspatch");
                BOOL xzdec = FALSE;
#else /* #ifdef PACKAGECODE */
                LPCTSTR fileType = _T("msi");
                BOOL xzdec = TRUE;
#endif /* #ifdef PACKAGECODE */

                if ((fileNameLength > 4 /* .exe */)
                        && ((freePathSize
                                    = (pathSize
                                        - (tempPathLength
                                            + fileNameLength
                                            + 1)))
                                >= 0))
                {
                    LPTSTR str = path + tempPathLength;
                    size_t fileTypeLength = _tcslen(fileType);

                    _tcsncpy(str, fileName, fileNameLength - 4);
                    str += (fileNameLength - 4);
                    *str = _T('.');
                    str++;
                    _tcsncpy(str, fileType, 3);
                    str += 3;

                    /*
                     * If possible, use the whole fileType for the extension and
                     * not just its first 3 characters.
                     */
                    fileTypeLength -= 3;
                    if ((fileTypeLength > 0)
                            && (fileTypeLength <= freePathSize))
                    {
                        _tcsncpy(str, fileType + 3, fileTypeLength);
                        str += fileTypeLength;
                    }

                    *str = 0;

                    file
                        = CreateFile(
                                path,
                                GENERIC_WRITE,
                                0,
                                NULL,
                                CREATE_NEW,
                                FILE_ATTRIBUTE_TEMPORARY,
                                NULL);
                }

                if (INVALID_HANDLE_VALUE == file)
                {
                    LPTSTR tempPath;

                    path[tempPathLength] = 0;
                    tempPath = _tcsdup(path);

                    if (tempPath)
                    {
                        if (0
                                == GetTempFileName(
                                        tempPath,
                                        fileType,
                                        0,
                                        path))
                        {
                            error = GetLastError();
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                        else
                        {
                            file
                                = CreateFile(
                                        path,
                                        GENERIC_WRITE,
                                        0,
                                        NULL,
                                        CREATE_ALWAYS,
                                        FILE_ATTRIBUTE_TEMPORARY,
                                        NULL);
                            if (INVALID_HANDLE_VALUE == file)
                            {
                                error = GetLastError();
                                LastError_setLastError(
                                        error,
                                        _T(__FILE__), __LINE__);
                            }
                        }
                        free(tempPath);
                    }
                    else
                    {
                        error = ERROR_OUTOFMEMORY;
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                    }
                }

                if (INVALID_HANDLE_VALUE != file)
                {
                    if (xzdec)
                        error = Setup_xzdec(ptr, size, file);
                    else
                    {
                        DWORD numberOfBytesWritten;

                        if (!WriteFile(
                                file,
                                ptr, size,
                                &numberOfBytesWritten,
                                NULL))
                        {
                            error = GetLastError();
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                    }

                    /* When executing as xzdec, do not execute the MSI. */
                    if ((ERROR_SUCCESS == error) && !Setup_xzdec_)
                    {
                        if (Setup_waitForParentProcess_)
                            Setup_waitForParentProcess();

                        CloseHandle(file);
                        if (_tcsnicmp(_T("bspatch"), fileType, 3) == 0)
                            error = Setup_executeBspatch(path);
                        else if (_tcsnicmp(_T("msi"), fileType, 3) == 0)
                            error = Setup_executeMsi(path);
                        else
                        {
                            error = ERROR_CALL_NOT_IMPLEMENTED;
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                    }
                    else
                        CloseHandle(file);
                    /*
                     * Delete the MSI if executing as setup or if executing as
                     * xzdec and the extraction has failed (in the fashion of
                     * other popular decompressors).
                     */
                    if (!Setup_xzdec_ || (ERROR_SUCCESS != error))
                        DeleteFile(path);
                }
            }
        }
    }
    else
    {
        error = GetLastError();
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
    return error;
}

#ifdef PACKAGECODE
static LONG
Setup_findLocalPackageByProductId(LPCTSTR productId, LPTSTR *localPackage)
{
    HKEY userDataKey;
    LONG error
        = RegOpenKeyEx(
                HKEY_LOCAL_MACHINE,
                _T("Software\\Microsoft\\Windows\\CurrentVersion\\Installer\\UserData"),
                0,
                KEY_ENUMERATE_SUB_KEYS | KEY_WOW64_64KEY,
                &userDataKey);

    if (ERROR_SUCCESS == error)
    {
        TCHAR userDataSubKeyName[1024];
        const DWORD userDataSubKeyNameCapacity
            = sizeof(userDataSubKeyName) / sizeof(TCHAR);
        DWORD index = 0;
        LPCTSTR products = _T("\\Products\\");
        const DWORD productsLength = 10;
        const DWORD productIdLength = 32;
        LPCTSTR installProperties = _T("\\InstallProperties");
        const DWORD installPropertiesLength = 18;
        TCHAR installPropertiesKeyName[
                userDataSubKeyNameCapacity
                    + productsLength
                    + productIdLength
                    + installPropertiesLength];

        *localPackage = NULL;
        do
        {
            DWORD userDataSubKeyNameLength = userDataSubKeyNameCapacity;
            LPTSTR str;
            HKEY installPropertiesKey;

            error
                = RegEnumKeyEx(
                        userDataKey,
                        index,
                        userDataSubKeyName, &userDataSubKeyNameLength,
                        NULL,
                        NULL, NULL,
                        NULL);
            index++;
            if (ERROR_MORE_DATA == error)
                continue;
            if (ERROR_SUCCESS != error)
            {
                LastError_setLastError(error, _T(__FILE__), __LINE__);
                break;
            }

            str = installPropertiesKeyName;
            _tcsncpy(str, userDataSubKeyName, userDataSubKeyNameLength);
            str += userDataSubKeyNameLength;
            _tcsncpy(str, products, productsLength);
            str += productsLength;
            _tcsncpy(str, productId, productIdLength);
            str += productIdLength;
            _tcsncpy(str, installProperties, installPropertiesLength);
            str += installPropertiesLength;
            *str = 0;

            error
                = RegOpenKeyEx(
                        userDataKey,
                        installPropertiesKeyName,
                        0,
                        KEY_QUERY_VALUE | KEY_WOW64_64KEY,
                        &installPropertiesKey);
            if (ERROR_SUCCESS == error)
            {
                error
                    = Run_getRegSzValue(
                            installPropertiesKey,
                            _T("LocalPackage"),
                            localPackage);
                /*
                 * Reset the value stored at localPackage to NULL in case
                 * Run_getRegSzValue has failed but has assigned an invalid
                 * value prior to the failure.
                 */
                if (ERROR_SUCCESS != error)
                    *localPackage = NULL;
                RegCloseKey(installPropertiesKey);
            }
        }
        while (!(*localPackage));
        RegCloseKey(userDataKey);
        if ((ERROR_SUCCESS == error) && !(*localPackage))
        {
            error = ERROR_FILE_NOT_FOUND;
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
    }
    else
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    return error;
}
#endif /* #ifdef PACKAGECODE */

#ifdef PACKAGECODE
static LONG
Setup_findProductIdByPackageCode(LPCTSTR packageCode, LPTSTR *productId)
{
    HKEY key;
    LONG error
        = RegOpenKeyEx(
                HKEY_LOCAL_MACHINE,
                _T("Software\\Classes\\Installer\\Products"),
                0,
                KEY_ENUMERATE_SUB_KEYS | KEY_WOW64_64KEY,
                &key);

    if (ERROR_SUCCESS == error)
    {
        TCHAR subKeyName[33];
        const DWORD subKeyNameCapacity = sizeof(subKeyName) / sizeof(TCHAR);
        DWORD index = 0;

        *productId = NULL;
        do
        {
            DWORD subKeyNameLength = subKeyNameCapacity;
            HKEY subKey;
            LPTSTR packageCodeOfProductId;

            error
                = RegEnumKeyEx(
                        key,
                        index,
                        subKeyName, &subKeyNameLength,
                        NULL,
                        NULL, NULL,
                        NULL);
            index++;
            if (ERROR_MORE_DATA == error)
                continue;
            if (ERROR_SUCCESS != error)
            {
                LastError_setLastError(error, _T(__FILE__), __LINE__);
                break;
            }
            if (32 != subKeyNameLength)
                continue;
            error
                = RegOpenKeyEx(
                        key,
                        subKeyName,
                        0,
                        KEY_QUERY_VALUE | KEY_WOW64_64KEY,
                        &subKey);
            if (ERROR_SUCCESS != error)
            {
                LastError_setLastError(error, _T(__FILE__), __LINE__);
                break;
            }
            error
                = Run_getRegSzValue(
                        subKey,
                        _T("PackageCode"),
                        &packageCodeOfProductId);
            if (ERROR_SUCCESS == error)
            {
                if (_tcsnicmp(
                            packageCodeOfProductId, packageCode,
                            subKeyNameLength)
                        == 0)
                {
                    *productId = _tcsdup(subKeyName);
                    if (!(*productId))
                    {
                        error = ERROR_NOT_ENOUGH_MEMORY;
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                    }
                }
                free(packageCodeOfProductId);
            }
            else
            {
                /*
                 * We failed to read one PackageCode out of many while looking
                 * for ours. Eventually, it makes no difference whether the
                 * failed one was ours or not. Most importantly, we cannot give
                 * up the search because the failed one may have not been ours
                 * anyway.
                 */
                error = ERROR_SUCCESS;
            }
            RegCloseKey(subKey);
            if (ERROR_SUCCESS != error)
                break;
        }
        while (!(*productId));
        RegCloseKey(key);
        if ((ERROR_SUCCESS == error) && !(*productId))
        {
            error = ERROR_FILE_NOT_FOUND;
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
    }
    else
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    return error;
}
#endif /* #ifdef PACKAGECODE */

static void
Setup_fixCommandLineQuotes()
{
    LPWSTR readCmdLine = Setup_commandLine;
    LPWSTR writeCmdLine = Setup_commandLine;

    do
    {
        LPWSTR arg;

        readCmdLine = Setup_getArgW(readCmdLine, &arg);
        if (arg)
        {
            /*
             * If the argument has unquoted whitespace, quote the whole
             * argument.
             */
            wchar_t c;
            LPWSTR a = arg;
            BOOL quoted = FALSE;
            BOOL whitespace = FALSE;
            size_t argLength;

            while ((c = *a))
            {
                if (iswspace(c))
                {
                    if (!quoted)
                    {
                        whitespace = TRUE;
                        break;
                    }
                }
                else if (L'\"' == c)
                    quoted = quoted ? FALSE : TRUE;
                a++;
            }

            /*
             * If the argument is not the first one, separate it from the
             * previous one with a space.
             */
            if (writeCmdLine != Setup_commandLine)
            {
                *writeCmdLine = L' ';
                writeCmdLine++;
            }
            /* Append the argument to the command line. */
            argLength = wcslen(arg);
            if (whitespace
                    && ((writeCmdLine + (argLength + 1)) < readCmdLine))
            {
                *writeCmdLine = L'\"';
                writeCmdLine++;
                wcsncpy(writeCmdLine, arg, argLength);
                writeCmdLine += argLength;
                *writeCmdLine = L'\"';
                writeCmdLine++;
            }
            else
            {
                wcsncpy(writeCmdLine, arg, argLength);
                writeCmdLine += argLength;
            }
        }
        else
            break;
    }
    while (1);
    /* At long last, terminate the command line. */
    *writeCmdLine = 0;
}

static LPWSTR
Setup_getArgW(LPWSTR commandLine, LPWSTR *value)
{
    if (commandLine)
    {
        LPWSTR v;
        size_t quoted;
        wchar_t prevC;
        wchar_t c;

        /* Obviously, any leading whitespace is not a part of an argument. */
        commandLine = Setup_skipWhitespaceW(commandLine);
        v = commandLine;

        quoted = 0;
        prevC = 0;
        while ((c = *commandLine))
        {
            if (iswspace(c))
            {
                if (!quoted)
                    break;
            }
            else if (L'\"' == c)
            {
                if (quoted)
                {
                    /*
                     * Quotes cannot simply be nested but the Java
                     * ProcessBuilder does nest them (in cases in which nesting
                     * is not even necessary) so try to deal with them. Clearly,
                     * the implementation will be heuristic in nature.
                     */
                    wchar_t nextC;

                    if ((1 == quoted)
                            && ((nextC = *(commandLine + 1)))
                            && !iswspace(nextC)
                            && ((L'=' == prevC)
                                    || ((L'\"' == prevC)
                                            && (v + 1 == commandLine))))
                        quoted++;
                    else
                        quoted--;
                }
                else
                    quoted = 1;
            }
            prevC = c;
            commandLine++;
        }
        /*
         * If there are more arguments, get rid of any whitespace before them.
         */
        if (c)
        {
            LPWSTR vEnd = commandLine;

            commandLine = Setup_skipWhitespaceW(commandLine);
            *vEnd = 0;
        }
        /*
         * If the argument is quoted, drop the quotes. Since the Java
         * ProcessBuilder will quote even the quoted command line arguments,
         * drop as many quotes as possible.
         */
        while (L'\"' == *v)
        {
            size_t vLength = wcslen(v);

            if (vLength > 1)
            {
                LPWSTR vEnd = v + (vLength - 1);

                if (L'\"' == *vEnd)
                {
                    /*
                     * Convert the opening quote to a whitespace character (just
                     * in case) and the closing quote to the null character (in
                     * order to terminate the argument).
                     */
                    *v = L' ';
                    v++;
                    *vEnd = 0;
                }
            }
        }

        if (value)
            *value = *v ? v : NULL;
    }
    else
    {
        if (value)
            *value = NULL;
    }
    return commandLine;
}

#define DEFINE_SETUP_GETBOOLARG(f, t, len, nicmp) \
    static LP ## t \
    Setup_getBoolArg ## f (LPC ## t argName, LP ## t commandLine, BOOL *boolValue) \
    { \
        size_t argNameLength; \
        BOOL argValue; \
     \
        argNameLength = len(argName); \
        commandLine = Setup_skipWhitespace ## f (commandLine); \
        if (0 == nicmp(commandLine, argName, argNameLength)) \
        { \
            argValue = TRUE; \
            commandLine = Setup_skipWhitespace ## f (commandLine + argNameLength); \
        } \
        else \
            argValue = FALSE; \
        if (boolValue) \
            *boolValue = argValue; \
        return commandLine; \
    }
#ifdef _UNICODE
#undef _UNICODE
DEFINE_SETUP_GETBOOLARG(A, STR, strlen, _strnicmp)
#define _UNICODE
DEFINE_SETUP_GETBOOLARG(W, WSTR, wcslen, _wcsnicmp)
#else /* #ifdef _UNICODE */
DEFINE_SETUP_GETBOOLARG(A, STR, strlen, _strnicmp)
#define _UNICODE
DEFINE_SETUP_GETBOOLARG(W, WSTR, wcslen, _wcsnicmp)
#undef _UNICODE
#endif /* #ifdef _UNICODE */

static LPCTSTR
Setup_getFileName()
{
    if (!Setup_fileName)
    {
        TCHAR moduleFileName[MAX_PATH + 1];
        DWORD moduleFileNameSize = sizeof(moduleFileName) / sizeof(TCHAR);
        DWORD moduleFileNameLength
            = GetModuleFileName(NULL, moduleFileName, moduleFileNameSize);

        if (moduleFileNameLength)
        {
            TCHAR *fileNameEnd = moduleFileName + moduleFileNameLength - 1;
            TCHAR *fileNameBegin = fileNameEnd;
            size_t fileNameLength;
            LPTSTR fileName;

            for (; fileNameBegin >= moduleFileName; fileNameBegin--)
            {
                TCHAR c = *fileNameBegin;

                if ((_T('\\') == c) || (_T('/') == c))
                    break;
            }
            fileNameBegin
                = (fileNameBegin == fileNameEnd)
                    ? moduleFileName
                    : (fileNameBegin + 1);

            fileNameLength = (fileNameEnd - fileNameBegin) + 1;
            fileName = (LPTSTR) malloc((fileNameLength + 1) * sizeof(TCHAR));
            if (fileName)
            {
                _tcsncpy(fileName, fileNameBegin, fileNameLength);
                *(fileName + fileNameLength) = 0;
                Setup_fileName = fileName;
            }
        }
    }
    return Setup_fileName;
}

static DWORD
Setup_getParentProcess(DWORD *ppid, LPTSTR *fileName)
{
    HANDLE snapshot;
    DWORD error;

    snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE)
    {
        error = GetLastError();
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
    else
    {
        PROCESSENTRY32 entry;

        entry.dwSize = sizeof(PROCESSENTRY32);
        if (Process32First(snapshot, &entry))
        {
            DWORD pid;

            error = ERROR_SUCCESS;
            pid = GetCurrentProcessId();
            if (ppid)
                *ppid = 0;

            do
            {
                if (entry.th32ProcessID == pid)
                {
                    if (ppid)
                        *ppid = entry.th32ParentProcessID;
                    break;
                }
                if (!Process32Next(snapshot, &entry))
                {
                    error = GetLastError();
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                    break;
                }
            }
            while (1);
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
        if ((ERROR_SUCCESS == error) && fileName && ppid && *ppid)
        {
            if (Process32First(snapshot, &entry))
            {
                do
                {
                    if (entry.th32ProcessID == *ppid)
                    {
                        *fileName = _tcsdup(entry.szExeFile);
                        if (NULL == *fileName)
                        {
                            error = ERROR_OUTOFMEMORY;
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                        break;
                    }
                    if (!Process32Next(snapshot, &entry))
                    {
                        error = GetLastError();
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                        break;
                    }
                }
                while (1);
            }
            else
            {
                error = GetLastError();
                LastError_setLastError(error, _T(__FILE__), __LINE__);
            }
        }
        CloseHandle(snapshot);
    }
    return error;
}

static LPCTSTR
Setup_getProductName()
{
    if (!Setup_productName)
    {
        LPCTSTR fileName = Setup_getFileName();

        if (fileName)
        {
            int fileNameLength = _tcslen(fileName);

            if ((fileNameLength > 4)
                    && (_tcsnicmp(fileName + fileNameLength - 4, _T(".exe"), 4)
                            == 0))
            {
                LPTSTR productName;

                fileNameLength -= 4;
                productName
                    = (LPTSTR) malloc((fileNameLength + 1) * sizeof(TCHAR));
                if (productName)
                {
                    _tcsncpy(productName, fileName, fileNameLength);
                    *(productName + fileNameLength) = 0;
                    Setup_productName = productName;
                }
            }
            if (!Setup_productName)
                Setup_productName = (LPTSTR) fileName;
        }
    }
    return Setup_productName;
}

static DWORD
Setup_getWinMainCmdLine(LPTSTR *winMainCmdLine)
{
    LPWSTR cmdLineW = GetCommandLineW();
    DWORD error;

    if (cmdLineW && wcslen(cmdLineW))
    {
        int argc;
        LPWSTR *argvW = CommandLineToArgvW(cmdLineW, &argc);

        if (argvW)
        {
            if (argc)
            {
                LPWSTR argvW0 = argvW[0];
                LPWSTR argvW0InCmdLineW = wcsstr(cmdLineW, argvW0);

                if (argvW0InCmdLineW)
                {
                    wchar_t c;

                    cmdLineW = argvW0InCmdLineW + wcslen(argvW0);
                    /*
                     * CommandLineToArgvW may not report quotes as part of
                     * argvW0. As a workaround, skip non-whitespace characters.
                     */
                    while ((c = *cmdLineW) && !iswspace(c))
                        cmdLineW++;
                }
#ifdef _UNICODE
                *winMainCmdLine = cmdLineW;
                error = ERROR_SUCCESS;
#else /* #ifdef _UNICODE */
                *winMainCmdLine = NLS_wstr2str(cmdLineW);
                if (*winMainCmdLine)
                    error = ERROR_SUCCESS;
                else
                {
                    error = ERROR_GEN_FAILURE;
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                }
#endif /* #ifdef _UNICODE */
            }
            else
            {
                *winMainCmdLine = NULL;
                error = ERROR_SUCCESS;
            }
            LocalFree(argvW);
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
    }
    else
    {
        *winMainCmdLine = NULL;
        error = ERROR_SUCCESS;
    }
    return error;
}

static int
Setup_isWow64Acceptable()
{
    int answer = IDYES;
    HMODULE kernel32;

    /*
     * If this is an (automatic) update, do not ask because (1) the user has
     * already answered during the initial install and (2) it is plain annoying.
     */
    if (Setup_commandLine
            && wcsstr(
                    Setup_commandLine,
                    SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR_PROPERTY_BEGIN))
        return answer;

    kernel32 = GetModuleHandle(_T("kernel32"));
    if (kernel32)
    {
        typedef BOOL (WINAPI *LPISWOW64PROCESS)(HANDLE, PBOOL);

        LPISWOW64PROCESS isWow64Process
            = (LPISWOW64PROCESS) GetProcAddress(kernel32, "IsWow64Process");
        BOOL wow64Process = FALSE;

        if (isWow64Process
                && isWow64Process(GetCurrentProcess(), &wow64Process)
                && wow64Process)
        {
            TCHAR fileName[MAX_PATH + 1];

            if (GetModuleFileName(NULL, fileName, sizeof(fileName) / sizeof(TCHAR)))
            {
                UINT questionId;
                UINT buttonType;
                DWORD questionLength;
                TCHAR question[1024];

#ifdef X64_SETUP_URL
                HHOOK hook
                    = SetWindowsHookEx(
                            WH_CALLWNDPROCRET,
                            (HOOKPROC) Setup_isWow64AcceptableMessageBoxCallWndRetProc,
                            NULL,
                            GetCurrentThreadId());

                if (hook)
                {
                    questionId = IDS_ISWOW64ACCEPTABLE3;
                    buttonType = MB_YESNOCANCEL | MB_DEFBUTTON3;
                }
                else
#endif /* #ifdef X64_SETUP_URL */
                {
                    questionId = IDS_ISWOW64ACCEPTABLE2;
                    buttonType = MB_YESNO;
                }

                questionLength
                    = LoadString(
                            GetModuleHandle(NULL),
                            questionId,
                            question,
                            sizeof(question) / sizeof(TCHAR));
                if (questionLength)
                {
                    answer
                        = MessageBox(
                                NULL,
                                question,
                                fileName,
                                MB_ICONQUESTION | buttonType);
                    LocalFree(question);
                }

#ifdef X64_SETUP_URL
                if (hook)
                {
                    UnhookWindowsHookEx(hook);

                    switch (answer)
                    {
                    case IDNO: // Continue
                        answer = IDYES;
                        break;
                    case IDYES: // Download
                        answer = IDNO;
                        ShellExecute(
                                NULL,
                                _T("open"),
                                _T(X64_SETUP_URL),
                                NULL,
                                NULL,
                                SW_SHOWNORMAL);
                        break;
                    }
                }
#endif /* #ifdef X64_SETUP_URL */
            }
        }
    }
    return answer;
}

LRESULT CALLBACK
Setup_isWow64AcceptableMessageBoxCallWndRetProc(
        int code,
        WPARAM wParam,
        LPARAM lParam)
{
    CWPRETSTRUCT *cwprs = (CWPRETSTRUCT *) lParam;

    if (cwprs && (WM_INITDIALOG == cwprs->message))
    {
        HWND yes, no;

        yes = GetDlgItem(cwprs->hwnd, IDYES);
        if (yes)
            SendMessage(yes, WM_SETTEXT, 0, (LPARAM) _T("&Download"));

        no = GetDlgItem(cwprs->hwnd, IDNO);
        if (no)
            SendMessage(no, WM_SETTEXT, 0, (LPARAM) _T("&Continue"));
    }
    return CallNextHookEx(NULL, code, wParam, lParam);
}

static DWORD
Setup_msiexec()
{
    LPWSTR msi;
    DWORD error;

    Setup_commandLine = Setup_getArgW(Setup_commandLine, &msi);
    if (msi)
        error = Setup_executeMsiW(msi);
    else
    {
        error = ERROR_BAD_ARGUMENTS;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
    }
    return error;
}

static DWORD
Setup_parseCommandLine(LPTSTR cmdLine)
{
    LPTSTR commandLine;
    DWORD error = ERROR_SUCCESS;

//#ifdef _UNICODE
//    if (cmdLine)
//    {
//        commandLine = NLS_str2wstr(cmdLine);
//        if (!commandLine)
//        {
//            error = ERROR_OUTOFMEMORY;
//            LastError_setLastError(error, _T(__FILE__), __LINE__);
//        }
//    }
//    else
//        commandLine = NULL;
//#else
    commandLine = cmdLine;
//#endif /* #ifdef _UNICODE */

    if (commandLine)
    {
        LPTSTR noWaitParentCommandLine
            = Setup_getBoolArg(
                    _T("--wait-parent"),
                    commandLine,
                    &Setup_waitForParentProcess_);
        /*
         * The command line argument --allow-elevation is up2date legacy which
         * has to be taken into account by removing it in order to prevent it
         * from breaking msiexec.
         */
        BOOL up2date;
        LPTSTR noAllowElevationCommandLine
            = Setup_getBoolArg(
                    _T("--allow-elevation"),
                    noWaitParentCommandLine,
                    &up2date);
        size_t noAllowElevationCommandLineLength
            = _tcslen(noAllowElevationCommandLine);
        TCHAR envVarValue[1 /* " */ + MAX_PATH + 1 /* " */ + 1];

        /*
         * If there are no arguments on the command line to reveal that up2date
         * is involved, try detecting it by the fact that it sets the
         * SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR environment variable. If the
         * environment variable in question is not set, it is sure that this
         * setup is to execute its post-up2date logic.
         */
        if (!up2date && !noAllowElevationCommandLineLength)
        {
            DWORD envVarValueSize
                = (sizeof(envVarValue) / sizeof(TCHAR)) - 2 /* "" */;
            DWORD envVarValueLength
                = GetEnvironmentVariable(
                        _T("SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR"),
                        &(envVarValue[1]),
                        envVarValueSize);

            if (envVarValueLength)
            {
                if (envVarValueLength > envVarValueSize)
                {
                    error = ERROR_NOT_ENOUGH_MEMORY;
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                }
                else
                {
                    if ((envVarValueLength >= 2)
                            && ('\"' == envVarValue[1])
                            && ('\"' == envVarValue[1 + envVarValueLength - 1]))
                    {
                        noAllowElevationCommandLine = &(envVarValue[1]);
                    }
                    else
                    {
                        envVarValue[0] = '\"';
                        envVarValue[1 + envVarValueLength] = '\"';
                        envVarValue[1 + envVarValueLength + 1] = 0;
                        envVarValueLength += 2;
                        noAllowElevationCommandLine = envVarValue;
                    }
                    noAllowElevationCommandLineLength = envVarValueLength;
                    up2date = TRUE;
                }
            }
            else
            {
                DWORD envVarError = GetLastError();

                if (ERROR_ENVVAR_NOT_FOUND != envVarError)
                {
                    error = envVarError;
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                }
            }
        }
        /*
         * If the up2date logic is in effect, then there are no post-up2date
         * command line arguments to parse and any command line just tells the
         * value of SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR. The latter can
         * easily be translated to the post-up2date logic at this point by
         * converting it to an msiexec property.
         */
        if (up2date && noAllowElevationCommandLineLength)
        {
            LPWSTR commandLineW;

#ifdef _UNICODE
            commandLineW = noAllowElevationCommandLine;
#else
            commandLineW = NLS_str2wstr(noAllowElevationCommandLine);
            if (!commandLineW)
            {
                error = ERROR_OUTOFMEMORY;
                LastError_setLastError(error, _T(__FILE__), __LINE__);
            }
#endif /* #ifdef _UNICODE */

            if (commandLineW)
            {
                int argc;
                LPWSTR *argv = CommandLineToArgvW(commandLineW, &argc);

                if (argv)
                {
                    if ((1 == argc) || (2 == argc))
                    {
                        LPWSTR argv1 = *(argv + (argc - 1));
                        size_t argv1Length = wcslen(argv1);

                        if ((argv1Length >= 2)
                                && (L'\"' == argv1[0])
                                && (L'\"' == argv1[argv1Length - 1]))
                        {
                            argv1++;
                            argv1Length -= 2;
                        }
                        if (argv1Length)
                        {
                            LPCWSTR propertyBegin
                                = SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR_PROPERTY_BEGIN;
                            size_t propertyBeginLength = wcslen(propertyBegin);
                            LPCWSTR propertyEnd = L"\"";
                            size_t propertyEndLength = wcslen(propertyEnd);

                            Setup_commandLine
                                = (LPWSTR)
                                    malloc(
                                            sizeof(wchar_t)
                                                * (propertyBeginLength
                                                        + argv1Length
                                                        + propertyEndLength
                                                        + 1));
                            if (Setup_commandLine)
                            {
                                LPWSTR str = Setup_commandLine;

                                wcsncpy(
                                        str,
                                        propertyBegin,
                                        propertyBeginLength);
                                str += propertyBeginLength;
                                wcsncpy(str, argv1, argv1Length);
                                str += argv1Length;
                                wcsncpy(str, propertyEnd, propertyEndLength);
                                *(str + propertyEndLength) = 0;
                            }
                            else
                            {
                                error = ERROR_OUTOFMEMORY;
                                LastError_setLastError(
                                        error,
                                        _T(__FILE__), __LINE__);
                            }
                        }
                    }
                    LocalFree(argv);
                }
                else
                {
                    error = GetLastError();
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                }

                if (((LPVOID) commandLineW)
                        != ((LPVOID) noAllowElevationCommandLine))
                    free(commandLineW);
            }
        }
        /*
         * If up2date.exe is running while the MSI is being installed, the MSI
         * will display a dialog notifying of the fact and asking the user to
         * either let it close the application in question (and it will not be
         * able to if the user actually chooses the option) or reboot.
         */
        if (up2date)
            Setup_terminateUp2DateExe();

        /*
         * If this is the post-up2date logic, then there may be post-up2date
         * command line arguments to parse.
         */
        if (!up2date && noAllowElevationCommandLineLength)
        {
#ifdef _UNICODE
            Setup_commandLine = _wcsdup(noAllowElevationCommandLine);
#else
            Setup_commandLine = NLS_str2wstr(noAllowElevationCommandLine);
#endif /* #ifdef _UNICODE */
            if (Setup_commandLine)
            {
                /*
                 * It is just easier to parse the command line arguments if they
                 * are ordered.
                 */

                Setup_commandLine
                    = Setup_getBoolArgW(
                            L"--quiet",
                            Setup_commandLine,
                            &Setup_quiet);
#ifdef PACKAGECODE
                Setup_commandLine
                    = Setup_getBoolArgW(
                            L"--msiexec",
                            Setup_commandLine,
                            &Setup_msiexec_);
#endif /* #ifdef PACKAGECODE */
                if (!Setup_msiexec_)
                {
                    Setup_commandLine
                        = Setup_getBoolArgW(
                                L"--xzdec",
                                Setup_commandLine,
                                &Setup_xzdec_);
                }

                /*
                 * Unfortunately, the Java ProcessBuilder will break the format
                 * PROPERTY="VALUE" by quoting the whole command line argument.
                 * Solve the problem in general regardless of the formats of the
                 * command line arguments.
                 */
                Setup_fixCommandLineQuotes();
            }
            else
            {
                error = ERROR_OUTOFMEMORY;
                LastError_setLastError(error, _T(__FILE__), __LINE__);
            }
        }

        if ((void *) commandLine != (void *) cmdLine)
            free(commandLine);
    }
    return error;
}

#define DEFINE_SETUP_SKIPWHITESPACE(f, tc, ts, space) \
    static LP ## ts \
    Setup_skipWhitespace ## f (LP ## ts str) \
    { \
        tc c; \
     \
        while ((c = *str) && space(c)) \
            ++str; \
        return str; \
    }
#ifdef _UNICODE
#undef _UNICODE
DEFINE_SETUP_SKIPWHITESPACE(A, char, STR, isspace)
#define _UNICODE
DEFINE_SETUP_SKIPWHITESPACE(W, wchar_t, WSTR, iswspace)
#else /* #ifdef _UNICODE */
DEFINE_SETUP_SKIPWHITESPACE(A, char, STR, isspace)
#define _UNICODE
DEFINE_SETUP_SKIPWHITESPACE(W, wchar_t, WSTR, iswspace)
#undef _UNICODE
#endif /* #ifdef _UNICODE */

static DWORD
Setup_terminateUp2DateExe()
{
    DWORD error;
    DWORD ppid = 0;
    LPTSTR ppFileName = NULL;

    error = Setup_getParentProcess(&ppid, &ppFileName);
    if ((ERROR_SUCCESS == error) && ppFileName)
    {
        size_t ppFileNameLength = _tcslen(ppFileName);
        LPCTSTR up2DateExe = _T("up2date.exe");
        size_t up2DateExeLength = _tcslen(up2DateExe);

        if ((ppFileNameLength >= up2DateExeLength)
                && (_tcsncmp(
                            ppFileName + (ppFileNameLength - up2DateExeLength),
                            up2DateExe,
                            up2DateExeLength)
                        == 0))
        {
            HANDLE parentProcess
                = OpenProcess(PROCESS_TERMINATE, FALSE, ppid);

            if (parentProcess)
            {
                if (!TerminateProcess(parentProcess, 0))
                {
                    error = GetLastError();
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                }
                CloseHandle(parentProcess);
            }
            else
            {
                error = GetLastError();
                LastError_setLastError(error, _T(__FILE__), __LINE__);
            }
        }
    }
    return error;
}

static DWORD
Setup_waitForParentProcess()
{
    DWORD error;
    DWORD ppid = 0;

    error = Setup_getParentProcess(&ppid, NULL);
    if (ERROR_SUCCESS == error)
    {
        HANDLE parentProcess = OpenProcess(SYNCHRONIZE, FALSE, ppid);

        if (parentProcess)
        {
            DWORD event;

            error = ERROR_SUCCESS;
            do
            {
                event = WaitForSingleObject(parentProcess, INFINITE);
                if (WAIT_FAILED == event)
                {
                    error = GetLastError();
                    LastError_setLastError(error, _T(__FILE__), __LINE__);
                    break;
                }
            }
            while (WAIT_TIMEOUT == event);
            CloseHandle(parentProcess);
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, _T(__FILE__), __LINE__);
        }
    }
    return error;
}

static DWORD
Setup_xzdec(LPVOID ptr, DWORD size, HANDLE file)
{
    lzma_stream strm = LZMA_STREAM_INIT;
    lzma_stream *_strm = &strm;
    DWORD error;

    switch (lzma_stream_decoder(_strm, UINT64_MAX, 0))
    {
    case LZMA_OK:
        error = ERROR_SUCCESS;
        {
            uint8_t *input = (uint8_t *) ptr;
            const size_t maxInputLengthPerCode = 8 * 1024;
            uint8_t output[maxInputLengthPerCode];
            const size_t outputCapacity = sizeof(output);

            while (size)
            {
                size_t inputLength;
                lzma_action action;
                DWORD outputLength;

                strm.next_in = input;
                strm.avail_in
                    = inputLength
                        = (maxInputLengthPerCode < size)
                            ? maxInputLengthPerCode
                            : size;

                size -= inputLength;
                action = size ? LZMA_RUN : LZMA_FINISH;

                do
                {
                    DWORD numberOfBytesWritten;

                    strm.next_out = output;
                    strm.avail_out = outputCapacity;

                    switch (lzma_code(_strm, action))
                    {
                    case LZMA_OK:
                    case LZMA_STREAM_END:
                        outputLength = outputCapacity - strm.avail_out;
                        if (outputLength
                                && !WriteFile(
                                        file,
                                        output, outputLength,
                                        &numberOfBytesWritten,
                                        NULL))
                        {
                            error = GetLastError();
                            LastError_setLastError(error, _T(__FILE__), __LINE__);
                        }
                        break;
                    default:
                        error = ERROR_WRITE_FAULT;
                        LastError_setLastError(error, _T(__FILE__), __LINE__);
                        break;
                    }

                    if (ERROR_SUCCESS != error)
                    {
                        action = LZMA_FINISH; /* Break out of the input loop. */
                        break; /* Break out of the output loop. */
                    }
                }
                while (outputLength);

                if (LZMA_FINISH == action)
                    break;
                else
                    input += inputLength;
            }
        }
        break;
    case LZMA_MEM_ERROR:
        error = ERROR_NOT_ENOUGH_MEMORY;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
        break;
    case LZMA_OPTIONS_ERROR:
        error = ERROR_INVALID_PARAMETER;
        LastError_setLastError(error, _T(__FILE__), __LINE__);
        break;
    case LZMA_PROG_ERROR:
    default:
        error = ERROR_OPEN_FAILED; /* For the lack of a better idea. */
        LastError_setLastError(error, _T(__FILE__), __LINE__);
        break;
    }
    lzma_end(_strm);
    return error;
}

int CALLBACK
WinMain(
        HINSTANCE instance, HINSTANCE prevInstance,
        LPSTR cmdLine,
        int cmdShow)
{
    /*
     * The value of WinMain's cmdLine argument may be incorrect, for example, in
     * the case of a character with an accent in the path of the executable. As
     * a workaround, try to reconstruct the correct value using GetCommandLineW
     * and CommandLineToArgvW.
     */
    LPTSTR winMainCmdLine = NULL;
    DWORD error = Setup_getWinMainCmdLine(&winMainCmdLine);

    if (ERROR_SUCCESS == error)
    {
        Setup_parseCommandLine(winMainCmdLine);

        /*
         * If the --xzdec command line argument has been specified, the caller
         * obviously knows what they are doing so do not ask whether Wow64 is
         * acceptable.
         */
        /* if (Setup_quiet || Setup_xzdec_ || (IDYES == Setup_isWow64Acceptable())) */
        {
            if (Setup_msiexec_)
                error = Setup_msiexec();
            else
            {
                Setup_enumResNameProc(
                        NULL,
                        RT_RCDATA,
                        MAKEINTRESOURCE(IDRCDATA_PAYLOAD),
                        (LONG_PTR) &error);
            }
        }
    }

    /*
     * If anything has gone wrong, report it to the user. In accord with the
     * --quiet command line argument, report the error via either an error
     * message dialog or the application exit code.
     */
    if ((ERROR_SUCCESS != error) && !Setup_quiet)
    {
        LPTSTR message;
        DWORD messageLength
            = FormatMessage(
                    FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
                    NULL,
                    error,
                    LANG_USER_DEFAULT,
                    (LPTSTR) &message,
                    0,
                    NULL);

        if (messageLength)
        {
            /*
             * If there is debug information about the particular piece of
             * source code which has caused the error, display it to the user as
             * well so that we/the developers may get a more accurate report and
             * have a greater chance of understanding and fixing the problem.
             */
            LPCTSTR lastErrorFile = LastError_file();
            LPCTSTR productName = Setup_getProductName();

            if (lastErrorFile)
            {
                TCHAR lastErrorFormat[1024];
                int lastErrorFormatLength
                    = LoadString(
                            GetModuleHandle(NULL),
                            IDS_LASTERRORFORMAT,
                            lastErrorFormat,
                            sizeof(lastErrorFormat) / sizeof(TCHAR));

                if (lastErrorFormatLength)
                {
                    LPTSTR lastErrorMessage;
                    DWORD_PTR lastErrorArguments[]
                        = {
                            (DWORD_PTR) productName,
                            (DWORD_PTR) error,
                            (DWORD_PTR) lastErrorFile,
                            (DWORD_PTR) LastError_line(),
                            (DWORD_PTR) message
                        };
                    DWORD lastErrorMessageLength
                        = FormatMessage(
                                FORMAT_MESSAGE_ALLOCATE_BUFFER
                                    | FORMAT_MESSAGE_ARGUMENT_ARRAY
                                    | FORMAT_MESSAGE_FROM_STRING,
                                lastErrorFormat,
                                0,
                                LANG_USER_DEFAULT,
                                (LPTSTR) &lastErrorMessage,
                                0,
                                (va_list *) lastErrorArguments);

                    if (lastErrorMessageLength)
                    {
                        LocalFree(message);
                        message = lastErrorMessage;
                    }
                }
            }

            MessageBox(NULL, message, productName, MB_ICONERROR | MB_OK);
            LocalFree(message);

            /*
             * The error has just been reported to the user as an error message
             * dialog so assume it is enough and do not report it as the
             * application exit code.
             */
            error = ERROR_SUCCESS;
        }
    }

    /*
     * Clean up. (It is not really necessary because this application is about
     * to exit.)
     */
    if (Setup_productName && (Setup_productName != Setup_fileName))
        free(Setup_productName);
    if (Setup_fileName)
        free(Setup_fileName);

    return error;
}
