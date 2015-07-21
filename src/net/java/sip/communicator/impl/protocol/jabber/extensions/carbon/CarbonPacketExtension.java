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
package net.java.sip.communicator.impl.protocol.jabber.extensions.carbon;

import org.jivesoftware.smack.packet.*;
import org.xmlpull.v1.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * This class implements message carbons extensions added to message stanzas.
 *
 * @author Hristo Terezov
 */
public class CarbonPacketExtension
    extends AbstractPacketExtension
{

    /**
     * The namespace for the XML element.
     */
    public static final String NAMESPACE = "urn:xmpp:carbons:2";

    /**
     * The name of the "received" XML element.
     */
    public static final String RECEIVED_ELEMENT_NAME = "received";

    /**
     * The name of the "sent" XML element.
     */
    public static final String SENT_ELEMENT_NAME = "sent";

    /**
     * The name of the "private" XML element.
     */
    public static final String PRIVATE_ELEMENT_NAME = "private";


    /**
     * Constructs new <tt>CarbonPacketExtension</tt> instance.
     * @param elementName the name of the XML element.
     */
    public CarbonPacketExtension( String elementName)
    {
        super(NAMESPACE, elementName);
    }

    /**
     * Parses sent and received XML elements.
     */
    public static class Provider
        extends ForwardedPacketExtension.Provider
    {

        /**
         * The name of the elements to be parsed.
         */
        private String elementName;

        /**
         * Constructs new <tt>Provider</tt> instance.
         * @param elementName the name of the elements to be parsed
         */
        public Provider(String elementName)
        {
            this.elementName = elementName;
        }

        /**
         * Creates a <tt>CarbonPacketExtension</tt> by parsing
         * an XML document.
         * @param parser the parser to use.
         * @return the created <tt>CarbonPacketExtension</tt>.
         * @throws Exception
         */
        @Override
        public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception
        {
            CarbonPacketExtension packetExtension
                    = new CarbonPacketExtension(elementName);

            //now parse the sub elements
            boolean done = false;
            String elementName;
            ForwardedPacketExtension extension = null;
            while (!done)
            {
                switch (parser.next())
                {
                case XmlPullParser.START_TAG:
                {
                    elementName = parser.getName();
                    if (ForwardedPacketExtension.ELEMENT_NAME.equals(
                        elementName))
                    {
                        extension = (ForwardedPacketExtension) super
                            .parseExtension(parser);
                        if (extension != null)
                        {
                            packetExtension.addChildExtension(extension);
                        }
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                {
                    elementName = parser.getName();
                    if (this.elementName.equals(elementName))
                    {
                        done = true;
                    }
                    break;
                }
                }
            }
            return packetExtension;
        }
    }

    /**
     * This class implements the private carbon extension.
     */
    public static class PrivateExtension extends AbstractPacketExtension
    {
        public PrivateExtension()
        {
            super(NAMESPACE, PRIVATE_ELEMENT_NAME);
        }

    }
}
