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
 * The <tt>Container</tt> wraps a string which is meant to point
 * to a plugin container. The plugin container is a GUI container that contains
 * plugin components.
 *
 * @author Yana Stamcheva
 */
public class Container
{
    public static final String CONTAINER_ID = "CONTAINER_ID";

    /**
     * Main application window "file menu" container.
     */
    public static final Container CONTAINER_FILE_MENU
        = new Container("CONTAINER_FILE_MENU");
    /**
     * Main application window "tools menu" container.
     */
    public static final Container CONTAINER_TOOLS_MENU
        = new Container("CONTAINER_TOOLS_MENU");
    /**
     * Main application window "view menu" container.
     */
    public static final Container CONTAINER_VIEW_MENU
        = new Container("CONTAINER_VIEW_MENU");
    /**
     * Main application window "help menu" container.
     */
    public static final Container CONTAINER_HELP_MENU
        = new Container("CONTAINER_HELP_MENU");
    /**
     * Main application window "settings menu" container.
     */
    public static final Container CONTAINER_SETTINGS_MENU
          = new Container("CONTAINER_SETTINGS_MENU");
    /**
     * Main application window main toolbar container.
     */
    public static final Container CONTAINER_MAIN_TOOL_BAR
        = new Container("CONTAINER_MAIN_TOOL_BAR");

    /**
     * The container added on the south of the account panel above the
     * contact list.
     */
    public static final Container CONTAINER_ACCOUNT_SOUTH
        = new Container("CONTAINER_ACCOUNT_SOUTH");

    /**
     * Main application window main tabbedpane container.
     */
    public static final Container CONTAINER_MAIN_TABBED_PANE
            = new Container("CONTAINER_MAIN_TABBED_PANE");
    /**
     * Chat window toolbar container.
     */
    public static final Container CONTAINER_CHAT_TOOL_BAR
            = new Container("CONTAINER_CHAT_TOOL_BAR");
    /**
     * Main application window "right button menu" over a contact container.
     */
    public static final Container CONTAINER_CONTACT_RIGHT_BUTTON_MENU
            = new Container("CONTAINER_CONTACT_RIGHT_BUTTON_MENU");

    /**
     * Accounts window "right button menu" over an account.
     */
    public static final Container CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU
            = new Container("CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU");

    /**
     * Main application window "right button menu" over a group container.
     */
    public static final Container CONTAINER_GROUP_RIGHT_BUTTON_MENU
            = new Container("CONTAINER_GROUP_RIGHT_BUTTON_MENU");

    /**
     * Chat write panel container.
     */
    public static final Container CONTAINER_CHAT_WRITE_PANEL
            = new Container("CONTAINER_CHAT_WRITE_PANEL");
    /**
     * Chat window "menu bar" container.
     */
    public static final Container CONTAINER_CHAT_MENU_BAR
            = new Container("CONTAINER_CHAT_MENU_BAR");
    /**
     * Chat window "file menu" container.
     */
    public static final Container CONTAINER_CHAT_FILE_MENU
            = new Container("CONTAINER_CHAT_FILE_MENU");
    /**
     * Chat window "edit menu" container.
     */
    public static final Container CONTAINER_CHAT_EDIT_MENU
            = new Container("CONTAINER_CHAT_EDIT_MENU");
    /**
     * Chat window "settings menu" container.
     */
    public static final Container CONTAINER_CHAT_SETTINGS_MENU
            = new Container("CONTAINER_CHAT_SETTINGS_MENU");

    /**
     * Chat window "help menu" container.
     */
    public static final Container CONTAINER_CHAT_HELP_MENU
            = new Container("CONTAINER_CHAT_HELP_MENU");

    /**
     * Chat window container.
     */
    public static final Container CONTAINER_CHAT_WINDOW
            = new Container("CONTAINER_CHAT_WINDOW");

    /**
     * Main window container.
     */
    public static final Container CONTAINER_MAIN_WINDOW
            = new Container("CONTAINER_MAIN_WINDOW");

    /**
     * The contact list panel.
     */
    public static final Container CONTAINER_CONTACT_LIST
            = new Container("CONTAINER_CONTACT_LIST");
    /**
     * Call history panel container.
     */
    public static final Container CONTAINER_CALL_HISTORY
            = new Container("CONTAINER_CALL_HISTORY");

    /**
     * Call dialog container.
     */
    public static final Container CONTAINER_CALL_DIALOG
            = new Container("CONTAINER_CALL_DIALOG");

    /**
     * Call panel container.
     */
    public static final Container CONTAINER_CALL_BUTTONS_PANEL
            = new Container("CONTAINER_CALL_BUTTONS_PANEL");

    /**
     * Status bar container.
     */
    public static final Container CONTAINER_STATUS_BAR
            = new Container("CONTAINER_STATUS_BAR");

    /**
     * Status bar container.
     */
    public static final Container CONTAINER_CHAT_STATUS_BAR
            = new Container("CONTAINER_CHAT_STATUS_BAR");

    /*
     * Constraints
     */
    /**
     * Indicates the most left/top edge of a container.
     */
    public static final String START = "Start";
    /**
     * Indicates the most right/bottom edge of a container.
     */
    public static final String END = "End";
    /**
     * Indicates the top edge of a container.
     */
    public static final String TOP = "Top";
    /**
     * Indicates the bottom edge of a container.
     */
    public static final String BOTTOM = "Bottom";
    /**
     * Indicates the left edge of a container.
     */
    public static final String LEFT = "Left";
    /**
     * Indicates the right edge of a container.
     */
    public static final String RIGHT = "Right";

    /**
     * The name of the container.
     */
    private String containerName;

    /**
     * Creates a <tt>Container</tt> from the given container name.
     *
     * @param containerName the name of the container.
     */
    public Container(String containerName)
    {
        this.containerName = containerName;
    }

    /**
     * Returns the String identifier of this <tt>Container</tt>.
     *
     * @return the String identifier of this <tt>Container</tt>.
     */
    public String getID()
    {
        return this.containerName;
    }

    /**
     * Indicates whether some other object is "equal to" this one which in terms
     * of containers translates to having equal identifiers. If the given object
     * is a String we'll compare it directly to the identifier of our container.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this container has the same id as that of
     * the <code>obj</code> argument or if the object argument is the id of this
     * container.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;

        if (obj instanceof Container)
        {
            Container container = (Container) obj;

            return this.getID().equals(container.getID());
        }
        else if (obj instanceof String)
        {
            String containerID = (String) obj;

            return this.getID().equals(containerID);
        }
        else
            return false;
    }
}
