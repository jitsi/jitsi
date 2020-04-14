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
package net.java.sip.communicator.service.customavatar;

/**
 * Service implementers can give a custom way of retrieving
 * avatars for given contact address. ProtocolProviders will use
 * these methods to search for avatar if their contacts are missing
 * picture.
 * @author Damian Minkov
 */
public interface CustomAvatarService
{
    /**
     * Returns the avatar bytes for the given contact address.
     * @param contactAddress the address of a contact to search for its avatar.
     * @return image bytes.
     */
    public byte[] getAvatar(String contactAddress);
}
