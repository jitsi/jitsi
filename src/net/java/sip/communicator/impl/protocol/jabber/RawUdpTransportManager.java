/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * A {@link TransportManager} implementation that would only gather a single
 * candidate pair (i.e. RTP and RTCP).
 *
 * @author Emil Ivov
 */
public class RawUdpTransportManager
{
    /**
     * This is where we keep our answer between the time we get the offer and
     * are ready with the answer;
     */
    private List<ContentPacketExtension> cpeList;

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
     * @param mediaHandler this is a temporary hack param that should eventually
     * go away and that we currently use to access the connecotrs.
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public void startCandidateHarvest(List<ContentPacketExtension> theirOffer,
                    List<ContentPacketExtension>             ourAnswer,
                    CallPeerMediaHandler<CallPeerJabberImpl> mediaHandler)
        throws OperationFailedException
    {
        //XXX for the time being we only generate almost dummy XML in here in
        //order to patch up an early Raw UDP impl and help up the guys from
        //Jingle Nodes. Eventually, the stream connector should be connected in
        //here.
        for(ContentPacketExtension content : theirOffer)
        {
            RtpDescriptionPacketExtension rtpDesc
                = (RtpDescriptionPacketExtension)content
                    .getFirstChildOfType(RtpDescriptionPacketExtension.class);

            StreamConnector connector = mediaHandler.getStreamConnector(
                            MediaType.parseString( rtpDesc.getMedia()));

            //XXX we are temporarily using ice ext as a catchall for both ICE
            //and RawUDP
            IceUdpTransportPacketExtension theirTransport
                = (IceUdpTransportPacketExtension)content
                    .getFirstChildOfType(IceUdpTransportPacketExtension.class);

            IceUdpTransportPacketExtension ourTransport
                = new IceUdpTransportPacketExtension(
                                theirTransport.getNamespace(),
                                theirTransport.getElementName());

            //create and add candidates that correspond to the stream connector
            //rtp
            CandidatePacketExtension rtpCand = new CandidatePacketExtension();
            rtpCand.setComponent(1);
            rtpCand.setFoundation(1);
            rtpCand.setGeneration(0);
            rtpCand.setID("2");
            rtpCand.setIP(connector.getDataSocket()
                            .getLocalAddress().getHostAddress());
            rtpCand.setNetwork(1);
            rtpCand.setPort(connector.getDataSocket().getLocalPort());
            rtpCand.setPriority(2013266431 );
            rtpCand.setProtocol("udp");
            rtpCand.setType(CandidateType.host);

            ourTransport.addCandidate(rtpCand);

            //rtcp
            CandidatePacketExtension rtcpCand = new CandidatePacketExtension();
            rtcpCand.setComponent(2);
            rtcpCand.setFoundation(2);
            rtcpCand.setGeneration(0);
            rtcpCand.setID("1");
            rtcpCand.setIP(connector.getControlSocket()
                            .getLocalAddress().getHostAddress());
            rtcpCand.setNetwork(1);
            rtcpCand.setPort(connector.getControlSocket().getLocalPort());
            rtcpCand.setPriority(2013266430);
            rtcpCand.setProtocol("udp");
            rtcpCand.setType(CandidateType.host);

            ourTransport.addCandidate(rtcpCand);

            //now add our transport to our answer
            ContentPacketExtension cpExt
                = findContentByName(ourAnswer, content.getName());

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
     * @param ourAnswer
     * @param mediaHandler
     *
     * @param ourOffer the content descriptions that we should be adding our
     * transport lists to (although not necessarily in this very instance).
     * @throws OperationFailedException
     */
    public void startCandidateHarvest(
                            List<ContentPacketExtension>   ourAnswer,
                            CallPeerMediaHandlerJabberImpl mediaHandler)
        throws OperationFailedException
    {
        //XXX for the time being we only generate almost dummy XML in here in
        //order to patch up an early Raw UDP impl and help up the guys from
        //Jingle Nodes. Eventually, the stream connector should be connected in
        //here.
        for(ContentPacketExtension content : ourAnswer)
        {
            RtpDescriptionPacketExtension rtpDesc
                = (RtpDescriptionPacketExtension)content
                    .getFirstChildOfType(RtpDescriptionPacketExtension.class);

            StreamConnector connector = mediaHandler.getStreamConnector(
                            MediaType.parseString( rtpDesc.getMedia()));

            //XXX we are temporarily using ice ext as a catchall for both ICE
            //and RawUDP
            IceUdpTransportPacketExtension ourTransport
                = new IceUdpTransportPacketExtension(
                                RawUdpTransportPacketExtension.NAMESPACE,
                                RawUdpTransportPacketExtension.ELEMENT_NAME);

            //create and add candidates that correspond to the stream connector
            //rtp
            CandidatePacketExtension rtpCand = new CandidatePacketExtension();
            rtpCand.setComponent(1);
            rtpCand.setFoundation(1);
            rtpCand.setGeneration(0);
            rtpCand.setID("2");
            rtpCand.setIP(connector.getDataSocket()
                            .getLocalAddress().getHostAddress());
            rtpCand.setNetwork(1);
            rtpCand.setPort(connector.getDataSocket().getLocalPort());
            rtpCand.setPriority(2013266431 );
            rtpCand.setProtocol("udp");
            rtpCand.setType(CandidateType.host);

            ourTransport.addCandidate(rtpCand);

            //rtcp
            CandidatePacketExtension rtcpCand = new CandidatePacketExtension();
            rtcpCand.setComponent(2);
            rtcpCand.setFoundation(2);
            rtcpCand.setGeneration(0);
            rtcpCand.setID("1");
            rtcpCand.setIP(connector.getControlSocket()
                            .getLocalAddress().getHostAddress());
            rtcpCand.setNetwork(1);
            rtcpCand.setPort(connector.getControlSocket().getLocalPort());
            rtcpCand.setPriority(2013266430);
            rtcpCand.setProtocol("udp");
            rtcpCand.setType(CandidateType.host);

            ourTransport.addCandidate(rtcpCand);

            //now add our transport to our answer
            ContentPacketExtension cpExt
                = findContentByName(ourAnswer, content.getName());

            cpExt.addChildExtension(ourTransport);
        }

        this.cpeList = ourAnswer;
    }

    /**
     * Notifies the transport manager that it should conclude candidate
     * harvesting as soon as possible an return the lists of candidates
     * gathered so far.
     *
     * @return the content list that we received earlier (possibly cloned into
     * a new instance) and that we have updated with transport lists.
     */
    public List<ContentPacketExtension> wrapupHarvest()
    {
        //XXX we don't really do anything here for the time being
        return cpeList;
    }

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
