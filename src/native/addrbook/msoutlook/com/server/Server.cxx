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
#include "../../MAPIBitness.h"
#include "../../MAPISession.h"
#include "../../MsOutlookAddrBookContactSourceService.h"
#include "../../StringUtils.h"
#include "../MsOutlookAddrBookClient.h"
#include "../MsOutlookAddrBookServerClassFactory.h"
#include "../TypeLib.h"
#include "../../MsOutlookUtils.h"
#include "../../MAPINotification.h"

#include <stdio.h>
#include <string.h>
#include <TlHelp32.h>
#include <stdlib.h>

#define MAPI_NO_COINIT 8

void waitParentProcessStop();
static void Server_contactDeleted(LPSTR id);
static void Server_contactInserted(LPSTR id);
static void Server_contactUpdated(LPSTR id);
static void Server_calendarDeleted(LPSTR id);
static void Server_calendarInserted(LPSTR id);
static void Server_calendarUpdated(LPSTR id);
static void Server_deleted(LPSTR id, ULONG type);
static void Server_inserted(LPSTR id, ULONG type);
static void Server_updated(LPSTR id, ULONG type);

/**
 * Starts the COM server.
 */
int main(int argc, char** argv)
{
    HRESULT hr = E_FAIL;


    if(argc > 2)
    {
    	char* path = argv[1];
    	int loggerLevel = 0;
		char* loggerLevelString = argv[2];
		loggerLevel = atoi(loggerLevelString);
    	MsOutlookUtils_createLogger("msoutlookaddrbook_server.log", path,
    			loggerLevel);

    }

    MsOutlookUtils_logInfo(argv[1]);
    MsOutlookUtils_logInfo(argv[2]);
    MsOutlookUtils_logInfo("Starting the Outlook Server.");
    if((hr = ::CoInitializeEx(NULL, COINIT_MULTITHREADED)) != S_OK
            && hr != S_FALSE)
    {
    	MsOutlookUtils_logInfo("Error in initialization of the Outlook Server.[1]");
        return hr;
    }
    MAPISession_initLock();


    WCHAR * path = (WCHAR*) L"IMsOutlookAddrBookServer.tlb"; 
    LPTYPELIB typeLib = TypeLib_loadRegTypeLib(path);
    if(typeLib != NULL)
    {

    	MsOutlookUtils_logInfo("TLB initialized.");
        ClassFactory *classObject = new MsOutlookAddrBookServerClassFactory();
        if(classObject != NULL)
        {
        	MsOutlookUtils_logInfo("Server object created.");
            hr = classObject->registerClassObject();
            hr = ::CoResumeClassObjects();

            if(MsOutlookAddrBookContactSourceService_NativeMAPIInitialize(
                            MAPI_INIT_VERSION,
                            MAPI_MULTITHREAD_NOTIFICATIONS | MAPI_NO_COINIT,
                            (void*) Server_contactDeleted,
                            (void*) Server_contactInserted,
                            (void*) Server_contactUpdated)
                        != S_OK)
			{
				MsOutlookUtils_logInfo("Error in native MAPI initialization of the Outlook Server.[2]");
				CoUninitialize();
			}
            else
            {
				MAPINotification_registerCalendarNativeNotificationsDelegate(
						(void*) Server_calendarDeleted,
						(void*) Server_calendarInserted,
						(void*) Server_calendarUpdated);

				MsOutlookUtils_logInfo("Server started.");
				waitParentProcessStop();
            }

            MsOutlookUtils_logInfo("Stop waiting.[3]");
            hr = ::CoSuspendClassObjects();
            hr = classObject->revokeClassObject();

            classObject->Release();
        }
        else
        {
        	MsOutlookUtils_logInfo("Error - server object can't be created.");
        }
        TypeLib_releaseTypeLib(typeLib);
    }
    else
    {
    	MsOutlookUtils_logInfo("Error - TLB isn't initialized.");
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
static void Server_deleted(LPSTR id, ULONG type)
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
        msOutlookAddrBookClient->deleted(res, type);
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
static void Server_inserted(LPSTR id, ULONG type)
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
        msOutlookAddrBookClient->inserted(res, type);
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
static void Server_updated(LPSTR id, ULONG type)
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
        msOutlookAddrBookClient->updated(res, type);
        SysFreeString(res);
        free(idW);
        msOutlookAddrBookClient->Release();
    }
}

static void Server_contactDeleted(LPSTR id)
{
	Server_deleted(id, CONTACTS_FOLDER_TYPE);
}

static void Server_contactInserted(LPSTR id)
{
	Server_inserted(id, CONTACTS_FOLDER_TYPE);
}

static void Server_contactUpdated(LPSTR id)
{
	Server_updated(id, CONTACTS_FOLDER_TYPE);
}

static void Server_calendarDeleted(LPSTR id)
{
	Server_deleted(id, CALENDAR_FOLDER_TYPE);
}

static void Server_calendarInserted(LPSTR id)
{
	Server_inserted(id, CALENDAR_FOLDER_TYPE);
}

static void Server_calendarUpdated(LPSTR id)
{
	Server_updated(id, CALENDAR_FOLDER_TYPE);
}
