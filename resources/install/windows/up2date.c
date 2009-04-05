/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include <windows.h>
#include <shellapi.h> /* ShellExecute */

#include <stdlib.h>
#include <string.h>
#include <tchar.h> /* _istspace */

#ifndef ERROR_ELEVATION_REQUIRED
#define ERROR_ELEVATION_REQUIRED 740
#endif

#ifndef _tcsncicmp
#ifdef _UNICODE
#define _tcsncicmp _wcsnicmp
#else
#define _tcsncicmp _strnicmp
#endif
#endif

DWORD  up2date_createProcess(LPCTSTR);
DWORD  up2date_displayError(DWORD error);
LPTSTR up2date_getAllowElevation(LPTSTR, BOOL *);
DWORD  up2date_getExePath(LPTSTR, DWORD);
LPTSTR up2date_skipWhitespace(LPTSTR);
LPTSTR up2date_str2tstr(LPCSTR);

int WINAPI
WinMain(HINSTANCE instance, HINSTANCE prevInstance, LPSTR cmdLine, int cmdShow) {
    LPTSTR commandLine;

    commandLine = up2date_str2tstr(cmdLine);
    if (commandLine) {
        BOOL allowElevation;
        LPTSTR noAllowElevationCommandLine;
        DWORD error;

        allowElevation = FALSE;
        noAllowElevationCommandLine
            = up2date_getAllowElevation(commandLine, &allowElevation);

        error = up2date_createProcess(noAllowElevationCommandLine);
        if ((ERROR_ELEVATION_REQUIRED == error) && allowElevation) {
            TCHAR exePath[MAX_PATH + 1];

            if (!up2date_getExePath(exePath, MAX_PATH + 1)) {
                ShellExecute(NULL, TEXT("runas"), exePath,
                    noAllowElevationCommandLine, NULL, SW_SHOWDEFAULT);
            }
        } else if (error)
            up2date_displayError(error);

        if (((LPVOID) commandLine) != ((LPVOID) cmdLine))
            free(commandLine);
    }

    return 0;
}

DWORD
up2date_createProcess(LPCTSTR commandLine) {
    LPWSTR *argv;
    int argc;
    DWORD error;

    argv = CommandLineToArgvW(commandLine, &argc);
    if (argv) {
        switch (argc) {
        case 2:
            if (!SetEnvironmentVariable(
                        TEXT("SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR"),
                        *(argv + 1))) {
                error = GetLastError();
                break;
            }
        case 1: {
            STARTUPINFO si;
            PROCESS_INFORMATION pi;

            ZeroMemory(&si, sizeof(si));
            si.cb = sizeof(si);

            error
                = CreateProcess(NULL, *argv, NULL, NULL, FALSE, 0, NULL,
                        NULL, &si, &pi)
                    ? 0
                    : GetLastError();
            }
            break;
        default:
            error = 0;
            break;
        }

        LocalFree((HLOCAL) argv);
    } else
        error = GetLastError();
    return error;
}

DWORD
up2date_displayError(DWORD error) {
    LPTSTR message;
    DWORD ret;

    if (FormatMessage(
            FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
            0,
            error,
            0,
            (LPTSTR) &message,
            0,
            NULL)) {
        TCHAR caption[MAX_PATH + 1];

        MessageBox(
            NULL,
            message,
            up2date_getExePath(caption, MAX_PATH + 1) ? NULL : caption,
            MB_ICONERROR);
        LocalFree((HLOCAL) message);
        ret = 0;
    } else
        ret = GetLastError();
    return ret;
}

LPTSTR
up2date_getAllowElevation(LPTSTR commandLine, BOOL *allowElevation) {
    LPCTSTR argName = TEXT("--allow-elevation");
    size_t argNameLength = _tcslen(argName);
    BOOL argValue;

    commandLine = up2date_skipWhitespace(commandLine);
    if (0 == _tcsncicmp(commandLine, argName, argNameLength)) {
        argValue = TRUE;
        commandLine
            = up2date_skipWhitespace(commandLine + argNameLength);
    } else
        argValue = FALSE;
    if (allowElevation)
        *allowElevation = argValue;
    return commandLine;
}

DWORD
up2date_getExePath(LPTSTR exePath, DWORD exePathSize) {
    return
        GetModuleFileName(NULL, (LPTSTR) exePath, exePathSize)
            ? 0
            : GetLastError();
}

LPTSTR
up2date_skipWhitespace(LPTSTR str) {
    TCHAR ch;

    while ((ch = *str) && _istspace(ch))
        ++str;
    return str;
}

LPTSTR
up2date_str2tstr(LPCSTR str) {
    LPTSTR tstr;

#ifdef UNICODE
    int tstrSize;

    tstrSize =
        MultiByteToWideChar(CP_THREAD_ACP, 0, str, -1, NULL, 0);
    if (tstrSize) {
        tstr = (LPTSTR) malloc(tstrSize * sizeof(TCHAR));
        if (tstr) {
            tstrSize
                = MultiByteToWideChar(CP_THREAD_ACP, 0, str, -1, tstr, tstrSize);
            if (!tstrSize) {
                free(tstr);
                tstr = NULL;
            }
        } else
            tstr = NULL;
    } else
        tstr = NULL;
#else
    tstr = str;
#endif

    return tstr;
}
