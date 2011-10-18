/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import net.java.sip.communicator.impl.gui.main.chat.conference.*;

/**
 * The <tt>NewChatRoom</tt> is meant to be used from the
 * <tt>JoinChatRoomWizard</tt>, to collect information concerning the chat
 * room to join.
 * 
 * @author Yana Stamcheva
 */
public class NewChatRoom
{
    private ChatRoomProviderWrapper chatRoomProvider;

    private String chatRoomName;

    /**
     * Returns the name of the chat room.
     * 
     * @return the name of the chat room
     */
    public String getChatRoomName()
    {
        return chatRoomName;
    }

    /**
     * Sets the name of the chat room.
     * 
     * @param chatRoomName the name of the chat room
     */
    public void setChatRoomName(String chatRoomName)
    {
        this.chatRoomName = chatRoomName;
    }

    /**
     * Returns the chat room provider corresponding to the chosen account.
     * 
     * @return the chat room provider corresponding to the chosen account
     */
    public ChatRoomProviderWrapper getChatRoomProvider()
    {
        return chatRoomProvider;
    }

    /**
     * Sets the chat room provider corresponding to the chosen account.
     * 
     * @param provider the chat room provider corresponding to
     * the chosen account
     */
    public void setChatRoomProvider(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;
    }
}
