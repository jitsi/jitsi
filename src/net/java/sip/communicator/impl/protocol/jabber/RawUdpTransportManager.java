/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A {@link TransportManagerJabberImpl} implementation that would only gather a
 * single candidate pair (i.e. RTP and RTCP).
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class RawUdpTransportManager
    extends TransportManagerJabberImpl
{
    /**
     * The list of <tt>ContentPacketExtension</tt>s which represents the local
     * counterpart of the negotiation between the local and the remote peers.
     */
    private List<ContentPacketExtension> local;

    /**
     * The collection of <tt>ContentPacketExtension</tt>s which represents the
     * remote counterpart of the negotiation between the local and the remote
     * peers.
     */
    private Iterable<ContentPacketExtension> remote;

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeer} whose traffic we will be taking
     * care of.
     */
    public RawUdpTransportManager(CallPeerJabberImpl callPeer)
    {
        super(callPeer);
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
        MediaStreamTarget streamTarget = null;

        if (remote != null)
        {
            for (ContentPacketExtension content : remote)
            {
                RtpDescriptionPacketExtension rtpDescription
                    = content.getFirstChildOfType(
                            RtpDescriptionPacketExtension.class);
                MediaType contentMediaType
                    = MediaType.parseString(rtpDescription.getMedia());

                if (mediaType.equals(contentMediaType))
                {
                    streamTarget = JingleUtils.extractDefaultTarget(content);
                    break;
                }
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
    public String getXmlNamespace()
    {
        return ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RAW_UDP_0;
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
                = content.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);

            StreamConnector connector = getStreamConnector(
                            MediaType.parseString( rtpDesc.getMedia()));

            RawUdpTransportPacketExtension ourTransport
                                        = createTransport(connector);

            //now add our transport to our answer
            ContentPacketExtension cpExt
                = findContentByName(ourAnswer, content.getName());

            //it might be that we decided not to reply to this content
            if(cpExt == null)
                continue;

            cpExt.addChildExtension(ourTransport);
        }

        this.local = ourAnswer;
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
    public void startCandidateHarvest(List<ContentPacketExtension> ourOffer)
        throws OperationFailedException
    {
        for(ContentPacketExtension content : ourOffer)
        {
            RtpDescriptionPacketExtension rtpDesc
                = content.getFirstChildOfType(
                        RtpDescriptionPacketExtension.class);

            StreamConnector connector
                = getStreamConnector(
                    MediaType.parseString( rtpDesc.getMedia()));

            RawUdpTransportPacketExtension ourTransport
                = createTransport(connector);

            //now add our transport to our offer
            ContentPacketExtension cpExt
                = findContentByName(ourOffer, content.getName());

            cpExt.addChildExtension(ourTransport);
        }

        this.local = ourOffer;
    }

    /**
     * Creates a raw UDP transport element according to the specified stream
     * <tt>connector</tt>.
     *
     * @param connector the connector that we'd like to describe within the
     * transport element.
     *
     * @return a {@link RawUdpTransportPacketExtension} containing the RTP and
     * RTCP candidates of the specified {@link StreamConnector}.
     */
    private RawUdpTransportPacketExtension createTransport(
                                                StreamConnector connector)
    {
        RawUdpTransportPacketExtension ourTransport
            = new RawUdpTransportPacketExtension();

        // create and add candidates that correspond to the stream connector
        // RTP
        CandidatePacketExtension rtpCand = new CandidatePacketExtension();
        rtpCand.setComponent(CandidatePacketExtension.RTP_COMPONENT_ID);
        rtpCand.setGeneration(getCurrentGeneration());
        rtpCand.setID(getNextID());
        rtpCand.setIP(connector.getDataSocket().getLocalAddress()
                        .getHostAddress());
        rtpCand.setPort(connector.getDataSocket().getLocalPort());
        rtpCand.setType(CandidateType.host);

        ourTransport.addCandidate(rtpCand);

        // RTCP
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
     * harvest. This is a raw UDP transport manager so there's no real wrapping
     * up to do.
     *
     * @return the list of local candidates that we gathered during the harvest
     * @see TransportManagerJabberImpl#wrapupCandidateHarvest()
     */
    public List<ContentPacketExtension> wrapupCandidateHarvest()
    {
        return local;
    }

    /**
     * Implements
     * {@link TransportManagerJabberImpl#startConnectivityEstablishment(Iterable)}.
     * Since this represents a raw UDP transport, performs no connectivity
     * checks and just remembers the remote counterpart of the negotiation
     * between the local and the remote peers in order to be able to report the
     * <tt>MediaStreamTarget</tt>s upon request.
     *
     * @param remote the collection of <tt>ContentPacketExtension</tt>s which
     * represent the remote counterpart of the negotiation between the local and
     * the remote peers
     * @see TransportManagerJabberImpl#startConnectivityEstablishment(Iterable)
     */
    public void startConnectivityEstablishment(
            Iterable<ContentPacketExtension> remote)
    {
        this.remote = remote;
    }

    /**
     * Implements
     * {@link TransportManagerJabberImpl#wrapupConnectivityEstablishment()}.
     * Since this represents a raw UDP transport i.e. no connectivity checks are
     * performed, just returns the local counterpart of the negotiation between
     * the local and the remote peers.
     *
     * @return the local counterpart of the negotiation between the local and
     * the remote peers
     * @see TransportManagerJabberImpl#wrapupConnectivityEstablishment()
     */
    public Iterable<ContentPacketExtension> wrapupConnectivityEstablishment()
    {
        return local;
    }
}
