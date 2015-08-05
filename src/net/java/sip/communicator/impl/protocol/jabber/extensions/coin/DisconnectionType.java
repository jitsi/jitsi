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
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

/**
 * Disconnection type.
 *
 * @author Sebastien Vincent
 */
public enum DisconnectionType
{
    /**
     * Departed.
     */
    departed("departed"),

    /**
     * Booted.
     */
    booted("booted"),

    /**
     * Failed.
     */
    failed("failed"),

    /**
     * Busy
     */
    busy("busy");

    /**
     * The name of this type.
     */
    private final String type;

    /**
     * Creates a <tt>DisconnectionType</tt> instance with the specified name.
     *
     * @param type type name.
     */
    private DisconnectionType(String type)
    {
        this.type = type;
    }

    /**
     * Returns the type name.
     *
     * @return type name
     */
    @Override
    public String toString()
    {
        return type;
    }

    /**
     * Returns a <tt>DisconnectionType</tt>.
     *
     * @param typeStr the <tt>String</tt> that we'd like to
     * parse.
     * @return an DisconnectionType.
     *
     * @throws IllegalArgumentException in case <tt>typeStr</tt> is
     * not a valid <tt>EndPointType</tt>.
     */
    public static DisconnectionType parseString(String typeStr)
        throws IllegalArgumentException
    {
        for (DisconnectionType value : values())
            if (value.toString().equals(typeStr))
                return value;

        throw new IllegalArgumentException(
            typeStr + " is not a valid reason");
    }
}
