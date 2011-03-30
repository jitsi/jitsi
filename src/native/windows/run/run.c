/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "run.h"

#include <ctype.h> /* isspace */
#include <psapi.h> /* GetModuleFileNameEx */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <tchar.h>
#include <tlhelp32.h> /* CreateToolhelp32Snapshot */

/**
 * The pipe through which the launcher is to communicate with the crash handler.
 */
static HANDLE Run_channel = INVALID_HANDLE_VALUE;

/**
 * The command line that the application has received as the <tt>cmdLine</tt>
 * function argument of its <tt>WinMain</tt> entry point and that is currently
 * unparsed i.e. the parts which have already been parsed are no longer present.
 */
static LPSTR Run_cmdLine = NULL;

/**
 * The indicator which determines whether the crash handler is to launch the
 * application.
 */
static BOOL Run_launch = TRUE;

static DWORD Run_addPath(LPCTSTR path);
static int Run_displayMessageBoxFromString(DWORD textId, DWORD_PTR *textArgs, LPCTSTR caption, UINT type);
static DWORD Run_equalsParentProcessExecutableFilePath(LPCTSTR executableFilePath, BOOL *equals);
static DWORD Run_getExecutableFilePath(LPTSTR *executableFilePath);
static DWORD Run_getJavaExeCommandLine(LPCTSTR javaExe, LPTSTR *commandLine);
static LONG Run_getJavaPathsFromRegKey(HKEY key, LPTSTR *runtimeLib, LPTSTR *javaHome);
static LPTSTR Run_getLockFilePath();
static DWORD Run_getParentProcessId(DWORD *ppid);
static LONG Run_getRegSzValue(HKEY key, LPCTSTR value, LPTSTR *data);
static DWORD Run_handleLauncherExitCode(DWORD exitCode, LPCTSTR lockFilePath, LPCTSTR executableFilePath);
static BOOL Run_isDirectory(LPCTSTR fileName);
static BOOL Run_isFile(LPCTSTR fileName);
static DWORD Run_openProcessAndResumeThread(DWORD processId, DWORD threadId, HANDLE *process);
static DWORD Run_runAsCrashHandler(LPCTSTR executableFilePath, LPSTR cmdLine);
static DWORD Run_runAsCrashHandlerWithPipe(LPCTSTR executableFilePath, LPSTR cmdLine, HANDLE *readPipe, HANDLE *writePipe);
static DWORD Run_runAsLauncher(LPCTSTR executableFilePath, LPSTR cmdLine);
static DWORD Run_runJava(LPCTSTR executableFilePath, LPSTR cmdLine);
static DWORD Run_runJavaExe(LPCTSTR javaExe, BOOL searchPath, BOOL *searchForJava);
static DWORD Run_runJavaFromEnvVar(LPCTSTR envVar, BOOL *searchForJava);
static DWORD Run_runJavaFromJavaHome(LPCTSTR javaHome, BOOL searchForRuntimeLib, BOOL *searchForJava);
static DWORD Run_runJavaFromRegKey(HKEY key, BOOL *searchForJava);
static DWORD Run_runJavaFromRuntimeLib(LPCTSTR runtimeLib, BOOL *searchForJava);
static LPSTR Run_skipWhitespace(LPSTR str);

static DWORD
Run_addPath(LPCTSTR path)
{
    LPCTSTR envVarName = _T("PATH");
    TCHAR envVar[4096];
    DWORD envVarCapacity = sizeof(envVar) / sizeof(TCHAR);
    DWORD envVarLength
        = GetEnvironmentVariable(envVarName, envVar, envVarCapacity);
    DWORD error;

    if (envVarLength)
    {
        if (envVarLength >= envVarCapacity)
            error = ERROR_NOT_ENOUGH_MEMORY;
        else
        {
            DWORD pathLength = _tcslen(path);

            if (envVarLength + 1 + pathLength + 1 > envVarCapacity)
                error = ERROR_NOT_ENOUGH_MEMORY;
            else
            {
                LPTSTR str = envVar + envVarLength;

                *str = _T(';');
                str++;
                _tcsncpy(str, path, pathLength);
                str += pathLength;
                *str = 0;

                if (SetEnvironmentVariable(envVarName, envVar))
                    error = ERROR_SUCCESS;
                else
                    error = GetLastError();
            }
        }
    }
    else
        error = GetLastError();
    return error;
}

