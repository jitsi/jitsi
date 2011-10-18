/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Instances of this class are used for listening for notifications coming out
 * of a telephony Provider - such as an incoming Call for example. Whenever a
 * telephony Provider receives an invitation to a call from a particular
 *
 * @author Emil Ivov
 */
public interface CallListener
    extends EventListener
{

    /**
     * This method is called by a protocol provider whenever an incoming call is
     * received.
     *
     * @param event a CallEvent instance describing the new incoming call
     */
    public void incomingCallReceived(CallEvent event);

    /**
     * This method is called by a protocol provider upon initiation of an
     * outgoing call.
     * <p>
     *
     * @param event a CalldEvent instance describing the new incoming call.
     */
    public void outgoingCallCreated(CallEvent event);

    /**
     * Indicates that all peers have left the source call and that it has
     * been ended. The event may be considered redundant since there are already
     * events issued upon termination of a single call peer but we've
     * decided to keep it for listeners that are only interested in call
     * duration and don't want to follow other call details.
     *
     * @param event the <tt>CallEvent</tt> containing the source call.
     */
    public void callEnded(CallEvent event);
}
