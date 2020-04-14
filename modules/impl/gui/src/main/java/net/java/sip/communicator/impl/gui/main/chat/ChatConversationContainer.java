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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

/**
 * The <tt>ChatConversationContainer</tt> is used as an abstraction of the
 * conversation area, which is included in both the chat window and the history
 * window.
 *
 * @author Yana Stamcheva
 */
public interface ChatConversationContainer
{
    /**
     * Returns the window, where this chat conversation container is contained.
     * (the chat window, the history window, etc)
     *
     * @return the window, where this chat conversation container is contained.
     */
    public Window getConversationContainerWindow();

    /**
     * Sets the given status message to this conversation container.
     *
     * @param statusMessage the status message to set
     */
    public void addTypingNotification(String statusMessage);
}
