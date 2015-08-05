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
 * Status type.
 *
 * @author Sebastien Vincent
 */
public enum StateType
{
    /**
     * Full state.
     */
    full,

    /**
     * Partial state.
     */
    partial,

    /**
     * Deleted state.
     */
    deleted;

    /**
     * Returns a <tt>StateType</tt>.
     *
     * @param typeStr the <tt>String</tt> that we'd like to
     * parse.
     * @return a StateType.
     *
     * @throws IllegalArgumentException in case <tt>typeStr</tt> is
     * not a valid <tt>EndPointType</tt>.
     */
    public static StateType parseString(String typeStr)
        throws IllegalArgumentException
    {
        for (StateType value : values())
            if (value.toString().equals(typeStr))
                return value;

        throw new IllegalArgumentException(
            typeStr + " is not a valid reason");
    }
}
