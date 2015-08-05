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
#include "MessengerServices.h"

#include "MessengerService.h"

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMessengerServices
    = { 0x2E50547B, 0xA8AA, 0x4f60, { 0xB5, 0x7E, 0x1F, 0x41, 0x47, 0x11, 0x00, 0x7B } };

MessengerServices::MessengerServices(IMessenger *messenger)
    : _messenger(messenger),
      _primaryService(NULL)
{
    _messenger->AddRef();
}

MessengerServices::~MessengerServices()
{
    _messenger->Release();
    if (_primaryService)
        _primaryService->Release();
}

STDMETHODIMP MessengerServices::get__NewEnum(IUnknown **ppUnknown)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerServices::get_Count(long *pcServices)
{
    HRESULT hr;

    if (pcServices)
    {
        *pcServices = 1;
        hr = S_OK;
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP MessengerServices::get_PrimaryService(IDispatch **ppService)
    STDMETHODIMP_RESOLVE_WEAKREFERENCE_OR_NEW(ppService,_primaryService,MessengerService,_messenger)

STDMETHODIMP MessengerServices::Item(long Index, IDispatch **ppService)
{
    HRESULT hr;

    if (ppService)
    {
        if (0 > Index)
        {
            *ppService = NULL;
            hr = E_INVALIDARG;
        }
        else if (0 == Index)
            hr = get_PrimaryService(ppService);
        else
        {
            *ppService = NULL;
            hr = E_FAIL;
        }
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}
