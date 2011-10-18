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
 * @author Yana Stamcheva
 */
public interface WhiteboardInvitationRejectionListener
{
    /**
     * Called when an invitee rejects an invitation previously sent by us.
     *
     * @param evt the instance of the <tt>WhiteboardInvitationRejectedEvent</tt>
     * containing the rejected white-board invitation as well as the source
     * provider where this happened.
     */
    public void invitationRejected(WhiteboardInvitationRejectedEvent evt);
}
