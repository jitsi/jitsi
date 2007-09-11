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
public class MultiUserChatServerWrapper
{
    private ProtocolProviderService protocolProvider;

    private ChatRoomWrapper systemRoomWrapper;

    /**
     * Creates an instance of <tt>MultiUserChatServerWrapper</tt> by specifying
     * the protocol provider, corresponding to the multi user chat account.
     * 
     * @param protocolProvider protocol provider, corresponding to the multi
     * user chat account.
     */
    public MultiUserChatServerWrapper(
        ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
        this.systemRoomWrapper
            = new ChatRoomWrapper(
                protocolProvider,
                protocolProvider.getAccountID().getService(),
                protocolProvider.getAccountID().getService());
    }

    /**
     * Returns the system room wrapper corresponding to this server.
     * 
     * @return the system room wrapper corresponding to this server.
     */
    public ChatRoomWrapper getSystemRoomWrapper()
    {
        return systemRoomWrapper;
    }

    /**
     * Sets the system room corresponding to this server.
     * 
     * @param systemRoom the system room to set
     */
    public void setSystemRoom(ChatRoom systemRoom)
    {
        systemRoomWrapper.setChatRoom(systemRoom);
    }

    /**
     * Returns the protocol provider service corresponding to this server
     * wrapper.
     * 
     * @return the protocol provider service corresponding to this server
     * wrapper.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }
}
