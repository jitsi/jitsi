/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Google P2P TransportManager.
 *
 * @author Sebastien Vincent
 */
public class P2PTransportManager
    extends IceUdpTransportManager
{
    /**
     * The <tt>Logger</tt> used by the <tt>P2PTransportManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(P2PTransportManager.class);

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
    public P2PTransportManager(CallPeerJabberImpl callPeer)
    {
        super(callPeer);
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
        CallPeerJabberImpl peer = getCallPeer();
        ProtocolProviderServiceJabberImpl provider = peer.getProtocolProvider();

        Agent iceAgent = TransportManagerGTalkImpl.createAgent(provider,
            !peer.isInitiator());

        /* We use a custom strategy that will wait a little bit before choosing
         * to go through a relay. In fact Empathy will begin to send first the
         * relay candidates and then we can end up using a relay candidate
         * instead of a host/server reflexive ones (because the connectivity
         * checks will start earlier for relay.
         */
        iceAgent.setNominationStrategy(
            NominationStrategy.NOMINATE_FIRST_HOST_OR_REFLEXIVE_VALID);
        return iceAgent;
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
    public String getXmlNamespace()
    {
        return "http://www.google.com/transport/p2p";
    }

    /**
     * Get the transport <tt>PacketExtension</tt> to add.
     *
     * @return <tt>PacketExtension</tt>
     */
    protected PacketExtension getTransportPacketExtension()
    {
        return new GTalkTransportPacketExtension();
    }

    /**
     * Converts the ICE media <tt>stream</tt> and its local candidates into a
     * {@link GTalkTransportPacketExtension}.
     *
     * @param stream the {@link IceMediaStream} that we'd like to describe in
     * XML.
     *
     * @return the {@link GTalkTransportPacketExtension}
     */
    public PacketExtension createTransport(IceMediaStream stream)
    {
        GTalkTransportPacketExtension trans
            = new GTalkTransportPacketExtension();

        if(stream != null)
            for(Component component : stream.getComponents())
            {
                List<LocalCandidate> candToRemove =
                    new ArrayList<LocalCandidate>();
                List<LocalCandidate> candidates =
                    component.getLocalCandidates();

                for(Candidate candidate : component.getLocalCandidates())
                {
                    if(candidate instanceof UPNPCandidate)
                    {
                        LocalCandidate base = candidate.getBase();
                        candToRemove.add(base);
                    }
                }

                for(Candidate candidate : candToRemove)
                {
                    candidates.remove(candidate);
                }

                for(Candidate candidate : candidates)
                    trans.addCandidate(createCandidate(candidate));
            }

        return trans;
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
    private GTalkCandidatePacketExtension createCandidate(Candidate candidate)
    {
        String name =
            candidate.getParentComponent().getParentStream().getName();

        if(candidate.getParentComponent().getComponentID() == Component.RTP)
        {
            name = "rtp";
        }
        else
        {
            name = "rtcp";
        }

        return GTalkPacketFactory.createCandidate(candidate, name);
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
    public void startCandidateHarvest(
                            List<ContentPacketExtension>   ourOffer,
                            final TransportInfoSender transportInfoSender)
        throws OperationFailedException
    {
        final List<ContentPacketExtension> offer = ourOffer;
        this.cpeList = ourOffer;

        if(transportInfoSender == null)
        {
            synchronized(wrapupSyncRoot)
            {
                for(ContentPacketExtension ourContent : ourOffer)
                {
                    RtpDescriptionPacketExtension rtpDesc
                        = ourContent.getFirstChildOfType(
                                RtpDescriptionPacketExtension.class);

                    IceMediaStream stream = null;

                    stream = createIceStream(rtpDesc.getMedia());

                    //we now generate the XMPP code containing the candidates.
                    ourContent.addChildExtension(createTransport(stream));
                }
            }
            return;
        }

        for(ContentPacketExtension ourContent : offer)
        {
            ourContent.addChildExtension(
                    getTransportPacketExtension());
        }

        new Thread()
        {
            public void run()
            {
                Collection<ContentPacketExtension> transportInfoContents
                    = (transportInfoSender == null)
                        ? null
                        : new LinkedList<ContentPacketExtension>();

                synchronized(wrapupSyncRoot)
                {
                    for(ContentPacketExtension ourContent : offer)
                    {
                        RtpDescriptionPacketExtension rtpDesc
                            = ourContent.getFirstChildOfType(
                                    RtpDescriptionPacketExtension.class);

                        ourContent.addChildExtension(getTransportPacketExtension());

                        IceMediaStream stream = null;
                        try
                        {
                            stream = createIceStream(rtpDesc.getMedia());
                        }
                        catch (OperationFailedException e)
                        {
                            logger.info("Failed to create ICE stream", e);
                        }

                        ContentPacketExtension transportInfoContent
                            = new ContentPacketExtension();

                        for (String name : ourContent.getAttributeNames())
                        {
                            Object value = ourContent.getAttribute(name);

                            if (value != null)
                                transportInfoContent.setAttribute(name, value);
                        }
                        transportInfoContent.addChildExtension(
                                createTransport(stream));

                        transportInfoContents.clear();
                        transportInfoContents.add(transportInfoContent);

                        transportInfoSender.sendTransportInfo(
                            transportInfoContents);
                    }
                }
            }
        }.start();
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
        /* If ICE is already running, we try to update the checklists with
         * the candidates. Note that this is a best effort.
         */
        if (IceProcessingState.RUNNING.equals(iceAgent.getState()))
        {
            if(logger.isInfoEnabled())
            {
                logger.info("Update ICE remote candidates");
            }

            for (ContentPacketExtension content : remote)
            {
                GTalkTransportPacketExtension transport
                    = content.getFirstChildOfType(
                            GTalkTransportPacketExtension.class);

                List<GTalkCandidatePacketExtension> candidates
                    = transport.getChildExtensionsOfType(
                        GTalkCandidatePacketExtension.class);

                if(candidates == null || candidates.size() == 0)
                {
                    return false;
                }

                RtpDescriptionPacketExtension description
                    = content.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);

                if (description == null)
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

                if (description == null)
                    continue;

                IceMediaStream stream = iceAgent.getStream(
                        description.getMedia());

                for (GTalkCandidatePacketExtension candidate : candidates)
                {
                    /* Different stream may have different ufrag/password */
                    String ufrag = candidate.getUsername();

                    /*
                     * Is the remote candidate from the current generation of
                     * the iceAgent?
                     */
                    if (candidate.getGeneration() != iceAgent.getGeneration())
                        continue;

                    Component component
                        = stream.getComponent(candidate.getComponent());

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

        for (ContentPacketExtension content : remote)
        {
            GTalkTransportPacketExtension transport
                = content.getFirstChildOfType(
                        GTalkTransportPacketExtension.class);

            List<GTalkCandidatePacketExtension> candidates
                = transport.getChildExtensionsOfType(
                        GTalkCandidatePacketExtension.class);

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

            if (description == null)
                continue;

            IceMediaStream stream = iceAgent.getStream(description.getMedia());

            for (GTalkCandidatePacketExtension candidate : candidates)
            {
                String ufrag = candidate.getUsername();

                /*
                 * Is the remote candidate from the current generation of the
                 * iceAgent?
                 */
                if (candidate.getGeneration() != generation)
                    continue;

                if(candidate.getProtocol().equalsIgnoreCase("ssltcp"))
                    continue;

                Component component = null;

                synchronized(wrapupSyncRoot)
                {
                    component = stream.getComponent(candidate.getComponent());
                }

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

                logger.info("add remote candidate");
                startConnectivityEstablishment = true;
            }
        }
        if (startConnectivityEstablishment)
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
}
