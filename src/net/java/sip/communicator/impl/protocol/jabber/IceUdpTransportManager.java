/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.ice4j.security.*;

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
     * The <tt>Logger</tt> used by the <tt>IceUdpTransportManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(IceUdpTransportManager.class.getName());

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
        NetworkAddressManagerService namSer = getNetAddrMgr();

        Agent agent = namSer.createIceAgent();

        //we will now create the harvesters
        JabberAccountID accID = (JabberAccountID)provider.getAccountID();

        if (accID.isStunServerDiscoveryEnabled())
        {
            //the default server is supposed to use the same user name and
            //password as the account itself.
            String username = provider.getOurJID();
            String password = JabberActivator
                .getProtocolProviderFactory().loadPassword(accID);

            StunCandidateHarvester autoHarvester = namSer.discoverStunServer(
                                accID.getService(),
                                StringUtils.getUTF8Bytes(username),
                                StringUtils.getUTF8Bytes(password) );

            logger.info("Auto discovered harvester is " + autoHarvester);

            if (autoHarvester != null)
                agent.addCandidateHarvester(autoHarvester);
        }

        //now create stun server descriptors for whatever other STUN/TURN
        //servers the user may have set.
        for(StunServerDescriptor desc : accID.getStunServers())
        {
            TransportAddress addr = new TransportAddress(
                            desc.getAddress(), desc.getPort(), Transport.UDP);

            StunCandidateHarvester harvester;

            if(desc.isTurnSupported())
            {
                //Yay! a TURN server
                harvester = new TurnCandidateHarvester(
                    addr, new LongTermCredential(
                                    desc.getUsername(), desc.getPassword()));
            }
            else
            {
                //this is a STUN only server
                harvester = new StunCandidateHarvester(addr);
            }

            logger.info("Adding pre-conficugred harvester " + harvester);

            agent.addCandidateHarvester(harvester);
        }

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


            //now add our transport to our answer
            ContentPacketExtension cpExt
                = findContentByName(ourAnswer, content.getName());

            //it might be that we decided not to reply to this content
            if(cpExt == null)
                continue;

            //cpExt.addChildExtension(ourTransport);
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
     * @param ourOffer the content list that should tell us how many stream
     * connectors we actually need.
     *
     * @throws OperationFailedException in case we fail allocating ports
     */
    public void startCandidateHarvest(
                            List<ContentPacketExtension>   ourOffer)
        throws OperationFailedException
    {
        for(ContentPacketExtension content : ourOffer)
        {
            RtpDescriptionPacketExtension rtpDesc
                = (RtpDescriptionPacketExtension)content
                    .getFirstChildOfType(RtpDescriptionPacketExtension.class);

            IceMediaStream stream;
            try
            {
                //the following call involves STUN calls so it may take a while.
                stream = getNetAddrMgr().createIceStream(
                            nextMediaPortToTry, rtpDesc.getMedia(), iceAgent);
            }
            catch (Exception exc)
            {
                throw new OperationFailedException(
                        "Failed to initialize stream " + rtpDesc.getMedia(),
                        OperationFailedException.INTERNAL_ERROR);
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

            //we now generate the XMPP code containing the candidates.
            content.addChildExtension(JingleUtils.createTransport(stream));
        }

        this.cpeList = ourOffer;
    }



    /**
     * Simply returns the list of local candidates that we gathered during the
     * harvest. This is a raw udp transport manager so there's no real wrapping
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
}
