/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;

/**
 * The element containing details about an encryption algorithm that could be
 * used during a jingle session.
 *
 * @author Emil Ivov
 */
public class CryptoPacketExtension
    implements PacketExtension
{
    /**
     * There's no namespace for the <tt>crypto</tt> element itself.
     */
    public static final String NAMESPACE = null;

    /**
     * The name of the "crypto" element.
     */
    public static final String ELEMENT_NAME = "crypto";

    /**
     * Returns the name of the <tt>encryption</tt> element.
     *
     * @return the name of the <tt>encryption</tt> element.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns <tt>null</tt> since there's no encryption specific ns.
     *
     * @return <tt>null</tt> since there's no encryption specific ns.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of this <tt>description</tt> packet
     * extension including all child elements.
     *
     * @return this packet extension as an XML <tt>String</tt>.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder(
            "<" + ELEMENT_NAME+ " ");


        bldr.append("</" + ELEMENT_NAME + ">");
        return bldr.toString();
    }

}
