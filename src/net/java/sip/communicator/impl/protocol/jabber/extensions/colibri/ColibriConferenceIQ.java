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

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jitsi.util.*;
import org.jitsi.service.neomedia.*;

import org.jivesoftware.smack.packet.*;
import org.jxmpp.jid.parts.*;

/**
 * Implements the Jitsi Videobridge <tt>conference</tt> IQ within the
 * COnferencing with LIghtweight BRIdging.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 * @author George Politis
 */
public class ColibriConferenceIQ
    extends IQ
{
    /**
     * The XML element name of the Jitsi Videobridge <tt>conference</tt> IQ.
     */
    public static final String ELEMENT_NAME = "conference";

    /**
     * The XML name of the <tt>id</tt> attribute of the Jitsi Videobridge
     * <tt>conference</tt> IQ which represents the value of the <tt>id</tt>
     * property of <tt>ColibriConferenceIQ</tt>.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The XML name of the <tt>gid</tt> attribute of the Jitsi Videobridge
     * <tt>conference</tt> IQ which represents the value of the <tt>gid</tt>
     * property of <tt>ColibriConferenceIQ</tt>.
     * This is a "global" ID of a conference, which is selected by the
     * conference organizer, as opposed to "id" which is specific to a single
     * Jitsi Videobridge and is selected by the bridge.
     *
     */
    public static final String GID_ATTR_NAME = "gid";

    /**
     * The XML name of the <tt>name</tt> attribute of the Jitsi Videobridge
     * <tt>conference</tt> IQ which represents the value of the <tt>name</tt>
     * property of <tt>ColibriConferenceIQ</tt> if available.
     */
    public static final String NAME_ATTR_NAME = "name";

    /**
     * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi
     * Videobridge <tt>conference</tt> IQ.
     */
    public static final String NAMESPACE
        = "http://jitsi.org/protocol/colibri";

    /**
     * The logger instance used by this class.
     */
    private final static Logger logger
            = Logger.getLogger(ColibriConferenceIQ.class);

    /**
     * An array of <tt>int</tt>s which represents the lack of any (RTP) SSRCs
     * seen/received on a <tt>Channel</tt>. Explicitly defined to reduce
     * unnecessary allocations.
     */
    public static final int[] NO_SSRCS = new int[0];

    /**
     * The {@link ChannelBundle}s included in this {@link ColibriConferenceIQ},
     * mapped by their ID.
     */
    private final Map<String, ChannelBundle> channelBundles
        = new ConcurrentHashMap<>();

    /**
     * The list of {@link Content}s included into this <tt>conference</tt> IQ.
     */
    private final List<Content> contents = new LinkedList<>();

    /**
     * The {@link Endpoint}s included in this {@link ColibriConferenceIQ},
     * mapped by their ID.
     */
    private final Map<String, Endpoint> endpoints = new ConcurrentHashMap<>();

    /**
     * The ID of the conference represented by this IQ.
     */
    private String id;

    /**
     * The ID of the global conference to which the conference represented by
     * this {@link ColibriConferenceIQ} belongs.
     */
    private String gid;

    /**
     * Media recording.
     */
    private Recording recording;

    private RTCPTerminationStrategy rtcpTerminationStrategy;

    /**
     * Indicates if the information about graceful shutdown status is being
     * carried by this IQ.
     */
    private boolean gracefulShutdown;

    /**
     * World readable name for the conference.
     */
    private Localpart name;

    /**
     * Returns an error response for given <tt>IQ</tt> that is returned by
     * the videobridge after it has entered graceful shutdown mode and new
     * conferences can no longer be created.
     *
     * @param request the IQ for which error response will be created.
     * @return an IQ of 'error' type and 'service-unavailable' condition plus
     *         the body of request IQ.
     */
    public static IQ createGracefulShutdownErrorResponse(final IQ request)
    {
        final XMPPError error = XMPPError.getBuilder()
            .setCondition(XMPPError.Condition.service_unavailable)
            .setType(XMPPError.Type.CANCEL)
            .addExtension(new GracefulShutdown())
            .build();

        final IQ result = IQ.createErrorResponse(request, error);

        result.setType(Type.error);
        result.setStanzaId(request.getStanzaId());
        result.setFrom(request.getTo());
        result.setTo(request.getFrom());

        return result;
    }

    /** Initializes a new <tt>ColibriConferenceIQ</tt> instance. */
    public ColibriConferenceIQ()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Adds a specific {@link ChannelBundle} instance to this
     * {@link ColibriConferenceIQ}. The bundle must have a non-null ID, and
     * if another {@link ChannelBundle} with the same ID exists in this
     * {@link ColibriConferenceIQ}, the old one will be replaced.
     *
     * @param channelBundle the <tt>ChannelBundle</tt> to add.
     * @return the previous {@link ChannelBundle} with the same ID as
     * {@code channelBundle}, or {@code null} if one did not exist.
     */
    public ChannelBundle addChannelBundle(ChannelBundle channelBundle)
    {
        Objects.requireNonNull(channelBundle, "channelBundle");
        String id =
            Objects.requireNonNull(channelBundle.getId(), "channelBundle ID");

        return channelBundles.put(id, channelBundle);
    }

    /**
     * Adds a specific {@link Content} instance to the list of <tt>Content</tt>
     * instances included into this <tt>conference</tt> IQ.
     *
     * @param content the <tt>Content</tt> instance to be added to this list of
     * <tt>Content</tt> instances included into this <tt>conference</tt> IQ
     * @return <tt>true</tt> if the list of <tt>Content</tt> instances included
     * into this <tt>conference</tt> IQ has been modified as a result of the
     * method call; otherwise, <tt>false</tt>
     */
    public boolean addContent(Content content)
    {
        Objects.requireNonNull(content, "content");

        return contents.contains(content) ? false : contents.add(content);
    }

    /**
     * Initializes a new {@link Content} instance with a specific name and adds
     * it to the list of <tt>Content</tt> instances included into this
     * <tt>conference</tt> IQ.
     *
     * @param contentName the name which which the new <tt>Content</tt> instance
     * is to be initialized
     * @return <tt>true</tt> if the list of <tt>Content</tt> instances included
     * into this <tt>conference</tt> IQ has been modified as a result of the
     * method call; otherwise, <tt>false</tt>
     */
    public boolean addContent(String contentName)
    {
        return addContent(new Content(contentName));
    }

    /**
     * Adds an {@link Endpoint} to this {@link ColibriConferenceIQ}. The
     * endpoint must be non-null and must have a non-null ID. If an
     * {@link Endpoint} with the same ID as the given {@link Endpoint} already
     * exists it is replaced and the previous one is returned.
     * @param endpoint the {@link Endpoint} to add.
     * @return The previous {@link Endpoint} with the same ID, or {@code null}.
     */
    public Endpoint addEndpoint(Endpoint endpoint)
    {
        Objects.requireNonNull(endpoint, "endpoint");
        String id = Objects.requireNonNull(endpoint.getId(), "endpoint ID");

        return endpoints.put(id, endpoint);
    }

    /**
     * @return a list which contains the {@link ChannelBundle}s of this
     * {@link ColibriConferenceIQ}.
     */
    public List<ChannelBundle> getChannelBundles()
    {
        return new LinkedList<>(channelBundles.values());
    }

    /**
     * @param channelBundleId The ID of the {@link ChannelBundle} to get.
     * @return The {@link ChannelBundle} identified by {@code channelBundleId},
     * or {@code null}.
     */
    public ChannelBundle getChannelBundle(String channelBundleId)
    {
        Objects.requireNonNull(channelBundleId, "channelBundleId");
        return channelBundles.get(channelBundleId);
    }

    /**
     * Finds {@link Endpoint} identified by given <tt>endpointId</tt>.
     * @param endpointId <tt>Endpoint</tt> identifier.
     * @return {@link Endpoint} identified by given <tt>endpointId</tt> or
     *         <tt>null</tt> if not found.
     */
    public Endpoint getEndpoint(String endpointId)
    {
        if (endpointId == null)
        {
            return null;
        }
        return endpoints.get(endpointId);
    }

    /**
     * Returns an XML <tt>String</tt> representation of this <tt>IQ</tt>.
     *
     * @return an XML <tt>String</tt> representation of this <tt>IQ</tt>
     */
    @Override
    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(
        IQ.IQChildElementXmlStringBuilder xml)
    {
        xml.optAttribute(ID_ATTR_NAME, getID());
        xml.optAttribute(GID_ATTR_NAME, getGID());
        xml.optAttribute(NAME_ATTR_NAME, name);

        List<Content> contents = getContents();
        List<ChannelBundle> channelBundles = getChannelBundles();
        List<Endpoint> endpoints = getEndpoints();

        boolean hasChildren
            = (recording != null)
                || (rtcpTerminationStrategy != null)
                || (gracefulShutdown)
                || (contents.size() > 0)
                || (channelBundles.size() > 0)
                || (endpoints.size() > 0);

        if (!hasChildren)
        {
            xml.setEmptyElement();
        }
        else
        {
            xml.rightAngleBracket();
            for (Content content : contents)
                content.toXML(xml);
            for (ChannelBundle channelBundle : channelBundles)
                channelBundle.toXML(xml);
            for (Endpoint endpoint : endpoints)
                endpoint.toXML(xml);
            if (recording != null)
                recording.toXML(xml);
            if (rtcpTerminationStrategy != null)
                rtcpTerminationStrategy.toXML(xml);
            if (gracefulShutdown)
                xml.append(new GracefulShutdown().toXML());
        }

        return xml;
    }

    /**
     * Returns a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has a specific name. If no such
     * <tt>Content</tt> exists, returns <tt>null</tt>.
     *
     * @param contentName the name of the <tt>Content</tt> to be returned
     * @return a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has the specified <tt>contentName</tt> if
     * such a <tt>Content</tt> exists; otherwise, <tt>null</tt>
     */
    public Content getContent(String contentName)
    {
        for (Content content : getContents())
        {
            if (contentName.equals(content.getName()))
            {
                return content;
            }
        }
        return null;
    }

    /**
     * Returns a list of the <tt>Content</tt>s included into this
     * <tt>conference</tt> IQ.
     *
     * @return an unmodifiable <tt>List</tt> of the <tt>Content</tt>s included
     * into this <tt>conference</tt> IQ
     */
    public List<Content> getContents()
    {
        return Collections.unmodifiableList(contents);
    }

    /**
     * Returns a list of all {@link Endpoint}s in this
     * {@link ColibriConferenceIQ}.
     */
    public List<Endpoint> getEndpoints()
    {
        return new LinkedList<>(endpoints.values());
    }

    /**
     * Gets the ID of the conference represented by this IQ.
     *
     * @return the ID of the conference represented by this IQ
     */
    public String getID()
    {
        return id;
    }

    /**
     * @return the "global" ID of the conference represented by this IQ.
     */
    public String getGID()
    {
        return gid;
    }

    /**
     * Returns a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has a specific name. If no such
     * <tt>Content</tt> exists at the time of the invocation of the method,
     * initializes a new <tt>Content</tt> instance with the specified
     * <tt>contentName</tt> and includes it into this <tt>conference</tt> IQ.
     *
     * @param contentName the name of the <tt>Content</tt> to be returned
     * @return a <tt>Content</tt> from the list of <tt>Content</tt>s of this
     * <tt>conference</tt> IQ which has the specified <tt>contentName</tt>
     */
    public Content getOrCreateContent(String contentName)
    {
        Content content = getContent(contentName);

        if (content == null)
        {
            content = new Content(contentName);
            addContent(content);
        }
        return content;
    }

    /**
     * Gets the value of the recording field.
     * @return the value of the recording field.
     */
    public Recording getRecording()
    {
        return recording;
    }

    public RTCPTerminationStrategy getRTCPTerminationStrategy()
    {
        return rtcpTerminationStrategy;
    }

    /**
     * Removes a specific {@link Content} instance from the list of
     * <tt>Content</tt> instances included into this <tt>conference</tt> IQ.
     *
     * @param content the <tt>Content</tt> instance to be removed from the list
     * of <tt>Content</tt> instances included into this <tt>conference</tt> IQ
     * @return <tt>true</tt> if the list of <tt>Content</tt> instances included
     * into this <tt>conference</tt> IQ has been modified as a result of the
     * method call; otherwise, <tt>false</tt>
     */
    public boolean removeContent(Content content)
    {
        return contents.remove(content);
    }

    /**
     * Sets the ID of the conference represented by this IQ.
     *
     * @param id the value to set.
     */
    public void setID(String id)
    {
        this.id = id;
    }

    /**
     * Sets the "global" ID of the conference represented by this IQ.
     *
     * @param gid the value to set.
     */
    public void setGID(String gid)
    {
        this.gid = gid;
    }

    /**
     * Sets the recording field.
     * @param recording the value to set.
     */
    public void setRecording(Recording recording)
    {
        this.recording = recording;
    }

    public void setRTCPTerminationStrategy(
            RTCPTerminationStrategy rtcpTerminationStrategy)
    {
        this.rtcpTerminationStrategy = rtcpTerminationStrategy;
    }

    /**
     * Sets whether this IQ should contain the information about graceful
     * shutdown in progress status.
     *
     * @param isGracefulShutdown <tt>true</tt> if graceful shutdown status
     *        should be indicated in this IQ.
     */
    public void setGracefulShutdown(boolean isGracefulShutdown)
    {
        this.gracefulShutdown = isGracefulShutdown;
    }

    /**
     * Returns <tt>true</tt> if graceful shutdown status info is indicated in
     * this <tt>ColibriConferenceIQ</tt> instance.
     */
    public boolean isGracefulShutdown()
    {
        return gracefulShutdown;
    }

    /**
     * The world readable name of the conference.
     * @return name of the conference.
     */
    public Localpart getName()
    {
        return name;
    }

    /**
     * Sets name.
     * @param name the name to set.
     */
    public void setName(Localpart name)
    {
        this.name = name;
    }

    /**
     * Represents a <tt>channel</tt> included into a <tt>content</tt> of a Jitsi
     * Videobridge <tt>conference</tt> IQ.
     */
    public static class Channel
        extends ChannelCommon
    {
        /**
         * The name of the XML attribute of a <tt>channel</tt> which represents
         * its direction.
         */
        public static final String DIRECTION_ATTR_NAME = "direction";

        /**
         * The XML element name of a <tt>channel</tt> of a <tt>content</tt> of a
         * Jitsi Videobridge <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "channel";

        /**
         * The XML name of the <tt>host</tt> attribute of a <tt>channel</tt> of
         * a <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>host</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         *
         * @deprecated The attribute is supported for the purposes of
         * compatibility with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public static final String HOST_ATTR_NAME = "host";

        /**
         * The XML name of the <tt>last-n</tt> attribute of a video
         * <tt>channel</tt> which specifies the maximum number of video RTP
         * streams to be sent from Jitsi Videobridge to the endpoint associated
         * with the video <tt>channel</tt>. The value of the <tt>last-n</tt>
         * attribute is a positive number.
         */
        public static final String LAST_N_ATTR_NAME = "last-n";

        /**
         * The XML name of the <tt>simulcast-mode</tt> attribute of a video
         * <tt>channel</tt>.
         */
        public static final String SIMULCAST_MODE_ATTR_NAME
                = "simulcast-mode";

        /**
         * The XML name of the <tt>receive-simulcast-layer</tt> attribute of a
         * video <tt>Channel</tt> which specifies the target quality of the
         * simulcast substreams to be sent from Jitsi Videobridge to the
         * endpoint associated with the video <tt>Channel</tt>. The value of the
         * <tt>receive-simulcast-layer</tt> attribute is an unsigned integer.
         * Typically used for debugging purposes.
         */
        public static final String RECEIVING_SIMULCAST_LAYER
                = "receive-simulcast-layer";

        /**
         * The XML name of the <tt>packet-delay</tt> attribute of
         * a <tt>channel</tt> of a <tt>content</tt> of a <tt>conference</tt> IQ
         * which represents the value of the {@link #packetDelay} property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String PACKET_DELAY_ATTR_NAME = "packet-delay";

        /**
         * The XML name of the <tt>rtcpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtcpPort</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         *
         * @deprecated The attribute is supported for the purposes of
         * compatibility with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public static final String RTCP_PORT_ATTR_NAME = "rtcpport";

        public static final String RTP_LEVEL_RELAY_TYPE_ATTR_NAME
            = "rtp-level-relay-type";

        /**
         * The XML name of the <tt>rtpport</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>rtpPort</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         *
         * @deprecated The attribute is supported for the purposes of
         * compatibility with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public static final String RTP_PORT_ATTR_NAME = "rtpport";

        /**
         * The name of the XML element which is a child of the &lt;channel&gt;
         * element and which identifies/specifies an (RTP) SSRC which has been
         * seen/received on the respective <tt>Channel</tt>.
         */
        public static final String SSRC_ELEMENT_NAME = "ssrc";

        /**
         * The direction of the <tt>channel</tt> represented by this instance.
         */
        private MediaDirection direction;

        /**
         * The host of the <tt>channel</tt> represented by this instance.
         *
         * @deprecated The field is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        private String host;

        /**
         * The maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         */
        private Integer lastN;

        /**
         * The 'simulcast-mode' flag.
         */
        private SimulcastMode simulcastMode;

        /**
         * The amount of delay added to the RTP stream in a number of packets.
         */
        private Integer packetDelay;

        /**
         * The <tt>payload-type</tt> elements defined by XEP-0167: Jingle RTP
         * Sessions associated with this <tt>channel</tt>.
         */
        private final List<PayloadTypePacketExtension> payloadTypes
            = new ArrayList<>();

        /**
         * The <tt>rtp-hdrext</tt> elements defined by XEP-0294: Jingle RTP
         * Header Extensions Negotiation associated with this channel.
         */
        private final Map<Integer, RTPHdrExtPacketExtension> rtpHeaderExtensions
            = new HashMap<>();

        /**
         * The target quality of the simulcast substreams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         */
        private Integer receivingSimulcastLayer;

        /**
         * The RTCP port of the <tt>channel</tt> represented by this instance.
         *
         * @deprecated The field is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        private int rtcpPort;

        /**
         * The type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         */
        private RTPLevelRelayType rtpLevelRelayType;

        /**
         * The RTP port of the <tt>channel</tt> represented by this instance.
         *
         * @deprecated The field is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        private int rtpPort;

        /**
         * The <tt>SourceGroupPacketExtension</tt>s of this channel.
         */
        private List<SourceGroupPacketExtension> sourceGroups;

        /**
         * The <tt>SourcePacketExtension</tt>s of this channel.
         */
        private final List<SourcePacketExtension> sources
            = new LinkedList<>();

        /**
         * The list of (RTP) SSRCs which have been seen/received on this
         * <tt>Channel</tt> by now. These may exclude SSRCs which are no longer
         * active. Set by the Jitsi Videobridge server, not its clients.
         */
        private int[] ssrcs = NO_SSRCS;

        /** Initializes a new <tt>Channel</tt> instance. */
        public Channel()
        {
            super(Channel.ELEMENT_NAME);
        }

        /**
         * Adds a <tt>payload-type</tt> element defined by XEP-0167: Jingle RTP
         * Sessions to this <tt>channel</tt>.
         *
         * @param payloadType the <tt>payload-type</tt> element to be added to
         * this <tt>channel</tt>
         * @return <tt>true</tt> if the list of <tt>payload-type</tt> elements
         * associated with this <tt>channel</tt> has been modified as part of
         * the method call; otherwise, <tt>false</tt>
         */
        public boolean addPayloadType(PayloadTypePacketExtension payloadType)
        {
            Objects.requireNonNull(payloadType, "payloadType");

            // Make sure that the COLIBRI namespace is used.
            payloadType.setNamespace(null);
            for (ParameterPacketExtension p : payloadType.getParameters())
                p.setNamespace(null);

            return
                payloadTypes.contains(payloadType)
                    ? false
                    : payloadTypes.add(payloadType);
        }

        /**
         * Adds an <tt>rtp-hdrext</tt> element defined by XEP-0294: Jingle RTP
         * Header Extensions Negotiation to this <tt>Channel</tt>.
         *
         * @param ext the <tt>payload-type</tt> element to be added to
         * this <tt>channel</tt>
         * @return <tt>true</tt> if the list of <tt>rtp-hdrext</tt> elements
         * associated with this <tt>channel</tt> has been modified as part of
         * the method call; otherwise, <tt>false</tt>
         */
        public void addRtpHeaderExtension(RTPHdrExtPacketExtension ext)
        {
            Objects.requireNonNull(ext, "ext");

            // Create a new instance, because we are going to modify the NS
            RTPHdrExtPacketExtension newExt
                = RTPHdrExtPacketExtension.clone(ext);

            // Make sure that the parent namespace (COLIBRI) is used.
            newExt.setNamespace(null);

            int id = -1;
            try
            {
                id = Integer.valueOf(newExt.getID());
            }
            catch (NumberFormatException nfe)
            {}

            // Only accept valid extension IDs (4-bits, 0xF reserved)
            if (id < 0 || id > 14)
            {
                logger.warn("Failed to add an RTP header extension element "
                                    + "with an invalid ID: " + newExt.getID());
                return;
            }

            rtpHeaderExtensions.put(id, newExt);
        }

        /**
         * Adds a <tt>SourcePacketExtension</tt> to the list of sources of this
         * channel.
         *
         * @param source the <tt>SourcePacketExtension</tt> to add to the list
         * of sources of this channel
         * @return <tt>true</tt> if the list of sources of this channel changed
         * as a result of the execution of the method; otherwise, <tt>false</tt>
         */
        public synchronized boolean addSource(SourcePacketExtension source)
        {
            Objects.requireNonNull(source, "source");

            return sources.contains(source) ? false : sources.add(source);
        }

        /**
         * Adds a <tt>SourceGroupPacketExtension</tt> to the list of source
         * groups of this channel.
         *
         * @param sourceGroup the <tt>SourcePacketExtension</tt> to add to the
         * list of sources of this channel
         *
         * @return <tt>true</tt> if the list of sources of this channel changed
         * as a result of the execution of the method; otherwise, <tt>false</tt>
         */
        public synchronized boolean addSourceGroup(
                SourceGroupPacketExtension sourceGroup)
        {
            Objects.requireNonNull(sourceGroup, "sourceGroup");

            if (sourceGroups == null)
            {
                sourceGroups = new LinkedList<>();
            }

            return
                sourceGroups.contains(sourceGroup)
                    ? false
                    : sourceGroups.add(sourceGroup);
        }

        /**
         * Adds a specific (RTP) SSRC to the list of SSRCs seen/received on this
         * <tt>Channel</tt>. Invoked by the Jitsi Videobridge server, not its
         * clients.
         *
         * @param ssrc the (RTP) SSRC to be added to the list of SSRCs
         * seen/received on this <tt>Channel</tt>
         * @return <tt>true</tt> if the list of SSRCs seen/received on this
         * <tt>Channel</tt> has been modified as part of the method call;
         * otherwise, <tt>false</tt>
         */
        public synchronized boolean addSSRC(int ssrc)
        {
            // contains
            for (int i = 0; i < ssrcs.length; i++)
            {
                if (ssrcs[i] == ssrc)
                    return false;
            }

            // add
            int[] newSSRCs = new int[ssrcs.length + 1];

            System.arraycopy(ssrcs, 0, newSSRCs, 0, ssrcs.length);
            newSSRCs[ssrcs.length] = ssrc;
            ssrcs = newSSRCs;
            return true;
        }

        /**
         * Gets the <tt>direction</tt> of this <tt>Channel</tt>.
         *
         * @return the <tt>direction</tt> of this <tt>Channel</tt>.
         */
        public MediaDirection getDirection()
        {
            return (direction == null) ? MediaDirection.SENDRECV : direction;
        }

        /**
         * Gets the IP address (as a <tt>String</tt> value) of the host on which
         * the <tt>channel</tt> represented by this instance has been allocated.
         *
         * @return a <tt>String</tt> value which represents the IP address of
         * the host on which the <tt>channel</tt> represented by this instance
         * has been allocated
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public String getHost()
        {
            return host;
        }

        /**
         * Gets the maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         *
         * @return the maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>
         */
        public Integer getLastN()
        {
            return lastN;
        }

        /**
         * Gets the value of the 'simulcast-mode' flag.
         * @return the value of the 'simulcast-mode' flag.
         */
        public SimulcastMode getSimulcastMode()
        {
            return simulcastMode;
        }

        /**
         * Returns an <tt>Integer</tt> which stands for the amount of delay
         * added to the RTP stream in a number of packets.
         *
         * @return <tt>Integer</tt> with the value or <tt>null</tt> if
         * unspecified.
         */
        public Integer getPacketDelay()
        {
            return packetDelay;
        }

        /**
         * Gets a list of <tt>payload-type</tt> elements defined by XEP-0167:
         * Jingle RTP Sessions added to this <tt>channel</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of <tt>payload-type</tt>
         * elements defined by XEP-0167: Jingle RTP Sessions added to this
         * <tt>channel</tt>
         */
        public List<PayloadTypePacketExtension> getPayloadTypes()
        {
            return Collections.unmodifiableList(payloadTypes);
        }

        /**
         * Gets a list of <tt>rtp-hdrext</tt> elements defined by XEP-0294:
         * Jingle RTP Header Extensions Negotiation added to this
         * <tt>channel</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of <tt>rtp-hdrext</tt>
         * elements defined by XEP-0294: Jingle RTP Header Extensions
         * Negotiation added to this <tt>channel</tt>
         */
        public Collection<RTPHdrExtPacketExtension> getRtpHeaderExtensions()
        {
            return Collections
                    .unmodifiableCollection(rtpHeaderExtensions.values());
        }

        /**
         * Gets the target quality of the simulcast substreams to be sent from
         * Jitsi Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         *
         * @return the target quality of the simulcast substreams to be sent
         * from Jitsi Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         */
        public Integer getReceivingSimulcastLayer()
        {
            return receivingSimulcastLayer;
        }

        /**
         * Gets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTCP packets.
         *
         * @return the port which has been allocated to this <tt>channel</tt>
         * for the purposes of transmitting RTCP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public int getRTCPPort()
        {
            return rtcpPort;
        }

        /**
         * Gets the type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         *
         * @return the type of RTP-level relay used for this <tt>Channel</tt>
         */
        public RTPLevelRelayType getRTPLevelRelayType()
        {
            return rtpLevelRelayType;
        }

        /**
         * Gets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTP packets.
         *
         * @return the port which has been allocated to this <tt>channel</tt>
         * for the purposes of transmitting RTP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public int getRTPPort()
        {
            return rtpPort;
        }

        /**
         * Gets the list of <tt>SourceGroupPacketExtensions</tt>s which
         * represent the source groups of this channel.
         *
         * @return a <tt>List</tt> of <tt>SourceGroupPacketExtension</tt>s which
         * represent the source groups of this channel
         */
        public synchronized List<SourceGroupPacketExtension> getSourceGroups()
        {
            return
                (sourceGroups == null)
                    ? null
                    : new ArrayList<SourceGroupPacketExtension>(sourceGroups);
        }

        /**
         * Gets the list of <tt>SourcePacketExtensions</tt>s which represent the
         * sources of this channel.
         *
         * @return a <tt>List</tt> of <tt>SourcePacketExtension</tt>s which
         * represent the sources of this channel
         */
        public synchronized List<SourcePacketExtension> getSources()
        {
            return new ArrayList<SourcePacketExtension>(sources);
        }

        /**
         * Gets (a copy of) the list of (RTP) SSRCs seen/received on this
         * <tt>Channel</tt>.
         *
         * @return an array of <tt>int</tt>s which represents (a copy of) the
         * list of (RTP) SSRCs seen/received on this <tt>Channel</tt>
         */
        public synchronized int[] getSSRCs()
        {
            return (ssrcs.length == 0) ? NO_SSRCS : ssrcs.clone();
        }

        @Override
        protected boolean hasContent()
        {
            List<PayloadTypePacketExtension> payloadTypes = getPayloadTypes();

            if (!payloadTypes.isEmpty())
                return true;

            List<SourceGroupPacketExtension> sourceGroups = getSourceGroups();
            if (sourceGroups != null && !getSourceGroups().isEmpty())
                return true;

            List<SourcePacketExtension> sources = getSources();
            if (!sources.isEmpty())
                return true;

            int[] ssrcs = getSSRCs();

            return (ssrcs.length != 0);
        }

        @Override
        protected IQChildElementXmlStringBuilder printAttributes(IQChildElementXmlStringBuilder xml)
        {
            // direction
            MediaDirection direction = getDirection();
            if ((direction != null) && (direction != MediaDirection.SENDRECV))
            {
                xml.attribute(DIRECTION_ATTR_NAME, direction.toString());
            }

            xml.optAttribute(HOST_ATTR_NAME, getHost());

            // lastN
            Integer lastN = getLastN();
            if (lastN != null)
            {
                xml.attribute(LAST_N_ATTR_NAME, getLastN());
            }

            // packet-delay
            Integer packetDelay = getPacketDelay();
            if (packetDelay != null)
            {
                xml.attribute(PACKET_DELAY_ATTR_NAME, packetDelay);
            }

            // simulcastMode
            SimulcastMode simulcastMode = getSimulcastMode();
            if (simulcastMode != null)
            {
                xml.attribute(SIMULCAST_MODE_ATTR_NAME, simulcastMode.toString());
            }

            // rtcpPort
            int rtcpPort = getRTCPPort();
            if (rtcpPort > 0)
            {
                xml.attribute(RTCP_PORT_ATTR_NAME, rtcpPort);
            }

            // rtpLevelRelayType
            RTPLevelRelayType rtpLevelRelayType = getRTPLevelRelayType();
            if (rtpLevelRelayType != null)
            {
                xml.attribute(RTP_LEVEL_RELAY_TYPE_ATTR_NAME, rtpLevelRelayType.toString());
            }

            // rtpPort
            int rtpPort = getRTPPort();
            if (rtpPort > 0)
            {
                xml.attribute(RTP_PORT_ATTR_NAME, rtpPort);
            }

            return xml;
        }

        @Override
        protected IQChildElementXmlStringBuilder printContent(IQChildElementXmlStringBuilder xml)
        {
            List<PayloadTypePacketExtension> payloadTypes = getPayloadTypes();
            Collection<RTPHdrExtPacketExtension> rtpHdrExtPacketExtensions
                    = getRtpHeaderExtensions();
            List<SourcePacketExtension> sources = getSources();
            List<SourceGroupPacketExtension> sourceGroups = getSourceGroups();
            int[] ssrcs = getSSRCs();

            for (PayloadTypePacketExtension payloadType : payloadTypes)
                xml.append(payloadType.toXML());

            for (RTPHdrExtPacketExtension ext : rtpHdrExtPacketExtensions)
                xml.append(ext.toXML());

            for (SourcePacketExtension source : sources)
                xml.append(source.toXML());

            if (sourceGroups != null && sourceGroups.size() != 0)
                for (SourceGroupPacketExtension sourceGroup : sourceGroups)
                    xml.append(sourceGroup.toXML());

            for (int i = 0; i < ssrcs.length; i++)
            {
                xml.element(SSRC_ELEMENT_NAME,
                    Long.toString(ssrcs[i] & 0xFFFFFFFFL));
            }

            return xml;
        }

        /**
         * Removes a <tt>payload-type</tt> element defined by XEP-0167: Jingle
         * RTP Sessions from this <tt>channel</tt>.
         *
         * @param payloadType the <tt>payload-type</tt> element to be removed
         * from this <tt>channel</tt>
         * @return <tt>true</tt> if the list of <tt>payload-type</tt> elements
         * associated with this <tt>channel</tt> has been modified as part of
         * the method call; otherwise, <tt>false</tt>
         */
        public boolean removePayloadType(PayloadTypePacketExtension payloadType)
        {
            return payloadTypes.remove(payloadType);
        }

        /**
         * Removes a <tt>rtp-hdrext</tt> element defined by XEP-0294: Jingle
         * RTP Header Extensions Negotiation from this <tt>channel</tt>.
         *
         * @param ext the <tt>rtp-hdrext</tt> element to be removed
         * from this <tt>channel</tt>
         * @return <tt>true</tt> if the list of <tt>rtp-hdrext</tt> elements
         * associated with this <tt>channel</tt> has been modified as part of
         * the method call; otherwise, <tt>false</tt>
         */
        public void removeRtpHeaderExtension(RTPHdrExtPacketExtension ext)
        {
            int id = -1;
            try
            {
                id = Integer.valueOf(ext.getID());
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("Invalid ID: " + ext.getID());
                return;
            }

            rtpHeaderExtensions.remove(id);
        }

        /**
         * Removes a <tt>SourcePacketExtension</tt> from the list of sources of
         * this channel.
         *
         * @param source the <tt>SourcePacketExtension</tt> to remove from the
         * list of sources of this channel
         * @return <tt>true</tt> if the list of sources of this channel changed
         * as a result of the execution of the method; otherwise, <tt>false</tt>
         */
        public synchronized boolean removeSource(SourcePacketExtension source)
        {
            return sources.remove(source);
        }

        /**
         * Removes a specific (RTP) SSRC from the list of SSRCs seen/received on
         * this <tt>Channel</tt>. Invoked by the Jitsi Videobridge server, not
         * its clients.
         *
         * @param ssrc the (RTP) SSRC to be removed from the list of SSRCs
         * seen/received on this <tt>Channel</tt>
         * @return <tt>true</tt> if the list of SSRCs seen/received on this
         * <tt>Channel</tt> has been modified as part of the method call;
         * otherwise, <tt>false</tt>
         */
        public synchronized boolean removeSSRC(int ssrc)
        {
            if (ssrcs.length == 1)
            {
                if (ssrcs[0] == ssrc)
                {
                    ssrcs = NO_SSRCS;
                    return true;
                }
                else
                    return false;
            }
            else
            {
                for (int i = 0; i < ssrcs.length; i++)
                {
                    if (ssrcs[i] == ssrc)
                    {
                        int[] newSSRCs = new int[ssrcs.length - 1];

                        if (i != 0)
                            System.arraycopy(ssrcs, 0, newSSRCs, 0, i);
                        if (i != newSSRCs.length)
                        {
                            System.arraycopy(
                                    ssrcs, i + 1,
                                    newSSRCs, i,
                                    newSSRCs.length - i);
                        }
                        ssrcs = newSSRCs;
                        return true;
                    }
                }
                return false;
            }
        }

        /**
         * Sets the <tt>direction</tt> of this <tt>Channel</tt>
         *
         * @param direction the <tt>MediaDirection</tt> to set the
         * <tt>direction</tt> of this <tt>Channel</tt> to.
         */
        public void setDirection(MediaDirection direction)
        {
            this.direction = direction;
        }

        /**
         * Sets the IP address (as a <tt>String</tt> value) of the host on which
         * the <tt>channel</tt> represented by this instance has been allocated.
         *
         * @param host a <tt>String</tt> value which represents the IP address
         * of the host on which the <tt>channel</tt> represented by this
         * instance has been allocated
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public void setHost(String host)
        {
            this.host = host;
        }

        /**
         * Sets the maximum number of video RTP streams to be sent from Jitsi
         * Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         *
         * @param lastN the maximum number of video RTP streams to be sent from
         * Jitsi Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>
         */
        public void setLastN(Integer lastN)
        {
            this.lastN = lastN;
        }

        /**
         * Configures channel's packet delay which tells by how many packets
         * the RTP streams will be delayed.
         * @param packetDelay an <tt>Integer</tt> value which stands for
         * the packet delay that will be set or <tt>null</tt> to leave undefined
         */
        public void setPacketDelay(Integer packetDelay)
        {
            this.packetDelay = packetDelay;
        }

        /**
         * Sets the value of the 'simulcast-mode' flag.
         * @param simulcastMode the value to set.
         */
        public void setSimulcastMode(SimulcastMode simulcastMode)
        {
            this.simulcastMode = simulcastMode;
        }

        /**
         * Sets the target quality of the simulcast substreams to be sent from
         * Jitsi Videobridge to the endpoint associated with this video
         * <tt>Channel</tt>.
         *
         * @param simulcastLayer the target quality of the simulcast substreams
         * to be sent from Jitsi Videobridge to the endpoint associated with
         * this video <tt>Channel</tt>.
         */
        public void setReceivingSimulcastLayer(Integer simulcastLayer)
        {
            this.receivingSimulcastLayer = simulcastLayer;
        }

        /**
         * Sets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTCP packets.
         *
         * @param rtcpPort the port which has been allocated to this
         * <tt>channel</tt> for the purposes of transmitting RTCP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public void setRTCPPort(int rtcpPort)
        {
            this.rtcpPort = rtcpPort;
        }

        /**
         * Sets the type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         *
         * @param rtpLevelRelayType the type of RTP-level relay used for
         * this <tt>Channel</tt>
         */
        public void setRTPLevelRelayType(RTPLevelRelayType rtpLevelRelayType)
        {
            this.rtpLevelRelayType = rtpLevelRelayType;
        }

        /**
         * Sets the type of RTP-level relay (in the terms specified by RFC 3550
         * &quot;RTP: A Transport Protocol for Real-Time Applications&quot; in
         * section 2.3 &quot;Mixers and Translators&quot;) used for this
         * <tt>Channel</tt>.
         *
         * @param s the type of RTP-level relay used for this <tt>Channel</tt>
         */
        public void setRTPLevelRelayType(String s)
        {
            setRTPLevelRelayType(RTPLevelRelayType.parseRTPLevelRelayType(s));
        }

        /**
         * Sets the port which has been allocated to this <tt>channel</tt> for
         * the purposes of transmitting RTP packets.
         *
         * @param rtpPort the port which has been allocated to this
         * <tt>channel</tt> for the purposes of transmitting RTP packets
         *
         * @deprecated The method is supported for the purposes of compatibility
         * with legacy versions of Jitsi and Jitsi Videobridge. 
         */
        @Deprecated
        public void setRTPPort(int rtpPort)
        {
            this.rtpPort = rtpPort;
        }

        /**
         * Sets the list of (RTP) SSRCs seen/received on this <tt>Channel</tt>.
         *
         * @param ssrcs the list of (RTP) SSRCs to be set as seen/received on
         * this <tt>Channel</tt>
         */
        public void setSSRCs(int[] ssrcs)
        {
            /*
             * TODO Make sure that the SSRCs set on this instance do not contain
             * duplicates.
             */
            this.ssrcs
                = ((ssrcs == null) || (ssrcs.length == 0))
                    ? NO_SSRCS
                    : ssrcs.clone();
        }
    }

    /**
     * Represents a {@link Channel} of type "octo".
     */
    public static class OctoChannel
        extends Channel
    {
        /**
         * The value of the "type" attribute which corresponds to Octo channels.
         */
        public static final String TYPE = "octo";

        /**
         * The name of the "relay" child element of an {@link OctoChannel}.
         */
        public static final String RELAY_ELEMENT_NAME = "relay";

        /**
         * The name of the "id" attribute of child elements with name "relay".
         */
        public static final String RELAY_ID_ATTR_NAME = "id";

        /**
         * The list of relays of this {@link OctoChannel}.
         */
        private List<String> relays = new LinkedList<>();

        /**
         * Initializes a new {@link OctoChannel} instance.
         */
        public OctoChannel()
        {
            setType(TYPE);
        }

        /**
         * Sets the list of relays of this {@link OctoChannel}.
         * @param relays the ids of the relays to set.
         */
        public void setRelays(List<String> relays)
        {
            this.relays = new LinkedList<>(relays);
        }

        /**
         * @return the list of relays of this {@link OctoChannel}.
         */
        public List<String> getRelays()
        {
            return relays;
        }

        /**
         * Adds a relay to this {@link OctoChannel}.
         * @param relay the id of the relay to add.
         */
        public void addRelay(String relay)
        {
            if (!relays.contains(relay))
            {
                relays.add(relay);
            }
        }

        /**
         * Removes a relay from this {@link OctoChannel}.
         * @param relay the id of the relay to remove.
         */
        public void removeRelay(String relay)
        {
            relays.remove(relay);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean hasContent()
        {
            return !relays.isEmpty() || super.hasContent();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IQChildElementXmlStringBuilder printContent(IQChildElementXmlStringBuilder xml)
        {
            super.printContent(xml);
            for (String relay : relays)
            {
                xml.halfOpenElement(RELAY_ELEMENT_NAME)
                    .attribute(ID_ATTR_NAME, relay)
                    .closeEmptyElement();
            }

            return xml;
        }
    }

    /**
     * Represents a "channel-bundle" element.
     */
    public static class ChannelBundle
    {
        /**
         * The name of the "channel-bundle" element.
         */
        public static final String ELEMENT_NAME = "channel-bundle";

        /**
         * The name of the "id" attribute.
         */
        public static final String ID_ATTR_NAME = "id";

        /**
         * The ID of this <tt>ChannelBundle</tt>.
         */
        private String id;

        /**
         * The transport element of this <tt>ChannelBundle</tt>.
         */
        private IceUdpTransportPacketExtension transport;

        /**
         * Initializes a new <tt>ChannelBundle</tt> with the given ID.
         * @param id the ID.
         */
        public ChannelBundle(String id)
        {
            this.id = id;
        }

        /**
         * Returns the ID of this <tt>ChannelBundle</tt>.
         * @return  the ID of this <tt>ChannelBundle</tt>.
         */
        public String getId()
        {
            return id;
        }

        /**
         * Returns the transport element of this <tt>ChannelBundle</tt>.
         * @return  the transport element of this <tt>ChannelBundle</tt>.
         */
        public IceUdpTransportPacketExtension getTransport()
        {
            return transport;
        }

        /**
         * Sets the ID of this <tt>ChannelBundle</tt>.
         * @param id the ID to set.
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * Sets the transport element of this <tt>ChannelBundle</tt>.
         * @param transport the transport to set.
         */
        public void setTransport(IceUdpTransportPacketExtension transport)
        {
            this.transport = transport;
        }

        /**
         * Appends an XML representation of this <tt>ChannelBundle</tt> to
         * <tt>xml</tt>.
         * @param xml the <tt>StringBuilder</tt> to append to.
         */
        public IQChildElementXmlStringBuilder toXML(
            IQChildElementXmlStringBuilder xml)
        {
            xml.halfOpenElement(ELEMENT_NAME)
                .attribute(ID_ATTR_NAME, id);

            if (transport != null)
            {
                xml.rightAngleBracket();
                xml.append(transport.toXML());
                xml.closeElement(ELEMENT_NAME);
            }
            else
            {
                xml.closeEmptyElement();
            }

            return xml;
        }
    }

    /**
     * Class contains common code for both <tt>Channel</tt> and
     * <tt>SctpConnection</tt> IQ classes.
     *
     * @author Pawel Domas
     */
    public static abstract class ChannelCommon
    {
        /**
         * The name of the "channel-bundle-id" attribute.
         */
        public static final String CHANNEL_BUNDLE_ID_ATTR_NAME
                = "channel-bundle-id";

        /**
         * The XML name of the <tt>endpoint</tt> attribute which specifies the
         * optional identifier of the endpoint of the conference participant
         * associated with a <tt>channel</tt>. The value of the
         * <tt>endpoint</tt> attribute is an opaque <tt>String</tt> from the
         * point of view of Jitsi Videobridge.
         */
        public static final String ENDPOINT_ATTR_NAME = "endpoint";

        /**
         * The XML name of the <tt>expire</tt> attribute of a <tt>channel</tt>
         * of a <tt>content</tt> of a <tt>conference</tt> IQ which represents
         * the value of the <tt>expire</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String EXPIRE_ATTR_NAME = "expire";

        /**
         * The value of the <tt>expire</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt> which indicates that no actual
         * value has been specified for the property in question.
         */
        public static final int EXPIRE_NOT_SPECIFIED = -1;

        /**
         * The XML name of the <tt>id</tt> attribute of a <tt>channel</tt> of a
         * <tt>content</tt> of a <tt>conference</tt> IQ which represents the
         * value of the <tt>id</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String ID_ATTR_NAME = "id";

        /**
         * The XML name of the <tt>initiator</tt> attribute of a
         * <tt>channel</tt> of a <tt>content</tt> of a <tt>conference</tt> IQ
         * which represents the value of the <tt>initiator</tt> property of
         * <tt>ColibriConferenceIQ.Channel</tt>.
         */
        public static final String INITIATOR_ATTR_NAME = "initiator";

        /**
         * The name of the "type" attribute.
         */
        public static final String TYPE_ATTR_NAME = "type";

        /**
         * The channel-bundle-id attribute of this <tt>CommonChannel</tt>.
         */
        private String channelBundleId = null;

        /**
         * XML element name.
         */
        private String elementName;

        /**
         * The identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>.
         */
        private String endpoint;

        /**
         * The optional type of the channel.
         */
        private String type;

        /**
         * The number of seconds of inactivity after which the <tt>channel</tt>
         * represented by this instance expires.
         */
        private int expire = EXPIRE_NOT_SPECIFIED;

        /**
         * The ID of the <tt>channel</tt> represented by this instance.
         */
        private String id;

        /**
         * The indicator which determines whether the conference focus is the
         * initiator/offerer (as opposed to the responder/answerer) of the media
         * negotiation associated with this instance.
         */
        private Boolean initiator;

        private IceUdpTransportPacketExtension transport;

        /**
         * Initializes this class with given XML <tt>elementName</tt>.
         * @param elementName XML element name to be used for producing XML
         *                    representation of derived IQ class.
         */
        protected ChannelCommon(String elementName)
        {
            this.elementName = elementName;
        }

        /**
         * Get the channel-bundle-id attribute of this <tt>CommonChannel</tt>.
         * @return  the channel-bundle-id attribute of this
         * <tt>CommonChannel</tt>.
         */
        public String getChannelBundleId()
        {
            return channelBundleId;
        }

        /**
         * Gets the identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>.
         *
         * @return the identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>
         */
        public String getEndpoint()
        {
            return endpoint;
        }

        /**
         * @return optional type of this channel.
         */
        public String getType()
        {
            return type;
        }

        /**
         * Gets the number of seconds of inactivity after which the
         * <tt>channel</tt> represented by this instance expires.
         *
         * @return the number of seconds of inactivity after which the
         * <tt>channel</tt> represented by this instance expires
         */
        public int getExpire()
        {
            return expire;
        }

        /**
         * Gets the ID of the <tt>channel</tt> represented by this instance.
         *
         * @return the ID of the <tt>channel</tt> represented by this instance
         */
        public String getID()
        {
            return id;
        }

        public IceUdpTransportPacketExtension getTransport()
        {
            return transport;
        }

        /**
         * Indicates whether there are some contents that should be printed as
         * child elements of this IQ. If <tt>true</tt> is returned
         * {@link #printContent(IQChildElementXmlStringBuilder)} method will be
         * called when XML representation of this IQ is being constructed.
         * @return <tt>true</tt> if there are content to be printed as child
         *         elements of this IQ or <tt>false</tt> otherwise.
         */
        protected abstract boolean hasContent();

        /**
         * Gets the indicator which determines whether the conference focus is
         * the initiator/offerer (as opposed to the responder/answerer) of the
         * media negotiation associated with this instance.
         *
         * @return {@link Boolean#TRUE} if the conference focus is the
         * initiator/offerer of the media negotiation associated with this
         * instance, {@link Boolean#FALSE} if the conference focus is the
         * responder/answerer or <tt>null</tt> if the <tt>initiator</tt> state
         * is unspecified
         */
        public Boolean isInitiator()
        {
            return initiator;
        }

        /**
         * Derived class implements this method in order to print additional
         * attributes to main XML element.
         * @param xml <the <tt>StringBuilder</tt> to which the XML
         *            <tt>String</tt> representation of this <tt>Channel</tt>
         *            is to be appended</tt>
         */
        protected abstract IQChildElementXmlStringBuilder printAttributes(
            IQChildElementXmlStringBuilder xml);

        /**
         * Implement in order to print content child elements of this IQ using
         * given <tt>StringBuilder</tt>. Called during construction of XML
         * representation if {@link #hasContent()} returns <tt>true</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         *        <tt>String</tt> representation of this <tt>Channel</tt>
         *        is to be appended</tt></tt>.
         */
        protected abstract IQChildElementXmlStringBuilder printContent(
            IQChildElementXmlStringBuilder xml);

        /**
         * Sets the channel-bundle-id attribute of this <tt>CommonChannel</tt>.
         * @param channelBundleId the value to set.
         */
        public void setChannelBundleId(String channelBundleId)
        {
            this.channelBundleId = channelBundleId;
        }

        /**
         * Sets the identifier of the endpoint of the conference participant
         * associated with this <tt>Channel</tt>.
         *
         * @param endpoint the identifier of the endpoint of the conference
         * participant associated with this <tt>Channel</tt>
         */
        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }
        /**
         * Sets the optional type of this channel.
         * @param type the value to set.
         */
        public void setType(String type)
        {
            this.type = type;
        }

        /**
         * Sets the number of seconds of inactivity after which the
         * <tt>channel</tt> represented by this instance expires.
         *
         * @param expire the number of seconds of activity after which the
         * <tt>channel</tt> represented by this instance expires
         * @throws IllegalArgumentException if the value of the specified
         * <tt>expire</tt> is other than {@link #EXPIRE_NOT_SPECIFIED} and
         * negative
         */
        public void setExpire(int expire)
        {
            if ((expire != EXPIRE_NOT_SPECIFIED) && (expire < 0))
                throw new IllegalArgumentException("expire");

            this.expire = expire;
        }

        /*
         * Sets the ID of the <tt>channel</tt> represented by this instance.
         *
         * @param id the ID of the <tt>channel</tt> represented by this instance
         */
        public void setID(String id)
        {
            this.id = id;
        }

        /**
         * Sets the indicator which determines whether the conference focus is
         * the initiator/offerer (as opposed to the responder/answerer) of the
         * media negotiation associated with this instance.
         *
         * @param initiator {@link Boolean#TRUE} if the conference focus is the
         * initiator/offerer of the media negotiation associated with this
         * instance, {@link Boolean#FALSE} if the conference focus is the
         * responder/answerer or <tt>null</tt> if the <tt>initiator</tt> state
         * is to be unspecified
         */
        public void setInitiator(Boolean initiator)
        {
            this.initiator = initiator;
        }

        public void setTransport(IceUdpTransportPacketExtension transport)
        {
            this.transport = transport;
        }

        /**
         * Appends the XML <tt>String</tt> representation of this
         * <tt>Channel</tt> to a specific <tt>StringBuilder</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         * <tt>String</tt> representation of this <tt>Channel</tt> is to be
         * appended
         */
        public IQChildElementXmlStringBuilder toXML(
            IQChildElementXmlStringBuilder xml)
        {
            xml.halfOpenElement(elementName)
                .optAttribute(ENDPOINT_ATTR_NAME, getEndpoint())
                .optIntAttribute(EXPIRE_ATTR_NAME, getExpire())
                .optAttribute(ID_ATTR_NAME, getID())
                .optAttribute(TYPE_ATTR_NAME, getType())
                .optBooleanAttribute(INITIATOR_ATTR_NAME, isInitiator() == null
                    ? false
                    : isInitiator())
                .optAttribute(
                    CHANNEL_BUNDLE_ID_ATTR_NAME, getChannelBundleId());

            // Print derived class attributes
            printAttributes(xml);

            IceUdpTransportPacketExtension transport = getTransport();
            boolean hasTransport = (transport != null);
            if (hasTransport || hasContent())
            {
                xml.rightAngleBracket();
                if(hasContent())
                {
                    printContent(xml);
                }

                if (hasTransport)
                {
                    xml.append(transport.toXML());
                }

                xml.closeElement(elementName);
            }
            else
            {
                xml.closeEmptyElement();
            }

            return xml;
        }
    }

    /**
     * Represents a <tt>content</tt> included into a Jitsi Videobridge
     * <tt>conference</tt> IQ.
     */
    public static class Content
    {
        /**
         * The XML element name of a <tt>content</tt> of a Jitsi Videobridge
         * <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "content";

        /**
         * The XML name of the <tt>name</tt> attribute of a <tt>content</tt> of
         * a <tt>conference</tt> IQ which represents the <tt>name</tt> property
         * of <tt>ColibriConferenceIQ.Content</tt>.
         */
        public static final String NAME_ATTR_NAME = "name";

        /**
         * The list of {@link Channel}s included into this <tt>content</tt> of a
         * <tt>conference</tt> IQ.
         */
        private final List<Channel> channels = new LinkedList<Channel>();

        /**
         * The name of the <tt>content</tt> represented by this instance.
         */
        private String name;

        /**
         * The list of {@link SctpConnection}s included into this
         * <tt>content</tt> of a <tt>conference</tt> IQ.
         */
        private final List<SctpConnection> sctpConnections
            = new LinkedList<SctpConnection>();

        /**
         * Initializes a new <tt>Content</tt> instance without a name and
         * channels.
         */
        public Content()
        {
        }

        /**
         * Initializes a new <tt>Content</tt> instance with a specific name and
         * without channels.
         *
         * @param name the name to initialize the new instance with
         */
        public Content(String name)
        {
            setName(name);
        }

        /**
         * Adds a specific <tt>Channel</tt> to the list of <tt>Channel</tt>s
         * included into this <tt>Content</tt>.
         *
         * @param channel the <tt>Channel</tt> to be included into this
         * <tt>Content</tt>
         * @return <tt>true</tt> if the list of <tt>Channel</tt>s included into
         * this <tt>Content</tt> was modified as a result of the execution of
         * the method; otherwise, <tt>false</tt>
         */
        public boolean addChannel(Channel channel)
        {
            Objects.requireNonNull(channel, "channel");

            return channels.contains(channel) ? false : channels.add(channel);
        }

        /**
         * Adds <tt>ChannelCommon</tt> to this <tt>Content</tt>.
         * @param channelCommon {@link ChannelCommon} instance to be added to
         *        this content.
         * @return <tt>true</tt> if given <tt>channelCommon</tt> has been
         *         actually added to this <tt>Content</tt> instance.
         */
        public boolean addChannelCommon(ChannelCommon channelCommon)
        {
            if (channelCommon instanceof Channel)
            {
                return addChannel((Channel) channelCommon);
            }
            else
            {
                return addSctpConnection((SctpConnection) channelCommon);
            }
        }

        /**
         * Adds a specific <tt>SctpConnection</tt> to the list of
         * <tt>SctpConnection</tt>s included into this <tt>Content</tt>.
         *
         * @param conn the <tt>SctpConnection</tt> to be included into this
         * <tt>Content</tt>
         * @return <tt>true</tt> if the list of <tt>SctpConnection</tt>s
         * included into this <tt>Content</tt> was modified as a result of
         * the execution of the method; otherwise, <tt>false</tt>
         */
        public boolean addSctpConnection(SctpConnection conn)
        {
            Objects.requireNonNull(conn, "conn");

            return !sctpConnections.contains(conn) && sctpConnections.add(conn);
        }

        /**
         * Gets the <tt>Channel</tt> at a specific index/position within the
         * list of <tt>Channel</tt>s included in this <tt>Content</tt>.
         *
         * @param channelIndex the index/position within the list of
         * <tt>Channel</tt>s included in this <tt>Content</tt> of the
         * <tt>Channel</tt> to be returned
         * @return the <tt>Channel</tt> at the specified <tt>channelIndex</tt>
         * within the list of <tt>Channel</tt>s included in this
         * <tt>Content</tt>
         */
        public Channel getChannel(int channelIndex)
        {
            return getChannels().get(channelIndex);
        }

        /**
         * Gets a <tt>Channel</tt> which is included into this <tt>Content</tt>
         * and which has a specific ID.
         *
         * @param channelID the ID of the <tt>Channel</tt> included into this
         * <tt>Content</tt> to be returned
         * @return the <tt>Channel</tt> which is included into this
         * <tt>Content</tt> and which has the specified <tt>channelID</tt> if
         * such a <tt>Channel</tt> exists; otherwise, <tt>null</tt>
         */
        public Channel getChannel(String channelID)
        {
            for (Channel channel : getChannels())
            {
                if (channelID.equals(channel.getID()))
                    return channel;
            }
            return null;
        }

        /**
         * Finds an SCTP connection identified by given <tt>connectionID</tt>.
         * @param connectionID the ID of the SCTP connection to find.
         * @return <tt>SctpConnection</tt> instance identified by given ID
         *         or <tt>null</tt> if no such connection is contained in
         *         this IQ.
         */
        public SctpConnection getSctpConnection(String connectionID)
        {
            for (SctpConnection conn : getSctpConnections())
                if (connectionID.equals(conn.getID()))
                    return conn;
            return null;
        }

        /**
         * Gets the number of <tt>Channel</tt>s included into/associated with
         * this <tt>Content</tt>.
         *
         * @return the number of <tt>Channel</tt>s included into/associated with
         * this <tt>Content</tt>
         */
        public int getChannelCount()
        {
            return getChannels().size();
        }

        /**
         * Gets a list of the <tt>Channel</tt> included into/associated with
         * this <tt>Content</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of the <tt>Channel</tt>s
         * included into/associated with this <tt>Content</tt>
         */
        public List<Channel> getChannels()
        {
            return Collections.unmodifiableList(channels);
        }

        /**
         * Gets the name of the <tt>content</tt> represented by this instance.
         *
         * @return the name of the <tt>content</tt> represented by this instance
         */
        public String getName()
        {
            return name;
        }

        /**
         * Gets a list of the <tt>SctpConnection</tt>s included into/associated
         * with this <tt>Content</tt>.
         *
         * @return an unmodifiable <tt>List</tt> of the <tt>SctpConnection</tt>s
         * included into/associated with this <tt>Content</tt>
         */
        public List<SctpConnection> getSctpConnections()
        {
            return Collections.unmodifiableList(sctpConnections);
        }

        /**
         * Removes a specific <tt>Channel</tt> from the list of
         * <tt>Channel</tt>s included into this <tt>Content</tt>.
         *
         * @param channel the <tt>Channel</tt> to be excluded from this
         * <tt>Content</tt>
         * @return <tt>true</tt> if the list of <tt>Channel</tt>s included into
         * this <tt>Content</tt> was modified as a result of the execution of
         * the method; otherwise, <tt>false</tt>
         */
        public boolean removeChannel(Channel channel)
        {
            return channels.remove(channel);
        }

        /**
         * Sets the name of the <tt>content</tt> represented by this instance.
         *
         * @param name the name of the <tt>content</tt> represented by this
         * instance
         */
        public void setName(String name)
        {
            Objects.requireNonNull(name, "name");

            this.name = name;
        }

        /**
         * Appends the XML <tt>String</tt> representation of this
         * <tt>Content</tt> to a specific <tt>StringBuilder</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         * <tt>String</tt> representation of this <tt>Content</tt> is to be
         * appended
         */
        public IQChildElementXmlStringBuilder toXML(
            IQChildElementXmlStringBuilder xml)
        {
            xml.halfOpenElement(ELEMENT_NAME)
                .attribute(NAME_ATTR_NAME, getName());

            List<Channel> channels = getChannels();
            List<SctpConnection> connections = getSctpConnections();

            if (channels.size() == 0 && connections.size() == 0)
            {
                xml.closeEmptyElement();
            }
            else
            {
                xml.rightAngleBracket();
                for (Channel channel : channels)
                {
                    channel.toXML(xml);
                }

                for(SctpConnection conn : connections)
                {
                    conn.toXML(xml);
                }

                xml.closeElement(ELEMENT_NAME);
            }

            return xml;
        }

        /**
         * Removes given SCTP connection from this IQ.
         * @param connection the SCTP connection instance to be removed.
         * @return <tt>true</tt> if given <tt>connection</tt> was contained in
         *         this IQ and has been removed successfully.
         */
        public boolean removeSctpConnection(SctpConnection connection)
        {
            return sctpConnections.remove(connection);
        }
    }

    /**
     * Represents an 'endpoint' element.
     */
    public static class Endpoint
    {
        /**
         * The name of the 'displayname' attribute.
         */
        public static final String DISPLAYNAME_ATTR_NAME = "displayname";

        /**
         * The name of the 'endpoint' element.
         */
        public static final String ELEMENT_NAME = "endpoint";

        /**
         * The name of the 'id' attribute.
         */
        public static final String ID_ATTR_NAME = "id";

        /**
         * The name of the 'stats-id' attribute.
         */
        public static final String STATS_ID_ATTR_NAME = "stats-id";

        /**
         * The 'display name' of this <tt>Endpoint</tt>.
         */
        private String displayName;

        /**
         * The 'id' of this <tt>Endpoint</tt>.
         */
        private String id;

        /**
         * The 'stats-id' of this <tt>Endpoint</tt>.
         */
        private String statsId;

        /**
         * Initializes a new <tt>Endpoint</tt> with the given ID and display
         * name.
         * @param id the ID.
         * @param statsId stats ID value
         * @param displayName the display name.
         */
        public Endpoint(String id, String statsId, String displayName)
        {
            this.id = id;
            this.statsId = statsId;
            this.displayName = displayName;
        }

        /**
         * Returns the display name of this <tt>Endpoint</tt>.
         * @return the display name of this <tt>Endpoint</tt>.
         */
        public String getDisplayName()
        {
            return displayName;
        }

        /**
         * Returns the ID of this <tt>Endpoint</tt>.
         * @return the ID of this <tt>Endpoint</tt>.
         */
        public String getId()
        {
            return id;
        }

        /**
         * Returns the stats ID of this <tt>Endpoint</tt>.
         * @return the stats ID of this <tt>Endpoint</tt>.
         */
        public String getStatsId()
        {
            return statsId;
        }

        /**
         * Sets the display name of this <tt>Endpoint</tt>.
         * @param displayName the display name to set.
         */
        public void setDisplayName(String displayName)
        {
            this.displayName = displayName;
        }

        /**
         * Sets the ID of this <tt>Endpoint</tt>.
         * @param id the ID to set.
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * Sets the stats ID of this <tt>Endpoint</tt>.
         * @param statsId the stats ID to set.
         */
        public void setStatsId(String statsId)
        {
            this.statsId = statsId;
        }

        /**
         * Appends the XML <tt>String</tt> representation of this
         * <tt>Endpoint</tt> to <tt>xml</tt>.
         *
         * @param xml the <tt>StringBuilder</tt> to which the XML
         * <tt>String</tt> representation of this <tt>Endpoint</tt> is to be
         * appended
         */
        public IQChildElementXmlStringBuilder toXML(
            IQChildElementXmlStringBuilder xml)
        {
            xml.halfOpenElement(ELEMENT_NAME)
                .attribute(ID_ATTR_NAME, id);

            xml.optAttribute(DISPLAYNAME_ATTR_NAME, displayName);
            xml.optAttribute(STATS_ID_ATTR_NAME, statsId);
            xml.closeEmptyElement();

            return xml;
        }
    }

    /**
     * Represents a <tt>recording</tt> element.
     */
    public static class Recording
    {
        /**
         * The XML name of the <tt>recording</tt> element.
         */
        public static final String ELEMENT_NAME = "recording";

        /**
         * The XML name of the <tt>path</tt> attribute.
         */
        public static final String DIRECTORY_ATTR_NAME = "directory";

        /**
         * The XML name of the <tt>state</tt> attribute.
         */
        public static final String STATE_ATTR_NAME = "state";

        /**
         * The XML name of the <tt>token</tt> attribute.
         */
        public static final String TOKEN_ATTR_NAME = "token";

        /**
         * The target directory.
         */
        private String directory;

        /**
         * State of the recording..
         */
        private State state;

        /**
         * Access token.
         */
        private String token;

        /**
         * Construct new recording element.
         * @param state the state as string
         */
        public Recording(String state)
        {
            this.state = State.parseString(state);
        }

        /**
         * Construct new recording element.
         * @param state
         */
        public Recording(State state)
        {
            this.state = state;
        }

        /**
         * Construct new recording element.
         * @param state the state as string
         * @param token the token to authenticate
         */
        public Recording(String state, String token)
        {
            this(State.parseString(state), token);
        }
        /**
         * Construct new recording element.
         * @param state the state
         * @param token the token to authenticate
         */
        public Recording(State state, String token)
        {
            this(state);

            this.token = token;
        }

        public String getDirectory()
        {
            return directory;
        }

        public State getState()
        {
            return state;
        }

        public String getToken()
        {
            return token;
        }

        public void setToken(String token)
        {
            this.token = token;
        }

        public void setDirectory(String directory)
        {
            this.directory = directory;
        }

        public IQChildElementXmlStringBuilder toXML(IQChildElementXmlStringBuilder xml)
        {
            xml.halfOpenElement(ELEMENT_NAME)
                .attribute(STATE_ATTR_NAME, state.toString())
                .optAttribute(TOKEN_ATTR_NAME, token)
                .optAttribute(DIRECTORY_ATTR_NAME, directory)
                .closeEmptyElement();
            return xml;
        }

        /**
         * The recording state.
         */
        public enum State
        {
            /**
             * Recording is started.
             */
            ON("on"),
            /**
             * Recording is stopped.
             */
            OFF("off"),
            /**
             * Recording is pending. Record has been requested but no conference
             * has been established and it will be started once this is done.
             */
            PENDING("pending");

            /**
             * The name.
             */
            private String name;

            /**
             * Constructs new state.
             * @param name
             */
            private State(String name)
            {
                this.name = name;
            }

            /**
             * Returns state name.
             * @return returns state name.
             */
            public String toString()
            {
                return name;
            }

            /**
             * Parses state.
             * @param s state name.
             * @return the state found.
             */
            public static State parseString(String s)
            {
                if (ON.toString().equalsIgnoreCase(s))
                    return ON;
                else if (PENDING.toString().equalsIgnoreCase(s))
                    return PENDING;
                return OFF;
            }
        }
    }

    /**
     * Packet extension indicating graceful shutdown in progress status.
     */
    public static class GracefulShutdown
        extends AbstractPacketExtension
    {
        public static final String ELEMENT_NAME = "graceful-shutdown";

        public static final String NAMESPACE = ColibriConferenceIQ.NAMESPACE;

        public GracefulShutdown()
        {
            super(ColibriConferenceIQ.NAMESPACE, ELEMENT_NAME);
        }
    }

    public static class RTCPTerminationStrategy
    {
        public static final String ELEMENT_NAME = "rtcp-termination-strategy";

        public static final String NAME_ATTR_NAME = "name";

        private String name;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public IQChildElementXmlStringBuilder toXML(IQChildElementXmlStringBuilder xml)
        {
            xml.halfOpenElement(ELEMENT_NAME)
                .attribute(NAME_ATTR_NAME, name)
                .closeEmptyElement();
            return xml;
        }
    }

    /**
     * Represents a <tt>SCTP connection</tt> included into a <tt>content</tt>
     * of a Jitsi Videobridge <tt>conference</tt> IQ.
     *
     * @author Pawel Domas
     */
    public static class SctpConnection
        extends ChannelCommon
    {
        /**
         * The XML element name of a <tt>content</tt> of a Jitsi Videobridge
         * <tt>conference</tt> IQ.
         */
        public static final String ELEMENT_NAME = "sctpconnection";

        /**
         * The XML name of the <tt>port</tt> attribute of a
         * <tt>SctpConnection</tt> of a <tt>conference</tt> IQ which represents
         * the SCTP port property of
         * <tt>ColibriConferenceIQ.SctpConnection</tt>.
         */
        public static final String PORT_ATTR_NAME = "port";

        /**
         * SCTP port attribute. 5000 by default.
         */
        private int port = 5000;

        /**
         * Initializes a new <tt>SctpConnection</tt> instance without an
         * endpoint name and with default port value set.
         */
        public SctpConnection()
        {
            super(SctpConnection.ELEMENT_NAME);
        }

        /**
         * Gets the SCTP port of the <tt>SctpConnection</tt> described by this
         * instance.
         *
         * @return the SCTP port of the <tt>SctpConnection</tt> represented by
         *         this instance.
         */
        public int getPort()
        {
            return port;
        }

        /**
         * {@inheritDoc}
         *
         * No content other than transport for <tt>SctpConnection</tt>.
         */
        @Override
        protected boolean hasContent()
        {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IQChildElementXmlStringBuilder printAttributes(IQChildElementXmlStringBuilder xml)
        {
            xml.attribute(PORT_ATTR_NAME, getPort());
            return xml;
        }

        @Override
        protected IQChildElementXmlStringBuilder printContent(IQChildElementXmlStringBuilder xml)
        {
            // No other content than the transport shared from ChannelCommon
            return xml;
        }

        /**
         * Sets the SCTP port of the <tt>SctpConnection</tt> represented by this
         * instance.
         *
         * @param port the SCTP port of the <tt>SctpConnection</tt>
         *             represented by this instance
         */
        public void setPort(int port)
        {
            this.port = port;
        }
    }
}
