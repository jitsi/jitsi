/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Relay packet extension.
 *
 * @author Sebastien Vincent
 */
public class RelayPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace.
     */
    public static final String NAMESPACE = null;

    /**
     * The element name.
     */
    public static final String ELEMENT_NAME = "relay";

    /**
     * The token.
     */
    private String token = null;

    /**
     * Constructor.
     */
    public RelayPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Set the token.
     *
     * @param token token
     */
    public void setToken(String token)
    {
        this.token = token;
    }

    /**
     * Get the token.
     *
     * @return authentication token
     */
    public String getToken()
    {
        return token;
    }

    /**
     * Get an XML string representation.
     *
     * @return XML string representation
     */
    public String toXML()
    {
        StringBuilder bld = new StringBuilder();

        bld.append("<").append(ELEMENT_NAME).append(">");

        if(token != null)
        {
            bld.append("<").append("token").append(">");
            bld.append(token);
            bld.append("</").append("token").append(">");
        }

        for(PacketExtension pe : getChildExtensions())
        {
            bld.append(pe.toXML());
        }
        bld.append("</").append(ELEMENT_NAME).append(">");

        return bld.toString();
    }
}
