/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

/**
 * 
 * @author Yana Stamcheva
 */
public interface ChatRoomListChangeListener
{
    /**
     * Indicates that a change has occurred in the chat room data list.
     */
    public void contentChanged(ChatRoomListChangeEvent evt);
}
