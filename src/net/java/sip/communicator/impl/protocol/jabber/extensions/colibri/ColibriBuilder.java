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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;

import org.jivesoftware.smack.packet.*;
import org.jxmpp.jid.*;

import java.util.*;

/**
 * Utility class for building Colibri queries. It can be used to allocate,
 * expire Colibri channels and update it's ICE transport info on the bridge.
 * The flow is as follows:<br/>
 * <ol>
 *     <li>
 * Add one or multiple requests of the same type by calling
 * {@link #addAllocateChannelsReq(
 *              boolean, String, String, boolean, java.util.List)}}
 * or {@link #addExpireChannelsReq(ColibriConferenceIQ)}
 * or {@link #addRtpDescription(RtpDescriptionPacketExtension, String, String)}
 * and {@link #addSourceGroupsInfo(Map, ColibriConferenceIQ)}
 * and {@link #addSourceInfo(Map, ColibriConferenceIQ)}.
 *     </li>
 *     <li>
 * Compile the request by calling {@link #getRequest(Jid)}. Then send it to
 * the bridge.
 *     </li>
 *     <li>
 * Use {@link ColibriAnalyser} to extract particular channels info or create
 * response to the client.
 *     </li>
 *     <li>
 * Call {@link #reset()} and start next query.
 *     </li>
 * </ol>
 * The important thing is to share the same {@link ColibriConferenceIQ} instance
 * between the {@link ColibriAnalyser} and {@link ColibriBuilder}. The builder
 * needs to have conference ID set once the first response from JVB is received.
 * Otherwise it will be allocating new conferences for each allocate request and
 * it will fail on other request types because it will not supply valid
 * conference ID.
 *
 * @author Pawel Domas
 * @author Boris Grozev
 */
public class ColibriBuilder
{
    /**
     * The logger used by this instance.
     */
    private final static Logger logger = Logger.getLogger(ColibriBuilder.class);

    /**
     * Copies the transport info from a {@link ContentPacketExtension} to a
     * {@link ColibriConferenceIQ.ChannelCommon}.
     * @param content the {@link ContentPacketExtension} from which to copy.
     * @param channel the {@link ColibriConferenceIQ.ChannelCommon} to which to copy.
     */
    private static void copyTransport(
        ContentPacketExtension content,
        ColibriConferenceIQ.ChannelCommon channel)
    {
        IceUdpTransportPacketExtension transport
            = content.getFirstChildOfType(
            IceUdpTransportPacketExtension.class);

        channel.setTransport(
            IceUdpTransportPacketExtension
                .cloneTransportAndCandidates(transport, true));
    }

    /**
     * Copies the contents of the description element from a
     * {@link ContentPacketExtension} to a {@link ColibriConferenceIQ.Channel}.
     * @param content the {@link ContentPacketExtension} from which to copy.
     * @param channel the {@link ColibriConferenceIQ.Channel} to which to copy.
     */
    private static boolean copyDescription(
        ContentPacketExtension content,
        ColibriConferenceIQ.Channel channel)
    {
        RtpDescriptionPacketExtension description
            = content.getFirstChildOfType(
            RtpDescriptionPacketExtension.class);
        if (description != null)
        {
            return copyDescription(description, channel);
        }
        return false;
    }

    /**
     * Copies the contents of a {@link RtpDescriptionPacketExtension} to a
     * {@link ColibriConferenceIQ.Channel}.
     * @param description the {@link RtpDescriptionPacketExtension} from which
     * to copy.
     * @param channel the {@link ColibriConferenceIQ.Channel} to which to copy.
     */
    private static boolean copyDescription(
        RtpDescriptionPacketExtension description,
        ColibriConferenceIQ.Channel channel)
    {
        boolean added = false;
        for (PayloadTypePacketExtension payloadType : description.getPayloadTypes())
        {
            channel.addPayloadType(new PayloadTypePacketExtension(payloadType));
            added = true;
        }

        for (RTPHdrExtPacketExtension rtpHdrExt : description.getExtmapList())
        {
            channel
                .addRtpHeaderExtension(new RTPHdrExtPacketExtension(rtpHdrExt));
            added = true;
        }

        return added;
    }

    /**
     * Sets the {@link SourcePacketExtension}s of a particular
     * {@link ColibriConferenceIQ.Channel}.
     * @param channel the channel to which to add sources.
     * @param sources the list of sources to add.
     */
    private static void addSources(
        ColibriConferenceIQ.Channel channel,
        List<SourcePacketExtension> sources)
    {
        for (SourcePacketExtension source : sources)
        {
            channel.addSource(source.copy());
        }

        if (channel.getSources() == null
            || channel.getSources().isEmpty())
        {
            // Put an empty source to remove all sources
            SourcePacketExtension emptySource = new SourcePacketExtension();
            emptySource.setSSRC(-1L);
            channel.addSource(emptySource);
        }
    }

