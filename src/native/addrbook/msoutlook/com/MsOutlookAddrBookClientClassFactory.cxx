/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
