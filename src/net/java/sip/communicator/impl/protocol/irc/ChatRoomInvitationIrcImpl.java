/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a chat room invitation.
 *
 * @author Stephane Remy
 * @author Yana Stamcheva
 */
public class ChatRoomInvitationIrcImpl
    implements ChatRoomInvitation
{
    /**
     * The chat room on which we are invited.
     */
    private ChatRoom chatRoom = null;
    
    /**
     * The inviter that sent the invitation.
     */
    private String inviter = null;
    
    /** 
     * The reason of the invitation or null if there is no reason.
     */
    private String reason = null;
    
    /**
     * The password of the chat room.
     */
    private byte[] chatRoomPasword;
    
    /**
     * Creates a <tt>ChatRoomInvitationIrcImpl</tt>, by specifying the
     * <tt>chatRoom</tt>, for which the invitation is, the <tt>inviter</tt> who
     * sent this invitation and the <tt>reason</tt> of the invitation.
     * 
     * @param chatRoom the <tt>ChatRoom</tt>, for which the invitation is
     * @param inviter the person who sent this invitation. 
     * @param reason the reason of the invitation
     */
    public ChatRoomInvitationIrcImpl(   ChatRoom chatRoom,
                                        String inviter,
                                        String reason,
                                        byte[] chatRoomPassword)
    {
        this.chatRoom = chatRoom;
        this.inviter = inviter;
        this.reason = reason;
        this.chatRoomPasword = chatRoomPassword;
    }
    
    /**
     * Returns the chat room target of this invitation
     *
     * @return the <tt>ChatRoom</tt> target of this invitation
     */
    public ChatRoom getTargetChatRoom()
    {
        return this.chatRoom;
    }
    
    /**
     * Returns the reason of this invitation, or null if there is no reason
     *
     * @return the reason of this invitation, or null if there is no reason
     */
    public String getReason()
    {
        return this.reason;
    }

    /**
     * Returns the inviter.
     * 
     * @return the inviter
     */
    public String getInviter()
    {
        return inviter;
    }

    /**
     * Returns the password for the chat room.
     * 
     * @return the password for the chat room 
     */
    public byte[] getChatRoomPassword()
    {
        return chatRoomPasword;
    }    
}