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
#include "ComClient.h"

#include "../MAPIBitness.h"
#include "MsOutlookAddrBookServerClassFactory.h"
#include "MsOutlookAddrBookClientClassFactory.h"
#include "../MsOutlookUtils.h"
#include "TypeLib.h"


#include <process.h>
#include <stdio.h>

/**
 * Starts and stops registration for the COM client.
 *
 * @author Vincent Lucas
 */

/**
 * A pointer to the COM server interface to make remote MAPI requests.
 */
static IMsOutlookAddrBookServer * ComClient_iServer = NULL;

/**
 * The class factory of the COM client activated when the client type library is
 * registered.
 */
static ClassFactory * ComClient_classFactory = NULL;

/**
 * The client type library.
 */
static LPTYPELIB ComClient_typeLib = NULL;

/**
 * Initializes the COM client.
 */
void ComClient_start(void)
{
    HRESULT hr = E_FAIL;

    MsOutlookUtils_log("Starting COM client.");
    if((hr = CoInitializeEx(NULL, COINIT_MULTITHREADED)) == S_OK
            || hr == S_FALSE)
    {
        // The server may be long to start, then retry 20 times with 1s pause
        // between each try.
        int retry = 20;
        while(retry > 0)
        {
            if((hr = CoCreateInstance(
                    CLSID_MsOutlookAddrBookServer,
                    NULL,
                    CLSCTX_LOCAL_SERVER,
                    IID_IMsOutlookAddrBookServer,
                    (void**) &ComClient_iServer)) == S_OK)
            {
                WCHAR * path = (WCHAR*) L"IMsOutlookAddrBookClient.tlb"; 
                ComClient_typeLib = TypeLib_loadRegTypeLib(path);

                ClassFactory *ComClient_classFactory
                    = new MsOutlookAddrBookClientClassFactory();
                if(ComClient_classFactory->registerClassObject() != S_OK)
                {
                	MsOutlookUtils_log("Failed to start COM client.[1]");
                    ComClient_classFactory->Release();
                    ComClient_classFactory = NULL;
                }
                ::CoResumeClassObjects();
                MsOutlookUtils_log("COM Client is started.");
                retry = 0;
            }
            Sleep(1000);
            --retry;
        }
    }
    else
    {
    	MsOutlookUtils_log("Failed to start COM client.");
    }
}


/**
 * Uninitializes the COM client.
 */
void ComClient_stop(void)
{
    if(ComClient_iServer)
    {
        ComClient_iServer->Release();
        ComClient_iServer = NULL;
    }

    ::CoSuspendClassObjects();

    if(ComClient_classFactory)
    {
        ComClient_classFactory->revokeClassObject();
        ComClient_classFactory->Release();
        ComClient_classFactory = NULL;
    }
    if(ComClient_typeLib)
    {
        TypeLib_releaseTypeLib(ComClient_typeLib);
        ComClient_typeLib = NULL;
    }

    CoUninitialize();
}

/**
 * A pointer to the COM server interface to make remote MAPI requests.
 */
IMsOutlookAddrBookServer * ComClient_getIServer()
{
    return ComClient_iServer;
}
