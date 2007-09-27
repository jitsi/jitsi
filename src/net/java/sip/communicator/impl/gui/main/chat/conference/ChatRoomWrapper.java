/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatRoomWrapper</tt> is the representation of the <tt>ChatRoom</tt>
 * in the GUI. It stores the information for the chat room even when the
 * corresponding protocol provider is not connected.
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
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the protocol provider,
     * the identifier and the name of the chat room.
     * 
     * @param protocolProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoomID the identifier of the corresponding chat room
     * @param chatRoomName the name of the corresponding chat room
     */
    public ChatRoomWrapper(ProtocolProviderService protocolProvider,
        String chatRoomID, String chatRoomName)
    {
        this.parentProvider = protocolProvider;
        this.chatRoomID = chatRoomID;
        this.chatRoomName = chatRoomName;
    }

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the corresponding chat
     * room.
     * 
     * @param chatRoom the chat room to which this wrapper corresponds.
     */
    public ChatRoomWrapper(ChatRoom chatRoom)
    {
        this(chatRoom.getParentProvider(),
            chatRoom.getIdentifier(), chatRoom.getName());
        
        this.chatRoom = chatRoom;
    }

    /**
     * Returns the <tt>ChatRoom</tt> that this wrapper represents.
     * 
     * @return the <tt>ChatRoom</tt> that this wrapper represents.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    /**
     * Sets the <tt>ChatRoom</tt> that this wrapper represents.
     * 
     * @param chatRoom the chat room
     */
    public void setChatRoom(ChatRoom chatRoom)
    {
        this.chatRoom = chatRoom;
    }

    /**
     * Returns the chat room name.
     * 
     * @return the chat room name
     */
    public String getChatRoomName()
    {
        return chatRoomName;
    }

    /**
     * Sets the chat room name.
     * 
     * @param chatRoomName the name of the chat room
     */
    public void setChatRoomName(String chatRoomName)
    {
        this.chatRoomName = chatRoomName;
    }

    /**
     * Returns the identifier of the chat room.
     * 
     * @return the identifier of the chat room
     */
    public String getChatRoomID()
    {
        return chatRoomID;
    }

    /**
     * Sets the identifier of the chat room.
     * 
     * @param chatRoomID the identifier of the chat room
     */
    public void setChatRoomID(String chatRoomID)
    {
        this.chatRoomID = chatRoomID;
    }

    /**
     * Returns the parent protocol provider.
     * 
     * @return the parent protocol provider
     */
    public ProtocolProviderService getParentProvider()
    {
        return this.parentProvider;
    }
}
