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
package net.java.sip.communicator.service.customcontactactions;

import net.java.sip.communicator.service.protocol.*;

/**
 * A custom contact action menu item, used to define an action that can be 
 * represented in the contact list entry in the user interface.
 *
 * @author Hristo Terezov
 */
public interface ContactActionMenuItem<T>
{
    /**
     * Invoked when an action occurs.
     *
     * @param actionSource the source of the action
     */
    public void actionPerformed(T actionSource)
        throws OperationFailedException;

    /**
     * The icon used by the UI to visualize this action.
     * @return the button icon.
     */
    public byte[] getIcon();

    /**
     * Returns the text of the component to create for this contact
     * action.
     * 
     * @param actionSource the action source for associated with the
     * action.
     * @return the tool tip text of the component to create for this contact
     * action
     */
    public String getText(T actionSource);

    /**
     * Indicates if this action is visible for the given <tt>actionSource</tt>.
     *
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return <tt>true</tt> if the action should be visible for the given
     * <tt>actionSource</tt>, <tt>false</tt> - otherwise
     */
    public boolean isVisible(T actionSource);
    
    /**
     * 
     * @return
     */
    public char getMnemonics();
    
    /**
     * Returns <tt>true</tt> if the item should be enabled and <tt>false</tt>
     *  - not.
     *  
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return <tt>true</tt> if the item should be enabled and <tt>false</tt>
     *  - not.
     */
    public boolean isEnabled(T actionSource);
    
    /**
     * Returns <tt>true</tt> if the item should be a check box and 
     * <tt>false</tt> if not
     * 
     * @return <tt>true</tt> if the item should be a check box and 
     * <tt>false</tt> if not
     */
    public boolean isCheckBox();

    /**
     * Returns the state of the item if the item is check box.
     * 
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return the state of the item.
     */
    public boolean isSelected(T actionSource);
}
