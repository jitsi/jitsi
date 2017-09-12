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
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * A implementation of a {@link PacketExtension} for the jitsi-meet "avatar-url"
 * element.
 *
 * @author Boris Grozev
 */
public class AvatarUrl
    implements PacketExtension
{
    public static final String NAMESPACE = "jabber:client";

    public static final String ELEMENT_NAME = "avatar-url";

    private String avatarUrl = null;

    /**
     * Initializes an {@link AvatarUrl} instance with a given string value.
     * @param avatarUrl the string value.
     */
    public AvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    /**
     * @return the value of the avatar-url element as a string.
     */
    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    /**
     * Sets the value of this avatar-url element.
     *
     * @param avatarUrl the value to set.
     */
    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    /**
     * Element name.
     * @return element name for this extension.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the namespace for this extension.
     * @return the namespace for this extension.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /*
     * Returns xml representation of this extension.
     * @return xml representation of this extension.
     */
    public String toXML()
    {
        final StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(">");
        buf.append(getAvatarUrl());
        buf.append("</").append(ELEMENT_NAME).append('>');

        return buf.toString();
    }

    /**
     * The provider.
     */
    public static class Provider
        implements PacketExtensionProvider
    {
        public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception
        {
            parser.next();
            final String address = parser.getText();

            // Advance to end of extension.
            while(parser.getEventType() != XmlPullParser.END_TAG)
            {
                parser.next();
            }

            return new AvatarUrl(address);
        }
    }
}
