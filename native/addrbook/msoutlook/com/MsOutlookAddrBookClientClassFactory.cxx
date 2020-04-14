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
#include "MsOutlookAddrBookClientClassFactory.h"

#include "MsOutlookAddrBookClient.h"

/**
 * Instanciates a new MsOutlookAddrBookClientClassFactory.
 */
MsOutlookAddrBookClientClassFactory::MsOutlookAddrBookClientClassFactory():
    ClassFactory(CLSID_MsOutlookAddrBookClient),
    _msOutlookAddrBookClient(NULL)
{
}

/**
 * Deletest this instance of MsOutlookAddrBookClientClassFactory.
 */
MsOutlookAddrBookClientClassFactory::~MsOutlookAddrBookClientClassFactory()
{
    if (_msOutlookAddrBookClient)
        _msOutlookAddrBookClient->Release();
}

/**
 * Creates a new instance of a MsOutlookAddrBookClient.
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
MsOutlookAddrBookClientClassFactory::CreateInstance
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
        if(_msOutlookAddrBookClient)
        {
            _msOutlookAddrBookClient->Release();
        }
        _msOutlookAddrBookClient = NULL;
        _msOutlookAddrBookClient = new MsOutlookAddrBookClient();
        hr = _msOutlookAddrBookClient->QueryInterface(iid, obj);
    }
    return hr;
}
