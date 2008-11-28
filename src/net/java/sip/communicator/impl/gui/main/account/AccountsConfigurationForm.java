/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.swing.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>AccountsConfigurationForm</tt> is the form where the user could
 * create, modify or delete an account.
 *
 * @author Yana Stamcheva
 */
public class AccountsConfigurationForm
    extends TransparentPanel
    implements  ConfigurationForm,
                ActionListener,
                ServiceListener
{
    private Logger logger =
        Logger.getLogger(AccountsConfigurationForm.class.getName());

    private JScrollPane scrollPane = new JScrollPane();

    private TransparentPanel wrapAccountsPanel
        = new TransparentPanel(new BorderLayout());

    private TransparentPanel accountsPanel = new TransparentPanel();

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private I18NString newString = Messages.getI18NString("newAccount");

    private JButton newButton = new JButton(newString.getText());

    private I18NString saveString = Messages.getI18NString("save");

    private JButton saveButton = new JButton(saveString.getText());

    private final Map<ProtocolProviderService, AccountPanel> accounts =
        new Hashtable<ProtocolProviderService, AccountPanel>();

    /**
     * Creates an instance of <tt>AccountsConfigurationForm</tt>.
     *
     * @param mainFrame the main application window
     */
    public AccountsConfigurationForm()
    {
        super(new BorderLayout());

        GuiActivator.bundleContext.addServiceListener(this);

        this.add(scrollPane, BorderLayout.CENTER);
        this.add(buttonsPanel, BorderLayout.SOUTH);

        this.newButton.addActionListener(this);
        this.saveButton.addActionListener(this);

        this.newButton.setMnemonic(newString.getMnemonic());
        this.saveButton.setMnemonic(saveString.getMnemonic());

        this.buttonsPanel.add(newButton);
        this.buttonsPanel.add(saveButton);

        this.scrollPane.getViewport().add(wrapAccountsPanel);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        this.wrapAccountsPanel.add(accountsPanel, BorderLayout.NORTH);

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
                boolean isHidden =
                    (accountID.getAccountProperties().get(
                        ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null);

                if (isHidden)
                    continue;

                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider =
                    (ProtocolProviderService) GuiActivator.bundleContext
                        .getService(serRef);

                AccountPanel accountPanel = new AccountPanel(protocolProvider);

                accountsPanel.add(accountPanel);
                accounts.put(protocolProvider, accountPanel);
            }
        }
    }

    /**
     *
     */
    private class AccountPanel
        extends JPanel
        implements ActionListener
    {
        private JLabel protocolLabel = new JLabel();

        private JLabel accountLabel = new JLabel();

        private JPasswordField passwordField = new JPasswordField();

        private I18NString modifyString = Messages.getI18NString("settings");

        private I18NString removeString = Messages.getI18NString("remove");

        private JButton modifyButton = new JButton(modifyString.getText());

        private JButton removeButton = new JButton(removeString.getText());

        private GridBagConstraints constraints = new GridBagConstraints();

        private Image protocolImage;

        private ProtocolProviderService protocolProvider;

        public AccountPanel(ProtocolProviderService protocolProvider)
        {
            super(new GridBagLayout());

            this.protocolProvider = protocolProvider;

            this.setPreferredSize(new Dimension(400, 80));
            this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                Color.GRAY));

            try
            {
                protocolImage =
                    ImageIO.read(new ByteArrayInputStream(protocolProvider
                        .getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
            catch (IOException e)
            {
                logger.error("Could not read image.", e);
            }
            protocolLabel.setIcon(new ImageIcon(protocolImage));
            constraints.insets = new Insets(0, 5, 0, 5);
            constraints.weightx = 0;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.FIRST_LINE_START;
            this.add(protocolLabel, constraints);

            this.accountLabel.setText(protocolProvider.getAccountID()
                .getUserID());
            constraints.insets = new Insets(0, 0, 0, 0);
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.FIRST_LINE_START;
            constraints.weightx = 1.0;
            this.add(accountLabel, constraints);

            String passwordRequiredProperty = (String) protocolProvider
                .getAccountID().getAccountProperties().get(
                    ProtocolProviderFactory.NO_PASSWORD_REQUIRED);

            boolean isPasswordRequired = true;
            if (passwordRequiredProperty != null
                && passwordRequiredProperty != "")
            {
                isPasswordRequired
                    = !(new Boolean(passwordRequiredProperty).booleanValue());
            }

            if (isPasswordRequired)
            {
                String password =
                    (String) protocolProvider.getAccountID()
                        .getAccountProperties()
                            .get(ProtocolProviderFactory.PASSWORD);

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

            Map accountProperties
                = protocolProvider.getAccountID().getAccountProperties();

            String password = new String(passwordField.getPassword());

            if (!password.equals(
                accountProperties.get(ProtocolProviderFactory.PASSWORD)))
            {
                accountProperties.put(ProtocolProviderFactory.PASSWORD,
                    new String(passwordField.getPassword()));

                providerFactory
                    .modifyAccount(protocolProvider, accountProperties);
            }
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
                        JOptionPane.showConfirmDialog(this, Messages
                            .getI18NString("removeAccountMessage").getText(),
                            Messages.getI18NString("removeAccount").getText(),
                            JOptionPane.YES_NO_CANCEL_OPTION);

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

                wizard.setTitle(Messages.getI18NString(
                    "accountRegistrationWizard").getText());

                wizard.modifyAccount(protocolProvider);
                wizard.showDialog(false);
            }
        }
    }

    /**
     * Returns the title of this configuration form.
     *
     * @return the title of this configuration form.
     */
    public String getTitle()
    {
        return Messages.getI18NString("accounts").getText();
    }

    /**
     * Returns the icon of this configuration form.
     *
     * @return the icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return ImageLoader.getImageInBytes(ImageLoader.ACCOUNT_ICON);
    }

    /**
     * Returns the form of this configuration form.
     *
     * @return the form of this configuration form.
     */
    public Object getForm()
    {
        return this;
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
            NewAccountDialog newAccountDialog = new NewAccountDialog();

            newAccountDialog.pack();
            newAccountDialog.setVisible(true);
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
            this.accountsPanel.remove((JPanel) accounts.get(pps));
            this.accountsPanel.revalidate();
            this.accountsPanel.repaint();
        }
    }

    public int getIndex()
    {
        return 1;
    }
}
