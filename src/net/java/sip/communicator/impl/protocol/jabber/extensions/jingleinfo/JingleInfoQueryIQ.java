/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import org.jivesoftware.smack.packet.*;

/**
 * The <tt>JingleInfoQueryIQ</tt> is used to discover STUN and relay server via
 * the Google's Jingle Server Discovery extension.
 *
 * @author Sebastien Vincent
 */
public class JingleInfoQueryIQ
    extends IQ
{
    /**
     * The namespace.
     */
    public static final String NAMESPACE = "google:jingleinfo";

    /**
     * The element name.
     */
    public static final String ELEMENT_NAME = "query";

    /**
     * Returns the sub-element XML section of the IQ packet, or null if
     * there isn't one. Packet extensions must be included, if any are defined.
     *
     * @return the child element section of the IQ XML.
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder bld = new StringBuilder();

        bld.append("<").append(ELEMENT_NAME).append(" xmlns='").
             append(NAMESPACE).append("'");

        if(getExtensions().size() == 0)
        {
            bld.append("/>");
        }
        else
        {
            bld.append(">");

            for(PacketExtension pe : getExtensions())
            {
                bld.append(pe.toXML());
            }

            bld.append("</").append(ELEMENT_NAME).append(">");
        }
        return bld.toString();
    }
}
