/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A <tt>ChatRoomJoinedEvent</tt> is triggered every time a
 * {@link net.java.sip.communicator.service.protocol.ChatRoom} has been joined.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomJoinedEvent
    extends EventObject
{    
    /**
     * Creates an instance of <tt>ChatRoomJoinedEvent</tt> by specifying the
     * <tt>ChatRoom</tt> that has been joined.
     * 
     * @param chatRoom the <tt>ChatRoom</tt> that has been joined
     */
    public ChatRoomJoinedEvent(ChatRoom chatRoom)
    {
        super(chatRoom);        
    }
 
    /**
     * Returns the <tt>ChatRoom</tt> that has been joined.
     * @return the <tt>ChatRoom</tt> that has been joined
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom) getSource();
    }
    
    /**
     * Returns a String representation of this ChatRoomJoinedEvent.
     *
     * @return  A a String representation of this ChatRoomJoinedEvent.
     */
    public String toString()
    {
        return "ChatRoomJoinedEvent: chatRoom=" + getSourceChatRoom();
    }
}
