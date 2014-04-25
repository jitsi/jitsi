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
#include "../../MsOutlookUtils.h"

#include <stdio.h>
#include <string.h>
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


    if(argc > 1)
    {
    	char* path = argv[1];
    	*(path + strlen(path) - 1) = '\\';
    	MsOutlookUtils_createLogger("msoutlookaddrbook_server.log", path);
    }

    MsOutlookUtils_log("Starting the Outlook Server.");
    if((hr = ::CoInitializeEx(NULL, COINIT_MULTITHREADED)) != S_OK
            && hr != S_FALSE)
    {
    	MsOutlookUtils_log("Error in initialization of the Outlook Server.[1]");
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
    	MsOutlookUtils_log("Error in native MAPI initialization of the Outlook Server.[2]");
        CoUninitialize();
        return hr;
    }

    WCHAR * path = (WCHAR*) L"IMsOutlookAddrBookServer.tlb"; 
    LPTYPELIB typeLib = TypeLib_loadRegTypeLib(path);
    if(typeLib != NULL)
    {

    	MsOutlookUtils_log("TLB initialized.");
        ClassFactory *classObject = new MsOutlookAddrBookServerClassFactory();
        if(classObject != NULL)
        {
        	MsOutlookUtils_log("Server object created.");
            hr = classObject->registerClassObject();
            hr = ::CoResumeClassObjects();

			MsOutlookUtils_log("Server started.");
            waitParentProcessStop();

            MsOutlookUtils_log("Stop waiting.[3]");
            hr = ::CoSuspendClassObjects();
            hr = classObject->revokeClassObject();

            classObject->Release();
        }
        else
        {
        	MsOutlookUtils_log("Error - server object can't be created.");
        }
        TypeLib_releaseTypeLib(typeLib);
    }
    else
    {
    	MsOutlookUtils_log("Error - TLB isn't initialized.");
    }
    MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize();
    MsOutlookUtils_deleteLogger();
    MAPISession_freeLock();

    CoUninitialize();

    return hr;
}

/**
 * Wait that the parent process stops.
 */
void waitParentProcessStop()
{
	MsOutlookUtils_log("Waits parent process to stop.");
    HANDLE handle = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if(handle != INVALID_HANDLE_VALUE)
    {
    	MsOutlookUtils_log("Valid handle is found.");
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
                    MsOutlookUtils_log("Stop waiting.[1]");
                    CloseHandle(parentHandle);
                    return;
                }
            }
            while(Process32Next(handle, &processEntry));
        }
        CloseHandle(handle);
    }
    else
    {
    	MsOutlookUtils_log("Error - not valid handle found.");
    }
    MsOutlookUtils_log("Stop waiting.[2]");
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
