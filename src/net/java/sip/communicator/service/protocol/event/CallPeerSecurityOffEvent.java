/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallPeerSecurityAuthenticationEvent</tt> is triggered whenever
 * a the security strings are received in a secure call.
 *
 * @author Yana Stamcheva
 */
public class CallPeerSecurityOffEvent
    extends CallPeerSecurityStatusEvent
{

    /**
     * The event constructor.
     *
     * @param callPeer the call peer associated with this event
     * @param sessionType the type of the session: audio or video
     */
    public CallPeerSecurityOffEvent( CallPeer callPeer,
                                            int sessionType)
    {
        super(callPeer, sessionType);
    }
}
