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
package net.java.sip.communicator.plugin.accountinfo;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements <tt>PluginComponent</tt> for the "Account Info" menu item.
 * 
 * @author Marin Dzhigarov
 */
public class AccountInfoMenuItemComponent
    extends AbstractPluginComponent
{
    /**
     * The "Account Info" menu item.
     */
    private JMenuItem accountInfoMenuItem;

    /**
     * Currently set account id if any.
     */
    private AccountID accountID = null;

    /**
     * Initializes a new "Account Info" menu item.
     *
     * @param container the container of the update menu component
     */
    public AccountInfoMenuItemComponent(Container container,
                                        PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);
    }

    public void setCurrentAccountID(AccountID accountID)
    {
        this.accountID = accountID;

        accountInfoMenuItem.setEnabled(
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
        if(accountInfoMenuItem == null)
        {
            accountInfoMenuItem
                = new JMenuItem(
                    Resources.getString("plugin.accountinfo.TITLE"));
            accountInfoMenuItem.setIcon(
                Resources.getImage(
                    "plugin.contactinfo.CONTACT_INFO_ICON"));
            accountInfoMenuItem.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            AccountInfoDialog dialog
                                = new AccountInfoDialog(accountID);

                            dialog.setVisible(true);
                        }
                    });
        }
        return accountInfoMenuItem;
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
            Resources.getString("plugin.accountinfo.TITLE");
    }

    /**
     * Returns the position of this <tt>PluginComponent</tt> within its
     * <tt>Container</tt>
     * 
     * @return Always returns 0. 0 is index of the first section in the "Tools"
     * menu bar in the Contacts list that also contains "Options",
     * "Create a video bridge" etc...
     */
    public int getPositionIndex()
    {
        return 0;
    }

    /**
     * The dialog that appears when "Account Info" menu item is clicked.
     */
    static class AccountInfoDialog
        extends SIPCommDialog
    {
        private AccountInfoPanel accountInfoPanel;

        private AccountInfoDialog(AccountID accountID)
        {
            this.accountInfoPanel = new AccountInfoPanel(this);

            this.setPreferredSize(new java.awt.Dimension(600, 400));
            this.setTitle(Resources.getString("plugin.accountinfo.TITLE"));

            if(accountID != null)
            {
                accountInfoPanel.getAccountsComboBox().setSelectedItem(
                    accountInfoPanel.getAccountsTable().get(accountID));
            }

            this.add(accountInfoPanel);
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

            accountInfoPanel.dispose();
        }

        @Override
        public void setVisible(boolean isVisible)
        {
            if(isVisible)
            {
                accountInfoPanel.setVisible(true);
            }

            super.setVisible(isVisible);
        }
    }
}
