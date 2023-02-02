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

import org.jitsi.xmpp.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.JingleUtils;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.utils.*;
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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TransportManagerJabberImpl.class);

    /**
     * The ID that we will be assigning to our next candidate. We use
     * <tt>int</tt>s for interoperability reasons (Emil: I believe that GTalk
     * uses <tt>int</tt>s. If that turns out not to be the case we can stop
     * using <tt>int</tt>s here if that's an issue).
     */
    private static int nextID = 1;

    /**
     * The generation of the candidates we are currently generating
     */
    private int currentGeneration = 0;

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
     * @return a <tt>ExtensionElement</tt> to be added as a child to
     * <tt>ourContent</tt>; otherwise, <tt>null</tt>
     * @throws OperationFailedException if anything goes wrong while starting
     * transport candidate harvest for the specified <tt>ourContent</tt>
     */
    protected abstract ExtensionElement startCandidateHarvest(
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
        List<ContentPacketExtension> cpes = (theirOffer == null) ? ourAnswer : theirOffer;

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
                ExtensionElement pe
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
        throws OperationFailedException
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
     * {@inheritDoc}
     *
     * @see #doCreateStreamConnector(MediaType)
     */
    @Override
    protected StreamConnector createStreamConnector(final MediaType mediaType)
        throws OperationFailedException
    {
        return doCreateStreamConnector(mediaType);
    }

    protected abstract ExtensionElement createTransport(String media)
        throws OperationFailedException;

    protected ExtensionElement createTransportForStartCandidateHarvest(String media)
        throws OperationFailedException
    {
        return createTransport(media);
    }

    /**
     * Initializes a new <tt>ExtensionElement</tt> instance appropriate to the
     * type of Jingle transport represented by this <tt>TransportManager</tt>.
     * The new instance is not initialized with any attributes or child
     * extensions.
     *
     * @return a new <tt>ExtensionElement</tt> instance appropriate to the type
     * of Jingle transport represented by this <tt>TransportManager</tt>
     */
    protected abstract ExtensionElement createTransportPacketExtension();

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
     * Sets the flag which indicates whether to use rtcpmux or not.
     */
    public abstract void setRtcpmux(boolean rtcpmux);
}
