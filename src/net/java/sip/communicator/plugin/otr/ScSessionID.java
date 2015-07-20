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
package net.java.sip.communicator.plugin.otr;

import java.util.*;

import net.java.otr4j.session.*;

/**
 * Class used to associate a random UUID to an OTR4J SessionID.
 *
 * @author Daniel Perren
 */
public class ScSessionID
{
    private final UUID guid = UUID.randomUUID();

    private final SessionID sessionID;

    /**
     * Creates a new instance of this class.
     *
     * @param sessionID the OTR4J SessionID that is being wrapped.
     */
    public ScSessionID(SessionID sessionID)
    {
        this.sessionID = sessionID;
    }

    /**
     * Overrides equals() for the ability to get the hashcode from sessionID.
     *
     * @param obj the object to compare
     * @return true if the objects are considered equal.
     */
    @Override
    public boolean equals(Object obj)
    {
        return (obj != null) && sessionID.toString().equals(obj.toString());
    }

    /**
     * Get the current GUID.
     *
     * @return The GUID generated for this SessionID.
     */
    public UUID getGUID()
    {
        return guid;
    }

    /**
     * Gets the wrapped session ID
     *
     * @return sessionID
     */
    public SessionID getSessionID()
    {
        return sessionID;
    }

    /**
     * Returns {@link SessionID#hashCode()} of the wrapped SessionID.
     *
     * @return HashCode of the wrapped SessionID.
     */
    @Override
    public int hashCode()
    {
        return sessionID.hashCode();
    }

    /**
     * Returns {@link SessionID#toString()} of the wrapped SessionID.
     * @return String representation of the wrapped SessionID.
     */
    @Override
    public String toString()
    {
        return sessionID.toString();
    }
}