    /**
     * Adds a list of {@link SourceGroupPacketExtension}s to a particular
     * {@link ColibriConferenceIQ.Channel}.
     * @param channel the channel to which to add source groups.
     * @param sourceGroups the source groups to add.
     * @param video whether the channel's media type is video or not.
     *
     * @return {@code true} iff any source groups we added to the channel.
     */
    private static boolean addSourceGroups(
        ColibriConferenceIQ.Channel channel,
        List<SourceGroupPacketExtension> sourceGroups,
        boolean video)
    {
        boolean hasAnyChanges = false;

        if (sourceGroups.isEmpty() && video)
        {
            hasAnyChanges = true;

            // Put an empty source group to turn off simulcast layers
            channel.addSourceGroup(
                SourceGroupPacketExtension.createSimulcastGroup());
        }

        for (SourceGroupPacketExtension sourceGroup : sourceGroups)
        {
            hasAnyChanges = true;

            channel.addSourceGroup(sourceGroup);
        }

        return hasAnyChanges;
    }

    /**
     * Colibri IQ that holds conference state.
     */
    private final ColibriConferenceIQ conferenceState;

    /**
     * The type of the request currently being build.
     * {@link RequestType#UNDEFINED} if this instance is in the "zero" state.
     * To get into the "zero" state call {@link #reset()}.
     */
    private RequestType requestType = RequestType.UNDEFINED;

    /**
     * Object holding the request currently being constructed.
     */
    private ColibriConferenceIQ request;

    /**
     * Field used to hold info if there are any channels to be expired
     */
    private boolean hasAnyChannelsToExpire = false;

    /**
     * Channel 'last-n' option that will be added when channels are created.
     * Set to <tt>null</tt> in order to omit.
     */
    private Integer channelLastN;

    /**
     * Channel 'simulcast-mode' option that will be added when channels are
     * created.
     * Set to <tt>null</tt> in order to omit.
     */
    private SimulcastMode simulcastMode;

    /**
     * Specifies the audio packet delay that will be set on all created audio
     * channels. When set to <tt>null</tt> the builder will clear the attribute
     * which stands for 'undefined'.
     **/
    private Integer audioPacketDelay;

    /**
     * Channel 'rtp-level-relay-type' option that will be used with all created
     * audio channels. Possible values: mixer or translator (default).
     */
    private RTPLevelRelayType rtpLevelRelayType;

    /**
     * Creates new instance of {@link ColibriBuilder} for given
     * <tt>conferenceState</tt>.
     *
     * @param conferenceState the conference state which will be taken into
     * account when constructing specific requests (the need to allocate new
     * conference or re-use the one currently in progress).
     */
    public ColibriBuilder(ColibriConferenceIQ conferenceState)
    {
        this.conferenceState
            = Objects.requireNonNull(conferenceState, "conferenceState");

        reset();
    }

    /**
     * Resets this builder state to the "zero" state when no request info is
     * stored.
     */
    public void reset()
    {
        requestType = RequestType.UNDEFINED;

        // Copy conference ID if any
        // otherwise null or empty will be placed which means
        // that new conference will be allocated
        request = new ColibriConferenceIQ();
        request.setID(conferenceState.getID());
        request.setName(conferenceState.getName());
        request.setGID(conferenceState.getGID());

        request.setType(IQ.Type.set);
    }