static int
Run_displayMessageBoxFromString(
    DWORD textId, DWORD_PTR *textArgs,
    LPCTSTR caption,
    UINT type)
{
    TCHAR format[1024];
    int formatLength
        = LoadString(
                GetModuleHandle(NULL),
                textId,
                format, sizeof(format) / sizeof(TCHAR));
    int answer = 0;

    if (formatLength > 0)
    {
        LPTSTR message = NULL;
        DWORD messageLength
            = FormatMessage(
                    FORMAT_MESSAGE_ALLOCATE_BUFFER
                        | FORMAT_MESSAGE_ARGUMENT_ARRAY
                        | FORMAT_MESSAGE_FROM_STRING,
                    format,
                    0,
                    LANG_USER_DEFAULT,
                    (LPTSTR) &message,
                    0,
                    (va_list *) textArgs);

        if (messageLength)
        {
            answer
                = MessageBox(
                        NULL,
                        message,
                        caption,
                        type);
            LocalFree(message);
        }
    }
    return answer;
}

static DWORD
Run_equalsParentProcessExecutableFilePath(
    LPCTSTR executableFilePath,
    BOOL *equals)
{
    DWORD ppid = 0;
    DWORD error = Run_getParentProcessId(&ppid);

    if (ERROR_SUCCESS == error)
    {
        HANDLE parentProcess
            = OpenProcess(
                    PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
                    FALSE,
                    ppid);

        if (parentProcess)
        {
            TCHAR parentProcessExecutableFilePath[MAX_PATH + 1];
            DWORD parentProcessExecutableFilePathLength
                = GetModuleFileNameEx(
                        parentProcess,
                        NULL,
                        parentProcessExecutableFilePath,
                        sizeof(parentProcessExecutableFilePath));

            if (parentProcessExecutableFilePathLength)
            {
                *equals
                    = (_tcsnicmp(
                                parentProcessExecutableFilePath,
                                executableFilePath,
                                parentProcessExecutableFilePathLength)
                            == 0);
            }
            else
                error = GetLastError();

            CloseHandle(parentProcess);
        }
        else
            error = GetLastError();
    }
    return error;
}

