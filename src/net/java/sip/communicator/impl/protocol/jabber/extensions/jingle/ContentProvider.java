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
     *
     * @throws Exception if an error occurs parsing the XML.
     */
    public ContentPacketExtension parseExtension(final XmlPullParser parser)
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

        // Now let's try to parse the content in case we support the extensions
        // that it's transporting.
        boolean done = false;
        int eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG)
            {
                PacketExtensionProvider provider
                    = (PacketExtensionProvider)ProviderManager.getInstance()
                        .getExtensionProvider( elementName, namespace );

                if(provider == null)
                {
                    //we don't know how to handle this kind of extensions.
                    continue;
                }

                PacketExtension extension = provider.parseExtension(parser);
                content.addChildExtension(extension);
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(JingleIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }
        return content;
    }

}