    /**
     * Adds next channel allocation request to
     * {@link RequestType#ALLOCATE_CHANNELS} query currently being built.
     *
     * @param useBundle <tt>true</tt> if allocated channels should all use
     * the same bundle.
     * @param endpointId name of the endpoint for which Colibri channels will
     * @param peerIsInitiator the value that will be set in 'initiator'
     * attribute ({@link ColibriConferenceIQ.Channel#initiator}).
     * @param contents the list of {@link ContentPacketExtension} describing
     * channels media.
     *
     * @return {@code true} if the request yields any changes in Colibri
     * channels state on the bridge or {@code false} otherwise. In general when
     * {@code false} is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addAllocateChannelsReq(
        boolean useBundle,
        String endpointId,
        String statsId,
        boolean peerIsInitiator,
        List<ContentPacketExtension> contents)
    {
        return addAllocateChannelsReq(
            useBundle,
            endpointId,
            statsId,
            peerIsInitiator,
            contents,
            null, null, null);
    }

    /**
     * Adds next channel allocation request to
     * {@link RequestType#ALLOCATE_CHANNELS} query currently being built.
     *
     * @param useBundle <tt>true</tt> if allocated channels should all use
     * the same bundle.
     * @param endpointId name of the endpoint for which Colibri channels will
     * @param statsId the stats ID of the endpoint for which channels are to be
     * allocated.
     * @param peerIsInitiator the value that will be set in 'initiator'
     * attribute ({@link ColibriConferenceIQ.Channel#initiator}).
     * @param contents the list of {@link ContentPacketExtension} describing
     * channels media.
     * @param sourceMap a map of content name to the list of
     * {@link SourcePacketExtension}s which are to be added to the channel
     * allocation request for this content.
     * @param sourceGroupMap a map of content name to the list of
     * {@link SourceGroupPacketExtension}s which are to be added to the channel
     * allocation request for this content.
     * @param octoRelayIds the optional list of Octo relay IDs to be added to
     * the channel. If this parameter is non-null, the allocated channels will
     * be of type Octo.
     *
     * @return {@code true} if the request yields any changes in Colibri
     * channels state on the bridge or {@code false} otherwise. In general when
     * {@code false} is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addAllocateChannelsReq(
            boolean useBundle,
            String endpointId,
            String statsId,
            boolean peerIsInitiator,
            List<ContentPacketExtension> contents,
            Map<String, List<SourcePacketExtension>> sourceMap,
            Map<String, List<SourceGroupPacketExtension>> sourceGroupMap,
            List<String> octoRelayIds)
    {
        Objects.requireNonNull(contents, "contents");
        assertRequestType(RequestType.ALLOCATE_CHANNELS);

        boolean hasAnyChanges = false;

        for (ContentPacketExtension content : contents)
        {
            MediaType mediaType = JingleUtils.getMediaType(content);
            String contentName = mediaType.toString();
            ColibriConferenceIQ.Content requestContent
                = request.getOrCreateContent(contentName);

            ColibriConferenceIQ.ChannelCommon requestChannel;
            if (mediaType == MediaType.DATA)
            {
                requestChannel = new ColibriConferenceIQ.SctpConnection();
            }
            else if (octoRelayIds != null)
            {
                requestChannel = new ColibriConferenceIQ.OctoChannel();
            }
            else
            {
                requestChannel = new ColibriConferenceIQ.Channel();
            }

            if (endpointId != null)
            {
                requestChannel.setEndpoint(endpointId);
            }
            requestChannel.setInitiator(peerIsInitiator);

            if (useBundle && endpointId != null)
            {
                requestChannel.setChannelBundleId(endpointId);
            }

            if (requestChannel instanceof ColibriConferenceIQ.Channel)
            {
                ColibriConferenceIQ.Channel requestRtpChannel
                    = (ColibriConferenceIQ.Channel) requestChannel;

                copyDescription(content, requestRtpChannel);

                // Config options
                requestRtpChannel.setLastN(channelLastN);
                requestRtpChannel.setSimulcastMode(simulcastMode);
                if (MediaType.AUDIO.equals(mediaType))
                {
                    // When audioPacketDelay is null it will clear the attribute
                    requestRtpChannel.setPacketDelay(audioPacketDelay);
                    // Set rtp packet relay type for this channel
                    requestRtpChannel
                        .setRTPLevelRelayType(rtpLevelRelayType);
                }

                // Copy the sources and source groups
                if (sourceMap != null)
                {
                    addSources(requestRtpChannel, sourceMap.get(contentName));
                }
                if (sourceGroupMap != null)
                {
                    addSourceGroups(
                        requestRtpChannel,
                        sourceGroupMap.get(contentName),
                        MediaType.VIDEO.equals(mediaType));
                }
            }

            if (requestChannel instanceof ColibriConferenceIQ.OctoChannel)
            {
                ((ColibriConferenceIQ.OctoChannel) requestChannel)
                    .setRelays(octoRelayIds);
            }

            // Copy transport
            if (!useBundle)
            {
                copyTransport(content, requestChannel);
            }

            if (requestChannel instanceof ColibriConferenceIQ.Channel)
            {
                requestContent.addChannel(
                    (ColibriConferenceIQ.Channel) requestChannel);
            }
            else
            {
                requestContent.addSctpConnection(
                    (ColibriConferenceIQ.SctpConnection) requestChannel);
            }

            hasAnyChanges = true;
        }

        if (useBundle && endpointId != null && contents.size() >= 1)
        {
            // Copy first transport to bundle
            ColibriConferenceIQ.ChannelBundle bundle
                = new ColibriConferenceIQ.ChannelBundle(endpointId);

            ContentPacketExtension firstContent = contents.get(0);

            IceUdpTransportPacketExtension transport
                = firstContent.getFirstChildOfType(
                        IceUdpTransportPacketExtension.class);
            if (transport != null)
            {
                hasAnyChanges = true;

                bundle.setTransport(
                    IceUdpTransportPacketExtension
                        .cloneTransportAndCandidates(transport, true));
            }

            request.addChannelBundle(bundle);
        }
        else if (useBundle && endpointId == null)
        {
            logger.error("Bundle requested, but no endpointId provided.");
        }

        if (endpointId != null)
        {
            // Set the endpoint
            ColibriConferenceIQ.Endpoint endpoint
                = new ColibriConferenceIQ.Endpoint(endpointId, statsId, null);

            request.addEndpoint(endpoint);
        }

        return hasAnyChanges;
    }

    /**
     * Adds a {@link ColibriConferenceIQ.ChannelBundle} with a specific
     * ID and a specific {@code transport} element to a
     * {@link RequestType#CHANNEL_INFO_UPDATE} query.
     * @param transport the transport element to add to the bundle.
     * @param channelBundleId the ID of the bundle
     * @return {@code true} if the request yields any changes in Colibri
     * channels state on the bridge or {@code false} otherwise. In general when
     * {@code false} is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addBundleTransportUpdateReq(
        IceUdpTransportPacketExtension transport,
        String channelBundleId)
        throws IllegalArgumentException
    {
        Objects.requireNonNull(transport, "transport");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet.

            // XXX by returning false we silently indicate that there is no
            // need to send a packet, while in fact the state of the conference
            // does NOT reflect the information that this method call was
            // supposed to add. At least log something.
            logger.warn("Not adding a transport bundle, not initialized");
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        ColibriConferenceIQ.ChannelBundle channelBundleRequest
            = new ColibriConferenceIQ.ChannelBundle(channelBundleId);

        channelBundleRequest
            .setTransport(
                IceUdpTransportPacketExtension
                    .cloneTransportAndCandidates(transport, true));

        request.addChannelBundle(channelBundleRequest);

        // Note that we don't actually check whether the addition of the bundle
        // made any changes. We return true, because it is safe. It might lead
        // us to send an unnecessary request, but it will not fail to send a
        // request when we need it. We may need to add a check for modification
        // of the content of the channel bundle extension.
        return true;
    }

    /**
     * Adds next expire channel request to {@link RequestType#EXPIRE_CHANNELS}
     * query currently being built.
     * @param channelInfo the {@link ColibriConferenceIQ} instance that contains
     * info about the channels to be expired.
     * @return <tt>true</tt> if the request yields any changes in Colibri
     * channels state on the bridge or <tt>false</tt> otherwise. In general when
     * <tt>false</tt> is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addExpireChannelsReq(ColibriConferenceIQ channelInfo)
    {
        Objects.requireNonNull(channelInfo, "channelInfo");

        // Formulate the ColibriConferenceIQ request which is to be sent.
        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            return false;
        }

        assertRequestType(RequestType.EXPIRE_CHANNELS);

        for (ColibriConferenceIQ.Content expiredContent
            : channelInfo.getContents())
        {
            ColibriConferenceIQ.Content stateContent
                = conferenceState.getContent(expiredContent.getName());

            if (stateContent != null)
            {
                ColibriConferenceIQ.Content requestContent
                    = request.getOrCreateContent(
                            stateContent.getName());

                for (ColibriConferenceIQ.Channel expiredChannel
                    : expiredContent.getChannels())
                {
                    ColibriConferenceIQ.Channel stateChannel
                        = stateContent.getChannel(expiredChannel.getID());

                    if (stateChannel != null)
                    {
                        ColibriConferenceIQ.Channel requestChannel
                            = new ColibriConferenceIQ.Channel();

                        requestChannel.setExpire(0);
                        requestChannel.setID(stateChannel.getID());
                        requestContent.addChannel(requestChannel);

                        hasAnyChannelsToExpire = true;
                    }
                }
                for (ColibriConferenceIQ.SctpConnection expiredConn
                    : expiredContent.getSctpConnections())
                {
                    ColibriConferenceIQ.SctpConnection stateConn
                        = stateContent.getSctpConnection(expiredConn.getID());

                    if (stateConn != null)
                    {
                        ColibriConferenceIQ.SctpConnection requestConn
                            = new ColibriConferenceIQ.SctpConnection();

                        requestConn.setID(stateConn.getID());
                        requestConn.setExpire(0);

                        //FIXME: can be removed once we do not handle SCTP
                        //       by endpoint anymore
                        requestConn.setEndpoint(stateConn.getEndpoint());

                        requestContent.addSctpConnection(requestConn);

                        hasAnyChannelsToExpire = true;
                    }
                }
            }
        }

        /*
         * Remove the channels which are to be expired from the internal
         * state of the conference managed by this ColibriBuilder.
         */
        for (ColibriConferenceIQ.Content requestContent
            : request.getContents())
        {
            ColibriConferenceIQ.Content stateContent
                = conferenceState.getContent(requestContent.getName());

            for (ColibriConferenceIQ.Channel requestChannel
                    : requestContent.getChannels())
            {
                ColibriConferenceIQ.Channel stateChannel
                    = stateContent.getChannel(requestChannel.getID());

                stateContent.removeChannel(stateChannel);

                /*
                 * If the last remote channel is to be expired, expire
                 * the local channel as well.
                 */
                /*if (stateContent.getChannelCount() == 1)
                {
                    stateChannel = stateContent.getRtpChannel(0);

                    ColibriConferenceIQ.Channel requestChannel
                        = new ColibriConferenceIQ.Channel();
                    requestChannel.setExpire(0);
                    requestChannel.setID(stateChannel.getID());
                    requestContent.addChannel(requestChannel);

                    hasAnyChannelsToExpire = true;

                    stateContent.removeChannel(stateChannel);
                    break;
                }*/
            }
            for (ColibriConferenceIQ.SctpConnection requestConn
                : requestContent.getSctpConnections())
            {
                ColibriConferenceIQ.SctpConnection stateConn
                    = stateContent.getSctpConnection(
                            requestConn.getID());

                stateContent.removeSctpConnection(stateConn);

                /*
                 * If the last remote channel is to be expired, expire
                 * the local channel as well.
                 */
                /*if (stateContent.getSctpConnections().size() == 1)
                {
                    stateConn = stateContent.getSctpConnections().get(0);

                    ColibriConferenceIQ.SctpConnection requestConn
                        = new ColibriConferenceIQ.SctpConnection();
                    requestConn.setID(stateConn.getID());
                    requestConn.setExpire(0);
                    requestConn.setEndpoint(stateConn.getEndpoint());
                    requestContent.addSctpConnection(requestConn);

                    hasAnyChannelsToExpire = true;

                    stateContent.removeSctpConnection(stateConn);

                    break;
                }*/
            }
        }

