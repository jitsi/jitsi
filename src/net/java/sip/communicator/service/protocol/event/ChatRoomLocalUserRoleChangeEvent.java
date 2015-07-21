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
 * Dispatched to notify interested parties that a change in our role in the
 * source chat room has occurred. Changes may include us being granted admin
 * permissions, or other permissions.
 *
 * @see ChatRoomMemberRole
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomLocalUserRoleChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The previous role that local participant had.
     */
    private ChatRoomMemberRole previousRole = null;

    /**
     * The new role that local participant get.
     */
    private ChatRoomMemberRole newRole = null;
    
    /**
     * If <tt>true</tt> this is initial role set.
     */
    private boolean isInitial = false;

    /**
     * Creates a <tt>ChatRoomLocalUserRoleChangeEvent</tt> representing that
     * a change in local participant role in the source chat room has
     * occured.
     *
     * @param sourceRoom the <tt>ChatRoom</tt> that produced the event
     * @param previousRole the previous role that local participant had
     * @param newRole the new role that local participant get
     * @param isInitial if <tt>true</tt> this is initial role set.
     */
    public ChatRoomLocalUserRoleChangeEvent(ChatRoom sourceRoom,
                                        ChatRoomMemberRole previousRole,
                                        ChatRoomMemberRole newRole, 
                                        boolean isInitial)
    {
        super(sourceRoom);
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.isInitial = isInitial;
    }

    /**
     * Returns the new role the local participant get.
     *
     * @return newRole the new role the local participant get
     */
    public ChatRoomMemberRole getNewRole()
    {
        return newRole;
    }

    /**
     * Returns the previous role that local participant had.
     *
     * @return previousRole the previous role that local participant had
     */
    public ChatRoomMemberRole getPreviousRole()
    {
        return previousRole;
    }

    /**
     * Returns the <tt>ChatRoom</tt>, where this event occured.
     *
     * @return the <tt>ChatRoom</tt>, where this event occured
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom)getSource();
    }

    /**
     * Returns <tt>true</tt> if this is initial role set.
     * @return <tt>true</tt> if this is initial role set.
     */
    public boolean isInitial()
    {
        return isInitial;
    }
}
