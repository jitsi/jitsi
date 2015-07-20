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

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * Dispatched to notify interested parties that a <tt>ChatRoomMember</tt> has
 * published a conference description.
 *
 * @author Boris Grozev
 */
public class ChatRoomConferencePublishedEvent
    extends EventObject
{
    /**
     * The <tt>ChatRoom</tt> which is the source of this event.
     */
    private final ChatRoom chatRoom;

    /**
     * The <tt>ChatRoomMember</tt> who published a
     * <tt>ConferenceDescription</tt>
     */
    private final ChatRoomMember member;

    /**
     * The <tt>ConferenceDescription</tt> that was published.
     */
    private final ConferenceDescription conferenceDescription;
    
    /**
     * The type of the event. It can be <tt>CONFERENCE_DESCRIPTION_SENT</tt> or 
     * <tt>CONFERENCE_DESCRIPTION_RECEIVED</tt>.
     */
    private final int eventType;
    
    /**
     * Event type that indicates sending of conference description by the local 
     * user.
     */
    public final static int CONFERENCE_DESCRIPTION_SENT = 0;
    
    /**
     * Event type that indicates receiving conference description.
     */
    public final static int CONFERENCE_DESCRIPTION_RECEIVED = 1;

    /**
     * Creates a new instance.
     * @param chatRoom The <tt>ChatRoom</tt> which is the source of this event.
     * @param member The <tt>ChatRoomMember</tt> who published a
     * <tt>ConferenceDescription</tt>
     * @param conferenceDescription The <tt>ConferenceDescription</tt> that was
     * published.
     */
    public ChatRoomConferencePublishedEvent(
            int eventType,
            ChatRoom chatRoom,
            ChatRoomMember member,
            ConferenceDescription conferenceDescription)
    {
        super(chatRoom);
        
        this.eventType = eventType;
        this.chatRoom = chatRoom;
        this.member = member;
        this.conferenceDescription = conferenceDescription;
    }

    /**
     * Returns the <tt>ChatRoom</tt> which is the source of this event.
     * @return the <tt>ChatRoom</tt> which is the source of this event.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

   /**
    * Returns the <tt>ChatRoomMember</tt> who published a
    * <tt>ConferenceDescription</tt>
    * @return the <tt>ChatRoomMember</tt> who published
    * a <tt>ConferenceDescription</tt>
    */
    public ChatRoomMember getMember()
    {
        return member;
    }

    /**
     * Returns the <tt>ConferenceDescription</tt> that was published.
     * @return the <tt>ConferenceDescription</tt> that was published.
     */
    public ConferenceDescription getConferenceDescription()
    {
        return conferenceDescription;
    }
    
    /**
     * Returns the event type.
     * @return the event type.
     */
    public int getType()
    {
        return eventType;
    }
}
