/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Implements <tt>AbstractPacketExtension</tt> for the "transferred" element
 * defined by XEP-0251: Jingle Session Transfer.
 *
 * @author Lyubomir Marinov
 */
public class TransferredPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the "transfer" element.
     */
    public static final String ELEMENT_NAME = "transferred";

    /**
     * The namespace of the "transfer" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:transfer:0";

    /**
     * Initializes a new <tt>TransferredPacketExtension</tt> instance.
     */
    public TransferredPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
