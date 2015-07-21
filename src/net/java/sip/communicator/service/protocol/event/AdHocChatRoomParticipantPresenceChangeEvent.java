/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Dispatched to notify interested parties that a change in the presence of an
 * ad-hoc chat room participant has occurred. Changes may include the
 * participant being join, left...
 *
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class AdHocChatRoomParticipantPresenceChangeEvent
    extends EventObject
{
    /**
     * Indicates that this event was triggered as a result of the participant
     * joining the source ad-hoc chat room.
     */
    public static final String CONTACT_JOINED = "ContactJoined";

    /**
     * Indicates that this event was triggered as a result of the participant
     * leaving the source ad-hoc chat room.
     */
    public static final String CONTACT_LEFT = "ContactLeft";

    /**
     * Indicates that this event was triggered as a result of the participant
     * being disconnected from the server brutally, or due to a ping timeout.
     */
    public static final String CONTACT_QUIT = "ContactQuit";

    /**
     * The well-known reason for a
     * <code>AdHocChatRoomParticipantPresenceChangeEvent</code> to occur as part
     * of an operation which lists all users in an <code>AdHocChatRoom</code>.
     */
    public static final String REASON_USER_LIST = "ReasonUserList";

    /**
     * The ad-hoc chat room participant that the event relates to.
     */
    private final Contact sourceParticipant;

    /**
     * The type of this event. Values can be any of the CONTACT_XXX fields.
     */
    private final String eventType;

    /**
     * An optional String indicating a possible reason as to why the event
     * might have occurred.
     */
    private final String reason;

    /**
     * Creates an <tt>AdHocChatRoomParticipantPresenceChangeEvent</tt>
     * representing that a change in the presence of an <tt>Contact</tt>
     * has occurred. Changes may include the participant being join, left, etc.
     *
     * @param sourceAdHocRoom the <tt>AdHocChatRoom</tt> that produced this
     * event
     * @param sourceParticipant the <tt>Contact</tt> that this event is about
     * @param eventType the event type; one of the CONTACT_XXX constants
     * @param reason the reason explaining why this event might have occurred
     */
    public AdHocChatRoomParticipantPresenceChangeEvent(
                                            AdHocChatRoom sourceAdHocRoom,
                                            Contact sourceParticipant,
                                            String eventType,
                                            String reason )
    {
        super(sourceAdHocRoom);
        this.sourceParticipant = sourceParticipant;
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the ad-hoc chat room that produced this event.
     *
     * @return the <tt>AdHocChatRoom</tt> that produced this event
     */
    public AdHocChatRoom getAdHocChatRoom()
    {
        return (AdHocChatRoom)getSource();
    }

    /**
     * Returns the participant that this event is about.
     *
     * @return the <tt>Contact</tt> that this event is about.
     */
    public Contact getParticipant()
    {
        return this.sourceParticipant;
    }

    /**
     * A reason String indicating a human readable reason for this event.
     *
     * @return a human readable String containing the reason for this event,
     * or null if no particular reason was specified.
     */
    public String getReason()
    {
        return this.reason;
    }

    /**
     * Gets the indicator which determines whether this event has occurred with
     * the well-known reason of listing all users in a <code>ChatRoom</code>.
     *
     * @return <tt>true</tt> if this event has occurred with the well-known
     * reason of listing all users in a <code>ChatRoom</code> i.e.
     * {@link #getReason()} returns a value of {@link #REASON_USER_LIST};
     * otherwise, <tt>false</tt>
     */
    public boolean isReasonUserList()
    {
        return REASON_USER_LIST.equals(getReason());
    }

    /**
     * Returns the type of this event which could be one of the MEMBER_XXX
     * member field values.
     *
     * @return one of the MEMBER_XXX member field values indicating the type
     * of this event.
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * Returns a String representation of this event.
     *
     * @return string representation of this event
     */
    @Override
    public String toString()
    {
        return "AdHocChatRoomParticipantPresenceChangeEvent[type="
            + getEventType()
            + " sourceAdHocRoom="
            + getAdHocChatRoom().toString()
            + " member="
            + getParticipant().toString()
            + "]";
    }
}
