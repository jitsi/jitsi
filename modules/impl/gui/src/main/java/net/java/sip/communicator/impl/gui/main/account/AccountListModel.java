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

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A custom list model that allows us to refresh the content of a single
 * row.
 *
 * @author Yana Stamcheva
 */
public class AccountListModel
    extends DefaultListModel
{
    /**
     * Indicates that the data model content has changed.
     *
     * @param account the account that has changed
     */
    public void contentChanged(Account account)
    {
        int index = this.indexOf(account);
        this.fireContentsChanged(this, index, index);
    }

    /**
     * Adds the given <tt>account</tt> to this model.
     *
     * @param account the <tt>Account</tt> to add
     */
    public void addAccount(final Account account)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            addAccount(account);
                        }
                    });
            return;
        }

        // If this is the first account in our menu.
        if (getSize() == 0)
        {
            addElement(account);
            return;
        }

        boolean isAccountAdded = false;
        Enumeration<?> accounts = elements();

        // If we already have other accounts.
        while (accounts.hasMoreElements())
        {
            Account a = (Account) accounts.nextElement();

            int accountIndex = indexOf(a);

            int protocolCompare
                = account.getProtocolName().compareTo(a.getProtocolName());

            // If the new account protocol name is before the name of the
            // menu we insert the new account before the given menu.
            if (protocolCompare < 0)
            {
                insertElementAt(account, accountIndex);
                isAccountAdded = true;
                break;
            }
            else if (protocolCompare == 0)
            {
                // If we have the same protocol name, we check the account
                // name.
                if (account.getName()
                        .compareTo(a.getName()) < 0)
                {
                    insertElementAt(account, accountIndex);
                    isAccountAdded = true;
                    break;
                }
            }
        }

        if (!isAccountAdded)
            addElement(account);
    }

    /**
     * Removes an account from this model.
     *
     * @param account the <tt>Account</tt> to remove
     */
    public void removeAccount(final Account account)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            removeAccount(account);
                        }
                    });
            return;
        }

        super.removeElement(account);
    }

    /**
     * Returns <tt>true</tt> if an account with the given <tt>accountUID</tt>
     * is contained in the model, otherwise returns <tt>false</tt>.
     *
     * @param accountID the identifier of the account we're looking for
     * @return <tt>true</tt> if an account with the given <tt>accountUID</tt>
     * is contained in the model, otherwise returns <tt>false</tt>
     */
    public Account getAccount(AccountID accountID)
    {
        Enumeration<?> accounts = elements();

        // If we already have other accounts.
        while (accounts.hasMoreElements())
        {
            Account account = (Account) accounts.nextElement();

            if (account.getAccountID().equals(accountID))
                return account;
        }
        return null;
    }
}
