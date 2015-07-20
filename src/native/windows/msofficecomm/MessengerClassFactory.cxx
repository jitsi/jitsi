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
