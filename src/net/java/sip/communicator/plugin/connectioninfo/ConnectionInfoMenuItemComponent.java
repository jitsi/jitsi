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
package net.java.sip.communicator.plugin.connectioninfo;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements <tt>PluginComponent</tt> for the "Connection Info" menu item.
 *
 * @author Marin Dzhigarov
 */
public class ConnectionInfoMenuItemComponent
    extends AbstractPluginComponent
{
    /**
     * The "Connection Info" menu item.
     */
    private JMenuItem connectionInfoMenuItem;

    /**
     * Currently set account id if any.
     */
    private AccountID accountID = null;

    /**
     * Initializes a new "Connection Info" menu item.
     *
     * @param container the container of the update menu component
     * @param parentFactory the parent bundle activator
     */
    public ConnectionInfoMenuItemComponent(Container container,
                                        PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);
    }

    @Override
    public void setCurrentAccountID(AccountID accountID)
    {
        this.accountID = accountID;

        connectionInfoMenuItem.setEnabled(
            accountID != null && accountID.isEnabled());
    }

    /**
     * Gets the UI <tt>Component</tt> of this <tt>PluginComponent</tt>.
     *
     * @return the UI <tt>Component</tt> of this <tt>PluginComponent</tt>
     * @see PluginComponent#getComponent()
     */
    public Object getComponent()
    {
        if(connectionInfoMenuItem == null)
        {
            connectionInfoMenuItem
                = new JMenuItem(
                    Resources.getString("plugin.connectioninfo.TITLE"));
            connectionInfoMenuItem.setIcon(
                Resources.getImage(
                    "plugin.contactinfo.CONTACT_INFO_ICON"));
            connectionInfoMenuItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            ConnectionInfoDialog dialog
                                = new ConnectionInfoDialog(accountID);

                            dialog.setVisible(true);
                        }
                    });
        }
        return connectionInfoMenuItem;
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
            Resources.getString("plugin.connectioninfo.TITLE");
    }

    /**
     * Returns the position of this <tt>PluginComponent</tt> within its
     * <tt>Container</tt>
     * 
     * @return Always returns 0. 0 is index of the first section in the "Tools"
     * menu bar in the Contacts list that also contains "Options",
     * "Create a video bridge" etc...
     */
    @Override
    public int getPositionIndex()
    {
        return 0;
    }

    /**
     * The dialog that appears when "Connection Info" menu item is clicked.
     */
    static class ConnectionInfoDialog
        extends SIPCommDialog
    {
        private final ConnectionInfoPanel connectionInfoPanel;

        private ConnectionInfoDialog(AccountID accountID)
        {
            this.connectionInfoPanel = new ConnectionInfoPanel(this);

            this.setPreferredSize(new java.awt.Dimension(600, 400));
            this.setTitle(Resources.getString("plugin.connectioninfo.TITLE"));

            if(accountID != null)
            {
                connectionInfoPanel.getAccountsComboBox().setSelectedItem(
                    connectionInfoPanel.getAccountsTable().get(accountID));
            }

            this.add(connectionInfoPanel);
        }

        /**
         * Presses programmatically the cancel button, when Esc key is pressed.
         *
         * @param isEscaped indicates if the Esc button was pressed on close
         */
        @Override
        protected void close(boolean isEscaped)
        {
            this.setVisible(false);

            connectionInfoPanel.dispose();
        }

        @Override
        public void setVisible(boolean isVisible)
        {
            if(isVisible)
            {
                connectionInfoPanel.setVisible(true);
            }

            super.setVisible(isVisible);
        }
    }
}
