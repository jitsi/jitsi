/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * This interface represents an invitation, which is send from a chat room
 * member to another user in order to invite this user to join the chat room.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public interface ChatRoomInvitation
{
    /**
     * Returns the <tt>ChatRoom</tt>, which is the  target of this invitation.
     * The chat room returned by this method will be the room to which the user
     * is invited to join to.
     *
     * @return the <tt>ChatRoom</tt>, which is the  target of this invitation
     */
    public ChatRoom getTargetChatRoom();
    
    /**
     * Returns the password to use when joining the room.
     * 
     * @return the password to use when joining the room
     */
    public byte[] getChatRoomPassword();
    
    /**
     * Returns the <tt>ChatRoomMember</tt> that sent this invitation.
     * 
     * @return the <tt>ChatRoomMember</tt> that sent this invitation.
     */
    public String getInviter();
    
    /**
     * Returns the reason of this invitation, or null if there is no reason.
     *
     * @return the reason of this invitation, or null if there is no reason
     */
    public String getReason();    
}
