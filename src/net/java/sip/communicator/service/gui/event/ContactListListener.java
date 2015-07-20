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
package net.java.sip.communicator.service.gui.event;

import java.util.*;

/**
 * Listens for events coming from mouse events over the contact list. For
 * example a contact been clicked or a group been selected.
 *
 * @author Yana Stamcheva
 */
public interface ContactListListener extends EventListener
{
    /**
     * Indicates that a group has been selected.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user selection
     */
    public void groupClicked(ContactListEvent evt);

    /**
     * Indicates that a group has been selected.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user selection
     */
    public void groupSelected(ContactListEvent evt);

    /**
     * Indicates that a contact has been clicked.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user click
     */
    public void contactClicked(ContactListEvent evt);

    /**
     * Indicates that a contact has been selected.
     *
     * @param evt the <tt>ContactListEvent</tt> that has been triggered from
     * the user selection
     */
    public void contactSelected(ContactListEvent evt);
}
