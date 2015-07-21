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
 * A listener that will be notified of changes in our presence in the chat
 * room such as us being kicked, join, left.
 *
 * @author Valentin Martinet
 */
public interface LocalUserAdHocChatRoomPresenceListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in our presence in
     * an ad-hoc chat room has occurred. Changes may include us being join,
     * left.
     * @param evt the <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt>
     * instance containing the ad-hoc chat room and the type, and reason of the
     * change
     */
    public void localUserAdHocPresenceChanged(
        LocalUserAdHocChatRoomPresenceChangeEvent evt);
}
