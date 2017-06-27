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
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import org.jivesoftware.smack.packet.*;
import org.jxmpp.jid.*;

/**
 * KeepAlive Event. Events are sent if there are no received packets
 * for a specified interval of time.
 * XEP-0199: XMPP Ping.
 *
 * @author Damian Minkov
 */
public class KeepAliveEvent
    extends IQ
{
    /**
     * Element name for ping.
     */
    public static final String ELEMENT_NAME = "ping";

    /**
     * Namespace for ping.
     */
    public static final String NAMESPACE = "urn:xmpp:ping";

    /**
     * Constructs empty packet
     */
    public KeepAliveEvent()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Construct packet for sending.
     *
     * @param from the address of the contact that the packet coming from.
     * @param to the address of the contact that the packet is to be sent to.
     */
    public KeepAliveEvent(Jid from, Jid to)
    {
        this();
        if (to == null)
        {
            throw new IllegalArgumentException("to cannot be null");
        }

        setType(Type.get);
        setTo(to);
        setFrom(from);
    }

    @Override

    /**
     * Returns the sub-element XML section of this packet
     *
     * @return the packet as XML.
     */
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf)
    {
        buf.setEmptyElement();
        return buf;
    }
}
