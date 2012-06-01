/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
