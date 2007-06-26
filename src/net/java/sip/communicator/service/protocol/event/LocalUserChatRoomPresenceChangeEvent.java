/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.EventObject;
import net.java.sip.communicator.service.protocol.*;

/**
 * Dispatched to notify interested parties that a change in our presence in
 * the source chat room has occured. Changes may include us being kicked, join,
 * left, etc.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class LocalUserChatRoomPresenceChangeEvent
    extends EventObject
{
    /**
     * Indicates that this event was triggered as a result of the local
     * participant joining a chat room.
     */
    public static final String CHAT_ROOM_JOINED = "ChatRoomJoined";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant leaving a chat room.
     */
    public static final String CHAT_ROOM_LEFT = "ChatRoomLeft";

   /**
    * Indicates that this event was triggered as a result of the local
    * participant being kicked from a chat room.
    */
    public static final String USER_KICKED = "UserKicked";
    
    /**
     * Indicates that this event was triggered as a result of the local
     * participant beeing disconnected from the server brutally, or ping timeout.
     */
    public static final String USER_DROPPED = "UserDropped";

    /**
     * The <tt>ChatRoom</tt> to which the change is related.
     */
    public ChatRoom chatRoom = null;
    
    /**
     * The type of this event.
     */
    private String eventType = null;

    /**
     * An optional String indicating a possible reason as to why the event
     * might have occurred.
     */
    private String reason = null;

    /**
     * Creates a <tt>ChatRoomLocalUserPresenceChangeEvent</tt> representing that
     * a change in local participant presence in the source chat room has
     * occured.
     * 
     * @param source the <tt>OperationSetMultiUserChat</tt>, which produced this
     * event
     * @param chatRoom the <tt>ChatRoom</tt> that this event is about
     * @param eventType the type of this event.
     * @param reason the reason explaining why this event might have occurred
     */
    public LocalUserChatRoomPresenceChangeEvent(OperationSetMultiUserChat source,
                                                ChatRoom chatRoom,
                                                String eventType,
                                                String reason)
    {
        super(source);
        
        this.chatRoom = chatRoom;
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the <tt>OperationSetMultiUserChat</tt>, where this event has
     * occurred.
     *
     * @return the <tt>OperationSetMultiUserChat</tt>, where this event has
     * occurred
     */
    public OperationSetMultiUserChat getMultiUserChatOpSet()
    {
        return (OperationSetMultiUserChat) getSource();
    }
    
    /**
     * Returns the <tt>ChatRoom</tt>, that this event is about.
     *
     * @return the <tt>ChatRoom</tt>, that this event is about
     */
    public ChatRoom getChatRoom()
    {
        return this.chatRoom;
    }

    /**
     * A reason string indicating a human readable reason for this event.
     *
     * @return a human readable String containing the reason for this event,
     * or null if no particular reason was specified
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns the type of this event which could be one of the LOCAL_USER_XXX
     * member fields.
     *
     * @return one of the LOCAL_USER_XXX fields indicating the type of this event.
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * Returns a String representation of this event.
     */
    public String toString()
    {
        return "ChatRoomLocalUserPresenceChangeEvent[type="
            + getEventType()
            + " sourceRoom="
            + getChatRoom()
            + "]";
    }
}
