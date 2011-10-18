/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * This interface represents an invitation, which is send from a whiteboard
 * participant to another user in order to invite this user to join the
 * whiteboard.
 *
 * @author Yana Stamcheva
 */
public interface WhiteboardInvitation
{
    /**
     * Returns the <tt>WhiteboardSession</tt>, which is the  target of this
     * invitation.
     * The whiteboard returned by this method will be the one to which the user
     * is invited to join to.
     *
     * @return the <tt>WhiteboardSession</tt>, which is the target of this
     * invitation
     */
    public WhiteboardSession getTargetWhiteboard();

    /**
     * Returns the password to use when joining the whiteboard.
     * 
     * @return the password to use when joining the whiteboard
     */
    public byte[] getWhiteboardPassword();
    
    /**
     * Returns the <tt>WhiteboardParticipant</tt> that sent this invitation.
     * 
     * @return the <tt>WhiteboardParticipant</tt> that sent this invitation.
     */
    public String getInviter();
    
    /**
     * Returns the reason of this invitation, or null if there is no reason
     * specified.
     *
     * @return the reason of this invitation, or null if there is no reason
     * specified
     */
    public String getReason();
}
