/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.httputil.HttpUtils.HTTPResponseResult;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.xmpp.jnodes.smack.*;

/**
 * <tt>TransportManager</tt>s gather local candidates for incoming and outgoing
 * calls. Their work starts by calling a start method which, using the remote
 * peer's session description, would start the harvest. Calling a second wrapup
 * method would deliver the candidate harvest, possibly after blocking if it has
 * not yet completed.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class TransportManagerGTalkImpl
    extends TransportManager<CallPeerGTalkImpl>
    implements PropertyChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>IceUdpTransportManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(TransportManagerGTalkImpl.class);

    /**
     * Default STUN server address.
     */
    private static final String DEFAULT_STUN_SERVER_ADDRESS = "stun.jitsi.net";

    /**
     * Default STUN server port.
     */
    private static final int DEFAULT_STUN_SERVER_PORT = 3478;

    /**
     * The generation of the candidates we are currently generating
     */
    private int currentGeneration = 0;

    /**
     * The ID that we will be assigning to our next candidate. We use
     * <tt>int</tt>s for interoperability reasons (Emil: I believe that GTalk
     * uses <tt>int</tt>s. If that turns out not to be the case we can stop
     * using <tt>int</tt>s here if that's an issue).
     */
    private static int nextID = 1;

    /**
     * The ICE <tt>Component</tt> IDs in their common order used, for example,
     * by <tt>DefaultStreamConnector</tt>, <tt>MediaStreamTarget</tt>.
     */
    private static final int[] COMPONENT_IDS
        = new int[] { Component.RTP, Component.RTCP };

    /**
     * The ICE agent that this transport manager would be using for ICE
     * negotiation.
     */
    private final Agent iceAgent;

    /**
     * Synchronization object.
     */
    private final Object wrapupSyncRoot = new Object();

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeer} whose traffic we will be taking
     * care of.
     */
    public TransportManagerGTalkImpl(CallPeerGTalkImpl callPeer)
    {
        super(callPeer);
        iceAgent = createIceAgent();
        iceAgent.addStateChangeListener(this);
    }

    /**
     * Returns the ID that we will be assigning to the next candidate we create.
     *
     * @return the next ID to use with a candidate.
     */
    protected String getNextID()
    {
        return Integer.toString(nextID++);
    }

    /**
     * Returns the generation that our current candidates belong to.
     *
     * @return the generation that we should assign to candidates that we are
     * currently advertising.
     */
    protected int getCurrentGeneration()
    {
        return currentGeneration;
    }

    /**
     * Increments the generation that we are assigning candidates.
     */
    protected void incrementGeneration()
    {
        currentGeneration++;
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers.
     *
     * @param peer the CallPeer that we would contact.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/IP/FQDN
     */
    @Override
    protected InetAddress getIntendedDestination(CallPeerGTalkImpl peer)
    {
        return peer.getProtocolProvider().getNextHop();
    }

    /**
     * Request Google's Jingle info.
     *
     * @param provider <tt>ProtocolProviderService</tt> instance
     * @return list of servers
     */
    public static List<StunServerDescriptor> requestJingleInfo(
        ProtocolProviderServiceJabberImpl provider)
    {
        JingleInfoQueryIQ iq = new JingleInfoQueryIQ();
        //ProtocolProviderServiceJabberImpl provider =
        //    getCallPeer().getProtocolProvider();
        String accountIDService = provider.getAccountID().getService();
        boolean jingleInfoIsSupported
            = provider.isFeatureSupported(accountIDService,
                    JingleInfoQueryIQ.NAMESPACE);
        List<StunServerDescriptor> servers =
            new ArrayList<StunServerDescriptor>();

        // check for google:jingleinfo support
        if(!jingleInfoIsSupported)
        {
            return servers;
        }

        if(logger.isDebugEnabled())
            logger.debug("google:jingleinfo supported for " +
                    provider.getOurJID());

        iq.setFrom(provider.getOurJID());
        iq.setTo(StringUtils.parseBareAddress(
                provider.getOurJID()));
        iq.setType(Type.GET);

        XMPPConnection connection = provider.getConnection();
        PacketCollector collector = connection.createPacketCollector(
                new PacketIDFilter(iq.getPacketID()));
        provider.getConnection().sendPacket(iq);
        Packet p = collector.nextResult(
                SmackConfiguration.getPacketReplyTimeout());
        if(p != null)
        {
            JingleInfoQueryIQ response_iq = (JingleInfoQueryIQ)p;
            Iterator<PacketExtension> it =
                response_iq.getExtensions().iterator();

            while(it.hasNext())
            {
                AbstractPacketExtension ext =
                    (AbstractPacketExtension)it.next();

                if(ext.getElementName().equals(
                            StunPacketExtension.ELEMENT_NAME))
                {
                    for(ServerPacketExtension e :
                            ext.getChildExtensionsOfType(
                                ServerPacketExtension.class))
                    {
                        StunServerDescriptor dsc = new StunServerDescriptor(
                                e.getHost(),
                                e.getUdp(),
                                false,
                                null,
                                null);
                        servers.add(dsc);
                    }
                }
                else if(ext.getElementName().equals(
                            RelayPacketExtension.ELEMENT_NAME))
                {
                    String token = ((RelayPacketExtension)ext).getToken();
                    for(ServerPacketExtension e :
                            ext.getChildExtensionsOfType(
                                ServerPacketExtension.class))
                    {
                        String headerNames[] = new String[2];
                        String headerValues[] = new String[2];
                        String addr = "http://"
                            + e.getHost()
                            + "/create_session";

                        headerNames[0] = "X-Talk-Google-Relay-Auth";
                        headerNames[1] = "X-Google-Relay-Auth";
                        headerValues[0] = token;
                        headerValues[1] = token;

                        HTTPResponseResult res = HttpUtils.openURLConnection(
                                addr,
                                headerNames,
                                headerValues);
                        Hashtable<String, String> relayData = null;

                        try
                        {
                            relayData =
                                parseGoogleRelay(res.getContentString());
                        }
                        catch (IOException excpt)
                        {
                            logger.info("HTTP query to " + e.getHost() +
                                    "failed", excpt);
                            break;
                        }

                        String user = relayData.get("username");
                        String password = relayData.get("password");
                        StunServerDescriptor dsc =
                            new StunServerDescriptor(
                                    relayData.get("relay"),
                                    Integer.parseInt(relayData.get("udpport")),
                                    true,
                                    user,
                                    password);
                        // not the RFC5766 TURN support
                        dsc.setOldTurn(true);
                        servers.add(dsc);

                        dsc = new StunServerDescriptor(
                                relayData.get("relay"),
                                Integer.parseInt(relayData.get("tcpport")),
                                true,
                                user,
                                password);
                        dsc.setOldTurn(true);
                        dsc.setProtocol("tcp");
                        servers.add(dsc);

                        dsc = new StunServerDescriptor(
                                relayData.get("relay"),
                                Integer.parseInt(relayData.get("ssltcpport")),
                                true,
                                user,
                                password);
                        dsc.setOldTurn(true);
                        dsc.setProtocol("ssltcp");
                        servers.add(dsc);
                    }
                }
            }
        }

        collector.cancel();

        return servers;
    }

    /**
     * Parse HTTP response from Google relay.
     *
     * @param res content string
     * @return String
     */
    public static Hashtable<String, String> parseGoogleRelay(String res)
    {
        // Separate each line
        StringTokenizer tokenizer = new StringTokenizer(res, "\n");
        Hashtable<String, String> ret = new Hashtable<String, String>();

        while(tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();

            if(token.startsWith("relay.ip="))
            {
                ret.put("relay", token.substring(token.indexOf("=") + 1));
            }
            else if(token.startsWith("relay.udp_port="))
            {
                ret.put("udpport", token.substring(token.indexOf("=") + 1));
            }
            else if(token.startsWith("relay.tcp_port="))
            {
                ret.put("tcpport", token.substring(token.indexOf("=") + 1));
            }
            else if(token.startsWith("relay.ssltcp_port="))
            {
                ret.put("ssltcpport", token.substring(token.indexOf("=") + 1));
            }
            else if(token.startsWith("username="))
            {
                ret.put("username", token.substring(token.indexOf("=") + 1));
            }
            else if(token.startsWith("password="))
            {
                ret.put("password", token.substring(token.indexOf("=") + 1));
            }

        }
        return ret;
    }

    /**
     * Creates the ICE agent that we would be using in this transport manager
     * for all negotiation.
     *
     * @param provider <tt>ProtocolProviderServiceJabberImpl</tt> instance
     * @param controlling if the peer is controlling or not
     * @return the ICE agent to use for all the ICE negotiation that this
     * transport manager would be going through
     */
    static Agent createAgent(ProtocolProviderServiceJabberImpl provider,
        boolean controlling)
    {
        Agent agent = new Agent(CompatibilityMode.GTALK);
        List<StunServerDescriptor> servers = null;
        boolean atLeastOneStunServer = false;

        JabberAccountID accID = (JabberAccountID)provider.getAccountID();

        agent.setControlling(controlling);

        servers = requestJingleInfo(provider);

        //servers.addAll(accID.getStunServers());

        for(StunServerDescriptor desc : servers)
        {
            Transport transport;

            /* Google ssltcp mode is in fact pseudo-SSL (just the
             * client/server hello)
             */
            if(desc.getProtocol().equals("ssltcp"))
            {
                transport = Transport.TCP;
            }
            else
            {
                transport = Transport.parse(desc.getProtocol());
            }

            TransportAddress addr = new TransportAddress(
                            desc.getAddress(),
                            desc.getPort(),
                            transport);

            // if we get STUN server from automatic discovery, it may just
            // be server name (i.e. stun.domain.org) and it may be possible
            // that it cannot be resolved
            if(addr.getAddress() == null)
            {
                logger.info("Unresolved address for " + addr);
                continue;
            }

            StunCandidateHarvester harvester = null;

            if(desc.isTurnSupported())
            {
                logger.info("Google TURN descriptor");
                /* Google relay server used a special way to allocate
                 * address (token + HTTP request, ...) and they don't
                 * support long-term authentication
                 */
                if(desc.isOldTurn())
                {
                    logger.info("new Google TURN harvester");
                    if(desc.getProtocol().equals("ssltcp"))
                    {
                        harvester = new GoogleTurnSSLCandidateHarvester(
                            addr,
                            new String(desc.getUsername()),
                            new String(desc.getPassword()));
                    }
                    else
                        harvester = new GoogleTurnCandidateHarvester(
                            addr,
                            new String(desc.getUsername()),
                            new String(desc.getPassword()));
                }
            }
            else
            {
                // take only the first STUN server for now
                if(atLeastOneStunServer)
                    continue;

                //this is a STUN only server
                harvester = new StunCandidateHarvester(addr);
                atLeastOneStunServer = true;
                logger.info("Found Google STUN server " + harvester);
            }

            if(harvester != null)
            {
                agent.addCandidateHarvester(harvester);
            }
        }

        if(!atLeastOneStunServer)
        {
            /* we have no configured or discovered STUN server so takes the
             * default provided by us if user allows it
             */
            if(accID.isUseDefaultStunServer())
            {
                TransportAddress addr = new TransportAddress(
                        DEFAULT_STUN_SERVER_ADDRESS,
                        DEFAULT_STUN_SERVER_PORT,
                        Transport.UDP);
                StunCandidateHarvester harvester =
                    new StunCandidateHarvester(addr);

                if(harvester != null)
                {
                    agent.addCandidateHarvester(harvester);
                }
            }
        }

        /* Jingle nodes candidate */
        if(accID.isJingleNodesRelayEnabled())
        {
            SmackServiceNode serviceNode =
                provider.getJingleNodesServiceNode();

            if(serviceNode != null)
            {
                JingleNodesHarvester harvester = new JingleNodesHarvester(
                        serviceNode);

                if(harvester != null)
                {
                    agent.addCandidateHarvester(harvester);
                }
            }
        }

        if(accID.isUPNPEnabled())
        {
            UPNPHarvester harvester = new UPNPHarvester();

            if(harvester != null)
            {
                agent.addCandidateHarvester(harvester);
            }
        }

        return agent;
    }

    /**
     * Creates the ICE agent that we would be using in this transport manager
     * for all negotiation.
     *
     * @return the ICE agent to use for all the ICE negotiation that this
     * transport manager would be going through
     */
    private Agent createIceAgent()
    {
        CallPeerGTalkImpl peer = getCallPeer();
        ProtocolProviderServiceJabberImpl provider = peer.getProtocolProvider();

        return createAgent(provider, !peer.isInitiator());
    }

    /**
     * Initializes a new <tt>StreamConnector</tt> to be used as the
     * <tt>connector</tt> of the <tt>MediaStream</tt> with a specific
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> which
     * is to have its <tt>connector</tt> set to the returned
     * <tt>StreamConnector</tt>
     * @return a new <tt>StreamConnector</tt> to be used as the
     * <tt>connector</tt> of the <tt>MediaStream</tt> with the specified
     * <tt>MediaType</tt>
     * @throws OperationFailedException if anything goes wrong while
     * initializing the new <tt>StreamConnector</tt>
     */
    @Override
    protected StreamConnector createStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        DatagramSocket[] streamConnectorSockets
            = getStreamConnectorSockets(mediaType);
        Socket[] tcpStreamConnectorSockets
            = getStreamConnectorTCPSockets(mediaType);

        /*
         * XXX If the iceAgent has not completed (yet), go with a default
         * StreamConnector (until it completes).
         */
        if(tcpStreamConnectorSockets == null)
        {
            return
                (streamConnectorSockets == null)
                    ? super.createStreamConnector(mediaType)
                    : new DefaultStreamConnector(
                            streamConnectorSockets[0 /* RTP */],
                            streamConnectorSockets[1 /* RTCP */]);
        }
        else
        {
            return
                (tcpStreamConnectorSockets == null)
                    ? super.createStreamConnector(mediaType)
                        : new DefaultTCPStreamConnector(
                            tcpStreamConnectorSockets[0 /* RTP */],
                            tcpStreamConnectorSockets[1 /* RTCP */]);
        }
    }

    /**
     * Gets the <tt>StreamConnector</tt> to be used as the <tt>connector</tt> of
     * the <tt>MediaStream</tt> with a specific <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> which
     * is to have its <tt>connector</tt> set to the returned
     * <tt>StreamConnector</tt>
     * @return the <tt>StreamConnector</tt> to be used as the <tt>connector</tt>
     * of the <tt>MediaStream</tt> with the specified <tt>MediaType</tt>
     * @throws OperationFailedException if anything goes wrong while
     * initializing the requested <tt>StreamConnector</tt>
     * @see net.java.sip.communicator.service.protocol.media.TransportManager#
     * getStreamConnector(MediaType)
     */
    @Override
    public StreamConnector getStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        StreamConnector streamConnector = super.getStreamConnector(mediaType);

        /*
         * Since the super caches the StreamConnectors, make sure that the
         * returned one is up-to-date with the iceAgent.
         */
        if (streamConnector != null && streamConnector.getProtocol() ==
            StreamConnector.Protocol.UDP)
        {
            DatagramSocket[] streamConnectorSockets
                = getStreamConnectorSockets(mediaType);

            /*
             * XXX If the iceAgent has not completed (yet), go with the default
             * StreamConnector (until it completes).
             */
            if ((streamConnectorSockets != null)
                    && ((streamConnector.getDataSocket()
                                    != streamConnectorSockets[0 /* RTP */])
                            || (streamConnector.getControlSocket()
                                   != streamConnectorSockets[1 /* RTCP */])))
            {
                // Recreate the StreamConnector for the specified mediaType.
                closeStreamConnector(mediaType);
                streamConnector = super.getStreamConnector(mediaType);
            }
        }
        else if (streamConnector != null && streamConnector.getProtocol() ==
            StreamConnector.Protocol.TCP)
        {
            Socket[] streamConnectorSockets
                = getStreamConnectorTCPSockets(mediaType);

            /*
             * XXX If the iceAgent has not completed (yet), go with the default
             * StreamConnector (until it completes).
             */
            if ((streamConnectorSockets != null)
                    && ((streamConnector.getDataTCPSocket()
                                    != streamConnectorSockets[0 /* RTP */])
                            || (streamConnector.getControlTCPSocket()
                                   != streamConnectorSockets[1 /* RTCP */])))
            {
                // Recreate the StreamConnector for the specified mediaType.
                closeStreamConnector(mediaType);
                streamConnector = super.getStreamConnector(mediaType);
            }
        }
        return streamConnector;
    }

    /**
     * Gets an array of <tt>Socket</tt>s which represents the sockets to
     * be used by the <tt>StreamConnector</tt> with the specified
     * <tt>MediaType</tt> in the order of {@link #COMPONENT_IDS} if
     * {@link #iceAgent} has completed.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>StreamConnector</tt>
     * for which the <tt>Socket</tt>s are to be returned
     * @return an array of <tt>Socket</tt>s which represents the sockets
     * to be used by the <tt>StreamConnector</tt> which the specified
     * <tt>MediaType</tt> in the order of {@link #COMPONENT_IDS} if
     * {@link #iceAgent} has completed; otherwise, <tt>null</tt>
     */
    private Socket[] getStreamConnectorTCPSockets(MediaType mediaType)
    {
        String mediaName = null;

        if(mediaType == MediaType.AUDIO)
        {
            mediaName = "rtp";
        }
        else if(mediaType == MediaType.VIDEO)
        {
            mediaName = "video_rtp";
        }
        else
        {
            logger.error("Not an audio/rtp mediatype");
            return null;
        }

        IceMediaStream stream = iceAgent.getStream(mediaName);

        if (stream != null)
        {
            Socket[] streamConnectorSockets = new Socket[COMPONENT_IDS.length];
            int streamConnectorSocketCount = 0;

            for (int i = 0; i < COMPONENT_IDS.length; i++)
            {
                Component component = stream.getComponent(COMPONENT_IDS[i]);

                if (component != null)
                {
                    CandidatePair selectedPair = component.getSelectedPair();

                    if (selectedPair != null)
                    {
                        Socket streamConnectorSocket
                            = selectedPair.getLocalCandidate().
                                getSocket();

                        if (streamConnectorSocket != null)
                        {
                            streamConnectorSockets[i] = streamConnectorSocket;
                            streamConnectorSocketCount++;
                        }
                    }
                }
            }
            if (streamConnectorSocketCount > 0)
            {
                return streamConnectorSockets;
            }
        }
        return null;
    }

    /**
     * Gets an array of <tt>DatagramSocket</tt>s which represents the sockets to
     * be used by the <tt>StreamConnector</tt> with the specified
     * <tt>MediaType</tt> in the order of {@link #COMPONENT_IDS} if
     * {@link #iceAgent} has completed.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>StreamConnector</tt>
     * for which the <tt>DatagramSocket</tt>s are to be returned
     * @return an array of <tt>DatagramSocket</tt>s which represents the sockets
     * to be used by the <tt>StreamConnector</tt> which the specified
     * <tt>MediaType</tt> in the order of {@link #COMPONENT_IDS} if
     * {@link #iceAgent} has completed; otherwise, <tt>null</tt>
     */
    private DatagramSocket[] getStreamConnectorSockets(MediaType mediaType)
    {
        String mediaName = null;

        if(mediaType == MediaType.AUDIO)
        {
            mediaName = "rtp";
        }
        else if(mediaType == MediaType.VIDEO)
        {
            mediaName = "video_rtp";
        }
        else
        {
            logger.error("Not an audio/rtp mediatype");
            return null;
        }

        IceMediaStream stream = iceAgent.getStream(mediaName);

        if (stream != null)
        {
            DatagramSocket[] streamConnectorSockets
                = new DatagramSocket[COMPONENT_IDS.length];
            int streamConnectorSocketCount = 0;

            for (int i = 0; i < COMPONENT_IDS.length; i++)
            {
                Component component = stream.getComponent(COMPONENT_IDS[i]);

                if (component != null)
                {
                    CandidatePair selectedPair = component.getSelectedPair();

                    if (selectedPair != null)
                    {
                        DatagramSocket streamConnectorSocket
                            = selectedPair.getLocalCandidate().
                                getDatagramSocket();

                        if (streamConnectorSocket != null)
                        {
                            streamConnectorSockets[i] = streamConnectorSocket;
                            streamConnectorSocketCount++;
                        }
                    }
                }
            }
            if (streamConnectorSocketCount > 0)
            {
                return streamConnectorSockets;
            }
        }
        return null;
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
    public MediaStreamTarget getStreamTarget(MediaType mediaType)
    {
        IceMediaStream stream = null;
        MediaStreamTarget streamTarget = null;
        String mediaName = null;

        if(mediaType == MediaType.AUDIO)
        {
            mediaName = "rtp";
        }
        else if(mediaType == MediaType.VIDEO)
        {
            mediaName = "video_rtp";
        }
        else
        {
            logger.error("Not an audio/rtp mediatype");
            return null;
        }

        stream = iceAgent.getStream(mediaName);

        if (stream != null)
        {
            InetSocketAddress[] streamTargetAddresses
                = new InetSocketAddress[COMPONENT_IDS.length];
            int streamTargetAddressCount = 0;

            for (int i = 0; i < COMPONENT_IDS.length; i++)
            {
                Component component = stream.getComponent(COMPONENT_IDS[i]);

                if (component != null)
                {
                    CandidatePair selectedPair = component.getSelectedPair();

                    if (selectedPair != null)
                    {
                        InetSocketAddress streamTargetAddress
                            = selectedPair
                                .getRemoteCandidate()
                                    .getTransportAddress();

                        if (streamTargetAddress != null)
                        {
                            streamTargetAddresses[i] = streamTargetAddress;
                            streamTargetAddressCount++;
                        }
                    }
                }
            }
            if (streamTargetAddressCount > 0)
            {
                streamTarget
                    = new MediaStreamTarget(
                            streamTargetAddresses[0 /* RTP */],
                            streamTargetAddresses[1 /* RTCP */]);
            }
        }
        return streamTarget;
    }

    /**
     * Creates an {@link IceMediaStream} with the specified <tt>media</tt>
     * name.
     *
     * @param media the name of the stream we'd like to create.
     *
     * @param rtcp if true allocate an RTCP port
     *
     * @return the newly created {@link IceMediaStream}
     *
     * @throws OperationFailedException if binding on the specified media stream
     * fails for some reason.
     */
    private IceMediaStream createIceStream(String media, boolean rtcp)
        throws OperationFailedException
    {
        IceMediaStream stream;

        try
        {
            //the following call involves STUN processing so it may take a while
            stream = iceAgent.createMediaStream(media);
            int rtpPort = getNextMediaPortToTry();

            //rtp
            iceAgent.createComponent(stream, Transport.UDP, rtpPort, rtpPort,
                rtpPort + 100);

            if(rtcp)
                iceAgent.createComponent(stream, Transport.UDP,
                                rtpPort + 1, rtpPort + 1, rtpPort + 101);
        }
        catch (Exception ex)
        {
            throw new OperationFailedException(
                    "Failed to initialize stream " + media,
                    OperationFailedException.INTERNAL_ERROR,
                    ex);
        }

        //let's now update the next port var as best we can: we would assume
        //that all local candidates are bound on the same port and set it
        //to the one just above. if the assumption is wrong the next bind
        //would simply include one more bind retry.
        try
        {
            setNextMediaPortToTry(
                    1
                        + stream
                            .getComponent(rtcp ? Component.RTCP : Component.RTP)
                                .getLocalCandidates()
                                    .get(0)
                                        .getTransportAddress()
                                            .getPort());
        }
        catch(Throwable t)
        {
            //hey, we were just trying to be nice. if that didn't work for
            //some reason we really can't be held responsible!
            logger.debug("Determining next port didn't work: ", t);
        }

        return stream;
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread.
     *
     * @param ourAnswer the content descriptions that we should be adding our
     * transport lists to (although not necessarily in this very instance).
     * @param candidatesSender the <tt>CandidatesSender</tt> to be used by
     * this <tt>TransportManagerGTalkImpl</tt> to send <tt>candidates</tt>
     * <tt>SessionIQ</tt>s from the local peer to the remote peer if this
     * <tt>TransportManagerGTalkImpl</tt> wishes to utilize
     * <tt>candidates</tt>.
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public void startCandidateHarvest(
            List<PayloadTypePacketExtension> ourAnswer,
            CandidatesSender candidatesSender)
        throws OperationFailedException
    {
        boolean audio = false;
        boolean video = false;
        List<GTalkCandidatePacketExtension> candidates
            = new LinkedList<GTalkCandidatePacketExtension>();

        synchronized(wrapupSyncRoot)
        {
            for(PayloadTypePacketExtension ext : ourAnswer)
            {
                if(ext.getNamespace().equals(
                        SessionIQProvider.GTALK_AUDIO_NAMESPACE))
                {
                    audio = true;
                }
                else if(ext.getNamespace().equals(
                        SessionIQProvider.GTALK_VIDEO_NAMESPACE))
                {
                    video = true;
                }
            }

            if(audio)
            {
                IceMediaStream stream = createIceStream("rtp", video);

                candidates.addAll(GTalkPacketFactory.createCandidates("rtp",
                        stream));
            }

            if(video)
            {
                IceMediaStream stream = createIceStream("video_rtp", true);
                candidates.addAll(
                    GTalkPacketFactory.createCandidates("video_rtp", stream));
            }

            /* send candidates */
            candidatesSender.sendCandidates(candidates);
        }
    }

    /**
     * Notifies the transport manager that it should conclude candidate
     * harvesting as soon as possible.
     */
    public void wrapupCandidateHarvest()
    {
    }

    /**
     * Starts the connectivity establishment of this
     * <tt>TransportManagerGTalkImpl</tt> i.e. checks the connectivity between
     * the local and the remote peers given the remote counterpart of the
     * negotiation between them.
     *
     * @param remote the collection of <tt>CandidatePacketExtension</tt>s which
     * represents the remote counterpart of the negotiation between the local
     * and the remote peer
     * @return <tt>true</tt> if connectivity establishment has been started in
     * response to the call; otherwise, <tt>false</tt>.
     */
    public boolean startConnectivityEstablishment(
            Iterable<GTalkCandidatePacketExtension> remote)
    {
        if (IceProcessingState.COMPLETED.equals(iceAgent.getState())/* ||
                IceProcessingState.FAILED.equals(iceAgent.getState())*/)
        {
            return true;
        }

        /* If ICE is already running, we try to update the checklists with
         * the candidates. Note that this is a best effort.
         */
        if (IceProcessingState.RUNNING.equals(iceAgent.getState()))
        {
            if(logger.isInfoEnabled())
            {
                logger.info("Update Google ICE remote candidates");
            }

            if(remote == null)
            {
                return false;
            }

            for(GTalkCandidatePacketExtension candidate : remote)
            {
                String name = candidate.getName();
                int numComponent = 0;

                logger.info("new cand " + name + " " + candidate.getProtocol() +
                    " " + candidate.getPort());
                // change name to retrieve properly the ICE media stream
                if(name.equals("rtp"))
                {
                    numComponent = Component.RTP;
                }
                else if(name.equals("rtcp"))
                {
                    name = "rtp";
                    numComponent = Component.RTCP;
                }
                else if(name.equals("video_rtp"))
                {
                    numComponent = Component.RTP;
                }
                else if(name.equals("video_rtcp"))
                {
                    name = "video_rtp";
                    numComponent = Component.RTCP;
                }

                IceMediaStream stream = iceAgent.getStream(name);

                if(stream == null)
                {
                    continue;
                }

                /* Different candidates may have different ufrag/password */
                String ufrag = candidate.getUsername();
                //String password = candidate.getPassword();

                /*
                 * Is the remote candidate from the current generation of
                 * the iceAgent?
                 */
                if (candidate.getGeneration() != iceAgent.getGeneration())
                    continue;

                if(candidate.getProtocol().equalsIgnoreCase("ssltcp"))
                    candidate.setProtocol("tcp");

                Component component
                    = stream.getComponent(numComponent);

                RemoteCandidate remoteCandidate = new RemoteCandidate(
                        new TransportAddress(
                                candidate.getAddress(),
                                candidate.getPort(),
                                Transport.parse(
                                        candidate.getProtocol())),
                        component,
                        org.ice4j.ice.CandidateType.parse(
                                candidate.getType().toString()),
                        "0",
                        (long)(candidate.getPreference() * 1000),
                        ufrag);
                component.addUpdateRemoteCandidate(remoteCandidate);
            }

            /* update all components of all streams */
            for(IceMediaStream stream : iceAgent.getStreams())
            {
                for(Component component : stream.getComponents())
                {
                    component.updateRemoteCandidate();
                }
            }
            return false;
        }

        int generation = iceAgent.getGeneration();
        boolean startConnectivityEstablishment = false;

        if(remote == null)
        {
            return false;
        }

        for(GTalkCandidatePacketExtension candidate : remote)
        {
            String name = candidate.getName();
            int numComponent = 0;

            // change name to retrieve properly the ICE media stream
            if(name.equals("rtp"))
            {
                numComponent = Component.RTP;
            }
            else if(name.equals("rtcp"))
            {
                name = "rtp";
                numComponent = Component.RTCP;
            }
            else if(name.equals("video_rtp"))
            {
                numComponent = Component.RTP;
            }
            else if(name.equals("video_rtcp"))
            {
                name = "video_rtp";
                numComponent = Component.RTCP;
            }

            IceMediaStream stream = null;

            synchronized(wrapupSyncRoot)
            {
                stream = iceAgent.getStream(name);
            }

            if(stream == null)
            {
                continue;
            }

            /* Different candidates may have different ufrag/password */
            String ufrag = candidate.getUsername();
            //String password = candidate.getPassword();

            /*
             * Is the remote candidate from the current generation of
             * the iceAgent?
             */
            if (candidate.getGeneration() != generation)
                continue;

            if(candidate.getProtocol().equalsIgnoreCase("ssltcp"))
                candidate.setProtocol("tcp");

            Component component
                = stream.getComponent(numComponent);

            RemoteCandidate remoteCandidate = new RemoteCandidate(
                    new TransportAddress(
                            candidate.getAddress(),
                            candidate.getPort(),
                            Transport.parse(
                                    candidate.getProtocol())),
                    component,
                    org.ice4j.ice.CandidateType.parse(
                            candidate.getType().toString()),
                    "0",
                    (long)(candidate.getPreference() * 1000),
                    ufrag);
            component.addRemoteCandidate(remoteCandidate);
            startConnectivityEstablishment = true;
        }

        if (startConnectivityEstablishment)
        {
            /*
             * Once again because the ICE Agent does not support adding
             * candidates after the connectivity establishment has been started
             * and because multiple transport-info JingleIQs may be used to send
             * the whole set of transport candidates from the remote peer to the
             * local peer, do not really start the connectivity establishment
             * until we have at least two remote candidates (i.e. local and
             * stun) per ICE Component.
             */
            for (IceMediaStream stream : iceAgent.getStreams())
            {
                for (Component component : stream.getComponents())
                {
                    if (component.getRemoteCandidateCount() < 1)
                    {
                        startConnectivityEstablishment = false;
                        break;
                    }
                }
                if (!startConnectivityEstablishment)
                    break;
            }

            if (startConnectivityEstablishment)
            {
                iceAgent.startConnectivityEstablishment();
                return true;
            }
        }
        return false;
    }

    /**
     * Waits for the associated ICE <tt>Agent</tt> to finish any started
     * connectivity checks.
     *
     * @throws OperationFailedException if ICE processing has failed
     */
    public void wrapupConnectivityEstablishment()
        throws OperationFailedException
    {
        final Object iceProcessingStateSyncRoot = new Object();
        PropertyChangeListener stateChangeListener
            = new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    Object newValue = evt.getNewValue();

                    if (IceProcessingState.COMPLETED.equals(newValue)
                            || IceProcessingState.FAILED.equals(newValue)
                            || IceProcessingState.TERMINATED.equals(newValue))
                    {
                        if (logger.isTraceEnabled())
                            logger.trace("ICE " + newValue);

                        Agent iceAgent = (Agent) evt.getSource();

                        iceAgent.removeStateChangeListener(this);

                        if (iceAgent == TransportManagerGTalkImpl.this.iceAgent)
                        {
                            synchronized (iceProcessingStateSyncRoot)
                            {
                                iceProcessingStateSyncRoot.notify();
                            }
                        }
                    }
                }
            };

        iceAgent.addStateChangeListener(stateChangeListener);

        // Wait for the connectivity checks to finish if they have been started.
        boolean interrupted = false;

        synchronized (iceProcessingStateSyncRoot)
        {
            while (IceProcessingState.RUNNING.equals(iceAgent.getState()))
            {
                try
                {
                    iceProcessingStateSyncRoot.wait();
                }
                catch (InterruptedException ie)
                {
                    interrupted = true;
                }
            }
        }

        if (interrupted)
            Thread.currentThread().interrupt();

        /*
         * Make sure stateChangeListener is removed from iceAgent in case its
         * #propertyChange(PropertyChangeEvent) has never been executed.
         */
        iceAgent.removeStateChangeListener(stateChangeListener);

        /* check the state of ICE processing and throw exception if failed */
        if(iceAgent.getState().equals(IceProcessingState.FAILED))
        {
            throw new OperationFailedException(
                    "Could not establish connection (ICE failed)",
                    OperationFailedException.GENERAL_ERROR);
        }
        if(iceAgent.getState().equals(IceProcessingState.WAITING))
        {
            throw new IllegalArgumentException("ICE not started");
        }
    }

    /**
     * Send empty UDP packet to target destination data/control ports
     * in order to open port on NAT or RTP proxy if any.
     *
     * @param target <tt>MediaStreamTarget</tt>
     * @param type the {@link MediaType} of the connector we'd like to send
     * the hole punching packet through.
     */
    @Override
    public void sendHolePunchPacket(MediaStreamTarget target, MediaType type)
    {
        /* Override TransportManager.sendHolePunchPacket that send zero length
         * UDP packet and do nothing instead. For some applications (especially
         * those installed on smartphones like Android ones), the zero length
         * UDP packet can confuse them.
         */
        return;
    }

    /**
     * Close this transport manager and release resources.
     */
    public void close()
    {
        if(iceAgent != null)
        {
            logger.info("Close transport manager agent");
            iceAgent.removeStateChangeListener(this);
            iceAgent.free();
        }
    }

    /**
     * Returns the extended type of the candidate selected if this transport
     * manager is using ICE.
     *
     * @return The extended type of the candidate selected if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public String getICECandidateExtendedType()
    {
        return TransportManager.getICECandidateExtendedType(
                this.iceAgent);
    }

    /**
     * Retransmit state change events from the Agent.
     * @param evt the event for state change.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        getCallPeer().getMediaHandler().firePropertyChange(
            evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
    }

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing.
     */
    public String getICEState()
    {
        return iceAgent.getState().name();
    }
}
