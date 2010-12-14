/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

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
        return Integer.toString(nextID++);
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
    public abstract void startCandidateHarvest(
            List<ContentPacketExtension> theirOffer,
            List<ContentPacketExtension> ourAnswer,
            TransportInfoSender transportInfoSender)
        throws OperationFailedException;

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
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public abstract void startCandidateHarvest(
            List<ContentPacketExtension> ourOffer)
        throws OperationFailedException;

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
    protected static ContentPacketExtension findContentByName(
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
                RtpDescriptionPacketExtension rtpDescription
                    = content.getFirstChildOfType(
                            RtpDescriptionPacketExtension.class);

                if (rtpDescription != null)
                {
                    closeStreamConnector(
                        MediaType.parseString(rtpDescription.getMedia()));
                }

                return content;
            }
        }
        return null;
    }

    /**
     * Close this transport manager and release resources.
     */
    public void close()
    {
    }
}
