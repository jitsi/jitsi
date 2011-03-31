/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "lasterror.h"
#include "setup.h"

#include <ctype.h> /* isspace */
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

static LPWSTR Setup_commandLine = NULL;

/*
 * When <tt>FALSE</tt> is returned from <tt>Setup_enumResNameProc</tt> to stop
 * <tt>EnumResourceNames</tt>, <tt>GetLastError</tt> on Windows XP seems to
 * return an error code which is in contrast with the MSDN documentation. If
 * <tt>Setup_enumResNameProc</tt> has managed to
 * <tt>Setup_extractAndExecuteMsi</tt>, <tt>Setup_enumResNameProcUserStop</tt>
 * indicates that the error code returned by <tt>GetLastError</tt> is to be
 * ignored.
 */
static BOOL Setup_enumResNameProcUserStop = FALSE;
static LPTSTR Setup_fileName = NULL;
static LPTSTR Setup_productName = NULL;
static BOOL Setup_waitForParentProcess_ = FALSE;

BOOL CALLBACK Setup_enumResNameProc(HMODULE module, LPCTSTR type, LPTSTR name, LONG_PTR param);
static DWORD Setup_executeMsi(LPCTSTR path);
static DWORD Setup_extractAndExecuteMsi(LPVOID ptr, DWORD size);
static LPTSTR Setup_getBoolArg(LPCTSTR argName, LPTSTR commandLine, BOOL *boolValue);
static LPCTSTR Setup_getFileName();
static DWORD Setup_getParentProcess(DWORD *ppid, LPTSTR *fileName);
static LPCTSTR Setup_getProductName();
static int Setup_isWow64Acceptable();
LRESULT CALLBACK Setup_isWow64AcceptableMessageBoxCallWndRetProc(int code, WPARAM wParam, LPARAM lParam);
static DWORD Setup_parseCommandLine(LPSTR cmdLine);
static LPTSTR Setup_skipWhitespace(LPTSTR str);
static LPWSTR Setup_str2wstr(LPCSTR str);
static DWORD Setup_terminateUp2DateExe();
static DWORD Setup_waitForParentProcess();

BOOL CALLBACK
Setup_enumResNameProc(
        HMODULE module,
        LPCTSTR type, LPTSTR name,
        LONG_PTR param)
{
    BOOL proceed = TRUE;
    DWORD error = ERROR_SUCCESS;

    if (!IS_INTRESOURCE(name)
            && (_tcslen(name) > 3)
            && (_tcsnicmp(name, _T("MSI"), 3) == 0))
    {
        HRSRC rsrc = FindResource(module, name, type);

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
                        Setup_enumResNameProcUserStop = TRUE;
                        error = Setup_extractAndExecuteMsi(ptr, size);
                    }
                    else
                    {
                        error = GetLastError();
                        LastError_setLastError(error, __FILE__, __LINE__);
                    }
                }
                else
                {
                    error = GetLastError();
                    LastError_setLastError(error, __FILE__, __LINE__);
                }
            }
            else
            {
                error = GetLastError();
                LastError_setLastError(error, __FILE__, __LINE__);
            }
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, __FILE__, __LINE__);
        }
    }
    if (param)
        *((DWORD *) param) = error;
    return proceed;
}

static DWORD
Setup_executeMsi(LPCTSTR path)
{
    DWORD error = ERROR_SUCCESS;
    LPWSTR p0, p1, p2, p3;
    size_t p0Length, p1Length, p2Length, p3Length;
    LPWSTR parameters;

    p0 = L"/i \"";
    p0Length = wcslen(p0);
#ifdef _UNICODE
    p1 = path;
#else
    p1 = Setup_str2wstr(path);
#endif /* #ifdef _UNICODE */
    if (p1)
        p1Length = wcslen(p1);
    else
    {
        error = ERROR_OUTOFMEMORY;
        LastError_setLastError(error, __FILE__, __LINE__);
        return error;
    }
    p2 = L"\" ";
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

        if (ShellExecuteExW(&sei) && (((int) (sei.hInstApp)) > 32))
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
                        LastError_setLastError(error, __FILE__, __LINE__);
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
            LastError_setLastError(error, __FILE__, __LINE__);
        }

        free(parameters);
    }
    else
    {
        error = ERROR_OUTOFMEMORY;
        LastError_setLastError(error, __FILE__, __LINE__);
    }

    if (((LPVOID) p1) != ((LPVOID) path))
        free(p1);

    return error;
}

