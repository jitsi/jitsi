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

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

/**
 * An {@link AbstractPacketExtension} implementation for transport elements.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class RawUdpTransportPacketExtension
    extends IceUdpTransportPacketExtension
{
    /**
     * The name of the "transport" element.
     */
    public static final String NAMESPACE
        = "urn:xmpp:jingle:transports:raw-udp:1";
    /**
     * The name of the "transport" element.
     */
    public static final String ELEMENT_NAME = "transport";

    /**
     * Creates a new {@link RawUdpTransportPacketExtension} instance.
     */
    public RawUdpTransportPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns this element's child (local or remote) candidate elements.
     *
     * @return this element's child (local or remote) candidate elements.
     */
    @Override
    public List<? extends PacketExtension> getChildExtensions()
    {
        // TODO Auto-generated method stub
        return super.getChildExtensions();
    }
}
