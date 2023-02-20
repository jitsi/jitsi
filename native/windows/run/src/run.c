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

// The Java invocation API doesn't support unicode
#ifdef UNICODE
#undef UNICODE
#endif

#ifdef _UNICODE
#undef _UNICODE
#endif

#include "run.h"

#include <windows.h>
#include <ctype.h> /* isspace */
#include <jni.h>
#include <psapi.h> /* GetModuleFileNameEx */
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <tchar.h>
#include <tlhelp32.h> /* CreateToolhelp32Snapshot */
#include <Shlobj.h> /* SHGetFolderPath */

#include <win_shared/nls.h>

#define JAVA_MAIN_CLASS "net.java.sip.communicator.launcher.Jitsi"

/**
 * The pipe through which the launcher is to communicate with the crash handler.
 */
static HANDLE Run_channel = INVALID_HANDLE_VALUE;

/**
 * The command line that the application has received as the <tt>cmdLine</tt>
 * function argument of its <tt>WinMain</tt> entry point and that is currently
 * unparsed i.e. the parts which have already been parsed are no longer present.
 */
static LPTSTR Run_cmdLine = NULL;

/**
 * The indicator which determines whether the crash handler is to launch the
 * application.
 */
static BOOL Run_launch = TRUE;

static DWORD Run_prependPath(LPCTSTR path);
static DWORD Run_callStaticVoidMain(JNIEnv *jniEnv, BOOL *searchForJava);
static int Run_displayMessageBoxFromString(DWORD textId, DWORD_PTR *textArgs, LPCTSTR caption, UINT type);
static DWORD Run_equalsParentProcessExecutableFilePath(LPCTSTR executableFilePath, BOOL *equals);
static DWORD Run_getExecutableFilePath(LPTSTR *executableFilePath);
static LPTSTR Run_getJavaLibraryPath();
static DWORD Run_getJavaVMOptionStrings(size_t head, TCHAR separator, size_t tail, LPTSTR *optionStrings, jint *optionStringCount);
static LPTSTR Run_getLockFilePath();
static DWORD Run_getParentProcessId(DWORD *ppid);
static DWORD Run_handleLauncherExitCode(DWORD exitCode, LPCTSTR lockFilePath, LPCTSTR executableFilePath);
static BOOL Run_isDirectory(LPCTSTR fileName);
static BOOL Run_isFile(LPCTSTR fileName);
static DWORD Run_openProcessAndResumeThread(DWORD processId, DWORD threadId, HANDLE *process);
static DWORD Run_runAsCrashHandler(LPCTSTR executableFilePath, LPTSTR cmdLine);
static DWORD Run_runAsCrashHandlerWithPipe(LPCTSTR executableFilePath, LPTSTR cmdLine, HANDLE *readPipe, HANDLE *writePipe);
static DWORD Run_runAsLauncher(LPCTSTR executableFilePath, LPTSTR cmdLine);
static DWORD Run_runJava(LPCTSTR executableFilePath, LPTSTR cmdLine);
static DWORD Run_runJavaFromJavaHome(LPCTSTR javaHome, BOOL searchForRuntimeLib, BOOL *searchForJava);
static DWORD Run_runJavaFromRuntimeLib(LPCTSTR runtimeLib, LPCTSTR javaHome, BOOL *searchForJava);
static LPTSTR Run_skipWhitespace(LPTSTR str);

typedef void (CALLBACK *SplashInit)();
typedef int (CALLBACK *SplashLoadFile)(const char* file);

static DWORD
Run_prependPath(LPCTSTR path)
{
    size_t pathLength = _tcslen(path);
    TCHAR envVar[32767];
    if (GetEnvironmentVariable(_T("PATH"), envVar + pathLength, (DWORD) (32767 - pathLength - 1)) == 0)
    {
        return GetLastError();
    }

    _tcsncpy(envVar, path, pathLength);
    envVar[pathLength] = ';';

    if (SetEnvironmentVariable(_T("PATH"), envVar))
        return ERROR_SUCCESS;
    else
        return GetLastError();
}

