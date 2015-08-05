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
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>NewChatRoom</tt> is meant to be used from the
 * <tt>CreateChatRoomWizard</tt>, to collect information concerning the new chat
 * room.
 *
 * @author Yana Stamcheva
 */
public class NewChatRoom
{
   private ProtocolProviderService protocolProvider;

   private String chatRoomName;

   private List<String> userList;

   private String invitationMessage = "";

   public String getInvitationMessage()
   {
       return invitationMessage;
   }

   public void setInvitationMessage(String invitationMessage)
   {
       this.invitationMessage = invitationMessage;
   }

   public List<String> getUserList()
   {
       if(userList == null || userList.size() < 1)
           return new LinkedList<String>();

       return userList;
   }

   public void setUserList(List<String> userList)
   {
       this.userList = userList;
   }

   public String getChatRoomName()
   {
       return chatRoomName;
   }

   public void setChatRoomName(String chatRoomName)
   {
       this.chatRoomName = chatRoomName;
   }

   public ProtocolProviderService getProtocolProvider()
   {
       return protocolProvider;
   }

   public void setProtocolProvider(ProtocolProviderService protocolProvider)
   {
       this.protocolProvider = protocolProvider;
   }
}
