/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.chatroomwizard;

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
