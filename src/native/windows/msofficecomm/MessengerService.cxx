/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
