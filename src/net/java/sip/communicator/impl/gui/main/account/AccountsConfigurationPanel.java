/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 */
public class AccountsConfigurationPanel
    extends TransparentPanel
    implements  ActionListener,
                ServiceListener,
                ProviderPresenceStatusListener
{
    private static final long serialVersionUID = 1L;

    private final JPanel accountsPanel = new TransparentPanel();

    private final JButton newButton =
        new JButton(GuiActivator.getResources().getI18NString(
            "service.gui.NEW_ACCOUNT"));

    private final JButton saveButton =
        new JButton(GuiActivator.getResources().getI18NString(
            "service.gui.SAVE"));

    private final Map<ProtocolProviderService, AccountPanel> accounts =
        new Hashtable<ProtocolProviderService, AccountPanel>();

    public AccountsConfigurationPanel()
    {
        super(new BorderLayout());

        JPanel buttonsPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        JScrollPane scrollPane = new SCScrollPane();
        JPanel wrapAccountsPanel = new TransparentPanel(new BorderLayout());

        GuiActivator.bundleContext.addServiceListener(this);

        this.add(scrollPane, BorderLayout.CENTER);
        this.add(buttonsPanel, BorderLayout.SOUTH);

        newButton.addActionListener(this);
        saveButton.addActionListener(this);

        this.newButton.setMnemonic(GuiActivator.getResources().getI18nMnemonic(
            "service.gui.NEW_ACCOUNT"));

        this.saveButton.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SAVE"));

        buttonsPanel.add(newButton);
        buttonsPanel.add(saveButton);

        scrollPane.getViewport().add(wrapAccountsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        wrapAccountsPanel.add(accountsPanel, BorderLayout.NORTH);

        this.accountsInit();

        this.setPreferredSize(new Dimension(500, 400));
    }

    /**
     * Initializes the accounts table.
     */
    private void accountsInit()
    {
        this.accountsPanel.setLayout(new BoxLayout(accountsPanel,
            BoxLayout.Y_AXIS));

        for (ProtocolProviderFactory providerFactory : GuiActivator
            .getProtocolProviderFactories().values())
        {
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                boolean isHidden
                    = (accountID.getAccountProperty
                        (ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null);

                if (isHidden)
                    continue;

                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider =
                    (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);

                OperationSetPresence presence
                    = (OperationSetPresence) protocolProvider
                        .getOperationSet(OperationSetPresence.class);

                if (presence != null)
                {
                    presence.addProviderPresenceStatusListener(this);
                }

                AccountPanel accountPanel = new AccountPanel(protocolProvider);

                accountsPanel.add(accountPanel);
                accounts.put(protocolProvider, accountPanel);
            }
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on on the
     * buttons. Shows the account registration wizard when user clicks on "New".
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton sourceButton = (JButton) evt.getSource();

        if (sourceButton.equals(newButton))
        {
            NewAccountDialog.showNewAccountDialog();
        }
        else if (sourceButton.equals(saveButton))
        {
            for (AccountPanel accountPanel : accounts.values())
                accountPanel.save();
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }
        Object sourceService =
            GuiActivator.bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(sourceService instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService pps = (ProtocolProviderService) sourceService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            AccountPanel accountPanel = new AccountPanel(pps);

            this.accountsPanel.add(accountPanel);
            this.accountsPanel.revalidate();
            this.accountsPanel.repaint();

            this.accounts.put(pps, accountPanel);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            this.accountsPanel.remove(accounts.get(pps));
            this.accountsPanel.revalidate();
            this.accountsPanel.repaint();
        }
    }

    /**
     * Refreshes the account status icon, when the status is changed.
     */
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        ProtocolProviderService pps = evt.getProvider();

        AccountPanel accountPanel = accounts.get(pps);

        accountPanel.updateStatus();
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt) {}

    private static class AccountPanel
        extends TransparentPanel
        implements ActionListener
    {
        private static final long serialVersionUID = 1L;

        private JLabel protocolLabel = new JLabel();

        private JLabel accountLabel = new JLabel();

        private JPasswordField passwordField = new JPasswordField();

        private JButton modifyButton =
            new JButton(GuiActivator.getResources().getI18NString(
                "service.gui.MODIFY"));

        private JButton removeButton =
            new JButton(GuiActivator.getResources().getI18NString(
                "service.gui.REMOVE"));

        private GridBagConstraints constraints = new GridBagConstraints();

        private ProtocolProviderService protocolProvider;

        public AccountPanel(ProtocolProviderService protocolProvider)
        {
            super(new GridBagLayout());

            this.protocolProvider = protocolProvider;

            this.setPreferredSize(new Dimension(400, 80));
            this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                Color.GRAY));

            Image protocolImage =
                ImageLoader.getAccountStatusImage(protocolProvider);

            protocolLabel.setIcon(new ImageIcon(protocolImage));
            constraints.insets = new Insets(0, 5, 0, 5);
            constraints.weightx = 0;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.FIRST_LINE_START;
            this.add(protocolLabel, constraints);

            this.accountLabel.setText(protocolProvider.getAccountID()
                .getDisplayName());
            constraints.insets = new Insets(0, 0, 0, 0);
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.FIRST_LINE_START;
            constraints.weightx = 1.0;
            this.add(accountLabel, constraints);

            String passwordRequiredProperty =
                protocolProvider.getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.NO_PASSWORD_REQUIRED);

            boolean isPasswordRequired = true;
            if (passwordRequiredProperty != null
                && passwordRequiredProperty != "")
            {
                isPasswordRequired =
                    !(new Boolean(passwordRequiredProperty).booleanValue());
            }

            if (isPasswordRequired)
            {
                String password =
                    protocolProvider.getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.PASSWORD);

                passwordField.setText(password);
                constraints.gridx = 1;
                constraints.gridy = 1;
                constraints.anchor = GridBagConstraints.FIRST_LINE_START;
                this.add(passwordField, constraints);
            }

            constraints.gridx = 2;
            constraints.gridy = 0;
            constraints.weightx = 0;
            this.add(modifyButton, constraints);
            this.modifyButton.addActionListener(this);

            constraints.gridx = 2;
            constraints.gridy = 1;
            constraints.weightx = 0;
            this.add(removeButton, constraints);
            this.removeButton.addActionListener(this);
        }

        public void save()
        {
            ProtocolProviderFactory providerFactory =
                GuiActivator.getProtocolProviderFactory(protocolProvider);

            Map accountProperties =
                protocolProvider.getAccountID().getAccountProperties();

            String password = new String(passwordField.getPassword());

            if (!password.equals(accountProperties
                .get(ProtocolProviderFactory.PASSWORD)))
            {
                accountProperties.put(ProtocolProviderFactory.PASSWORD,
                    new String(passwordField.getPassword()));

                providerFactory.modifyAccount(protocolProvider,
                    accountProperties);
            }
        }

        public void updateStatus()
        {
            Image protocolImage
                = ImageLoader.getAccountStatusImage(protocolProvider);

            protocolLabel.setIcon(new ImageIcon(protocolImage));
        }

        public void actionPerformed(ActionEvent e)
        {
            JButton sourceButton = (JButton) e.getSource();

            if (sourceButton.equals(removeButton))
            {
                ProtocolProviderFactory providerFactory =
                    GuiActivator.getProtocolProviderFactory(protocolProvider);

                if (providerFactory != null)
                {
                    int result =
                        JOptionPane.showConfirmDialog(this, GuiActivator
                            .getResources().getI18NString(
                                "service.gui.REMOVE_ACCOUNT_MESSAGE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.REMOVE_ACCOUNT"),
                            JOptionPane.YES_NO_OPTION);

                    if (result == JOptionPane.YES_OPTION)
                    {
                        ConfigurationService configService =
                            GuiActivator.getConfigurationService();

                        String prefix =
                            "net.java.sip.communicator.impl.gui.accounts";

                        List<String> accounts =
                            configService
                                .getPropertyNamesByPrefix(prefix, true);

                        for (String accountRootPropName : accounts)
                        {
                            String accountUID =
                                configService.getString(accountRootPropName);

                            if (accountUID.equals(protocolProvider
                                .getAccountID().getAccountUniqueID()))
                            {
                                configService.setProperty(accountRootPropName,
                                    null);
                                break;
                            }
                        }
                        providerFactory.uninstallAccount(protocolProvider
                            .getAccountID());
                    }
                }
            }
            else if (sourceButton.equals(modifyButton))
            {
                AccountRegWizardContainerImpl wizard =
                    (AccountRegWizardContainerImpl) GuiActivator.getUIService()
                        .getAccountRegWizardContainer();

                wizard.setTitle(GuiActivator.getResources().getI18NString(
                    "service.gui.ACCOUNT_REGISTRATION_WIZARD"));

                wizard.modifyAccount(protocolProvider);
                wizard.showDialog(false);
            }
        }
    }
}
