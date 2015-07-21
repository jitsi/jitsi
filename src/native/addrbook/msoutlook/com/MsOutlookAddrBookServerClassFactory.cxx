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
#include "MsOutlookAddrBookServerClassFactory.h"

#include "MsOutlookAddrBookServer.h"

/**
 * Instanciates a new MsOutlookAddrBookServerClassFactory.
 */
MsOutlookAddrBookServerClassFactory::MsOutlookAddrBookServerClassFactory():
    ClassFactory(CLSID_MsOutlookAddrBookServer),
    _msOutlookAddrBookServer(NULL)
{
}

/**
 * Deletest this instance of MsOutlookAddrBookServerClassFactory.
 */
MsOutlookAddrBookServerClassFactory::~MsOutlookAddrBookServerClassFactory()
{
    if (_msOutlookAddrBookServer)
        _msOutlookAddrBookServer->Release();
}


/**
 * Creates a new instance of a MsOutlookAddrBookServer.
 *
 * @param outer A pointer used if the object is being created as part of an
 * aggregate,
 * @param iid The identifier of the interface requested.
 * @param obj A pointer use to return an object implemented the interface
 * requested.
 *
 * @return S_OK if the corresponding interface has been found. Any other error
 * otherwise.
 */
STDMETHODIMP
MsOutlookAddrBookServerClassFactory::CreateInstance
    (LPUNKNOWN outer, REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if(outer)
    {
        *obj = NULL;
        hr = CLASS_E_NOAGGREGATION;
    }
    else
    {
        if(_msOutlookAddrBookServer)
        {
            _msOutlookAddrBookServer->Release();
        }
        _msOutlookAddrBookServer = NULL;
        _msOutlookAddrBookServer = new MsOutlookAddrBookServer();
        hr = _msOutlookAddrBookServer->QueryInterface(iid, obj);
    }
    return hr;
}
