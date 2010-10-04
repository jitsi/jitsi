/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Parent class for SecurityOn and SecurityOff events.
 *
 * @author Yana Stamcheva
 */
public abstract class CallPeerSecurityStatusEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constant value defining that security is enabled.
     */
    public static final int AUDIO_SESSION = 1;

    /**
     * Constant value defining that security is disabled.
     */
    public static final int VIDEO_SESSION = 2;

    /**
     * Session type of the event <code>AUDIO_SESSION</code> or
     * <code>VIDEO_SESSION</code>.
     */
    private final int sessionType;

    /**
     * Constructor required by the EventObject.
     *
     * @param source the source object for this event.
     * @param sessionType either <code>AUDIO_SESSION</code> or
     *                    <code>VIDEO_SESSION</code> to indicate the type of the
     *                    session
     */
    public CallPeerSecurityStatusEvent(Object source, int sessionType)
    {
        super(source);

        this.sessionType = sessionType;
    }

    /**
     * Returns the type of the session, either AUDIO_SESSION or VIDEO_SESSION.
     *
     * @return the type of the session, either AUDIO_SESSION or VIDEO_SESSION.
     */
    public int getSessionType()
    {
        return sessionType;
    }
}
