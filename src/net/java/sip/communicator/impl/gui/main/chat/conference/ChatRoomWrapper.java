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
    private final ChatRoomProviderWrapper parentProvider;

    private ChatRoom chatRoom;

    private final String chatRoomName;

    private final String chatRoomID;

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the protocol provider,
     * the identifier and the name of the chat room.
     * 
     * @param parentProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoomID the identifier of the corresponding chat room
     * @param chatRoomName the name of the corresponding chat room
     */
    public ChatRoomWrapper( ChatRoomProviderWrapper parentProvider,
                            String chatRoomID,
                            String chatRoomName)
    {
        this.parentProvider = parentProvider;
        this.chatRoomID = chatRoomID;
        this.chatRoomName = chatRoomName;
    }

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the corresponding chat
     * room.
     * 
     * @param chatRoom the chat room to which this wrapper corresponds.
     */
    public ChatRoomWrapper( ChatRoomProviderWrapper parentProvider,
                            ChatRoom chatRoom)
    {
        this(   parentProvider,
                chatRoom.getIdentifier(),
                chatRoom.getName());

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
     * Returns the identifier of the chat room.
     * 
     * @return the identifier of the chat room
     */
    public String getChatRoomID()
    {
        return chatRoomID;
    }

    /**
     * Returns the parent protocol provider.
     * 
     * @return the parent protocol provider
     */
    public ChatRoomProviderWrapper getParentProvider()
    {
        return this.parentProvider;
    }

    /**
     * Returns <code>true</code> if the chat room inside is persistent,
     * otherwise - returns <code>false</code>.
     * 
     * @return <code>true</code> if the chat room inside is persistent,
     * otherwise - returns <code>false</code>.
     */
    public boolean isPersistent()
    {
        if (chatRoom != null)
            return chatRoom.isPersistent();
        else
            return true;
    }
}
