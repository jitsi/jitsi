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
package net.java.sip.communicator.service.ldap.event;

import java.util.*;

/**
 * An LdapEvent is triggered when
 * the state of the LDAP connection changes.
 * Available states for the moment are :
 * connected
 * disconnected
 * connecting
 * disconnecting
 *
 * @author Sebastien Mazy
 */
public class LdapEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates the possible causes why an event was triggered
     */
    public static enum LdapEventCause
    {
        /**
         * New result is available.
         */
        NEW_SEARCH_RESULT,

        /**
         * Search is achieved.
         */
        SEARCH_ACHIEVED,

        /**
         * Search is cancelled.
         */
        SEARCH_CANCELLED,

        /**
         * Problem occurred during search.
         */
        SEARCH_ERROR,

        /**
         * Authentication failed.
         */
        SEARCH_AUTH_ERROR
    }

    /**
     * the cause of this event
     */
    private final LdapEventCause cause;

    /**
     * the content of this event
     */
    private final Object content;

    /**
     * Simple constructor for this class
     *
     * @param source the source of the event (most likely an LdapDirectory)
     * @param cause the cause why it was triggered
     */
    public LdapEvent(LdapEventManager source, LdapEventCause cause)
    {
        this(source, cause, null);
    }

    /**
     * Another constructor for this class. Use that one to pass more
     * information to the listener using any Object.
     *
     * @param source the source of the event (most likely an LdapDirectory)
     * @param cause the cause why it was triggered
     * @param content related content
     */
    public LdapEvent(LdapEventManager source, LdapEventCause cause,
            Object content)
    {
        super(source);
        this.cause = cause;
        this.content = content;
    }

    /**
     * @return the cause why this event was triggered
     */
    public LdapEventCause getCause()
    {
        return this.cause;
    }

    /**
     * @return the object embedded in this event, or null if there isn't one
     */
    public Object getContent()
    {
        return this.content;
    }

    /**
     * Returns the LdapEventManager which sent the LdapEvent.
     *
     * @return the LdapEventManager which sent the LdapEvent
     */
    @Override
    public LdapEventManager getSource()
    {
        return (LdapEventManager) super.getSource();
    }
}
