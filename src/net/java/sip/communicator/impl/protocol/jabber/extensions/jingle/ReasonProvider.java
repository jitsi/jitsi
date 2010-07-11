/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.packet.*;
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
    public ReasonPacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        String text = null;
        String reason = null;
        //ReasonPacketException reason
        //    = new ReasonPacketExtension(reason, text, packetExtension);

        boolean done = false;

        int eventType;
        String elementName;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                // the reason itself.
                if( reason = null)
                {
                    reason = element
                }
                if (elementName.equals(ContentPacketExtension.ELEMENT_NAME))
                {
                    ContentPacketExtension content
                        = contentProvider.parseExtension(parser);
                    jingleIQ.addContent(content);
                }
                // <reason/>
                if (elementName.equals(ReasonPacketExtension.ELEMENT_NAME))
                {
                    ReasonPacketExtension reason
                        = reasonProvider.parseExtension(parser);
                    jingleIQ.setReason(reason);
                }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(Jingle.getElementName()))
                {
                    done = true;
                }
            }
        }

        //return reason;
        return null;
    }
}
