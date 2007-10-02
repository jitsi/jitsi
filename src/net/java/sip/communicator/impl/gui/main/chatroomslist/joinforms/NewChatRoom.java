/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>NewChatRoom</tt> is meant to be used from the
 * <tt>JoinChatRoomWizard</tt>, to collect information concerning the chat
 * room to join.
 * 
 * @author Yana Stamcheva
 */
public class NewChatRoom
{
    private ProtocolProviderService protocolProvider;

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
     * Returns the protocol provider corresponding to the chosen account.
     * 
     * @return the protocol provider corresponding to the chosen account
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Sets the protocol provider corresponding to the chosen account.
     * 
     * @param protocolProvider the protocol provider corresponding to
     * the chosen account
     */
    public void setProtocolProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }
}
