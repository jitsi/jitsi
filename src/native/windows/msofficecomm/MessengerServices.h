/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_MESSENGERSERVICES_H_
#define _JMSOFFICECOMM_MESSENGERSERVICES_H_

#include "DispatchImpl.h"
#include <msgrua.h>

class MessengerServices
    : public DispatchImpl<IMessengerServices, IID_IMessengerServices>
{
public:
    MessengerServices(IMessenger *messenger);

    // IMessengerServices
    STDMETHODIMP get_PrimaryService(IDispatch **ppService);
    STDMETHODIMP get_Count(long *pcServices);
    STDMETHODIMP Item(long Index, IDispatch **ppService);
    STDMETHODIMP get__NewEnum(IUnknown **ppUnknown);

protected:
    virtual ~MessengerServices();

private:
    IMessenger *_messenger;
    IWeakReference *_primaryService;
};

#endif /* _JMSOFFICECOMM_MESSENGERSERVICES_H_ */ 
