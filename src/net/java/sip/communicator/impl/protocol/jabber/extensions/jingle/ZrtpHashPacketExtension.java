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
 * An implementation of the "zrtp-hash" attribute as described in the currently
 * deferred XEP-0262.
 *
 * @author Emil Ivov
 */
public class ZrtpHashPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "zrtp-hash" element.
     */
    public static final String ELEMENT_NAME = "zrtp-hash";

    /**
     * The namespace for the "zrtp-hash" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:zrtp:1";

    /**
     * The name of the <tt>version</tt> attribute.
     */
    public static final String VERSION_ATTR_NAME = "version";

    /**
     * Creates a {@link ZrtpHashPacketExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     */
    public ZrtpHashPacketExtension()
    {
        super (NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns the ZRTP version used by the implementation that created the
     * hash.
     *
     * @return the ZRTP version used by the implementation that created the
     * hash.
     */
    public String getVersion()
    {
        return getAttributeAsString(VERSION_ATTR_NAME);
    }

    /**
     * Sets the ZRTP version used by the implementation that created the
     * hash.
     *
     * @param version the ZRTP version used by the implementation that created
     * the hash.
     */
    public void setVersion(String version)
    {
        setAttribute(VERSION_ATTR_NAME, version);
    }

    /**
     * Returns the value of the ZRTP hash this element is carrying.
     *
     * @return the value of the ZRTP hash this element is carrying.
     */
    public String getValue()
    {
        return getText();
    }

    /**
     * Sets the value of the ZRTP hash this element will be carrying.
     *
     * @param value the value of the ZRTP hash this element will be carrying.
     */
    public void setValue(String value)
    {
        setText(value);
    }
}
