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
public class ChatRoomLocalUserPresenceChangeEvent
    extends EventObject
{
    /**
     * Indicates that this event was triggered as a result of the local
     * participant joining the source chat room.
     */
    public static final String LOCAL_USER_JOINED = "LocalUserJoined";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant leaving the chat room.
     */
    public static final String LOCAL_USER_LEFT = "LocalUserLeft";

   /**
    * Indicates that this event was triggered as a result of the local
    * participant being kicked from the source chat room.
    */
    public static final String LOCAL_USER_KICKED = "LocalUserKicked";
    
    /**
     * Indicates that this event was triggered as a result of the local
     * participant beeing disconnected from the server brutally, or ping timeout.
     */
     public static final String LOCAL_USER_QUIT = "LocalUserQuit";

    /**
     * The type of this event. Values can be any of the LOCAL_USER_XXX fields.
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
     * @param sourceRoom the <tt>ChatRoom</tt> that produced this event
     * @param eventType the type of this event. One of the LOCAL_USER_XXX
     * constants
     * @param reason the reason explaining why this event might have occurred
     */
    public ChatRoomLocalUserPresenceChangeEvent(ChatRoom sourceRoom,
                                                String eventType,
                                                String reason)
    {
        super(sourceRoom);
        
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the <tt>ChatRoom</tt>, where this event has occurred.
     *
     * @return the <tt>ChatRoom</tt>, where this event has occurred
     */
    public ChatRoom getChatRoom()
    {
        return (ChatRoom) getSource();
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
