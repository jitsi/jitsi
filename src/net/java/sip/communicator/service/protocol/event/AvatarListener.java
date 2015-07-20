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

/**
 * The listener interface for receiving avatar events. The class that is
 * interested in processing a avatar event implements this interface, and the
 * object created with that class is registered with the avatar operation set,
 * using its <code>addAvatarListener</code> method. When a avatar event occurs,
 * that object's <code>avatarChanged</code> method is invoked.
 *
 * @see AvatarEvent
 *
 * @author Damien Roth
 */
public interface AvatarListener
    extends EventListener
{
    /**
     * Called whenever a new avatar is defined for one of the protocols that we
     * have subscribed for.
     *
     * @param event the event containing the new image
     */
    public void avatarChanged(AvatarEvent event);
}
