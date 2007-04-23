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
 * Dispatched to notify interested parties that a change in the status of the
 * source room participant has changed. Changes may include the participant
 * being kicked, banned, or granted admin permissions.
 *
 * @author Emil Ivov
 */
public class ChatRoomMemberEvent
    extends EventObject
{
    /**
     * Indicates that this event was triggered as a result of the source
     * participant joining the source chat room.
     */
    public static final String MEMBER_JOINED = "MemberJoined";

    /**
     * Indicates that this event was triggered as a result of the source
     * participant being "kicked" out of the chat room.
     */
    public static final String MEMBER_LEFT = "MemberJoined";

   /**
    * Indicates that this event was triggered as a result of the source
    * participant leaving the source chat room.
    */
    public static final String MEMBER_KICKED = "MemberKicked";

    /**
     * The member that the event relates to.
     */
    private ChatRoomMember sourceMember = null;

    /**
     * The type of this event. Values can be any of the MEMBER_XXX-ED fields.
     */
    private String eventType = null;

    /**
     * An optional String indicating a possible reason as to why the event
     * might have occurred.
     */
    private String reason = null;


    public ChatRoomMemberEvent(ChatRoom       sourceRoom,
                               ChatRoomMember sourceMember,
                               String         eventType,
                               String         reason )
    {
        super(sourceRoom);
        this.sourceMember = sourceMember;
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the source chat room for this event.
     *
     * @return the <tt>ChatRoom</tt> associated with that is the source of this
     * event and that the corresponding ChatRoomMemberBelongs to.
     */
    public ChatRoom getChatRoom()
    {
        return (ChatRoom)getSource();
    }

    /**
     * Returns the member that this event is pertaining to.
     * @return the <tt>ChatRoomMember</tt> that this event is pertaining to.
     */
    public ChatRoomMember getChatRoomMember()
    {
        return sourceMember;
    }

    /**
     * A reason string indicating a human readable reason for this event.
     *
     * @return a human readable String containing the reason for this event,
     * or null if no particular reason was specified.
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns the type of this event which could be one of the MEMBER_XXX-ed
     * member field values.
     *
     * @return one of the MEMBER_XXXed member field values indicating the type
     * of this event.
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
        return "ChatRoomMemberEvent[type="
            + getEventType()
            + " sourceRoom="
            + getChatRoom()
            + " member="
            + getChatRoomMember()
            + "]";
    }
}
