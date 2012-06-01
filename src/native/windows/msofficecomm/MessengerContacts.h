/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_MESSENGERCONTACTS_H_
#define _JMSOFFICECOMM_MESSENGERCONTACTS_H_

#include "DispatchImpl.h"
#include <msgrua.h>

/**
 * Implements the <tt>IMessengerContacts</tt> interface.
 *
 * @author Lyubomir Marinov
 */
class MessengerContacts
    : public DispatchImpl<IMessengerContacts, IID_IMessengerContacts>
{
public:
    MessengerContacts(IMessenger *messenger);

    // IMessengerContacts
    STDMETHODIMP get_Count(LONG *pcContacts);
    STDMETHODIMP Item(LONG Index, IDispatch **ppMContact);
    STDMETHODIMP Remove(IDispatch *pMContact);
    STDMETHODIMP get__NewEnum(IUnknown **ppUnknown);

protected:
    virtual ~MessengerContacts();

private:
    STDMETHODIMP getSelf(IDispatch **ppMContact);

    IMessenger *_messenger;
    IWeakReference *_self;
};

#endif /* #ifndef _JMSOFFICECOMM_MESSENGERCONTACTS_H_ */