        return hasAnyChannelsToExpire;
    }

    /**
     * Adds an {@link RtpDescriptionPacketExtension} to a specific channel in
     * the request which is currently being built. The channel is identified
     * by the content name and channel ID.
     *
     * @param description the {@link RtpDescriptionPacketExtension} to add.
     * @param contentName the name of the content to which the channel belongs.
     * @param channelId the ID of the channel.
     *
     * @return {@code true} if the request yields any changes in Colibri
     * channels state on the bridge or {@code false} otherwise. In general when
     * {@code false} is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addRtpDescription(
        RtpDescriptionPacketExtension description,
        String contentName, String channelId)
    {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(contentName, "contentName");
        Objects.requireNonNull(channelId, "channelId");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet

            // XXX by returning false we silently indicate that there is no
            // need to send a packet, while in fact the state of the conference
            // does NOT reflect the information that this method call was
            // supposed to add. At least log something.
            logger.warn("Not adding description to a channel, not initialized");
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        ColibriConferenceIQ.Channel requestChannel
            = getRequestChannel(contentName, channelId);
        return copyDescription(description, requestChannel);
    }

    /**
     * Adds next source information update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * @param sourceMap the map of content name to the list of
     * <tt>SourcePacketExtension</tt>.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     * Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     * channels state on the bridge or <tt>false</tt> otherwise. In general when
     * <tt>false</tt> is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addSourceInfo(
        Map<String, List<SourcePacketExtension>>    sourceMap,
        ColibriConferenceIQ                         localChannelsInfo)
    {
        Objects.requireNonNull(sourceMap, "sourceMap");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        boolean hasAnyChanges = false;

        // Go over sources
        for (String contentName : sourceMap.keySet())
        {
            // Get channel from local channel info
            ColibriConferenceIQ.ChannelCommon channel
                = getChannel(localChannelsInfo, contentName);
            if (channel == null)
            {
                // There's no channel for this content name in localChannelsInfo
                continue;
            }

            hasAnyChanges = true;

            // Ok we have channel for this content, let's add sources
            ColibriConferenceIQ.Channel requestChannel
                = getRequestChannel(contentName, channel.getID());

            addSources(requestChannel, sourceMap.get(contentName));
        }

        return hasAnyChanges;
    }

    /**
     * Adds next source group information update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * @param sourceGroupMap the map of content name to the list of
     * <tt>SourceGroupPacketExtension</tt>.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     * Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     * channels state on the bridge or <tt>false</tt> otherwise. In general when
     * <tt>false</tt> is returned for all combined requests it makes no sense
     * to send it.
     */
    public boolean addSourceGroupsInfo(
        Map<String, List<SourceGroupPacketExtension>>    sourceGroupMap,
        ColibriConferenceIQ                              localChannelsInfo)
    {
        Objects.requireNonNull(sourceGroupMap, "sourceGroupMap");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        boolean hasAnyChanges = false;

        // Go over source groups
        for (String contentName : sourceGroupMap.keySet())
        {
            // Get channel from local channel info
            ColibriConferenceIQ.Channel channel
                = getChannel(localChannelsInfo, contentName);
            if (channel == null)
            {
                // There's no channel for this content name in localChannelsInfo
                continue;
            }

            // Ok we have channel for this content, let's add sources
            ColibriConferenceIQ.Channel requestChannel
                = getRequestChannel(contentName, channel.getID());

            hasAnyChanges
                |= addSourceGroups(
                    requestChannel,
                    sourceGroupMap.get(contentName),
                    "video".equalsIgnoreCase(contentName));
        }

        return hasAnyChanges;
    }

    /**
     * Adds next ICE transport update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * Note that this should only be used for channels which do NOT use bundle.
     * For the bundle case use {@link #addBundleTransportUpdateReq} instead.
     *
     * @param transportMap the transportMap of content name to transport extensions. Maps
     * transport to media types.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     * Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     * channels state on the bridge or <tt>false</tt> otherwise. In general when
     * <tt>false</tt> is returned for all combined requests it makes no sense to
     * send it.
     */
    public boolean addTransportUpdateReq(
            Map<String, IceUdpTransportPacketExtension>    transportMap,
            ColibriConferenceIQ                            localChannelsInfo)
    {
        Objects.requireNonNull(transportMap, "transportMap");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        boolean hasAnyChanges = false;

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        for (Map.Entry<String,IceUdpTransportPacketExtension> e
            : transportMap.entrySet())
        {
            String contentName = e.getKey();
            ColibriConferenceIQ.ChannelCommon channel
                = getChannelCommon(localChannelsInfo, contentName);

            if (channel != null)
            {
                IceUdpTransportPacketExtension transport
                    = IceUdpTransportPacketExtension
                        .cloneTransportAndCandidates(e.getValue(), true);

                ColibriConferenceIQ.ChannelCommon requestChannel
                    = channel instanceof ColibriConferenceIQ.Channel
                        ? new ColibriConferenceIQ.Channel()
                        : new ColibriConferenceIQ.SctpConnection();

                requestChannel.setID(channel.getID());
                requestChannel.setEndpoint(channel.getEndpoint());
                requestChannel.setTransport(transport);

                request.getOrCreateContent(contentName)
                       .addChannelCommon(requestChannel);

                hasAnyChanges = true;
            }
        }
        return hasAnyChanges;
    }
    /**
     * Adds next request to {@link RequestType#CHANNEL_INFO_UPDATE} query.
     *
     * @param mediaDirectionMap a map of content name to media direction.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     * Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     * channels state on the bridge or <tt>false</tt> otherwise. In general when
     * <tt>false</tt> is returned for all combined requests it makes no sense to
     * send it.
     *
     * @deprecated Please refactor accordingly if/when this API starts to be
     * used.
     */
    public boolean addDirectionUpdateReq(
            Map<String, MediaDirection> mediaDirectionMap,
            ColibriConferenceIQ         localChannelsInfo)
    {
        Objects.requireNonNull(mediaDirectionMap, "mediaDirectionMap");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        boolean hasAnyChanges = false;

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        for (String contentName : mediaDirectionMap.keySet())
        {
            ColibriConferenceIQ.Content content
                = localChannelsInfo.getContent(contentName);
            if (content != null)
            {
                for (ColibriConferenceIQ.Channel channel : content.getChannels())
                {
                    ColibriConferenceIQ.Channel requestChannel
                        = getRequestChannel(contentName, channel.getID());

                    requestChannel
                        .setDirection(mediaDirectionMap.get(contentName));

                    hasAnyChanges = true;
                }
            }
        }
        return hasAnyChanges;
    }

    /**
     * Finds the first {@link ColibriConferenceIQ.ChannelCommon} with a given
     * content name in a specific {@link ColibriConferenceIQ}.
     * @param colibriConferenceIQ the {@link ColibriConferenceIQ} from which
     * to find a channel.
     * @param contentName the name of the content.
     * @return the first {@link ColibriConferenceIQ.ChannelCommon} in the
     * content with name {@code contentName} in {@code colibriConferenceIQ}.
     */
    private ColibriConferenceIQ.ChannelCommon getChannelCommon(
            ColibriConferenceIQ colibriConferenceIQ, String contentName)
    {
        ColibriConferenceIQ.Content content
            = colibriConferenceIQ.getContent(contentName);

        if (content == null)
        {
            return null;
        }

        if (content.getChannelCount() > 0)
        {
            return content.getChannel(0);
        }

        if (content.getSctpConnections().size() > 0)
        {
            return content.getSctpConnections().get(0);
        }

        return null;
    }

    /**
     * Finds the first {@link ColibriConferenceIQ.Channel} with a given content
     * name in a specific {@link ColibriConferenceIQ}.
     * @param localChannelsInfo the {@link ColibriConferenceIQ} from which
     * to find a channel.
     * @param contentName the name of the content.
     * @return the first {@link ColibriConferenceIQ.Channel} in the
     * content with name {@code contentName} in {@code colibriConferenceIQ}.
     */
    private ColibriConferenceIQ.Channel getChannel(
        ColibriConferenceIQ    localChannelsInfo,
        String                 contentName)
    {
        ColibriConferenceIQ.ChannelCommon channel
            = getChannelCommon(localChannelsInfo, contentName);
        return
            channel instanceof ColibriConferenceIQ.Channel
                ? (ColibriConferenceIQ.Channel) channel : null;
    }


    /**
     * Finishes query construction and returns it. It does not reset this
     * instance state and {@link #reset()} must be called to start new query.
     * Otherwise we can continue adding next requests into current query.
     * @param videobridge the JID of videobridge to which this query is
     *                    directed.
     * @return constructed query directed to given <tt>videobridge</tt>. If
     *         request type of current query is
     *         {@link RequestType#EXPIRE_CHANNELS} and there are no channels to
     *         be expired then <tt>null</tt> is returned which signals that
     *         there's nothing to be done.
     */
    public ColibriConferenceIQ getRequest(Jid videobridge)
    {
        Objects.requireNonNull(videobridge);

        request.setTo(videobridge);

        if (requestType == RequestType.EXPIRE_CHANNELS)
        {
            if (!hasAnyChannelsToExpire)
            {
                return null;
            }

            hasAnyChannelsToExpire = false;
        }

        return request;
    }

    /**
     * Returns {@link ColibriBuilder.RequestType} of the query currently being
     * constructed or {@link RequestType#UNDEFINED} if no query construction has
     * been started yet.
     */
    public RequestType getRequestType()
    {
        return requestType;
    }

    /**
     * Makes sure that we do not modify the request type after construction has
     * been started.
     *
     * @param currentReqType the current request type to be used.
     */
    private void assertRequestType(RequestType currentReqType)
    {
        if (requestType == RequestType.UNDEFINED)
        {
            requestType = currentReqType;
        }
        if (requestType != currentReqType)
        {
            throw new IllegalStateException(
                "Request type already set to " + requestType
                    + ", can not change to " + currentReqType);
        }
    }

    /**
     * Returns <tt>true</tt> if {@link RequestType#EXPIRE_CHANNELS} request is
     * being constructed and there are valid channels to be expired.
     */
    public boolean hasAnyChannelsToExpire()
    {
        return hasAnyChannelsToExpire;
    }

    /**
     * Channel 'last-n' option that will be added when channels are created.
     * Set to <tt>null</tt> in order to omit. Value is reset after
     * {@link #reset} is called.
     *
     * @return an integer value or <tt>null</tt> if option is unspecified.
     */
    public Integer getChannelLastN()
    {
        return channelLastN;
    }

    /**
     * Sets channel 'last-n' option that will be added to the request when
     * channels are created.
     * @param channelLastN an integer value to specify 'last-n' option or
     *        <tt>null</tt> in order to omit in requests.
     */
    public void setChannelLastN(Integer channelLastN)
    {
        this.channelLastN = channelLastN;
    }

    /**
     * Returns an <tt>Integer</tt> which stands for the audio packet delay
     * that will be set on all created audio channels or <tt>null</tt> if
     * the builder should leave not include the XML attribute at all.
     */
    public Integer getAudioPacketDelay()
    {
        return audioPacketDelay;
    }

    /**
     * Configures audio channels packet delay.
     * @param audioPacketDelay an <tt>Integer</tt> value which stands for
     * the audio packet delay that will be set on all created audio channels or
     * <tt>null</tt> if the builder should not set that channel property to any
     * value.
     */
    public void setAudioPacketDelay(Integer audioPacketDelay)
    {
        this.audioPacketDelay = audioPacketDelay;
    }

    /**
     * Sets channel 'simulcast-mode' option that will be added to the
     * request when channels are created.
     * @param simulcastMode a <tt>SimulcastMode</tt> value to specify
     *        'simulcast-mode' option or <tt>null</tt> in order to omit in
     *        requests.
     */
    public void setSimulcastMode(SimulcastMode simulcastMode)
    {
        this.simulcastMode = simulcastMode;
    }

    /**
     * Returns the channel from {@link #request} from a particular content
     * and with a particular ID. If the request does not contain such a channel,
     * a new instance is created and added to {@link #request}, and it is
     * initialized only with the fields required to identify the particular
     * Colibri channel on the bridge (i.e. just the ID is specified).
     *
     * The returned value is always a non-null instance of
     * {@link ColibriConferenceIQ.Channel} if {@code sctpConnection} is
     * {@code false} and a non-null instance of
     * {@link ColibriConferenceIQ.SctpConnection} otherwise.
     *
     * @param contentName the name of the content.
     * @param channelId the ID of the channel.
     * @param sctpConnection whether to create a
     * {@link ColibriConferenceIQ.SctpConnection} or a
     * {@link ColibriConferenceIQ.Channel}, if a new channel is to be created.
     *
     * @return the channel from {@link #request} with the specified content
     * name and ID.
     */
    private ColibriConferenceIQ.ChannelCommon getRequestChannel(
            String contentName, String channelId, boolean sctpConnection)
    {
        ColibriConferenceIQ.Content requestContent
            = request.getOrCreateContent(contentName);
        ColibriConferenceIQ.ChannelCommon requestChannel
            = requestContent.getChannel(channelId);
        if (requestChannel == null)
        {
            if (sctpConnection)
            {
                requestChannel = new ColibriConferenceIQ.SctpConnection();
            }
            else
            {
                requestChannel = new ColibriConferenceIQ.Channel();
            }

            requestChannel.setID(channelId);
            requestContent.addChannelCommon(requestChannel);
        }
        else if (
            (sctpConnection && !(requestChannel
                    instanceof ColibriConferenceIQ.SctpConnection))
            || (!sctpConnection && !(requestChannel
                    instanceof ColibriConferenceIQ.Channel)))
        {
            throw new IllegalStateException(
                "Channel type mismatch: requested sctpConnection="
                    + sctpConnection + ", found a channel of class "
                    + requestChannel.getClass().getSimpleName());
        }

        return requestChannel;
    }

    /**
     * Returns the channel from {@link #request} from a particular content
     * and with a particular ID. If the request does not contain such a channel,
     * a new instance is created and added to {@link #request}, and it is
     * initialized only with the fields required to identify the particular
     * Colibri channel on the bridge (i.e. just the ID is specified).
     *
     * @param contentName the name of the content.
     * @param channelId the ID of the channel.
     *
     * @return the channel from {@link #request} with the specified content
     * name and ID.
     */
    private ColibriConferenceIQ.Channel getRequestChannel(
        String contentName, String channelId)
    {
        return
            (ColibriConferenceIQ.Channel)
                getRequestChannel(contentName, channelId, false);
    }

    /**
     * Returns the {@link ColibriConferenceIQ.SctpConnection} from
     * {@link #request} from a particular content and with a particular ID. If
     * the request does not contain such a channel, a new instance is created
     * and added to {@link #request}, and it is initialized only with the
     * fields required to identify the particular Colibri channel on the
     * bridge (i.e. just the ID is specified).
     *
     * @param contentName the name of the content.
     * @param channelId the ID of the {@link ColibriConferenceIQ.SctpConnection}.
     *
     * @return the channel from {@link #request} with the specified content
     * name and ID.
     */
    private ColibriConferenceIQ.SctpConnection getRequestSctpConnection(
        String contentName, String channelId)
    {
        return
            (ColibriConferenceIQ.SctpConnection)
                getRequestChannel(contentName, channelId, true);
    }

    /**
     * Configures RTP-level relay (RFC 3550, section 2.3).
     * @param rtpLevelRelayType an <tt>RTPLevelRelayType</tt> value which
     * stands for the rtp level relay type that will be set on all created
     * audio channels.
     */
    public void setRTPLevelRelayType(RTPLevelRelayType rtpLevelRelayType)
    {
        this.rtpLevelRelayType = rtpLevelRelayType;
    }

    /**
     * Configures RTP-level relay (RFC 3550, section 2.3).
     * @param rtpLevelRelayType a <tt>String</tt> value which
     * stands for the rtp level relay type that will be set on all created
     * audio channels.
     */
    public void setRTPLevelRelayType(String rtpLevelRelayType)
    {
        setRTPLevelRelayType
                (RTPLevelRelayType.parseRTPLevelRelayType(rtpLevelRelayType));
    }

    /**
     * The types of request that can be built with {@link ColibriBuilder}.
     */
    public enum RequestType
    {
        /**
         * Allocates new channels on the bridge request.
         */
        ALLOCATE_CHANNELS,

        /**
         * An update request which is meant to modify some values of existing
         * Colibri channels on the bridge.
         */
        CHANNEL_INFO_UPDATE,

        /**
         * Expires specified Colibri channels.
         */
        EXPIRE_CHANNELS,

        /**
         * Undefined means {@link ColibriBuilder} "zero" state. The point where
         * builder instance can be used to start next query.
         */
        UNDEFINED;
    }
}
