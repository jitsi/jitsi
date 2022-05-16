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
 * The event that contains information about global display details change.
 *
 * @author Yana Stamcheva
 */
public class GlobalDisplayNameChangeEvent
    extends EventObject
{
    /**
     * A default serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The display name this event is about.
     */
    private String displayName;

    /**
     * Creates an instance of <tt>GlobalDisplayDetailsEvent</tt>
     *
     * @param source the source of this event
     * @param newDisplayName the new display name
     */
    public GlobalDisplayNameChangeEvent(   Object source,
                                        String newDisplayName)
    {
        super(source);

        this.displayName = newDisplayName;
    }

    /**
     * Returns the new global display name.
     *
     * @return a string representing the new global display name
     */
    public String getNewDisplayName()
    {
        return displayName;
    }
}
