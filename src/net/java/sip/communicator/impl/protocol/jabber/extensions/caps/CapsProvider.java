/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;
/**
 * The provider that parses <tt>c</tt> packet extensions into {@link
 * CapsPacketExtension} instances.
 *
 * This work is based on Jonas Adahl's smack fork.
 *
 * @author Emil Ivov
 */
public class CapsProvider implements PacketExtensionProvider
{

    /**
     * Parses and returns an Entity Capabilities.
     *
     * @param parser the pull parser positioned at the caps element.
     *
     * @return the newly created {@link CapsPacketExtension}.
     *
     * @throws Exception in case there's anything wrong with the xml.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception
    {
        boolean done   = false;
        String ext     = null;
        String hash    = null;
        String version = null;
        String node    = null;

        while(!done)
        {
            if(parser.getEventType() == XmlPullParser.START_TAG
               && parser.getName().equalsIgnoreCase("c"))
            {
                ext = parser.getAttributeValue(null, "ext");
                hash = parser.getAttributeValue(null, "hash");
                version = parser.getAttributeValue(null, "ver");
                node = parser.getAttributeValue(null, "node");
            }

            if( parser.getEventType()==XmlPullParser.END_TAG
                && parser.getName().equalsIgnoreCase("c"))
            {
                done=true;
            }
            else
            {
                parser.next();
            }
        }

        return new CapsPacketExtension(ext, node, hash, version);
    }
}
