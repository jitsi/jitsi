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
package net.java.sip.communicator.service.globalshortcut;

import java.awt.*;

/**
 * Event related to global shortcut.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutEvent
{
    /**
     * Key stroke.
     */
    private final AWTKeyStroke keyStroke;

    /**
     * Shows the event type:
     * pressed is false
     * released is true
     */
    private final boolean isReleased;

    /**
     * Initializes a new <tt>GlobalShortcutEvent</tt>.
     *
     * @param keyStroke keystroke
     */
    public GlobalShortcutEvent(AWTKeyStroke keyStroke)
    {
        this.keyStroke = keyStroke;
        isReleased = false;
    }

    /**
     * Initializes a new <tt>GlobalShortcutEvent</tt>.
     *
     * @param keyStroke keystroke
     * @param isRelease if the event is for release this parameter is true
     * else this parameter is false
     */
    public GlobalShortcutEvent(AWTKeyStroke keyStroke, boolean isReleased)
    {
        this.keyStroke = keyStroke;
        this.isReleased = isReleased;
    }

    /**
     * Returns keyStroke.
     *
     * @return keystroke
     */
    public AWTKeyStroke getKeyStroke()
    {
        return keyStroke;
    }

    /**
     * Returns isReleased.
     *
     * @return release flag of the event
     */
    public boolean isReleased()
    {
        return this.isReleased;
    }
}
