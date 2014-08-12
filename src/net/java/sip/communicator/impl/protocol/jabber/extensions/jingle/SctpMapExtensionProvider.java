/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;

/**
 * The <tt>SctpMapExtensionProvider</tt> parses "sctpmap" elements into
 * <tt>SctpMapExtension</tt> instances.
 * 
 * @author lishunyang
 * @see SctpMapExtension
 */
public class SctpMapExtensionProvider
    implements PacketExtensionProvider
{

    /**
     * {@inheritDoc}
     */
    @Override
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        SctpMapExtension result = new SctpMapExtension();

        if (parser.getName().equals(SctpMapExtension.ELEMENT_NAME)
            && parser.getNamespace().equals(SctpMapExtension.NAMESPACE))
        {
            result.setPort(Integer.parseInt(parser.getAttributeValue(null,
                SctpMapExtension.PORT_ATTR_NAME)));
            result.setProtocol(parser.getAttributeValue(null,
                SctpMapExtension.PROTOCOL_ATTR_NAME));
            result.setStreams(Integer.parseInt(parser.getAttributeValue(null,
                SctpMapExtension.STREAMS_ATTR_NAME)));
        }

        return result;
    }
}
