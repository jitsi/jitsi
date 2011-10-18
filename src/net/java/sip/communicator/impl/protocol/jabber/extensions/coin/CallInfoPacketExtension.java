/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Call information packet extension.
 *
 * @author Sebastien Vincent
 */
public class CallInfoPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace that call info belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the element that contains the call info.
     */
    public static final String ELEMENT_NAME = "call-info";

    /**
     * Constructor.
     */
    public CallInfoPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
