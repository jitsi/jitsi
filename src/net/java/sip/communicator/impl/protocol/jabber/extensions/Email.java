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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * A implementation of a {@link PacketExtension} for emails.
 *
 * @author Damian Minkov
 */
public class Email
    implements PacketExtension
{
    public static final String NAMESPACE = "jabber:client";

    public static final String ELEMENT_NAME = "email";

    private String address = null;

    public Email(String address)
    {
        this.address = address;
    }

    /**
     * The value of this email
     *
     * @return the email address
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Sets the value of this email
     *
     * @param address the address to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /*
     * Element name.
     * @return element name for this extension.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /*
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
        buf.append(getAddress());
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

            return new Email(address);
        }
    }
}
