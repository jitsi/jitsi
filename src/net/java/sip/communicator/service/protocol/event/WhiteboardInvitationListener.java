/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * A listener that dispatches events notifying that an invitation to join a 
 * white board is received.
 *
 * @author Yana Stamcheva
 */
public interface WhiteboardInvitationListener
{
    /**
     * Called when we receive an invitation to join an existing
     * <tt>WhiteboardSession</tt>.
     * <p>
     * @param evt the <tt>WhiteboardInvitationReceivedEvent</tt> that contains
     * the newly received invitation and its source provider.
     */
    public abstract void invitationReceived(WhiteboardInvitationReceivedEvent evt);

}
