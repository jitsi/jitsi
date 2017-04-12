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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * @author Boris Grozev
 */
public class WebSocketPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "web-socket" element.
     */
    public static final String ELEMENT_NAME = "web-socket";

    public static final String NAMESPACE = ColibriConferenceIQ.NAMESPACE;

    /**
     * The name of the "url" attribute.
     */
    public static final String URL_ATTR_NAME = "url";

    /**
     * Creates a new {@link WebSocketPacketExtension}
     */
    public WebSocketPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Creates a new {@link WebSocketPacketExtension}
     */
    public WebSocketPacketExtension(String url)
    {
        super(NAMESPACE, ELEMENT_NAME);
        setUrl(url);
    }

    /**
     * Sets the URL.
     */
    public void setUrl(String url)
    {
        super.setAttribute(URL_ATTR_NAME, url);
    }

    /**
     * @return the URL.
     */
    public String getUrl()
    {
        return super.getAttributeAsString(URL_ATTR_NAME);
    }
}
