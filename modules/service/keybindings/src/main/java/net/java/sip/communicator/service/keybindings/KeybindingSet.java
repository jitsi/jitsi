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
package net.java.sip.communicator.service.keybindings;

import java.util.*;

import javax.swing.*;

/**
 * Wrapper for keybinding sets. Observers are notified when there's a change.
 * @author Damian Johnson
 */
public abstract class KeybindingSet
    extends Observable
{
    /**
     * Provides current keybinding mappings.
     * @return mapping of keystrokes to the string representation of the actions
     *         they perform
     */
    public abstract HashMap <KeyStroke, String> getBindings();

    /**
     * Resets the bindings and notifies the observer's listeners if they've
     * changed.
     * @param newBindings new keybindings to be held
     */
    public abstract void setBindings(Map <KeyStroke, String> newBindings);

    /**
     * Provides the portion of the UI to which the bindings belong.
     * @return binding category
     */
    public abstract Category getCategory();

    /**
     * Keybinding sets available in the Sip Communicator.
     */
    public enum Category
    {
        /**
         * The "chat" category.
         */
        CHAT("keybindings-chat", Persistence.SERIAL_HASH),

        /**
         * The "main" category.
         */
        MAIN("keybindings-main", Persistence.SERIAL_HASH);

        private final String resource;
        private final Persistence persistenceFormat;

        Category(String resource, Persistence format)
        {
            this.resource = resource;
            this.persistenceFormat = format;
        }

        /**
         * Provides the name keybindings are saved and loaded with.
         * @return filename used for keybindings
         */
        public String getResource()
        {
            return this.resource;
        }

        /**
         * Provides the format used to save and load keybinding resources.
         * @return style of persistence used by keybindings
         */
        public Persistence getFormat()
        {
            return this.persistenceFormat;
        }
    }
}
