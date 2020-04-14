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
 * The <tt>ContactResourceEvent</tt> is the event that notifies for any changes
 * in the <tt>ContactResource</tt>-s for a certain <tt>Contact</tt>.
 *
 * @author Yana Stamcheva
 */
public class ContactResourceEvent
    extends EventObject
{
    /**
     * The <tt>ContactResource</tt> that is concerned by the change.
     */
    private final ContactResource contactResource;

    /**
     * One of the event types defined in this class: RESOURCE_ADDED,
     * RESOURCE_REMOVED, RESOURCE_MODIFIED.
     */
    private final int eventType;

    /**
     * Indicates that the <tt>ContactResourceEvent</tt> instance was triggered
     * by the add of a <tt>ContactResource</tt>.
     */
    public static final int RESOURCE_ADDED = 0;

    /**
     * Indicates that the <tt>ContactResourceEvent</tt> instance was triggered
     * by the removal of a <tt>ContactResource</tt>.
     */
    public static final int RESOURCE_REMOVED = 1;

    /**
     * Indicates that the <tt>ContactResourceEvent</tt> instance was triggered
     * by the modification of a <tt>ContactResource</tt>.
     */
    public static final int RESOURCE_MODIFIED = 2;

    /**
     * Creates an instance of <tt>ContactResourceEvent</tt> by specifying the
     * source, where this event occurred and the concerned
     * <tt>ContactSource</tt>.
     *
     * @param source the source where this event occurred
     * @param contactResource the <tt>ContactResource</tt> that is concerned by
     * the change
     * @param eventType an integer representing the type of this event. One of
     * the types defined in this class: RESOURCE_ADDED, RESOURCE_REMOVED,
     * RESOURCE_MODIFIED.
     */
    public ContactResourceEvent(Contact source,
                                ContactResource contactResource,
                                int eventType)
    {
        super(source);

        this.contactResource = contactResource;
        this.eventType = eventType;
    }

    /**
     * Returns the <tt>Contact</tt>, which is the source of this event.
     *
     * @return the <tt>Contact</tt>, which is the source of this event
     */
    public Contact getContact()
    {
        return (Contact) getSource();
    }

    /**
     * Returns the <tt>ContactResource</tt> that is concerned by the change.
     *
     * @return the <tt>ContactResource</tt> that is concerned by the change
     */
    public ContactResource getContactResource()
    {
        return contactResource;
    }

    /**
     * Returns the type of the event.
     * <p>
     * One of the event types defined in this class: RESOURCE_ADDED,
     * RESOURCE_REMOVED, RESOURCE_MODIFIED.
     *
     * @return an int representing the type of the event
     */
    public int getEventType()
    {
        return eventType;
    }
}
