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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Represents <tt>session-info</tt> elements such as active, ringing, or hold
 * for example.
 *
 * @author Emil Ivov
 */
public class SessionInfoPacketExtension extends AbstractPacketExtension
{
    /**
     * The name space for RTP description elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:info:1";

    /**
     * The exact type of this info packet.
     */
    private final SessionInfoType type;

    /**
     * Creates a new info element of the specified type.
     *
     * @param type the name of the element we'd like to create (mute, active,
     * hold);
     */
    public SessionInfoPacketExtension(SessionInfoType type)
    {
        super(NAMESPACE, type.toString());
        this.type = type;
    }

    /**
     * Returns the exact type of this {@link SessionInfoPacketExtension}.
     *
     * @return the {@link SessionInfoType} of this extension.
     */
    public SessionInfoType getType()
    {
        return type;
    }
}
