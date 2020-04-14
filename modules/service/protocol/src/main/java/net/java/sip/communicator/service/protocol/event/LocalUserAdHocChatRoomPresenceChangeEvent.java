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
 * Dispatched to notify interested parties that a change in our presence in
 * the source ad-hoc chat room has occurred. Changes may include us being join,
 * left, etc.
 *
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class LocalUserAdHocChatRoomPresenceChangeEvent
    extends EventObject
{
    /**
     * Indicates that this event was triggered as a result of the local
     * participant joining an ad-hoc chat room.
     */
    public static final String LOCAL_USER_JOINED = "LocalUserJoined";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant failed to join an ad-hoc chat room.
     */
    public static final String LOCAL_USER_JOIN_FAILED = "LocalUserJoinFailed";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant leaving an ad-hoc chat room.
     */
    public static final String LOCAL_USER_LEFT = "LocalUserLeft";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant being disconnected from the server brutally, or ping timeout.
     */
    public static final String LOCAL_USER_DROPPED = "LocalUserDropped";

    /**
     * The <tt>AdHocChatRoom</tt> to which the change is related.
     */
    private AdHocChatRoom adHocChatRoom = null;

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
     * Creates an <tt>AdHocChatRoomLocalUserPresenceChangeEvent</tt>
     * representing that a change in local participant presence in the source
     * ad-hoc chat room has occurred.
     *
     * @param source the <tt>OperationSetAdHocMultiUserChat</tt>, which
     * produced this event
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt> that this event is about
     * @param eventType the type of this event.
     * @param reason the reason explaining why this event might have occurred
     */
    public LocalUserAdHocChatRoomPresenceChangeEvent(
                                OperationSetAdHocMultiUserChat  source,
                                AdHocChatRoom                   adHocChatRoom,
                                String                          eventType,
                                String                          reason)
    {
        super(source);

        this.adHocChatRoom = adHocChatRoom;
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the <tt>OperationSetAdHocMultiUserChat</tt>, where this event has
     * occurred.
     *
     * @return the <tt>OperationSetAdHocMultiUserChat</tt>, where this event has
     * occurred
     */
    public OperationSetAdHocMultiUserChat getAdHocMultiUserChatOpSet()
    {
        return (OperationSetAdHocMultiUserChat) getSource();
    }

    /**
     * Returns the <tt>AdHocChatRoom</tt>, that this event is about.
     *
     * @return the <tt>AdHocChatRoom</tt>, that this event is about
     */
    public AdHocChatRoom getAdHocChatRoom()
    {
        return this.adHocChatRoom;
    }

    /**
     * A reason string indicating a human readable reason for this event.
     *
     * @return a human readable String containing the reason for this event,
     * or null if no particular reason was specified
     */
    public String getReason()
    {
        return this.reason;
    }

    /**
     * Returns the type of this event which could be one of the LOCAL_USER_XXX
     * member fields.
     *
     * @return one of the LOCAL_USER_XXX fields indicating the type of this
     * event.
     */
    public String getEventType()
    {
        return this.eventType;
    }

    /**
     * Returns a String representation of this event.
     *
     * @return a <tt>String</tt> for representing this event.
     */
    @Override
    public String toString()
    {
        return "AdHocChatRoomLocalUserPresenceChangeEvent[type="
            + getEventType()
            + " \nsourceAdHocRoom="
            + getAdHocChatRoom().toString()
            + "]";
    }
}
