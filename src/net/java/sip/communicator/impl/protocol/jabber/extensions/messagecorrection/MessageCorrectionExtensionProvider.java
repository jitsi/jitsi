/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
