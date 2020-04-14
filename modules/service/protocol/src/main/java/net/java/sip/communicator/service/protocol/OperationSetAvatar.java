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
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * This interface is an extension of the operation set, meant to be implemented
 * by protocols that support user avatar.
 *
 * @author Damien Roth
 */
public interface OperationSetAvatar
    extends OperationSet
{
    /**
     * Returns the maximum width of the avatar. This method should return 0
     * (zero) if there is no maximum width.
     *
     * @return the maximum width of the avatar
     */
    public int getMaxWidth();

    /**
     * Returns the maximum height of the avatar. This method should return 0
     * (zero) if there is no maximum height.
     *
     * @return the maximum height of the avatar
     */
    public int getMaxHeight();

    /**
     * Returns the maximum size of the avatar. This method should return 0
     * (zero) if there is no maximum size.
     *
     * @return the maximum size of the avatar
     */
    public int getMaxSize();

    /**
     * Defines a new avatar for this protocol
     *
     * @param avatar
     *            the new avatar
     */
    public void setAvatar(byte[] avatar);

    /**
     * Returns the current avatar of this protocol. May return null if the
     * account has no avatar
     *
     * @return avatar's bytes or null if no avatar set
     */
    public byte[] getAvatar();

    /**
     * Registers a listener that would receive events upon avatar changes.
     *
     * @param listener
     *            a AvatarListener that would receive events upon avatar
     *            changes.
     */
    public void addAvatarListener(AvatarListener listener);

    /**
     * Removes the specified group change listener so that it won't receive any
     * further events.
     *
     * @param listener
     *            the AvatarListener to remove
     */
    public void removeAvatarListener(AvatarListener listener);
}
