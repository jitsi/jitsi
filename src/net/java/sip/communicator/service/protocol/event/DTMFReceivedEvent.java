/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import org.jitsi.service.protocol.*;

/**
 * <tt>DTMFReceivedEvent</tt>s indicate reception of a DTMF tone.
 *
 * @author Damian Minkov
 * @author Boris Grozev
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
    private final DTMFTone value;

    /**
     * The duration.
     */
    private final long duration;

    /**
     * Whether this <tt>DTMFReceivedEvent</tt> represents the start of reception
     * of a tone (if <tt>true</tt>), the end of reception of a tone (if
     * <tt>false</tt>), or the reception of a tone with a given duration (if
     * <tt>null</tt>).
     */
    private final Boolean start;

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the source of the event.
     * @param value dmtf tone value.
     * @param start whether this event represents the start of reception (if
     * <tt>true</tt>), the end of reception (if <tt>false</tt>) or the reception
     * of a tone with a given direction (if <tt>null</tt>).
     */
    public DTMFReceivedEvent(Object source,
                             DTMFTone value,
                             boolean start)
    {
        this(source, value, -1, start);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the source of the event.
     * @param value dmtf tone value.
     * @param duration duration of the DTMF tone.
     */
    public DTMFReceivedEvent(Object source,
                             DTMFTone value,
                             long duration)
    {
        this(source, value, duration, null);
    }

    /**
     * Creates a <tt>MessageReceivedEvent</tt> representing reception of the
     * <tt>source</tt> message received from the specified <tt>from</tt>
     * contact.
     *
     * @param source the source of the event.
     * @param value dmtf tone value.
     * @param duration duration of the DTMF tone.
     * @param start whether this event represents the start of reception (if
     * <tt>true</tt>), the end of reception (if <tt>false</tt>) or the reception
     * of a tone with a given direction (if <tt>null</tt>).
     */
    public DTMFReceivedEvent(Object source,
                             DTMFTone value,
                             long duration,
                             Boolean start)
    {
        super(source);

        this.value = value;
        this.duration = duration;
        this.start = start;
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

    /**
     * Returns the value of the <tt>start</tt> attribute of this
     * <tt>DTMFReceivedEvent</tt>, which indicates whether this
     * <tt>DTMFReceivedEvent</tt> represents the start of reception of a tone
     * (if <tt>true</tt>), the end of reception of a tone (if <tt>false</tt>),
     * or the reception of a tone with a given duration (if <tt>null</tt>).
     */
    public Boolean getStart()
    {
        return start;
    }
}
