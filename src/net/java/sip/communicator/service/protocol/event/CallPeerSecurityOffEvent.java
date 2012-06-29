/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.protocol.event.*;

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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
