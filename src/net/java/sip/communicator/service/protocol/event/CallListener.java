/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.event;

import java.util.*;


/**
 * Instances of this class are used for listening for notifications coming out
 * of a telephony Provider - such as an incoming Call for example. Whenever
 * a telephony Provider receives an invitation to a call from a particular
 *
 * @author Emil Ivov
 */
public interface CallListener extends EventListener
{
    /**
     * This method is called by a protocol provider whenever an incoming call
     * is received.
     * @param event a CallReceivedEvent instance describing the new incoming
     * call
     */
    public void incomingCallReceived(CallReceivedEvent event);
}
