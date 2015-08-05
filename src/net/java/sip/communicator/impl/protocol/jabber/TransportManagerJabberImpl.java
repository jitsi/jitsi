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

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.packet.*;

/**
 * <tt>TransportManager</tt>s gather local candidates for incoming and outgoing
 * calls. Their work starts by calling a start method which, using the remote
 * peer's session description, would start the harvest. Calling a second wrapup
 * method would deliver the candidate harvest, possibly after blocking if it has
 * not yet completed.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public abstract class TransportManagerJabberImpl
    extends TransportManager<CallPeerJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>TransportManagerJabberImpl</tt> class
     * and its instances to print debug messages.
     */
    private static final Logger logger
        = Logger.getLogger(TransportManagerJabberImpl.class);

    /**
     * The ID that we will be assigning to our next candidate. We use
     * <tt>int</tt>s for interoperability reasons (Emil: I believe that GTalk
     * uses <tt>int</tt>s. If that turns out not to be the case we can stop
     * using <tt>int</tt>s here if that's an issue).
     */
    private static int nextID = 1;

    /**
     * The information pertaining to the Jisti Videobridge conference which the
     * local peer represented by this instance is a focus of. It gives a view of
     * the whole Jitsi Videobridge conference managed by the associated
     * <tt>CallJabberImpl</tt> which provides information specific to this
     * <tt>TransportManager</tt> only.
     */
    private ColibriConferenceIQ colibri;

    /**
     * The generation of the candidates we are currently generating
     */
    private int currentGeneration = 0;

    /**
     * The indicator which determines whether this <tt>TransportManager</tt>
     * instance is responsible to establish the connectivity with the associated
     * Jitsi Videobridge (in case it is being employed at all).
     */
    boolean isEstablishingConnectivityWithJitsiVideobridge = false;

    /**
     * The indicator which determines whether this <tt>TransportManager</tt>
     * instance is yet to start establishing the connectivity with the
     * associated Jitsi Videobridge (in case it is being employed at all).
     */
    boolean startConnectivityEstablishmentWithJitsiVideobridge = false;

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link CallPeer} whose traffic we will be taking
     * care of.
     */
    protected TransportManagerJabberImpl(CallPeerJabberImpl callPeer)
    {
        super(callPeer);
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
    protected InetAddress getIntendedDestination(CallPeerJabberImpl peer)
    {
        return peer.getProtocolProvider().getNextHop();
    }

    /**
     * Returns the ID that we will be assigning to the next candidate we create.
     *
     * @return the next ID to use with a candidate.
     */
    protected String getNextID()
    {
        int nextID;

        synchronized (TransportManagerJabberImpl.class)
        {
            nextID = TransportManagerJabberImpl.nextID++;
        }
        return Integer.toString(nextID);
    }

    /**
     * Gets the <tt>MediaStreamTarget</tt> to be used as the <tt>target</tt> of
     * the <tt>MediaStream</tt> with a specific <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> which
     * is to have its <tt>target</tt> set to the returned
     * <tt>MediaStreamTarget</tt>
     * @return the <tt>MediaStreamTarget</tt> to be used as the <tt>target</tt>
     * of the <tt>MediaStream</tt> with the specified <tt>MediaType</tt>
     */
    public abstract MediaStreamTarget getStreamTarget(MediaType mediaType);

    /**
     * Gets the XML namespace of the Jingle transport implemented by this
     * <tt>TransportManagerJabberImpl</tt>.
     *
     * @return the XML namespace of the Jingle transport implemented by this
     * <tt>TransportManagerJabberImpl</tt>
     */
    public abstract String getXmlNamespace();

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
     * Sends transport-related information received from the remote peer to the
     * associated Jiitsi Videobridge in order to update the (remote)
     * <tt>ColibriConferenceIQ.Channel</tt> associated with this
     * <tt>TransportManager</tt> instance.
     *
     * @param map a <tt>Map</tt> of media-IceUdpTransportPacketExtension pairs
     * which represents the transport-related information which has been
     * received from the remote peer and which is to be sent to the associated
     * Jitsi Videobridge
     */
    protected void sendTransportInfoToJitsiVideobridge(
            Map<String,IceUdpTransportPacketExtension> map)
    {
        CallPeerJabberImpl peer = getCallPeer();
        boolean initiator = !peer.isInitiator();
        ColibriConferenceIQ conferenceRequest = null;

        for (Map.Entry<String,IceUdpTransportPacketExtension> e
                : map.entrySet())
        {
            String media = e.getKey();
            MediaType mediaType = MediaType.parseString(media);
            ColibriConferenceIQ.Channel channel
                = getColibriChannel(mediaType, false /* remote */);

            if (channel != null)
            {
                IceUdpTransportPacketExtension transport;

                try
                {
                    transport = cloneTransportAndCandidates(e.getValue());
                }
                catch (OperationFailedException ofe)
                {
                    transport = null;
                }
                if (transport == null)
                    continue;

                ColibriConferenceIQ.Channel channelRequest
                    = new ColibriConferenceIQ.Channel();

                channelRequest.setID(channel.getID());
                channelRequest.setInitiator(initiator);
                channelRequest.setTransport(transport);

                if (conferenceRequest == null)
                {
                    if (colibri == null)
                        break;
                    else
                    {
                        String id = colibri.getID();

                        if ((id == null) || (id.length() == 0))
                            break;
                        else
                        {
                            conferenceRequest = new ColibriConferenceIQ();
                            conferenceRequest.setID(id);
                            conferenceRequest.setTo(colibri.getFrom());
                            conferenceRequest.setType(IQ.Type.SET);
                        }
                    }
                }
                conferenceRequest.getOrCreateContent(media).addChannel(
                        channelRequest);
            }
        }
        if (conferenceRequest != null)
        {
            peer.getProtocolProvider().getConnection().sendPacket(
                    conferenceRequest);
        }
    }

    /**
     * Starts transport candidate harvest for a specific
     * <tt>ContentPacketExtension</tt> that we are going to offer or answer
     * with.
     *
     * @param theirContent the <tt>ContentPacketExtension</tt> offered by the
     * remote peer to which we are going to answer with <tt>ourContent</tt> or
     * <tt>null</tt> if <tt>ourContent</tt> will be an offer to the remote peer 
     * @param ourContent the <tt>ContentPacketExtension</tt> for which transport
     * candidate harvest is to be started
     * @param transportInfoSender a <tt>TransportInfoSender</tt> if the
     * harvested transport candidates are to be sent in a
     * <tt>transport-info</tt> rather than in <tt>ourContent</tt>; otherwise,
     * <tt>null</tt>
     * @param media the media of the <tt>RtpDescriptionPacketExtension</tt>
     * child of <tt>ourContent</tt>
     * @return a <tt>PacketExtension</tt> to be added as a child to
     * <tt>ourContent</tt>; otherwise, <tt>null</tt>
     * @throws OperationFailedException if anything goes wrong while starting
     * transport candidate harvest for the specified <tt>ourContent</tt>
     */
    protected abstract PacketExtension startCandidateHarvest(
            ContentPacketExtension theirContent,
            ContentPacketExtension ourContent,
            TransportInfoSender transportInfoSender,
            String media)
        throws OperationFailedException;

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
     */
    public void startCandidateHarvest(
            List<ContentPacketExtension> theirOffer,
            List<ContentPacketExtension> ourAnswer,
            TransportInfoSender transportInfoSender)
        throws OperationFailedException
    {
        CallPeerJabberImpl peer = getCallPeer();
        CallJabberImpl call = peer.getCall();
        boolean isJitsiVideobridge = call.getConference().isJitsiVideobridge();
        List<ContentPacketExtension> cpes
            = (theirOffer == null) ? ourAnswer : theirOffer;

        /*
         * If Jitsi Videobridge is to be used, determine which channels are to
         * be allocated and attempt to allocate them now.
         */
        if (isJitsiVideobridge)
        {
            Map<ContentPacketExtension,ContentPacketExtension> contentMap
                = new LinkedHashMap
                    <ContentPacketExtension,ContentPacketExtension>();

            for (ContentPacketExtension cpe : cpes)
            {
                MediaType mediaType = JingleUtils.getMediaType(cpe);

                /*
                 * The existence of a content for the mediaType and regardless
                 * of the existence of channels in it signals that a channel
                 * allocation request has already been sent for that mediaType.
                 */
                if ((colibri == null)
                        || (colibri.getContent(mediaType.toString()) == null))
                {
                    ContentPacketExtension local, remote;

                    if (cpes == ourAnswer)
                    {
                        local = cpe;
                        remote
                            = (theirOffer == null)
                                ? null
                                : findContentByName(theirOffer, cpe.getName());
                    }
                    else
                    {
                        local = findContentByName(ourAnswer, cpe.getName());
                        remote = cpe;
                    }
                    contentMap.put(local, remote);
                }
            }
            if (!contentMap.isEmpty())
            {
                /*
                 * We are about to request the channel allocations for the media
                 * types found in contentMap. Regardless of the response, we do
                 * not want to repeat these requests.
                 */
                if (colibri == null)
                    colibri = new ColibriConferenceIQ();
                for (Map.Entry<ContentPacketExtension,ContentPacketExtension> e
                        : contentMap.entrySet())
                {
                    ContentPacketExtension cpe = e.getValue();

                    if (cpe == null)
                        cpe = e.getKey();

                    colibri.getOrCreateContent(
                        JingleUtils.getMediaType(cpe).toString());
                }

                ColibriConferenceIQ conferenceResult
                    = call.createColibriChannels(peer, contentMap);

                if (conferenceResult != null)
                {
                    String videobridgeID = colibri.getID();
                    String conferenceResultID = conferenceResult.getID();

                    if (videobridgeID == null)
                        colibri.setID(conferenceResultID);
                    else if (!videobridgeID.equals(conferenceResultID))
                        throw new IllegalStateException("conference.id");

                    String videobridgeFrom = conferenceResult.getFrom();

                    if ((videobridgeFrom != null)
                            && (videobridgeFrom.length() != 0))
                    {
                        colibri.setFrom(videobridgeFrom);
                    }

                    for (ColibriConferenceIQ.Content contentResult
                            : conferenceResult.getContents())
                    {
                        ColibriConferenceIQ.Content content
                            = colibri.getOrCreateContent(
                                    contentResult.getName());

                        for (ColibriConferenceIQ.Channel channelResult
                                : contentResult.getChannels())
                        {
                            if (content.getChannel(channelResult.getID())
                                    == null)
                            {
                                content.addChannel(channelResult);
                            }
                        }
                    }
                }
                else
                {
                    /*
                     * The call fails if the createColibriChannels method fails
                     * which may happen if the conference packet times out or it
                     * can't be built.
                     */
                    ProtocolProviderServiceJabberImpl
                        .throwOperationFailedException(
                                "Failed to allocate colibri channel.",
                                OperationFailedException.GENERAL_ERROR,
                                null,
                                logger);
                }
            }
        }

        for (ContentPacketExtension cpe : cpes)
        {
            String contentName = cpe.getName();
            ContentPacketExtension ourContent
                = findContentByName(ourAnswer, contentName);

            //it might be that we decided not to reply to this content
            if (ourContent != null)
            {
                ContentPacketExtension theirContent
                    = (theirOffer == null)
                        ? null
                        : findContentByName(theirOffer, contentName);
                RtpDescriptionPacketExtension rtpDesc
                    = ourContent.getFirstChildOfType(
                            RtpDescriptionPacketExtension.class);
                String media = rtpDesc.getMedia();
                PacketExtension pe
                    = startCandidateHarvest(
                            theirContent,
                            ourContent,
                            transportInfoSender,
                            media);

                if (pe != null)
                    ourContent.addChildExtension(pe);
            }
        }
    }

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the
     * {@link #wrapupCandidateHarvest()} method which would be called once we
     * absolutely need the candidates.
     *
     * @param ourOffer the content descriptions that we should be adding our
     * transport lists to (although not necessarily in this very instance).
     * @param transportInfoSender the <tt>TransportInfoSender</tt> to be used by
     * this <tt>TransportManagerJabberImpl</tt> to send <tt>transport-info</tt>
     * <tt>JingleIQ</tt>s from the local peer to the remote peer if this
     * <tt>TransportManagerJabberImpl</tt> wishes to utilize
     * <tt>transport-info</tt>. Local candidate addresses sent by this
     * <tt>TransportManagerJabberImpl</tt> in <tt>transport-info</tt> are
     * expected to not be included in the result of
     * {@link #wrapupCandidateHarvest()}.
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public void startCandidateHarvest(
            List<ContentPacketExtension> ourOffer,
            TransportInfoSender transportInfoSender)
        throws OperationFailedException
    {
        startCandidateHarvest(
                /* theirOffer */ null,
                ourOffer,
                transportInfoSender);
    }

    /**
     * Notifies the transport manager that it should conclude candidate
     * harvesting as soon as possible and return the lists of candidates
     * gathered so far.
     *
     * @return the content list that we received earlier (possibly cloned into
     * a new instance) and that we have updated with transport lists.
     */
    public abstract List<ContentPacketExtension> wrapupCandidateHarvest();

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
    public static ContentPacketExtension findContentByName(
            Iterable<ContentPacketExtension> cpExtList,
            String name)
    {
        for(ContentPacketExtension cpExt : cpExtList)
        {
            if(cpExt.getName().equals(name))
                return cpExt;
        }
        return null;
    }

    /**
     * Starts the connectivity establishment of this
     * <tt>TransportManagerJabberImpl</tt> i.e. checks the connectivity between
     * the local and the remote peers given the remote counterpart of the
     * negotiation between them.
     *
     * @param remote the collection of <tt>ContentPacketExtension</tt>s which
     * represents the remote counterpart of the negotiation between the local
     * and the remote peer
     * @return <tt>true</tt> if connectivity establishment has been started in
     * response to the call; otherwise, <tt>false</tt>.
     * <tt>TransportManagerJabberImpl</tt> implementations which do not perform
     * connectivity checks (e.g. raw UDP) should return <tt>true</tt>. The
     * default implementation does not perform connectivity checks and always
     * returns <tt>true</tt>.
     */
    public boolean startConnectivityEstablishment(
            Iterable<ContentPacketExtension> remote)
    {
        return true;
    }

    /**
     * Starts the connectivity establishment of this
     * <tt>TransportManagerJabberImpl</tt> i.e. checks the connectivity between
     * the local and the remote peers given the remote counterpart of the
     * negotiation between them.
     *
     * @param remote a <tt>Map</tt> of
     * media-<tt>IceUdpTransportPacketExtension</tt> pairs which represents the
     * remote counterpart of the negotiation between the local and the remote
     * peers
     * @return <tt>true</tt> if connectivity establishment has been started in
     * response to the call; otherwise, <tt>false</tt>.
     * <tt>TransportManagerJabberImpl</tt> implementations which do not perform
     * connectivity checks (e.g. raw UDP) should return <tt>true</tt>. The
     * default implementation does not perform connectivity checks and always
     * returns <tt>true</tt>.
     */
    protected boolean startConnectivityEstablishment(
            Map<String,IceUdpTransportPacketExtension> remote)
    {
        return true;
    }

    /**
     * Notifies this <tt>TransportManagerJabberImpl</tt> that it should conclude
     * any started connectivity establishment.
     *
     * @throws OperationFailedException if anything goes wrong with connectivity
     * establishment (i.e. ICE failed, ...)
     */
    public void wrapupConnectivityEstablishment()
        throws OperationFailedException
    {
    }

    /**
     * Removes a content with a specific name from the transport-related part of
     * the session represented by this <tt>TransportManagerJabberImpl</tt> which
     * may have been reported through previous calls to the
     * <tt>startCandidateHarvest</tt> and
     * <tt>startConnectivityEstablishment</tt> methods.
     * <p>
     * <b>Note</b>: Because <tt>TransportManager</tt> deals with
     * <tt>MediaType</tt>s, not content names and
     * <tt>TransportManagerJabberImpl</tt> does not implement translating from
     * content name to <tt>MediaType</tt>, implementers are expected to call
     * {@link TransportManager#closeStreamConnector(MediaType)}.
     * </p>
     *
     * @param name the name of the content to be removed from the
     * transport-related part of the session represented by this
     * <tt>TransportManagerJabberImpl</tt>
     */
    public abstract void removeContent(String name);

    /**
     * Removes a content with a specific name from a specific collection of
     * contents and closes any associated <tt>StreamConnector</tt>.
     *
     * @param contents the collection of contents to remove the content with the
     * specified name from
     * @param name the name of the content to remove
     * @return the removed <tt>ContentPacketExtension</tt> if any; otherwise,
     * <tt>null</tt>
     */
    protected ContentPacketExtension removeContent(
            Iterable<ContentPacketExtension> contents,
            String name)
    {
        for (Iterator<ContentPacketExtension> contentIter = contents.iterator();
                contentIter.hasNext();)
        {
            ContentPacketExtension content = contentIter.next();

            if (name.equals(content.getName()))
            {
                contentIter.remove();

                // closeStreamConnector
                MediaType mediaType = JingleUtils.getMediaType(content);
                if (mediaType != null)
                {
                    closeStreamConnector(mediaType);
                }

                return content;
            }
        }
        return null;
    }

    /**
     * Clones a specific <tt>IceUdpTransportPacketExtension</tt> and its
     * candidates.
     *
     * @param src the <tt>IceUdpTransportPacketExtension</tt> to be cloned
     * @return a new <tt>IceUdpTransportPacketExtension</tt> instance which has
     * the same run-time type, attributes, namespace, text and candidates as the
     * specified <tt>src</tt>
     * @throws OperationFailedException if an error occurs during the cloing of
     * the specified <tt>src</tt> and its candidates
     */
    static IceUdpTransportPacketExtension cloneTransportAndCandidates(
            IceUdpTransportPacketExtension src)
        throws OperationFailedException
    {
        try
        {
            return IceUdpTransportPacketExtension
                    .cloneTransportAndCandidates(src);
        }
        catch (Exception e)
        {
            ProtocolProviderServiceJabberImpl
                .throwOperationFailedException(
                        "Failed to close transport and candidates.",
                        OperationFailedException.GENERAL_ERROR,
                        e,
                        logger);

        }
        return null;
    }

    /**
     * Releases the resources acquired by this <tt>TransportManager</tt> and
     * prepares it for garbage collection.
     */
    public void close()
    {
        for (MediaType mediaType : MediaType.values())
            closeStreamConnector(mediaType);
    }

    /**
     * Closes a specific <tt>StreamConnector</tt> associated with a specific
     * <tt>MediaType</tt>. If this <tt>TransportManager</tt> has a reference to
     * the specified <tt>streamConnector</tt>, it remains.
     * Also expires the <tt>ColibriConferenceIQ.Channel</tt> associated with
     * the closed <tt>StreamConnector</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> associated with the specified
     * <tt>streamConnector</tt>
     * @param streamConnector the <tt>StreamConnector</tt> to be closed
     */
    @Override
    protected void closeStreamConnector(
            MediaType mediaType,
            StreamConnector streamConnector)
    {
        try
        {
            boolean superCloseStreamConnector = true;

            if (streamConnector instanceof ColibriStreamConnector)
            {
                CallPeerJabberImpl peer = getCallPeer();

                if (peer != null)
                {
                    CallJabberImpl call = peer.getCall();

                    if (call != null)
                    {
                        superCloseStreamConnector = false;
                        call.closeColibriStreamConnector(
                            peer,
                            mediaType,
                            (ColibriStreamConnector) streamConnector);
                    }
                }
            }
            if (superCloseStreamConnector)
                super.closeStreamConnector(mediaType, streamConnector);
        }
        finally
        {
            /*
             * Expire the ColibriConferenceIQ.Channel associated with the closed
             * StreamConnector.
             */
            if (colibri != null)
            {
                ColibriConferenceIQ.Content content
                    = colibri.getContent(mediaType.toString());

                if (content != null)
                {
                    List<ColibriConferenceIQ.Channel> channels
                        = content.getChannels();

                    if (channels.size() == 2)
                    {
                        ColibriConferenceIQ requestConferenceIQ
                            = new ColibriConferenceIQ();

                        requestConferenceIQ.setID(colibri.getID());

                        ColibriConferenceIQ.Content requestContent
                            = requestConferenceIQ.getOrCreateContent(
                                    content.getName());

                        requestContent.addChannel(channels.get(1 /* remote */));

                        /*
                         * Regardless of whether the request to expire the
                         * Channel associated with mediaType succeeds, consider
                         * the Channel in question expired. Since
                         * RawUdpTransportManager allocates a single channel per
                         * MediaType, consider the whole Content expired.
                         */
                        colibri.removeContent(content);

                        CallPeerJabberImpl peer = getCallPeer();

                        if (peer != null)
                        {
                            CallJabberImpl call = peer.getCall();

                            if (call != null)
                            {
                                call.expireColibriChannels(
                                        peer,
                                        requestConferenceIQ);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Adds support for telephony conferences utilizing the Jitsi Videobridge
     * server-side technology.
     *
     * @see #doCreateStreamConnector(MediaType)
     */
    @Override
    protected StreamConnector createStreamConnector(final MediaType mediaType)
        throws OperationFailedException
    {
        ColibriConferenceIQ.Channel channel
            = getColibriChannel(mediaType, true /* local */);

        if (channel != null)
        {
            CallPeerJabberImpl peer = getCallPeer();
            CallJabberImpl call = peer.getCall();
            StreamConnector streamConnector
                = call.createColibriStreamConnector(
                        peer,
                        mediaType,
                        channel,
                        new StreamConnectorFactory()
                        {
                            public StreamConnector createStreamConnector()
                            {
                                try
                                {
                                    return doCreateStreamConnector(mediaType);
                                }
                                catch (OperationFailedException ofe)
                                {
                                    return null;
                                }
                            }
                        });

            if (streamConnector != null)
                return streamConnector;
        }

        return doCreateStreamConnector(mediaType);
    }

    protected abstract PacketExtension createTransport(String media)
        throws OperationFailedException;

    protected PacketExtension createTransportForStartCandidateHarvest(
            String media)
        throws OperationFailedException
    {
        PacketExtension pe = null;

        if (getCallPeer().isJitsiVideobridge())
        {
            MediaType mediaType = MediaType.parseString(media);
            ColibriConferenceIQ.Channel channel
                = getColibriChannel(mediaType, false /* remote */);

            if (channel != null)
                pe = cloneTransportAndCandidates(channel.getTransport());
        }
        else
            pe = createTransport(media);
        return pe;
    }

    /**
     * Initializes a new <tt>PacketExtension</tt> instance appropriate to the
     * type of Jingle transport represented by this <tt>TransportManager</tt>.
     * The new instance is not initialized with any attributes or child
     * extensions.
     *
     * @return a new <tt>PacketExtension</tt> instance appropriate to the type
     * of Jingle transport represented by this <tt>TransportManager</tt>
     */
    protected abstract PacketExtension createTransportPacketExtension();

    /**
     * Creates a media <tt>StreamConnector</tt> for a stream of a specific
     * <tt>MediaType</tt>. The minimum and maximum of the media port boundaries
     * are taken into account.
     *
     * @param mediaType the <tt>MediaType</tt> of the stream for which a
     * <tt>StreamConnector</tt> is to be created
     * @return a <tt>StreamConnector</tt> for the stream of the specified
     * <tt>mediaType</tt>
     * @throws OperationFailedException if the binding of the sockets fails
     */
    protected StreamConnector doCreateStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        return super.createStreamConnector(mediaType);
    }

    /**
     * Finds a <tt>TransportManagerJabberImpl</tt> participating in a telephony
     * conference utilizing the Jitsi Videobridge server-side technology that
     * this instance is participating in which is establishing the connectivity
     * with the Jitsi Videobridge server (as opposed to a <tt>CallPeer</tt>).
     *
     * @return a <tt>TransportManagerJabberImpl</tt> which is participating in
     * a telephony conference utilizing the Jitsi Videobridge server-side
     * technology that this instance is participating in which is establishing
     * the connectivity with the Jitsi Videobridge server (as opposed to a
     * <tt>CallPeer</tt>).
     */
    TransportManagerJabberImpl
        findTransportManagerEstablishingConnectivityWithJitsiVideobridge()
    {
        Call call = getCallPeer().getCall();
        TransportManagerJabberImpl transportManager = null;

        if (call != null)
        {
            CallConference conference = call.getConference();

            if ((conference != null) && conference.isJitsiVideobridge())
            {
                for (Call aCall : conference.getCalls())
                {
                    Iterator<? extends CallPeer> callPeerIter
                        = aCall.getCallPeers();

                    while (callPeerIter.hasNext())
                    {
                        CallPeer aCallPeer = callPeerIter.next();

                        if (aCallPeer instanceof CallPeerJabberImpl)
                        {
                            TransportManagerJabberImpl aTransportManager
                                = ((CallPeerJabberImpl) aCallPeer)
                                    .getMediaHandler()
                                        .getTransportManager();

                            if (aTransportManager
                                    .isEstablishingConnectivityWithJitsiVideobridge)
                            {
                                transportManager = aTransportManager;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return transportManager;
    }

    /**
     * Gets the {@link ColibriConferenceIQ.Channel} which belongs to a content
     * associated with a specific <tt>MediaType</tt> and is to be either locally
     * or remotely used.
     * <p>
     * <b>Note</b>: Modifications to the <tt>ColibriConferenceIQ.Channel</tt>
     * instance returned by the method propagate to (the state of) this
     * instance.
     * </p>
     *
     * @param mediaType the <tt>MediaType</tt> associated with the content which
     * contains the <tt>ColibriConferenceIQ.Channel</tt> to get
     * @param local <tt>true</tt> if the <tt>ColibriConferenceIQ.Channel</tt>
     * which is to be used locally is to be returned or <tt>false</tt> for the
     * one which is to be used remotely
     * @return the <tt>ColibriConferenceIQ.Channel</tt> which belongs to a
     * content associated with the specified <tt>mediaType</tt> and which is to
     * be used in accord with the specified <tt>local</tt> indicator if such a
     * channel exists; otherwise, <tt>null</tt>
     */
    ColibriConferenceIQ.Channel getColibriChannel(
            MediaType mediaType,
            boolean local)
    {
        ColibriConferenceIQ.Channel channel = null;

        if (colibri != null)
        {
            ColibriConferenceIQ.Content content
                = colibri.getContent(mediaType.toString());

            if (content != null)
            {
                List<ColibriConferenceIQ.Channel> channels
                    = content.getChannels();

                if (channels.size() == 2)
                    channel = channels.get(local ? 0 : 1);
            }
        }
        return channel;
    }
}
