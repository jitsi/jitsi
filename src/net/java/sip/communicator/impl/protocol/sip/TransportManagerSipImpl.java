/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

import javax.sdp.*;

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
    @Override
    protected InetAddress getIntendedDestination(CallPeerSipImpl peer)
    {
        return peer.getProtocolProvider()
            .getIntendedDestination(peer.getPeerAddress()).getAddress();
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the
     * {@link #wrapupCandidateHarvest()} method which would be called once we
     * absolutely need the candidates.
     * <p>
     * This specific implementation does not do any sort of candidate harvesting
     * since it is working without ICE.
     *
     * @param ourOffer the SDP that should tell us how many stream connectors we
     * actually need.
     * @param trickleCallback the callback that will be taking care of
     * candidates that we discover asynchronously or <tt>null</tt> in case we
     * wouldn't won't to use trickle ICE (either because it is disabled or,
     * potentially, because we are doing half trickle).
     * @param advertiseTrickle indicates whether we should be including the
     * ice-options:trickle attribute in the SDP. Note that this parameter is
     * ignored and considered <tt>true</tt> if <tt>trickleCallback</tt> is not
     * <tt>null</tt>.
     * @param useBundle indicates whether or not we are using bundle.
     * @param advertiseBundle indicates whether or not we should be advertising
     * bundle to the remote party ( assumed as <tt>true</tt> in case
     * <tt>useBundle</tt> is already set to <tt>true</tt>).
     * @param useRtcpMux indicates whether or not we are using rtcp-mux.
     * @param advertiseRtcpMux indicates whether or not we should be advertising
     * rtcp-mux to the remote party ( assumed as <tt>true</tt> in case
     * <tt>useRtcpMux</tt> is already set to <tt>true</tt>).
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public void startCandidateHarvest(SessionDescription ourOffer,
                                      Object             trickleCallback,
                                      boolean            advertiseTrickle,
                                      boolean            useBundle,
                                      boolean            advertiseBundle,
                                      boolean            useRtcpMux,
                                      boolean            advertiseRtcpMux)
        throws OperationFailedException
    {

    }
}
