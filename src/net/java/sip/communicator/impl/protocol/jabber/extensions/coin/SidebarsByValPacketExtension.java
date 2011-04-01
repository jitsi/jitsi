/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Sidebars by val packet extension.
 *
 * @author Sebastien Vincent
 */
public class SidebarsByValPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that sidebars by val belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the sidebars by val.
     */
    public static final String ELEMENT_NAME = "sidebars-by-val";

    /**
     * Constructor.
     */
    public SidebarsByValPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