static DWORD
Run_getExecutableFilePath(LPTSTR *executableFilePath)
{
    TCHAR str[MAX_PATH + 1];
    DWORD capacity = sizeof(str);
    DWORD length = GetModuleFileName(NULL, str, capacity);
    DWORD error;

    if (length)
    {
        /* Make sure str is null terminated on Windows XP/2000. */
        if (length == capacity)
        {
            length--;
            str[length] = 0;
        }

        *executableFilePath = (LPTSTR) malloc(sizeof(TCHAR) * (length + 1));
        if (*executableFilePath)
        {
            _tcsncpy(*executableFilePath, str, length);
            *((*executableFilePath) + length) = 0;
            error = ERROR_SUCCESS;
        }
        else
            error = ERROR_OUTOFMEMORY;
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_getJavaExeCommandLine(LPCTSTR javaExe, LPTSTR *commandLine)
{
    LPCTSTR classpath[]
        = {
            _T("lib\\felix.jar"),
            _T("lib\\jdic-all.jar"),
            _T("lib\\jdic_stub.jar"),
            _T("lib\\bcprovider.jar"),
            _T("sc-bundles\\sc-launcher.jar"),
            _T("sc-bundles\\util.jar"),
            NULL
        };
    LPCTSTR properties[]
        = {
            _T("felix.config.properties"),
            _T("file:./lib/felix.client.run.properties"),
            _T("java.util.logging.config.file"),
            _T("lib/logging.properties"),
            _T("java.library.path"),
            _T("./native"),
            _T("jna.library.path"),
            _T("./native"),
            NULL
        };
    LPCTSTR scHomeDirNameProperty
        = _T("net.java.sip.communicator.SC_HOME_DIR_NAME");
    LPCTSTR scHomeDirNameValue = PRODUCTNAME;
    LPCTSTR mainClass
        = _T("net.java.sip.communicator.launcher.SIPCommunicator");

    size_t javaExeLength;
    size_t classpathLength;
    size_t propertiesLength;
    size_t scHomeDirNamePropertyLength;
    size_t scHomeDirNameValueLength;
    size_t mainClassLength;
    size_t cmdLineLength;
    DWORD error;

    javaExeLength = _tcslen(javaExe);
    classpathLength = 0;
    {
        size_t i = 0;

        while (1)
        {
            LPCTSTR cp = classpath[i];

            if (cp)
            {
                classpathLength += _tcslen(cp);
                i++;
            }
            else
                break;
        }
        if (i)
        {
            classpathLength += 5; /* "-cp  " */
            classpathLength += (i - 1); /* ';' */
        }
    }
    propertiesLength = 0;
    {
        size_t i = 0;

        while (1)
        {
            LPCTSTR property = properties[i];

            if (property)
            {
                i++;
                propertiesLength
                    += (2 /* "-D" */
                            + _tcslen(property)
                            + 1 /* '=' */
                            + _tcslen(properties[i])
                            + 1 /* ' ' */);
                i++;
            }
            else
                break;
        }
        scHomeDirNamePropertyLength = _tcslen(scHomeDirNameProperty);
        scHomeDirNameValueLength = _tcslen(scHomeDirNameValue);
        propertiesLength
            += (2
                    + scHomeDirNamePropertyLength
                    + 2
                    + scHomeDirNameValueLength
                    + 2);
    }
    mainClassLength = _tcslen(mainClass);
    cmdLineLength = Run_cmdLine ? (1 /* ' ' */ + _tcslen(Run_cmdLine)) : 0;

    *commandLine
        = (LPTSTR)
            malloc(
                    sizeof(TCHAR)
                        * (javaExeLength
                            + 1 /* ' ' */
                            + classpathLength
                            + propertiesLength
                            + mainClassLength
                            + cmdLineLength
                            + 1 /* 0 */));
    if (*commandLine)
    {
        LPTSTR str = *commandLine;

        _tcsncpy(str, javaExe, javaExeLength);
        str += javaExeLength;
        *str = _T(' ');
        str++;
        if (classpathLength)
        {
            size_t i;

            _tcscpy(str, _T("-cp "));
            str += 4;
            i = 0;
            while (1)
            {
                LPCTSTR cp = classpath[i];

                if (cp)
                {
                    size_t length = _tcslen(cp);

                    _tcsncpy(str, cp, length);
                    str += length;
                    *str = _T(';');
                    str++;
                    i++;
                }
                else
                    break;
            }
            str--; /* Drop the last ';'. */
            *str = _T(' ');
            str++;
        }
        if (propertiesLength)
        {
            size_t i = 0;

            while (1)
            {
                LPCTSTR property = properties[i];

                if (property)
                {
                    size_t length;
                    LPCTSTR value;

                    _tcscpy(str, _T("-D"));
                    str += 2;
                    length = _tcslen(property);
                    _tcsncpy(str, property, length);
                    str += length;
                    i++;
                    *str = _T('=');
                    str++;
                    value = properties[i];
                    length = _tcslen(value);
                    _tcsncpy(str, value, length);
                    str += length;
                    i++;
                    *str = _T(' ');
                    str++;
                }
                else
                    break;
            }
            _tcscpy(str, _T("-D"));
            str += 2;
            _tcsncpy(str, scHomeDirNameProperty, scHomeDirNamePropertyLength);
            str += scHomeDirNamePropertyLength;
            _tcscpy(str, _T("=\""));
            str += 2;
            _tcsncpy(str, scHomeDirNameValue, scHomeDirNameValueLength);
            str += scHomeDirNameValueLength;
            _tcscpy(str, _T("\" "));
            str += 2;
        }
        _tcsncpy(str, mainClass, mainClassLength);
        str += mainClassLength;
        if (Run_cmdLine)
        {
            *str = _T(' ');
            str++;
            _tcsncpy(str, Run_cmdLine, cmdLineLength);
            str += cmdLineLength;
        }
        *str = 0;

        error = ERROR_SUCCESS;
    }
    else
        error = ERROR_OUTOFMEMORY;
    return error;
}

static LONG
Run_getJavaPathsFromRegKey(HKEY key, LPTSTR *runtimeLib, LPTSTR *javaHome)
{
    HKEY jreKey = 0;
    LONG error
        = RegOpenKeyEx(
                key,
                _T("Software\\JavaSoft\\Java Runtime Environment"),
                0,
                KEY_QUERY_VALUE,
                &jreKey);

    if (ERROR_SUCCESS == error)
    {
        LPTSTR currentVersion = NULL;

        error
            = Run_getRegSzValue(jreKey, _T("CurrentVersion"), &currentVersion);
        if (ERROR_SUCCESS == error)
        {
            HKEY currentVersionKey = 0;

            error
                = RegOpenKeyEx(
                        jreKey,
                        currentVersion,
                        0,
                        KEY_QUERY_VALUE,
                        &currentVersionKey);
            free(currentVersion);
            if (ERROR_SUCCESS == error)
            {
                if (ERROR_SUCCESS
                        != Run_getRegSzValue(
                                currentVersionKey,
                                _T("RuntimeLib"),
                                runtimeLib))
                    *runtimeLib = NULL;
                if (ERROR_SUCCESS
                        != Run_getRegSzValue(
                                currentVersionKey,
                                _T("JavaHome"),
                                javaHome))
                    *javaHome = NULL;
                RegCloseKey(currentVersionKey);
            }
        }
        RegCloseKey(jreKey);
    }
    return error;
}

static LPTSTR
Run_getLockFilePath()
{
    TCHAR appData[MAX_PATH + 1];
    DWORD appDataCapacity = sizeof(appData) / sizeof(TCHAR);
    DWORD appDataLength
        = GetEnvironmentVariable(
                _T("APPDATA"),
                appData, appDataCapacity);
    LPTSTR lockFilePath = NULL;

    if (appDataLength && (appDataLength < appDataCapacity))
    {
        LPCTSTR productName = PRODUCTNAME;
        size_t productNameLength = _tcslen(productName);
        LPCTSTR lockFileName = _T(".lock");
        size_t lockFileNameLength = _tcslen(lockFileName);

        lockFilePath
            = (LPTSTR)
                malloc(
                        sizeof(TCHAR)
                            * (appDataLength
                                    + 1
                                    + productNameLength
                                    + 1
                                    + lockFileNameLength
                                    + 1));
        if (lockFilePath)
        {
            LPTSTR str = lockFilePath;

            _tcsncpy(str, appData, appDataLength);
            str += appDataLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, productName, productNameLength);
            str += productNameLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, lockFileName, lockFileNameLength);
            str += lockFileNameLength;
            *str = 0;
        }
    }
    return lockFilePath;
}

