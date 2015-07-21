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

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * This class parses incoming messages and extracts the whiteboard parameters
 * from the raw XML messages.
 *
 * @author Julien Waechter
 */
public class WhiteboardObjectJabberProvider
    implements PacketExtensionProvider
{
    /**
     * Creates a new WhiteboardObjectJabberProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public WhiteboardObjectJabberProvider ()
    {}

    /**
     * Parses a WhiteboardPacketExtension packet (extension sub-packet).
     *
     * @param parser an XML parser.
     * @return a new WhiteboardPacketExtension instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension (XmlPullParser parser)
        throws Exception
    {
        PacketExtension extension = null;

        StringBuilder sb = new StringBuilder ();
        boolean done = false;
        while (!done)
        {
            int eventType = parser.next ();

            if (eventType == XmlPullParser.START_TAG
                && !parser.getName ().equals (
                    WhiteboardObjectPacketExtension.ELEMENT_NAME)
                && !parser.getName ().equals (
                    WhiteboardSessionPacketExtension.ELEMENT_NAME))
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.TEXT)
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.END_TAG
                && parser.getName ().equals ("image"))
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.END_TAG
                && parser.getName ().equals ("text"))
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.END_TAG
                && parser.getName ().equals (
                  WhiteboardObjectPacketExtension.ELEMENT_NAME))
            {
                extension = new WhiteboardObjectPacketExtension(sb.toString ());
                done = true;
            }
            else if (eventType == XmlPullParser.END_TAG
                && parser.getName ().equals (
                WhiteboardSessionPacketExtension.ELEMENT_NAME))
            {
                extension = new WhiteboardSessionPacketExtension(sb.toString ());
                done = true;
            }
        }

        return extension;
    }
}
