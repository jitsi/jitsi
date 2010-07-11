/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

/**
 * @author Emil Ivov
 */
public class CandidatePacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "candidate" element.
     */
    public static final String ELEMENT_NAME = "candidate";

    /**
     * Creates a new {@link CandidatePacketExtension}
     */
    public CandidatePacketExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Creates a new {@link CandidatePacketExtension} with the specified
     * <tt>elementName</tt> so that this class would be usable as a
     * <tt>RemoteCandidatePacketExtension</tt> parent.
     *
     * @param elementName the element name that this instance should be using.
     */
    protected CandidatePacketExtension(String elementName)
    {
        super(null, elementName);
    }
}
