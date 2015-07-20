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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Server packet extension.
 *
 * @author Sebastien Vincent
 */
public class ServerPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace.
     */
    public static final String NAMESPACE = null;

    /**
     * The element name.
     */
    public static final String ELEMENT_NAME = "server";

    /**
     * Host attribute name.
     */
    public static final String HOST_ATTR_NAME = "host";

    /**
     * TCP attribute name.
     */
    public static final String TCP_ATTR_NAME = "tcp";

    /**
     * UDP attribute name.
     */
    public static final String UDP_ATTR_NAME = "udp";

    /**
     * SSL attribute name.
     */
    public static final String SSL_ATTR_NAME = "tcpssl";

    /**
     * Constructor.
     */
    public ServerPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns the host address.
     *
     * @return this host address
     */
    public String getHost()
    {
        return super.getAttributeAsString(HOST_ATTR_NAME);
    }

    /**
     * Returns the UDP port.
     *
     * @return the UDP port
     */
    public int getUdp()
    {
        return Integer.parseInt(super.getAttributeAsString(UDP_ATTR_NAME));
    }

    /**
     * Returns the TCP port.
     *
     * @return the TCP port
     */
    public int getTcp()
    {
        return Integer.parseInt(super.getAttributeAsString(TCP_ATTR_NAME));
    }

    /**
     * Returns the SSL port.
     *
     * @return the SSL port
     */
    public int getSsl()
    {
        return Integer.parseInt(super.getAttributeAsString(SSL_ATTR_NAME));
    }
}
