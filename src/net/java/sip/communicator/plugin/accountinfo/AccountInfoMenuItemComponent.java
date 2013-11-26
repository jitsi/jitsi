/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
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
    JMenuItem accountInfoMenuItem;

    /**
     * The dialog that appears when "Account Info" menu item is clicked.
     */
    static final SIPCommDialog dialog = new SIPCommDialog() {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Presses programmatically the cancel button, when Esc key is pressed.
         * 
         * @param isEscaped indicates if the Esc button was pressed on close
         */
        protected void close(boolean isEscaped)
        {
            this.setVisible(false);
        }
    };

    /**
     * The main panel containing account information.
     */
    static final AccountInfoPanel accountInfoPanel = new AccountInfoPanel();

    /**
     * Initializes a new "Account Info" menu item.
     *
     * @param container the container of the update menu component
     */
    public AccountInfoMenuItemComponent(Container container,
                                        PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);

        AccountInfoActivator.bundleContext.addServiceListener(accountInfoPanel);

        dialog.setPreferredSize(new java.awt.Dimension(600, 400));
        dialog.setTitle(Resources.getString("plugin.accountinfo.TITLE"));
        dialog.add(accountInfoPanel);
    }

    public void setCurrentAccountID(AccountID accountID)
    {
        accountInfoMenuItem.setEnabled(
            accountID != null && accountID.isEnabled());
        accountInfoPanel.getAccountsComboBox().setSelectedItem(
            accountInfoPanel.getAccountsTable().get(accountID));
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
                            dialog.setVisible(true);
                            accountInfoPanel.setVisible(true);
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
}
