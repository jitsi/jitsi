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
 * Dispatched to notify interested parties that a change in a member role in the
 * source room has occurred. Changes may include member being granted admin
 * permissions, or other permissions.
 *
 * @see ChatRoomMemberRole
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomMemberRoleChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The member that the event relates to.
     */
    private ChatRoomMember sourceMember = null;

    /**
     * The previous role that this member had.
     */
    private ChatRoomMemberRole previousRole = null;

    /**
     * The new role that this member get.
     */
    private ChatRoomMemberRole newRole = null;

    /**
     * Creates a <tt>ChatRoomMemberRoleChangeEvent</tt> representing that
     * a change in member role in the source chat room has occured.
     *
     * @param sourceRoom the <tt>ChatRoom</tt> that produced this event
     * @param sourceMember the <tt>ChatRoomMember</tt> that this event is about
     * @param previousRole the previous role that member had
     * @param newRole the new role that member get
     */
    public ChatRoomMemberRoleChangeEvent(ChatRoom sourceRoom,
                                        ChatRoomMember sourceMember,
                                        ChatRoomMemberRole previousRole,
                                        ChatRoomMemberRole newRole)
    {
        super(sourceRoom);
        this.sourceMember = sourceMember;
        this.previousRole = previousRole;
        this.newRole = newRole;
    }

    /**
     * Returns the new role given to the member that this event is about.
     *
     * @return the new role given to the member that this event is about
     */
    public ChatRoomMemberRole getNewRole()
    {
        return newRole;
    }

    /**
     * Returns the previous role the member that this event is about had.
     *
     * @return the previous role the member that this event is about had
     */
    public ChatRoomMemberRole getPreviousRole()
    {
        return previousRole;
    }

    /**
     * Returns the chat room that produced this event.
     *
     * @return the <tt>ChatRoom</tt> that produced this event
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom)getSource();
    }

    /**
     * Returns the member that this event is about.
     * @return the <tt>ChatRoomMember</tt> that this event is about
     */
    public ChatRoomMember getSourceMember()
    {
        return sourceMember;
    }
}
