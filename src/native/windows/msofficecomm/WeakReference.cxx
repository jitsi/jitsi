/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "WeakReference.h"

STDMETHODIMP WeakReference::QueryInterface(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (!obj)
        hr = E_POINTER;
    else if (IID_IWeakReferenceSource == iid)
    {
        /*
         * While a weak reference to a weak reference should technically be
         * possible, such functionality does not seem necessary at the time of
         * this writing.
         */
        *obj = NULL;
        hr = E_NOINTERFACE;
    }
    else
        hr = UnknownImpl::QueryInterface(iid, obj);
    return hr;
}

STDMETHODIMP WeakReference::Resolve(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (obj)
    {
        if (_iUnknown)
            hr = _iUnknown->QueryInterface(iid, obj);
        else
        {
            *obj = NULL;
            hr = E_FAIL;
        }
    }
    else
        hr = E_POINTER;
    return hr;
}
