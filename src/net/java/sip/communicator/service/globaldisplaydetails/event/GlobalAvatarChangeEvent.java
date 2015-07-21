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
package net.java.sip.communicator.service.globaldisplaydetails.event;

import java.util.*;

/**
 * The event that contains information about global avatar change.
 *
 * @author Yana Stamcheva
 */
public class GlobalAvatarChangeEvent
    extends EventObject
{
    /**
     * A default serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The avatar this event is about.
     */
    private byte[] avatar;

    /**
     * Creates an instance of <tt>GlobalDisplayDetailsEvent</tt>
     *
     * @param source the source of this event
     * @param newAvatar the new avatar
     */
    public GlobalAvatarChangeEvent( Object source,
                                    byte[] newAvatar)
    {
        super(source);

        this.avatar = newAvatar;
    }

    /**
     * Returns the new global avatar.
     *
     * @return a byte array representing the new global avatar
     */
    public byte[] getNewAvatar()
    {
        return avatar;
    }
}
