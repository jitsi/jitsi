/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that is fired anytime an invitation to join a MUC room is received.
 *
 * @author Emil Ivov
 */
public interface InvitationListener
{
    /**
     * Called when we receive an invitation to join an existing ChatRoom.
     * <p>
     * @param evt the <tt>InvitationReceivedEvent</tt> that contains the newly
     * received invitation and its source provider.
     */
    public abstract void invitationReceived(InvitationReceivedEvent evt);

}
