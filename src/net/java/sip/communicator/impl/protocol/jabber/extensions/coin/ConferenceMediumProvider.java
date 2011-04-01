/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for ConferenceMediumProvider.
 *
 * @author Sebastien Vincent
 */
public class ConferenceMediumProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a ConferenceMedium extension sub-packet and creates a {@link
     * ConferenceMediumPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>ConferenceMedium</tt> element.
     *
     * @return a new {@link ConferenceMediumPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;
        String label = parser.getAttributeValue(
                "",
                ConferenceMediumPacketExtension.LABEL_ATTR_NAME);

        if(label == null)
        {
            throw new Exception(
                    "Coin medium element must contain entity attribute");
        }

        ConferenceMediumPacketExtension ext =
            new ConferenceMediumPacketExtension("entry", label);

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(
                        MediaPacketExtension.ELEMENT_DISPLAY_TEXT))
                {
                    ext.setDisplayText(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        MediaPacketExtension.ELEMENT_STATUS))
                {
                    ext.setStatus(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        MediaPacketExtension.ELEMENT_TYPE))
                {
                    ext.setType(CoinIQProvider.parseText(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        ConferenceMediumPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
