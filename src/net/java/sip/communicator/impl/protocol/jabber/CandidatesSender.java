/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

/**
 * Represents functionality which allows a <tt>TransportManagerGTalkImpl</tt>
 * implementation to send <tt>candidates</tt> {@link SessionIQ}s for the
 * purposes of expediting candidate negotiation.
 *
 * @author Sebastien Vincent
 */
public interface CandidatesSender
{
    /**
     * Sends specific {@link CandidatePacketExtension}s in a <tt>candidates</tt>
     * {@link SessionIQ} from the local peer to the remote peer.
     *
     * @param candidates the <tt>CandidatePacketExtension</tt>s to be sent in a
     * <tt>candidates</tt> <tt>SessionIQ</tt> from the local peer to the
     * remote peer
     */
    public void sendCandidates(
            Iterable<GTalkCandidatePacketExtension> candidates);
}
