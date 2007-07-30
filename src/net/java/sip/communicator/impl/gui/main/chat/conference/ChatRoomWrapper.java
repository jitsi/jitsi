/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ChatRoomWrapper
{
    private ProtocolProviderService parentProvider;
    
    private ChatRoom chatRoom;

    private String chatRoomName;
    
    private String chatRoomID;
    
    /**
     * 
     * @param protocolProvider
     * @param chatRoomID
     * @param chatRoomName
     */
    public ChatRoomWrapper(ProtocolProviderService protocolProvider,
        String chatRoomID, String chatRoomName)
    {
        this.parentProvider = protocolProvider;
        this.chatRoomID = chatRoomID;
        this.chatRoomName = chatRoomName;
    }
    
    public ChatRoomWrapper(ChatRoom chatRoom)
    {   
        this(chatRoom.getParentProvider(),
            chatRoom.getIdentifier(), chatRoom.getName());
        
        this.chatRoom = chatRoom;
    }
    
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom)
    {
        this.chatRoom = chatRoom;
    }
    
    public String getChatRoomName()
    {
        return chatRoomName;
    }

    public void setChatRoomName(String chatRoomName)
    {
        this.chatRoomName = chatRoomName;
    }

    public String getChatRoomID()
    {
        return chatRoomID;
    }

    public void setChatRoomID(String chatRoomID)
    {
        this.chatRoomID = chatRoomID;
    }
    
    public ProtocolProviderService getParentProvider()
    {
        return this.parentProvider;
    }
}
