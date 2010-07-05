/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * The <tt>ReasonProvider</tt> parses "reason" elements into {@link
 * ReasonPacketExtension} instances.
 *
 * @author Emil Ivov
 */
public class ReasonProvider implements PacketExtensionProvider
{

    /**
     * Parses a reason extension sub-packet and creates a {@link
     * ReasonPacketExtension} instance. At the beginning of the method call,
     * the xml parser will be positioned on the opening element of the packet
     * extension. As required by the smack API, at the end of the method call,
     * the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening <tt>reason</tt>
     * element.
     *
     * @return a new {@link ReasonPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        String creator = parser.getAttributeValue("",
                        ContentPacketExtension.CREATOR_ARG_NAME);
        String disposition = parser.getAttributeValue("",
                        ContentPacketExtension.DISPOSITION_ARG_NAME);
        String name = parser.getAttributeValue("",
                        ContentPacketExtension.NAME_ARG_NAME);
        String sendersStr = parser.getAttributeValue(
                            "", ContentPacketExtension.SENDERS_ARG_NAME);

        ContentPacketExtension.SendersEnum senders = null;
        if(sendersStr != null && sendersStr.trim().length() > 0)
            senders = ContentPacketExtension.SendersEnum.valueOf(sendersStr);

        // Try to get an Audio content info
        ContentPacketExtension content = new ContentPacketExtension(
                        ContentPacketExtension.CreatorEnum.valueOf(creator),
                        disposition,
                        name,
                        senders);

        return content;
    }
}
