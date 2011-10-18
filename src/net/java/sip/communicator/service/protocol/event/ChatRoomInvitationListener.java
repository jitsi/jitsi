/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that dispatches events notifying that an invitation to join a MUC
 * room is received.
 *
 * @author Emil Ivov
 */
public interface ChatRoomInvitationListener
{
    /**
     * Called when we receive an invitation to join an existing <tt>ChatRoom</tt>.
     * <p>
     * @param evt the <tt>ChatRoomInvitationReceivedEvent</tt> that contains the
     * newly received invitation and its source provider.
     */
    public abstract void invitationReceived(ChatRoomInvitationReceivedEvent evt);

}
