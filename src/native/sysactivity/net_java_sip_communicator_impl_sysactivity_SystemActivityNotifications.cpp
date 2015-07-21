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

#include "net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.h"

/**
 * \def WIN32_LEAN_AND_MEAN
 * \brief Excludes not commonly used headers from Win32 API.
 *
 * Excludes some unused stuff from Windows headers which allows the code to
 * compile faster.
 */
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <iphlpapi.h> /* CancelIPChangeNotify, NotifyAddrChange */
#include <process.h> /* _beginthreadex */
#include <stdint.h> /* uintptr_t */

static void SystemActivityNotifications_notify(jint type);
unsigned WINAPI SystemActivityNotifications_runMessageLoop(LPVOID);
LRESULT CALLBACK SystemActivityNotifications_wndProc(HWND, UINT, WPARAM, LPARAM);

/**
 * The Java object which has been set on the SystemActivityNotifications class
 * with a call to the setDelegate method.
 */
static jobject SystemActivityNotifications_delegate = NULL;
static OVERLAPPED SystemActivityNotifications_overlapped;
static JavaVM *SystemActivityNotifications_vm;

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_allocAndInit
    (JNIEnv* env, jclass clazz)
{
    (void) env;
    (void) clazz;

    HINSTANCE hInstance = GetModuleHandle(NULL);

    if (hInstance)
    {
        LPCWSTR lpszClassName = L"Jitsi SystemActivityNotifications Window";
        WNDCLASSEXW wcex;

        wcex.cbSize         = sizeof(WNDCLASSEXW);
        wcex.cbClsExtra     = 0;
        wcex.cbWndExtra     = 0;
        wcex.hbrBackground  = (HBRUSH) (COLOR_WINDOW + 1);
        wcex.hIcon          = 0;
        wcex.hIconSm        = 0;
        wcex.hCursor        = 0;
        wcex.hInstance      = hInstance;
        wcex.lpfnWndProc    = SystemActivityNotifications_wndProc;
        wcex.lpszClassName  = lpszClassName;
        wcex.lpszMenuName   = 0;
        wcex.style          = CS_HREDRAW | CS_VREDRAW;

        if (RegisterClassExW(&wcex))
        {
            HWND hWnd
                = CreateWindowW(
                        lpszClassName,
                        /* lpWindowName*/ NULL,
                        WS_OVERLAPPEDWINDOW,
                        /* x */ CW_USEDEFAULT,
                        /* y */ CW_USEDEFAULT,
                        /* nWidth */ CW_USEDEFAULT,
                        /* nHeight */ CW_USEDEFAULT,
                        /* hWndParent */ NULL,
                        /* hMenu */ NULL,
                        hInstance,
                        /* LPVOID */ NULL);

            if (hWnd)
            {
                ZeroMemory(
                        &SystemActivityNotifications_overlapped,
                        sizeof(SystemActivityNotifications_overlapped));

                uintptr_t thrdh
                    = _beginthreadex(
                            /* security */ NULL,
                            /* stack_size */ 0,
                            SystemActivityNotifications_runMessageLoop,
                            (LPVOID) hWnd,
                            /* initflag */ 0,
                            /* thrdaddr */ NULL);

                return (jlong) thrdh;
            }
        }
    }

    return 0;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_getLastInput
    (JNIEnv* env, jclass clazz)
{
    (void) env;
    (void) clazz;

    LASTINPUTINFO lii;

    lii.cbSize = sizeof(LASTINPUTINFO);
    lii.dwTime = 0;
    return GetLastInputInfo(&lii) ? (GetTickCount() - lii.dwTime) : -1;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_release
    (JNIEnv* env, jclass clazz, jlong ptr)
{
    Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_setDelegate(
            env,
            clazz,
            ptr,
            NULL);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_setDelegate
    (JNIEnv* env, jclass clazz, jlong ptr, jobject delegate)
{
    (void) clazz;
    (void) ptr;

    if (SystemActivityNotifications_delegate)
    {
        env->DeleteGlobalRef(SystemActivityNotifications_delegate);
        SystemActivityNotifications_delegate = NULL;
    }
    if (delegate)
    {
        delegate = env->NewGlobalRef(delegate);
        if (delegate)
            SystemActivityNotifications_delegate = delegate;
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_start
    (JNIEnv* env, jclass clazz, jlong ptr)
{
    (void) env;
    (void) clazz;
    (void) ptr;

    do
    {
        HANDLE handle = NULL;

        if (ERROR_IO_PENDING
                != NotifyAddrChange(
                        &handle,
                        &SystemActivityNotifications_overlapped))
            break; // Break in case of an error.

        DWORD numberOfBytesTransferred;

        if (!GetOverlappedResult(
                handle,
                &SystemActivityNotifications_overlapped,
                &numberOfBytesTransferred,
                /* bWait */ TRUE))
            break; // Break in case of an error.

        SystemActivityNotifications_notify(
                net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_NETWORK_CHANGE);
    }
    while (TRUE);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_stop
    (JNIEnv* env, jclass clazz, jlong ptr)
{
    (void) env;
    (void) clazz;
    (void) ptr;

    CancelIPChangeNotify(&SystemActivityNotifications_overlapped);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    (void) reserved;

    SystemActivityNotifications_vm = vm;
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    (void) vm;
    (void) reserved;

    SystemActivityNotifications_vm = NULL;
}

static void
SystemActivityNotifications_notify(jint type)
{
    jobject delegate = SystemActivityNotifications_delegate;

    if (delegate)
    {
        JavaVM *vm = SystemActivityNotifications_vm;

        if (vm)
        {
            JNIEnv *env;

            if (0 == vm->AttachCurrentThreadAsDaemon((void **) &env, NULL))
            {
                jclass clazz = env->GetObjectClass(delegate);

                if (clazz)
                {
                    jmethodID methodID
                        = env->GetMethodID(clazz,"notify", "(I)V");

                    if (methodID)
                        env->CallVoidMethod(delegate, methodID, type);
                }
                env->ExceptionClear();
            }
        }
    }
}

unsigned WINAPI
SystemActivityNotifications_runMessageLoop(LPVOID pv)
{
    MSG msg;
    HWND hWnd = (HWND) pv;

    while (GetMessageW(&msg, hWnd, 0, 0))
    {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    return msg.wParam;
}

LRESULT CALLBACK
SystemActivityNotifications_wndProc
    (HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
    switch (uMsg)
    {
    case WM_POWERBROADCAST:
        if (wParam == PBT_APMSUSPEND)
        {
            SystemActivityNotifications_notify(
                    net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SLEEP);
            return TRUE;
        }
        else if (wParam == PBT_APMRESUMESUSPEND)
        {
            SystemActivityNotifications_notify(
                    net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_WAKE);
            return TRUE;
        }
        break; // WM_POWERBROADCAST

    case WM_QUERYENDSESSION:
        SystemActivityNotifications_notify(
                net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_QUERY_ENDSESSION);
        return TRUE;

    case WM_ENDSESSION:
        // We fire the message only if we are really ending the session. If
        // wParam is FALSE, then someone has canceled the shutdown/logoff.
        if (wParam == TRUE)
        {
            SystemActivityNotifications_notify(
                    net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_ENDSESSION);
            return TRUE;
        }
        else
            break; // WM_ENDSESSION

    default:
        break;
    }

    return DefWindowProcW(hWnd, uMsg, wParam, lParam);
}
