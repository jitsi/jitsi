/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Represents the content <tt>inputevt</tt> element that may be find in
 * <tt>content</tt> part of a Jingle media negociation.
 *
 * @author Sebastien Vincent
 */
public class InputEvtPacketExtension extends AbstractPacketExtension
{
    /**
     * Name of the XML element representing the extension.
     */
    public final static String ELEMENT_NAME = "inputevt";

    /**
     * Namespace..
     */
    public final static String NAMESPACE =
        "http://jitsi.org/protocol/inputevt";

    /**
     * Constructs a new <tt>inputevt</tt> extension.
     */
    public InputEvtPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }
}
