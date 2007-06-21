/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The <tt>ChatRoomJoinListener</tt> is a listener that would be notified every
 * time a <tt>ChatRoom</tt> has been joined. 
 * 
 * @see net.java.sip.communicator.service.protocol.ChatRoom
 * @see net.java.sip.communicator.service.protocol.event.ChatRoomJoinedEvent 
 * 
 * @author Yana Stamcheva
 */
public interface ChatRoomJoinListener
    extends EventListener
{
    /**
     * Indicates that a new
     * {@link net.java.sip.communicator.service.protocol.ChatRoom} has been
     * joined.
     *  
     * @param event the <tt>ChatRoomJoinedEvent</tt> containing the
     * <tt>ChatRoom</tt> that has been joined
     */
    public void chatRoomJoined(ChatRoomJoinedEvent event);
}
