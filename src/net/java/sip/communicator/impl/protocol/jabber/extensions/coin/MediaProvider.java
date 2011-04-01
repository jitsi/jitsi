/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for MediaPacketExtension.
 *
 * @author Sebastien Vincent
 */
public class MediaProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a media extension sub-packet and creates a {@link
     * MediaPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>Media</tt> element.
     *
     * @return a new {@link MediaPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public MediaPacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;
        String id = parser.getAttributeValue(
                "",
                MediaPacketExtension.ID_ATTR_NAME);

        if(id == null)
        {
            throw new Exception("Coin media must contains src-id element");
        }

        MediaPacketExtension ext
            = new MediaPacketExtension(id);

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
                        MediaPacketExtension.ELEMENT_LABEL))
                {
                    ext.setLabel(CoinIQProvider.parseText(parser));
                }
                else if(elementName.equals(
                        MediaPacketExtension.ELEMENT_SRC_ID))
                {
                    ext.setSrcID(CoinIQProvider.parseText(parser));
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
                        MediaPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
