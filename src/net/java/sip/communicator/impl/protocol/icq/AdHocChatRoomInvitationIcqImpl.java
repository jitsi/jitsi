/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;

/**
 * The ICQ implementation of the <tt>ChatRoomInvitation</tt> interface for 
 * ad-hoc chat rooms.
 * 
 * @author Valentin Martinet
 */
public class AdHocChatRoomInvitationIcqImpl
    implements AdHocChatRoomInvitation
{
   /**
    * Corresponding ad-hoc chat room instance.
    */
   private AdHocChatRoom chatRoom;

   /**
    * The name of the inviter
    */
   private String inviter;

   /**
    * The invitation reason.
    */
   private String reason;

   /**
    * The password. 
    */
   private byte[] password;

   /**
    * Creates an instance of the <tt>AdHocChatRoomInvitationIcqImpl</tt> by
    * specifying the targetChatRoom, the inviter, the reason and the password.
    * 
    * @param targetChatRoom The <tt>AdHocChatRoom</tt> for which the invitation
    * is
    * @param inviter The <tt>Contact</tt>, which sent the invitation
    * @param reason The Reason for the invitation
    * @param password The password
    */
   public AdHocChatRoomInvitationIcqImpl(AdHocChatRoom     targetChatRoom, 
                                            String         inviter,
                                            String         reason, 
                                            byte[]         password)
   {
       this.chatRoom = targetChatRoom;
       this.inviter = inviter;
       this.reason = reason;
       this.password = password;
   }

   /**
    * Returns the corresponding ad-hoc chat room.
    * @return The ad-hoc chat room
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
       return reason;
   }

   /**
    * Returns the password of the chat room.
    * @return The password
    */
   public byte[] getAdHocChatRoomPassword()
   {
       return password;
   }
}
