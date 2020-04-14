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
