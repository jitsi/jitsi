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

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for UserPacketExtension.
 *
 * @author Sebastien Vincent
 */
public class UserProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a User extension sub-packet and creates a {@link
     * UserPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>User</tt> element.
     *
     * @return a new {@link UserPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public UserPacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;
        String entity = parser.getAttributeValue("",
                UserPacketExtension.ENTITY_ATTR_NAME);
        StateType state = StateType.full;
        String stateStr = parser.getAttributeValue("",
                UserPacketExtension.STATE_ATTR_NAME);

        if(stateStr != null)
        {
            state = StateType.parseString(stateStr);
        }

        if(entity == null)
        {
            throw new Exception(
                    "Coin user element must contain entity attribute");
        }

        UserPacketExtension ext = new UserPacketExtension(entity);
        ext.setAttribute(UserPacketExtension.STATE_ATTR_NAME, state);

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(UserPacketExtension.ELEMENT_DISPLAY_TEXT))
                {
                    ext.setDisplayText(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        EndpointPacketExtension.ELEMENT_NAME))
                {
                    PacketExtensionProvider provider
                        = new EndpointProvider();
                    PacketExtension childExtension = provider.parseExtension(
                        parser);
                    ext.addChildExtension(childExtension);
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        UserPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
