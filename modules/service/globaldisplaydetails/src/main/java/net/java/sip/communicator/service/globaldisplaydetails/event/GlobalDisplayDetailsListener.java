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
 * The listener interface for receiving global display details events. Notifies
 * all interested parties when a change in the global display name or avatar
 * has occurred.
 *
 * @see GlobalDisplayNameChangeEvent
 *
 * @author Yana Stamcheva
 */
public interface GlobalDisplayDetailsListener
    extends EventListener
{
    /**
     * Indicates a change in the global display name.
     *
     * @param event the event containing the new global display name
     */
    public void globalDisplayNameChanged(GlobalDisplayNameChangeEvent event);

    /**
     * Indicates a change in the global avatar.
     *
     * @param event the event containing the new global avatar
     */
    public void globalDisplayAvatarChanged(GlobalAvatarChangeEvent event);
}
