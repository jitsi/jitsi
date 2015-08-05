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
 * Parser for UsersPacketExtension.
 *
 * @author Sebastien Vincent
 */
public class UsersProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a users extension sub-packet and creates a {@link
     * UsersPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>Users</tt> element.
     *
     * @return a new {@link UsersPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public UsersPacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;
        StateType state = StateType.full;
        String stateStr = parser.getAttributeValue("",
                UserPacketExtension.STATE_ATTR_NAME);

        if(stateStr != null)
        {
            state = StateType.parseString(stateStr);
        }

        UsersPacketExtension ext
            = new UsersPacketExtension();

        ext.setAttribute(UsersPacketExtension.STATE_ATTR_NAME, state);

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(UserPacketExtension.ELEMENT_NAME))
                {
                    PacketExtensionProvider provider
                        = new UserProvider();
                    PacketExtension childExtension = provider.parseExtension(
                            parser);
                    ext.addChildExtension(childExtension);
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        UsersPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
