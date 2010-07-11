/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;

/**
 * @author Emil Ivov
 */
public class RtpDescriptionPacketExtension
    implements PacketExtension
{
    /**
     * The name space for RTP description elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1";

    /**
     * The name of the "description" element.
     */
    public static final String ELEMENT_NAME = "description";

    /**
     * Returns the name of the <tt>description</tt> element.
     *
     * @return the name of the <tt>description</tt> element.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the namespace for the <tt>description</tt> element.
     *
     * @return the namespace for the <tt>description</tt> element.
     */
    public String getNamespace()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.smack.packet.PacketExtension#toXML()
     */
    public String toXML()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
