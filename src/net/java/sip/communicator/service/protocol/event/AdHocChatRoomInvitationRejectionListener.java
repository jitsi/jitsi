/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that dispatches events notifying that an invitation which was
 * sent earlier has been rejected by the invitee.
 *
 * @author Valentin Martinet
 */
public interface AdHocChatRoomInvitationRejectionListener
{
    /**
     * Called when an invitee rejects an invitation previously sent by us.
     *
     * @param evt the instance of the <tt>AdHocChatRoomInvitationRejectedEvent
     * </tt>
     * containing the rejected ad-hoc chat room invitation as well as the source
     * provider where this happened.
     */
    public void invitationRejected(AdHocChatRoomInvitationRejectedEvent evt);
}
