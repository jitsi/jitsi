/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * The KeepAliveEventProvider parses KeepAlive Event packets.
 *
 * @author Damian Minkov
 */
public class KeepAliveEventProvider
    implements PacketExtensionProvider
{
    public static final String ELEMENT_NAME = "x";
    public static final String NAMESPACE = "sip-communicator:x:keepalive";

    /**
     * Creates a new KeepAliveEventProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public KeepAliveEventProvider()
    {}

    /**
     * Parses a KeepAliveEvent packet (extension sub-packet).
     *
     * @param parser an XML parser.
     * @return a new IQ instance.
     * @throws Exception if an error occurs parsing the XML.
     * @todo Implement this
     *   org.jivesoftware.smack.provider.PacketExtensionProvider method
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        KeepAliveEvent result = new KeepAliveEvent();

        boolean done = false;
        while (!done)
        {
            try
            {
                int eventType = parser.next();
                if(eventType == XmlPullParser.START_TAG)
                {
                    if(parser.getName().equals(KeepAliveEvent.
                                               SOURCE_PROVIDER_HASH))
                    {
                        result.setSrcProviderHash(Integer.parseInt(parser.
                            nextText()));
                    }
                    if(parser.getName().equals(KeepAliveEvent.SOURCE_OPSET_HASH))
                    {
                        result.setSrcOpSetHash(Integer.parseInt(parser.nextText()));
                    }
                }
                else if(eventType == XmlPullParser.END_TAG)
                {
                    if(parser.getName().equals("x"))
                    {
                        done = true;
                    }
                }
            }
            catch(NumberFormatException ex)
            {
                ex.printStackTrace();
            }
        }

        return result;
    }
}
