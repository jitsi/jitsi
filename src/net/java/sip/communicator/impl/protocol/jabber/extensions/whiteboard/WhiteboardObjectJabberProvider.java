/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * This class parses incoming messages and extracts the whiteboard parameters
 * from the raw XML messages.
 *
 * @author Julien Waechter
 */
public class WhiteboardObjectJabberProvider
    implements PacketExtensionProvider
{
    /**
     * Creates a new WhiteboardObjectJabberProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public WhiteboardObjectJabberProvider ()
    {}

    /**
     * Parses a WhiteboardPacketExtension packet (extension sub-packet).
     *
     * @param parser an XML parser.
     * @return a new WhiteboardPacketExtension instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public PacketExtension parseExtension (XmlPullParser parser)
        throws Exception
    {
        StringBuilder sb = new StringBuilder ();
        boolean done = false;
        while (!done)
        {
            int eventType = parser.next ();
            if (eventType == XmlPullParser.START_TAG &&
              !parser.getName ().equals (
              WhiteboardObjectPacketExtensionImpl.ELEMENT_NAME))
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.TEXT)
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.END_TAG &&
              parser.getName ().equals ("image"))
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.END_TAG &&
              parser.getName ().equals ("text"))
            {
                sb.append (parser.getText ());
            }
            else if (eventType == XmlPullParser.END_TAG &&
              parser.getName ().equals (
              WhiteboardObjectPacketExtensionImpl.ELEMENT_NAME))
            {
                done = true;
            }
        }
        WhiteboardObjectPacketExtensionImpl wbo
            = new WhiteboardObjectPacketExtensionImpl(sb.toString ());

        return wbo;
    }
}