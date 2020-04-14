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
#ifndef _JMSOFFICECOMM_MESSENGERCLASSFACTORY_H_
#define _JMSOFFICECOMM_MESSENGERCLASSFACTORY_H_

#include "ClassFactory.h"
#include <msgrua.h>

/**
 * Implements the <tt>IClassFactory</tt> interface for the <tt>IMessenger</tt>
 * interface implementation.
 *
 * @author Lyubomir Marinov
 */
class MessengerClassFactory
    : public ClassFactory
{
public:
    MessengerClassFactory()
        : ClassFactory(CLSID_Messenger), _messenger(NULL) {}

    STDMETHODIMP CreateInstance(LPUNKNOWN, REFIID, PVOID *);

protected:
    virtual ~MessengerClassFactory()
        {
            if (_messenger)
                _messenger->Release();
        }

private:
    IWeakReference *_messenger;
};

#endif /* _JMSOFFICECOMM_MESSENGERCLASSFACTORY_H_ */
