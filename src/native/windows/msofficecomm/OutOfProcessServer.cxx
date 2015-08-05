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
#include "OutOfProcessServer.h"

#include "Log.h"
#include "Messenger.h"
#include "MessengerClassFactory.h"
#include "MessengerContact.h"
#include "net_java_sip_communicator_plugin_msofficecomm_OutOfProcessServer.h"
#include "process.h"

EXTERN_C const GUID DECLSPEC_SELECTANY LIBID_CommunicatorUA
    = { 0x2B317E1D, 0x50E5, 0x4f5e, { 0xA3, 0xA4, 0xFB, 0x85, 0x20, 0x6E, 0xDA, 0x48 } };

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_plugin_msofficecomm_OutOfProcessServer_start
    (JNIEnv *env, jclass clazz)
{
    LPSTR functionName = ::strdup(__FUNCTION__);
    LPSTR packageName;

    if (functionName)
    {
        packageName = functionName + 5 /* Java_ */;

        size_t packageNameLength
            = ::strlen(packageName) - 24 /* OutOfProcessServer_start */;

        packageName[packageNameLength] = '\0';

        char ch;
        LPSTR str = packageName;

        while ((ch = *str))
        {
            if ('_' == ch)
                *str = '/';
            str++;
        }
    }
    else
        packageName = NULL;

    jint ret = OutOfProcessServer::start(env, clazz, packageName);

    if (functionName)
        ::free(functionName);

    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_plugin_msofficecomm_OutOfProcessServer_stop
    (JNIEnv *env, jclass clazz)
{
    return OutOfProcessServer::stop(env, clazz);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    return OutOfProcessServer::JNI_OnLoad(vm);
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    OutOfProcessServer::JNI_OnUnload(vm);
}

CRITICAL_SECTION OutOfProcessServer::_criticalSection;
LPTYPELIB        OutOfProcessServer::_iTypeLib;
ClassFactory *   OutOfProcessServer::_messengerClassFactory = NULL;
LPSTR            OutOfProcessServer::_packageName;
HANDLE           OutOfProcessServer::_threadHandle;
DWORD            OutOfProcessServer::_threadId;
JavaVM *         OutOfProcessServer::_vm = NULL;

LPSTR OutOfProcessServer::getClassName(LPCSTR className)
{
    size_t packageNameLength = _packageName ? ::strlen(_packageName) : 0;
    size_t classNameLength = ::strlen(className);
    LPSTR ret = (LPSTR) ::malloc(packageNameLength + classNameLength + 1);

    if (ret)
    {
        LPSTR str = ret;

        if (packageNameLength)
        {
            ::memcpy(str, _packageName, packageNameLength);
            str += packageNameLength;
        }
        if (classNameLength)
        {
            ::memcpy(str, className, classNameLength);
            str += classNameLength;
        }
        *str = '\0';
    }
    return ret;
}

BOOL OutOfProcessServer::isMicrosoftOfficeOutlookCallIntegrationIMApplication()
{
    TCHAR path[MAX_PATH + 1];
    DWORD pathCapacity = sizeof(path) / sizeof(TCHAR);
    DWORD pathLength = ::GetModuleFileName(NULL, path, pathCapacity);
    BOOL b;

    if (pathLength && (pathLength < pathCapacity))
    {
        LPTSTR fileName = NULL;

        for (LPTSTR str = path + (pathLength - 1); str != path; str--)
        {
            TCHAR ch = *str;

            if (('\\' == ch) || ('/' == ch))
            {
                fileName = str + 1;
                break;
            }
        }
        if (fileName && *fileName)
        {
            DWORD dataSize = (pathLength + 2) * sizeof(TCHAR);
            LPBYTE data = (LPBYTE) ::malloc(dataSize);

            if (data)
            {
                SYSTEM_INFO systemInfo;
                REGSAM alternatives86[] = { 0 };
                REGSAM alternatives64[] = { KEY_WOW64_32KEY, KEY_WOW64_64KEY };
                REGSAM *alternatives;
                size_t alternativeCount;

                ::GetNativeSystemInfo(&systemInfo);
                if (PROCESSOR_ARCHITECTURE_INTEL
                        == systemInfo.wProcessorArchitecture)
                {
                    alternatives = alternatives86;
                    alternativeCount = sizeof(alternatives86) / sizeof(REGSAM);
                }
                else
                {
                    alternatives = alternatives64;
                    alternativeCount = sizeof(alternatives64) / sizeof(REGSAM);
                }

                LPCTSTR key
                    = _T("SOFTWARE\\Microsoft\\Office\\Outlook\\Call Integration");
                LPCTSTR valueName = _T("IMApplication");
                size_t fileNameLength = ::_tcslen(fileName);

                b = FALSE;
                for (size_t i = 0; i < alternativeCount; i++)
                {
                    HKEY hkey;

                    if (::RegOpenKeyEx(
                                HKEY_LOCAL_MACHINE,
                                key,
                                0,
                                KEY_QUERY_VALUE | alternatives[i],
                                &hkey)
                            == ERROR_SUCCESS)
                    {
                        DWORD type;

                        dataSize = (pathLength + 1) * sizeof(TCHAR);
                        ::ZeroMemory(data, dataSize + sizeof(TCHAR));
                        if ((::RegQueryValueEx(
                                        hkey,
                                        valueName,
                                        NULL,
                                        &type,
                                        data,
                                        &dataSize)
                                    == ERROR_SUCCESS)
                                && (REG_SZ == type))
                        {
                            b
                                = (::_tcsnicmp(
                                            fileName,
                                            (LPCTSTR) data,
                                            fileNameLength)
                                        == 0);
                        }
                        ::RegCloseKey(hkey);

                        if (b)
                            break;
                    }
                }
                ::free(data);
            }
            else
                b = FALSE;
        }
        else
            b = FALSE;
    }
    else
        b = FALSE;
    return b;
}

jint OutOfProcessServer::JNI_OnLoad(JavaVM *vm)
{
    jint version;

    if (isMicrosoftOfficeOutlookCallIntegrationIMApplication())
    {
        ::InitializeCriticalSection(&_criticalSection);
        _vm = vm;
        version = JNI_VERSION_1_4;
    }
    else
        version = JNI_ERR;
    return version;
}

HRESULT OutOfProcessServer::loadRegTypeLib()
{
    /*
     * Microsoft Office will need the Office Communicator 2007 API to be able to
     * talk to us. Make sure it is available.
     */

    LPTYPELIB iTypeLib;
    HRESULT hr = ::LoadRegTypeLib(LIBID_CommunicatorUA, 1, 0, 0, &iTypeLib);

    if (SUCCEEDED(hr))
        _iTypeLib = iTypeLib;
    else
    {
        HMODULE module;

        _iTypeLib = NULL;

        if (::GetModuleHandleEx(
                GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                    | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                (LPCTSTR) (OutOfProcessServer::loadRegTypeLib),
                &module))
        {
            WCHAR path[MAX_PATH + 1];
            DWORD pathCapacity = sizeof(path) / sizeof(WCHAR);
            DWORD pathLength = ::GetModuleFileNameW(module, path, pathCapacity);

            if (pathLength && (pathLength < pathCapacity))
            {
                hr = ::LoadTypeLibEx(path, REGKIND_NONE, &iTypeLib);
                if (SUCCEEDED(hr))
                {
                    HMODULE oleaut32 = ::GetModuleHandle(_T("oleaut32.dll"));

                    if (oleaut32)
                    {
                        typedef HRESULT (WINAPI *RTLFU)(LPTYPELIB,LPOLESTR,LPOLESTR);
                        RTLFU registerTypeLibForUser
                            = (RTLFU)
                                ::GetProcAddress(
                                        oleaut32,
                                        "RegisterTypeLibForUser");

                        if (registerTypeLibForUser)
                        {
                            hr = registerTypeLibForUser(iTypeLib, path, NULL);
                            if (SUCCEEDED(hr))
                            {
                                /*
                                 * The whole point of what has been done till
                                 * now is securing the success of future calls
                                 * to LoadRegTypeLib. Make sure that is indeed
                                 * the case.
                                 */

                                iTypeLib->Release();

                                hr
                                    = ::LoadRegTypeLib(
                                            LIBID_CommunicatorUA,
                                            1,
                                            0,
                                            0,
                                            &iTypeLib);
                                if (SUCCEEDED(hr))
                                    _iTypeLib = iTypeLib;
                            }
                        }
                        else
                            hr = E_UNEXPECTED;
                    }
                    else
                        hr = E_UNEXPECTED;
                    if (iTypeLib != _iTypeLib)
                        iTypeLib->Release();
                }
            }
        }
    }
    return hr;
}

DWORD
OutOfProcessServer::regCreateKeyAndSetValue
    (LPCTSTR key, LPCTSTR valueName, DWORD data)
{
    SYSTEM_INFO systemInfo;
    REGSAM alternatives86[] = { 0 };
    REGSAM alternatives64[] = { KEY_WOW64_32KEY, KEY_WOW64_64KEY };
    REGSAM *alternatives;
    size_t alternativeCount;

    ::GetNativeSystemInfo(&systemInfo);
    if (PROCESSOR_ARCHITECTURE_INTEL == systemInfo.wProcessorArchitecture)
    {
        alternatives = alternatives86;
        alternativeCount = sizeof(alternatives86) / sizeof(REGSAM);
    }
    else
    {
        alternatives = alternatives64;
        alternativeCount = sizeof(alternatives64) / sizeof(REGSAM);
    }

    DWORD lastError;

    for (size_t i = 0; i < alternativeCount; i++)
    {
        HKEY hkey;

        lastError
            = ::RegCreateKeyEx(
                    HKEY_CURRENT_USER,
                    key,
                    0,
                    NULL,
                    REG_OPTION_VOLATILE,
                    KEY_SET_VALUE | alternatives[i],
                    NULL,
                    &hkey,
                    NULL);
        if (ERROR_SUCCESS == lastError)
        {
            lastError
                = ::RegSetValueEx(
                        hkey,
                        valueName,
                        0,
                        REG_DWORD,
                        (const BYTE *) &data,
                        sizeof(data));
            ::RegCloseKey(hkey);
        }
        if (ERROR_SUCCESS != lastError)
            break;
    }
    return lastError;
}

HRESULT OutOfProcessServer::registerClassObjects()
{
    ClassFactory *classObject = new MessengerClassFactory();
    HRESULT hresult = classObject->registerClassObject();

    if (SUCCEEDED(hresult))
        _messengerClassFactory = classObject;
    else
        classObject->Release();

    if (SUCCEEDED(hresult))
    {
        hresult = ::CoResumeClassObjects();
        if (FAILED(hresult))
            revokeClassObjects();
    }

    return hresult;
}

ULONG OutOfProcessServer::releaseTypeLib()
{
    // TODO UnRegisterTypeLibForUser
    return _iTypeLib->Release();
}

HRESULT OutOfProcessServer::revokeClassObjects()
{
    HRESULT ret = ::CoSuspendClassObjects();

    if (SUCCEEDED(ret))
    {
        ClassFactory *classObject = _messengerClassFactory;

        if (classObject)
        {
            _messengerClassFactory = NULL;

            HRESULT hr = classObject->revokeClassObject();

            classObject->Release();
            if (FAILED(hr))
                ret = hr;
        }
    }
    return ret;
}

unsigned __stdcall OutOfProcessServer::run(void *)
{
    Log::open();

    HRESULT hr = ::CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    unsigned ret = 0;

    if (SUCCEEDED(hr))
    {
        hr = loadRegTypeLib();
        if (SUCCEEDED(hr))
        {
            if (ERROR_SUCCESS == setIMProvidersCommunicatorUpAndRunning(1))
            {
                MSG msg;

                /*
                 * Create the message queue of this thread before any other part
                 * of the code (e.g. the release method) has a chance to invoke
                 * PostThreadMessage.
                 */
                ::PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);

                hr = registerClassObjects();
                if (SUCCEEDED(hr))
                {
                    if (ERROR_SUCCESS
                            == setIMProvidersCommunicatorUpAndRunning(2))
                    {
                        HANDLE threadHandle = _threadHandle;
                        BOOL logMsgWaitForMultipleObjectsExFailed = TRUE;
                        BOOL quit = FALSE;

                        do
                        {
                            /*
                             * Enable the use of the QueueUserAPC function by
                             * entering an alertable state.
                             */
                            if ((WAIT_FAILED
                                        == ::MsgWaitForMultipleObjectsEx(
                                                1,
                                                &threadHandle,
                                                INFINITE,
                                                QS_ALLINPUT | QS_ALLPOSTMESSAGE,
                                                MWMO_ALERTABLE
                                                    | MWMO_INPUTAVAILABLE))
                                    && logMsgWaitForMultipleObjectsExFailed)
                            {
                                /*
                                 * Logging the possible failures of the calls to
                                 * MsgWaitForMultipleObjectsEx multiple times is
                                 * unlikely to be useful. Besides, the call in
                                 * question is performed inside the message loop
                                 * and the logging will be an unnecessary
                                 * performance penalty.
                                 */
                                logMsgWaitForMultipleObjectsExFailed = FALSE;
                                Log::d(
                                        _T("OutOfProcessServer::run:")
                                        _T(" MsgWaitForMultipleObjectsEx=WAIT_FAILED;")
                                        _T("\n"));
                            }
                            while (::PeekMessage(&msg, NULL, 0, 0, PM_REMOVE))
                            {
                                if (WM_QUIT == msg.message)
                                {
                                    quit = TRUE;
                                    ret = msg.wParam;
                                    break;
                                }
                                else if (msg.hwnd)
                                {
                                    ::TranslateMessage(&msg);
                                    ::DispatchMessage(&msg);
                                }
                            }
                        }
                        while (!quit);
                    }

                    revokeClassObjects();
                }
            }

            /*
             * Even if setIMProvidersCommunicatorUpAndRunning(DWORD) failed, it
             * may have successfully set some of the multiple related registry
             * keys.
             */
            setIMProvidersCommunicatorUpAndRunning(0);

            releaseTypeLib();
        }

        ::CoUninitialize();
    }

    Log::close();
    return ret;
}

