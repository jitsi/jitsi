/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * The provider that parses and <tt>content</tt> packet extensions into {@link
 * ContentPacketExtension} instances.
 *
 * @author Emil Ivov
 */
public class ContentProvider implements PacketExtensionProvider
{

    /**
     * Creates a new <tt>PacketExtensionProvider</tt>. {@link ProviderManager}
     * requires that every {@link PacketExtensionProvider} has a public,
     * no-argument constructor which is why we have this one.
     */
    public ContentProvider()
    {
    }

    /**
     * Parses and returns a Jingle <tt>content</tt> extension.
     *
     * @param parser the pull parser positioned at the content element.
     *
     * @return the newly created {@link ContentPacketExtension}.
     */
    public ContentPacketExtension parseExtension(final XmlPullParser parser)
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
