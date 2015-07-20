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
package net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Creates Smack packet extensions by parsing <replace /> tags
 * from incoming XMPP packets.
 *
 * @author Ivan Vergiliev
 */
public class MessageCorrectionExtensionProvider
    implements PacketExtensionProvider
{

    /**
     * Creates a new correction extension by parsing an XML element.
     *
     * @param parser An XML parser.
     * @return A new MesssageCorrectionExtension parsed from the XML.
     * @throws Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception
    {
        MessageCorrectionExtension res = new MessageCorrectionExtension(null);

        do
        {
            if (parser.getEventType() == XmlPullParser.START_TAG)
            {
                res.setCorrectedMessageUID(parser.getAttributeValue(
                        null, MessageCorrectionExtension.ID_ATTRIBUTE_NAME));
            }
        }
        while (parser.next() != XmlPullParser.END_TAG);

        return res;
    }
}