static DWORD
Run_callStaticVoidMain(JNIEnv *jniEnv, BOOL *searchForJava)
{
    LPSTR mainClassName;
    jclass mainClass;
    DWORD error;

    mainClassName = _strdup(JAVA_MAIN_CLASS);
    if (mainClassName)
    {
        LPSTR ch;

        for (ch = mainClassName; *ch; ch++)
            if (_T('.') == *ch)
                *ch = '/';
        mainClass = (*jniEnv)->FindClass(jniEnv, mainClassName);
        free(mainClassName);
    }
    else
        mainClass = NULL;
    if (mainClass)
    {
        jmethodID mainMethodID
            = (*jniEnv)->GetStaticMethodID(
                jniEnv,
                mainClass,
                "main",
                "([Ljava/lang/String;)V");

        if (mainMethodID)
        {
            jclass stringClass
                = (*jniEnv)->FindClass(jniEnv, "java/lang/String");

            if (stringClass)
            {
                int argc = 0;
                LPWSTR *argv = NULL;

                if (Run_cmdLine && _tcslen(Run_cmdLine))
                {
#if defined(UNICODE) || defined(_UNICODE)
                    argv = CommandLineToArgvW(Run_cmdLine, &argc);
                    error = argv ? ERROR_SUCCESS : GetLastError();
#else
                    LPWSTR cmdLineW = NLS_str2wstr(Run_cmdLine);
                    if (cmdLineW)
                    {
                        argv = CommandLineToArgvW(cmdLineW, &argc);
                        free(cmdLineW);
                        error = argv ? ERROR_SUCCESS : GetLastError();
                    }
                    else
                        error = ERROR_NOT_ENOUGH_MEMORY;
#endif
                }
                else
                    error = ERROR_SUCCESS;
                if (ERROR_SUCCESS == error)
                {
                    jobjectArray mainArgs
                        = (*jniEnv)->NewObjectArray(
                                jniEnv,
                                argc, stringClass, NULL);

                    if (mainArgs)
                    {
                        int i;

                        for (i = 0; (ERROR_SUCCESS == error) && (i < argc); i++)
                        {
                            LPWSTR arg = *(argv + i);
                            jstring mainArg
                                = (*jniEnv)->NewString(
                                        jniEnv,
                                        arg, (jsize) wcslen(arg));

                            if (mainArg)
                            {
                                (*jniEnv)->SetObjectArrayElement(
                                        jniEnv,
                                        mainArgs, i, mainArg);
                                if (JNI_TRUE
                                        == (*jniEnv)->ExceptionCheck(jniEnv))
                                    error = ERROR_FUNCTION_FAILED;
                            }
                            else
                                error = ERROR_NOT_ENOUGH_MEMORY;
                        }
                        if (argv)
                        {
                            LocalFree(argv);
                            argv = NULL;
                        }

                        if (ERROR_SUCCESS == error)
                        {
                            *searchForJava = FALSE;

                            /*
                             * The parent process will have to wait for and get
                             * the exit code of its child, not java.exe so it
                             * does not need telling who to wait for or get the
                             * exit code of.
                             */
                            if (INVALID_HANDLE_VALUE != Run_channel)
                            {
                                CloseHandle(Run_channel);
                                Run_channel = INVALID_HANDLE_VALUE;
                            }

                            (*jniEnv)->CallStaticVoidMethod(
                                    jniEnv,
                                    mainClass, mainMethodID, mainArgs);
                        }
                    }
                    else
                        error = ERROR_NOT_ENOUGH_MEMORY;

                    if (argv)
                        LocalFree(argv);
                }
            }
            else
                error = ERROR_CLASS_DOES_NOT_EXIST;
        }
        else
            error = ERROR_INVALID_FUNCTION;
    }
    else
        error = ERROR_CLASS_DOES_NOT_EXIST;

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
                        sizeof(parentProcessExecutableFilePath) / sizeof(TCHAR));

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
    DWORD capacity = sizeof(str) / sizeof(TCHAR);
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

        size_t executeFilePathSize = sizeof(TCHAR) * (length + 1);
        *executableFilePath = (LPTSTR) malloc(executeFilePathSize);
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

