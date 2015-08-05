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

import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for DescriptionPacketExtension.
 *
 * @author Sebastien Vincent
 */
public class DescriptionProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a description extension sub-packet and creates a {@link
     * DescriptionPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>description</tt> element.
     *
     * @return a new {@link DescriptionPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public DescriptionPacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;

        DescriptionPacketExtension ext
            = new DescriptionPacketExtension();

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(
                        DescriptionPacketExtension.ELEMENT_SUBJECT))
                {
                    ext.setSubject(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        DescriptionPacketExtension.ELEMENT_FREE_TEXT))
                {
                    ext.setFreeText(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        DescriptionPacketExtension.ELEMENT_DISPLAY_TEXT))
                {
                    ext.setDisplayText(CoinIQProvider.parseText(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        DescriptionPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
