/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

/**
 * A representation of the <tt>remote-candidate</tt> ICE transport element.
 *
 * @author Emil Ivov
 */
public class RemoteCandidatePacketExtension extends CandidatePacketExtension
{
    /**
     * The name of the "candidate" element.
     */
    public static final String ELEMENT_NAME = "remote-candidate";

    /**
     * Creates a new {@link CandidatePacketExtension}
     */
    public RemoteCandidatePacketExtension()
    {
        super(ELEMENT_NAME);
    }
}