static LPTSTR
Run_getJavaLibraryPath()
{
    TCHAR installedNativePath[MAX_PATH];
    size_t installedNativePathLength = GetFullPathName(_T("native"), MAX_PATH, installedNativePath, NULL);
    if (!installedNativePathLength || installedNativePathLength >= MAX_PATH)
    {
        return _tcsdup(_T(""));
    }

    TCHAR javaSharedLibraryPath[MAX_PATH];
    HRESULT hr = SHGetFolderPath(NULL, CSIDL_COMMON_APPDATA, 0, 0/*SHGFP_TYPE_CURRENT*/, javaSharedLibraryPath);
    if (FAILED(hr))
    {
        return _tcsdup(_T(""));
    }

    TCHAR javaLibraryPath[MAX_PATH];
    memset(javaLibraryPath, 0, sizeof(javaLibraryPath));
    size_t javaLibraryPathLength = _sntprintf(javaLibraryPath, MAX_PATH, _T("%s;%s\\%s\\native"), installedNativePath, javaSharedLibraryPath, _T(PRODUCTNAME));
    if (javaLibraryPathLength < 0 || javaLibraryPathLength >= MAX_PATH)
    {
        return _tcsdup(_T(""));
    }

    return _tcsdup(javaLibraryPath);
}

static DWORD
Run_getJavaVMOptionStrings
    (size_t head, TCHAR separator, size_t tail,
        LPTSTR *optionStrings, jint *optionStringCount)
{
    LPTSTR javaLibraryPath = Run_getJavaLibraryPath();
    jint _optionStringCount = 0;
    DWORD error;

    if (javaLibraryPath)
    {
        WIN32_FIND_DATA findFileData;
        size_t jarCount = 0;
        HANDLE hFind = FindFirstFile("lib\\*.jar", &findFileData);
        if (hFind != INVALID_HANDLE_VALUE)
        {
            do
            {
                jarCount++;
            }
            while(FindNextFile(hFind, &findFileData));
            FindClose(hFind);
        }

        LPTSTR *classpath = calloc(jarCount + 2, MAX_PATH);
        classpath[0] = malloc(sizeof(TCHAR) * 7);
        _tcscpy(classpath[0], "config/");
        classpath[0][6] = '\0';
        hFind = FindFirstFile("lib\\*.jar", &findFileData);
        if (hFind != INVALID_HANDLE_VALUE)
        {
            size_t pos = 1;
            do
            {
                classpath[pos] = (LPTSTR)malloc(sizeof(TCHAR) * (_tcslen(findFileData.cFileName) + 5));
                sprintf(classpath[pos], "lib\\%s", findFileData.cFileName);
                ++pos;
            }
            while(FindNextFile(hFind, &findFileData));
            FindClose(hFind);
        }

        LPCTSTR properties[]
            = {
                _T("-Dlogback.configurationFile"), _T("config/logback.xml"),
                _T("-Djava.library.path"), javaLibraryPath,
                _T("-Djna.library.path"), javaLibraryPath,
                _T("--add-opens"), _T("java.base/jdk.internal.loader=ALL-UNNAMED"),
                _T("--add-opens"), _T("java.base/java.lang=ALL-UNNAMED"),
                NULL
            };

        size_t classpathLength = 0;
        size_t propertiesLength = 0;
        BOOL quote = separator;

        {
            LPCTSTR cp = NULL;
            size_t i = 0;

            classpathLength = 0;
            while ((cp = classpath[i++]) && *cp != '\0')
                classpathLength += (_tcslen(cp) + 1 /* ';' */);
            if (classpathLength > 0)
                classpathLength += 18 /* "-Djava.class.path=" */;
        }
        {
            LPCTSTR property;
            size_t i = 0;

            propertiesLength = 0;
            while ((property = properties[i++]))
            {
                propertiesLength
                    += (2 /* "\"-D" */
                        + _tcslen(property)
                        + 1 /* '=' */
                        + _tcslen(properties[i++])
                        + 1 /* ' ' */);
                if (quote)
                    propertiesLength += 2;
            }
        }

        size_t optionStringsSize = head
                                   + classpathLength
                                   + propertiesLength
                                   + 1 /* 0 */
                                   + tail;
        *optionStrings = (LPTSTR)malloc(sizeof(TCHAR) * optionStringsSize);
        if (*optionStrings)
        {
            LPTSTR str = (*optionStrings) + head;

            if (classpathLength > 0)
            {
                LPCTSTR cp;
                size_t i = 0;

                _tcscpy(str, _T("-Djava.class.path="));
                str += 18;
                while ((cp = classpath[i++]) && *cp != '\0')
                {
                    size_t length = _tcslen(cp);

                    _tcsncpy(str, cp, length);
                    str += length;
                    *str = _T(';');
                    str++;
                }
                str--; /* Drop the last ';'. */
                *str = separator;
                str++;
                _optionStringCount++;
            }
            if (propertiesLength > 0)
            {
                LPCTSTR property;
                size_t i = 0;

                while ((property = properties[i++]))
                {
                    size_t length;
                    LPCTSTR value;

                    if (quote)
                        *str++ = _T('"');
                    length = _tcslen(property);
                    _tcsncpy(str, property, length);
                    str += length;
                    *str++ = _T('=');

                    value = properties[i++];
                    length = _tcslen(value);
                    _tcsncpy(str, value, length);
                    str += length;
                    if (quote)
                        *str++ = _T('"');
                    *str++ = separator;

                    _optionStringCount++;
                }
            }
            *str = 0;

            if (optionStringCount)
                *optionStringCount = _optionStringCount;
            error = ERROR_SUCCESS;
        }
        else
            error = ERROR_OUTOFMEMORY;

        free(javaLibraryPath);
    }
    else
        error = ERROR_OUTOFMEMORY;

    return error;
}

