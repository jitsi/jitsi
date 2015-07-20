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
package net.java.sip.communicator.impl.gui.main.chat.history;

import java.util.*;

/**
 * Manages all history windows within the gui.
 *
 * @author Yana Stamcheva
 */
public class HistoryWindowManager
{
    private final Map<Object, HistoryWindow> contactHistory
        = new Hashtable<Object, HistoryWindow>();

    /**
     * Checks if there's an open history window for the given history contact.
     *
     * @param historyContact the contact to check for
     * @return TRUE if there's an opened history window for the given contact,
     *         FALSE otherwise.
     */
    public boolean containsHistoryWindowForContact(Object historyContact)
    {
        return contactHistory.containsKey(historyContact);
    }

    /**
     * Returns the history window for the given contact.
     *
     * @param historyContact the contact to search for
     * @return the history window for the given contact
     */
    public HistoryWindow getHistoryWindowForContact(Object historyContact)
    {
        return contactHistory.get(historyContact);
    }

    /**
     * Adds a history window for a given contact in the table of opened history
     * windows.
     *
     * @param historyContact the contact to add
     * @param historyWindow the history window to add
     */
    public void addHistoryWindowForContact(Object historyContact,
        HistoryWindow historyWindow)
    {
        contactHistory.put(historyContact, historyWindow);
    }

    /**
     * Removes the history window for the given contact.
     *
     * @param historyContact the contact to remove the history window
     */
    public void removeHistoryWindowForContact(Object historyContact)
    {
        contactHistory.remove(historyContact);
    }
}
