/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A whiteboard change listener receives events indicating that a whiteboard
 * has changed and a participant has either left or joined.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public interface WhiteboardChangeListener
    extends EventListener
{
    /**
     * Indicates that a new whiteboard participant has joined
     * the source whiteboard.
     *
     * @param evt the <tt>WhiteboardParticipantEvent</tt> containing the source
     * whiteboard and whiteboard participant.
     */
    public void whiteboardParticipantAdded(WhiteboardParticipantEvent evt);

    /**
     * Indicates that a whiteboard participant has left the source whiteboard.
     *
     * @param evt the <tt>WhiteboardParticipantEvent</tt> containing the source
     * whiteboard and whiteboard participant.
     */
    public void whiteboardParticipantRemoved(WhiteboardParticipantEvent evt);

    /**
     * Indicates that a change has occurred in the state of the source
     * whiteboard.
     *
     * @param evt the <tt>WhiteboardChangeEvent</tt> instance containing the
     * source whiteboards and its old and new state.
     */
    public void whiteboardStateChanged(WhiteboardChangeEvent evt);
}
