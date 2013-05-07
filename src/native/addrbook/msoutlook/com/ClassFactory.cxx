/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "ClassFactory.h"

#include <stdio.h>

/**
 * Represents a base implementation of the <tt>IClassFactory</tt> interface.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */

/**
 * Returns an instance of the interface requested, if available.
 *
 * @param iid The identifier of the interface requested.
 * @param obj A pointer use to return an object instance implementing the
 * interface requested.
 *
 * @return S_OK if the corresponding interface has been found. E_POINTER, or
 * E_NOINTERFACE otherwise.
 */
STDMETHODIMP ClassFactory::QueryInterface(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if(!obj)
    {
        hr = E_POINTER;
    }
    else if(IID_IUnknown == iid)
    {
        AddRef();
        *obj = static_cast<LPUNKNOWN>(this);
        hr = S_OK;
    }
    else if(IID_IClassFactory == iid)
    {
        AddRef();
        *obj = static_cast<IClassFactory *>(this);
        hr = S_OK;
    }
    else
    {
        *obj = NULL;
        hr = E_NOINTERFACE;
    }

    return hr;
}

/**
 * Increment the number of reference.
 *
 * @return The number of reference.
 */
STDMETHODIMP_(ULONG) ClassFactory::AddRef()
{
    return ++_refCount;
}

/**
 * Decrement the number of reference.
 *
 * @return The number of reference.
 */
STDMETHODIMP_(ULONG) ClassFactory::Release()
{
    ULONG refCount = --_refCount;

    if(!refCount)
    {
        delete this;
    }

    return refCount;
}

/**
 * Unused.
 */
HRESULT ClassFactory::LockServer(BOOL lock)
{
    return S_OK;
};

/**
 * Register the CLISD of the implementer of this ClassFactory.
 *
 * @return S_OK if the class object was registered successfully. Any other value
 * if fail.
 */
HRESULT ClassFactory::registerClassObject()
{
    return
        ::CoRegisterClassObject(
                _clsid,
                this,
                CLSCTX_LOCAL_SERVER,
                REGCLS_MULTIPLEUSE | REGCLS_SUSPENDED,
                &_registration);
};

/**
 * Unregister the CLISD of the implementer of this ClassFactory.
 *
 * @return S_OK the class object was revoked successfully. Any other value if
 * fail.
 */
HRESULT ClassFactory::revokeClassObject()
{
    return ::CoRevokeClassObject(_registration);
};

/**
 * Instanciates this class factory for a given CLSID.
 *
 * @param clsid The CLSID to manage.
 */
ClassFactory::ClassFactory(REFCLSID clsid):
    _clsid(clsid),
    _registration(0),
    _refCount(1)
{
};

/**
 * Deletes this class factory.
 */
ClassFactory::~ClassFactory()
{
    Release();
};
