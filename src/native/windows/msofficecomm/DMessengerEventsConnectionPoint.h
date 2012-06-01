/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_DMESSENGEREVENTSCONNECTIONPOINT_H_
#define _JMSOFFICECOMM_DMESSENGEREVENTSCONNECTIONPOINT_H_

#include "ConnectionPoint.h"
#include <msgrua.h>

class DMessengerEventsConnectionPoint
    : public ConnectionPoint<DMessengerEvents, DIID_DMessengerEvents>
{
public:
    DMessengerEventsConnectionPoint(IConnectionPointContainer *container)
        : ConnectionPoint(container) {}
    virtual ~DMessengerEventsConnectionPoint() {}

    STDMETHODIMP OnContactStatusChange(LPDISPATCH pMContact, MISTATUS mStatus);
};

#endif /* #ifndef _JMSOFFICECOMM_DMESSENGEREVENTSCONNECTIONPOINT_H_ */
