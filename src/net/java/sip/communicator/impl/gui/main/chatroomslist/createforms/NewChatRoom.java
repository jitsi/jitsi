/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
