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
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * The <tt>ContactQueryStatusEvent</tt> is triggered each time a
 * <tt>ContactQuery</tt> changes its status. Possible statuses are:
 * QUERY_COMPLETED, QUERY_CANCELED and QUERY_ERROR.
 *
 * @author Yana Stamcheva
 */
public class ContactQueryStatusEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a query has been completed.
     */
    public static final int QUERY_COMPLETED = 0;

    /**
     * Indicates that a query has been canceled.
     */
    public static final int QUERY_CANCELED = 1;

    /**
     * Indicates that a query has been stopped because of an error.
     */
    public static final int QUERY_ERROR = 2;

    /**
     * Indicates the type of this event.
     */
    private final int eventType;

    /**
     * Creates a <tt>ContactQueryStatusEvent</tt> by specifying the source
     * <tt>ContactQuery</tt> and the <tt>eventType</tt> indicating why initially
     * this event occurred.
     * @param source the initiator of the event
     * @param eventType the type of the event. One of the QUERY_XXX constants
     * defined in this class
     */
    public ContactQueryStatusEvent( ContactQuery source,
                                    int eventType)
    {
        super(source);

        this.eventType = eventType;
    }

    /**
     * Returns the <tt>ContactQuery</tt> that triggered this event.
     * @return the <tt>ContactQuery</tt> that triggered this event
     */
    public ContactQuery getQuerySource()
    {
        return (ContactQuery) source;
    }

    /**
     * Returns the type of this event.
     * @return the type of this event
     */
    public int getEventType()
    {
        return eventType;
    }
}
