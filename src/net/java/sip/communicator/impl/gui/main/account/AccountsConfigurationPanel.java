/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>AccountsConfigurationPanel</tt> is the panel containing the accounts
 * list and according buttons shown in the options form.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class AccountsConfigurationPanel
    extends TransparentPanel
    implements ActionListener,
               ListSelectionListener,
               PropertyChangeListener
{
    private final AccountList accountList;

    private final JButton newButton =
        new JButton(GuiActivator.getResources().getI18NString(
            "service.gui.ADD"));

    private final JButton editButton =
        new JButton(GuiActivator.getResources().getI18NString(
            "service.gui.EDIT"));

    private final JButton removeButton =
        new JButton(GuiActivator.getResources().getI18NString(
            "service.gui.DELETE"));

    /**
     * Creates and initializes this account configuration panel.
     */
    public AccountsConfigurationPanel()
    {
        super(new BorderLayout());

        accountList = new AccountList(this);

        /*
         * It seems that we can only delete one account at a time because our
         * confirmation dialog asks for one account.
         */
        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.setPreferredSize(new Dimension(500, 400));

        JScrollPane accountListPane = new JScrollPane();

        accountListPane.getViewport().add(accountList);
        accountListPane.getVerticalScrollBar().setUnitIncrement(30);

        this.add(accountListPane, BorderLayout.CENTER);

        JPanel buttonsPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        newButton.addActionListener(this);
        removeButton.addActionListener(this);

        this.newButton.setMnemonic(GuiActivator.getResources().getI18nMnemonic(
                "service.gui.ADD"));
        this.removeButton
            .setMnemonic(GuiActivator.getResources().getI18nMnemonic(
                "service.gui.DELETE"));

        buttonsPanel.add(newButton);

        if (!ConfigurationManager.isAdvancedAccountConfigDisabled())
        {
            buttonsPanel.add(editButton);
            this.editButton
                .setMnemonic(GuiActivator.getResources().getI18nMnemonic(
                    "service.gui.EDIT"));
            editButton.addActionListener(this);
        }

        buttonsPanel.add(removeButton);

        this.add(buttonsPanel, BorderLayout.SOUTH);

        accountList.addListSelectionListener(this);
        accountList.addPropertyChangeListener(
            AccountList.ACCOUNT_STATE_CHANGED, this);
        updateButtons();
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on on the
     * buttons. Shows the account registration wizard when user clicks on "New".
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object sourceButton = evt.getSource();

        if (sourceButton.equals(newButton))
        {
            NewAccountDialog.showNewAccountDialog();
        }
        else if (sourceButton.equals(removeButton))
        {
            Account account = accountList.getSelectedAccount();

            if (account == null)
                return;

            AccountID accountID = account.getAccountID();

            ProtocolProviderFactory providerFactory =
                GuiActivator.getProtocolProviderFactory(
                    accountID.getProtocolName());

            if (providerFactory != null)
            {
                int result = JOptionPane.showConfirmDialog(
                    this,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.REMOVE_ACCOUNT_MESSAGE"),
                    GuiActivator.getResources().getI18NString(
                        "service.gui.REMOVE_ACCOUNT"),
                    JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION)
                {
                    ConfigurationService configService
                        = GuiActivator.getConfigurationService();
                    String prefix
                        = "net.java.sip.communicator.impl.gui.accounts";
                    List<String> accounts
                        = configService.getPropertyNamesByPrefix(prefix, true);

                    for (String accountRootPropName : accounts)
                    {
                        String accountUID
                            = configService.getString(accountRootPropName);

                        if (accountUID.equals(accountID.getAccountUniqueID()))
                        {
                            configService.setProperty(accountRootPropName, null);
                            break;
                        }
                    }
                    boolean isUninstalled
                        = providerFactory.uninstallAccount(accountID);

                    if (isUninstalled)
                    {
                        accountList.ensureAccountRemoved(accountID);

                        // Notify the corresponding wizard that the account
                        // would be removed.
                        AccountRegWizardContainerImpl wizardContainer
                            = (AccountRegWizardContainerImpl) GuiActivator
                                .getUIService().getAccountRegWizardContainer();

                        AccountRegistrationWizard wizard
                            = wizardContainer.getProtocolWizard(
                                    account.getProtocolProvider());

                        wizard.accountRemoved(account.getProtocolProvider());
                    }
                }
            }
        }
        else if (sourceButton.equals(editButton))
        {
            Account account = accountList.getSelectedAccount();

            if (account == null)
                return;

            AccountRegWizardContainerImpl wizard =
                (AccountRegWizardContainerImpl) GuiActivator.getUIService()
                    .getAccountRegWizardContainer();

            wizard.setTitle(GuiActivator.getResources().getI18NString(
                "service.gui.ACCOUNT_REGISTRATION_WIZARD"));

            wizard.modifyAccount(account.getProtocolProvider());
            wizard.showDialog(false);
        }
    }

    /**
     * Returns the edit button.
     * 
     * @return the edit button
     */
    public JButton getEditButton()
    {
        return editButton;
    }

    /**
     * Updates enabled states of the buttons of this
     * <tt>AccountsConfigurationPanel</tt> to reflect their applicability to the
     * current selection in <tt>accountList</tt>.
     */
    private void updateButtons()
    {
        Account account = accountList.getSelectedAccount();
        boolean enabled = (account != null);

        editButton.setEnabled(enabled && account.isEnabled());
        removeButton.setEnabled(enabled);
    }

    /**
     * Implements ListSelectionListener#valueChanged(ListSelectionEvent).
     * @param e the <tt>ListSelectionEvent</tt> that notified us
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
            updateButtons();
    }

    /**
     * This method gets called when a property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        // update buttons whenever an account changes its state
        updateButtons();
    }
}
