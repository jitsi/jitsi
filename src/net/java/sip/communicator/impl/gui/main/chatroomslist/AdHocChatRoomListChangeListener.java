/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

/**
 * Listener that dispatches events coming from the ad-hoc chat room list.
 * 
 * @author Valentin Martinet
 */
public interface AdHocChatRoomListChangeListener
{
    /**
     * Indicates that a change has occurred in the ad-hoc chat room data list.
     */
    public void contentChanged(AdHocChatRoomListChangeEvent evt);
}
