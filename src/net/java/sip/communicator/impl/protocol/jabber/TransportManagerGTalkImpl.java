/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.beans.*;
import java.net.*;
import java.util.*;

import org.ice4j.*;
import org.ice4j.ice.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

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
{
    /**
     * The <tt>Logger</tt> used by the <tt>IceUdpTransportManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(TransportManagerGTalkImpl.class);

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
     * Creates the ICE agent that we would be using in this transport manager
     * for all negotiation.
     *
     * @return the ICE agent to use for all the ICE negotiation that this
     * transport manager would be going through
     */
    private Agent createIceAgent()
    {
        CallPeerGTalkImpl peer = getCallPeer();
        Agent agent = null;

        /* XXX wait changes from ice4j
        agent = new Agent(CompatibilityMode.GTALK);
        agent.setControlling(!peer.isInitiator());
        */

        /* XXX no configured STUN/TURN for the moment
         * it should be discovered by a Google XMPP extension
        for(StunServerDescriptor desc : accID.getStunServers())
        {
            TransportAddress addr = new TransportAddress(
                            desc.getAddress(), desc.getPort(), Transport.UDP);

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

        if(accID.isUPNPEnabled())
        {
            UPNPHarvester harvester = new UPNPHarvester();

            if(harvester != null)
            {
                agent.addCandidateHarvester(harvester);
            }
        }
        */

        return agent;
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

        /*
         * XXX If the iceAgent has not completed (yet), go with a default
         * StreamConnector (until it completes).
         */
        return
            (streamConnectorSockets == null)
                ? super.createStreamConnector(mediaType)
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
                                    ))
                            //|| (streamConnector.getControlSocket()
                           //        != streamConnectorSockets[1 /* RTCP */])))
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
                            = selectedPair.getLocalCandidate().getSocket();

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
                // XXX GTalk audio has not RTCP channel
                if(mediaName.equals("rtp") && streamConnectorSocketCount == 1)
                {
                    try
                    {
                        streamConnectorSockets[1] = new DatagramSocket();
                    }
                    catch(Exception e)
                    {
                    }
                }

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
                int rtcpIndex = 1;

                // XXX GTalk audio has not RTCP channel
                if(mediaName.equals("rtp") && streamTargetAddressCount == 1)
                {
                    rtcpIndex = 0;
                }

                streamTarget
                    = new MediaStreamTarget(
                            streamTargetAddresses[0 /* RTP */],
                            streamTargetAddresses[rtcpIndex /* RTCP */]);
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
     * @return the newly created {@link IceMediaStream}
     *
     * @throws OperationFailedException if binding on the specified media stream
     * fails for some reason.
     */
    private IceMediaStream createIceStream(String media)
        throws OperationFailedException
    {
        IceMediaStream stream;

        try
        {
            //the following call involves STUN processing so it may take a while
            stream = getNetAddrMgr().createIceStream(
                        nextMediaPortToTry, media, iceAgent);
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
            nextMediaPortToTry = stream.getComponent(Component.RTCP)
                .getLocalCandidates().get(0)
                    .getTransportAddress().getPort() + 1;
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
            IceMediaStream stream = createIceStream("rtp");

            /* remove RTCP component for the audio as it is not used and
             * remote gmail peer does not send them
             */
            for(Component cmp : stream.getComponents())
            {
                if(cmp.getComponentID() == 2)
                {
                    stream.removeComponent(cmp);
                }
            }

            candidates.addAll(GTalkPacketFactory.createCandidates("rtp",
                    stream));
        }

        if(video)
        {
            IceMediaStream stream = createIceStream("video_rtp");
            candidates.addAll(GTalkPacketFactory.createCandidates("video_rtp",
                    stream));
        }

        /* send candidates */
        candidatesSender.sendCandidates(candidates);
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

                // change name to retrieve properly the ICE media stream
                if(name.equals("rtp"))
                {
                    numComponent = 1;
                }
                else if(name.equals("rtcp"))
                {
                    name = "rtp";
                    numComponent = 1;
                }
                else if(name.equals("video_rtp"))
                {
                    numComponent = 1;
                }
                else if(name.equals("video_rtcp"))
                {
                    name = "video_rtp";
                    numComponent = 2;
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

                // XXX UDP only for the moment as ice4j.org does not support
                // TCP yet
                if(!candidate.getProtocol().equalsIgnoreCase(
                        Transport.UDP.toString()))
                    continue;

                Component component
                    = stream.getComponent(numComponent);
                /* XXX wait changes from ice4j
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
                */
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
                numComponent = 1;
            }
            else if(name.equals("rtcp"))
            {
                name = "rtp";
                numComponent = 1;
            }
            else if(name.equals("video_rtp"))
            {
                numComponent = 1;
            }
            else if(name.equals("video_rtcp"))
            {
                name = "video_rtp";
                numComponent = 2;
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
            if (candidate.getGeneration() != generation)
                continue;

            if(!candidate.getProtocol().equalsIgnoreCase(
                    Transport.UDP.toString()))
                continue;

            Component component
                = stream.getComponent(numComponent);

            /* XXX wait changes from ice4j
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
            */
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
                    if(component.getName().equals("RTCP"))
                        continue;
                    if (component.getRemoteCandidateCount() < 2)
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
    }

    /**
     * Close this transport manager and release resources.
     */
    public void close()
    {
        if(iceAgent != null)
        {
            iceAgent.free();
        }
    }
}
