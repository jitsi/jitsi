/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;

/**
 * The <tt>CallPeerSecurityOnEvent</tt> is triggered whenever a
 * communication with a given peer is going secure.
 *
 * @author Werner Dittmann
 * @author Yana Stamcheva
 */
public class CallPeerSecurityOnEvent
    extends CallPeerSecurityStatusEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final String cipher;

    private final SrtpControl srtpControl;

    /**
     * The event constructor
     * 
     * @param callPeer the call peer associated with this event
     * @param sessionType the type of the session, either
     *            {@link CallPeerSecurityStatusEvent#AUDIO_SESSION} or
     *            {@link CallPeerSecurityStatusEvent#VIDEO_SESSION}
     * @param cipher the cipher used for the encryption
     * @param srtpControl the security controller that caused this event
     */
    public CallPeerSecurityOnEvent( CallPeer callPeer,
                                    int sessionType,
                                    String cipher,
                                    SrtpControl srtpControl)
    {
        super(callPeer, sessionType);
        this.srtpControl = srtpControl;
        this.cipher = cipher;
    }

    /**
     * Returns the cipher used for the encryption.
     *
     * @return the cipher used for the encryption.
     */
    public String getCipher()
    {
        return cipher;
    }

    /**
     * Gets the security controller that caused this event.
     * 
     * @return the security controller that caused this event.
     */
    public SrtpControl getSecurityController()
    {
        return srtpControl;
    }
}