DWORD OutOfProcessServer::setIMProvidersCommunicatorUpAndRunning(DWORD dw)
{
    DWORD lastError;

    if (dw)
    {
        /*
         * Testing on various machines/setups has shown that the following may
         * or may not succeed without affecting the presence integration so just
         * try them and then go on with the rest regardless of their success.
         */
        lastError = ERROR_SUCCESS;
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\11.0\\Common\\PersonaMenu"),
                _T("RTCApplication"),
                3);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\12.0\\Common\\PersonaMenu"),
                _T("RTCApplication"),
                3);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\11.0\\Common\\PersonaMenu"),
                _T("QueryServiceForStatus"),
                2);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\12.0\\Common\\PersonaMenu"),
                _T("QueryServiceForStatus"),
                2);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\11.0\\Outlook\\IM"),
                _T("SetOnlineStatusLevel"),
                3);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\12.0\\Outlook\\IM"),
                _T("SetOnlineStatusLevel"),
                3);
    }
    else
        lastError = ERROR_SUCCESS;
    if (ERROR_SUCCESS == lastError)
    {
        lastError
            = regCreateKeyAndSetValue(
                    _T("Software\\IM Providers\\Communicator"),
                    _T("UpAndRunning"),
                    dw);
    }
    return lastError;
}

