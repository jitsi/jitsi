/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Parser for src-id packet extension.
 *
 * @author Sebastien Vincent
 */
public class SrcIdProvider
    implements PacketExtensionProvider
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
        boolean done = false;
        int eventType;
        String ns = parser.getNamespace();

        SrcIdPacketExtension ext
            = new SrcIdPacketExtension(ns);

        while (!done)
        {
            eventType = parser.next();

            if (eventType == XmlPullParser.TEXT)
            {
                ext.setText(parser.getText());
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(
                        SrcIdPacketExtension.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return ext;
    }
}
