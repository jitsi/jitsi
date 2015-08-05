/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import org.ice4j.ice.*;
import org.ice4j.ice.sdp.*;
import org.jitsi.service.neomedia.*;

import javax.sdp.*;

/**
 * A {@link TransportManager} implementation for SIP that uses ICE for candidate
 * management.
 *
 * @author Emil Ivov
 */
public class IceTransportManagerSipImpl
    extends TransportManagerSipImpl
{

    /**
     * Ths ICE {@link Agent} that this transport manager is using for
     * connectivity establishment.
     */
    private Agent iceAgent = null;

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeerMediaHandler} whose traffic we
     * will be taking care of.
     */
    public IceTransportManagerSipImpl(CallPeerSipImpl callPeer)
    {
        super(callPeer);
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the
     * {@link #wrapupCandidateHarvest()} method which would be called once we
     * absolutely need the candidates.
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
        iceAgent = createIceAgent();
        iceAgent.setControlling(true);

        //obviously we ARE the controlling agent since we are the ones creating
        //the offer.
        iceAgent.setControlling(true);

        //add the candidate attributes and set default candidates
        for(MediaDescription mLine
                : SdpUtils.extractMediaDescriptions(ourOffer))
        {
            createIceStream(SdpUtils.getMediaType(mLine).toString(), iceAgent);
        }

        //now that our iceAgent is ready, reflect it on our offer.
        IceSdpUtils.initSessionDescription(ourOffer, iceAgent);
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the
     * {@link #wrapupCandidateHarvest()} method which would be called once we
     * absolutely need the candidates.
     *
     * @param trickleCallback the callback that will be taking care of
     * candidates that we discover asynchronously.
     * @param  isInitiator specifies whether we are the initiating party in this
     * call and hence must be the controlling ICE agent.
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public void startCandidateHarvest( SessionDescription theirOffer,
                                       SessionDescription ourAnswer,
                                       Object  trickleCallback,
                                      boolean isInitiator)
        throws OperationFailedException
    {
        iceAgent = createIceAgent();
        iceAgent.setControlling(false);
    }

    /**
     * Notifies the transport manager that it should conclude candidate
     * harvesting as soon as possible and return the lists of candidates
     * gathered so far.
     *
     * @return the content list that we received earlier (possibly cloned into
     * a new instance) and that we have updated with transport lists.
     */
    public List<Candidate<?>> wrapupCandidateHarvest()
    {
        return null;
    }

    /**
     * Starts the connectivity establishment of this
     * <tt>TransportManagerJabberImpl</tt> i.e. checks the connectivity between
     * the local and the remote peers given the remote counterpart of the
     * negotiation between them.
     *
     * @param remote the collection of {@link RemoteCandidate} s which
     * represents the remote counterpart of the negotiation between the local
     * and the remote peer
     * @return <tt>true</tt> if connectivity establishment has been started in
     * response to the call; otherwise, <tt>false</tt>.
     * <tt>TransportManager</tt> implementations which do not perform
     * connectivity checks (e.g. raw UDP) should return <tt>true</tt>. The
     * default implementation does not perform connectivity checks and always
     * returns <tt>true</tt>.
     */
    public boolean startConnectivityEstablishment(
            Iterable<RemoteCandidate> remote)
    {
        return true;
    }

    /**
     * Notifies this <tt>TransportManagerJabberImpl</tt> that it should conclude
     * any started connectivity establishment.
     *
     * @throws OperationFailedException if anything goes wrong with connectivity
     * establishment (i.e. ICE failed, ...)
     */
    public void wrapupConnectivityEstablishment()
        throws OperationFailedException
    {
    }

    /**
     * Releases the resources acquired by this <tt>TransportManager</tt> and
     * prepares it for garbage collection.
     */
    public void close()
    {
        for (MediaType mediaType : MediaType.values())
            closeStreamConnector(mediaType);
    }
}