static DWORD
Run_getParentProcessId(DWORD *ppid)
{
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    DWORD error;

    if (INVALID_HANDLE_VALUE == snapshot)
        error = GetLastError();
    else
    {
        PROCESSENTRY32 pe32;

        pe32.dwSize = sizeof(PROCESSENTRY32);
        if (Process32First(snapshot, &pe32))
        {
            DWORD pid = GetCurrentProcessId();

            error = ERROR_FILE_NOT_FOUND;
            do
            {
                if (pe32.th32ProcessID == pid)
                {
                    error = ERROR_SUCCESS;
                    *ppid = pe32.th32ParentProcessID;
                    break;
                }

                if (!Process32Next(snapshot, &pe32))
                {
                    error = GetLastError();
                    break;
                }
            }
            while (1);
        }
        else
            error = GetLastError();

        CloseHandle(snapshot);
    }
    return error;
}

static LONG
Run_getRegSzValue(HKEY key, LPCTSTR name, LPTSTR *data)
{
    LONG error;
    DWORD type;
    DWORD size;

    error = RegQueryValueEx(key, name, NULL, &type, NULL, &size);
    if (ERROR_SUCCESS == error)
    {
        if ((REG_SZ == type) && size)
        {
            *data = (LPTSTR) malloc(size + sizeof(TCHAR));

            if (*data)
            {
                error
                    = RegQueryValueEx(
                            key,
                            name,
                            NULL,
                            &type,
                            (LPBYTE) (*data),
                            &size);
                if (ERROR_SUCCESS == error)
                {
                    if ((REG_SZ == type) && size)
                        *((*data) + (size / sizeof(TCHAR))) = 0;
                    else
                        error = ERROR_FILE_NOT_FOUND;
                }
            }
        }
        else
            error = ERROR_FILE_NOT_FOUND;
    }
    return error;
}

static DWORD
Run_handleLauncherExitCode(
    DWORD exitCode, LPCTSTR lockFilePath,
    LPCTSTR executableFilePath)
{
    DWORD error = ERROR_SUCCESS;

    if (Run_isFile(lockFilePath))
    {
        DWORD_PTR arguments[] = { (DWORD_PTR) PRODUCTNAME };
        int answer
            = Run_displayMessageBoxFromString(
                    IDS_CRASHANDRELAUNCH, arguments,
                    executableFilePath,
                    MB_ICONEXCLAMATION | MB_YESNO);

        if (answer)
        {
            if (IDYES == answer)
                Run_launch = TRUE;

            /*
             * We believe the lockFilePath is related to the reported crash
             * instance so we have to remove it after notifying the user in
             * order to not take it into account upon a possible next launch.
             */
            DeleteFile(lockFilePath);
        }
        else
            error = GetLastError();
    }
    return error;
}

static BOOL
Run_isDirectory(LPCTSTR fileName)
{
    DWORD fileAttributes = GetFileAttributes(fileName);

    return
        (INVALID_FILE_ATTRIBUTES != fileAttributes)
            && (0 != (FILE_ATTRIBUTE_DIRECTORY & fileAttributes));
}

static BOOL
Run_isFile(LPCTSTR fileName)
{
    DWORD fileAttributes = GetFileAttributes(fileName);

    return
        (INVALID_FILE_ATTRIBUTES != fileAttributes)
            && (0 == (FILE_ATTRIBUTE_DIRECTORY & fileAttributes));
}

