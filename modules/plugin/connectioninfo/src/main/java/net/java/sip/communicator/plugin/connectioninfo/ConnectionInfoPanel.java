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

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.plugin.connectioninfo.ConnectionInfoMenuItemComponent.*;

import org.osgi.framework.*;

/**
 * A GUI plug-in for Jitsi that will allow users to set cross
 * protocol account information.
 *
 * @author Adam Goldstein
 * @author Marin Dzhigarov
 */
public class ConnectionInfoPanel
    extends TransparentPanel
    implements ServiceListener,
               RegistrationStateChangeListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The panel that contains the currently active <tt>AccountDetailsPanel</tt>
     */
    private final JPanel centerPanel =
        new TransparentPanel(new BorderLayout(10, 10));

    /**
     * The currently active <tt>AccountDetailsPanel</tt>
     */
    private ConnectionDetailsPanel currentDetailsPanel;

    /**
     * Combo box that is used for switching between accounts.
     */
    private final JComboBox accountsComboBox;

    /**
     * Instances of the <tt>AccountDetailsPanel</tt> are created for every
     * registered <tt>AccountID</tt>. All such pairs are stored in
     * this map.
     */
    private final Map<AccountID, ConnectionDetailsPanel>
        accountsTable =
            new HashMap<AccountID, ConnectionDetailsPanel>();

    /**
     * The parent dialog.
     */
    private ConnectionInfoDialog dialog;

    /**
     * Creates an instance of <tt>AccountInfoPanel</tt> that contains combo box
     * component with active user accounts and <tt>AccountDetailsPanel</tt> to
     * display and edit account information.
     */
    public ConnectionInfoPanel(ConnectionInfoDialog dialog)
    {
        this.dialog = dialog;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        accountsComboBox = new JComboBox();
        accountsComboBox.setOpaque(false);
        accountsComboBox.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    ConnectionDetailsPanel panel =
                        (ConnectionDetailsPanel) e.getItem();
                    panel.setOpaque(false);
                    centerPanel.removeAll();
                    centerPanel.add(panel, BorderLayout.CENTER);
                    centerPanel.revalidate();
                    centerPanel.repaint();
                    currentDetailsPanel = panel;
                }
            }
        });

        init();

        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ComboBoxRenderer renderer = new ComboBoxRenderer();
        accountsComboBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        accountsComboBox.setRenderer(renderer);

        JLabel comboLabel = new JLabel(
            Resources.getString(
                "plugin.accountinfo.SELECT_ACCOUNT"));
        comboLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel comboBoxPanel = new TransparentPanel();
        comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.X_AXIS));
        comboBoxPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)); 

        comboBoxPanel.add(comboLabel);
        comboBoxPanel.add(accountsComboBox);

        add(comboBoxPanel);
        add(centerPanel);
    }

    /**
     * Initialize.
     */
    private void init()
    {
        ConnectionInfoActivator.bundleContext.addServiceListener(this);

        for (ProtocolProviderFactory providerFactory : ConnectionInfoActivator
            .getProtocolProviderFactories().values())
        {
            ArrayList<AccountID> accountsList =
                providerFactory.getRegisteredAccounts();

            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : accountsList)
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider = (ProtocolProviderService)ConnectionInfoActivator
                    .bundleContext.getService(serRef);

                currentDetailsPanel = new ConnectionDetailsPanel(
                    dialog,
                    protocolProvider);

                accountsTable.put(
                    protocolProvider.getAccountID(), currentDetailsPanel);

                accountsComboBox.addItem(currentDetailsPanel);

                protocolProvider.addRegistrationStateChangeListener(this);
            }
        }
    }

    /**
     * Clears all listeners.
     */
    public void dispose()
    {
        ConnectionInfoActivator.bundleContext.removeServiceListener(this);

        for(ConnectionDetailsPanel pan : accountsTable.values())
        {
            pan.getProtocolProvider()
                .removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * A custom renderer to display properly <tt>AccountDetailsPanel</tt>
     * in a combo box.
     */
    private class ComboBoxRenderer extends DefaultListCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        @Override
        public Component getListCellRendererComponent(
            JList list, Object value, int index,
                boolean isSelected, boolean hasFocus)
        {
            JLabel renderer
                = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, hasFocus);

            if (value != null)
            {
                ConnectionDetailsPanel panel = (ConnectionDetailsPanel) value;

                renderer.setText(
                    panel.protocolProvider.getAccountID().getUserID());
                ImageIcon protocolIcon =
                    new ImageIcon(panel.protocolProvider.getProtocolIcon().
                        getIcon((ProtocolIcon.ICON_SIZE_16x16)));
                renderer.setIcon(protocolIcon);
            }

            return renderer;
        }
    }

    public void registrationStateChanged(final RegistrationStateChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    registrationStateChanged(evt);
                }
            });
            return;
        }

        ProtocolProviderService protocolProvider = evt.getProvider();

        if (evt.getNewState() == RegistrationState.REGISTERED)
        {
            if (accountsTable.containsKey(protocolProvider.getAccountID()))
            {
                ConnectionDetailsPanel detailsPanel
                    = accountsTable.get(protocolProvider.getAccountID());
                detailsPanel.loadDetails();
            }
            else
            {
                ConnectionDetailsPanel panel =
                    new ConnectionDetailsPanel(dialog, protocolProvider);
                accountsTable.put(protocolProvider.getAccountID(), panel);
                accountsComboBox.addItem(panel);
            }
        }
        else if (evt.getNewState() == RegistrationState.UNREGISTERING)
        {
            ConnectionDetailsPanel panel
                = accountsTable.get(protocolProvider.getAccountID());
            if (panel != null)
            {
                accountsTable.remove(protocolProvider.getAccountID());
                accountsComboBox.removeItem(panel);
                if (currentDetailsPanel == panel)
                {
                    currentDetailsPanel = null;
                    centerPanel.removeAll();
                    centerPanel.revalidate();
                    centerPanel.repaint();
                }
            }
        }
    }

    /**
     * Handles registration and unregistration of
     * <tt>ProtocolProviderService</tt>
     * 
     * @param event
     */
    @Override
    public void serviceChanged(final ServiceEvent event)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    serviceChanged(event);
                }
            });
            return;
        }

        // Get the service from the event.
        Object service
            = ConnectionInfoActivator.bundleContext.getService(
                event.getServiceReference());

        // We are not interested in any services
        // other than ProtocolProviderService
        if (!(service instanceof ProtocolProviderService))
            return;

        ProtocolProviderService protocolProvider =
            (ProtocolProviderService) service;

        // If a new protocol provider is registered we to add new 
        // AccountDetailsPanel to the combo box containing active accounts.
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            if (accountsTable.get(protocolProvider.getAccountID()) == null)
            {
                ConnectionDetailsPanel panel =
                    new ConnectionDetailsPanel(dialog, protocolProvider);
                accountsTable.put(protocolProvider.getAccountID(), panel);
                accountsComboBox.addItem(panel);
                protocolProvider.addRegistrationStateChangeListener(this);
            }
        }
        // If the protocol provider is being unregistered we have to remove
        // a AccountDetailsPanel from the combo box containing active accounts.
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            ConnectionDetailsPanel panel
                = accountsTable.get(protocolProvider.getAccountID());
            if (panel != null)
            {
                accountsTable.remove(protocolProvider.getAccountID());
                accountsComboBox.removeItem(panel);
                if (currentDetailsPanel == panel)
                {
                    currentDetailsPanel = null;
                    centerPanel.removeAll();
                    centerPanel.revalidate();
                    centerPanel.repaint();
                }
            }
        }
    }

    /**
     * Returns the combo box that switches between account detail panels. 
     *
     * @return The combo box that switches between account detail panels.
     */
    public JComboBox getAccountsComboBox()
    {
        return accountsComboBox;
    }

    /**
     * Returns mapping between registered AccountIDs and their respective
     * AccountDetailsPanel that contains all the details for the account.
     *
     * @return mapping between registered AccountIDs and AccountDetailsPanel.
     */
    public Map<AccountID, ConnectionDetailsPanel> getAccountsTable()
    {
        return accountsTable;
    }
}
