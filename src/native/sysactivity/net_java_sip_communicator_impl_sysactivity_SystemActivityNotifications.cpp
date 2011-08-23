/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.h"

/**
 * \def WIN32_LEAN_AND_MEAN
 * \brief Exclude not commonly used headers from win32 API.
 *
 * It excludes some unused stuff from windows headers and
 * by the way code compiles faster.
 */
#define WIN32_LEAN_AND_MEAN

#include <winsock2.h>
#include <ws2def.h>
#include <ws2ipdef.h>
#include <windows.h>
#include <iphlpapi.h>
#include <process.h>
#include <tchar.h>

// cache for the JavaVM* pointer
JavaVM *jvm;

// cache for the a generic pointer
jobject delegateObject;

// thread id
UINT uThreadId;

// cache for the instance handle
HINSTANCE hInstance;

void RegisterWindowClassW();
LRESULT CALLBACK WndProcW(HWND, UINT, WPARAM, LPARAM);
HRESULT callback(UINT Msg, WPARAM wParam, LPARAM lParam);

void notify(int notificationType);
void notifyNetwork(int family,
                    long luidIndex,
                    char* name,
                    long type,
                    bool connected);

typedef void (WINAPI *NIpIfaceChange)(ADDRESS_FAMILY,
                                    PIPINTERFACE_CHANGE_CALLBACK,
                                    PVOID,
                                    BOOLEAN,
                                    HANDLE);
typedef NETIO_STATUS (*FnConvertInterfaceLuidToNameA)(
    const NET_LUID *,
    PSTR,
    SIZE_T);
typedef NETIO_STATUS (*FnGetIpInterfaceEntry)(
    PMIB_IPINTERFACE_ROW);

OVERLAPPED overlap;

/*static HHOOK hhookSysMsg;

static LRESULT CALLBACK msghook(int nCode, WPARAM wParam, LPARAM lParam)
{
    if(nCode < 0)
    {
        CallNextHookEx(hhookSysMsg, nCode, wParam, lParam);
        return 0;
    }

    LPMSG msg = (LPMSG)lParam;

    if(msg->message == WM_SYSCOMMAND)
    {
        if(wParam == SC_SCREENSAVE)
        {
            notify(net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SCREENSAVER_START);
        }
        else if(wParam == SC_MONITORPOWER)
        {
            if(lParam == -1)
            {
                notify(net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_DISPLAY_WAKE);
            }
            else if(lParam == 2)
            {
                notify(net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_DISPLAY_SLEEP);
            }
        }
    }

    return CallNextHookEx(hhookSysMsg, nCode, wParam, lParam);
}
*/