static DWORD
Setup_extractAndExecuteMsi(LPVOID ptr, DWORD size)
{
    TCHAR path[MAX_PATH + 1];
    DWORD pathSize = sizeof(path) / sizeof(TCHAR);
    DWORD tempPathLength = GetTempPath(pathSize, path);
    DWORD error = ERROR_SUCCESS;

    if (tempPathLength)
    {
        if (tempPathLength > pathSize)
        {
            error = ERROR_NOT_ENOUGH_MEMORY;
            LastError_setLastError(error, __FILE__, __LINE__);
        }
        else
        {
            LPCTSTR fileName = Setup_getFileName();
            HANDLE file = INVALID_HANDLE_VALUE;

            if (fileName)
            {
                size_t fileNameLength = _tcslen(fileName);

                if ((fileNameLength > 4 /* .exe */)
                        && (tempPathLength + fileNameLength + 1 <= pathSize))
                {
                    LPTSTR str = path + tempPathLength;

                    _tcsncpy(str, fileName, fileNameLength - 4);
                    str += (fileNameLength - 4);
                    _tcsncpy(str, _T(".msi"), 4);
                    *(str + 4) = 0;

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
                                        _T("MSI"),
                                        0,
                                        path))
                        {
                            error = GetLastError();
                            LastError_setLastError(error, __FILE__, __LINE__);
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
                        }

                        free(tempPath);
                    }
                    else
                    {
                        error = ERROR_OUTOFMEMORY;
                        LastError_setLastError(error, __FILE__, __LINE__);
                    }
                }

                if (INVALID_HANDLE_VALUE != file)
                {
                    DWORD written;

                    if ((FALSE == WriteFile(file, ptr, size, &written, NULL))
                            || (written != size))
                    {
                        error = GetLastError();
                        LastError_setLastError(error, __FILE__, __LINE__);
                        CloseHandle(file);
                    }
                    else
                    {
                        if (Setup_waitForParentProcess_)
                            Setup_waitForParentProcess();

                        CloseHandle(file);
                        error = Setup_executeMsi(path);
                    }
                    DeleteFile(path);
                }
            }
        }
    }
    else
    {
        error = GetLastError();
        LastError_setLastError(error, __FILE__, __LINE__);
    }
    return error;
}

static LPTSTR
Setup_getBoolArg(LPCTSTR argName, LPTSTR commandLine, BOOL *boolValue)
{
    size_t argNameLength;
    BOOL argValue;

    argNameLength = _tcslen(argName);
    commandLine = Setup_skipWhitespace(commandLine);
    if (0 == _tcsnicmp(commandLine, argName, argNameLength))
    {
        argValue = TRUE;
        commandLine = Setup_skipWhitespace(commandLine + argNameLength);
    }
    else
        argValue = FALSE;
    if (boolValue)
        *boolValue = argValue;
    return commandLine;
}

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

                if (('\\' == c) || ('/' == c))
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
        LastError_setLastError(error, __FILE__, __LINE__);
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
                    LastError_setLastError(error, __FILE__, __LINE__);
                    break;
                }
            }
            while (1);
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, __FILE__, __LINE__);
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
                            LastError_setLastError(error, __FILE__, __LINE__);
                        }
                        break;
                    }
                    if (!Process32Next(snapshot, &entry))
                    {
                        error = GetLastError();
                        LastError_setLastError(error, __FILE__, __LINE__);
                        break;
                    }
                }
                while (1);
            }
            else
            {
                error = GetLastError();
                LastError_setLastError(error, __FILE__, __LINE__);
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

static int
Setup_isWow64Acceptable()
{
    HMODULE kernel32 = GetModuleHandle(_T("kernel32"));
    int answer = IDYES;

    if (kernel32)
    {
        typedef BOOL (WINAPI *LPISWOW64PROCESS)(HANDLE, PBOOL);

        LPISWOW64PROCESS isWow64Process
            = (LPISWOW64PROCESS) GetProcAddress(kernel32, _T("IsWow64Process"));
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
Setup_parseCommandLine(LPSTR cmdLine)
{
    LPTSTR commandLine;
    DWORD error = ERROR_SUCCESS;

#ifdef _UNICODE
    if (cmdLine)
    {
        commandLine = Setup_str2wstr(cmdLine);
        if (!commandLine)
        {
            error = ERROR_OUTOFMEMORY;
            LastError_setLastError(error, __FILE__, __LINE__);
        }
    }
    else
        commandLine = NULL;
#else
    commandLine = cmdLine;
#endif /* #ifdef _UNICODE */

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
                    LastError_setLastError(error, __FILE__, __LINE__);
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
                    LastError_setLastError(error, __FILE__, __LINE__);
                }
            }
        }

        if (up2date && noAllowElevationCommandLineLength)
        {
            LPWSTR commandLineW;

#ifdef _UNICODE
            commandLineW = noAllowElevationCommandLine;
#else
            commandLineW = Setup_str2wstr(noAllowElevationCommandLine);
            if (!commandLineW)
            {
                error = ERROR_OUTOFMEMORY;
                LastError_setLastError(error, __FILE__, __LINE__);
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
                                = L"SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR=\"";
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
                                        __FILE__, __LINE__);
                            }
                        }
                    }
                    LocalFree(argv);
                }
                else
                {
                    error = GetLastError();
                    LastError_setLastError(error, __FILE__, __LINE__);
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

        if (!up2date && noAllowElevationCommandLineLength)
        {
#ifdef _UNICODE
            Setup_commandLine = _wcsdup(noAllowElevationCommandLine);
#else
            Setup_commandLine = Setup_str2wstr(noAllowElevationCommandLine);
#endif /* #ifdef _UNICODE */
            if (Setup_commandLine)
            {
                /*
                 * At the time of this writing, we expect a single property to
                 * pass on to msiexec which wants it in the format
                 * PROPERTY="VALUE" if value contains spaces. Unfortunately,
                 * ProcessBuilder on the Java side will break it by quoting the
                 * whole command line argument.
                 */
                size_t commandLineLength = wcslen(Setup_commandLine);

                if ((commandLineLength > 3)
                        && (L'"' == *Setup_commandLine)
                        && (L'"' == *(Setup_commandLine + (commandLineLength - 2)))
                        && (L'"' == *(Setup_commandLine + (commandLineLength - 1))))
                {
                    *(Setup_commandLine + (commandLineLength - 1)) = 0;
                    Setup_commandLine++;
                }
            }
            else
            {
                error = ERROR_OUTOFMEMORY;
                LastError_setLastError(error, __FILE__, __LINE__);
            }
        }

        if (commandLine != cmdLine)
            free(commandLine);
    }
    return error;
}

