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
package net.java.sip.communicator.service.gui.event;

import net.java.sip.communicator.service.gui.*;

/**
 * Listens to the creation and closing of <tt>Chat</tt>s.
 *
 * @author Damian Johnson
 * @author Lyubomir Marinov
 */
public interface ChatListener
{
    /**
     * Notifies this instance that a <tt>Chat</tt> has been closed.
     *
     * @param chat the <tt>Chat</tt> which has been closed
     */
    public void chatClosed(Chat chat);

    /**
     * Notifies this instance that a new <tt>Chat</tt> has been created.
     *
     * @param chat the new <tt>Chat</tt> which has been created
     */
    public void chatCreated(Chat chat);
}
