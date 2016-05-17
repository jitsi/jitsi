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

import java.util.*;

/**
 * Utility class for building Colibri queries. It can be used to allocate,
 * expire Colibri channels and update it's ICE transport info on the bridge.
 * The flow is as follows:<br/>
 * <ol>
 *     <li>
 * Add one or multiple requests of the same type by calling
 * {@link #addAllocateChannelsReq(boolean, String, boolean, java.util.List)}}
 * or {@link #addExpireChannelsReq(ColibriConferenceIQ)}
 * or {@link #addRtpDescription(Map, ColibriConferenceIQ)}
 * and {@link #addSSSRCGroupsInfo(Map, ColibriConferenceIQ)}
 * and {@link #addSSSRCInfo(Map, ColibriConferenceIQ)}.
 *     </li>
 *     <li>
 * Compile the request by calling {@link #getRequest(String)}. Then send it to
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
 */
public class ColibriBuilder
{
    /**
     * The logger used by this instance.
     */
    private final static Logger logger = Logger.getLogger(ColibriBuilder.class);

    /**
     * Colibri IQ that holds conference state.
     */
    private final ColibriConferenceIQ conferenceState;

    /**
     * The type of the request currently being build. <tt>null</tt> if this
     * instance is in the "zero" state. To get into the "zero" state call
     * {@link #reset()}.
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
     * Channel 'adaptive-last-n' option that will be added when channels are
     * created.
     * Set to <tt>null</tt> in order to omit.
     */
    private Boolean adaptiveLastN;

    /**
     * Channel 'adaptive-simulcast' option that will be added when channels are
     * created.
     * Set to <tt>null</tt> in order to omit.
     */
    private Boolean adaptiveSimulcast;

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
     * audio channel. Possible values: mixer or translator (default).
     *
     */
    private RTPLevelRelayType rtpLevelRelayType;

    /**
     * Creates new instance of {@link ColibriBuilder} for given
     * <tt>conferenceState</tt>.
     *
     * @param conferenceState the conference state which will be taken into
     *        account when constructing specific requests(the need to
     *        allocate new conference or re-use the one currently in progress).
     */
    public ColibriBuilder(ColibriConferenceIQ conferenceState)
    {
        this.conferenceState = conferenceState;

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
    }

