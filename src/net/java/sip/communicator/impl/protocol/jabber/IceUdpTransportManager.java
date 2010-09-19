/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.CandidateType;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;

import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;

/**
 * A {@link TransportManagerJabberImpl} implementation that would use ICE for
 * candidate management.
 *
 * @author Emil Ivov
 */
public class IceUdpTransportManager
    extends TransportManagerJabberImpl
{
    /**
     * This is where we keep our answer between the time we get the offer and
     * are ready with the answer;
     */
    private List<ContentPacketExtension> cpeList;

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
    protected IceUdpTransportManager(CallPeerJabberImpl callPeer)
    {
        super(callPeer);

        iceAgent = createIceAgent();
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
        ProtocolProviderServiceJabberImpl provider
                = getCallPeer().getProtocolProvider();
        NetworkAddressManagerService namSer
                = JabberActivator.getNetworkAddressManagerService();

        Agent agent = namSer.createIceAgent();

        //we will now create the harvesters
        JabberAccountID accID = (JabberAccountID)provider.getAccountID();

        List<StunServerDescriptor> stunServers
            = new ArrayList<StunServerDescriptor>();
        if (accID.isStunServerDiscoveryEnabled())
        {
            //the default server is supposed to use the same user name and
            //password as the account itself.
            String username = provider.getOurJID();
            String password = JabberActivator
                .getProtocolProviderFactory().loadPassword(accID);

            StunCandidateHarvester autoHarvester
                = namSer.discoverStunServer(
                    accID.getService(), username.getBytes("UTF-8"), password.getBytes("UTF-8"));

        }

        List<StunServerDescriptor> additionalStunServers
            = accID.getStunServers();

        return agent;
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the {@link #wrapupHarvest()}
     * method which would be called once we absolutely need the candidates.
     *
     * @param theirOffer a media description offer that we've received from the
     * remote party and that we should use in case we need to know what
     * transports our peer is using.
     * @param ourAnswer the content descriptions that we should be adding our
     * transport lists to (although not necessarily in this very instance).
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public void startCandidateHarvest(List<ContentPacketExtension> theirOffer,
                                      List<ContentPacketExtension> ourAnswer)
        throws OperationFailedException
    {
        for(ContentPacketExtension content : theirOffer)
        {
            RtpDescriptionPacketExtension rtpDesc
                = (RtpDescriptionPacketExtension)content
                    .getFirstChildOfType(RtpDescriptionPacketExtension.class);

            StreamConnector connector = getStreamConnector(
                            MediaType.parseString( rtpDesc.getMedia()));

            IceUdpTransportPacketExtension ourTransport
                                        = createTransport(connector);

            //now add our transport to our answer
            ContentPacketExtension cpExt
                = findContentByName(ourAnswer, content.getName());

            //it might be that we decided not to reply to this content
            if(cpExt == null)
                continue;

            cpExt.addChildExtension(ourTransport);
        }

        this.cpeList = ourAnswer;
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the {@link #wrapupHarvest()}
     * method which would be called once we absolutely need the candidates.
     *
     * @param ourAnswer the content list that should tell us how many stream
     * connectors we actually need.
     *
     * @throws OperationFailedException in case we fail allocating ports
     */
    public void startCandidateHarvest(
                            List<ContentPacketExtension>   ourAnswer)
        throws OperationFailedException
    {
        for(ContentPacketExtension content : ourAnswer)
        {
            RtpDescriptionPacketExtension rtpDesc
                = (RtpDescriptionPacketExtension)content
                    .getFirstChildOfType(RtpDescriptionPacketExtension.class);

            StreamConnector connector = getStreamConnector(
                            MediaType.parseString( rtpDesc.getMedia()));

            IceUdpTransportPacketExtension ourTransport
                                        = createTransport(connector);

            //now add our transport to our answer
            ContentPacketExtension cpExt
                = findContentByName(ourAnswer, content.getName());

            cpExt.addChildExtension(ourTransport);
        }
        this.cpeList = ourAnswer;
    }

    /**
     * Creates an ICE UDP transport element according to the specified stream
     * <tt>connector</tt>
     *
     * @param connector the connector that we'd like to describe within the
     * transport element.
     *
     * @return a {@link RawUdpTransportPacketExtension} containing the RTP and
     * RTCP candidates of the specified {@link StreamConnector}.
     */
    private IceUdpTransportPacketExtension createTransport(
                                                StreamConnector connector)
    {
        RawUdpTransportPacketExtension ourTransport
            = new RawUdpTransportPacketExtension();





        // create and add candidates that correspond to the stream connector
        // rtp
        CandidatePacketExtension rtpCand = new CandidatePacketExtension();
        rtpCand.setComponent(CandidatePacketExtension.RTP_COMPONENT_ID);
        rtpCand.setGeneration(getCurrentGeneration());
        rtpCand.setID(getNextID());
        rtpCand.setIP(connector.getDataSocket().getLocalAddress()
                        .getHostAddress());
        rtpCand.setPort(connector.getDataSocket().getLocalPort());
        rtpCand.setType(CandidateType.host);

        ourTransport.addCandidate(rtpCand);

        // rtcp
        CandidatePacketExtension rtcpCand = new CandidatePacketExtension();
        rtcpCand.setComponent(CandidatePacketExtension.RTCP_COMPONENT_ID);
        rtcpCand.setGeneration(getCurrentGeneration());
        rtcpCand.setID(getNextID());
        rtcpCand.setIP(connector.getControlSocket().getLocalAddress()
                        .getHostAddress());
        rtcpCand.setPort(connector.getControlSocket().getLocalPort());
        rtcpCand.setType(CandidateType.host);

        ourTransport.addCandidate(rtcpCand);

        return ourTransport;
    }

    /**
     * Simply returns the list of local candidates that we gathered during the
     * harvest. This is a raw udp transport manager so there's no real wraping
     * up to do.
     *
     * @return the list of local candidates that we gathered during the
     * harvest.
     */
    public List<ContentPacketExtension> wrapupHarvest()
    {
        return cpeList;
    }

    /**
     * Looks through the <tt>cpExtList</tt> and returns the {@link
     * ContentPacketExtension} with the specified name.
     *
     * @param cpExtList the list that we will be searching for a specific
     * content.
     * @param name the name of the content element we are looking for.
     * @return the {@link ContentPacketExtension} with the specified name or
     * <tt>null</tt> if no such content element exists.
     */
    private ContentPacketExtension findContentByName(
                                        List<ContentPacketExtension> cpExtList,
                                        String                       name)
    {
        for(ContentPacketExtension cpExt : cpExtList)
        {
            if(cpExt.getName().equals(name))
                return cpExt;
        }

        return null;
    }
}
