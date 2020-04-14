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
 * A listener that will be notified of changes in the presence of a participant
 * in a particular ad-hoc chat room. Changes may include member being join,
 * left, etc.
 *
 * @author Valentin Martinet
 */
public interface AdHocChatRoomParticipantPresenceListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the presence of a
     * participant in a particular ad-hoc chat room has occurred. Changes may
     * include participant being join, left.
     *
     * @param evt the <tt>AdHocChatRoomParticipantPresenceChangeEvent</tt>
     * instance containing the source chat room and type, and reason of the
     * presence change
     */
    public void participantPresenceChanged(
            AdHocChatRoomParticipantPresenceChangeEvent evt);
}
