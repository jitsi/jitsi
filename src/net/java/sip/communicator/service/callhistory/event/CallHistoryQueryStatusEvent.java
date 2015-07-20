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
package net.java.sip.communicator.service.callhistory.event;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;

/**
 * The <tt>CallHistoryQueryStatusEvent</tt> is triggered each time a
 * <tt>CallHistoryQuery</tt> changes its status. Possible statuses are:
 * QUERY_COMPLETED, QUERY_CANCELED and QUERY_ERROR.
 *
 * @author Yana Stamcheva
 */
public class CallHistoryQueryStatusEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates the type of this event.
     */
    private final int eventType;

    /**
     * Creates a <tt>CallHistoryQueryStatusEvent</tt> by specifying the source
     * <tt>CallHistoryQuery</tt> and the <tt>eventType</tt> indicating why
     * initially this event occurred.
     * @param source the <tt>CallHistoryQuery</tt> this event is about
     * @param eventType the type of the event. One of the QUERY_XXX constants
     * defined in the <tt>CallHistoryQuery</tt>
     */
    public CallHistoryQueryStatusEvent( CallHistoryQuery source,
                                        int eventType)
    {
        super(source);

        this.eventType = eventType;
    }

    /**
     * Returns the <tt>CallHistoryQuery</tt> that triggered this event.
     * @return the <tt>CallHistoryQuery</tt> that triggered this event
     */
    public CallHistoryQuery getQuerySource()
    {
        return (CallHistoryQuery) source;
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