    /**
     * Adds next channel allocation request to
     * {@link RequestType#ALLOCATE_CHANNELS} query currently being built.
     *
     * @param useBundle <tt>true</tt> if allocated channels wil use RTP bundle.
     * @param endpointName name od the endpoint for which Colibri channels will
     *        be allocated.
     * @param peerIsInitiator the value that will be set in 'initiator'
     *        attribute({@link ColibriConferenceIQ.Channel#initiator}).
     * @param contents the list of {@link ContentPacketExtension} describing
     *                 channels media.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
     */
    public boolean addAllocateChannelsReq(
            boolean                      useBundle,
            String                       endpointName,
            boolean                      peerIsInitiator,
            List<ContentPacketExtension> contents)
    {
        Objects.requireNonNull(endpointName, "endpointName");
        Objects.requireNonNull(contents, "contents");

        assertRequestType(RequestType.ALLOCATE_CHANNELS);

        request.setType(IQ.Type.GET);

        boolean hasAnyChanges = false;

        for (ContentPacketExtension cpe : contents)
        {
            MediaType mediaType = JingleUtils.getMediaType(cpe);
            String contentName = mediaType.toString();
            ColibriConferenceIQ.Content contentRequest
                = request.getOrCreateContent(contentName);

            ColibriConferenceIQ.ChannelCommon remoteChannelRequest
                = mediaType != MediaType.DATA
                        ? new ColibriConferenceIQ.Channel()
                        : new ColibriConferenceIQ.SctpConnection();

            remoteChannelRequest.setEndpoint(endpointName);
            remoteChannelRequest.setInitiator(peerIsInitiator);

            if (useBundle)
            {
                remoteChannelRequest.setChannelBundleId(endpointName);
            }

            if (remoteChannelRequest instanceof ColibriConferenceIQ.Channel)
            {
                RtpDescriptionPacketExtension rdpe
                    = cpe.getFirstChildOfType(
                            RtpDescriptionPacketExtension.class);

                ColibriConferenceIQ.Channel remoteRtpChannelRequest
                    = (ColibriConferenceIQ.Channel) remoteChannelRequest;

                for (PayloadTypePacketExtension ptpe : rdpe.getPayloadTypes())
                    remoteRtpChannelRequest.addPayloadType(ptpe);

                for (RTPHdrExtPacketExtension ext : rdpe.getExtmapList())
                    remoteRtpChannelRequest.addRtpHeaderExtension(ext);

                // Config options
                remoteRtpChannelRequest.setLastN(channelLastN);
                remoteRtpChannelRequest.setAdaptiveLastN(adaptiveLastN);
                remoteRtpChannelRequest.setAdaptiveSimulcast(adaptiveSimulcast);
                remoteRtpChannelRequest.setSimulcastMode(simulcastMode);
                if (MediaType.AUDIO.equals(mediaType))
                {
                    // When audioPacketDelay is null it will clear the attribute
                    remoteRtpChannelRequest.setPacketDelay(audioPacketDelay);
                    // Set rtp packet relay type for this channel
                    remoteRtpChannelRequest.setRTPLevelRelayType(rtpLevelRelayType);
                }
            }

            // Copy transport
            if (!useBundle)
            {
                copyTransportOnChannel(cpe, remoteChannelRequest);
            }

            if (remoteChannelRequest instanceof ColibriConferenceIQ.Channel)
            {
                hasAnyChanges = true;

                contentRequest.addChannel(
                    (ColibriConferenceIQ.Channel) remoteChannelRequest);
            }
            else
            {
                hasAnyChanges = true;

                contentRequest.addSctpConnection(
                    (ColibriConferenceIQ.SctpConnection) remoteChannelRequest);
            }
        }

        if (useBundle && contents.size() >= 1)
        {
            // Copy first transport to bundle
            ColibriConferenceIQ.ChannelBundle bundle
                = new ColibriConferenceIQ.ChannelBundle(endpointName);

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

        return hasAnyChanges;
    }

    /**
     * Adds next request to {@link RequestType#CHANNEL_INFO_UPDATE} query.
     * @param localChannelsInfo the {@link ColibriConferenceIQ} instance that
     *        describes the channel for which bundle transport will be updated.
     *        It should contain the description of only one "channel bundle".
     *        If it contains more than one then the first one will be used.
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
     * @throws IllegalArgumentException if <tt>localChannelsInfo</tt> does not
     *         describe any channel bundles.
     */
    public boolean addBundleTransportUpdateReq(
        IceUdpTransportPacketExtension    transport,
        ColibriConferenceIQ               localChannelsInfo)
        throws IllegalArgumentException
    {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        request.setType(IQ.Type.SET);

        // We expect single bundle
        ColibriConferenceIQ.ChannelBundle localBundle;
        if (localChannelsInfo.getChannelBundles().size() > 0)
        {
            localBundle = localChannelsInfo.getChannelBundles().get(0);

            if (localChannelsInfo.getChannelBundles().size() > 1)
            {
                logger.error("More than one bundle in local channels info !");
            }
        }
        else
        {
            throw new IllegalArgumentException(
                    "Expected ChannelBundle as not found");
        }

        ColibriConferenceIQ.ChannelBundle bundleUpdate
            = new ColibriConferenceIQ.ChannelBundle(
                    localBundle.getId());

        IceUdpTransportPacketExtension transportUpdate;

        transportUpdate = IceUdpTransportPacketExtension
            .cloneTransportAndCandidates(transport, true);

        bundleUpdate.setTransport(transportUpdate);

        request.addChannelBundle(bundleUpdate);

        return true;
    }

    /**
     * Adds next expire channel request to
     * {@link RequestType#EXPIRE_CHANNELS} query currently being built.
     * @param channelInfo the {@link ColibriConferenceIQ} instance that contains
     *                    info about the channels to be expired.
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
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

        request.setType(IQ.Type.SET);

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
                        ColibriConferenceIQ.Channel channelRequest
                            = new ColibriConferenceIQ.Channel();

                        channelRequest.setExpire(0);
                        channelRequest.setID(stateChannel.getID());
                        requestContent.addChannel(channelRequest);

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
                        ColibriConferenceIQ.SctpConnection connRequest
                            = new ColibriConferenceIQ.SctpConnection();

                        connRequest.setID(stateConn.getID());
                        connRequest.setExpire(0);

                        //FIXME: can be removed once we do not handle SCTP
                        //       by endpoint anymore
                        connRequest.setEndpoint(stateConn.getEndpoint());

                        requestContent.addSctpConnection(connRequest);

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

                    ColibriConferenceIQ.Channel channelRequest
                        = new ColibriConferenceIQ.Channel();
                    channelRequest.setExpire(0);
                    channelRequest.setID(stateChannel.getID());
                    requestContent.addChannel(channelRequest);

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

                    ColibriConferenceIQ.SctpConnection connRequest
                        = new ColibriConferenceIQ.SctpConnection();
                    connRequest.setID(stateConn.getID());
                    connRequest.setExpire(0);
                    connRequest.setEndpoint(stateConn.getEndpoint());
                    requestContent.addSctpConnection(connRequest);

                    hasAnyChannelsToExpire = true;

                    stateContent.removeSctpConnection(stateConn);

                    break;
                }*/
            }
        }

        return hasAnyChannelsToExpire;
    }

    /**
     * Adds next payload type information update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * @param map the map of content name to RTP description packet extension.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     *        Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
     */
    public boolean addRtpDescription(
        Map<String, RtpDescriptionPacketExtension>    map,
        ColibriConferenceIQ                           localChannelsInfo)
    {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        request.setType(IQ.Type.SET);

        boolean anyUpdates = false;

        for (Map.Entry<String, RtpDescriptionPacketExtension> e
            : map.entrySet())
        {
            String contentName = e.getKey();
            ColibriConferenceIQ.ChannelCommon channel
                = getColibriChannel(localChannelsInfo, contentName);

            if (channel != null
                && channel instanceof ColibriConferenceIQ.Channel)
            {
                RtpDescriptionPacketExtension rtpPE = e.getValue();
                if (rtpPE == null)
                {
                    continue;
                }

                List<PayloadTypePacketExtension> pts = rtpPE.getPayloadTypes();
                if (pts == null || pts.isEmpty())
                {
                    continue;
                }

                anyUpdates = true;

                ColibriConferenceIQ.Channel channelRequest
                    = (ColibriConferenceIQ.Channel) getRequestChannel(
                            request.getOrCreateContent(contentName),
                            channel);
                if (channelRequest == null)
                {
                    channelRequest = new ColibriConferenceIQ.Channel();
                    channelRequest.setID(channel.getID());
                }

                for (PayloadTypePacketExtension ptPE : pts)
                {
                    channelRequest.addPayloadType(ptPE);
                }
            }
        }

        return anyUpdates;
    }

    /**
     * Adds next SSRC information update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * @param ssrcMap the map of content name to the list of
     *                <tt>SourcePacketExtension</tt>.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     *        Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
     */
    public boolean addSSSRCInfo(
        Map<String, List<SourcePacketExtension>>    ssrcMap,
        ColibriConferenceIQ                         localChannelsInfo)
    {
        Objects.requireNonNull(ssrcMap, "ssrcMap");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        request.setType(IQ.Type.SET);

        boolean anyUpdates = false;

        // Go over SSRCs
        for (String contentName : ssrcMap.keySet())
        {
            // Get channel from local channel info
            ColibriConferenceIQ.ChannelCommon rtpChanel
                = getRtpChannel(localChannelsInfo, contentName);
            if (rtpChanel == null)
            {
                // There's no channel for this content name in localChannelsInfo
                continue;
            }

            anyUpdates = true;

            // Ok we have channel for this content, let's add SSRCs
            ColibriConferenceIQ.Channel reqChannel
                = (ColibriConferenceIQ.Channel) getRequestChannel(
                        request.getOrCreateContent(contentName), rtpChanel);

            for (SourcePacketExtension ssrc : ssrcMap.get(contentName))
            {
                reqChannel.addSource(ssrc.copy());
            }

            if (reqChannel.getSources() == null
                || reqChannel.getSources().isEmpty())
            {
                // Put an empty source to remove all sources
                SourcePacketExtension emptySource = new SourcePacketExtension();
                emptySource.setSSRC(-1L);
                reqChannel.addSource(emptySource);
            }
        }

        return anyUpdates;
    }

    /**
     * Adds next SSRC group information update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * @param ssrcGroupMap the map of content name to the list of
     *                <tt>SourceGroupPacketExtension</tt>.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     *        Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
     */
    public boolean addSSSRCGroupsInfo(
        Map<String, List<SourceGroupPacketExtension>>    ssrcGroupMap,
        ColibriConferenceIQ                              localChannelsInfo)
    {
        Objects.requireNonNull(ssrcGroupMap, "ssrcGroupMap");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        request.setType(IQ.Type.SET);

        boolean anyUpdates = false;

        // Go over SSRC groups
        for (String contentName : ssrcGroupMap.keySet())
        {
            // Get channel from local channel info
            ColibriConferenceIQ.Channel rtpChannel
                = getRtpChannel(localChannelsInfo, contentName);
            if (rtpChannel == null)
            {
                // There's no channel for this content name in localChannelsInfo
                continue;
            }

            List<SourceGroupPacketExtension> groups
                = ssrcGroupMap.get(contentName);

            // Ok we have channel for this content, let's add SSRCs
            ColibriConferenceIQ.Channel reqChannel
                = (ColibriConferenceIQ.Channel) getRequestChannel(
                    request.getOrCreateContent(contentName), rtpChannel);

            if (groups.isEmpty() && "video".equalsIgnoreCase(contentName))
            {
                anyUpdates = true;

                // Put empty source group to turn off simulcast layers
                reqChannel.addSourceGroup(
                        SourceGroupPacketExtension.createSimulcastGroup());
            }

            for (SourceGroupPacketExtension group : groups)
            {
                anyUpdates = true;

                reqChannel.addSourceGroup(group);
            }
        }

        return anyUpdates;
    }

    /**
     * Adds next ICE transport update request to
     * {@link RequestType#CHANNEL_INFO_UPDATE} query currently being built.
     *
     * @param map the map of content name to transport extensions. Maps
     *        transport to media types.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     *        Colibri channels to be updated.
     *
     * @return <tt>true</tt> if the request yields any changes in Colibri
     *         channels state on the bridge or <tt>false</tt> otherwise.
     *         In general when <tt>false</tt> is returned for all
     *         combined requests it makes no sense to send it.
     */
    public boolean addTransportUpdateReq(
            Map<String, IceUdpTransportPacketExtension>    map,
            ColibriConferenceIQ                            localChannelsInfo)
    {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(localChannelsInfo, "localChannelsInfo");

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return false;
        }

        boolean hasAnyChanges = false;

        assertRequestType(RequestType.CHANNEL_INFO_UPDATE);

        request.setType(IQ.Type.SET);

        for (Map.Entry<String,IceUdpTransportPacketExtension> e
            : map.entrySet())
        {
            String contentName = e.getKey();
            ColibriConferenceIQ.ChannelCommon channel
                = getColibriChannel(localChannelsInfo, contentName);

            if (channel != null)
            {
                IceUdpTransportPacketExtension transport
                    = IceUdpTransportPacketExtension
                        .cloneTransportAndCandidates(e.getValue(), true);

                ColibriConferenceIQ.ChannelCommon channelRequest
                    = channel instanceof ColibriConferenceIQ.Channel
                        ? new ColibriConferenceIQ.Channel()
                        : new ColibriConferenceIQ.SctpConnection();

                channelRequest.setID(channel.getID());
                channelRequest.setEndpoint(channel.getEndpoint());
                channelRequest.setTransport(transport);

                request.getOrCreateContent(contentName)
                       .addChannelCommon(channelRequest);

                hasAnyChanges = true;
            }
        }
        return hasAnyChanges;
    }

    /**
     * Finds channel in <tt>localChannels</tt> info for given
     * <tt>contentName</tt>.
     */
    private ColibriConferenceIQ.ChannelCommon getColibriChannel(
            ColibriConferenceIQ localChannels, String contentName)
    {
        ColibriConferenceIQ.Content content
            = localChannels.getContent(contentName);

        if (content == null)
            return null;

        if (content.getChannelCount() > 0)
            return content.getChannel(0);

        if (content.getSctpConnections().size() > 0)
            return content.getSctpConnections().get(0);

        return null;
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
    public ColibriConferenceIQ getRequest(String videobridge)
    {
        if (StringUtils.isNullOrEmpty(videobridge))
        {
            throw new NullPointerException("videobridge");
        }

        request.setTo(videobridge);

        if (requestType == RequestType.EXPIRE_CHANNELS)
        {
            if (!hasAnyChannelsToExpire)
                return null;

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
     * Makes sure that we do not modify request which construction has been
     * started.
     *
     * @param currentReqType the current request type to be used.
     */
    private void assertRequestType(RequestType currentReqType)
    {
        if (requestType == RequestType.UNDEFINED)
        {
            requestType = currentReqType;
        }
        if (requestType !=currentReqType)
        {
            throw new IllegalStateException("Current request type");
        }
    }

    /**
     * Copies transport info from <tt>ContentPacketExtension</tt> to
     * <tt>ColibriConferenceIQ#ChannelCommon</tt>.
     */
    private void copyTransportOnChannel(
        ContentPacketExtension content,
        ColibriConferenceIQ.ChannelCommon remoteChannelRequest)
    {
        IceUdpTransportPacketExtension iceUdpTransportPacketExtension
            = content.getFirstChildOfType(
                    IceUdpTransportPacketExtension.class);

        IceUdpTransportPacketExtension iceUdpCopy
            = IceUdpTransportPacketExtension
                    .cloneTransportAndCandidates(
                        iceUdpTransportPacketExtension, true);

        remoteChannelRequest.setTransport(iceUdpCopy);
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
     * Channel 'adaptive-last-n' option that will be added when channels are
     * created.
     * Set to <tt>null</tt> in order to omit. Value is reset after
     * {@link #reset} is called.
     *
     * @return a boolean value or <tt>null</tt> if option is unspecified.
     */
    public Boolean getAdaptiveLastN()
    {
        return adaptiveLastN;
    }

    /**
     * Sets channel 'adaptive-last-n' option that will be added to the request
     * when channels are created.
     *
     * @param adaptiveLastN a boolean value to specify 'adaptive-last-n' option
     *                      or <tt>null</tt> in order  to omit in requests.
     */
    public void setAdaptiveLastN(Boolean adaptiveLastN)
    {
        this.adaptiveLastN = adaptiveLastN;
    }

    /**
     * Channel 'adaptive-simulcast' option that will be added when channels are
     * created. Set to <tt>null</tt> in order to omit.
     *
     * @return a boolean value or <tt>null</tt> if option is unspecified.
     */
    public Boolean getAdaptiveSimulcast()
    {
        return adaptiveSimulcast;
    }

    /**
     * Sets channel 'adaptive-simulcast' option that will be added to the
     * request when channels are created.
     * @param adaptiveSimulcast a boolean value to specify 'adaptive-simulcast'
     *        option or <tt>null</tt> in order to omit in requests.
     */
    public void setAdaptiveSimulcast(Boolean adaptiveSimulcast)
    {
        this.adaptiveSimulcast = adaptiveSimulcast;
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
     * Creates a new instance of <tt>localChannelInfo</tt> and initializes only
     * the fields required to identify particular Colibri channel on the bridge.
     * This instance is meant to be used in Colibri
     * {@link RequestType#CHANNEL_INFO_UPDATE} requests. This instance is also
     * added to given <tt>requestContent</tt> which used to construct current
     * request.
     *
     * @param requestContent <tt>Content</tt> of Colibri update request to which
     *        new instance wil be automatically added after has been created.
     * @param localChannelInfo the original channel for which "update request"
     *        equivalent is to be created with this call.
     *
     * @return new instance of <tt>localChannelInfo</tt> and initialized with
     * only those fields required to identify particular Colibri channel on
     * the bridge.
     */
    private ColibriConferenceIQ.ChannelCommon getRequestChannel(
        ColibriConferenceIQ.Content          requestContent,
        ColibriConferenceIQ.ChannelCommon    localChannelInfo)
    {
        ColibriConferenceIQ.ChannelCommon reqChannel
            = requestContent.getChannel(localChannelInfo.getID());
        if (reqChannel == null)
        {
            if (localChannelInfo instanceof ColibriConferenceIQ.Channel)
            {
                reqChannel = new ColibriConferenceIQ.Channel();
            }
            else if (
                localChannelInfo instanceof ColibriConferenceIQ.SctpConnection)
            {
                reqChannel = new ColibriConferenceIQ.SctpConnection();
            }
            else
            {
                throw new RuntimeException(
                        "Unsupported ChannelCommon class: "
                            + localChannelInfo.getClass());
            }

            reqChannel.setID(localChannelInfo.getID());

            requestContent.addChannelCommon(reqChannel);
        }
        return reqChannel;
    }

    private ColibriConferenceIQ.Channel getRtpChannel(
            ColibriConferenceIQ    localChannelsInfo,
            String                 contentName)
    {
        ColibriConferenceIQ.Content content
            = localChannelsInfo.getContent(contentName);

        if (content == null)
            return null;

        return content.getChannelCount() > 0 ? content.getChannel(0) : null;
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

}
