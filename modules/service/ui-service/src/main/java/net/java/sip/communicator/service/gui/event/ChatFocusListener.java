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

import java.util.*;

/**
 * The listener interface for receiving focus events on a <tt>Chat</tt>.
 *
 * @author Yana Stamcheva
 */
public interface ChatFocusListener extends EventListener {

    /**
     * Indicates that a <tt>Chat</tt> has gained the focus.
     *
     * @param event the ChatFocusEvent containing the corresponding chat.
     */
    public void chatFocusGained(ChatFocusEvent event);

    /**
     * Indicates that a <tt>Chat</tt> has lost the focus.
     *
     * @param event the ChatFocusEvent containing the corresponding chat.
     */
    public void chatFocusLost(ChatFocusEvent event);
}