unsigned WINAPI CreateWndThreadW(LPVOID pThreadParam)
{
    hInstance = GetModuleHandle (NULL);
    RegisterWindowClassW();

    /*
    hhookSysMsg = SetWindowsHookEx(
                    WH_MSGFILTER,
                    (HOOKPROC)msghook,
                    hInstance,
                    GetCurrentThreadId());
    if(hhookSysMsg == NULL)
    {
        fprintf(stderr, "Failed to create hoook %i\n", GetLastError() );
        fflush(stderr);
    }
    */

    HWND hWnd = CreateWindowW( L"Jitsi Window Hook", NULL, WS_OVERLAPPEDWINDOW,
                    CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
                    NULL, NULL, hInstance, NULL);
    if( hWnd == NULL)
    {
        fprintf(stderr, "Failed to create window\n" );
        fflush(stderr);

        return( 0 );

    }else
    {
        MSG Msg;

        while(GetMessageW(&Msg, hWnd, 0, 0)) {

            TranslateMessage(&Msg);

            DispatchMessageW(&Msg);
        }

        return Msg.wParam;
    }
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    setDelegate
 * Signature: (Lnet/java/sip/communicator/impl/sysactivity/SystemActivityNotifications/NotificationsDelegate;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_setDelegate
  (JNIEnv* jniEnv, jclass clazz, jlong ptr, jobject m_delegate)
{
    if (delegateObject)
    {
        if (!jniEnv)
            jvm->AttachCurrentThread((void **)&jniEnv, NULL);
        jniEnv->DeleteGlobalRef(delegateObject);
        delegateObject = NULL;
        jvm = NULL;
    }

    if (m_delegate)
    {
        m_delegate = jniEnv->NewGlobalRef(m_delegate);
        if (m_delegate)
        {
            jniEnv->GetJavaVM(&(jvm));
            delegateObject = m_delegate;
        }
    }
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    allocAndInit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_allocAndInit
  (JNIEnv* jniEnv, jclass clazz)
{
    HANDLE hThread;

    hThread = (HANDLE)_beginthreadex(NULL, 0, &CreateWndThreadW, NULL, 0, &uThreadId);

    if(!hThread)
    {
        //throwException( env, "_beginthreadex", "initialisation failed" );
    }

    return (jlong)hThread;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_release
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
     if (!jniEnv)
        jvm->AttachCurrentThread((void **)&jniEnv, NULL);
    jniEnv->DeleteGlobalRef(delegateObject);
    delegateObject = NULL;
    jvm = NULL;
}

static void InterfaceChangeCallback(
    PVOID CallerContext,
    PMIB_IPINTERFACE_ROW  Row,
    MIB_NOTIFICATION_TYPE NotificationType)
{
    if(Row)
    {
        FnGetIpInterfaceEntry getIpIfaceEntry;
        getIpIfaceEntry = (FnGetIpInterfaceEntry)GetProcAddress(
                        GetModuleHandle(TEXT("Iphlpapi.dll")),
                        "GetIpInterfaceEntry");

        if(getIpIfaceEntry && getIpIfaceEntry(Row) == NO_ERROR)
        {
            FnConvertInterfaceLuidToNameA convertName;
            convertName = (FnConvertInterfaceLuidToNameA)GetProcAddress(
                            GetModuleHandle(TEXT("Iphlpapi.dll")),
                            "ConvertInterfaceLuidToNameA");

            char interfaceName[MAX_PATH];
            if (convertName &&
                convertName(&(Row->InterfaceLuid),
                            interfaceName,
                            sizeof(interfaceName))
                    == NO_ERROR)
            {
                //fprintf( stderr, "Interface LUID Name : %s\n", interfaceName);
            }

            notifyNetwork(
                Row->Family,
                Row->InterfaceLuid.Info.NetLuidIndex,
                interfaceName,
                Row->InterfaceLuid.Info.IfType,
                Row->Connected);
        }
    }
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    getLastInput
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_getLastInput
  (JNIEnv* jniEnv, jclass clazz)
{
    DWORD result = 0;

    LASTINPUTINFO lii;
    memset(&lii, 0, sizeof(lii));
    lii.cbSize = sizeof(LASTINPUTINFO);
    if (GetLastInputInfo(&lii))
    {
        return GetTickCount() - lii.dwTime;
    }

    return -1;
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    start
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_start
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    OSVERSIONINFOEX osVersionInfoEx;
    memset( &osVersionInfoEx, 0, sizeof(OSVERSIONINFOEX) );
    osVersionInfoEx.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);
    GetVersionEx((OSVERSIONINFO*) &osVersionInfoEx );

    if( osVersionInfoEx.dwMajorVersion == 5)
    {
        // XP
        while(true)
        {
            HANDLE hand = NULL;
            DWORD ret, bytes;

            hand = NULL;
            ZeroMemory(&overlap, sizeof(overlap));
            overlap.hEvent = CreateEvent(NULL, FALSE, FALSE, NULL);
            ret = NotifyAddrChange(&hand, &overlap);

            if(ret != ERROR_IO_PENDING )
            {
                //fprintf(stderr, "NotifyAddrChange returned %d,
                //    expected ERROR_IO_PENDING \n", ret);fflush(stderr);

                // break in case of error.
                break;
            }

            BOOL success = GetOverlappedResult(hand, &overlap, &bytes, TRUE);

            if(!success)
                break;

            notify(net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_NETWORK_CHANGE);
        }
    }
    else if( osVersionInfoEx.dwMajorVersion > 5)
    {
        // Vista, 7, ....
        NIpIfaceChange nIpIfaceChange;
        nIpIfaceChange = (NIpIfaceChange)GetProcAddress(
            GetModuleHandle(TEXT("Iphlpapi.dll")),
            "NotifyIpInterfaceChange");

        if(nIpIfaceChange)
        {
            ADDRESS_FAMILY family = AF_UNSPEC;
            HANDLE hNotification;

            nIpIfaceChange(
                family,
                (PIPINTERFACE_CHANGE_CALLBACK)InterfaceChangeCallback,
                NULL,
                FALSE,
                &hNotification);
        }
    }
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_stop
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    CancelIPChangeNotify(&overlap);
}

void RegisterWindowClassW()
{
    WNDCLASSEXW wcex;

    wcex.cbSize = sizeof(WNDCLASSEXW);
    wcex.style          = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc    = WndProcW;
    wcex.cbClsExtra     = 0;
    wcex.cbWndExtra     = 0;
    wcex.hInstance      = hInstance;
    wcex.hIcon          = 0;
    wcex.hCursor        = 0;
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW + 1);
    wcex.lpszMenuName   = 0;
    wcex.lpszClassName  = L"Jitsi Window Hook";
    wcex.hIconSm        = 0;

    RegisterClassExW(&wcex);
}

LRESULT CALLBACK WndProcW(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)
{
    long res = callback( Msg, wParam, lParam );

    if ( res != -1 )
    {
        return( res );
    }

    return DefWindowProcW(hWnd, Msg, wParam, lParam);
}

HRESULT callback(UINT Msg, WPARAM wParam, LPARAM lParam)
{
    JNIEnv *env;

    if ( jvm->AttachCurrentThread((void **)&env, NULL ))
    {
        fprintf( stderr, "failed to attach current thread to JVM\n" );fflush(stderr);

        return( -1 );
    }

    jlong result = -1;

    if (Msg == WM_POWERBROADCAST)
    {
        if (wParam == PBT_APMSUSPEND)
        {
            notify(net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SLEEP);
            return TRUE;
        }
        else if (wParam == PBT_APMRESUMESUSPEND)
        {
            notify(net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_WAKE);
            return TRUE;
        }
    }

    jvm->DetachCurrentThread();

    return((HRESULT)result );
}

void notify(int notificationType)
{
    JNIEnv *jniEnv;
    jclass delegateClass = NULL;

    if (!delegateObject)
        return;

    if (0 != jvm->AttachCurrentThreadAsDaemon( (void **)&jniEnv, NULL))
        return;

    delegateClass = jniEnv->GetObjectClass(delegateObject);
    if(delegateClass)
    {
        jmethodID methodid = NULL;

        methodid = jniEnv->GetMethodID(delegateClass,"notify", "(I)V");
        if(methodid)
        {
            jniEnv->CallVoidMethod(delegateObject, methodid, notificationType);
        }
    }
    jniEnv->ExceptionClear();
}

void notifyNetwork(int family,
                    long luidIndex,
                    char* name,
                    long type,
                    bool connected)
{
    JNIEnv *jniEnv;
    jclass delegateClass = NULL;

    if (!delegateObject)
        return;

    if (0 != jvm->AttachCurrentThreadAsDaemon( (void **)&jniEnv, NULL))
        return;

    delegateClass = jniEnv->GetObjectClass(delegateObject);
    if(delegateClass)
    {
        jmethodID methodid = NULL;

        methodid = jniEnv->GetMethodID(delegateClass,
        "notifyNetworkChange",
        "(IJLjava/lang/String;JZ)V");

        if(methodid)
        {
            jniEnv->CallVoidMethod(delegateObject, methodid,
                family,
                luidIndex,
                name ? jniEnv->NewStringUTF(name) : NULL,
                type,
                connected);
        }
    }
    jniEnv->ExceptionClear();
}

