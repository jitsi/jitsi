/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallPeerSecurityNegotiationStartedEvent</tt> is triggered whenever a
 * communication with a given peer is established,
 * we started securing the connection.
 *
 * @author Damian Minkov
 */
public class CallPeerSecurityNegotiationStartedEvent
    extends CallPeerSecurityStatusEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The event constructor
     *
     * @param callPeer the call peer associated with this event
     * @param sessionType the type of the session, either
     *            {@link net.java.sip.communicator.service.protocol.event.CallPeerSecurityStatusEvent#AUDIO_SESSION} or
     *            {@link net.java.sip.communicator.service.protocol.event.CallPeerSecurityStatusEvent#VIDEO_SESSION}
     */
    public CallPeerSecurityNegotiationStartedEvent(CallPeer callPeer,
                                                   int sessionType)
    {
        super(callPeer, sessionType);
    }
}
