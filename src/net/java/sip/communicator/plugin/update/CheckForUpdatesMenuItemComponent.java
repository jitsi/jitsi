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
package net.java.sip.communicator.plugin.update;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

/**
 * Implements <tt>PluginComponent</tt> for the "Check for Updates" menu
 * item.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class CheckForUpdatesMenuItemComponent
    extends AbstractPluginComponent
{
    /**
     * The "Check for Updates" menu item.
     */
    private JMenuItem checkForUpdatesMenuItem;

    /**
     * Initializes a new "Check for Updates" menu item.
     *
     * @param container the container of the update menu component
     */
    public CheckForUpdatesMenuItemComponent(Container container,
                                            PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);
    }

    /**
     * Gets the UI <tt>Component</tt> of this <tt>PluginComponent</tt>.
     *
     * @return the UI <tt>Component</tt> of this <tt>PluginComponent</tt>
     * @see PluginComponent#getComponent()
     */
    public JMenuItem getComponent()
    {
        if(checkForUpdatesMenuItem == null)
        {
            checkForUpdatesMenuItem
                = new JMenuItem(
                        Resources.getResources().getI18NString(
                                "plugin.updatechecker.UPDATE_MENU_ENTRY"));
            checkForUpdatesMenuItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            UpdateActivator.getUpdateService().checkForUpdates(
                                    true);
                        }
                    });
        }
        return checkForUpdatesMenuItem;
    }

    /**
     * Gets the name of this <tt>PluginComponent</tt>.
     *
     * @return the name of this <tt>PluginComponent</tt>
     * @see PluginComponent#getName()
     */
    public String getName()
    {
        return
            Resources.getResources().getI18NString(
                    "plugin.updatechecker.UPDATE_MENU_ENTRY");
    }
}
