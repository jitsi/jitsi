/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_OUTOFPROCESSSERVER_H_
#define _JMSOFFICECOMM_OUTOFPROCESSSERVER_H_

#include <objbase.h>
#include <jni.h>
#include <windows.h>

class ClassFactory;

/**
 * Implements the jmsoutlookcomm application which is an out-of-process local COM
 * server.
 *
 * @author Lyubomir Marinov
 */
class OutOfProcessServer
{
public:
    static ULONG addRef() { /* TODO Auto-generated method stub */ return 0; }
    static void enterCriticalSection()
        { ::EnterCriticalSection(&_criticalSection); }
    static LPSTR getClassName(LPCSTR className);
    static JavaVM *getJavaVM() { return _vm; }
    static HANDLE getThreadHandle() { return _threadHandle; }
    static DWORD getThreadId() { return _threadId; }
    static HRESULT getTypeInfo(UINT index, ITypeInfo **ppTInfo)
        { return _iTypeLib->GetTypeInfo(index, ppTInfo); }
    static UINT getTypeInfoCount() { return _iTypeLib->GetTypeInfoCount(); }
    static HRESULT getTypeInfoOfGuid(REFGUID guid, ITypeInfo **ppTInfo)
        { return _iTypeLib->GetTypeInfoOfGuid(guid, ppTInfo); }
    static jint JNI_OnLoad(JavaVM *vm);
    static void JNI_OnUnload(JavaVM *vm) { _vm = NULL; }
    static void leaveCriticalSection()
        { ::LeaveCriticalSection(&_criticalSection); }
    static ULONG release() { /* TODO Auto-generated method stub */ return 0; }
    static HRESULT start(JNIEnv *env, jclass clazz, LPCSTR packageName);
    static HRESULT stop(JNIEnv *env, jclass clazz);

private:
    static BOOL isMicrosoftOfficeOutlookCallIntegrationIMApplication();
    static HRESULT loadRegTypeLib();
    static DWORD regCreateKeyAndSetValue(LPCTSTR key, LPCTSTR valueName, DWORD data);
    static HRESULT registerClassObjects();
    static ULONG releaseTypeLib();
    static HRESULT revokeClassObjects();
    static unsigned __stdcall run(void *);
    static DWORD setIMProvidersCommunicatorUpAndRunning(DWORD dw);

    static CRITICAL_SECTION _criticalSection;
    static ITypeLib *       _iTypeLib;
    static ClassFactory *   _messengerClassFactory;
    static LPSTR            _packageName;
    static HANDLE           _threadHandle;
    static DWORD            _threadId;
    static JavaVM *         _vm;
};

#endif /* #ifndef _JMSOFFICECOMM_OUTOFPROCESSSERVER_H_ */
