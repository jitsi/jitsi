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

/**
 * The <tt>CallPeerSecurityFailedEvent</tt> is triggered whenever
 * a problem has occurred during call security process.
 *
 * @author Yana Stamcheva
 * @author Werner Dittmann
 */
public class CallPeerSecurityMessageEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The internationalized message associated with this event.
     */
    private final String eventI18nMessage;

    /**
     * The message associated with this event.
     */
    private final String eventMessage;

    /**
     * The severity of the security message event.
     */
    private final int eventSeverity;

    /**
     * Creates a <tt>CallPeerSecurityFailedEvent</tt> by specifying the
     * call peer, event type and message associated with this event.
     *
     * @param source the object on which the event initially occurred
     * @param eventMessage the message associated with this event.
     * @param i18nMessage the internationalized message associated with this
     * event that could be shown to the user.
     * @param eventSeverity severity level.
     */
    public CallPeerSecurityMessageEvent(
            Object source,
            String eventMessage,
            String i18nMessage,
            int eventSeverity)
    {
        super(source);

        this.eventMessage = eventMessage;
        this.eventI18nMessage = i18nMessage;
        this.eventSeverity = eventSeverity;
    }

    /**
     * Returns the message associated with this event.
     *
     * @return the message associated with this event.
     */
    public String getMessage()
    {
        return eventMessage;
    }

    /**
     * Returns the internationalized message associated with this event.
     *
     * @return the internationalized message associated with this event.
     */
    public String getI18nMessage()
    {
        return eventI18nMessage;
    }

    /**
     * Returns the event severity.
     *
     * @return the eventSeverity
     */
    public int getEventSeverity()
    {
        return eventSeverity;
    }
}
