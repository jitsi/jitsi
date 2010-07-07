/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>AccountList</tt> is the list of currently registered accounts shown
 * in the options form.
 * 
 * @author Yana Stamcheva
 */
public class AccountList
    extends JList
    implements  ProviderPresenceStatusListener,
                RegistrationStateChangeListener,
                ServiceListener,
                MouseListener
{
    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(AccountList.class);

    /**
     * The account list model.
     */
    private final AccountListModel accountListModel = new AccountListModel();

    /**
     * The edit button.
     */
    private final JButton editButton;

    /**
     * Creates an instance of this account list by specifying the parent
     * container of the list.
     * 
     * @param parentConfigPanel the container where this list is added.
     */
    public AccountList(AccountsConfigurationPanel parentConfigPanel)
    {
        this.setModel(accountListModel);
        this.setCellRenderer(new AccountListCellRenderer());

        this.addMouseListener(this);

        this.accountsInit();

        GuiActivator.bundleContext.addServiceListener(this);

        this.editButton = parentConfigPanel.getEditButton();
    }

    /**
     * Initializes the accounts table.
     */
    private void accountsInit()
    {
        AccountManager accountManager = GuiActivator.getAccountManager();

        Iterator<AccountID> storedAccounts
            = accountManager.getStoredAccounts().iterator();

        while (storedAccounts.hasNext())
        {
            AccountID accountID = storedAccounts.next();

            boolean isHidden = accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_PROTOCOL_HIDDEN, false);

            if (isHidden)
                continue;

            Account uiAccount = null;

            if (accountManager.isAccountLoaded(accountID))
            {
                ProtocolProviderService protocolProvider
                    = GuiActivator.getRegisteredProviderForAccount(accountID);

                if (protocolProvider != null)
                {
                    uiAccount = new Account(protocolProvider);

                    protocolProvider.addRegistrationStateChangeListener(this);

                    OperationSetPresence presence
                        = protocolProvider
                            .getOperationSet(OperationSetPresence.class);
    
                    if (presence != null)
                    {
                        presence.addProviderPresenceStatusListener(this);
                    }
                }
            }
            else
                uiAccount = new Account(accountID);

            if (uiAccount != null)
                accountListModel.addAccount(uiAccount);
        }
    }

    /**
     * Returns the selected account.
     * @return the selected account
     */
    public Account getSelectedAccount()
    {
        return (Account) this.getSelectedValue();
    }

    /**
     * Refreshes the account status icon, when the status has changed.
     * @param evt the <tt>ProviderPresenceStatusChangeEvent</tt> that notified
     * us
     */
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        accountListModelContentChanged(evt.getProvider());
    }

    public void providerStatusMessageChanged(PropertyChangeEvent evt) {}

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
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
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

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) sourceService;

        // If the protocol provider is hidden we don't want to show it in the
        // list.
        boolean isHidden
            = (protocolProvider.getAccountID().getAccountProperty
                (ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null);

        if (isHidden)
            return;

        // Add or remove the protocol provider from our accounts list.
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            // Add a presence listener in order to listen for any status
            // changes.
            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            if (presence != null)
            {
                presence.addProviderPresenceStatusListener(this);
            }

            Account account = accountListModel
                .getAccount(protocolProvider.getAccountID());

            if (account != null)
                account.setProtocolProvider(protocolProvider);
            else
                accountListModel.addAccount(new Account(protocolProvider));

            this.repaint();
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            Account account = accountListModel
                .getAccount(protocolProvider.getAccountID());

            // If the unregistered account is a disabled one we don't want to
            // remove it from our list.
            if (account != null && account.isEnabled())
                accountListModel.removeElement(account);
        }
    }

    /**
     * Listens for double mouse click events in order to open the edit form.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() > 1)
        {
            editButton.doClick();
        }
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    /**
     * Dispatches the mouse event to the contained renderer check box.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mousePressed(MouseEvent e)
    {
        dispatchEventToCheckBox(e);
    }

    public void mouseReleased(MouseEvent e) {}

    /**
     * Refreshes the account status icon, when the status has changed.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        accountListModelContentChanged(evt.getProvider());
    }

    /**
     * Notifies <code>accountListModel</code> that the <code>Account</code>s of
     * a specific <code>ProtocolProviderService</code> has changed.
     * 
     * @param protocolProvider
     *            the <code>ProtocolProviderService</code> which had its
     *            <code>Account</code>s changed
     */
    private void accountListModelContentChanged(
        ProtocolProviderService protocolProvider)
    {
        Enumeration<?> accounts = accountListModel.elements();

        while (accounts.hasMoreElements())
        {
            Account account = (Account) accounts.nextElement();

            ProtocolProviderService accountProvider
                = account.getProtocolProvider();

            if (accountProvider == protocolProvider)
                accountListModel.contentChanged(account);
        }
    }

    /**
     * Dispatches the given mouse <tt>event</tt> to the underlying buttons.
     * @param event the <tt>MouseEvent</tt> to dispatch
     */
    private void dispatchEventToCheckBox(MouseEvent event)
    {
        int mouseIndex = this.locationToIndex(event.getPoint());

        if (logger.isTraceEnabled())
            logger.trace("Account list: index under mouse found:" + mouseIndex);

        // If this is an invalid index we have nothing to do here
        if (mouseIndex < 0)
            return;

        Account account = (Account) getModel().getElementAt(mouseIndex);

        if (logger.isTraceEnabled())
            logger.trace("Account list: element at mouse index:" + account);

        AccountListCellRenderer renderer
            = (AccountListCellRenderer) getCellRenderer()
                .getListCellRendererComponent(  this,
                                                account,
                                                mouseIndex,
                                                true,
                                                true);

        if (logger.isTraceEnabled())
            logger.trace("Account list: renderer bounds for mouse index:"
                    + renderer.getBounds());

        // We need to translate coordinates here.
        Rectangle r = this.getCellBounds(mouseIndex, mouseIndex);
        int translatedX = event.getX() - r.x;
        int translatedY = event.getY() - r.y;

        if (logger.isTraceEnabled())
            logger.trace("Account list: find component at:"
                    + translatedX + ", " + translatedY);

        if (renderer.isOverCheckBox(translatedX, translatedY))
        {
            JCheckBox checkBox = account.getEnableCheckBox();

            if (logger.isTraceEnabled())
                logger.trace("Account list: checkBox set selected"
                        + !checkBox.isSelected());

            checkBox.setSelected(!checkBox.isSelected());
            enableAccount(account, checkBox.isSelected());

            this.repaint();
        }
    }

    /**
     * Enables or disables the current account.
     * @param account the account to disable/enable
     * @param isEnable indicates if the account should be enabled or disabled
     */
    private void enableAccount(Account account, boolean isEnable)
    {
        account.setEnabled(isEnable);

        AccountID accountID = account.getAccountID();

        if (isEnable)
            GuiActivator.getAccountManager().loadAccount(accountID);
        else
            GuiActivator.getAccountManager().unloadAccount(accountID);
    }

    /**
     * Ensures that the account with the given <tt>accountID</tt> is removed
     * from the list.
     * @param accountID the identifier of the account
     */
    public void ensureAccountRemoved(AccountID accountID)
    {
        Account account = accountListModel.getAccount(accountID);

        if (account != null)
            accountListModel.removeElement(account);
    }
}
