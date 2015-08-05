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
 * Joining type.
 *
 * @author Sebastien Vincent
 */
public enum JoiningType
{
    /**
     * Dialed-in.
     */
    dialed_in("dialed-in"),

    /**
     * Dialed-out.
     */
    dialed_out("dialed-out"),

    /**
     * Focus owner.
     */
    focus_owner("focus-owner");

    /**
     * The name of this type.
     */
    private final String type;

    /**
     * Creates a <tt>JoiningType</tt> instance with the specified name.
     *
     * @param type type name.
     */
    private JoiningType(String type)
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
     * Returns a <tt>JoiningType</tt>.
     *
     * @param typeStr the <tt>String</tt> that we'd like to
     * parse.
     * @return an JoiningType.
     *
     * @throws IllegalArgumentException in case <tt>typeStr</tt> is
     * not a valid <tt>EndPointType</tt>.
     */
    public static JoiningType parseString(String typeStr)
        throws IllegalArgumentException
    {
        for (JoiningType value : values())
            if (value.toString().equals(typeStr))
                return value;

        throw new IllegalArgumentException(
            typeStr + " is not a valid reason");
    }
}
