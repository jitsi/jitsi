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
 * the source whiteboard has occured. Changes may include us being joined,
 * left, etc.
 *
 * @author Yana Stamcheva
 */
public class WhiteboardSessionPresenceChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that this event was triggered as a result of the local
     * participant joining a whiteboard.
     */
    public static final String LOCAL_USER_JOINED = "LocalUserJoined";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant failed to join a whiteboard.
     */
    public static final String LOCAL_USER_JOIN_FAILED = "LocalUserJoinFailed";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant leaving a whiteboard.
     */
    public static final String LOCAL_USER_LEFT = "LocalUserLeft";

   /**
    * Indicates that this event was triggered as a result of the local
    * participant being kicked from a whiteboard.
    */
    public static final String LOCAL_USER_KICKED = "LocalUserKicked";

    /**
     * Indicates that this event was triggered as a result of the local
     * participant beeing disconnected from the server brutally, or ping timeout.
     */
    public static final String LOCAL_USER_DROPPED = "LocalUserDropped";

    /**
     * The <tt>WhiteboardSession</tt> to which the change is related.
     */
    private WhiteboardSession whiteboardSession = null;

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
     * Creates a <tt>WhiteboardSessionPresenceChangeEvent</tt> representing that
     * a change in local participant presence in the source white-board has
     * occured.
     *
     * @param source the <tt>OperationSetWhiteboarding</tt>, which produced this
     * event
     * @param session the <tt>WhiteboardSession</tt> that this event is about
     * @param eventType the type of this event.
     * @param reason the reason explaining why this event might have occurred
     */
    public WhiteboardSessionPresenceChangeEvent(OperationSetWhiteboarding source,
                                                WhiteboardSession session,
                                                String eventType,
                                                String reason)
    {
        super(source);

        this.whiteboardSession = session;
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the <tt>OperationSetWhiteboarding</tt>, where this event has
     * occurred.
     *
     * @return the <tt>OperationSetWhiteboarding</tt>, where this event has
     * occurred
     */
    public OperationSetWhiteboarding getWhiteboardOpSet()
    {
        return (OperationSetWhiteboarding) getSource();
    }

    /**
     * Returns the <tt>WhiteboardSession</tt>, that this event is about.
     *
     * @return the <tt>WhiteboardSession</tt>, that this event is about
     */
    public WhiteboardSession getWhiteboardSession()
    {
        return this.whiteboardSession;
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
     *
     * @return String representation of this event
     */
    @Override
    public String toString()
    {
        return "WhiteboardSessionPresenceChangeEvent[type="
            + getEventType()
            + " whiteboard="
            + getWhiteboardSession()
            + "]";
    }
}
