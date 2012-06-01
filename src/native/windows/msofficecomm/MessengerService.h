/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_MESSENGERSERVICE_H_
#define _JMSOFFICECOMM_MESSENGERSERVICE_H_

#include "DispatchImpl.h"
#include <msgrua.h>

class MessengerService
    : public DispatchImpl<IMessengerService, IID_IMessengerService>
{
public:
    MessengerService(IMessenger *messenger);

    // IMessengerService
    STDMETHODIMP get_ServiceName(BSTR *pbstrServiceName);
    STDMETHODIMP get_ServiceID(BSTR *pbstrID);
    STDMETHODIMP get_MyFriendlyName(BSTR *pbstrName);
    STDMETHODIMP get_MyStatus(MISTATUS *pmiStatus);
    STDMETHODIMP get_MySigninName(BSTR *pbstrName);
    STDMETHODIMP get_Property(MSERVICEPROPERTY ePropType, VARIANT *pvPropVal);
    STDMETHODIMP put_Property(MSERVICEPROPERTY ePropType, VARIANT vPropVal);

protected:
    virtual ~MessengerService();

private:
    IMessenger *_messenger;
};

#endif /* #ifndef _JMSOFFICECOMM_MESSENGERSERVICE_H_ */
