/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;

import net.java.sip.communicator.service.protocol.media.*;

/**
 * @author Emil Ivov
 */
public class TransportManagerSipImpl extends TransportManager<CallPeerSipImpl>
{

    /**
     * Creates a new SIP {@link TransportManager}.
     *
     * @param callPeer the peer that we will be servicing.
     */
    protected TransportManagerSipImpl(CallPeerSipImpl callPeer)
    {
        super(callPeer);
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers. The method also takes into account
     * the existence of an outbound proxy and in that case returns its address
     * as the next hop.
     *
     * @param peer the CallPeer that we would contact.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    protected InetAddress getIntendedDestination(CallPeerSipImpl peer)
    {
        return peer.getProtocolProvider()
            .getIntendedDestination(peer.getPeerAddress()).getAddress();
    }

}
