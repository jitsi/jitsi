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

/**
 * A listener that will be notified of changes in the role of a chat
 * participant in a particular chat room. Changes may include participant being
 * granted any of the roles defined in <tt>ChatRoomMemberRole</tt>.
 *
 * @see net.java.sip.communicator.service.protocol.ChatRoomMemberRole
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public interface ChatRoomMemberRoleListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the role of a
     * chat room member has occurred.
     *
     * @param evt the <tt>ChatRoomMemberRoleChangeEvent</tt> instance
     * containing the source chat room and role old and new state.
     */
    public void memberRoleChanged(ChatRoomMemberRoleChangeEvent evt);

}
