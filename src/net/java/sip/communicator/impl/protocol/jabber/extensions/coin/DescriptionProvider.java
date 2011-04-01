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
