/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * <tt>DTMFReceivedEvent</tt>s indicate reception of a DTMF tone.
 *
 * @author Damian Minkov
 */
public class DTMFReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The tone.
     */
    private DTMFTone value = null;

    /**
     * The duration.
     */
    private long duration;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the <tt>Message</tt> whose reception this event represents.
     */
    public DTMFReceivedEvent(ProtocolProviderService source,
                             DTMFTone value,
                             long duration)
    {
        super(source);

        this.value = value;
        this.duration = duration;
    }

    /**
     * Returns the tone this event is indicating of.
     * @return the tone this event is indicating of.
     */
    public DTMFTone getValue()
    {
        return value;
    }

    /**
     * Returns the tone duration for this event.
     * @return the tone duration for this event.
     */
    public long getDuration()
    {
        return duration;
    }
}
