/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified of changes in the presence of a participant
 * in a particular ad-hoc chat room. Changes may include member being join,
 * left, etc.
 * 
 * @author Valentin Martinet
 */
public interface AdHocChatRoomParticipantPresenceListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the presence of a
     * participant in a particular ad-hoc chat room has occurred. Changes may
     * include participant being join, left.
     * 
     * @param evt the <tt>AdHocChatRoomParticipantPresenceChangeEvent</tt>
     * instance containing the source chat room and type, and reason of the
     * presence change
     */
    public void participantPresenceChanged(
            AdHocChatRoomParticipantPresenceChangeEvent evt);
}