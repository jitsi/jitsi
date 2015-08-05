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
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;

import org.osgi.framework.*;

/**
 * The <tt>AccountList</tt> is the list of currently registered accounts shown
 * in the options form.
 *
 * @author Yana Stamcheva
 */
@SuppressWarnings("serial")
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
     * Property that is changed when an account is enabled/disabled.
     */
    final static String ACCOUNT_STATE_CHANGED = "ACCOUNT_STATE_CHANGED";

    /**
     * The account list model.
     */
    private final AccountListModel accountListModel = new AccountListModel();

    /**
     * The edit button.
     */
    private final JButton editButton;

    /**
     * The menu that appears when right click on account is detected.
     */
    private final AccountRightButtonMenu rightButtonMenu
        = new AccountRightButtonMenu();

    /**
     * Used to prevent from running two enable account threads at the same time.
     * This field is set when new worker is created and cleared once it finishes
     * it's job.
     */
    private EnableAccountWorker enableAccountWorker;

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

        this.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    Point point = e.getPoint();

                    AccountList.this.setSelectedIndex(getRow(point));

                    rightButtonMenu.setAccount(getSelectedAccount());

                    SwingUtilities.convertPointToScreen(
                        point, AccountList.this);

                    ((JPopupMenu) rightButtonMenu).setInvoker(AccountList.this);

                    rightButtonMenu.setLocation(point.x, point.y);
                    rightButtonMenu.setVisible(true);
                }
            }
        });

        this.accountsInit();

        GuiActivator.bundleContext.addServiceListener(this);

        this.editButton = parentConfigPanel.getEditButton();
    }

    private int getRow(Point point)
    {
        return locationToIndex(point);
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

            if (accountID.isHidden()
                || accountID.isConfigHidden())
                continue;

            Account uiAccount = null;

            if (accountManager.isAccountLoaded(accountID))
            {
                ProtocolProviderService protocolProvider
                    = AccountUtils.getRegisteredProviderForAccount(accountID);

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
     *
     * @return the selected account
     */
    public Account getSelectedAccount()
    {
        return (Account) this.getSelectedValue();
    }

    /**
     * Refreshes the account status icon, when the status has changed.
     *
     * @param evt the <tt>ProviderPresenceStatusChangeEvent</tt> that notified
     * us
     */
    public void providerStatusChanged(
                                final ProviderPresenceStatusChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    providerStatusChanged(evt);
                }
            });
            return;
        }

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
        if (protocolProvider.getAccountID().isHidden()
            && protocolProvider.getAccountID().isConfigHidden())
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

            addAccount(protocolProvider);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            removeAccount(protocolProvider);
        }
    }

    /**
     * Adds the account given by the <tt><ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to add
     */
    private void addAccount(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addAccount(protocolProvider);
                }
            });
            return;
        }

        Account account = accountListModel
            .getAccount(protocolProvider.getAccountID());

        if (account != null)
            account.setProtocolProvider(protocolProvider);
        else
            accountListModel.addAccount(new Account(protocolProvider));

        this.repaint();
    }

    /**
     * Removes the account given by the <tt><ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to remove
     */
    private void removeAccount(final ProtocolProviderService protocolProvider)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeAccount(protocolProvider);
                }
            });
            return;
        }

        Account account = accountListModel
            .getAccount(protocolProvider.getAccountID());

        // If the unregistered account is a disabled one we don't want to
        // remove it from our list.
        if (account != null && account.isEnabled())
            accountListModel.removeAccount(account);

        this.repaint();
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
    public void mousePressed(final MouseEvent e)
    {
        dispatchEventToCheckBox(e);
    }

    public void mouseReleased(MouseEvent e) {}

    /**
     * Refreshes the account status icon, when the status has changed.
     */
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
     *
     * @param event the <tt>MouseEvent</tt> to dispatch
     */
    private void dispatchEventToCheckBox(MouseEvent event)
    {
        if(enableAccountWorker != null)
        {
            logger.warn("Enable account worker is already running");
            return;
        }

        int mouseIndex = this.locationToIndex(event.getPoint());

        if (logger.isTraceEnabled())
            logger.trace("Account list: index under mouse found:" + mouseIndex);

        // If this is an invalid index we have nothing to do here
        if (mouseIndex < 0)
            return;

        Account account = (Account) getModel().getElementAt(mouseIndex);

        if (logger.isTraceEnabled())
            logger.trace("Account list: element at mouse index:"
                    + account.getName());

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

            this.enableAccountWorker
                = new EnableAccountWorker(account, checkBox.isSelected());

            enableAccountWorker.start();
        }
    }

    /**
     * Enables or disables the current account.
     *
     * @param account the account to disable/enable
     * @param enable indicates if the account should be enabled or disabled
     */
    private void enableAccount(Account account, boolean enable)
    {
        account.setEnabled(enable);

        AccountManager accountManager = GuiActivator.getAccountManager();
        AccountID accountID = account.getAccountID();

        try
        {
            if (enable)
                accountManager.loadAccount(accountID);
            else
                accountManager.unloadAccount(accountID);

            // fire an event that account is enabled/disabled
            firePropertyChange(ACCOUNT_STATE_CHANGED, !enable, enable);
        }
        catch (OperationFailedException ofex)
        {
            throw new UndeclaredThrowableException(ofex);
        }
    }

    /**
     * Ensures that the account with the given <tt>accountID</tt> is removed
     * from the list.
     *
     * @param accountID the identifier of the account
     */
    public void ensureAccountRemoved(final AccountID accountID)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    ensureAccountRemoved(accountID);
                }
            });
            return;
        }

        Account account = accountListModel.getAccount(accountID);

        if (account != null)
            accountListModel.removeAccount(account);
    }

    /**
     * Enables the account in separate thread.
     */
    private class EnableAccountWorker
        extends SwingWorker
    {
        /**
         * The account to use.
         */
        private Account account;

        /**
         * Enable/disable account.
         */
        private boolean enable;

        EnableAccountWorker(Account account, boolean enable)
        {
            this.account = account;
            this.enable = enable;
        }

        /**
         * Worker thread.
         * @return
         * @throws Exception
         */
        @Override
        protected Object construct()
            throws
            Exception
        {
            enableAccount(account, enable);

            return null;
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        @Override
        protected void finished()
        {
            enableAccountWorker = null;

            AccountList.this.repaint();
        }
    }
}
