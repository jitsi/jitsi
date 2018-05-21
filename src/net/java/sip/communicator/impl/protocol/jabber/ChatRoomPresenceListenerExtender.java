/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2018 Atlassian Pty Ltd
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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.packet.*;

/**
 * Enables adding extensions to the presence of the {@link ChatRoom} without
 * explicitly adding getters and setters for the values stored in these
 * extensions but instead giving a {@link ChatRoomPresenceListenerExtender}
 * which handles the logic of reading the values and storing them in the
 * {@link ChatRoomMemberJabberImpl} by using for example
 * {@link ChatRoomMemberJabberImpl#setValue(String, Object)}
 */
public interface ChatRoomPresenceListenerExtender
{

    /**
     * Read a {@link Presence} and retrieve the associated
     * {@link ChatRoomMemberJabberImpl} to potentially modify
     *
     * @param presence the presence
     * @param member the member associated with the presence
     */
    void memberPresenceUpdated(Presence presence,
                               ChatRoomMemberJabberImpl member);

}