static LPTSTR
Run_getLockFilePath()
{
    TCHAR appData[MAX_PATH + 1];
    DWORD appDataCapacity = sizeof(appData) / sizeof(TCHAR);
    DWORD appDataLength
        = GetEnvironmentVariable(
                _T("LOCALAPPDATA"),
                appData, appDataCapacity);
    LPTSTR lockFilePath = NULL;

    if (appDataLength && (appDataLength < appDataCapacity))
    {
        LPCTSTR productName = _T(PRODUCTNAME);
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

static DWORD
Run_handleLauncherExitCode(
    DWORD exitCode, LPCTSTR lockFilePath,
    LPCTSTR executableFilePath)
{
    DWORD error = ERROR_SUCCESS;

    if (Run_isFile(lockFilePath))
    {
        DWORD_PTR arguments[] = { (DWORD_PTR) _T(PRODUCTNAME) };
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
Run_runAsCrashHandler(LPCTSTR executableFilePath, LPTSTR cmdLine)
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
    LPCTSTR executableFilePath, LPTSTR cmdLine,
    HANDLE *readPipe, HANDLE *writePipe)
{
    LPCTSTR commandLineFormat = _T("run.exe --channel=%d %s");
    size_t commandLineLength = 256 + _tcslen(cmdLine);
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
                    (int) (intptr_t) (*writePipe),
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
Run_runAsLauncher(LPCTSTR executableFilePath, LPTSTR cmdLine)
{
    LPTSTR commandLine;
    DWORD error = ERROR_SUCCESS;

    /* Parse the command line. */
    if (cmdLine)
    {
        size_t commandLineLength;
        LPCTSTR channelArg = _T("--channel=");
        size_t channelArgLength = _tcslen(channelArg);

        commandLine = Run_skipWhitespace(cmdLine);
        commandLineLength = _tcslen(commandLine);

        /* Get the value of the "--channel=" command-line argument. */
        if ((commandLineLength > channelArgLength)
                && (_tcsnicmp(commandLine, channelArg, channelArgLength) == 0))
        {
            commandLine += channelArgLength;
            if (!isspace(*commandLine))
            {
                HANDLE channel = (HANDLE) (intptr_t) _ttoi(commandLine);
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

    error = Run_runJava(executableFilePath, commandLine);
    return error;
}

static DWORD
Run_runJava(LPCTSTR executableFilePath, LPTSTR cmdLine)
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

    /* Notify the user that Java could not be found. */
    if ((ERROR_SUCCESS != error) || searchForJava)
    {
        DWORD_PTR arguments[] = { (DWORD_PTR) _T(PRODUCTNAME) };

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
                error
                    = Run_runJavaFromRuntimeLib(path, javaHome, searchForJava);

                if ((ERROR_SUCCESS != error) || *searchForJava)
                {
                    _tcscpy(
                            path + javaHomeLength,
                            _T("\\bin\\server\\jvm.dll"));
                    error
                        = Run_runJavaFromRuntimeLib(
                            path,
                            javaHome,
                            searchForJava);
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
Run_runJavaFromRuntimeLib
    (LPCTSTR runtimeLib, LPCTSTR javaHome, BOOL *searchForJava)
{
    HMODULE hRuntimeLib;
    DWORD error;

    if (Run_isFile(runtimeLib))
    {
        /*
         * It turns out that the bin directory in javaHome may contain
         * dependencies of the runtimeLib so add it to the PATH. Well, it may
         * not be standard but it happens to our private JRE.
         */
        if (javaHome && Run_isDirectory(javaHome))
        {
            size_t javaHomeLength = _tcslen(javaHome);
            LPTSTR javaHomeBin;

            /*
             * Drop the last file name separator if any because we will be
             * adding one later on.
             */
            while (javaHomeLength >= 1)
            {
                TCHAR ch = *(javaHome + (javaHomeLength - 1));

                if ((_T('\\') == ch) || (_T('/') == ch))
                    javaHomeLength--;
                else
                    break;
            }

            javaHomeBin
                = malloc(
                    sizeof(TCHAR) * (javaHomeLength + 4 /* "\\bin" */ + 1));
            if (javaHomeBin)
            {
                LPTSTR str = javaHomeBin;

                _tcsncpy(str, javaHome, javaHomeLength);
                str += javaHomeLength;
                _tcsncpy(str, _T("\\bin"), 4);
                str += 4;
                *str = 0;

                if (Run_isDirectory(javaHomeBin))
                    Run_prependPath(javaHomeBin);

                free(javaHomeBin);
            }
        }

        hRuntimeLib = LoadLibrary(runtimeLib);
    }
    else
        hRuntimeLib = NULL;

    if (hRuntimeLib)
    {
        typedef jint (JNICALL *JNICreateJavaVMFunc)(JavaVM **, void **, void *);
        JNICreateJavaVMFunc jniCreateJavaVM
            = (JNICreateJavaVMFunc)
                GetProcAddress(hRuntimeLib, "JNI_CreateJavaVM");

        if (jniCreateJavaVM)
        {
            LPTSTR optionStrings = NULL;
            jint optionStringCount = 0;

            error
                = Run_getJavaVMOptionStrings(
                    0, 0, 0,
                    &optionStrings, &optionStringCount);
            if (ERROR_SUCCESS == error)
            {
                JavaVMOption *options
                    = calloc(optionStringCount, sizeof(JavaVMOption));

                if (options)
                {
                    jint i;
                    LPTSTR optionString;
                    JavaVMInitArgs javaVMInitArgs;
                    JavaVM *javaVM;
                    JNIEnv *jniEnv;

                    for (i = 0, optionString = optionStrings;
                        i < optionStringCount;
                        i++, optionString += (_tcslen(optionString) + 1))
                    {
                        options[i].optionString = optionString;
                    }

                    javaVMInitArgs.ignoreUnrecognized = JNI_FALSE;
                    javaVMInitArgs.nOptions = optionStringCount;
                    javaVMInitArgs.options = options;
                    javaVMInitArgs.version = JNI_VERSION_10;

                    HMODULE hSplash = NULL;
                    LPTSTR lockFilePath = Run_getLockFilePath();

                    if(!(lockFilePath && Run_isFile(lockFilePath)))
                    {// Lets load and start splashscreen, if any
                        size_t javaHomeLength = _tcslen(javaHome);
                        LPTSTR path
                            = (LPTSTR) malloc(sizeof(TCHAR)
                                * (javaHomeLength + 20 + 1));
                        if (path)
                        {
                            if (javaHomeLength >= 1)
                            {
                                TCHAR *ch =
                                    (TCHAR *) (javaHome + (javaHomeLength - 1));

                                if ((_T('\\') == *ch) || (_T('/') == *ch))
                                {
                                    *ch = 0;
                                    javaHomeLength--;
                                }
                            }

                            _tcscpy(path, javaHome);
                            _tcscpy(path + javaHomeLength,
                                    _T("\\bin\\splashscreen.dll"));
                            hSplash = LoadLibrary(path);

                            if(hSplash > 0)
                            {
                                SplashInit splashInit =
                                    (SplashInit)GetProcAddress(
                                        hSplash, "SplashInit");
                                SplashLoadFile splashLoadFile =
                                    (SplashLoadFile)GetProcAddress(
                                        hSplash, "SplashLoadFile");

                                if (splashInit && splashLoadFile)
                                {
                                    splashInit();
                                    splashLoadFile("splash.gif");
                                }
                            }
                        }
                    }

                    if (jniCreateJavaVM(&javaVM, (void **) &jniEnv, &javaVMInitArgs) != JNI_OK)
                    {
                        error = ERROR_FUNCTION_FAILED;
                    }
                    else
                    {
                        free(options);
                        options = NULL;
                        free(optionStrings);
                        optionStrings = NULL;

                        error = Run_callStaticVoidMain(jniEnv, searchForJava);
                        if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                        {
                            (*jniEnv)->ExceptionDescribe(jniEnv);
                            (*jniEnv)->ExceptionClear(jniEnv);
                        }

                        (*javaVM)->DestroyJavaVM(javaVM);
                    }
                    if (options)
                        free(options);

                    if(hSplash)
                        FreeLibrary(hSplash);
                }
                else
                    error = ERROR_OUTOFMEMORY;
                if (optionStrings)
                    free(optionStrings);
            }
        }
        else
            error = GetLastError();
        FreeLibrary(hRuntimeLib);
    }
    else
        error = GetLastError();
    return error;
}

static LPTSTR
Run_skipWhitespace(LPTSTR str)
{
    TCHAR ch;

    while ((ch = *str) && isspace(ch))
        str++;
    return str;
}

int CALLBACK
WinMain(HINSTANCE instance, HINSTANCE prevInstance, LPTSTR cmdLine, int cmdShow)
{
    LPTSTR executableFilePath = NULL;
    DWORD error;

    AttachConsole(ATTACH_PARENT_PROCESS);

    error = Run_getExecutableFilePath(&executableFilePath);
    if (ERROR_SUCCESS == error)
    {
        BOOL runAsLauncher = FALSE;

        Run_equalsParentProcessExecutableFilePath(
                executableFilePath,
                &runAsLauncher);
        if (1 || runAsLauncher)
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
