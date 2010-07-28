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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * <tt>TransportManager</tt>s gather local candidates for incoming and outgoing
 * calls. Their work starts by calling a start method which, using the remote
 * peer's session description, would start the harvest. Calling a second wrapup
 * method would deliver the candidate harvest, possibly after blocking if
 * it has not yet completed.
 *
 * @author Emil Ivov
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
    public abstract void startCandidateHarvest(
                          List<ContentPacketExtension> theirOffer,
                          List<ContentPacketExtension> ourAnswer)
        throws OperationFailedException;

    /**
     * Starts transport candidate harvest. This method should complete rapidly
     * and, in case of lengthy procedures like STUN/TURN/UPnP candidate harvests
     * are necessary, they should be executed in a separate thread. Candidate
     * harvest would then need to be concluded in the {@link #wrapupHarvest()}
     * method which would be called once we absolutely need the candidates.
     *
     * @param ourOffer the content descriptions that we should be adding our
     * transport lists to (although not necessarily in this very instance).
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public abstract void startCandidateHarvest(
                                    List<ContentPacketExtension>   ourOffer)
        throws OperationFailedException;

    /**
     * Notifies the transport manager that it should conclude candidate
     * harvesting as soon as possible an return the lists of candidates
     * gathered so far.
     *
     * @return the content list that we received earlier (possibly cloned into
     * a new instance) and that we have updated with transport lists.
     */
    public abstract List<ContentPacketExtension> wrapupHarvest();
}
