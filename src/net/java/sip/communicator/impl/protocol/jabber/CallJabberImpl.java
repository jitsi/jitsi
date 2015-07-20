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
package net.java.sip.communicator.impl.protocol.jabber;

import java.lang.ref.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;

/**
 * A Jabber implementation of the <tt>Call</tt> abstract class encapsulating
 * Jabber jingle sessions.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class CallJabberImpl
    extends AbstractCallJabberGTalkImpl<CallPeerJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(CallJabberImpl.class);

    /**
     * The Jitsi Videobridge conference which the local peer represented by this
     * instance is a focus of.
     */
    private ColibriConferenceIQ colibri;

    /**
     * The shared <tt>CallPeerMediaHandler</tt> state which is to be used by the
     * <tt>CallPeer</tt>s of this <tt>Call</tt> which use {@link #colibri}.
     */
    private MediaHandler colibriMediaHandler;

    /**
     * Contains one ColibriStreamConnector for each <tt>MediaType</tt>
     */
    private final List<WeakReference<ColibriStreamConnector>>
        colibriStreamConnectors;

    /**
     * The entity ID of the Jitsi Videobridge to be utilized by this
     * <tt>Call</tt> for the purposes of establishing a server-assisted
     * telephony conference.
     */
    private String jitsiVideobridge;

    /**
     * Initializes a new <tt>CallJabberImpl</tt> instance.
     *
     * @param parentOpSet the {@link OperationSetBasicTelephonyJabberImpl}
     * instance in the context of which this call has been created.
     */
    protected CallJabberImpl(
            OperationSetBasicTelephonyJabberImpl parentOpSet)
    {
        super(parentOpSet);

        int mediaTypeValueCount = MediaType.values().length;

        colibriStreamConnectors
            = new ArrayList<WeakReference<ColibriStreamConnector>>(
                    mediaTypeValueCount);
        for (int i = 0; i < mediaTypeValueCount; i++)
            colibriStreamConnectors.add(null);

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);
    }

    /**
     * Closes a specific <tt>ColibriStreamConnector</tt> which is associated with
     * a <tt>MediaStream</tt> of a specific <tt>MediaType</tt> upon request from
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> which requests the closing of the
     * specified <tt>colibriStreamConnector</tt>
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> with
     * which the specified <tt>colibriStreamConnector</tt> is associated
     * @param colibriStreamConnector the <tt>ColibriStreamConnector</tt> to close on
     * behalf of the specified <tt>peer</tt>
     */
    public void closeColibriStreamConnector(
            CallPeerJabberImpl peer,
            MediaType mediaType,
            ColibriStreamConnector colibriStreamConnector)
    {
        colibriStreamConnector.close();
        synchronized (colibriStreamConnectors)
        {
            int index = mediaType.ordinal();
            WeakReference<ColibriStreamConnector> weakReference
                    = colibriStreamConnectors.get(index);
            if (weakReference != null && colibriStreamConnector
                    .equals(weakReference.get()))
            {
                colibriStreamConnectors.set(index, null);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Sends a <tt>content</tt> message to each of the <tt>CallPeer</tt>s
     * associated with this <tt>CallJabberImpl</tt> in order to include/exclude
     * the &quot;isfocus&quot; attribute.
     */
    @Override
    protected void conferenceFocusChanged(boolean oldValue, boolean newValue)
    {
        try
        {
            Iterator<CallPeerJabberImpl> peers = getCallPeers();

            while (peers.hasNext())
            {
                CallPeerJabberImpl callPeer = peers.next();

                if (callPeer.getState() == CallPeerState.CONNECTED)
                    callPeer.sendCoinSessionInfo();
            }
        }
        finally
        {
            super.conferenceFocusChanged(oldValue, newValue);
        }
    }

    /**
     * Allocates colibri (conference) channels for a specific <tt>MediaType</tt>
     * to be used by a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> which is to use the allocated colibri
     * (conference) channels
     * @param contentMap the local and remote <tt>ContentPacketExtension</tt>s
     * which specify the <tt>MediaType</tt>s for which colibri (conference)
     * channels are to be allocated
     * @return a <tt>ColibriConferenceIQ</tt> which describes the allocated
     * colibri (conference) channels for the specified <tt>mediaTypes</tt> which
     * are to be used by the specified <tt>peer</tt>; otherwise, <tt>null</tt>
     */
    public ColibriConferenceIQ createColibriChannels(
            CallPeerJabberImpl peer,
            Map<ContentPacketExtension,ContentPacketExtension> contentMap)
        throws OperationFailedException
    {
        if (!getConference().isJitsiVideobridge())
            return null;

        /*
         * For a colibri conference to work properly, all CallPeers in the
         * conference must share one and the same CallPeerMediaHandler state
         * i.e. they must use a single set of MediaStreams as if there was a
         * single CallPeerMediaHandler.
         */
        CallPeerMediaHandlerJabberImpl peerMediaHandler
            = peer.getMediaHandler();

        if (peerMediaHandler.getMediaHandler() != colibriMediaHandler)
        {
            for (MediaType mediaType : MediaType.values())
            {
                if (peerMediaHandler.getStream(mediaType) != null)
                    return null;
            }
        }

        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();
        String jitsiVideobridge
            = (colibri == null) ? getJitsiVideobridge() : colibri.getFrom();

        if ((jitsiVideobridge == null) || (jitsiVideobridge.length() == 0))
        {
            logger.error(
                    "Failed to allocate colibri channels: no videobridge"
                        + " found.");
            return null;
        }

        /*
         * The specified CallPeer will participate in the colibri conference
         * organized by this Call so it must use the shared CallPeerMediaHandler
         * state of all CallPeers in the same colibri conference.
         */
        if (colibriMediaHandler == null)
            colibriMediaHandler = new MediaHandler();
        peerMediaHandler.setMediaHandler(colibriMediaHandler);

        ColibriConferenceIQ conferenceRequest = new ColibriConferenceIQ();

        if (colibri != null)
            conferenceRequest.setID(colibri.getID());

        for (Map.Entry<ContentPacketExtension,ContentPacketExtension> e
                : contentMap.entrySet())
        {
            ContentPacketExtension localContent = e.getKey();
            ContentPacketExtension remoteContent = e.getValue();
            ContentPacketExtension cpe
                = (remoteContent == null) ? localContent : remoteContent;
            RtpDescriptionPacketExtension rdpe
                = cpe.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);
            String media = rdpe.getMedia();
            MediaType mediaType = MediaType.parseString(media);
            String contentName = mediaType.toString();
            ColibriConferenceIQ.Content contentRequest
                = new ColibriConferenceIQ.Content(contentName);

            conferenceRequest.addContent(contentRequest);

            boolean requestLocalChannel = true;

            if (colibri != null)
            {
                ColibriConferenceIQ.Content content
                    = colibri.getContent(contentName);

                if ((content != null) && (content.getChannelCount() > 0))
                    requestLocalChannel = false;
            }

            boolean peerIsInitiator = peer.isInitiator();

            if (requestLocalChannel)
            {
                ColibriConferenceIQ.Channel localChannelRequest
                    = new ColibriConferenceIQ.Channel();

                localChannelRequest.setEndpoint(protocolProvider.getOurJID());
                localChannelRequest.setInitiator(peerIsInitiator);

                for (PayloadTypePacketExtension ptpe : rdpe.getPayloadTypes())
                    localChannelRequest.addPayloadType(ptpe);
                setTransportOnChannel(peer, media, localChannelRequest);
                // DTLS-SRTP
                setDtlsEncryptionOnChannel(
                        jitsiVideobridge,
                        peer,
                        mediaType,
                        localChannelRequest);
                /*
                 * Since Jitsi Videobridge supports multiple Jingle transports,
                 * it is a good idea to indicate which one is expected on a
                 * channel.
                 */
                ensureTransportOnChannel(localChannelRequest, peer);
                contentRequest.addChannel(localChannelRequest);
            }

            ColibriConferenceIQ.Channel remoteChannelRequest
                = new ColibriConferenceIQ.Channel();

            remoteChannelRequest.setEndpoint(peer.getAddress());
            remoteChannelRequest.setInitiator(!peerIsInitiator);

            for (PayloadTypePacketExtension ptpe : rdpe.getPayloadTypes())
                remoteChannelRequest.addPayloadType(ptpe);
            setTransportOnChannel(
                    media,
                    localContent,
                    remoteContent,
                    peer,
                    remoteChannelRequest);
            // DTLS-SRTP
            setDtlsEncryptionOnChannel(
                    mediaType,
                    localContent,
                    remoteContent,
                    peer,
                    remoteChannelRequest);
            /*
             * Since Jitsi Videobridge supports multiple Jingle transports, it
             * is a good idea to indicate which one is expected on a channel.
             */
            ensureTransportOnChannel(remoteChannelRequest, peer);
            contentRequest.addChannel(remoteChannelRequest);
        }

        XMPPConnection connection = protocolProvider.getConnection();
        PacketCollector packetCollector
            = connection.createPacketCollector(
                    new PacketIDFilter(conferenceRequest.getPacketID()));

        conferenceRequest.setTo(jitsiVideobridge);
        conferenceRequest.setType(IQ.Type.GET);
        connection.sendPacket(conferenceRequest);

        Packet response
            = packetCollector.nextResult(
                    SmackConfiguration.getPacketReplyTimeout());

        packetCollector.cancel();

        if (response == null)
        {
            logger.error(
                    "Failed to allocate colibri channels: response is null."
                        + " Maybe the response timed out.");
            return null;
        }
        else if (response.getError() != null)
        {
            logger.error(
                    "Failed to allocate colibri channels: "
                        + response.getError());
            return null;
        }
        else if (!(response instanceof ColibriConferenceIQ))
        {
            logger.error(
                    "Failed to allocate colibri channels: response is not a"
                        + " colibri conference");
            return null;
        }

        ColibriConferenceIQ conferenceResponse = (ColibriConferenceIQ) response;
        String conferenceResponseID = conferenceResponse.getID();

        /*
         * Update the complete ColibriConferenceIQ representation maintained by
         * this instance with the information given by the (current) response.
         */
        {
            if (colibri == null)
            {
                colibri = new ColibriConferenceIQ();
                /*
                 * XXX We must remember the JID of the Jitsi Videobridge because
                 * (1) we do not want to re-discover it in every method
                 * invocation on this Call instance and (2) we want to use one
                 * and the same for all CallPeers within this Call instance.
                 */
                colibri.setFrom(conferenceResponse.getFrom());
            }

            String colibriID = colibri.getID();

            if (colibriID == null)
                colibri.setID(conferenceResponseID);
            else if (!colibriID.equals(conferenceResponseID))
                throw new IllegalStateException("conference.id");

            for (ColibriConferenceIQ.Content contentResponse
                    : conferenceResponse.getContents())
            {
                String contentName = contentResponse.getName();
                ColibriConferenceIQ.Content content
                    = colibri.getOrCreateContent(contentName);

                for (ColibriConferenceIQ.Channel channelResponse
                        : contentResponse.getChannels())
                {
                    int channelIndex = content.getChannelCount();

                    content.addChannel(channelResponse);
                    if (channelIndex == 0)
                    {
                        TransportManagerJabberImpl transportManager
                            = peerMediaHandler.getTransportManager();

                        transportManager
                            .isEstablishingConnectivityWithJitsiVideobridge
                                = true;
                        transportManager
                            .startConnectivityEstablishmentWithJitsiVideobridge
                                = true;

                        MediaType mediaType
                            = MediaType.parseString(contentName);

                        // DTLS-SRTP
                        addDtlsAdvertisedEncryptions(
                                peer,
                                channelResponse,
                                mediaType);
                    }
                }
            }
        }

        /*
         * Formulate the result to be returned to the caller which is a subset
         * of the whole conference information kept by this CallJabberImpl and
         * includes the remote channels explicitly requested by the method
         * caller and their respective local channels.
         */
        ColibriConferenceIQ conferenceResult = new ColibriConferenceIQ();

        conferenceResult.setFrom(colibri.getFrom());
        conferenceResult.setID(conferenceResponseID);

        for (Map.Entry<ContentPacketExtension,ContentPacketExtension> e
                : contentMap.entrySet())
        {
            ContentPacketExtension localContent = e.getKey();
            ContentPacketExtension remoteContent = e.getValue();
            ContentPacketExtension cpe
                = (remoteContent == null) ? localContent : remoteContent;
            MediaType mediaType = JingleUtils.getMediaType(cpe);
            ColibriConferenceIQ.Content contentResponse
                = conferenceResponse.getContent(mediaType.toString());

            if (contentResponse != null)
            {
                String contentName = contentResponse.getName();
                ColibriConferenceIQ.Content contentResult
                    = new ColibriConferenceIQ.Content(contentName);

                conferenceResult.addContent(contentResult);

                /*
                 * The local channel may have been allocated in a previous
                 * method call as part of the allocation of the first remote
                 * channel in the respective content. Anyway, the current method
                 * caller still needs to know about it.
                 */
                ColibriConferenceIQ.Content content
                    = colibri.getContent(contentName);
                ColibriConferenceIQ.Channel localChannel = null;

                if ((content != null) && (content.getChannelCount() > 0))
                {
                    localChannel = content.getChannel(0);
                    contentResult.addChannel(localChannel);
                }

                String localChannelID
                    = (localChannel == null) ? null : localChannel.getID();

                for (ColibriConferenceIQ.Channel channelResponse
                        : contentResponse.getChannels())
                {
                    if ((localChannelID == null)
                            || !localChannelID.equals(channelResponse.getID()))
                        contentResult.addChannel(channelResponse);
                }
            }
        }

        return conferenceResult;
    }

    /**
     * Initializes a <tt>ColibriStreamConnector</tt> on behalf of a specific
     * <tt>CallPeer</tt> to be used in association with a specific
     * <tt>ColibriConferenceIQ.Channel</tt> of a specific <tt>MediaType</tt>.
     *
     * @param peer the <tt>CallPeer</tt> which requests the initialization of a
     * <tt>ColibriStreamConnector</tt>
     * @param mediaType the <tt>MediaType</tt> of the stream which is to use the
     * initialized <tt>ColibriStreamConnector</tt> for RTP and RTCP traffic
     * @param channel the <tt>ColibriConferenceIQ.Channel</tt> to which RTP and
     * RTCP traffic is to be sent and from which such traffic is to be received
     * via the initialized <tt>ColibriStreamConnector</tt>
     * @param factory a <tt>StreamConnectorFactory</tt> implementation which is
     * to allocate the sockets to be used for RTP and RTCP traffic
     * @return a <tt>ColibriStreamConnector</tt> to be used for RTP and RTCP
     * traffic associated with the specified <tt>channel</tt>
     */
    public ColibriStreamConnector createColibriStreamConnector(
                CallPeerJabberImpl peer,
                MediaType mediaType,
                ColibriConferenceIQ.Channel channel,
                StreamConnectorFactory factory)
    {
        String channelID = channel.getID();

        if (channelID == null)
            throw new IllegalArgumentException("channel");

        if (colibri == null)
            throw new IllegalStateException("colibri");

        ColibriConferenceIQ.Content content
            = colibri.getContent(mediaType.toString());

        if (content == null)
            throw new IllegalArgumentException("mediaType");
        if ((content.getChannelCount() < 1)
                || !channelID.equals((channel = content.getChannel(0)).getID()))
            throw new IllegalArgumentException("channel");

        ColibriStreamConnector colibriStreamConnector;

        synchronized (colibriStreamConnectors)
        {
            int index = mediaType.ordinal();
            WeakReference<ColibriStreamConnector> weakReference
                = colibriStreamConnectors.get(index);

            colibriStreamConnector
                = (weakReference == null) ? null : weakReference.get();
            if (colibriStreamConnector == null)
            {
                StreamConnector streamConnector
                    = factory.createStreamConnector();

                if (streamConnector != null)
                {
                    colibriStreamConnector
                        = new ColibriStreamConnector(streamConnector);
                    colibriStreamConnectors.set(
                        index,
                        new WeakReference<ColibriStreamConnector>(
                            colibriStreamConnector));
                }
            }
        }

        return colibriStreamConnector;
    }

    /**
     * Expires specific (colibri) conference channels used by a specific
     * <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> which uses the specified (colibri)
     * conference channels to be expired
     * @param conference a <tt>ColibriConferenceIQ</tt> which specifies the
     * (colibri) conference channels to be expired
     */
    public void expireColibriChannels(
            CallPeerJabberImpl peer,
            ColibriConferenceIQ conference)
    {
        // Formulate the ColibriConferenceIQ request which is to be sent.
        if (colibri != null)
        {
            String conferenceID = colibri.getID();

            if (conferenceID.equals(conference.getID()))
            {
                ColibriConferenceIQ conferenceRequest
                    = new ColibriConferenceIQ();

                conferenceRequest.setID(conferenceID);

                for (ColibriConferenceIQ.Content content
                        : conference.getContents())
                {
                    ColibriConferenceIQ.Content colibriContent
                        = colibri.getContent(content.getName());

                    if (colibriContent != null)
                    {
                        ColibriConferenceIQ.Content contentRequest
                            = conferenceRequest.getOrCreateContent(
                            colibriContent.getName());

                        for (ColibriConferenceIQ.Channel channel
                                : content.getChannels())
                        {
                            ColibriConferenceIQ.Channel colibriChannel
                                = colibriContent.getChannel(channel.getID());

                            if (colibriChannel != null)
                            {
                                ColibriConferenceIQ.Channel channelRequest
                                    = new ColibriConferenceIQ.Channel();

                                channelRequest.setExpire(0);
                                channelRequest.setID(colibriChannel.getID());
                                contentRequest.addChannel(channelRequest);
                            }
                        }
                    }
                }

                /*
                 * Remove the channels which are to be expired from the internal
                 * state of the conference managed by this CallJabberImpl.
                 */
                for (ColibriConferenceIQ.Content contentRequest
                        : conferenceRequest.getContents())
                {
                    ColibriConferenceIQ.Content colibriContent
                        = colibri.getContent(contentRequest.getName());

                    for (ColibriConferenceIQ.Channel channelRequest
                            : contentRequest.getChannels())
                    {
                        ColibriConferenceIQ.Channel colibriChannel
                            = colibriContent.getChannel(channelRequest.getID());

                        colibriContent.removeChannel(colibriChannel);

                        /*
                         * If the last remote channel is to be expired, expire
                         * the local channel as well.
                         */
                        if (colibriContent.getChannelCount() == 1)
                        {
                            colibriChannel = colibriContent.getChannel(0);

                            channelRequest = new ColibriConferenceIQ.Channel();
                            channelRequest.setExpire(0);
                            channelRequest.setID(colibriChannel.getID());
                            contentRequest.addChannel(channelRequest);

                            colibriContent.removeChannel(colibriChannel);

                            break;
                        }
                    }
                }

                /*
                 * At long last, send the ColibriConferenceIQ request to expire
                 * the channels.
                 */
                conferenceRequest.setTo(colibri.getFrom());
                conferenceRequest.setType(IQ.Type.SET);
                getProtocolProvider().getConnection().sendPacket(
                        conferenceRequest);
            }
        }
    }

    /**
     * Sends a <tt>ColibriConferenceIQ</tt> to the videobridge used by this
     * <tt>CallJabberImpl</tt>, in order to request the the direction of
     * the <tt>channel</tt> with ID <tt>channelID</tt> be set to
     * <tt>direction</tt>
     * @param channelID the ID of the <tt>channel</tt> for which to set the
     * direction.
     * @param mediaType the <tt>MediaType</tt> of the channel (we can deduce this
     * by searching the <tt>ColibriConferenceIQ</tt>, but it's more convenient
     * to have it)
     * @param direction the <tt>MediaDirection</tt> to set.
     */
    public void setChannelDirection(String channelID,
                                    MediaType mediaType,
                                    MediaDirection direction)
    {
        if ((colibri != null) && (channelID != null))
        {
            ColibriConferenceIQ.Content content
                = colibri.getContent(mediaType.toString());

            if (content != null)
            {
                ColibriConferenceIQ.Channel channel
                    = content.getChannel(channelID);

                /*
                 * Note that we send requests even when the local Channel's
                 * direction and the direction we are setting are the same. We
                 * can easily avoid this, but we risk not sending necessary
                 * packets if local Channel and the actual channel on the
                 * videobridge are out of sync.
                 */
                if (channel != null)
                {
                    ColibriConferenceIQ.Channel requestChannel
                        = new ColibriConferenceIQ.Channel();

                    requestChannel.setID(channelID);
                    requestChannel.setDirection(direction);

                    ColibriConferenceIQ.Content requestContent
                        = new ColibriConferenceIQ.Content();

                    requestContent.setName(mediaType.toString());
                    requestContent.addChannel(requestChannel);

                    ColibriConferenceIQ conferenceRequest
                        = new ColibriConferenceIQ();

                    conferenceRequest.setID(colibri.getID());
                    conferenceRequest.setTo(colibri.getFrom());
                    conferenceRequest.setType(IQ.Type.SET);
                    conferenceRequest.addContent(requestContent);

                    getProtocolProvider().getConnection().sendPacket(
                            conferenceRequest);
                }
            }
        }
    }

    /**
     * Creates a <tt>CallPeerJabberImpl</tt> from <tt>calleeJID</tt> and sends
     * them <tt>session-initiate</tt> IQ request.
     *
     * @param calleeJID the party that we would like to invite to this call.
     * @param discoverInfo any discovery information that we have for the jid
     * we are trying to reach and that we are passing in order to avoid having
     * to ask for it again.
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>session-initiate</tt>
     * {@link JingleIQ} which is to init this <tt>CallJabberImpl</tt>
     * @param supportedTransports the XML namespaces of the jingle transports
     * to use.
     *
     * @return the newly created <tt>CallPeerJabberImpl</tt> corresponding to
     * <tt>calleeJID</tt>. All following state change events will be
     * delivered through this call peer.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     *  to create the call.
     */
    public CallPeerJabberImpl initiateSession(
            String calleeJID,
            DiscoverInfo discoverInfo,
            Iterable<PacketExtension> sessionInitiateExtensions,
            Collection<String> supportedTransports)
        throws OperationFailedException
    {
        // create the session-initiate IQ
        CallPeerJabberImpl callPeer = new CallPeerJabberImpl(calleeJID, this);

        callPeer.setDiscoveryInfo(discoverInfo);

        addCallPeer(callPeer);

        callPeer.setState(CallPeerState.INITIATING_CALL);

        // If this was the first peer we added in this call, then the call is
        // new and we need to notify everyone of its creation.
        if (getCallPeerCount() == 1)
            parentOpSet.fireCallEvent(CallEvent.CALL_INITIATED, this);

        CallPeerMediaHandlerJabberImpl mediaHandler
            = callPeer.getMediaHandler();

        //set the supported transports before the transport manager is created
        mediaHandler.setSupportedTransports(supportedTransports);

        /* enable video if it is a video call */
        mediaHandler.setLocalVideoTransmissionEnabled(localVideoAllowed);
        /* enable remote-control if it is a desktop sharing session */
        mediaHandler.setLocalInputEvtAware(getLocalInputEvtAware());

        /*
         * Set call state to connecting so that the user interface would start
         * playing the tones. We do that here because we may be harvesting
         * STUN/TURN addresses in initiateSession() which would take a while.
         */
        callPeer.setState(CallPeerState.CONNECTING);

        // if initializing session fails, set peer to failed
        boolean sessionInitiated = false;

        try
        {
            callPeer.initiateSession(sessionInitiateExtensions);
            sessionInitiated = true;
        }
        finally
        {
            // if initialization throws an exception
            if (!sessionInitiated)
                callPeer.setState(CallPeerState.FAILED);
        }
        return callPeer;
    }

    /**
     * Updates the Jingle sessions for the <tt>CallPeer</tt>s of this
     * <tt>Call</tt>, to reflect the current state of the the video contents of
     * this <tt>Call</tt>. Sends a <tt>content-modify</tt>, <tt>content-add</tt>
     * or <tt>content-remove</tt> message to each of the current
     * <tt>CallPeer</tt>s.
     *
     * @throws OperationFailedException if a problem occurred during message
     * generation or there was a network problem
     */
    @Override
    public void modifyVideoContent()
        throws OperationFailedException
    {
        if (logger.isDebugEnabled())
            logger.debug("Updating video content for " + this);

        boolean change = false;
        for (CallPeerJabberImpl peer : getCallPeerList())
            change |= peer.sendModifyVideoContent();

        if (change)
            fireCallChangeEvent(
                    CallChangeEvent.CALL_PARTICIPANTS_CHANGE, null, null);
    }

    /**
     * Notifies this instance that a specific <tt>ColibriConferenceIQ</tt> has
     * been received.
     *
     * @param conferenceIQ the <tt>ColibriConferenceIQ</tt> which has been
     * received
     * @return <tt>true</tt> if the specified <tt>conferenceIQ</tt> was
     * processed by this instance and no further processing is to be performed
     * by other possible processors of <tt>ColibriConferenceIQ</tt>s; otherwise,
     * <tt>false</tt>. Because a <tt>ColibriConferenceIQ</tt> request sent from
     * the Jitsi Videobridge server to the application as its client concerns a
     * specific <tt>CallJabberImpl</tt> implementation, no further processing by
     * other <tt>CallJabberImpl</tt> instances is necessary once the
     * <tt>ColibriConferenceIQ</tt> is processed by the associated
     * <tt>CallJabberImpl</tt> instance.
     */
    boolean processColibriConferenceIQ(ColibriConferenceIQ conferenceIQ)
    {
        if (colibri == null)
        {
            /*
             * This instance has not set up any conference using the Jitsi
             * Videobridge server-side technology yet so it cannot be bothered
             * with related requests.
             */
            return false;
        }
        else if (conferenceIQ.getID().equals(colibri.getID()))
        {
            /*
             * Remove the local Channels (from the specified conferenceIQ) i.e.
             * the Channels on which the local peer/user is sending to the Jitsi
             * Videobridge server because they concern this Call only and not
             * its CallPeers.
             */
            for (MediaType mediaType : MediaType.values())
            {
                String contentName = mediaType.toString();
                ColibriConferenceIQ.Content content
                    = conferenceIQ.getContent(contentName);

                if (content != null)
                {
                    ColibriConferenceIQ.Content thisContent
                        = colibri.getContent(contentName);

                    if ((thisContent != null)
                            && (thisContent.getChannelCount() > 0))
                    {
                        ColibriConferenceIQ.Channel thisChannel
                            = thisContent.getChannel(0);
                        ColibriConferenceIQ.Channel channel
                            = content.getChannel(thisChannel.getID());

                        if (channel != null)
                            content.removeChannel(channel);
                    }
                }
            }

            for (CallPeerJabberImpl callPeer : getCallPeerList())
                callPeer.processColibriConferenceIQ(conferenceIQ);

            /*
             * We have removed the local Channels from the specified
             * conferenceIQ. Consequently, it is no longer the same and fit for
             * processing by other CallJabberImpl instances.
             */
            return true;
        }
        else
        {
            /*
             * This instance has set up a conference using the Jitsi Videobridge
             * server-side technology but it is not the one referred to by the
             * specified conferenceIQ i.e. the specified conferenceIQ does not
             * concern this instance.
             */
            return false;
        }
    }

    /**
     * Creates a new call peer and sends a RINGING response.
     *
     * @param jingleIQ the {@link JingleIQ} that created the session.
     *
     * @return the newly created {@link CallPeerJabberImpl} (the one that sent
     * the INVITE).
     */
    public CallPeerJabberImpl processSessionInitiate(JingleIQ jingleIQ)
    {
        // Use the IQs 'from', instead of the jingle 'initiator' field,
        // because we want to make sure that following IQs are sent with the
        // correct 'to'.
        String remoteParty = jingleIQ.getFrom();

        boolean autoAnswer = false;
        CallPeerJabberImpl attendant = null;
        OperationSetBasicTelephonyJabberImpl basicTelephony = null;

        CallPeerJabberImpl callPeer
            = new CallPeerJabberImpl(remoteParty, this, jingleIQ);

        addCallPeer(callPeer);

        /*
         * We've already sent ack to the specified session-initiate so if it has
         * been sent as part of an attended transfer, we have to hang up on the
         * attendant.
         */
        try
        {
            TransferPacketExtension transfer
                = (TransferPacketExtension)
                    jingleIQ.getExtension(
                            TransferPacketExtension.ELEMENT_NAME,
                            TransferPacketExtension.NAMESPACE);

            if (transfer != null)
            {
                String sid = transfer.getSID();

                if (sid != null)
                {
                    ProtocolProviderServiceJabberImpl protocolProvider
                        = getProtocolProvider();
                    basicTelephony
                        = (OperationSetBasicTelephonyJabberImpl)
                            protocolProvider.getOperationSet(
                                    OperationSetBasicTelephony.class);
                    CallJabberImpl attendantCall
                        = basicTelephony
                            .getActiveCallsRepository()
                                .findSID(sid);

                    if (attendantCall != null)
                    {
                        attendant = attendantCall.getPeer(sid);
                        if ((attendant != null)
                                && basicTelephony
                                    .getFullCalleeURI(attendant.getAddress())
                                        .equals(transfer.getFrom())
                                && protocolProvider.getOurJID().equals(
                                        transfer.getTo()))
                        {
                            //basicTelephony.hangupCallPeer(attendant);
                            autoAnswer = true;
                        }
                    }
                }
            }
        }
        catch (Throwable t)
        {
            logger.error(
                    "Failed to hang up on attendant"
                        + " as part of session transfer",
                    t);

            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }

        CoinPacketExtension coin
            = (CoinPacketExtension)
                jingleIQ.getExtension(
                        CoinPacketExtension.ELEMENT_NAME,
                        CoinPacketExtension.NAMESPACE);

        if (coin != null)
        {
            boolean b
                = Boolean.parseBoolean(
                        (String)
                            coin.getAttribute(
                                    CoinPacketExtension.ISFOCUS_ATTR_NAME));

            callPeer.setConferenceFocus(b);
        }

        //before notifying about this call, make sure that it looks alright
        callPeer.processSessionInitiate(jingleIQ);

        // if paranoia is set, to accept the call we need to know that
        // the other party has support for media encryption
        if (getProtocolProvider().getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.MODE_PARANOIA, false)
            && callPeer.getMediaHandler().getAdvertisedEncryptionMethods()
                    .length
                == 0)
        {
            //send an error response;
            String reasonText
                = JabberActivator.getResources().getI18NString(
                        "service.gui.security.encryption.required");
            JingleIQ errResp
                = JinglePacketFactory.createSessionTerminate(
                        jingleIQ.getTo(),
                        jingleIQ.getFrom(),
                        jingleIQ.getSID(),
                        Reason.SECURITY_ERROR,
                        reasonText);

            callPeer.setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);

            return null;
        }

        if (callPeer.getState() == CallPeerState.FAILED)
            return null;

        callPeer.setState( CallPeerState.INCOMING_CALL );

        // in case of attended transfer, auto answer the call
        if (autoAnswer)
        {
            /* answer directly */
            try
            {
                callPeer.answer();
            }
            catch(Exception e)
            {
                logger.info(
                        "Exception occurred while answer transferred call",
                        e);
                callPeer = null;
            }

            // hang up now
            try
            {
                basicTelephony.hangupCallPeer(attendant);
            }
            catch(OperationFailedException e)
            {
                logger.error(
                        "Failed to hang up on attendant as part of session"
                            + " transfer",
                        e);
            }

            return callPeer;
        }

        /* see if offer contains audio and video so that we can propose
         * option to the user (i.e. answer with video if it is a video call...)
         */
        List<ContentPacketExtension> offer
            = callPeer.getSessionIQ().getContentList();
        Map<MediaType, MediaDirection> directions
            = new HashMap<MediaType, MediaDirection>();

        directions.put(MediaType.AUDIO, MediaDirection.INACTIVE);
        directions.put(MediaType.VIDEO, MediaDirection.INACTIVE);

        for (ContentPacketExtension c : offer)
        {
            String contentName = c.getName();
            MediaDirection remoteDirection
                = JingleUtils.getDirection(c, callPeer.isInitiator());

            if (MediaType.AUDIO.toString().equals(contentName))
                directions.put(MediaType.AUDIO, remoteDirection);
            else if (MediaType.VIDEO.toString().equals(contentName))
                directions.put(MediaType.VIDEO, remoteDirection);
        }

        // If this was the first peer we added in this call, then the call is
        // new and we need to notify everyone of its creation.
        if (getCallPeerCount() == 1)
        {
            parentOpSet.fireCallEvent(
                    CallEvent.CALL_RECEIVED,
                    this,
                    directions);
        }

        // Manages auto answer with "audio only", or "audio/video" answer.
        OperationSetAutoAnswerJabberImpl autoAnswerOpSet
            = (OperationSetAutoAnswerJabberImpl)
                getProtocolProvider().getOperationSet(
                        OperationSetBasicAutoAnswer.class);

        if (autoAnswerOpSet != null)
            autoAnswerOpSet.autoAnswer(this, directions);

        return callPeer;
    }

    /**
     * Updates the state of the local DTLS-SRTP endpoint (i.e. the local
     * <tt>DtlsControl</tt> instance) from the state of the remote DTLS-SRTP
     * endpoint represented by a specific <tt>ColibriConferenceIQ.Channel</tt>.
     *
     * @param peer the <tt>CallPeer</tt> associated with the method invocation
     * @param channel the <tt>ColibriConferenceIQ.Channel</tt> which represents
     * the state of the remote DTLS-SRTP endpoint
     * @param mediaType the <tt>MediaType</tt> of the media to be transmitted
     * over the DTLS-SRTP session
     */
    private boolean addDtlsAdvertisedEncryptions(
            CallPeerJabberImpl peer,
            ColibriConferenceIQ.Channel channel,
            MediaType mediaType)
    {
        CallPeerMediaHandlerJabberImpl peerMediaHandler
            = peer.getMediaHandler();
        DtlsControl dtlsControl
            = (DtlsControl)
                peerMediaHandler.getSrtpControls().get(
                        mediaType,
                        SrtpControlType.DTLS_SRTP);

        if (dtlsControl != null)
        {
            dtlsControl.setSetup(
                    peer.isInitiator()
                        ? DtlsControl.Setup.ACTIVE
                        : DtlsControl.Setup.PASSIVE);
        }

        IceUdpTransportPacketExtension remoteTransport = channel.getTransport();

        return
            peerMediaHandler.addDtlsAdvertisedEncryptions(
                    true,
                    remoteTransport,
                    mediaType);
    }

    /**
     * Updates the state of the remote DTLS-SRTP endpoint represented by a
     * specific <tt>ColibriConferenceIQ.Channel</tt> from the state of the local
     * DTLS-SRTP endpoint. The specified <tt>channel</tt> is to be used by the
     * conference focus for the purposes of transmitting media between a remote
     * peer and the Jitsi Videobridge server.
     *
     * @param mediaType the <tt>MediaType</tt> of the media to be transmitted
     * over the DTLS-SRTP session
     * @param localContent the <tt>ContentPacketExtension</tt> of the local peer
     * in the negotiation between the local and the remote peers. If
     * <tt>remoteContent</tt> is <tt>null</tt>, represents an offer from the
     * local peer to the remote peer; otherwise, represents an answer from the
     * local peer to an offer from the remote peer.
     * @param remoteContent the <tt>ContentPacketExtension</tt>, if any, of the
     * remote peer in the negotiation between the local and the remote peers. If
     * <tt>null</tt>, <tt>localContent</tt> represents an offer from the local
     * peer to the remote peer; otherwise, <tt>localContent</tt> represents an
     * answer from the local peer to an offer from the remote peer
     * @param peer the <tt>CallPeer</tt> which represents the remote peer and
     * which is associated with the specified <tt>channel</tt>
     * @param channel the <tt>ColibriConferenceIQ.Channel</tt> which represents
     * the state of the remote DTLS-SRTP endpoint.
     */
    private void setDtlsEncryptionOnChannel(
            MediaType mediaType,
            ContentPacketExtension localContent,
            ContentPacketExtension remoteContent,
            CallPeerJabberImpl peer,
            ColibriConferenceIQ.Channel channel)
    {
        AccountID accountID = getProtocolProvider().getAccountID();

        if (accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && accountID.isEncryptionProtocolEnabled(
                        SrtpControlType.DTLS_SRTP)
                && (remoteContent != null))
        {
            IceUdpTransportPacketExtension remoteTransport
                = remoteContent.getFirstChildOfType(
                        IceUdpTransportPacketExtension.class);

            if (remoteTransport != null)
            {
                List<DtlsFingerprintPacketExtension> remoteFingerprints
                    = remoteTransport.getChildExtensionsOfType(
                            DtlsFingerprintPacketExtension.class);

                if (!remoteFingerprints.isEmpty())
                {
                    IceUdpTransportPacketExtension localTransport
                        = ensureTransportOnChannel(channel, peer);

                    if (localTransport != null)
                    {
                        List<DtlsFingerprintPacketExtension> localFingerprints
                            = localTransport.getChildExtensionsOfType(
                                    DtlsFingerprintPacketExtension.class);

                        if (localFingerprints.isEmpty())
                        {
                            for (DtlsFingerprintPacketExtension remoteFingerprint
                                    : remoteFingerprints)
                            {
                                DtlsFingerprintPacketExtension localFingerprint
                                    = new DtlsFingerprintPacketExtension();

                                localFingerprint.setFingerprint(
                                        remoteFingerprint.getFingerprint());
                                localFingerprint.setHash(
                                        remoteFingerprint.getHash());
                                localTransport.addChildExtension(
                                        localFingerprint);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the state of the remote DTLS-SRTP endpoint represented by a
     * specific <tt>ColibriConferenceIQ.Channel</tt> from the state of the local
     * DTLS-SRTP endpoint (i.e. the local <tt>DtlsControl</tt> instance). The
     * specified <tt>channel</tt> is to be used by the conference focus for the
     * purposes of transmitting media between the local peer and the Jitsi
     * Videobridge server.
     *
     * @param jitsiVideobridge the address/JID of the Jitsi Videobridge
     * @param peer the <tt>CallPeer</tt> associated with the method invocation
     * @param mediaType the <tt>MediaType</tt> of the media to be transmitted
     * over the DTLS-SRTP session
     * @param channel the <tt>ColibriConferenceIQ.Channel</tt> which represents
     * the state of the remote DTLS-SRTP endpoint.
     */
    private void setDtlsEncryptionOnChannel(
            String jitsiVideobridge,
            CallPeerJabberImpl peer,
            MediaType mediaType,
            ColibriConferenceIQ.Channel channel)
    {
        ProtocolProviderServiceJabberImpl protocolProvider
            = getProtocolProvider();
        AccountID accountID = protocolProvider.getAccountID();

        if (accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && accountID.isEncryptionProtocolEnabled(
                        SrtpControlType.DTLS_SRTP)
                && protocolProvider.isFeatureSupported(
                        jitsiVideobridge,
                        ProtocolProviderServiceJabberImpl
                            .URN_XMPP_JINGLE_DTLS_SRTP))
        {
            CallPeerMediaHandlerJabberImpl mediaHandler
                = peer.getMediaHandler();
            DtlsControl dtlsControl
                = (DtlsControl)
                    mediaHandler.getSrtpControls().getOrCreate(
                            mediaType,
                            SrtpControlType.DTLS_SRTP);

            if (dtlsControl != null)
            {
                IceUdpTransportPacketExtension transport
                    = ensureTransportOnChannel(channel, peer);

                if (transport != null)
                    setDtlsEncryptionOnTransport(dtlsControl, transport);
            }
        }
    }

    /**
     * Sets the properties (i.e. fingerprint and hash function) of a specific
     * <tt>DtlsControl</tt> on the specific
     * <tt>IceUdpTransportPacketExtension</tt>.
     *
     * @param dtlsControl the <tt>DtlsControl</tt> the properties of which are
     * to be set on the specified <tt>localTransport</tt>
     * @param localTransport the <tt>IceUdpTransportPacketExtension</tt> on
     * which the properties of the specified <tt>dtlsControl</tt> are to be set
     */
    static void setDtlsEncryptionOnTransport(
            DtlsControl dtlsControl,
            IceUdpTransportPacketExtension localTransport)
    {
        String fingerprint = dtlsControl.getLocalFingerprint();
        String hash = dtlsControl.getLocalFingerprintHashFunction();

        DtlsFingerprintPacketExtension fingerprintPE
            = localTransport.getFirstChildOfType(
                    DtlsFingerprintPacketExtension.class);

        if (fingerprintPE == null)
        {
            fingerprintPE = new DtlsFingerprintPacketExtension();
            localTransport.addChildExtension(fingerprintPE);
        }
        fingerprintPE.setFingerprint(fingerprint);
        fingerprintPE.setHash(hash);
    }

    private void setTransportOnChannel(
            CallPeerJabberImpl peer,
            String media,
            ColibriConferenceIQ.Channel channel)
        throws OperationFailedException
    {
        PacketExtension transport
            = peer.getMediaHandler().getTransportManager().createTransport(
                    media);

        if (transport instanceof IceUdpTransportPacketExtension)
            channel.setTransport((IceUdpTransportPacketExtension) transport);
    }

    private void setTransportOnChannel(
            String media,
            ContentPacketExtension localContent,
            ContentPacketExtension remoteContent,
            CallPeerJabberImpl peer,
            ColibriConferenceIQ.Channel channel)
        throws OperationFailedException
    {
        if (remoteContent != null)
        {
            IceUdpTransportPacketExtension transport
                = remoteContent.getFirstChildOfType(
                        IceUdpTransportPacketExtension.class);

            channel.setTransport(
                    TransportManagerJabberImpl.cloneTransportAndCandidates(
                            transport));
        }
    }

    /**
     * Makes an attempt to ensure that a specific
     * <tt>ColibriConferenceIQ.Channel</tt> has a non-<tt>null</tt>
     * <tt>transport</tt> set. If the specified <tt>channel</tt> does not have
     * a <tt>transport</tt>, the method invokes the <tt>TransportManager</tt> of
     * the specified <tt>CallPeerJabberImpl</tt> to initialize a new
     * <tt>PacketExtension</tt>.
     *
     * @param channel the <tt>ColibriConferenceIQ.Channel</tt> to ensure the
     * <tt>transport</tt> on
     * @param peer the <tt>CallPeerJabberImpl</tt> which is associated with the
     * specified <tt>channel</tt> and which specifies the
     * <tt>TransportManager</tt> to be described in the specified
     * <tt>channel</tt>
     * @return the <tt>transport</tt> of the specified <tt>channel</tt>
     */
    private IceUdpTransportPacketExtension ensureTransportOnChannel(
            ColibriConferenceIQ.Channel channel,
            CallPeerJabberImpl peer)
    {
        IceUdpTransportPacketExtension transport
            = channel.getTransport();

        if (transport == null)
        {
            PacketExtension pe
                = peer
                    .getMediaHandler()
                        .getTransportManager()
                            .createTransportPacketExtension();

            if (pe instanceof IceUdpTransportPacketExtension)
            {
                transport = (IceUdpTransportPacketExtension) pe;
                channel.setTransport(transport);
            }
        }
        return transport;
    }

    /**
     * Gets the entity ID of the Jitsi Videobridge to be utilized by this
     * <tt>Call</tt> for the purposes of establishing a server-assisted
     * telephony conference.
     *
     * @return the entity ID of the Jitsi Videobridge to be utilized by this
     * <tt>Call</tt> for the purposes of establishing a server-assisted
     * telephony conference.
     */
    public String getJitsiVideobridge()
    {
        if ((this.jitsiVideobridge == null)
                && getConference().isJitsiVideobridge())
        {
            String jitsiVideobridge
                = getProtocolProvider().getJitsiVideobridge();

            if (jitsiVideobridge != null)
                this.jitsiVideobridge = jitsiVideobridge;
        }
        return this.jitsiVideobridge;
    }

    /**
     * {@inheritDoc}
     *
     * Implements
     * {@link net.java.sip.communicator.service.protocol.event.DTMFListener#toneReceived(net.java.sip.communicator.service.protocol.event.DTMFReceivedEvent)}
     *
     * Forwards DTMF events to the <tt>IncomingDTMF</tt> operation set, setting
     * this <tt>Call</tt> as the source.
     */
    @Override
    public void toneReceived(DTMFReceivedEvent evt)
    {
        OperationSetIncomingDTMF opSet
            = getProtocolProvider()
                .getOperationSet(OperationSetIncomingDTMF.class);

        if (opSet != null && opSet instanceof OperationSetIncomingDTMFJabberImpl)
        {
            // Re-fire the event using this Call as the source.
            ((OperationSetIncomingDTMFJabberImpl) opSet).toneReceived(
                    new DTMFReceivedEvent(
                            this,
                            evt.getValue(),
                            evt.getDuration(),
                            evt.getStart()));
        }
    }
}
