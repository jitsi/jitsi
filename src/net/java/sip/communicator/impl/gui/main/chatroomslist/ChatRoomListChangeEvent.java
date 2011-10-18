/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.chat.conference.*;

/**
 * Parent class for gui chat room events indicating addition and removal of
 * chat rooms in the gui chat rooms list.
 * 
 * @author Yana Stamcheva
 */
public class ChatRoomListChangeEvent
    extends EventObject
{
    private int eventID = -1;

    /**
     * Indicates that the ChatRoomListChangeEvent instance was triggered by
     * adding a ChatRoom in the gui.
     */
    public static final int CHAT_ROOM_ADDED = 1;

    /**
     * Indicates that the ChatRoomListChangeEvent instance was triggered by
     * removing a ChatRoom from the gui.
     */
    public static final int CHAT_ROOM_REMOVED = 2;

    /**
     * Indicates that the ChatRoomListChangeEvent instance was triggered by
     * changing a ChatRoom in the gui (like changing its status, etc.).
     */
    public static final int CHAT_ROOM_CHANGED = 3;

    /**
     * Creates a new <tt>ChatRoom</tt> event according to the specified parameters.
     * @param source the <tt>ChatRoom</tt> instance that is added to the
     * ChatRoomsList
     * @param eventID one of the CHAT_ROOM_XXX static fields indicating the
     * nature of the event.
     */
    public ChatRoomListChangeEvent(    ChatRoomWrapper source,
                               int eventID)
    {
        super(source);
        this.eventID = eventID;
    }

    /**
     * Returns the source <tt>ChatRoom</tt>.
     * @return the source <tt>ChatRoom</tt>.
     */
    public ChatRoomWrapper getSourceChatRoom()
    {
        return (ChatRoomWrapper) getSource();
    }

    /**
     * Returns a String representation of this <tt>GuiChatRoomEvent</tt>.
     *
     * @return  A String representation of this <tt>GuiChatRoomEvent</tt>.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("GuiChatRoomEvent-[ ChatRoomID=");
        buff.append(getSourceChatRoom().getChatRoomName());
        buff.append(", eventID=").append(getEventID());
        buff.append(", ProtocolProvider=");

        return buff.toString();
    }

    /**
     * Returns an event id specifying whether the type of this event (e.g.
     * CHAT_ROOM_ADDED or CHAT_ROOM_REMOVED)
     * @return one of the CHAT_ROOM_XXX int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }
}
