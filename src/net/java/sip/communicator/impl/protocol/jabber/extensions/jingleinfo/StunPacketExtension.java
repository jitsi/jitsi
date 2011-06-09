/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Stun packet extension.
 *
 * @author Sebastien Vincent
 */
public class StunPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace.
     */
    public static final String NAMESPACE = null;

    /**
     * The element name.
     */
    public static final String ELEMENT_NAME = "stun";

    /**
     * Constructor.
     */
    public StunPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
