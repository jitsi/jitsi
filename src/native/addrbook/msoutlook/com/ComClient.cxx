/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "ComClient.h"

#include "../MAPIBitness.h"
#include "MsOutlookAddrBookServerClassFactory.h"
#include "MsOutlookAddrBookClientClassFactory.h"
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

    if((hr = CoInitializeEx(NULL, COINIT_MULTITHREADED)) == S_OK
            || hr == S_FALSE)
    {
        // The server may be long to start, then retry 10 times with 1s pause
        // between each try.
        int retry = 10;
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
                    ComClient_classFactory->Release();
                    ComClient_classFactory = NULL;
                }
                ::CoResumeClassObjects();

                retry = 0;
            }
            Sleep(1000);
            --retry;
        }
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
        ComClient_classFactory->Release();
        ComClient_classFactory = NULL;
    }
    if(ComClient_typeLib)
    {
        TypeLib_releaseTypeLib(ComClient_typeLib);
        ComClient_typeLib = NULL;
    }
}

/**
 * A pointer to the COM server interface to make remote MAPI requests.
 */
IMsOutlookAddrBookServer * ComClient_getIServer()
{
    return ComClient_iServer;
}
