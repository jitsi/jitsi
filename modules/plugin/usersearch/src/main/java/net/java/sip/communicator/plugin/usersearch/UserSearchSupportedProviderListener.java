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
package net.java.sip.communicator.plugin.usersearch;

import net.java.sip.communicator.service.protocol.*;

/**
 * A interface for a listener that will be notified when providers that support
 * user search are added or removed.
 * @author Hristo Terezov
 */
public interface UserSearchSupportedProviderListener
{
    /**
     * Handles provider addition.
     *
     * @param provider the provider that was added.
     */
    public void providerAdded(ProtocolProviderService provider);

    /**
     * Handles provider removal.
     *
     * @param provider the provider that was removed.
     */
    public void providerRemoved(ProtocolProviderService provider);
}
