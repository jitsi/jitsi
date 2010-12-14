/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.accountinfo;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * A GUI plug-in for SIP Communicator that will allow users to set cross
 * protocol account information.
 * 
 * @author Adam Goldstein
 */
public class AccountInfoPanel
    extends TransparentPanel
{
    /**
     * The right side of the AccountInfo frame that contains protocol specific
     * account details.
     */
    private AccountDetailsPanel detailsPanel;

    private final Map<ProtocolProviderService, AccountDetailsPanel> accountsTable =
        new Hashtable<ProtocolProviderService, AccountDetailsPanel>();

    /**
     * Constructs a frame with an AccuontInfoAccountPanel to display all
     * registered accounts on the left, and an information interface,
     * AccountDetailsPanel, on the right.
     */
    public AccountInfoPanel()
    {
        super(new BorderLayout());

        JTabbedPane accountsTabbedPane = new SIPCommTabbedPane();

        for (ProtocolProviderFactory providerFactory : AccountInfoActivator
            .getProtocolProviderFactories().values())
        {
            ArrayList<AccountID> accountsList =
                providerFactory.getRegisteredAccounts();

            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : accountsList)
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider = (ProtocolProviderService) AccountInfoActivator
                    .bundleContext.getService(serRef);

                detailsPanel = new AccountDetailsPanel(protocolProvider);

                accountsTable.put(protocolProvider, detailsPanel);

                protocolProvider.addRegistrationStateChangeListener(
                    new RegistrationStateChangeListenerImpl());

                accountsTabbedPane.addTab(
                    accountID.getUserID(), detailsPanel);
            }
        }

        this.add(accountsTabbedPane, BorderLayout.CENTER);
    }

    private class RegistrationStateChangeListenerImpl
        implements RegistrationStateChangeListener
    {
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            ProtocolProviderService protocolProvider = evt.getProvider();

            if (protocolProvider.getOperationSet(
                    OperationSetServerStoredAccountInfo.class) != null
                && evt.getNewState() == RegistrationState.REGISTERED)
            {
                if (accountsTable.containsKey(protocolProvider))
                {
                    AccountDetailsPanel detailsPanel
                        = accountsTable.get(protocolProvider);

                    if(!detailsPanel.isDataLoaded())
                        detailsPanel.loadDetails();
                }
            }
        }
    }
}
