/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
    public ChatRoom getTarget();
    
    /**
     * Returns the reason of this invitation, or null if there is no reason.
     *
     * @return the reason of this invitation, or null if there is no reason
     */
    public String getReason();
    
    /**
     * Returns the subject of this invitation or null if the invitation contains
     * no subject.
     * @return the subject of this invitation or null if the invitation contains
     * no subject.
     */
    public String getSubject();
    
    /**
     * Returns the protocol provider instance where this invitation has originated.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created this
     * invitation
     */
    public ProtocolProviderService getProtocolProvider();
}
