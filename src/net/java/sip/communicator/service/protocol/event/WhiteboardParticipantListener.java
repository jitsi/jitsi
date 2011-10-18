/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A <tt>WhiteboardParticipantListener</tt> receives events notifying of changes
 * that have occurred within a <tt>WhiteboardParticipant</tt>. Such changes may
 * pertain to current whiteboard participant state, their display name,
 * address...
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public interface WhiteboardParticipantListener
    extends EventListener
{
    /**
     * Indicates that a change has occurred in the status of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt>
     * instance containing the source event
     * as well as its previous and its new status.
     */
    public void participantStateChanged(WhiteboardParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the display name of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt>
     * instance containing the source event
     * as well as its previous and its new display names.
     */
    public void participantDisplayNameChanged(
            WhiteboardParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the image of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt>
     * instance containing the source event
     * as well as its previous and its new image.
     */
    public void participantImageChanged(WhiteboardParticipantChangeEvent evt);
}
