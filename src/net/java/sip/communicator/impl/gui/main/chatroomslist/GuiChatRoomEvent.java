/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Parent class for gui chat room events indicating addition and removal of
 * chat rooms in the gui chat rooms list.
 * 
 * @author Yana Stamcheva
 */
public class GuiChatRoomEvent
    extends EventObject
{
    private int eventID = -1;

    /**
     * Indicates that the MetaContactEvent instance was triggered by
     * adding a MetaContact.
     */
    public static final int CHAT_ROOM_ADDED = 1;

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * removal of an existing MetaContact.
     */
    public static final int CHAT_ROOM_REMOVED = 2;

    private ProtocolProviderService parentProtocolProvider;
    
    /**
     * Creates a new <tt>ChatRoom</tt> event according to the specified parameters.
     * @param source the <tt>ChatRoom</tt> instance that is added to the
     * ChatRoomsList
     * @param protocolProvider the <tt>ProtocolProviderService</tt> underwhich
     * the corresponding <tt>ChatRoom</tt> is located
     * @param eventID one of the CHAT_ROOM_XXX static fields indicating the
     * nature of the event.
     */
    public GuiChatRoomEvent( ChatRoom source,
                       ProtocolProviderService protocolProvider,
                       int eventID)
    {
        super(source);
        this.parentProtocolProvider = protocolProvider;
        this.eventID = eventID;
    }

    /**
     * Returns the source <tt>ChatRoom</tt>.
     * @return the source <tt>ChatRoom</tt>.
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom) getSource();
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> that the <tt>ChatRoom</tt>
     * belongs to.
     * @return the <tt>ProtocolProviderService</tt> that the <tt>ChatRoom</tt>
     * belongs to
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProtocolProvider;
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
        buff.append(getSourceChatRoom().getName());
        buff.append(", eventID=").append(getEventID());
        buff.append(", ProtocolProvider=")
            .append(getProtocolProvider().getProtocolDisplayName());
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
