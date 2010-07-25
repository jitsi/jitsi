/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>TransportManager</tt>s gather local candidates for incoming and outgoing
 * calls. Their work starts by calling a start method which, using the remote
 * peer's session description, would start the harvest. Calling a second wrapup
 * method would deliver the candidate harvest, possibly after blocking if
 * it has not yet completed.
 *
 * @author Emil Ivov
 */
public abstract class TransportManager
{
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
    public abstract void startCandidateHarvest(
                    List<ContentPacketExtension>   theirOffer,
                    List<ContentPacketExtension>   ourAnswer,
                    CallPeerMediaHandlerJabberImpl mediaHandler)
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
     * @param mh this is a temporary hack param that should eventually
     * go away and that we currently use to access the connecotrs.
     *
     * @throws OperationFailedException if we fail to allocate a port number.
     */
    public abstract void startCandidateHarvest(
                                    List<ContentPacketExtension>   ourOffer,
                                    CallPeerMediaHandlerJabberImpl mh)
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
