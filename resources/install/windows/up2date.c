/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include <windows.h>
#include <shellapi.h> /* ShellExecute */
#include <tlhelp32.h> /* CreateToolhelp32Snapshot */

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
LPTSTR up2date_getBoolArg(LPCTSTR, LPTSTR, BOOL *);
DWORD  up2date_getExePath(LPTSTR, DWORD);
DWORD  up2date_getParentProcessId(DWORD *);
LPTSTR up2date_getWaitParent(LPTSTR, BOOL *);
LPTSTR up2date_skipWhitespace(LPTSTR);
LPTSTR up2date_str2tstr(LPCSTR);
DWORD  up2date_waitParent();

int WINAPI
WinMain(HINSTANCE instance, HINSTANCE prevInstance, LPSTR cmdLine, int cmdShow) {
    LPTSTR commandLine;

    commandLine = up2date_str2tstr(cmdLine);
    if (commandLine) {
        BOOL waitParent;
        LPTSTR noWaitParentCommandLine;
        BOOL allowElevation;
        LPTSTR noAllowElevationCommandLine;
        DWORD error;

        waitParent = FALSE;
        noWaitParentCommandLine
            = up2date_getWaitParent(commandLine, &waitParent);
        if (waitParent)
            up2date_waitParent();

        allowElevation = FALSE;
        noAllowElevationCommandLine
            = up2date_getAllowElevation(
                    noWaitParentCommandLine, &allowElevation);

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
    return
        up2date_getBoolArg(
            TEXT("--allow-elevation"),
            commandLine,
            allowElevation);
}

LPTSTR
up2date_getBoolArg(LPCTSTR argName, LPTSTR commandLine, BOOL *boolValue) {
    size_t argNameLength;
    BOOL argValue;

    argNameLength = _tcslen(argName);
    commandLine = up2date_skipWhitespace(commandLine);
    if (0 == _tcsncicmp(commandLine, argName, argNameLength)) {
        argValue = TRUE;
        commandLine
            = up2date_skipWhitespace(commandLine + argNameLength);
    } else
        argValue = FALSE;
    if (boolValue)
        *boolValue = argValue;
    return commandLine;
}

DWORD
up2date_getExePath(LPTSTR exePath, DWORD exePathSize) {
    return
        GetModuleFileName(NULL, (LPTSTR) exePath, exePathSize)
            ? 0
            : GetLastError();
}

DWORD
up2date_getParentProcessId(DWORD *ppid) {
    HANDLE snapshot;
    DWORD error;

    snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE)
        error = GetLastError();
    else {
        PROCESSENTRY32 entry;

        entry.dwSize = sizeof(PROCESSENTRY32);
        if (Process32First(snapshot, &entry)) {
            DWORD pid;

            error = 0;
            pid = GetCurrentProcessId();
            if (ppid)
                *ppid = 0;

            do {
                if (entry.th32ProcessID == pid) {
                    if (ppid)
                        *ppid = entry.th32ParentProcessID;
                    break;
                }
                if (!Process32Next(snapshot, &entry)) {
                    error = GetLastError();
                    break;
                }
            } while (1);
        } else
            error = GetLastError();
        CloseHandle(snapshot);
    }
    return error;
}

LPTSTR
up2date_getWaitParent(LPTSTR commandLine, BOOL *waitParent) {
    return
        up2date_getBoolArg(
            TEXT("--wait-parent"),
            commandLine,
            waitParent);
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

DWORD
up2date_waitParent() {
    DWORD error;
    DWORD ppid;

    error = up2date_getParentProcessId(&ppid);
    if (!error) {
        HANDLE parentProcess;

        parentProcess = OpenProcess(SYNCHRONIZE, FALSE, ppid);
        if (parentProcess) {
            DWORD event;

            error = 0;

            do {
                event = WaitForSingleObject(parentProcess, INFINITE);
                if (WAIT_FAILED == event) {
                    error = GetLastError();
                    break;
                }
            } while (WAIT_TIMEOUT == event);
            CloseHandle(parentProcess);
        } else
            error = GetLastError();
    }
    return error;
}
