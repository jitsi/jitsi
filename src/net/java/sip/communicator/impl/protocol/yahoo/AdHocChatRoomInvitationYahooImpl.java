/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Yahoo implementation of the <tt>AdHocChatRoomInvitation</tt> interface.
 *
 * @author Rupert Burchardi
 * @author Valentin Martinet
 */
public class AdHocChatRoomInvitationYahooImpl
    implements AdHocChatRoomInvitation
{
   /**
    * Corresponding chat room instance.
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
    * Creates an instance of the <tt>ChatRoomInvitationMsnImpl</tt> by
    * specifying the targetChatRoom, the inviter, the reason.
    *
    * @param targetChatRoom The <tt>AdHocChatRoom</tt> for which the invitation
    * is
    * @param inviter The <tt>Contact</tt>, which sent the invitation
    * @param reason The Reason for the invitation
    */
   public AdHocChatRoomInvitationYahooImpl( AdHocChatRoom targetChatRoom,
                                       String inviter,
                                       String reason)
   {
       this.chatRoom = targetChatRoom;
       this.inviter = inviter;
       this.reason = reason;
   }

   /**
    * Returns the corresponding chat room.
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
}
