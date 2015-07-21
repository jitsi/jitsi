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

/**
 * The event that occurs when an ad-hoc chat room has been created.
 *
 * @author Valentin Martinet
 */
public class AdHocChatRoomCreatedEvent
{
    /**
     * The ad-hoc room that has been created.
     */
    private AdHocChatRoom adHocChatRoom;

    /**
     * The <tt>Contact</tt> who created the ad-hoc room.
     */
    private Contact by;

    /**
     * Initializes an <tt>AdHocChatRoomCreatedEvent</tt> with the creator (<tt>
     * by</tt>) and the ad-hoc room <tt>adHocChatRoom</tt>.
     *
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt>
     * @param by the <tt>Contact</tt> who created this ad-hoc room
     */
    public AdHocChatRoomCreatedEvent(AdHocChatRoom adHocChatRoom, Contact by)
    {
        this.adHocChatRoom = adHocChatRoom;
        this.by = by;
    }

    /**
     * Returns the <tt>Contact</tt> who created the room.
     *
     * @return <tt>Contact</tt>
     */
    public Contact getBy()
    {
        return this.by;
    }

    /**
     * Returns the ad-hoc room concerned by this event.
     *
     * @return <tt>AdHocChatRoom</tt>
     */
    public AdHocChatRoom getAdHocCreatedRoom()
    {
        return this.adHocChatRoom;
    }
}
