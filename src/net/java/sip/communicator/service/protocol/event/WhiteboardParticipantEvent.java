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
 * <tt>WhiteboardParticipantEvent</tt>s indicate that a participant in a
 * whiteboard session has either left or entered the session.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardParticipantEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event id value indicating that this event is about the fact that
     * the source whiteboard participant has joined the source whiteboard.
     */
    public static final int WHITEBOARD_PARTICIPANT_ADDED = 1;

    /**
     * An event id value indicating that this event is about the fact that
     * the source whiteboard participant has left the source whiteboard.
     */
    public static final int WHITEBOARD_PARTICIPANT_REMOVED = 2;

    /**
     * The id indicating the type of this event.
     */
    private int eventID = -1;

    /**
     * The whiteboard session participant that this event is about.
     */
    private WhiteboardParticipant sourceWhiteboardParticipant = null;

    /**
     * Creates a whiteboard participant event instance indicating that
     * an event with id <tt>eventID</tt> has happened
     * to <tt>sourceWhiteboardParticipant</tt> in <tt>sourceWhiteboard</tt>
     *
     * @param sourceWhiteboardParticipant the whiteboard participant that this
     * event is about.
     * @param source the whiteboard that the source whiteboard participant is
     * associated with.
     * @param eventID one of the WHITEBOARD_PARTICIPANT_XXX member ints
     * indicating the type of this event.
     */
    public WhiteboardParticipantEvent(
                            WhiteboardSession source,
                            WhiteboardParticipant sourceWhiteboardParticipant,
                            int eventID)
    {
        super(source);
        this.sourceWhiteboardParticipant = sourceWhiteboardParticipant;
        this.eventID = eventID;
    }

    /**
     * Returnst one of the WHITEBOARD_PARTICIPANT_XXX member ints indicating
     * the type of this event.
     * @return one of the WHITEBOARD_PARTICIPANT_XXX member ints indicating
     * the type of this event.
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns the whiteboard session that produced this event.
     *
     * @return a reference to the <tt>WhiteboardSession</tt> that produced this
     * event.
     */
    public WhiteboardSession getSourceWhiteboard()
    {
        return (WhiteboardSession)getSource();
    }

    /**
     * Returns the whiteboard participant that this event is about.
     *
     * @return a reference to the <tt>WhiteboardParticipant</tt> instance that
     * triggered this event.
     */
    public WhiteboardParticipant getSourceWhiteboardParticipant()
    {
        return sourceWhiteboardParticipant;
    }

    /**
     * Returns a String representation of this
     * <tt>WhiteboardParticipantEvent</tt>.
     *
     * @return  a String representation of this
     * <tt>WhiteboardParticipantEvent</tt>.
     */
    @Override
    public String toString()
    {
        return "WhiteboardParticipantEvent: ID=" + getEventID()
               + " source participant=" + getSourceWhiteboardParticipant()
               + " source whiteboard=" + getSourceWhiteboard();
    }

}
