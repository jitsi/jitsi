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

import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * The <tt>UIGroup</tt> represents the user interface contact list group.
 *
 * @author Yana Stamcheva
 */
public abstract class UIGroup
{
    /**
     * The preferred height of this group in the contact list.
     */
    private int preferredGroupHeight = -1;

    /**
     * The display details of this group.
     */
    private String displayDetails = "";
    
    /**
     * The maximum number of contacts in the contact source.
     */
    public static int MAX_GROUPS = 10000000;
    
    /**
     * The maximum number of contacts in the group.
     */
    public static int MAX_CONTACTS = 10000;

    /**
     * Returns the descriptor of the group. This would be the underlying object
     * that should provide all other necessary information for the group.
     *
     * @return the descriptor of the group
     */
    public abstract Object getDescriptor();

    /**
     * The display name of the group. The display name is the name to be shown
     * in the contact list group row.
     *
     * @return the display name of the group
     */
    public abstract String getDisplayName();

    /**
     * Returns the display details of this contact. These would be shown
     * whenever the contact is selected. The display details aren't obligatory,
     * so we return an empty string.
     *
     * @return the display details of this contact
     */
    public String getDisplayDetails()
    {
        return displayDetails;
    }

    /**
     * Sets the display details of this group.
     *
     * @return the display details of this group
     */
    public void setDisplayDetails(String displayDetails)
    {
        this.displayDetails = displayDetails;
    }

    /**
     * Returns the index of this group in its source. In other words this is
     * the descriptor index.
     *
     * @return the index of this group in its source
     */
    public abstract int getSourceIndex();

    /**
     * Returns the parent group.
     *
     * @return the parent group
     */
    public abstract UIGroup getParentGroup();

    /**
     * Indicates if the group is collapsed or expanded.
     *
     * @return <tt>true</tt> to indicate that the group is collapsed,
     * <tt>false</tt> to indicate that it's expanded
     */
    public abstract boolean isGroupCollapsed();

    /**
     * Returns the count of online child contacts.
     *
     * @return the count of online child contacts
     */
    public abstract int countOnlineChildContacts();

    /**
     * Returns the child contacts count.
     *
     * @return child contacts count
     */
    public abstract int countChildContacts();

    /**
     * Returns the identifier of this group.
     *
     * @return the identifier of this group
     */
    public abstract String getId();

    /**
     * Returns the right button menu for this group.
     *
     * @return the right button menu component for this group
     */
    public abstract Component getRightButtonMenu();

    /**
     * Returns the preferred height of this group in the contact list.
     *
     * @return the preferred height of this group in the contact list
     */
    public int getPreferredHeight()
    {
        return preferredGroupHeight;
    }

    /**
     * Sets the preferred height of this group in the contact list.
     *
     * @param preferredHeight the preferred height of this group in the contact
     * list
     */
    public void setPreferredHeight(int preferredHeight)
    {
        this.preferredGroupHeight = preferredHeight;
    }

    /**
     * Returns all custom action buttons for this group.
     *
     * @return a list of all custom action buttons for this group
     */
    public Collection<? extends JButton> getCustomActionButtons()
    {
        return null;
    }
}
