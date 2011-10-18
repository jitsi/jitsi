/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import net.java.sip.communicator.service.protocol.*;

/**
 * The MSN implementation of the <tt>AdHocChatRoomInvitation</tt> interface.
 *
 * @author Rupert Burchardi
 * @author Yana Stamcheva
 */
public class AdHocChatRoomInvitationMsnImpl
    implements AdHocChatRoomInvitation
{
   /**
    * Corresponding chat room instance.
    */
   private AdHocChatRoom chatRoom;
   /**
    * The name of the inviter.
    */
   private String inviter;

   /**
    * Creates an instance of the <tt>AdHocChatRoomInvitationMsnImpl</tt> by
    * specifying the targetChatRoom, the inviter, the reason and the password.
    * 
    * @param targetChatRoom the <tt>AdHocChatRoom</tt> for which the invitation
    * is
    * @param inviter the contact, which sent the invitation
    */
   public AdHocChatRoomInvitationMsnImpl(AdHocChatRoom targetChatRoom,
                                        String inviter)
   {
       this.chatRoom = targetChatRoom;
       this.inviter = inviter;
   }

   /**
    * Returns the corresponding chat room.
    * @return The chat room
    */
   public AdHocChatRoom getTargetAdHocChatRoom()
   {
       return chatRoom;
   }

   /**
    * Returns the corresponding inviter.
    * @return The name of the inviter
    */
   public String getInviter()
   {
       return inviter;
   }

   /**
    * Returns the invitation reason.
    * @return the invitation reason
    */
   public String getReason()
   {
       //Not supported in the Msn protocol.
       return null;
   }
}
