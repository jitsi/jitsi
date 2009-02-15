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
public abstract class CallParticipantSecurityStatusEvent
    extends EventObject
{
    /**
     * Constant value defining that security is enabled.
     */
    public static final String AUDIO_SESSION = "AUDIO_SESSION";

    /**
     * Constant value defining that security is disabled.
     */
    public static final String VIDEO_SESSION = "VIDEO_SESSION";

    /**
     * Constructor required by the EventObject.
     * 
     * @param source the source object for this event.
     */
    public CallParticipantSecurityStatusEvent(Object source)
    {
        super(source);
    }
}
