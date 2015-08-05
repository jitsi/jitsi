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

import java.beans.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.CandidateType;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.ice4j.security.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;
import org.jivesoftware.smack.packet.*;
import org.xmpp.jnodes.smack.*;

/**
 * A {@link TransportManagerJabberImpl} implementation that would use ICE for
 * candidate management.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class IceUdpTransportManager
    extends TransportManagerJabberImpl
    implements PropertyChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>IceUdpTransportManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(IceUdpTransportManager.class);

    /**
     * The ICE <tt>Component</tt> IDs in their common order used, for example,
     * by <tt>DefaultStreamConnector</tt>, <tt>MediaStreamTarget</tt>.
     */
    private static final int[] COMPONENT_IDS
        = new int[] { Component.RTP, Component.RTCP };

    /**
     * This is where we keep our answer between the time we get the offer and
     * are ready with the answer.
     */
    protected List<ContentPacketExtension> cpeList;

    /**
     * The ICE agent that this transport manager would be using for ICE
     * negotiation.
     */
    protected final Agent iceAgent;

    /**
     * Default STUN server address.
     */
    protected static final String DEFAULT_STUN_SERVER_ADDRESS = "stun.jitsi.net";

    /**
     * Default STUN server port.
     */
    protected static final int DEFAULT_STUN_SERVER_PORT = 3478;

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeer} whose traffic we will be taking
     * care of.
     */
    public IceUdpTransportManager(CallPeerJabberImpl callPeer)
    {
        super(callPeer);
        iceAgent = createIceAgent();
        iceAgent.addStateChangeListener(this);
    }

    /**
     * Creates the ICE agent that we would be using in this transport manager
     * for all negotiation.
     *
     * @return the ICE agent to use for all the ICE negotiation that this
     * transport manager would be going through
     */
    protected Agent createIceAgent()
    {
        long startGatheringHarvesterTime = System.currentTimeMillis();
        CallPeerJabberImpl peer = getCallPeer();
        ProtocolProviderServiceJabberImpl provider = peer.getProtocolProvider();
        NetworkAddressManagerService namSer = getNetAddrMgr();
        boolean atLeastOneStunServer = false;
        Agent agent = namSer.createIceAgent();

        /*
         * XEP-0176:  the initiator MUST include the ICE-CONTROLLING attribute,
         * the responder MUST include the ICE-CONTROLLED attribute.
         */
        agent.setControlling(!peer.isInitiator());

        //we will now create the harvesters
        JabberAccountIDImpl accID
            = (JabberAccountIDImpl) provider.getAccountID();

        if (accID.isStunServerDiscoveryEnabled())
        {
            //the default server is supposed to use the same user name and
            //password as the account itself.
            String username
                = org.jivesoftware.smack.util.StringUtils.parseName(
                        provider.getOurJID());
            String password
                = JabberActivator.getProtocolProviderFactory().loadPassword(
                        accID);
            UserCredentials credentials = provider.getUserCredentials();

            if(credentials != null)
                password = credentials.getPasswordAsString();

            // ask for password if not saved
            if (password == null)
            {
                //create a default credentials object
                credentials = new UserCredentials();
                credentials.setUserName(accID.getUserID());
                //request a password from the user
                credentials
                    = provider.getAuthority().obtainCredentials(
                            accID.getDisplayName(),
                            credentials,
                            SecurityAuthority.AUTHENTICATION_REQUIRED);

                // in case user has canceled the login window
                if(credentials == null)
                    return null;

                //extract the password the user passed us.
                char[] pass = credentials.getPassword();

                // the user didn't provide us a password (i.e. canceled the
                // operation)
                if(pass == null)
                    return null;
                password = new String(pass);

                if (credentials.isPasswordPersistent())
                {
                    JabberActivator.getProtocolProviderFactory()
                        .storePassword(accID, password);
                }
            }

            StunCandidateHarvester autoHarvester
                = namSer.discoverStunServer(
                        accID.getService(),
                        StringUtils.getUTF8Bytes(username),
                        StringUtils.getUTF8Bytes(password));

            if (logger.isInfoEnabled())
                logger.info("Auto discovered harvester is " + autoHarvester);

            if (autoHarvester != null)
            {
                atLeastOneStunServer = true;
                agent.addCandidateHarvester(autoHarvester);
            }
        }

        //now create stun server descriptors for whatever other STUN/TURN
        //servers the user may have set.
        for(StunServerDescriptor desc : accID.getStunServers())
        {
            TransportAddress addr
                = new TransportAddress(
                        desc.getAddress(),
                        desc.getPort(),
                        Transport.UDP);

            // if we get STUN server from automatic discovery, it may just
            // be server name (i.e. stun.domain.org) and it may be possible that
            // it cannot be resolved
            if(addr.getAddress() == null)
            {
                logger.info("Unresolved address for " + addr);
                continue;
            }

            StunCandidateHarvester harvester;

            if(desc.isTurnSupported())
            {
                //Yay! a TURN server
                harvester
                    = new TurnCandidateHarvester(
                            addr,
                            new LongTermCredential(
                                    desc.getUsername(),
                                    desc.getPassword()));
            }
            else
            {
                //this is a STUN only server
                harvester = new StunCandidateHarvester(addr);
            }

            if (logger.isInfoEnabled())
                logger.info("Adding pre-configured harvester " + harvester);

            atLeastOneStunServer = true;
            agent.addCandidateHarvester(harvester);
        }

        if(!atLeastOneStunServer && accID.isUseDefaultStunServer())
        {
            /* we have no configured or discovered STUN server so takes the
             * default provided by us if user allows it
             */
            TransportAddress addr
                = new TransportAddress(
                        DEFAULT_STUN_SERVER_ADDRESS,
                        DEFAULT_STUN_SERVER_PORT,
                        Transport.UDP);

            agent.addCandidateHarvester(new StunCandidateHarvester(addr));
        }

        /* Jingle nodes candidate */
        if(accID.isJingleNodesRelayEnabled())
        {
            /* this method is blocking until Jingle Nodes auto-discovery (if
             * enabled) finished
             */
            SmackServiceNode serviceNode = provider.getJingleNodesServiceNode();

            if(serviceNode != null)
            {
                agent.addCandidateHarvester(
                        new JingleNodesHarvester(serviceNode));
            }
        }

        if(accID.isUPNPEnabled())
            agent.addCandidateHarvester(new UPNPHarvester());

        long stopGatheringHarvesterTime = System.currentTimeMillis();

        if (logger.isInfoEnabled())
        {
            long gatheringHarvesterTime
                = stopGatheringHarvesterTime - startGatheringHarvesterTime;

            logger.info(
                    "End gathering harvester within " + gatheringHarvesterTime
                        + " ms");
        }
        return agent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StreamConnector doCreateStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        /*
         * If this instance is participating in a telephony conference utilizing
         * the Jitsi Videobridge server-side technology that is organized by the
         * local peer, then there is a single MediaStream (of the specified
         * mediaType) shared among multiple TransportManagers and its
         * StreamConnector may be determined only by the TransportManager which
         * is establishing the connectivity with the Jitsi Videobridge server
         * (as opposed to a CallPeer).
         */
        TransportManagerJabberImpl delegate
            = findTransportManagerEstablishingConnectivityWithJitsiVideobridge();

        if ((delegate != null) && (delegate != this))
            return delegate.doCreateStreamConnector(mediaType);

        DatagramSocket[] streamConnectorSockets
            = getStreamConnectorSockets(mediaType);

        /*
         * XXX If the iceAgent has not completed (yet), go with a default
         * StreamConnector (until it completes).
         */
        return
            (streamConnectorSockets == null)
                ? super.doCreateStreamConnector(mediaType)
                : new DefaultStreamConnector(
                        streamConnectorSockets[0 /* RTP */],
                        streamConnectorSockets[1 /* RTCP */]);
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
        if (streamConnector != null)
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
        return streamConnector;
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
        IceMediaStream stream = iceAgent.getStream(mediaType.toString());

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
                return streamConnectorSockets;
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
    @Override
    public MediaStreamTarget getStreamTarget(MediaType mediaType)
    {
        /*
         * If this instance is participating in a telephony conference utilizing
         * the Jitsi Videobridge server-side technology that is organized by the
         * local peer, then there is a single MediaStream (of the specified
         * mediaType) shared among multiple TransportManagers and its
         * MediaStreamTarget may be determined only by the TransportManager
         * which is establishing the connectivity with the Jitsi Videobridge
         * server (as opposed to a CallPeer).
         */
        TransportManagerJabberImpl delegate
            = findTransportManagerEstablishingConnectivityWithJitsiVideobridge();

        if ((delegate != null) && (delegate != this))
            return delegate.getStreamTarget(mediaType);

        IceMediaStream stream = iceAgent.getStream(mediaType.toString());
        MediaStreamTarget streamTarget = null;

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
        return ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_ICE_UDP_1;
    }

    /**
     * {@inheritDoc}
     */
    protected PacketExtension createTransportPacketExtension()
    {
        return new IceUdpTransportPacketExtension();
    }

    /**
     * {@inheritDoc}
     */
    protected PacketExtension startCandidateHarvest(
            ContentPacketExtension theirContent,
            ContentPacketExtension ourContent,
            TransportInfoSender transportInfoSender,
            String media)
        throws OperationFailedException
    {
        PacketExtension pe;

        // Report the gathered candidate addresses.
        if (transportInfoSender == null)
        {
            pe = createTransportForStartCandidateHarvest(media);
        }
        else
        {
            /*
             * The candidates will be sent in transport-info so the transport of
             * session-accept just has to be present, not populated with
             * candidates.
             */
            pe = createTransportPacketExtension();

            /*
             * Create the content to be sent in a transport-info. The transport
             * is the only extension to be sent in transport-info so the content
             * has the same attributes as in our answer and none of its
             * non-transport extensions.
             */
            ContentPacketExtension transportInfoContent
                = new ContentPacketExtension();

            for (String name : ourContent.getAttributeNames())
            {
                Object value = ourContent.getAttribute(name);

                if (value != null)
                    transportInfoContent.setAttribute(name, value);
            }
            transportInfoContent.addChildExtension(
                    createTransportForStartCandidateHarvest(media));

            /*
             * We send each media content in separate transport-info. It is
             * absolutely not mandatory (we can simply send all content in one
             * transport-info) but the XMPP Jingle client Empathy (via
             * telepathy-gabble), which is present on many Linux distributions
             * and N900 mobile phone, has a bug when it receives more than one
             * content in transport-info. The related bug has been fixed in
             * mainstream but the Linux distributions have not updated their
             * packages yet. That's why we made this modification to be fully
             * interoperable with Empathy right now. In the future, we will get
             * back to the original behavior: sending all content in one
             * transport-info.
             */
            Collection<ContentPacketExtension> transportInfoContents
                = new LinkedList<ContentPacketExtension>();

            transportInfoContents.add(transportInfoContent);

            transportInfoSender.sendTransportInfo(transportInfoContents);
        }

        return pe;
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
        this.cpeList = ourAnswer;

        super.startCandidateHarvest(theirOffer, ourAnswer, transportInfoSender);
    }

    /**
     * Converts the ICE media <tt>stream</tt> and its local candidates into a
     * {@link IceUdpTransportPacketExtension}.
     *
     * @param stream the {@link IceMediaStream} that we'd like to describe in
     * XML.
     *
     * @return the {@link IceUdpTransportPacketExtension} that we
     */
    protected PacketExtension createTransport(IceMediaStream stream)
    {
        IceUdpTransportPacketExtension transport
            = new IceUdpTransportPacketExtension();
        Agent iceAgent = stream.getParentAgent();

        transport.setUfrag(iceAgent.getLocalUfrag());
        transport.setPassword(iceAgent.getLocalPassword());

        for(Component component : stream.getComponents())
        {
            for(Candidate<?> candidate : component.getLocalCandidates())
                transport.addCandidate(createCandidate(candidate));
        }

        return transport;
    }

    /**
     * {@inheritDoc}
     */
    protected PacketExtension createTransport(String media)
        throws OperationFailedException
    {
        IceMediaStream iceStream = iceAgent.getStream(media);

        if (iceStream == null)
            iceStream = createIceStream(media);

        return createTransport(iceStream);
    }

    /**
     * Creates a {@link CandidatePacketExtension} and initializes it so that it
     * would describe the state of <tt>candidate</tt>
     *
     * @param candidate the ICE4J {@link Candidate} that we'd like to convert
     * into an XMPP packet extension.
     *
     * @return a new {@link CandidatePacketExtension} corresponding to the state
     * of the <tt>candidate</tt> candidate.
     */
    private CandidatePacketExtension createCandidate(Candidate<?> candidate)
    {
        CandidatePacketExtension packet = new CandidatePacketExtension();

        packet.setFoundation(candidate.getFoundation());

        Component component = candidate.getParentComponent();

        packet.setComponent(component.getComponentID());
        packet.setProtocol(candidate.getTransport().toString());
        packet.setPriority(candidate.getPriority());
        packet.setGeneration(
                component.getParentStream().getParentAgent().getGeneration());

        TransportAddress transportAddress = candidate.getTransportAddress();
        packet.setID(getNextID());
        packet.setIP(transportAddress.getHostAddress());
        packet.setPort(transportAddress.getPort());

        packet.setType(CandidateType.valueOf(candidate.getType().toString()));

        TransportAddress relAddr = candidate.getRelatedAddress();

        if(relAddr != null)
        {
            packet.setRelAddr(relAddr.getHostAddress());
            packet.setRelPort(relAddr.getPort());
        }

        /*
         * FIXME The XML schema of XEP-0176: Jingle ICE-UDP Transport Method
         * specifies the network attribute as required.
         */
        packet.setNetwork(0);

        return packet;
    }

    /**
     * Creates an {@link IceMediaStream} with the specified <tt>media</tt>
     * name.
     *
     * @param media the name of the stream we'd like to create.
     *
     * @return the newly created {@link IceMediaStream}
     *
     * @throws OperationFailedException if binding on the specified media stream
     * fails for some reason.
     */
    protected IceMediaStream createIceStream(String media)
        throws OperationFailedException
    {
        IceMediaStream stream;
        PortTracker portTracker;

        try
        {
            portTracker = getPortTracker(media);
            //the following call involves STUN processing so it may take a while
            stream
                = getNetAddrMgr().createIceStream(
                        portTracker.getPort(),
                        media,
                        iceAgent);
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
            portTracker.setNextPort(
                    1
                        + stream
                            .getComponent(Component.RTCP)
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
     * Simply returns the list of local candidates that we gathered during the
     * harvest.
     *
     * @return the list of local candidates that we gathered during the harvest
     * @see TransportManagerJabberImpl#wrapupCandidateHarvest()
     */
    @Override
    public List<ContentPacketExtension> wrapupCandidateHarvest()
    {
        return cpeList;
    }

    /**
     * Returns a reference to the {@link NetworkAddressManagerService}. The only
     * reason this method exists is that {@link JabberActivator
     * #getNetworkAddressManagerService()} is too long to write and makes code
     * look clumsy.
     *
     * @return  a reference to the {@link NetworkAddressManagerService}.
     */
    private static NetworkAddressManagerService getNetAddrMgr()
    {
        return JabberActivator.getNetworkAddressManagerService();
    }

    /**
     * Starts the connectivity establishment of the associated ICE
     * <tt>Agent</tt>.
     *
     * @param remote the collection of <tt>ContentPacketExtension</tt>s which
     * represents the remote counterpart of the negotiation between the local
     * and the remote peers
     * @return <tt>true</tt> if connectivity establishment has been started in
     * response to the call; otherwise, <tt>false</tt>
     * @see TransportManagerJabberImpl#startConnectivityEstablishment(Iterable)
     */
    @Override
    public synchronized boolean startConnectivityEstablishment(
            Iterable<ContentPacketExtension> remote)
    {
        Map<String,IceUdpTransportPacketExtension> map
            = new LinkedHashMap<String,IceUdpTransportPacketExtension>();

        for (ContentPacketExtension content : remote)
        {
            IceUdpTransportPacketExtension transport
                = content.getFirstChildOfType(
                        IceUdpTransportPacketExtension.class);
            /*
             * If we cannot associate an IceMediaStream with the remote content,
             * we will not have anything to add the remote candidates to.
             */
            RtpDescriptionPacketExtension description
                = content.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);

            if ((description == null) && (cpeList != null))
            {
                ContentPacketExtension localContent
                    = findContentByName(cpeList, content.getName());

                if (localContent != null)
                {
                    description
                        = localContent.getFirstChildOfType(
                                RtpDescriptionPacketExtension.class);
                }
            }
            if (description != null)
            {
                String media = description.getMedia();

                map.put(media, transport);
            }
        }

        /*
         * When the local peer is organizing a telephony conference using the
         * Jitsi Videobridge server-side technology, it is establishing
         * connectivity by using information from a colibri Channel and not from
         * the offer/answer of the remote peer.
         */
        if (getCallPeer().isJitsiVideobridge())
        {
            sendTransportInfoToJitsiVideobridge(map);
            return false;
        }
        else
        {
            return startConnectivityEstablishment(map);
        }
    }

    /**
     * Starts the connectivity establishment of the associated ICE
     * <tt>Agent</tt>.
     *
     * @param remote a <tt>Map</tt> of
     * media-<tt>IceUdpTransportPacketExtension</tt> pairs which represents the
     * remote counterpart of the negotiation between the local and the remote
     * peers
     * @return <tt>true</tt> if connectivity establishment has been started in
     * response to the call; otherwise, <tt>false</tt>
     * @see TransportManagerJabberImpl#startConnectivityEstablishment(Map)
     */
    @Override
    protected synchronized boolean startConnectivityEstablishment(
            Map<String,IceUdpTransportPacketExtension> remote)
    {
        /*
         * If ICE is running already, we try to update the checklists with the
         * candidates. Note that this is a best effort.
         */
        boolean iceAgentStateIsRunning
            = IceProcessingState.RUNNING.equals(iceAgent.getState());

        if (iceAgentStateIsRunning && logger.isInfoEnabled())
            logger.info("Update ICE remote candidates");

        int generation = iceAgent.getGeneration();
        boolean startConnectivityEstablishment = false;

        for (Map.Entry<String, IceUdpTransportPacketExtension> e
                : remote.entrySet())
        {
            IceUdpTransportPacketExtension transport = e.getValue();
            List<CandidatePacketExtension> candidates
                = transport.getChildExtensionsOfType(
                        CandidatePacketExtension.class);

            if (iceAgentStateIsRunning && (candidates.size() == 0))
                return false;

            String media = e.getKey();
            IceMediaStream stream = iceAgent.getStream(media);

            if (stream == null)
            {
                logger.warn(
                    "No ICE media stream for media: " + media
                        + " - ignored candidates.");
                continue;
            }

            // Sort the remote candidates (host < reflexive < relayed) in order
            // to create first the host, then the reflexive, the relayed
            // candidates and thus be able to set the relative-candidate
            // matching the rel-addr/rel-port attribute.
            Collections.sort(candidates);

            // Different stream may have different ufrag/password
            String ufrag = transport.getUfrag();

            if (ufrag != null)
                stream.setRemoteUfrag(ufrag);

            String password = transport.getPassword();

            if (password != null)
                stream.setRemotePassword(password);

            for (CandidatePacketExtension candidate : candidates)
            {
                /*
                 * Is the remote candidate from the current generation of the
                 * iceAgent?
                 */
                if (candidate.getGeneration() != generation)
                    continue;

                Component component
                    = stream.getComponent(candidate.getComponent());
                String relAddr;
                int relPort;
                TransportAddress relatedAddress = null;

                if (((relAddr = candidate.getRelAddr()) != null)
                        && ((relPort = candidate.getRelPort()) != -1))
                {
                    relatedAddress
                        = new TransportAddress(
                                relAddr,
                                relPort,
                                Transport.parse(candidate.getProtocol()));
                }

                RemoteCandidate relatedCandidate
                    = component.findRemoteCandidate(relatedAddress);
                RemoteCandidate remoteCandidate
                    = new RemoteCandidate(
                            new TransportAddress(
                                    candidate.getIP(),
                                    candidate.getPort(),
                                    Transport.parse(
                                            candidate.getProtocol())),
                            component,
                            org.ice4j.ice.CandidateType.parse(
                                    candidate.getType().toString()),
                            candidate.getFoundation(),
                            candidate.getPriority(),
                            relatedCandidate);

                if (iceAgentStateIsRunning)
                {
                    component.addUpdateRemoteCandidates(remoteCandidate);
                }
                else
                {
                    component.addRemoteCandidate(remoteCandidate);
                    startConnectivityEstablishment = true;
                }
            }
        }

        if (iceAgentStateIsRunning)
        {
            // update all components of all streams
            for (IceMediaStream stream : iceAgent.getStreams())
            {
                for (Component component : stream.getComponents())
                    component.updateRemoteCandidates();
            }
        }
        else if (startConnectivityEstablishment)
        {
            /*
             * Once again because the ICE Agent does not support adding
             * candidates after the connectivity establishment has been started
             * and because multiple transport-info JingleIQs may be used to send
             * the whole set of transport candidates from the remote peer to the
             * local peer, do not really start the connectivity establishment
             * until we have at least one remote candidate per ICE Component.
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
     * @see TransportManagerJabberImpl#wrapupConnectivityEstablishment()
     * @throws OperationFailedException if ICE processing has failed
     */
    @Override
    public void wrapupConnectivityEstablishment()
        throws OperationFailedException
    {
        TransportManagerJabberImpl delegate
            = findTransportManagerEstablishingConnectivityWithJitsiVideobridge();

        if ((delegate == null) || (delegate == this))
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

                        if (iceAgent == IceUdpTransportManager.this.iceAgent)
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
        if(IceProcessingState.FAILED.equals(iceAgent.getState()))
        {
            String msg = JabberActivator.getResources()
                .getI18NString("service.protocol.ICE_FAILED");
            throw new OperationFailedException(
                    msg,
                    OperationFailedException.GENERAL_ERROR);
        }
        }
        else
        {
            delegate.wrapupConnectivityEstablishment();
        }

        /*
         * Once we're done establishing connectivity, we shouldn't be sending
         * any more candidates because we will not be able to perform
         * connectivity checks for them. Besides, they must have been sent in
         * transport-info already.
         */
        if (cpeList != null)
        {
            for (ContentPacketExtension content : cpeList)
            {
                IceUdpTransportPacketExtension transport
                    = content.getFirstChildOfType(
                            IceUdpTransportPacketExtension.class);

                if (transport != null)
                {
                    for (CandidatePacketExtension candidate
                            : transport.getCandidateList())
                        transport.removeCandidate(candidate);

                    Collection<?> childExtensions
                        = transport.getChildExtensionsOfType(
                                CandidatePacketExtension.class);

                    if ((childExtensions == null) || childExtensions.isEmpty())
                    {
                        transport.removeAttribute(
                                IceUdpTransportPacketExtension.UFRAG_ATTR_NAME);
                        transport.removeAttribute(
                                IceUdpTransportPacketExtension.PWD_ATTR_NAME);
                    }
                }
            }
        }
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
        ContentPacketExtension content = removeContent(cpeList, name);

        if (content != null)
        {
            RtpDescriptionPacketExtension rtpDescription
                = content.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);

            if (rtpDescription != null)
            {
                IceMediaStream stream
                    = iceAgent.getStream(rtpDescription.getMedia());

                if (stream != null)
                    iceAgent.removeStream(stream);
            }
        }
    }

    /**
     * Close this transport manager and release resources. In case of ICE, it
     * releases Ice4j's Agent that will cleanup all streams, component and close
     * every candidate's sockets.
     */
    @Override
    public synchronized void close()
    {
        if(iceAgent != null)
        {
            iceAgent.removeStateChangeListener(this);
            iceAgent.free();
        }
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
        return
            TransportManager.getICECandidateExtendedType(iceAgent, streamName);
    }

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing.
     */
    @Override
    public String getICEState()
    {
        return iceAgent.getState().toString();
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
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getHostAddress();
        }
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
        if(iceAgent != null)
        {
            RemoteCandidate remoteCandidate
                = iceAgent.getSelectedRemoteCandidate(streamName);

            if(remoteCandidate != null)
                return remoteCandidate.getHostAddress();
        }
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
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getReflexiveAddress();
        }
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
        if(iceAgent != null)
        {
            RemoteCandidate remoteCandidate
                = iceAgent.getSelectedRemoteCandidate(streamName);

            if(remoteCandidate != null)
                return remoteCandidate.getReflexiveAddress();
        }
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
        if(iceAgent != null)
        {
            LocalCandidate localCandidate
                = iceAgent.getSelectedLocalCandidate(streamName);

            if(localCandidate != null)
                return localCandidate.getRelayedAddress();
        }
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
        if(iceAgent != null)
        {
            RemoteCandidate remoteCandidate
                = iceAgent.getSelectedRemoteCandidate(streamName);

            if(remoteCandidate != null)
                return remoteCandidate.getRelayedAddress();
        }
        return null;
    }

    /**
     * Returns the total harvesting time (in ms) for all harvesters.
     *
     * @return The total harvesting time (in ms) for all the harvesters. 0 if
     * the ICE agent is null, or if the agent has nevers harvested.
     */
    @Override
    public long getTotalHarvestingTime()
    {
        return (iceAgent == null) ? 0 : iceAgent.getTotalHarvestingTime();
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
        return
            (iceAgent == null) ? 0 : iceAgent.getHarvestingTime(harvesterName);
    }

    /**
     * Returns the number of harvesting for this agent.
     *
     * @return The number of harvesting for this agent.
     */
    @Override
    public int getNbHarvesting()
    {
        return (iceAgent == null) ? 0 : iceAgent.getHarvestCount();
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
    public int getNbHarvesting(String harvesterName)
    {
        return (iceAgent == null) ? 0 : iceAgent.getHarvestCount(harvesterName);
    }

    /**
     * Retransmit state change events from the Agent to the media handler.
     * @param evt the event for state change.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        getCallPeer().getMediaHandler().firePropertyChange(
            evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
    }
}
