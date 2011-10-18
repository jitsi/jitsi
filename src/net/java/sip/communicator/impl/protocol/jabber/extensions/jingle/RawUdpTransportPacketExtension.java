/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author Lyubomir Marinov
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
        return getCandidateList();
    }
}
