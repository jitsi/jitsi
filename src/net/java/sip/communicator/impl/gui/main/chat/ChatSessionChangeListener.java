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

/**
 * Listens for changes in {@link ChatSession}.
 * @author George Politis
 */
public interface ChatSessionChangeListener
{
    /**
     * The icon representing the ChatTransport has changed.
     */
    public static final int ICON_UPDATED = 1;

    /**
     * Called when the current {@link ChatTransport} has
     * changed.
     *
     * @param chatSession the {@link ChatSession} it's current
     * {@link ChatTransport} has changed
     */
    public void currentChatTransportChanged(ChatSession chatSession);

    /**
     * When a property of the chatTransport has changed.
     * @param eventID the event id representing the property of the transport
     * that has changed.
     */
    public void currentChatTransportUpdated(int eventID);
}
