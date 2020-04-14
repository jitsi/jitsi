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

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>GenericEvent</tt>s indicate reception of an new generic event.
 *
 * @author Damian Minkov
 */
public class GenericEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The contact which is the source of this event.
     */
    private Contact from;

    /**
     * The event name.
     */
    private String eventName;

    /**
     * The event value.
     */
    private String eventValue;

    /**
     * The source contact for this event.
     */
    private Contact sourceContact;

    /**
     * Constructs a GenericEvent.
     *
     * @param sourceProtocolProvider The object on which the Event initially occurred.
     * @param from the contact from which this event is coming from.
     * @param eventName the event name.
     * @param eventValue the event value.
     * @param sourceContact contact for this event.
     * @throws IllegalArgumentException if source is null.
     */
    public GenericEvent(ProtocolProviderService sourceProtocolProvider,
                         Contact from,
                         String eventName,
                         String eventValue,
                         Contact sourceContact)
    {
        super(sourceProtocolProvider);

        this.from = from;
        this.eventName = eventName;
        this.eventValue = eventValue;
        this.sourceContact = sourceContact;
    }

    /**
     * The event name.
     * @return the event name.
     */
    public String getEventName()
    {
        return eventName;
    }

    /**
     * The event value.
     * @return the event value.
     */
    public String getEventValue()
    {
        return eventValue;
    }

    /**
     * The contact which is the source of this event.
     * @return the contact which is the source of this event.
     */
    public Contact getFrom()
    {
        return from;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> which originated this event.
     *
     * @return the source <tt>ProtocolProviderService</tt>
     */
    public ProtocolProviderService getSourceProvider()
    {
        return (ProtocolProviderService) getSource();
    }

    /**
     * Returns a String representation of this GenericEvent.
     *
     * @return  A a String representation of this GenericEvent.
     */
    @Override
    public String toString()
    {
        return "GenericEvent from:" + from + " - eventName:"+ eventName
                + " eventValue:" + eventValue;
    }

    /**
     * Returns The source contact for this event.
     * @return the event source contact.
     */
    public Contact getSourceContact()
    {
        return sourceContact;
    }
}
