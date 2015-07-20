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
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ContactListContainer</tt> is a container of a <tt>ContactList</tt>
 * component.
 *
 * @author Yana Stamcheva
 */
public interface ContactListContainer
{
    /**
     * Called when the ENTER key was typed when this container was the focused
     * container. Performs the appropriate actions depending on the current
     * state of the contained contact list.
     */
    public void enterKeyTyped();

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this
     * container was the focused container. Performs the appropriate actions
     * depending on the current state of the contained contact list.
     */
    public void ctrlEnterKeyTyped();

    /**
     * Returns <tt>true</tt> if this contact list container has the focus,
     * otherwise returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if this contact list container has the focus,
     * otherwise returns <tt>false</tt>
     */
    public boolean isFocused();

    /**
     * Returns <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise.
     *
     * @return <tt>true</tt> if there's any currently selected menu related to
     * this <tt>ContactListContainer</tt>, <tt>false</tt> - otherwise
     */
    public boolean isMenuSelected();

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText();

    /**
     * Returns the current search text.
     *
     * @return the current search text
     */
    public String getCurrentSearchText();
}