static DWORD
Run_openProcessAndResumeThread(DWORD processId, DWORD threadId, HANDLE *process)
{
    HANDLE p
        = OpenProcess(
                PROCESS_QUERY_INFORMATION | SYNCHRONIZE,
                FALSE,
                processId);
    DWORD error;

    if (p)
    {
        HANDLE t = OpenThread(THREAD_SUSPEND_RESUME, FALSE, threadId);

        if (t)
        {
            DWORD prevSuspendCount = ResumeThread(t);

            if (1 == prevSuspendCount)
            {
                *process = p;
                error = ERROR_SUCCESS;
            }
            else
                error = ERROR_NOT_FOUND;
            CloseHandle(t);
        }
        else
            error = GetLastError();
        if (ERROR_SUCCESS != error)
            CloseHandle(p);
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_runAsCrashHandler(LPCTSTR executableFilePath, LPSTR cmdLine)
{
    SECURITY_ATTRIBUTES pipeAttributes;
    HANDLE readPipe = INVALID_HANDLE_VALUE;
    HANDLE writePipe = INVALID_HANDLE_VALUE;
    DWORD error;

    ZeroMemory(&pipeAttributes, sizeof(pipeAttributes));
    pipeAttributes.nLength = sizeof(pipeAttributes);
    pipeAttributes.bInheritHandle = TRUE;
    if (CreatePipe(&readPipe, &writePipe, &pipeAttributes, 0))
    {
        /*
         * Do not let the child process inherit the readPipe because it does not
         * need it.
         */
        HANDLE currentProcess = GetCurrentProcess();
        HANDLE readPipeDuplicate = INVALID_HANDLE_VALUE;

        if (DuplicateHandle(
                currentProcess, readPipe,
                currentProcess, &readPipeDuplicate,
                0,
                FALSE,
                DUPLICATE_SAME_ACCESS))
        {
            CloseHandle(readPipe);
            readPipe = readPipeDuplicate;

            error
                = Run_runAsCrashHandlerWithPipe(
                        executableFilePath, cmdLine,
                        &readPipe, &writePipe);
        }
        else
            error = GetLastError();

        if (INVALID_HANDLE_VALUE != readPipe)
            CloseHandle(readPipe);
        if (INVALID_HANDLE_VALUE != writePipe)
            CloseHandle(writePipe);
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_runAsCrashHandlerWithPipe(
    LPCTSTR executableFilePath, LPSTR cmdLine,
    HANDLE *readPipe, HANDLE *writePipe)
{
    LPCTSTR commandLineFormat = _T("run.exe --channel=%d %s");
    int commandLineLength = 256 + _tcslen(cmdLine);
    LPTSTR commandLine
        = (LPTSTR) malloc(sizeof(TCHAR) * (commandLineLength + 1));
    DWORD error;

    if (commandLine)
    {
        LPTSTR lockFilePath = Run_getLockFilePath();
        DWORD exitCode = 0;

        commandLineLength
            = _sntprintf(
                    commandLine,
                    commandLineLength,
                    commandLineFormat,
                    (int) (*writePipe),
                    cmdLine);
        if (commandLineLength < 0)
        {
            free(commandLine);
            commandLine = NULL;
            error = ERROR_NOT_ENOUGH_MEMORY;
        }
        else
        {
            *(commandLine + commandLineLength) = 0;
            error = ERROR_SUCCESS;
        }

        if (ERROR_SUCCESS == error)
        {
            BOOL waitForChildProcess
                = !(lockFilePath && Run_isFile(lockFilePath));
            STARTUPINFO si;
            PROCESS_INFORMATION pi;

            ZeroMemory(&si, sizeof(si));
            si.cb = sizeof(si);
            if (CreateProcess(
                    executableFilePath,
                    commandLine,
                    NULL,
                    NULL,
                    TRUE,
                    CREATE_NO_WINDOW,
                    NULL,
                    NULL,
                    &si,
                    &pi))
            {
                HANDLE childProcessToWaitFor = NULL;
                DWORD event;

                /* We didn't really want to hold on to the thread. */
                CloseHandle(pi.hThread);

                /*
                 * The command line of the child process is no longer necessary.
                 */
                free(commandLine);
                commandLine = NULL;

                /*
                 * The child process has inherited the writePipe so close it in
                 * the current process in order to let it know that it is only
                 * waiting for the child process.
                 */
                CloseHandle(*writePipe);
                *writePipe = INVALID_HANDLE_VALUE;

                /*
                 * Wait for the child process to tell the current process if it
                 * is to wait for another child process in order to get the exit
                 * code from it.
                 */
                if (INVALID_HANDLE_VALUE != *readPipe)
                {
                    DWORD childToWaitFor[2];
                    DWORD numberOfBytesRead = 0;

                    if (ReadFile(
                                *readPipe,
                                childToWaitFor,
                                sizeof(childToWaitFor),
                                &numberOfBytesRead,
                                NULL)
                            && (numberOfBytesRead
                                    == sizeof(childToWaitFor))
                            && childToWaitFor[0]
                            && childToWaitFor[1])
                    {
                        error
                            = Run_openProcessAndResumeThread(
                                    childToWaitFor[0],
                                    childToWaitFor[1],
                                    &childProcessToWaitFor);
                    }
                    CloseHandle(*readPipe);
                    *readPipe = INVALID_HANDLE_VALUE;
                }
                if (childProcessToWaitFor)
                {
                    /*
                     * We'll have to wait for another process, not the one that
                     * we have just created ourselves.
                     */
                    CloseHandle(pi.hProcess);
                }
                else
                    childProcessToWaitFor = pi.hProcess;

                if (waitForChildProcess)
                {
                    error = ERROR_SUCCESS;
                    do
                    {
                        event
                            = WaitForSingleObject(
                                    childProcessToWaitFor,
                                    INFINITE);
                        if (WAIT_FAILED == event)
                        {
                            error = GetLastError();
                            break;
                        }
                    }
                    while (WAIT_TIMEOUT == event);

                    if ((ERROR_SUCCESS == error)
                            && !GetExitCodeProcess(
                                    childProcessToWaitFor,
                                    &exitCode))
                        error = GetLastError();
                }

                CloseHandle(childProcessToWaitFor);
            }
            else
                error = GetLastError();
        }

        if (commandLine)
            free(commandLine);

        if (lockFilePath)
        {
            /*
             * Notify the user if the application has crashed and ask whether it
             * is to be relaunched.
             */
            if ((ERROR_SUCCESS == error) && exitCode)
            {
                error
                    = Run_handleLauncherExitCode(
                            exitCode, lockFilePath,
                            executableFilePath);
            }
            free(lockFilePath);
        }
    }
    else
        error = ERROR_OUTOFMEMORY;
    return error;
}

static DWORD
Run_runAsLauncher(LPCTSTR executableFilePath, LPSTR cmdLine)
{
    LPSTR commandLine;
    DWORD error = ERROR_SUCCESS;

    /* Parse the command line. */
    if (cmdLine)
    {
        size_t commandLineLength;
        LPCSTR channelArg = "--channel=";
        size_t channelArgLength = strlen(channelArg);

        commandLine = Run_skipWhitespace(cmdLine);
        commandLineLength = strlen(commandLine);

        /* Get the value of the "--channel=" command-line argument. */
        if ((commandLineLength > channelArgLength)
                && (strnicmp(commandLine, channelArg, channelArgLength) == 0))
        {
            commandLine += channelArgLength;
            if (!isspace(*commandLine))
            {
                HANDLE channel = (HANDLE) atoi(commandLine);
                DWORD flags;
                char ch;

                if (channel && GetHandleInformation(channel, &flags))
                {
                    /*
                     * Make sure channel will not be inherited by any child
                     * process.
                     */
                    HANDLE currentProcess = GetCurrentProcess();
                    HANDLE channelDuplicate = INVALID_HANDLE_VALUE;

                    if (DuplicateHandle(
                            currentProcess, channel,
                            currentProcess, &channelDuplicate,
                            0,
                            FALSE,
                            DUPLICATE_SAME_ACCESS))
                        Run_channel = channelDuplicate;
                    CloseHandle(channel);
                }

                /*
                 * Skip the value of the channelArg and the whitespace after it.
                 */
                while ((ch = *commandLine) && !isspace(ch))
                    commandLine++;
                commandLine = Run_skipWhitespace(commandLine);
            }
        }
    }
    else
        commandLine = cmdLine;

    /* Run the Java process in the directory of the executable file. */
    if (_tcslen(executableFilePath) <= MAX_PATH)
    {
        TCHAR path[MAX_PATH];
        DWORD pathCapacity = sizeof(path) / sizeof(TCHAR);
        LPTSTR filePart = NULL;
        DWORD pathLength
            = GetFullPathName(
                    executableFilePath,
                    pathCapacity, path, &filePart);

        if (!pathLength)
            error = GetLastError();
        else if (pathLength >= pathCapacity)
            error = ERROR_NOT_ENOUGH_MEMORY;
        else
        {
            /*
             * Strip the filePart because only the directory of the executable
             * file is necessary.
             */
            if (filePart && *filePart)
                *filePart = 0;
            if (!SetCurrentDirectory(path))
                error = GetLastError();
        }
    }

    /*
     * Add the .\native directory to the PATH because adding it to
     * java.library.path does not seem to always work.
     */
    Run_addPath(_T(".\\native"));

    error = Run_runJava(executableFilePath, commandLine);
    return error;
}

static DWORD
Run_runJava(LPCTSTR executableFilePath, LPSTR cmdLine)
{
    DWORD cdLength;
    DWORD error = ERROR_CALL_NOT_IMPLEMENTED;
    BOOL searchForJava = TRUE;

    Run_cmdLine = cmdLine;

    /* Try to use the private Java distributed with the application. */
    if ((cdLength = GetCurrentDirectory(0, NULL)))
    {
        LPTSTR cd = (LPTSTR) malloc(sizeof(TCHAR) * cdLength);

        if (cd)
        {
            cdLength = GetCurrentDirectory(cdLength, cd);
            if (cdLength)
            {
                if ((ERROR_SUCCESS != error) || searchForJava)
                    error = Run_runJavaFromJavaHome(cd, TRUE, &searchForJava);
            }
            else
                error = GetLastError();
            free(cd);
        }
        else
            error = ERROR_OUTOFMEMORY;
    }
    else
        error = GetLastError();

    /* Try to locate Java through the well-known enviroment variables. */
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaFromEnvVar(_T("JAVA_HOME"), &searchForJava);
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaFromEnvVar(_T("JRE_HOME"), &searchForJava);
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaFromEnvVar(_T("JDK_HOME"), &searchForJava);

    /* Try to locate Java through the Windows registry. */
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaFromRegKey(HKEY_CURRENT_USER, &searchForJava);
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaFromRegKey(HKEY_LOCAL_MACHINE, &searchForJava);

    /* Default to the Java in the PATH. */
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaExe(_T("javaw.exe"), TRUE, &searchForJava);
    if ((ERROR_SUCCESS != error) || searchForJava)
        error = Run_runJavaExe(_T("java.exe"), TRUE, &searchForJava);

    /* Notify the user that Java could not be found. */
    if ((ERROR_SUCCESS != error) || searchForJava)
    {
        DWORD_PTR arguments[] = { (DWORD_PTR) PRODUCTNAME };

        if (Run_displayMessageBoxFromString(
                IDS_JAVANOTFOUND, arguments,
                executableFilePath,
                MB_ICONSTOP | MB_OK))
        {
            /*
             * We have failed to locate Java but we've just notified the user
             * about this fact so the execution is according to plan.
             */
            error = ERROR_SUCCESS;
        }
        else
            error = GetLastError();
    }

    return error;
}

static DWORD
Run_runJavaExe(LPCTSTR javaExe, BOOL searchPath, BOOL *searchForJava)
{
    DWORD error;

    if (searchPath || Run_isFile(javaExe))
    {
        LPCTSTR applicationName;
        LPCTSTR fileName;
        LPTSTR commandLine = NULL;

        if (searchPath)
        {
            applicationName = NULL;
            fileName = javaExe;
        }
        else
        {
            applicationName = javaExe;
            fileName = _T("java.exe");
        }
        error = Run_getJavaExeCommandLine(fileName, &commandLine);

        if (ERROR_SUCCESS == error)
        {
            DWORD creationFlags;
            STARTUPINFO si;
            PROCESS_INFORMATION pi;

            creationFlags = CREATE_NO_WINDOW;
            if (INVALID_HANDLE_VALUE != Run_channel)
                creationFlags |= CREATE_SUSPENDED;
            ZeroMemory(&si, sizeof(si));
            si.cb = sizeof(si);
            if (CreateProcess(
                    applicationName,
                    commandLine,
                    NULL,
                    NULL,
                    TRUE,
                    creationFlags,
                    NULL,
                    NULL,
                    &si,
                    &pi))
            {
                *searchForJava = FALSE;

                /*
                 * Tell the parent process it will have to wait for java.exe and
                 * get its exit code.
                 */
                if (INVALID_HANDLE_VALUE != Run_channel)
                {
                    DWORD processAndThreadIds[2];
                    DWORD numberOfBytesWritten;

                    processAndThreadIds[0] = pi.dwProcessId;
                    processAndThreadIds[1] = pi.dwThreadId;
                    WriteFile(
                            Run_channel,
                            processAndThreadIds,
                            sizeof(processAndThreadIds),
                            &numberOfBytesWritten,
                            NULL);
                    FlushFileBuffers(Run_channel);
                    CloseHandle(Run_channel);
                    Run_channel = INVALID_HANDLE_VALUE;
                }

                CloseHandle(pi.hProcess);
                CloseHandle(pi.hThread);
            }
            else
                error = GetLastError();
        }

        if (commandLine)
            free(commandLine);
    }
    else
        error = ERROR_CALL_NOT_IMPLEMENTED;
    return error;
}

static DWORD
Run_runJavaFromEnvVar(LPCTSTR envVar, BOOL *searchForJava)
{
    TCHAR envVarValue[MAX_PATH + 1];
    DWORD envVarValueLength
        = GetEnvironmentVariable(envVar, envVarValue, sizeof(envVarValue));
    DWORD error;

    if (envVarValueLength)
    {
        if (envVarValueLength >= sizeof(envVarValue))
            error = ERROR_NOT_ENOUGH_MEMORY;
        else
            error = Run_runJavaFromJavaHome(envVarValue, TRUE, searchForJava);
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_runJavaFromJavaHome(
    LPCTSTR javaHome, BOOL searchForRuntimeLib,
    BOOL *searchForJava)
{
    DWORD error;

    if (Run_isDirectory(javaHome))
    {
        size_t javaHomeLength = _tcslen(javaHome);
        LPTSTR path
            = (LPTSTR) malloc(sizeof(TCHAR) * (javaHomeLength + 19 + 1));

        if (path)
        {
            if (javaHomeLength >= 1)
            {
                TCHAR *ch = (TCHAR *) (javaHome + (javaHomeLength - 1));

                if ((_T('\\') == *ch) || (_T('/') == *ch))
                {
                    *ch = 0;
                    javaHomeLength--;
                }
            }

            _tcscpy(path, javaHome);
            error = ERROR_CALL_NOT_IMPLEMENTED;

            if (searchForRuntimeLib)
            {
                _tcscpy(path + javaHomeLength, _T("\\bin\\client\\jvm.dll"));
                error = Run_runJavaFromRuntimeLib(path, searchForJava);

                if ((ERROR_SUCCESS != error) || *searchForJava)
                {
                    _tcscpy(
                            path + javaHomeLength,
                            _T("\\bin\\server\\jvm.dll"));
                    error = Run_runJavaFromRuntimeLib(path, searchForJava);
                }
            }

            if ((ERROR_SUCCESS != error) || *searchForJava)
            {
                if ((javaHomeLength >= 4)
                        && (_tcsnicmp(
                                    javaHome + javaHomeLength - 4,
                                    _T("\\jre"),
                                    4)
                                != 0))
                {
                    _tcscpy(path + javaHomeLength, _T("\\jre"));
                    error
                        = Run_runJavaFromJavaHome(
                                path, searchForRuntimeLib,
                                searchForJava);
                }

                if ((ERROR_SUCCESS != error) || *searchForJava)
                {
                    _tcscpy(path + javaHomeLength, _T("\\bin\\javaw.exe"));
                    error = Run_runJavaExe(path, FALSE, searchForJava);

                    if ((ERROR_SUCCESS != error) || *searchForJava)
                    {
                        _tcscpy(path + javaHomeLength, _T("\\bin\\java.exe"));
                        error = Run_runJavaExe(path, FALSE, searchForJava);
                    }
                }
            }

            free(path);
        }
        else
            error = ERROR_OUTOFMEMORY;
    }
    else
        error = ERROR_FILE_NOT_FOUND;
    return error;
}

static DWORD
Run_runJavaFromRegKey(HKEY key, BOOL *searchForJava)
{
    DWORD error;
    LPTSTR runtimeLib = NULL;
    LPTSTR javaHome = NULL;

    error = Run_getJavaPathsFromRegKey(key, &runtimeLib, &javaHome);
    if (ERROR_SUCCESS == error)
    {
        if (runtimeLib)
        {
            error = Run_runJavaFromRuntimeLib(runtimeLib, searchForJava);
            free(runtimeLib);
        }
        if (javaHome)
        {
            if ((ERROR_SUCCESS != error) || *searchForJava)
            {
                error
                    = Run_runJavaFromJavaHome(
                            javaHome,
                            NULL == runtimeLib,
                            searchForJava);
            }
            free(javaHome);
        }
    }
    return error;
}

static DWORD
Run_runJavaFromRuntimeLib(LPCTSTR runtimeLib, BOOL *searchForJava)
{
    if (Run_isFile(runtimeLib))
    {
        /* TODO Auto-generated method stub */
    }
    return ERROR_CALL_NOT_IMPLEMENTED;
}

static LPSTR
Run_skipWhitespace(LPSTR str)
{
    char ch;

    while ((ch = *str) && isspace(ch))
        str++;
    return str;
}

int CALLBACK
WinMain(HINSTANCE instance, HINSTANCE prevInstance, LPSTR cmdLine, int cmdShow)
{
    LPTSTR executableFilePath = NULL;
    DWORD error = Run_getExecutableFilePath(&executableFilePath);

    if (ERROR_SUCCESS == error)
    {
        BOOL runAsLauncher = FALSE;

        Run_equalsParentProcessExecutableFilePath(
                executableFilePath,
                &runAsLauncher);
        if (runAsLauncher)
        {
            error = Run_runAsLauncher(executableFilePath, cmdLine);

            if (ERROR_SUCCESS != error)
            {
                LPTSTR message;
                DWORD messageLength
                    = FormatMessage(
                            FORMAT_MESSAGE_ALLOCATE_BUFFER
                                | FORMAT_MESSAGE_FROM_SYSTEM,
                            NULL,
                            error,
                            LANG_USER_DEFAULT,
                            (LPTSTR) &message,
                            0,
                            NULL);

                if (messageLength)
                {
                    MessageBox(
                            NULL,
                            message,
                            executableFilePath,
                            MB_ICONERROR | MB_OK);
                    LocalFree(message);
                }
            }
        }
        else
        {
            while (Run_launch)
            {
                Run_launch = FALSE;
                error = Run_runAsCrashHandler(executableFilePath, cmdLine);
            }
        }
    }

    if (executableFilePath)
        free(executableFilePath);

    return 0;
}
