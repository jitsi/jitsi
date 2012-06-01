/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "WeakReferenceSource.h"

#include "WeakReference.h"

WeakReferenceSource::~WeakReferenceSource()
{
    if (_weakReference)
    {
        _weakReference->invalidate();
        _weakReference->Release();
        _weakReference = NULL;
    }
}

STDMETHODIMP
WeakReferenceSource::GetWeakReference(IWeakReference **weakReference)
{
    HRESULT hr;

    if (weakReference)
    {
        if (!_weakReference)
            _weakReference = new WeakReference(_iUnknown);
        _weakReference->AddRef();
        *weakReference = _weakReference;
        hr = S_OK;
    }
    else
        hr = E_POINTER;
    return hr;
}
