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
package net.java.sip.communicator.service.notification.event;

import java.util.*;

import net.java.sip.communicator.service.notification.*;

/**
 * Fired any time an event type is added or removed.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class NotificationEventTypeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a new event type is added.
     */
    public static final String EVENT_TYPE_ADDED = "EventTypeAdded";

    /**
     * Indicates that an event type was removed.
     */
    public static final String EVENT_TYPE_REMOVED = "EventTypeRemoved";

    /**
     * The type of the event that a new action is being added for.
     */
    private String sourceEventType = null;

    /**
     * The type of this event. One of the static field constants declared in
     * this class.
     */
    private String eventType = null;

    /**
     * Creates an instance of this event according to the specified type.
     *
     * @param source the <tt>NotificationService</tt> that dispatched this event
     * @param eventType the type of this event. One of the static fields
     * declared in this class
     * @param sourceEventType the event type for which this event occured
     */
    public NotificationEventTypeEvent(  NotificationService source,
                                        String eventType,
                                        String sourceEventType)
    {
        super(source);

        this.eventType = eventType;
        this.sourceEventType = sourceEventType;
    }

    /**
     * Returns the <tt>eventType</tt>, for which this event is about.
     *
     * @return the <tt>eventType</tt>, for which this event is about.
     */
    public String getSourceEventType()
    {
        return sourceEventType;
    }

    /**
     * The type of this event. One of EVENT_TYPE_XXX constants declared in this
     * class.
     *
     * @return the type of this event
     */
    public String getEventType()
    {
        return eventType;
    }
}
