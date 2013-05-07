/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
