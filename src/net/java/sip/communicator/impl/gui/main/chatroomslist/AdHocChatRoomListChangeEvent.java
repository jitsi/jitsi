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
 * Parent class for gui ad-hoc chat room events indicating addition and removal 
 * of ad-hoc chat rooms in the gui ad-hoc chat rooms list.
 * 
 * @author Valentin Martinet
 */
public class AdHocChatRoomListChangeEvent
    extends EventObject
{
    private int eventID = -1;

    /**
     * Indicates that the AdHocChatRoomListChangeEvent instance was triggered by
     * adding a AdHocChatRoom in the gui.
     */
    public static final int AD_HOC_CHAT_ROOM_ADDED = 1;

    /**
     * Indicates that the AdHocChatRoomListChangeEvent instance was triggered by
     * removing a AdHocChatRoom from the gui.
     */
    public static final int AD_HOC_CHAT_ROOM_REMOVED = 2;

    /**
     * Indicates that the AdHocChatRoomListChangeEvent instance was triggered by
     * changing a AdHocChatRoom in the gui (like changing its status, etc.).
     */
    public static final int AD_HOC_CHAT_ROOM_CHANGED = 3;

    /**
     * Creates a new <tt>AdHocChatRoom</tt> event according to the specified 
     * parameters.
     * 
     * @param source the <tt>AdHocChatRoom</tt> instance that is added to the
     * AdHocChatRoomsList
     * @param eventID one of the AD_HOC_CHAT_ROOM_XXX static fields indicating
     * the nature of the event.
     */
    public AdHocChatRoomListChangeEvent(AdHocChatRoomWrapper     source,
                                           int                     eventID)
    {
        super(source);
        this.eventID = eventID;
    }

    /**
     * Returns the source <tt>AdHocChatRoom</tt>.
     * @return the source <tt>AdHocChatRoom</tt>.
     */
    public AdHocChatRoomWrapper getSourceAdHocChatRoom()
    {
        return (AdHocChatRoomWrapper) getSource();
    }

    /**
     * Returns a String representation of this <tt>GuiAdHocChatRoomEvent</tt>.
     *
     * @return  A String representation of this <tt>GuiAdHocChatRoomEvent</tt>.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("GuiAdHocChatRoomEvent-[ AdHocChatRoomID=");
        buff.append(getSourceAdHocChatRoom().getAdHocChatRoomName());
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
