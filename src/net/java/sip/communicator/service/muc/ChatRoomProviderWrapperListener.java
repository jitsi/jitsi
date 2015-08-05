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
package net.java.sip.communicator.service.muc;

/**
 * Listener which registers for provider add/remove changes.
 */
public interface ChatRoomProviderWrapperListener
{
    /**
     * When a provider wrapper is added this method is called to inform
     * listeners.
     * @param provider which was added.
     */
    public void chatRoomProviderWrapperAdded(
        ChatRoomProviderWrapper provider);

    /**
     * When a provider wrapper is removed this method is called to inform
     * listeners.
     * @param provider which was removed.
     */
    public void chatRoomProviderWrapperRemoved(
        ChatRoomProviderWrapper provider);
}
