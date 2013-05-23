/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * A {@link TransportManagerJabberImpl} implementation that would only gather a
 * single candidate pair (i.e. RTP and RTCP).
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public class RawUdpTransportManager
    extends TransportManagerJabberImpl
{
    /**
     * The list of <tt>ContentPacketExtension</tt>s which represents the local
     * counterpart of the negotiation between the local and the remote peers.
     */
    private List<ContentPacketExtension> local;

    /**
     * The collection of <tt>ContentPacketExtension</tt>s which represents the
     * remote counterpart of the negotiation between the local and the remote
     * peers.
     */
    private final List<Iterable<ContentPacketExtension>> remotes
        = new LinkedList<Iterable<ContentPacketExtension>>();

    /**
     * The information pertaining to the Jisti VideoBridge conference which the
     * local peer represented by this instance is a focus of. It gives a view of
     * the whole Jitsi VideoBridge conference managed by the associated
     * <tt>CallJabberImpl</tt> which provides information specific to this
     * <tt>RawUdpTransportManager</tt> only.
     */
    private ColibriConferenceIQ colibri;

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeer} whose traffic we will be taking
     * care of.
     */
    public RawUdpTransportManager(CallPeerJabberImpl callPeer)
    {
        super(callPeer);
    }

    /**
     * Closes a specific <tt>StreamConnector</tt> associated with a specific
     * <tt>MediaType</tt>. If this <tt>TransportManager</tt> has a reference to
     * the specified <tt>streamConnector</tt>, it remains.
     * Also expires the <tt>ColibriConferenceIQ.Channel</tt> associated with
     * the closed <tt>StreamConnector</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> associated with the specified
     * <tt>streamConnector</tt>
     * @param streamConnector the <tt>StreamConnector</tt> to be closed
     */
    @Override
    protected void closeStreamConnector(
            MediaType mediaType,
            StreamConnector streamConnector)
    {
        try
        {
            boolean superCloseStreamConnector = true;

            if (streamConnector instanceof ColibriStreamConnector)
            {
                CallPeerJabberImpl peer = getCallPeer();

                if (peer != null)
                {
                    CallJabberImpl call = peer.getCall();

                    if (call != null)
                    {
                        superCloseStreamConnector = false;
                        call.closeColibriStreamConnector(
                            peer,
                            mediaType,
                            (ColibriStreamConnector) streamConnector);
                    }
                }
            }
            if (superCloseStreamConnector)
                super.closeStreamConnector(mediaType, streamConnector);
        }
        finally
        {
            /*
             * Expire the ColibriConferenceIQ.Channel associated with the closed
             * StreamConnector.
             */
            if (colibri != null)
            {
                ColibriConferenceIQ.Content content
                    = colibri.getContent(mediaType.toString());

                if (content != null)
                {
                    List<ColibriConferenceIQ.Channel> channels
                        = content.getChannels();

                    if (channels.size() == 2)
                    {
                        ColibriConferenceIQ requestConferenceIQ
                            = new ColibriConferenceIQ();

                        requestConferenceIQ.setID(colibri.getID());

                        ColibriConferenceIQ.Content requestContent
                            = requestConferenceIQ.getOrCreateContent(
                                    content.getName());

                        requestContent.addChannel(channels.get(1 /* remote */));

                        /*
                         * Regardless of whether the request to expire the
                         * Channel associated with mediaType succeeds, consider
                         * the Channel in question expired. Since
                         * RawUdpTransportManager allocates a single channel per
                         * MediaType, consider the whole Content expired.
                         */
                        colibri.removeContent(content);

                        CallPeerJabberImpl peer = getCallPeer();

                        if (peer != null)
                        {
                            CallJabberImpl call = peer.getCall();

                            if (call != null)
                                call.expireColibriChannels(
                                    peer,
                                    requestConferenceIQ);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a media <tt>StreamConnector</tt> for a stream of a specific
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the stream for which a
     * <tt>StreamConnector</tt> is to be created
     * @return a <tt>StreamConnector</tt> for the stream of the specified
     * <tt>mediaType</tt>
     * @throws OperationFailedException if the binding of the sockets fails
     */
    @Override
    protected StreamConnector createStreamConnector(final MediaType mediaType)
        throws OperationFailedException
    {
        ColibriConferenceIQ.Channel channel
                                        = getColibriChannel(mediaType, true);

        if (channel != null)
        {
            CallPeerJabberImpl peer = getCallPeer();
            CallJabberImpl call = peer.getCall();
            StreamConnector streamConnector
                = call.createColibriStreamConnector(
                peer,
                mediaType,
                channel,
                new StreamConnectorFactory()
                {
                    public StreamConnector createStreamConnector()
                    {
                        try
                        {
                            return
                                RawUdpTransportManager
                                    .super
                                    .createStreamConnector(
                                        mediaType);
                        }
                        catch (OperationFailedException ofe)
                        {
                            return null;
                        }
                    }
                });

            if (streamConnector != null)
                return streamConnector;
        }

        return super.createStreamConnector(mediaType);
    }

    /**
     * Creates a raw UDP transport element according to the specified stream
     * <tt>connector</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> which
     * uses the specified <tt>connector</tt>
     * @param connector the connector that we'd like to describe within the
     * transport element.
     *
     * @return a {@link RawUdpTransportPacketExtension} containing the RTP and
     * RTCP candidates of the specified {@link StreamConnector}.
     */
    private RawUdpTransportPacketExtension createTransport(
            MediaType mediaType,
            StreamConnector connector)
    {
        ColibriConferenceIQ.Channel channel = getColibriChannel(mediaType,
            false);

        RawUdpTransportPacketExtension ourTransport
            = new RawUdpTransportPacketExtension();
        int generation = getCurrentGeneration();

        // create and add candidates that correspond to the stream connector
        // RTP
        CandidatePacketExtension rtpCand = new CandidatePacketExtension();

        rtpCand.setComponent(CandidatePacketExtension.RTP_COMPONENT_ID);
        rtpCand.setGeneration(generation);
        rtpCand.setID(getNextID());
        rtpCand.setType(CandidateType.host);

        if (channel == null)
        {
            DatagramSocket dataSocket = connector.getDataSocket();

            rtpCand.setIP(dataSocket.getLocalAddress().getHostAddress());
            rtpCand.setPort(dataSocket.getLocalPort());
        }
        else
        {
            rtpCand.setIP(channel.getHost());
            rtpCand.setPort(channel.getRTPPort());
        }

        ourTransport.addCandidate(rtpCand);

        // RTCP
        CandidatePacketExtension rtcpCand = new CandidatePacketExtension();

        rtcpCand.setComponent(CandidatePacketExtension.RTCP_COMPONENT_ID);
        rtcpCand.setGeneration(generation);
        rtcpCand.setID(getNextID());
        rtcpCand.setType(CandidateType.host);

        if (channel == null)
        {
            DatagramSocket controlSocket = connector.getControlSocket();

            rtcpCand.setIP(controlSocket.getLocalAddress().getHostAddress());
            rtcpCand.setPort(controlSocket.getLocalPort());
        }
        else
        {
            rtcpCand.setIP(channel.getHost());
            rtcpCand.setPort(channel.getRTCPPort());
        }

        ourTransport.addCandidate(rtcpCand);

        return ourTransport;
    }

    /**
     * Implements {@link TransportManagerJabberImpl#getStreamTarget(MediaType)}.
     * Gets the <tt>MediaStreamTarget</tt> to be used as the <tt>target</tt> of
     * the <tt>MediaStream</tt> with a specific <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> which
     * is to have its <tt>target</tt> set to the returned
     * <tt>MediaStreamTarget</tt>
     * @return the <tt>MediaStreamTarget</tt> to be used as the <tt>target</tt>
     * of the <tt>MediaStream</tt> with the specified <tt>MediaType</tt>
     * @see TransportManagerJabberImpl#getStreamTarget(MediaType)
     */
    @Override
    public MediaStreamTarget getStreamTarget(MediaType mediaType)
    {
        MediaStreamTarget streamTarget = null;

        for (Iterable<ContentPacketExtension> remote : remotes)
        {
            for (ContentPacketExtension content : remote)
            {
                RtpDescriptionPacketExtension rtpDescription
                    = content.getFirstChildOfType(
                            RtpDescriptionPacketExtension.class);
                MediaType contentMediaType
                    = MediaType.parseString(rtpDescription.getMedia());

                if (mediaType.equals(contentMediaType))
                {
                    ColibriConferenceIQ.Channel channel
                        = getColibriChannel(mediaType, true);

                    if (channel == null)
                    {
                        streamTarget
                            = JingleUtils.extractDefaultTarget(content);
                    }
                    else
                    {
                        streamTarget
                            = new MediaStreamTarget(
                                    new InetSocketAddress(
                                            channel.getHost(),
                                            channel.getRTPPort()),
                                    new InetSocketAddress(
                                            channel.getHost(),
                                            channel.getRTCPPort()));
                    }

                    break;
                }
            }
        }
        return streamTarget;
    }

    /**
     * Gets the {@link ColibriConferenceIQ.Channel} which belongs to a content
     * associated with a specific <tt>MediaType</tt> and is to be either locally
     * or remotely used.
     * <p>
     * <b>Note</b>: Modifications to the <tt>ColibriConferenceIQ.Channel</tt>
     * instance returned by the method propagate to (the state of) this
     * instance.
     * </p>
     *
     * @param mediaType the <tt>MediaType</tt> associated with the content which
     * contains the <tt>ColibriConferenceIQ.Channel</tt> to get
     * @param local <tt>true</tt> if the <tt>ColibriConferenceIQ.Channel</tt>
     * which is to be used locally is to be returned or <tt>false</tt> for the
     * one which is to be used remotely
     * @return the <tt>ColibriConferenceIQ.Channel</tt> which belongs to a content
     * associated with the specified <tt>mediaType</tt> and which is to be used
     * in accord with the specified <tt>local</tt> indicator if such a channel
     * exists; otherwise, <tt>null</tt>
     */
    ColibriConferenceIQ.Channel getColibriChannel(
        MediaType mediaType,
        boolean local)
    {
        ColibriConferenceIQ.Channel channel = null;

        if (colibri != null)
        {
            ColibriConferenceIQ.Content content
                = colibri.getContent(mediaType.toString());

            if (content != null)
            {
                List<ColibriConferenceIQ.Channel> channels
                    = content.getChannels();

                if (channels.size() == 2)
                    channel = channels.get(local ? 0 : 1);
            }
        }

        return channel;
    }

    /**
     * Implements {@link TransportManagerJabberImpl#getXmlNamespace()}. Gets the
     * XML namespace of the Jingle transport implemented by this
     * <tt>TransportManagerJabberImpl</tt>.
     *
     * @return the XML namespace of the Jingle transport implemented by this
     * <tt>TransportManagerJabberImpl</tt>
     * @see TransportManagerJabberImpl#getXmlNamespace()
     */
    @Override
    public String getXmlNamespace()
    {
        return ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RAW_UDP_0;
    }

    /**
     * Removes a content with a specific name from the transport-related part of
     * the session represented by this <tt>TransportManagerJabberImpl</tt> which
     * may have been reported through previous calls to the
     * <tt>startCandidateHarvest</tt> and
     * <tt>startConnectivityEstablishment</tt> methods.
     *
     * @param name the name of the content to be removed from the
     * transport-related part of the session represented by this
     * <tt>TransportManagerJabberImpl</tt>
     * @see TransportManagerJabberImpl#removeContent(String)
     */
    @Override
    public void removeContent(String name)
    {
        if (local != null)
            removeContent(local, name);

        removeRemoteContent(name);
    }

    /**
     * Removes a content with a specific name from the remote counterpart of the
     * negotiation between the local and the remote peers.
     *
     * @param name the name of the content to be removed from the remote
     * counterpart of the negotiation between the local and the remote peers
     */
    private void removeRemoteContent(String name)
    {
        for (Iterator<Iterable<ContentPacketExtension>> remoteIter
                    = remotes.iterator();
                remoteIter.hasNext();)
        {
            Iterable<ContentPacketExtension> remote = remoteIter.next();

            /*
             * Once the remote content is removed, make sure that we are not
             * retaining sets which do not have any contents.
             */
            if ((removeContent(remote, name) != null)
                    && !remote.iterator().hasNext())
            {
                remoteIter.remove();
            }
        }
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the
     * {@link #wrapupCandidateHarvest()} method which would be called once we
     * absolutely need the candidates.
     *
     * @param ourOffer the content list that should tell us how many stream
     * connectors we actually need.
     * @param transportInfoSender the <tt>TransportInfoSender</tt> to be used by
     * this <tt>TransportManagerJabberImpl</tt> to send <tt>transport-info</tt>
     * <tt>JingleIQ</tt>s from the local peer to the remote peer if this
     * <tt>TransportManagerJabberImpl</tt> wishes to utilize
     * <tt>transport-info</tt>. Local candidate addresses sent by this
     * <tt>TransportManagerJabberImpl</tt> in <tt>transport-info</tt> are
     * expected to not be included in the result of
     * {@link #wrapupCandidateHarvest()}.
     * @throws OperationFailedException in case we fail allocating ports
     */
    @Override
    public void startCandidateHarvest(
            List<ContentPacketExtension> ourOffer,
            TransportInfoSender transportInfoSender)
        throws OperationFailedException
    {
        startCandidateHarvest(null, ourOffer, transportInfoSender);
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the
     * {@link #wrapupCandidateHarvest()} method which would be called once we
     * absolutely need the candidates.
     *
     * @param theirOffer a media description offer that we've received from the
     * remote party and that we should use in case we need to know what
     * transports our peer is using.
     * @param ourAnswer the content descriptions that we should be adding our
     * transport lists to (although not necessarily in this very instance).
     * @param transportInfoSender the <tt>TransportInfoSender</tt> to be used by
     * this <tt>TransportManagerJabberImpl</tt> to send <tt>transport-info</tt>
     * <tt>JingleIQ</tt>s from the local peer to the remote peer if this
     * <tt>TransportManagerJabberImpl</tt> wishes to utilize
     * <tt>transport-info</tt>. Local candidate addresses sent by this
     * <tt>TransportManagerJabberImpl</tt> in <tt>transport-info</tt> are
     * expected to not be included in the result of
     * {@link #wrapupCandidateHarvest()}.
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     * @see TransportManagerJabberImpl#startCandidateHarvest(List, List,
     * TransportInfoSender)
     */
    @Override
    public void startCandidateHarvest(
            List<ContentPacketExtension> theirOffer,
            List<ContentPacketExtension> ourAnswer,
            TransportInfoSender transportInfoSender)
        throws OperationFailedException
    {
        CallPeerJabberImpl peer = getCallPeer();
        CallJabberImpl call = peer.getCall();
        List<ContentPacketExtension> cpes
            = (theirOffer == null) ? ourAnswer : theirOffer;

        /*
         * If Jitsi VideoBridge is to be used, determine which channels are to
         * be allocated and attempt to allocate them now.
         */
        if (call.getConference().isJitsiVideoBridge())
        {
            List<RtpDescriptionPacketExtension> descriptions
                = new ArrayList<RtpDescriptionPacketExtension>();

            for (ContentPacketExtension cpe : cpes)
            {
                RtpDescriptionPacketExtension rtpDesc
                    = cpe.getFirstChildOfType(
                            RtpDescriptionPacketExtension.class);
                MediaType mediaType = MediaType.parseString(rtpDesc.getMedia());

                /*
                 * The existence of a content for the mediaType and regardless
                 * of the existence of channels in it signals that a channel
                 * allocation request has already been sent for that mediaType.
                 */
                if ((colibri == null)
                        || (colibri.getContent(mediaType.toString()) == null))
                {
                    if (!descriptions.contains(rtpDesc))
                        descriptions.add(rtpDesc);
                }
            }
            if (!descriptions.isEmpty())
            {
                /*
                 * We are about to request the channel allocations for the
                 * media types found in 'descriptions'. Regardless of the
                 * response, we do not want to repeat these requests.
                 */
                if (colibri == null)
                    colibri = new ColibriConferenceIQ();
                for (RtpDescriptionPacketExtension description : descriptions)
                    colibri.getOrCreateContent(description.getMedia());

                ColibriConferenceIQ conferenceResult
                    = call.createColibriChannels(peer, descriptions);

                if (conferenceResult != null)
                {
                    String videoBridgeID = colibri.getID();
                    String conferenceResultID = conferenceResult.getID();

                    if (videoBridgeID == null)
                        colibri.setID(conferenceResultID);
                    else if (!videoBridgeID.equals(conferenceResultID))
                        throw new IllegalStateException("conference.id");

                    for (ColibriConferenceIQ.Content contentResult
                            : conferenceResult.getContents())
                    {
                        ColibriConferenceIQ.Content content
                            = colibri.getOrCreateContent(
                                    contentResult.getName());

                        for (ColibriConferenceIQ.Channel channelResult
                                : contentResult.getChannels())
                        {
                            if (content.getChannel(channelResult.getID())
                                    == null)
                                content.addChannel(channelResult);
                        }
                    }
                }
                else
                {
                    /*
                     * The call fails if createColibriChannels method fails
                     * this can happen if the conference packet timeouts or
                     * it can't be build.
                     */
                    ProtocolProviderServiceJabberImpl
                        .throwOperationFailedException(
                            "Failed to allocate colibri channel.",
                            OperationFailedException.GENERAL_ERROR,
                            null,
                            Logger.getLogger(
                                RawUdpTransportManager.class));
                }
            }
        }

        /*
         * RawUdpTransportManager#startCandidateHarvest(
         * List<ContentPacketExtension>, TransportInfoSender) delegates here
         * because the implementations are pretty much identical and it's just
         * that there's no theirOffer and ourAnswer is in fact our offer to
         * which their answer is expected.
         */
        for (ContentPacketExtension cpe : cpes)
        {
            RtpDescriptionPacketExtension rtpDesc
                = cpe.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);
            MediaType mediaType = MediaType.parseString(rtpDesc.getMedia());
            StreamConnector connector = getStreamConnector(mediaType);
            RawUdpTransportPacketExtension ourTransport
                = createTransport(mediaType, connector);

            //now add our transport to our answer
            ContentPacketExtension ourCpe
                = findContentByName(ourAnswer, cpe.getName());

            //it might be that we decided not to reply to this content
            if (ourCpe != null)
                ourCpe.addChildExtension(ourTransport);
        }

        this.local = ourAnswer;
    }

    /**
     * Overrides
     * <tt>TransportManagerJabberImpl#startConnectivityEstablishment(Iterable&lt;ContentPacketExtension&gt;)</tt>
     * in order to remember the remote counterpart of the negotiation between
     * the local and the remote peer for subsequent calls to
     * {@link #getStreamTarget(MediaType)}.
     *
     * @param remote the collection of <tt>ContentPacketExtension</tt>s which
     * represents the remote counterpart of the negotiation between the local
     * and the remote peer
     * @return <tt>true</tt> because <tt>RawUdpTransportManager</tt> does not
     * perform connectivity checks
     * @see TransportManagerJabberImpl#startConnectivityEstablishment(Iterable)
     */
    @Override
    public boolean startConnectivityEstablishment(
            Iterable<ContentPacketExtension> remote)
    {
        if ((remote != null) && !remotes.contains(remote))
        {
            /*
             * The state of the session in Jingle is maintained by each peer and
             * is modified by content-add and content-remove. The remotes field
             * of this RawUdpTransportManager represents the state of the
             * session with respect to the remote peer. When the remote peer
             * tells us about a specific set of contents, make sure that it is
             * the only record we will have with respect to the specified set of
             * contents.
             */
            for (ContentPacketExtension content : remote)
                removeRemoteContent(content.getName());

            remotes.add(remote);
        }

        return super.startConnectivityEstablishment(remote);
    }

    /**
     * Simply returns the list of local candidates that we gathered during the
     * harvest. This is a raw UDP transport manager so there's no real wrapping
     * up to do.
     *
     * @return the list of local candidates that we gathered during the harvest
     * @see TransportManagerJabberImpl#wrapupCandidateHarvest()
     */
    @Override
    public List<ContentPacketExtension> wrapupCandidateHarvest()
    {
        return local;
    }

    /**
     * Returns the extended type of the candidate selected if this transport
     * manager is using ICE.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return The extended type of the candidate selected if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    @Override
    public String getICECandidateExtendedType(String streamName)
    {
        return null;
    }

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing.
     */
    @Override
    public String getICEState()
    {
        return null;
    }

    /**
     * Returns the ICE local host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    @Override
    public InetSocketAddress getICELocalHostAddress(String streamName)
    {
        return null;
    }

    /**
     * Returns the ICE remote host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    @Override
    public InetSocketAddress getICERemoteHostAddress(String streamName)
    {
        return null;
    }

    /**
     * Returns the ICE local reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * local candidate used.
     */
    @Override
    public InetSocketAddress getICELocalReflexiveAddress(String streamName)
    {
        return null;
    }

    /**
     * Returns the ICE remote reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * remote candidate used.
     */
    @Override
    public InetSocketAddress getICERemoteReflexiveAddress(String streamName)
    {
        return null;
    }

    /**
     * Returns the ICE local relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * local candidate used.
     */
    @Override
    public InetSocketAddress getICELocalRelayedAddress(String streamName)
    {
        return null;
    }

    /**
     * Returns the ICE remote relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * remote candidate used.
     */
    @Override
    public InetSocketAddress getICERemoteRelayedAddress(String streamName)
    {
        return null;
    }

    /**
     * Returns the total harvesting time (in ms) for all harvesters.
     *
     * @return The total harvesting time (in ms) for all the harvesters.  0 if
     * the ICE agent is null, or if the agent has nevers harvested.
     */
    @Override
    public long getTotalHarvestingTime()
    {
        return 0;
    }

    /**
     * Returns the harvesting time (in ms) for the harvester given in parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The harvesting time (in ms) for the harvester given in parameter.
     * 0 if this harvester does not exists, if the ICE agent is null, or if the
     * agent has never harvested with this harvester.
     */
    @Override
    public long getHarvestingTime(String harvesterName)
    {
        return 0;
    }

    /**
     * Returns the number of harvesting for this agent.
     *
     * @return The number of harvesting for this agent.
     */
    @Override
    public int getNbHarvesting()
    {
        return 0;
    }

    /**
     * Returns the number of harvesting time for the harvester given in
     * parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The number of harvesting time for the harvester given in
     * parameter.
     */
    @Override
    public int getNbHarvesting(String harvesterName)
    {
        return 0;
    }
}
