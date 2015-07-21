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
#include "MessengerService.h"

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMessengerService
    = { 0x2E50547C, 0xA8AA, 0x4f60, { 0xB5, 0x7E, 0x1F, 0x41, 0x47, 0x11, 0x00, 0x7B } };

MessengerService::MessengerService(IMessenger *messenger)
    : _messenger(messenger)
{
    _messenger->AddRef();
}

MessengerService::~MessengerService()
{
    _messenger->Release();
}

STDMETHODIMP MessengerService::get_MyFriendlyName(BSTR *pbstrName)
{
    return _messenger->get_MyFriendlyName(pbstrName);
}

STDMETHODIMP MessengerService::get_MySigninName(BSTR *pbstrName)
{
    return _messenger->get_MySigninName(pbstrName);
}

STDMETHODIMP MessengerService::get_MyStatus(MISTATUS *pmiStatus)
{
    return _messenger->get_MyStatus(pmiStatus);
}

STDMETHODIMP MessengerService::get_Property(MSERVICEPROPERTY ePropType, VARIANT *pvPropVal)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerService::get_ServiceID(BSTR *pbstrID)
{
    return _messenger->get_MyServiceId(pbstrID);
}

STDMETHODIMP MessengerService::get_ServiceName(BSTR *pbstrServiceName)
{
    return _messenger->get_MyServiceName(pbstrServiceName);
}

STDMETHODIMP MessengerService::put_Property(MSERVICEPROPERTY ePropType, VARIANT vPropVal)
    STDMETHODIMP_E_NOTIMPL_STUB