static LPTSTR
Setup_skipWhitespace(LPTSTR str)
{
    TCHAR c;

    while ((c = *str) && _istspace(c))
        ++str;
    return str;
}

static LPWSTR
Setup_str2wstr(LPCSTR str)
{
    int tstrSize;
    LPWSTR tstr;

    tstrSize = MultiByteToWideChar(CP_THREAD_ACP, 0, str, -1, NULL, 0);
    if (tstrSize)
    {
        tstr = (LPWSTR) malloc(tstrSize * sizeof(WCHAR));
        if (tstr)
        {
            tstrSize
                = MultiByteToWideChar(CP_THREAD_ACP, 0, str, -1, tstr, tstrSize);
            if (!tstrSize)
            {
                free(tstr);
                tstr = NULL;
            }
        }
        else
            tstr = NULL;
    }
    else
        tstr = NULL;
    return tstr;
}

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
                    LastError_setLastError(error, __FILE__, __LINE__);
                }
                CloseHandle(parentProcess);
            }
            else
            {
                error = GetLastError();
                LastError_setLastError(error, __FILE__, __LINE__);
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
                    LastError_setLastError(error, __FILE__, __LINE__);
                    break;
                }
            }
            while (WAIT_TIMEOUT == event);
            CloseHandle(parentProcess);
        }
        else
        {
            error = GetLastError();
            LastError_setLastError(error, __FILE__, __LINE__);
        }
    }
    return error;
}

int CALLBACK
WinMain(
        HINSTANCE instance, HINSTANCE prevInstance,
        LPSTR cmdLine,
        int cmdShow)
{
    DWORD error = ERROR_SUCCESS;

    Setup_parseCommandLine(cmdLine);

    if ((ERROR_SUCCESS == error)
            && (IDYES == Setup_isWow64Acceptable())
            && (FALSE
                    == EnumResourceNames(
                            NULL,
                            RT_RCDATA,
                            Setup_enumResNameProc,
                            (LONG_PTR) &error))
            && (ERROR_SUCCESS == error)
            && (FALSE == Setup_enumResNameProcUserStop))
    {
        DWORD enumResourceNamesError = GetLastError();

        if ((ERROR_SUCCESS != enumResourceNamesError)
                && (ERROR_RESOURCE_ENUM_USER_STOP != enumResourceNamesError))
        {
            error = enumResourceNamesError;
            LastError_setLastError(error, __FILE__, __LINE__);
        }
    }

    if (ERROR_SUCCESS != error)
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
        }
    }

    if (Setup_productName && (Setup_productName != Setup_fileName))
        free(Setup_productName);
    if (Setup_fileName)
        free(Setup_fileName);

    return 0;
}
