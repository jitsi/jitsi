/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;


/**
 * The KeepAliveEventProvider parses ping iq packets.
 *
 * @author Damian Minkov
 */
public class KeepAliveEventProvider
    implements IQProvider
{
    /**
     * Creates a new KeepAliveEventProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public KeepAliveEventProvider()
    {}

    /**
     * Parses a ping iq packet .
     *
     * @param parser an XML parser.
     * @return a new IQ instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        KeepAliveEvent result = new KeepAliveEvent();

        String type = parser.getAttributeValue(null, "type");
        String id = parser.getAttributeValue(null, "id");
        String from = parser.getAttributeValue(null, "from");
        String to = parser.getAttributeValue(null, "to");

        result.setType(IQ.Type.fromString(type));
        result.setPacketID(id);
        result.setFrom(from);
        result.setTo(to);

        return result;
    }
}
