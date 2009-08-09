/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A call change listener receives events indicating that a call has changed and
 * a peer has either left or joined.
 *
 * @author Emil Ivov
 */
public interface CallChangeListener
    extends EventListener
{
    /**
     * Indicates that a new call peer has joined the source call.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerAdded(CallPeerEvent evt);

    /**
     * Indicates that a call peer has left the source call.
     *
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerRemoved(CallPeerEvent evt);

    /**
     * Indicates that a change has occurred in the state of the source call.
     *
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt);
}
