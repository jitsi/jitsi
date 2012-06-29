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
 * The <tt>CallPeerSecurityTimeoutEvent</tt> is triggered whenever a
 * communication with a given peer cannot be established, the peer
 * did not answer our tries to secure the connection.
 *
 * @author Damian Minkov
 */
public class CallPeerSecurityTimeoutEvent
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
     *            {@link CallPeerSecurityStatusEvent#AUDIO_SESSION} or
     *            {@link CallPeerSecurityStatusEvent#VIDEO_SESSION}
     */
    public CallPeerSecurityTimeoutEvent( CallPeer callPeer,
                                    int sessionType)
    {
        super(callPeer, sessionType);
    }
}
