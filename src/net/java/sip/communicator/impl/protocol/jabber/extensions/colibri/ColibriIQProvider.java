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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Implements an <tt>org.jivesoftware.smack.provider.IQProvider</tt> for the
 * Jitsi Videobridge extension <tt>ColibriConferenceIQ</tt>.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class ColibriIQProvider
    implements IQProvider
{
    /** Initializes a new <tt>ColibriIQProvider</tt> instance. */
    public ColibriIQProvider()
    {
        ProviderManager providerManager = ProviderManager.getInstance();

        providerManager.addExtensionProvider(
                PayloadTypePacketExtension.ELEMENT_NAME,
                ColibriConferenceIQ.NAMESPACE,
                new DefaultPacketExtensionProvider<PayloadTypePacketExtension>(
                        PayloadTypePacketExtension.class));
        providerManager.addExtensionProvider(
                RtcpFbPacketExtension.ELEMENT_NAME,
                RtcpFbPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<RtcpFbPacketExtension>(
                        RtcpFbPacketExtension.class));
        providerManager.addExtensionProvider(
                RTPHdrExtPacketExtension.ELEMENT_NAME,
                ColibriConferenceIQ.NAMESPACE,
                new DefaultPacketExtensionProvider<RTPHdrExtPacketExtension>(
                        RTPHdrExtPacketExtension.class));
        providerManager.addExtensionProvider(
                SourcePacketExtension.ELEMENT_NAME,
                SourcePacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<SourcePacketExtension>(
                        SourcePacketExtension.class));
        providerManager.addExtensionProvider(
                SourceGroupPacketExtension.ELEMENT_NAME,
                SourceGroupPacketExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<SourceGroupPacketExtension>(
                        SourceGroupPacketExtension.class));

        PacketExtensionProvider parameterProvider
            = new DefaultPacketExtensionProvider<ParameterPacketExtension>(
                    ParameterPacketExtension.class);

        providerManager.addExtensionProvider(
                ParameterPacketExtension.ELEMENT_NAME,
                ColibriConferenceIQ.NAMESPACE,
                parameterProvider);
        providerManager.addExtensionProvider(
                ParameterPacketExtension.ELEMENT_NAME,
                SourcePacketExtension.NAMESPACE,
                parameterProvider);
        // Shutdown IQ
        providerManager.addIQProvider(
                GracefulShutdownIQ.ELEMENT_NAME,
                GracefulShutdownIQ.NAMESPACE,
                this);
        // Shutdown extension
        PacketExtensionProvider shutdownProvider
            = new DefaultPacketExtensionProvider
                    <ColibriConferenceIQ.GracefulShutdown>(
                            ColibriConferenceIQ.GracefulShutdown.class);

        providerManager.addExtensionProvider(
            ColibriConferenceIQ.GracefulShutdown.ELEMENT_NAME,
            ColibriConferenceIQ.GracefulShutdown.NAMESPACE,
            shutdownProvider);

        // ColibriStatsIQ
        providerManager.addIQProvider(
            ColibriStatsIQ.ELEMENT_NAME,
            ColibriStatsIQ.NAMESPACE,
            this);

        // ColibriStatsExtension
        PacketExtensionProvider statsProvider
            = new DefaultPacketExtensionProvider<ColibriStatsExtension>(
                    ColibriStatsExtension.class);

        providerManager.addExtensionProvider(
            ColibriStatsExtension.ELEMENT_NAME,
            ColibriStatsExtension.NAMESPACE,
            statsProvider);
        // ColibriStatsExtension.Stat
        PacketExtensionProvider statProvider
            = new DefaultPacketExtensionProvider<ColibriStatsExtension.Stat>(
                    ColibriStatsExtension.Stat.class);

        providerManager.addExtensionProvider(
            ColibriStatsExtension.Stat.ELEMENT_NAME,
            ColibriStatsExtension.NAMESPACE,
            statProvider);
    }

    private void addChildExtension(
            ColibriConferenceIQ.Channel channel,
            PacketExtension childExtension)
    {
        if (childExtension instanceof PayloadTypePacketExtension)
        {
            PayloadTypePacketExtension payloadType
                = (PayloadTypePacketExtension) childExtension;

            if ("opus".equals(payloadType.getName())
                    && (payloadType.getChannels() != 2))
            {
                /*
                 * We only have a Format for opus with 2 channels, because it
                 * MUST be advertised with 2 channels. Fixing the number of
                 * channels here allows us to be compatible with agents who
                 * advertise it with 1 channel.
                 */
                payloadType.setChannels(2);
            }
            channel.addPayloadType(payloadType);
        }
        else if (childExtension instanceof IceUdpTransportPacketExtension)
        {
            IceUdpTransportPacketExtension transport
                = (IceUdpTransportPacketExtension) childExtension;

            channel.setTransport(transport);
        }
        else if (childExtension instanceof SourceGroupPacketExtension)
        {
            SourceGroupPacketExtension sourceGroup
                    = (SourceGroupPacketExtension)childExtension;

            channel.addSourceGroup(sourceGroup);
        }
        else if (childExtension instanceof RTPHdrExtPacketExtension)
        {
            RTPHdrExtPacketExtension rtpHdrExtPacketExtension
                    = (RTPHdrExtPacketExtension) childExtension;

            channel.addRtpHeaderExtension(rtpHdrExtPacketExtension);
        }
    }

    private void addChildExtension(
            ColibriConferenceIQ.ChannelBundle bundle,
            PacketExtension childExtension)
    {
        if (childExtension instanceof IceUdpTransportPacketExtension)
        {
            IceUdpTransportPacketExtension transport
                = (IceUdpTransportPacketExtension) childExtension;

            bundle.setTransport(transport);
        }
    }

    private void addChildExtension(
            ColibriConferenceIQ.SctpConnection sctpConnection,
            PacketExtension childExtension)
    {
        if (childExtension instanceof IceUdpTransportPacketExtension)
        {
            IceUdpTransportPacketExtension transport
                = (IceUdpTransportPacketExtension) childExtension;

            sctpConnection.setTransport(transport);
        }
    }

    private PacketExtension parseExtension(
            XmlPullParser parser,
            String name,
            String namespace)
        throws Exception
    {
        PacketExtensionProvider extensionProvider
            = (PacketExtensionProvider)
                ProviderManager.getInstance().getExtensionProvider(
                        name,
                        namespace);
        PacketExtension extension;

        if (extensionProvider == null)
        {
            /*
             * No PacketExtensionProvider for the specified name and namespace
             * has been registered. Throw away the element.
             */
            throwAway(parser, name);
            extension = null;
        }
        else
        {
            extension = extensionProvider.parseExtension(parser);
        }
        return extension;
    }

    /**
     * Parses an IQ sub-document and creates an
     * <tt>org.jivesoftware.smack.packet.IQ</tt> instance.
     *
     * @param parser an <tt>XmlPullParser</tt> which specifies the IQ
     * sub-document to be parsed into a new <tt>IQ</tt> instance
     * @return a new <tt>IQ</tt> instance parsed from the specified IQ
     * sub-document
     * @see IQProvider#parseIQ(XmlPullParser)
     */
    @SuppressWarnings("deprecation") // Compatibility with legacy Jitsi and
                                     // Jitsi Videobridge
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        String namespace = parser.getNamespace();
        IQ iq;

        if (ColibriConferenceIQ.ELEMENT_NAME.equals(parser.getName())
                && ColibriConferenceIQ.NAMESPACE.equals(namespace))
        {
            ColibriConferenceIQ conference = new ColibriConferenceIQ();
            String conferenceID = parser
                    .getAttributeValue("", ColibriConferenceIQ.ID_ATTR_NAME);

            if ((conferenceID != null) && (conferenceID.length() != 0))
                conference.setID(conferenceID);

            boolean done = false;
            ColibriConferenceIQ.Channel channel = null;
            ColibriConferenceIQ.RTCPTerminationStrategy rtcpTerminationStrategy
                    = null;
            ColibriConferenceIQ.SctpConnection sctpConnection = null;
            ColibriConferenceIQ.ChannelBundle bundle = null;
            ColibriConferenceIQ.Content content = null;
            ColibriConferenceIQ.Recording recording = null;
            ColibriConferenceIQ.Endpoint conferenceEndpoint = null;
            StringBuilder ssrc = null;
            SourcePacketExtension ssrcPe = null;

            while (!done)
            {
                switch (parser.next())
                {
                case XmlPullParser.END_TAG:
                {
                    String name = parser.getName();

                    if (ColibriConferenceIQ.ELEMENT_NAME.equals(name))
                    {
                        done = true;
                    }
                    else if (ColibriConferenceIQ.Channel.ELEMENT_NAME.equals(
                            name))
                    {
                        content.addChannel(channel);
                        channel = null;
                    }
                    else if (ColibriConferenceIQ.SctpConnection.ELEMENT_NAME
                            .equals(name))
                    {
                        if (sctpConnection != null)
                            content.addSctpConnection(sctpConnection);

                        sctpConnection = null;
                    }
                    else if (ColibriConferenceIQ.ChannelBundle.ELEMENT_NAME
                            .equals(name))
                    {
                        if (bundle != null)
                        {
                            conference.addChannelBundle(bundle);

                            bundle = null;
                        }
                    }
                    else if (ColibriConferenceIQ.Endpoint.ELEMENT_NAME
                            .equals(name))
                    {
                        conference.addEndpoint(conferenceEndpoint);
                        conferenceEndpoint = null;
                    }
                    else if (ColibriConferenceIQ.Channel.SSRC_ELEMENT_NAME
                            .equals(name))
                    {
                        String s = ssrc.toString().trim();

                        if (s.length() != 0)
                        {
                            int i;

                            /*
                             * Legacy versions of Jitsi and Jitsi Videobridge
                             * may send a synchronization source (SSRC)
                             * identifier as a negative integer.
                             */
                            if (s.startsWith("-"))
                                i = Integer.parseInt(s);
                            else
                                i = (int) Long.parseLong(s);
                            channel.addSSRC(i);
                        }
                        ssrc = null;
                    }
                    else if (SourcePacketExtension.ELEMENT_NAME.equals(name))
                    {
                        if (channel != null && ssrcPe != null)
                        {
                            channel.addSource(ssrcPe);
                        }

                        ssrcPe = null;
                    }
                    else if (ColibriConferenceIQ.Content.ELEMENT_NAME.equals(
                            name))
                    {
                        conference.addContent(content);
                        content = null;
                    }
                    else if (ColibriConferenceIQ.RTCPTerminationStrategy
                            .ELEMENT_NAME.equals(name))
                    {
                        conference.setRTCPTerminationStrategy(
                                rtcpTerminationStrategy);
                        rtcpTerminationStrategy = null;
                    }
                    else if (ColibriConferenceIQ.Recording.ELEMENT_NAME.equals(
                            name))
                    {
                        conference.setRecording(recording);
                        recording = null;
                    }
                    else if (ColibriConferenceIQ.GracefulShutdown.ELEMENT_NAME
                        .equals(name))
                    {
                        conference.setGracefulShutdown(true);
                    }
                    break;
                }

                case XmlPullParser.START_TAG:
                {
                    String name = parser.getName();

                    if (ColibriConferenceIQ.Channel.ELEMENT_NAME.equals(name))
                    {
                        channel = new ColibriConferenceIQ.Channel();

                        // direction
                        String direction
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .DIRECTION_ATTR_NAME);

                        if ((direction != null) && (direction.length() != 0))
                        {
                            channel.setDirection(
                                    MediaDirection.parseString(direction));
                        }

                        // endpoint
                        String endpoint
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .ENDPOINT_ATTR_NAME);

                        if ((endpoint != null) && (endpoint.length() != 0))
                            channel.setEndpoint(endpoint);

                        String channelBundleId
                            = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.ChannelCommon
                                        .CHANNEL_BUNDLE_ID_ATTR_NAME);
                        if (!StringUtils.isNullOrEmpty(channelBundleId))
                            channel.setChannelBundleId(channelBundleId);

                        // expire
                        String expire
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .EXPIRE_ATTR_NAME);

                        if ((expire != null) && (expire.length() != 0))
                            channel.setExpire(Integer.parseInt(expire));

                        // host
                        String host
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel.HOST_ATTR_NAME);

                        if ((host != null) && (host.length() != 0))
                            channel.setHost(host);

                        // id
                        String channelID
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel.ID_ATTR_NAME);

                        if ((channelID != null) && (channelID.length() != 0))
                            channel.setID(channelID);

                        // initiator
                        String initiator
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .INITIATOR_ATTR_NAME);

                        if ((initiator != null) && (initiator.length() != 0))
                            channel.setInitiator(Boolean.valueOf(initiator));

                        // lastN
                        String lastN
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .LAST_N_ATTR_NAME);

                        if ((lastN != null) && (lastN.length() != 0))
                            channel.setLastN(Integer.parseInt(lastN));

                        String adaptiveLastN
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .ADAPTIVE_LAST_N_ATTR_NAME);

                        if (!StringUtils.isNullOrEmpty(adaptiveLastN))
                            channel.setAdaptiveLastN(
                                    Boolean.parseBoolean(adaptiveLastN));

                        // simulcastMode
                        String simulcastMode
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.Channel
                                        .SIMULCAST_MODE_ATTR_NAME);

                        if (!StringUtils.isNullOrEmpty(simulcastMode))
                            channel.setSimulcastMode(
                                    SimulcastMode.fromString(simulcastMode));

                        // receiving simulcast layer
                        String receivingSimulcastLayer
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.Channel
                                            .RECEIVING_SIMULCAST_LAYER);

                        if ((receivingSimulcastLayer != null)
                                && (receivingSimulcastLayer.length() != 0))
                            channel.setReceivingSimulcastLayer(
                                    Integer.parseInt(receivingSimulcastLayer));

                        // rtcpPort
                        String rtcpPort
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .RTCP_PORT_ATTR_NAME);

                        if ((rtcpPort != null) && (rtcpPort.length() != 0))
                            channel.setRTCPPort(Integer.parseInt(rtcpPort));

                        // rtpLevelRelayType
                        String rtpLevelRelayType
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .RTP_LEVEL_RELAY_TYPE_ATTR_NAME);

                        if ((rtpLevelRelayType != null)
                                && (rtpLevelRelayType.length() != 0))
                        {
                            channel.setRTPLevelRelayType(rtpLevelRelayType);
                        }

                        // rtpPort
                        String rtpPort
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Channel
                                            .RTP_PORT_ATTR_NAME);

                        if ((rtpPort != null) && (rtpPort.length() != 0))
                            channel.setRTPPort(Integer.parseInt(rtpPort));
                    }
                    else if (ColibriConferenceIQ.ChannelBundle
                            .ELEMENT_NAME.equals(name))
                    {
                        String bundleId
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ
                                        .ChannelBundle.ID_ATTR_NAME);

                        if(!StringUtils.isNullOrEmpty(bundleId))
                        {
                            bundle = new ColibriConferenceIQ
                                        .ChannelBundle(bundleId);
                        }
                    }
                    else if (ColibriConferenceIQ.RTCPTerminationStrategy
                            .ELEMENT_NAME.equals(name))
                    {
                        rtcpTerminationStrategy =
                                new ColibriConferenceIQ.RTCPTerminationStrategy();

                        // name
                        String strategyName
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.RTCPTerminationStrategy
                                        .NAME_ATTR_NAME);

                        if ((strategyName != null)
                                && (strategyName.length() != 0))
                            rtcpTerminationStrategy.setName(strategyName);

                    }
                    else if (ColibriConferenceIQ.Channel.SSRC_ELEMENT_NAME
                            .equals(name))
                    {
                        ssrc = new StringBuilder();
                    }
                    else if (SourcePacketExtension.ELEMENT_NAME.equals(name))
                    {
                        ssrcPe = new SourcePacketExtension();

                        String ssrcStr
                            = parser.getAttributeValue(
                                    "",
                                    SourcePacketExtension.SSRC_ATTR_NAME);

                        if (!StringUtils.isNullOrEmpty(ssrcStr))
                        {
                            ssrcPe.setSSRC(Long.parseLong(ssrcStr));
                        }
                    }
                    else if (ColibriConferenceIQ.Content.ELEMENT_NAME.equals(
                            name))
                    {
                        content = new ColibriConferenceIQ.Content();

                        String contentName
                            = parser.getAttributeValue(
                                    "",
                                    ColibriConferenceIQ.Content.NAME_ATTR_NAME);

                        if ((contentName != null)
                                && (contentName.length() != 0))
                            content.setName(contentName);
                    }
                    else if (ColibriConferenceIQ.Recording.ELEMENT_NAME.equals(
                            name))
                    {
                        String stateStr
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.Recording.STATE_ATTR_NAME);
                        boolean state = Boolean.parseBoolean(stateStr);

                        String token
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.Recording.TOKEN_ATTR_NAME);

                        recording
                                = new ColibriConferenceIQ.Recording(
                                state,
                                token);
                    }
                    else if (ColibriConferenceIQ.SctpConnection.ELEMENT_NAME
                        .equals(name))
                    {
                        // Endpoint
                        String endpoint
                            = parser.getAttributeValue(
                            "",
                            ColibriConferenceIQ.
                                SctpConnection.ENDPOINT_ATTR_NAME);

                        // id
                        String connID
                            = parser.getAttributeValue(
                            "",
                            ColibriConferenceIQ.
                                ChannelCommon.ID_ATTR_NAME);

                        if(StringUtils.isNullOrEmpty(connID)
                           && StringUtils.isNullOrEmpty(endpoint))
                        {
                            sctpConnection = null;
                            continue;
                        }

                        sctpConnection
                            = new ColibriConferenceIQ.SctpConnection();

                        if (!StringUtils.isNullOrEmpty(connID))
                            sctpConnection.setID(connID);

                        if (!StringUtils.isNullOrEmpty(endpoint))
                            sctpConnection.setEndpoint(endpoint);

                        // port
                        String port
                            = parser.getAttributeValue(
                            "",
                            ColibriConferenceIQ.SctpConnection.PORT_ATTR_NAME);
                        if (!StringUtils.isNullOrEmpty(port))
                            sctpConnection.setPort(Integer.parseInt(port));

                        String channelBundleId
                            = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.ChannelCommon
                                        .CHANNEL_BUNDLE_ID_ATTR_NAME);
                        if (!StringUtils.isNullOrEmpty(channelBundleId))
                            sctpConnection.setChannelBundleId(channelBundleId);

                        // initiator
                        String initiator
                            = parser.getAttributeValue(
                            "",
                            ColibriConferenceIQ.SctpConnection
                                .INITIATOR_ATTR_NAME);

                        if (!StringUtils.isNullOrEmpty(initiator))
                            sctpConnection.setInitiator(
                                Boolean.valueOf(initiator));

                        // expire
                        String expire
                            = parser.getAttributeValue(
                            "",
                            ColibriConferenceIQ.SctpConnection
                                .EXPIRE_ATTR_NAME);

                        if (!StringUtils.isNullOrEmpty(expire))
                            sctpConnection.setExpire(Integer.parseInt(expire));
                    }
                    else if (ColibriConferenceIQ.Endpoint.ELEMENT_NAME
                            .equals(name))
                    {
                        String id
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.Endpoint.ID_ATTR_NAME);

                        String endpointName
                                = parser.getAttributeValue(
                                "",
                                ColibriConferenceIQ.Endpoint
                                        .DISPLAYNAME_ATTR_NAME);

                        conferenceEndpoint
                                = new ColibriConferenceIQ.Endpoint(id,
                                                                   endpointName);

                    }
                    else if ( channel != null
                              || sctpConnection != null
                              || bundle != null )
                    {
                        String peName = null;
                        String peNamespace = null;

                        if (IceUdpTransportPacketExtension.ELEMENT_NAME
                                    .equals(name)
                                && IceUdpTransportPacketExtension.NAMESPACE
                                        .equals(parser.getNamespace()))
                        {
                            peName = name;
                            peNamespace
                                = IceUdpTransportPacketExtension.NAMESPACE;
                        }
                        else if (PayloadTypePacketExtension.ELEMENT_NAME.equals(
                                name))
                        {
                            /*
                             * The channel element of the Jitsi Videobridge
                             * protocol reuses the payload-type element defined
                             * in XEP-0167: Jingle RTP Sessions.
                             */
                            peName = name;
                            peNamespace = namespace;
                        }
                        else if (RtcpFbPacketExtension.ELEMENT_NAME.equals(
                                name)
                                && RtcpFbPacketExtension.NAMESPACE
                                .equals(parser.getNamespace()))
                        {
                            /*
                             * The channel element of the Jitsi Videobridge
                             * protocol reuses the payload-type element defined
                             * in XEP-0167: Jingle RTP Sessions.
                             */
                            peName = name;
                            peNamespace = namespace;
                        }
                        else if (RTPHdrExtPacketExtension.ELEMENT_NAME.equals(
                                name))
                        {
                            /*
                             * The channel element of the Jitsi Videobridge
                             * protocol reuses the rtp-hdrext element defined
                             * in XEP-0167: Jingle RTP Sessions.
                             */
                            peName = name;
                            peNamespace = namespace;
                        }
                        else if (RawUdpTransportPacketExtension.ELEMENT_NAME
                                    .equals(name)
                                && RawUdpTransportPacketExtension.NAMESPACE
                                        .equals(parser.getNamespace()))
                        {
                            peName = name;
                            peNamespace
                                = RawUdpTransportPacketExtension.NAMESPACE;
                        }
                        else if (SourcePacketExtension.ELEMENT_NAME.equals(name)
                                && SourcePacketExtension.NAMESPACE.equals(
                                        parser.getNamespace()))
                        {
                            peName = name;
                            peNamespace = SourcePacketExtension.NAMESPACE;
                        }
                        else if (SourceGroupPacketExtension.ELEMENT_NAME
                                                .equals(name)
                                && SourceGroupPacketExtension.NAMESPACE
                                                .equals(parser.getNamespace()))
                        {
                            peName = name;
                            peNamespace = SourceGroupPacketExtension.NAMESPACE;
                        }
                        if (peName == null)
                        {
                            throwAway(parser, name);
                        }
                        else
                        {
                            PacketExtension extension
                                = parseExtension(parser, peName, peNamespace);

                            if (extension != null)
                            {
                                if(channel != null)
                                    addChildExtension(channel, extension);
                                else if (sctpConnection != null)
                                    addChildExtension(sctpConnection,
                                                      extension);
                                else
                                    addChildExtension(bundle, extension);
                            }
                        }
                    }
                    break;
                }

                case XmlPullParser.TEXT:
                {
                    if (ssrc != null)
                        ssrc.append(parser.getText());
                    break;
                }
                }
            }

            iq = conference;
        }
        else if (GracefulShutdownIQ.ELEMENT_NAME.equals(parser.getName())
                    && GracefulShutdownIQ.NAMESPACE.equals(namespace))
        {
            String rootElement = parser.getName();

            iq = new GracefulShutdownIQ();

            boolean done = false;

            while (!done)
            {
                switch (parser.next())
                {
                    case XmlPullParser.END_TAG:
                    {
                        String name = parser.getName();

                        if (rootElement.equals(name))
                        {
                            done = true;
                        }
                        break;
                    }

                    case XmlPullParser.TEXT:
                    {
                        // Parse some text here
                        break;
                    }
                }
            }
        }
        else if (ColibriStatsIQ.ELEMENT_NAME.equals(parser.getName())
            && ColibriStatsIQ.NAMESPACE.equals(namespace))
        {
            String rootElement = parser.getName();

            ColibriStatsIQ statsIQ = new ColibriStatsIQ();
            iq = statsIQ;
            ColibriStatsExtension.Stat stat = null;

            boolean done = false;

            while (!done)
            {
                switch (parser.next())
                {
                    case XmlPullParser.START_TAG:
                    {
                        String name = parser.getName();

                        if (ColibriStatsExtension.Stat
                                    .ELEMENT_NAME.equals(name))
                        {
                            stat = new ColibriStatsExtension.Stat();

                            String statName
                                = parser.getAttributeValue(
                                    "",
                                    ColibriStatsExtension.Stat.NAME_ATTR_NAME);
                            stat.setName(statName);

                            String statValue
                                = parser.getAttributeValue(
                                    "",
                                    ColibriStatsExtension.Stat.VALUE_ATTR_NAME);
                            stat.setValue(statValue);
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG:
                    {
                        String name = parser.getName();

                        if (rootElement.equals(name))
                        {
                            done = true;
                        }
                        else if (ColibriStatsExtension.Stat.ELEMENT_NAME
                            .equals(name))
                        {
                            if (stat != null)
                            {
                                statsIQ.addStat(stat);
                                stat = null;
                            }
                        }
                        break;
                    }

                    case XmlPullParser.TEXT:
                    {
                        // Parse some text here
                        break;
                    }
                }
            }
        }
        else
            iq = null;

        return iq;
    }

    /**
     * Parses using a specific <tt>XmlPullParser</tt> and ignores XML content
     * presuming that the specified <tt>parser</tt> is currently at the start
     * tag of an element with a specific name and throwing away until the end
     * tag with the specified name is encountered.
     *
     * @param parser the <tt>XmlPullParser</tt> which parses the XML content
     * @param name the name of the element at the start tag of which the
     * specified <tt>parser</tt> is presumed to currently be and until the end
     * tag of which XML content is to be thrown away
     * @throws Exception if an errors occurs while parsing the XML content
     */
    private void throwAway(XmlPullParser parser, String name)
        throws Exception
    {
        while ((XmlPullParser.END_TAG != parser.next())
                || !name.equals(parser.getName()));
    }
}
