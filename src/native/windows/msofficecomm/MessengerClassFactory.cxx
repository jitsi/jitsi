/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MessengerClassFactory.h"

#include "Messenger.h"

EXTERN_C const GUID DECLSPEC_SELECTANY CLSID_Messenger
    = { 0x8885370D, 0xB33E, 0x44b7, { 0x87, 0x5D, 0x28, 0xE4, 0x03, 0xCF, 0x92, 0x70 } };

STDMETHODIMP
MessengerClassFactory::CreateInstance(LPUNKNOWN outer, REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (outer)
    {
        *obj = NULL;
        hr = CLASS_E_NOAGGREGATION;
    }
    else
    {
        IMessenger *messenger;

        if (_messenger)
        {
            hr = _messenger->Resolve(IID_IMessenger, (PVOID *) &messenger);
            if (FAILED(hr) && (E_NOINTERFACE != hr))
            {
                _messenger->Release();
                _messenger = NULL;
            }
        }
        else
            messenger = NULL;
        if (!messenger)
        {
            messenger = new Messenger();

            IWeakReferenceSource *weakReferenceSource;

            hr
                = messenger->QueryInterface(
                        IID_IWeakReferenceSource,
                        (PVOID *) &weakReferenceSource);
            if (SUCCEEDED(hr))
            {
                IWeakReference *weakReference;

                hr = weakReferenceSource->GetWeakReference(&weakReference);
                if (SUCCEEDED(hr))
                {
                    if (_messenger)
                        _messenger->Release();
                    _messenger = weakReference;
                }
            }
        }
        hr = messenger->QueryInterface(iid, obj);
        messenger->Release();
    }
    return hr;
}
