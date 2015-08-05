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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for EndpointPacketExtension.
 *
 * @author Sebastien Vincent
 */
public class EndpointProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a endpoint extension sub-packet and creates a {@link
     * EndpointPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>Endpoint</tt> element.
     *
     * @return a new {@link EndpointPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;
        String entity = parser.getAttributeValue("",
                EndpointPacketExtension.ENTITY_ATTR_NAME);
        StateType state = StateType.full;
        String stateStr = parser.getAttributeValue("",
                EndpointPacketExtension.STATE_ATTR_NAME);

        if(stateStr != null)
        {
            state = StateType.parseString(stateStr);
        }

        EndpointPacketExtension ext
            = new EndpointPacketExtension(entity);

        ext.setAttribute(EndpointPacketExtension.STATE_ATTR_NAME, state);

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(
                        EndpointPacketExtension.ELEMENT_DISPLAY_TEXT))
                {
                    ext.setDisplayText(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        EndpointPacketExtension.ELEMENT_DISCONNECTION))
                {
                    ext.setDisconnectionType(
                            DisconnectionType.parseString(parser.getText()));
                }
                else if(elementName.equals(
                        EndpointPacketExtension.ELEMENT_JOINING))
                {
                    ext.setJoiningType(JoiningType.parseString(
                            CoinIQProvider.parseText(parser)));
                }
                else if(elementName.equals(
                        EndpointPacketExtension.ELEMENT_STATUS))
                {
                    ext.setStatus(EndpointStatusType.parseString(
                            CoinIQProvider.parseText(parser)));
                }
                else if(elementName.equals(
                        CallInfoPacketExtension.ELEMENT_NAME))
                {
                    PacketExtensionProvider provider
                        = new DefaultPacketExtensionProvider<
                        CallInfoPacketExtension>(CallInfoPacketExtension.class);
                    PacketExtension childExtension = provider.parseExtension(
                            parser);
                    ext.addChildExtension(childExtension);
                }
                else if(elementName.equals(MediaPacketExtension.ELEMENT_NAME))
                {
                    PacketExtensionProvider provider
                        = new MediaProvider();
                    PacketExtension childExtension = provider.parseExtension(
                            parser);
                    ext.addChildExtension(childExtension);
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        EndpointPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