HRESULT OutOfProcessServer::start(JNIEnv *env, jclass clazz, LPCSTR packageName)
{
    HRESULT hr;

    if (packageName)
        hr = ((_packageName = ::strdup(packageName))) ? S_OK : E_OUTOFMEMORY;
    else
    {
        _packageName = NULL;
        hr = S_OK;
    }

    if (SUCCEEDED(hr))
    {
        hr = Messenger::start(env);
        if (SUCCEEDED(hr))
        {
            hr = MessengerContact::start(env);
            if (SUCCEEDED(hr))
            {
                unsigned threadId;
                HANDLE threadHandle
                    = (HANDLE)
                        ::_beginthreadex(
                                NULL,
                                0,
                                OutOfProcessServer::run,
                                NULL,
                                CREATE_SUSPENDED,
                                &threadId);

                if (threadHandle)
                {
                    enterCriticalSection();

                    _threadHandle = threadHandle;
                    _threadId = (DWORD) threadId;
                    if (((DWORD) -1) == ::ResumeThread(threadHandle))
                    {
                        DWORD lastError = ::GetLastError();

                        _threadHandle = NULL;

                        ::CloseHandle(threadHandle);
                        hr = HRESULT_FROM_WIN32(lastError);
                    }

                    leaveCriticalSection();
                }
                else
                    hr = E_UNEXPECTED;

                if (FAILED(hr))
                    MessengerContact::stop(env);
            }

            if (FAILED(hr))
                Messenger::stop(env);
        }

        if (FAILED(hr) && _packageName)
        {
            ::free(_packageName);
            _packageName = NULL;
        }
    }

    return hr;
}

HRESULT OutOfProcessServer::stop(JNIEnv *env, jclass clazz)
{
    DWORD lastError;

    if (::PostThreadMessage(_threadId, WM_QUIT, 0, 0))
    {
        do
        {
            DWORD exitCode;

            if (::GetExitCodeThread(_threadHandle, &exitCode))
            {
                if (STILL_ACTIVE == exitCode)
                {
                    if (WAIT_FAILED
                            == ::WaitForSingleObject(_threadHandle, INFINITE))
                        break;
                }
                else
                    break;
            }
            else
                break;
        }
        while (1);

        if (::CloseHandle(_threadHandle))
            lastError = 0;
        else
            lastError = ::GetLastError();

        MessengerContact::stop(env);
        Messenger::stop(env);

        if (_packageName)
        {
            ::free(_packageName);
            _packageName = NULL;
        }
    }
    else
        lastError = ::GetLastError();
    return lastError ? HRESULT_FROM_WIN32(lastError) : S_OK;
}
