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

import net.java.sip.communicator.service.protocol.*;

/**
 * Instances of this class represent a change avatar of a protocol
 *
 * @author Damien Roth
 */
public class AvatarEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The new avatar
     */
    private byte[] newAvatar;

    /**
     * The provider that has generated the event.
     */
    private ProtocolProviderService sourceProvider;

    /**
     * Creates an event instance indicating that the specified protocol
     * has changed its avatar to <tt>newAvatar</tt>.
     *
     * @param sourceOp the operation set that generated this event
     * @param sourceProvider the protocol provider that the contact belongs to
     * @param newAvatar the new avatar
     */
    public AvatarEvent(OperationSetAvatar sourceOp,
            ProtocolProviderService sourceProvider, byte[] newAvatar)
    {
        super(sourceOp);
        this.sourceProvider = sourceProvider;
        this.newAvatar = newAvatar;
    }

    /**
     * Returns the provider that the source belongs to.
     *
     * @return the provider that the source belongs to.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return this.sourceProvider;
    }

    /**
     * Returns the new avatar
     * @return the new avatar
     */
    public byte[] getNewAvatar()
    {
        return this.newAvatar;
    }

    /**
     * Returns the <tt>OperationSetAvatar</tt> instance that is the source
     * of this event.
     *
     * @return the <tt>OperationSetAvatar</tt> instance that is the source
     * of this event.
     */
    public OperationSetAvatar getSourceAvatarOperationSet()
    {
        return (OperationSetAvatar) getSource();
    }

    /**
     * Returns a String representation of this AvatarEvent
     *
     * @return a <tt>String</tt> representation of this <tt>AvatarEvent</tt>.
     */
    @Override
    public String toString()
    {
        return "AvatarEvent-[ Provider=" + getSourceProvider() + "]";
    }
}
