/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.accountinfo;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.osgi.framework.*;

/**
 * A GUI plug-in for SIP Communicator that will allow users to set cross
 * protocol account information.
 * 
 * @author Adam Goldstein
 */
public class AccountInfoForm
    extends JPanel
    implements ConfigurationForm
{
    /**
     * The right side of the AccountInfo frame that contains protocol specific
     * account details.
     */
    private AccountDetailsPanel detailsPanel;

    private JTabbedPane accountsTabbedPane = new JTabbedPane();

    private Hashtable<ProtocolProviderService, AccountDetailsPanel>
        accountsTable = new Hashtable();

    /**
     * Constructs a frame with an AccuontInfoAccountPanel to display all
     * registered accounts on the left, and an information interface,
     * AccountDetailsPanel, on the right.
     * 
     * @param metaContact
     */
    public AccountInfoForm()
    {
        super(new BorderLayout());

        Set set = AccountInfoActivator.getProtocolProviderFactories().entrySet();
        Iterator iter = set.iterator();

        boolean hasRegisteredAccounts = false;

        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();

            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) entry.getValue();

            ArrayList accountsList = providerFactory.getRegisteredAccounts();

            AccountID accountID;
            ServiceReference serRef;
            ProtocolProviderService protocolProvider;

            for (int i = 0; i < accountsList.size(); i++)
            {
                accountID = (AccountID) accountsList.get(i);

                boolean isHidden = 
                    accountID.getAccountProperties().get("HIDDEN_PROTOCOL") != null;

                if(!isHidden)
                    hasRegisteredAccounts = true;

                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider = (ProtocolProviderService) AccountInfoActivator
                    .bundleContext.getService(serRef);

                detailsPanel = new AccountDetailsPanel(protocolProvider);

                accountsTable.put(protocolProvider, detailsPanel);

                protocolProvider.addRegistrationStateChangeListener(
                    new RegistrationStateChangeListenerImpl());

                this.accountsTabbedPane.addTab(
                    accountID.getUserID(), detailsPanel);
            }
        }

        this.add(accountsTabbedPane, BorderLayout.CENTER);
    }

    /**
     * Returns the title of this configuration form.
     * 
     * @return the icon of this configuration form.
     */
    public String getTitle()
    {
        return Resources.getString("title");
    }

    /**
     * Returns the icon of this configuration form.
     * 
     * @return the icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return Resources.getImageInBytes("infoIcon");
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