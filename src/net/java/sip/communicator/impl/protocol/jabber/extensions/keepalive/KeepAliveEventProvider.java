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
 * The KeepAliveEventProvider parses KeepAlive Event packets.
 *
 * @author Damian Minkov
 */
public class KeepAliveEventProvider
    implements IQProvider
{
    /**
     * Element name for keepalive.
     */
    public static final String ELEMENT_NAME = "keepalive";

    /**
     * Namespace for keepalive.
     */
    public static final String NAMESPACE = "jitsi:iq:keepalive";

    /**
     * Creates a new KeepAliveEventProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public KeepAliveEventProvider()
    {}

    /**
     * Parses a KeepAliveEvent packet .
     *
     * @param parser an XML parser.
     * @return a new IQ instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public IQ parseIQ(XmlPullParser parser)
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
                    if(parser.getName().equals(ELEMENT_NAME))
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
