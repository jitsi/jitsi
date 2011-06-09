/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for StunPacketExtension.
 *
 * @author Sebastien Vincent
 */
public class StunProvider
    implements PacketExtensionProvider
{
    /**
     * Parses a users extension sub-packet and creates a {@link
     * StunPacketExtension} instance. At the beginning of the method
     * call, the xml parser will be positioned on the opening element of the
     * packet extension. As required by the smack API, at the end of the method
     * call, the parser will be positioned on the closing element of the packet
     * extension.
     *
     * @param parser an XML parser positioned at the opening
     * <tt>Server</tt> element.
     *
     * @return a new {@link StunPacketExtension} instance.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done = false;
        int eventType;
        String elementName = null;
        StunPacketExtension ext
            = new StunPacketExtension();

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                if(elementName.equals(ServerPacketExtension.ELEMENT_NAME))
                {
                    PacketExtensionProvider provider = (PacketExtensionProvider)
                        ProviderManager.getInstance().getExtensionProvider(
                                ServerPacketExtension.ELEMENT_NAME,
                                ServerPacketExtension.NAMESPACE);
                    PacketExtension childExtension =
                        provider.parseExtension(parser);
                    ext.addChildExtension(childExtension);
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        StunPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
