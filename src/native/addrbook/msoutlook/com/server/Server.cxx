/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "../../MAPIBitness.h"
#include "../../MAPISession.h"
#include "../../MsOutlookAddrBookContactSourceService.h"
#include "../../StringUtils.h"
#include "../MsOutlookAddrBookClient.h"
#include "../MsOutlookAddrBookServerClassFactory.h"
#include "../TypeLib.h"

#include <stdio.h>
#include <TlHelp32.h>

#define MAPI_NO_COINIT 8

void waitParentProcessStop();
static void Server_deleted(LPSTR id);
static void Server_inserted(LPSTR id);
static void Server_updated(LPSTR id);

/**
 * Starts the COM server.
 */
int main(int argc, char** argv)
{
    HRESULT hr = E_FAIL;

    if((hr = ::CoInitializeEx(NULL, COINIT_MULTITHREADED)) != S_OK
            && hr != S_FALSE)
    {
        return hr;
    }

    MAPISession_initLock();
    if(MsOutlookAddrBookContactSourceService_NativeMAPIInitialize(
                MAPI_INIT_VERSION,
                MAPI_MULTITHREAD_NOTIFICATIONS | MAPI_NO_COINIT,
                (void*) Server_deleted,
                (void*) Server_inserted,
                (void*) Server_updated)
            != S_OK)
    {
        CoUninitialize();
        return hr;
    }

    WCHAR * path = (WCHAR*) L"IMsOutlookAddrBookServer.tlb"; 
    LPTYPELIB typeLib = TypeLib_loadRegTypeLib(path);
    if(typeLib != NULL)
    {
        ClassFactory *classObject = new MsOutlookAddrBookServerClassFactory();
        if(classObject != NULL)
        {
            hr = classObject->registerClassObject();
            hr = ::CoResumeClassObjects();

            waitParentProcessStop();

            hr = ::CoSuspendClassObjects();
            hr = classObject->revokeClassObject();

            classObject->Release();
        }
        TypeLib_releaseTypeLib(typeLib);
    }
    MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize();
    MAPISession_freeLock();

    CoUninitialize();

    return hr;
}

/**
 * Wait that the parent process stops.
 */
void waitParentProcessStop()
{
    HANDLE handle = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if(handle != INVALID_HANDLE_VALUE)
    {
        PROCESSENTRY32 processEntry;
        memset(&processEntry, 0, sizeof(processEntry));
        processEntry.dwSize = sizeof(PROCESSENTRY32);
        DWORD id = GetCurrentProcessId();
        if(Process32First(handle, &processEntry))
        {
            do
            {
                // We have found this process
                if(processEntry.th32ProcessID == id)
                {
                    // Get the parent process handle.
                    HANDLE parentHandle
                        = OpenProcess(
                                SYNCHRONIZE
                                | PROCESS_QUERY_INFORMATION
                                | PROCESS_VM_READ,
                                FALSE,
                                processEntry.th32ParentProcessID);

                    // Wait for our parent to stop.
                    DWORD exitCode;
                    GetExitCodeProcess(parentHandle, &exitCode);
                    while(exitCode == STILL_ACTIVE)
                    {
                        WaitForSingleObject(parentHandle, INFINITE);
                        GetExitCodeProcess(parentHandle, &exitCode);
                    }
                    CloseHandle(parentHandle);
                    return;
                }
            }
            while(Process32Next(handle, &processEntry));
        }
        CloseHandle(handle);
    }
}

/**
 * Invoke the callback function of the COM client when a contact has been
 * deleted from MAPI.
 *
 * @param id The contact identifer.
 */
static void Server_deleted(LPSTR id)
{
    HRESULT hr =  E_FAIL;

    IMsOutlookAddrBookClient * msOutlookAddrBookClient = NULL;
    if((hr = CoCreateInstance(
            CLSID_MsOutlookAddrBookClient,
            NULL,
            CLSCTX_LOCAL_SERVER,
            IID_IMsOutlookAddrBookClient,
            (void**) &msOutlookAddrBookClient)) == S_OK)
    {
        LPWSTR idW = StringUtils::MultiByteToWideChar(id);
        BSTR res = SysAllocString(idW);
        msOutlookAddrBookClient->deleted(res);
        SysFreeString(res);
        free(idW);
        msOutlookAddrBookClient->Release();
    }
}

/**
 * Invoke the callback function of the COM client when a contact has been
 * created from MAPI.
 *
 * @param id The contact identifer.
 */
static void Server_inserted(LPSTR id)
{
    HRESULT hr =  E_FAIL;

    IMsOutlookAddrBookClient * msOutlookAddrBookClient = NULL;
    if((hr = CoCreateInstance(
            CLSID_MsOutlookAddrBookClient,
            NULL,
            CLSCTX_LOCAL_SERVER,
            IID_IMsOutlookAddrBookClient,
            (void**) &msOutlookAddrBookClient)) == S_OK)
    {
        LPWSTR idW = StringUtils::MultiByteToWideChar(id);
        BSTR res = SysAllocString(idW);
        msOutlookAddrBookClient->inserted(res);
        SysFreeString(res);
        free(idW);
        msOutlookAddrBookClient->Release();
    }
}

/**
 * Invoke the callback function of the COM client when a contact has been
 * modified from MAPI.
 *
 * @param id The contact identifer.
 */
static void Server_updated(LPSTR id)
{
    HRESULT hr =  E_FAIL;

    IMsOutlookAddrBookClient * msOutlookAddrBookClient = NULL;
    if((hr = CoCreateInstance(
            CLSID_MsOutlookAddrBookClient,
            NULL,
            CLSCTX_LOCAL_SERVER,
            IID_IMsOutlookAddrBookClient,
            (void**) &msOutlookAddrBookClient)) == S_OK)
    {
        LPWSTR idW = StringUtils::MultiByteToWideChar(id);
        BSTR res = SysAllocString(idW);
        msOutlookAddrBookClient->updated(res);
        SysFreeString(res);
        free(idW);
        msOutlookAddrBookClient->Release();
    }
}
