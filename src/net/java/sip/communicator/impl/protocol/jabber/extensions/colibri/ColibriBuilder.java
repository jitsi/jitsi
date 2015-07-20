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
 * or {@link #addTransportUpdateReq(boolean, java.util.Map, ColibriConferenceIQ)}
 * or {@link #addBundleTransportUpdateReq(
 * boolean, IceUdpTransportPacketExtension, ColibriConferenceIQ)}.
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
     * @return this instance fo calls chaining purpose.
     */
    public ColibriBuilder addAllocateChannelsReq(
            boolean useBundle,
            String endpointName,
            boolean peerIsInitiator,
            List<ContentPacketExtension> contents)
    {
        assertRequestType(RequestType.ALLOCATE_CHANNELS);

        request.setType(IQ.Type.GET);

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

            if (mediaType != MediaType.DATA)
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
            }

            // Copy transport
            if (!useBundle)
            {
                copyTransportOnChannel(cpe, remoteChannelRequest);
            }

            if (mediaType != MediaType.DATA)
            {
                contentRequest.addChannel(
                    (ColibriConferenceIQ.Channel) remoteChannelRequest);
            }
            else
            {
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
                bundle.setTransport(
                    IceUdpTransportPacketExtension
                        .cloneTransportAndCandidates(transport, true));
            }

            request.addChannelBundle(bundle);
        }

        return this;
    }

    /**
     * Adds next ICE transport update request to
     * {@link RequestType#TRANSPORT_UPDATE} query currently being built.
     *
     * @param initiator the value that will be set in 'initiator'
     *        attribute({@link ColibriConferenceIQ.Channel#initiator}).
     * @param map the map of content name to transport extensions. Maps
     *        transport to media types.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     *        Colibri channels to be updated.
     *
     * @return this instance fo calls chaining purpose.
     */
    public ColibriBuilder addTransportUpdateReq(
        boolean initiator,
        Map<String, IceUdpTransportPacketExtension> map,
        ColibriConferenceIQ localChannelsInfo)
    {
        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return null;
        }

        assertRequestType(RequestType.TRANSPORT_UPDATE);

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
                channelRequest.setInitiator(initiator);
                channelRequest.setTransport(transport);

                if (channelRequest instanceof ColibriConferenceIQ.Channel)
                {
                    request.getOrCreateContent(contentName)
                        .addChannel(
                            (ColibriConferenceIQ.Channel) channelRequest);
                }
                else
                {
                    request.getOrCreateContent(contentName)
                        .addSctpConnection(
                        (ColibriConferenceIQ.SctpConnection) channelRequest);
                }
            }
        }
        return this;
    }

    /**
     * Adds next request to {@link RequestType#BUNDLE_TRANSPORT_UPDATE} query.
     * @param initiator the value that will be set in 'initiator'
     *        attribute({@link ColibriConferenceIQ.Channel#initiator}).
     * @param localChannelsInfo the {@link ColibriConferenceIQ} instance that
     *        describes the channel for which bundle transport will be updated.
     *        It should contain the description of only one "channel bundle".
     *        If it contains more than one then the first one will be used.
     * @return this instance for calls chaining purpose.
     * @throws IllegalArgumentException if <tt>localChannelsInfo</tt> does not
     *         describe any channel bundles.
     */
    public ColibriBuilder addBundleTransportUpdateReq(
            boolean initiator,
            IceUdpTransportPacketExtension transport,
            ColibriConferenceIQ localChannelsInfo)
        throws IllegalArgumentException
    {
        // FIXME:'initiator' not used on bundle transport update ?

        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return null;
        }

        assertRequestType(RequestType.BUNDLE_TRANSPORT_UPDATE);

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

        return this;
    }

    /**
     * Adds next expire channel request to
     * {@link RequestType#EXPIRE_CHANNELS} query currently being built.
     * @param channelInfo the {@link ColibriConferenceIQ} instance that contains
     *                    info about the channels to be expired.
     * @return this instance for the purpose of calls chaining.
     */
    public ColibriBuilder addExpireChannelsReq(ColibriConferenceIQ channelInfo)
    {
        // Formulate the ColibriConferenceIQ request which is to be sent.
        if (conferenceState == null
            || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            return null;
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
                    stateChannel = stateContent.getChannel(0);

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

        return this;
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

        if (requestType == RequestType.EXPIRE_CHANNELS
            && !hasAnyChannelsToExpire)
        {
            return null;
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
     * Adds next payload type information update request to
     * {@link RequestType#RTP_DESCRIPTION_UPDATE} query currently being built.
     *
     * @param map the map of content name to RTP description packet extension.
     * @param localChannelsInfo {@link ColibriConferenceIQ} holding info about
     *        Colibri channels to be updated.
     *
     * @return this instance for calls chaining purpose.
     */
    public ColibriBuilder addRtpDescription(
            Map<String, RtpDescriptionPacketExtension> map,
            ColibriConferenceIQ localChannelsInfo) {

        if (conferenceState == null
                || StringUtils.isNullOrEmpty(conferenceState.getID()))
        {
            // We are not initialized yet
            return null;
        }

        assertRequestType(RequestType.RTP_DESCRIPTION_UPDATE);

        request.setType(IQ.Type.SET);

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

                ColibriConferenceIQ.Channel channelRequest
                        = new ColibriConferenceIQ.Channel();

                channelRequest.setID(channel.getID());

                for (PayloadTypePacketExtension ptPE : rtpPE.getPayloadTypes())
                {
                    channelRequest.addPayloadType(ptPE);
                }

                request.getOrCreateContent(contentName)
                        .addChannel(channelRequest);
            }

        }

        return this;
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
         * Updates transport information for channels that use RTP bundle.
         */
        BUNDLE_TRANSPORT_UPDATE,

        /**
         * Updates channel transport information(ICE transport candidates).
         */
        TRANSPORT_UPDATE,

        /**
         * Updates the RTP description of a channel (payload types).
         */
        RTP_DESCRIPTION_UPDATE,

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
