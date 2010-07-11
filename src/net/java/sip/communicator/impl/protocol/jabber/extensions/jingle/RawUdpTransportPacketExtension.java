/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * An {@link AbstractPacketExtension} implementation for transport elements.
 *
 * @author Emil Ivov
 */
public class RawUdpTransportPacketExtension
    extends IceUdpTransportPacketExtension
{
    /**
     * The name of the "transport" element.
     */
    public static final String NAMESPACE
        = "urn:xmpp:jingle:transports:raw-udp:1";
    /**
     * The name of the "transport" element.
     */
    public static final String ELEMENT_NAME = "transport";

    /**
     * A list of one or more candidates representing each of the initiator's
     * higher-priority transport candidates as determined in accordance with
     * the ICE methodology.
     */
    private final List<CandidatePacketExtension> candidateList
        = new ArrayList<CandidatePacketExtension>();

    /**
     * Creates a new {@link RawUdpTransportPacketExtension} instance.
     */
    public RawUdpTransportPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Returns this element's child (local or remote) candidate elements.
     *
     * @return this element's child (local or remote) candidate elements.
     */
    @Override
    public List<? extends PacketExtension> getChildExtensions()
    {
        return candidateList;
    }

    /**
     * Adds <tt>candidate</tt> to the list of {@link CandidatePacketExtension}s
     * registered with this transport.
     *
     * @param candidate the new {@link CandidatePacketExtension} to add to this
     * transport element.
     */
    public void addCandidate(CandidatePacketExtension candidate)
    {
        this.candidateList.add(candidate);
    }

    /**
     * Returns the list of {@link CandidatePacketExtension}s currently
     * registered with this transport.
     *
     * @return the list of {@link CandidatePacketExtension}s currently
     * registered with this transport.
     */
    public List<CandidatePacketExtension> getCandidateList()
    {
        synchronized(candidateList)
        {
            return new ArrayList<CandidatePacketExtension>(this.candidateList);
        }
    }
}
