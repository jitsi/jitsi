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
import java.awt.Container;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The implementation of the <tt>AccountRegistrationWizardContainer</tt>.
 *
 * @author Yana Stamcheva
 */
public class AccountRegWizardContainerImpl
    extends Wizard
    implements  WizardContainer,
                ServiceListener
{
    private static final Logger logger =
        Logger.getLogger(AccountRegWizardContainerImpl.class);

    private final AccountRegSummaryPage summaryPage;

    private AccountRegistrationWizard currentWizard;

    private final ConfigurationService configService
        = GuiActivator.getConfigurationService();

    private final Map<String, AccountRegistrationWizard> registeredWizards =
        new Hashtable<String, AccountRegistrationWizard>();

    public AccountRegWizardContainerImpl(MainFrame mainFrame)
    {
        super(mainFrame);

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ACCOUNT_REGISTRATION_WIZARD"));

        this.summaryPage = new AccountRegSummaryPage(this);

        this.registerWizardPage(summaryPage.getIdentifier(), summaryPage);

        Collection<ServiceReference<AccountRegistrationWizard>> accountWizardRefs;
        try
        {
            accountWizardRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        AccountRegistrationWizard.class,
                        null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, add them in this container.
        if ((accountWizardRefs != null) && !accountWizardRefs.isEmpty())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Found " + accountWizardRefs.size()
                            + " already installed providers.");
            }
            for (ServiceReference<AccountRegistrationWizard> serRef
                    : accountWizardRefs)
            {
                String protocolName
                    = (String)
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL);
                AccountRegistrationWizard wizard
                    = GuiActivator.bundleContext.getService(serRef);

                addAccountRegistrationWizard(protocolName, wizard);
            }
        }

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Adds the given <tt>AccountRegistrationWizard</tt> to the list of
     * containing wizards.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt> to add
     */
    public void addAccountRegistrationWizard(   String protocolName,
                                                AccountRegistrationWizard wizard)
    {
        synchronized (registeredWizards)
        {
            registeredWizards.put(protocolName, wizard);
        }
    }

    /**
     * Removes the given <tt>AccountRegistrationWizard</tt> from the list of
     * containing wizards.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt> to remove
     */
    public void removeAccountRegistrationWizard(String protocolName,
                                                AccountRegistrationWizard wizard)
    {
        synchronized (registeredWizards)
        {
            registeredWizards.remove(protocolName);
        }
    }

    /**
     * Returns the summary wizard page.
     *
     * @return the summary wizard page
     */
    public AccountRegSummaryPage getSummaryPage()
    {
        return summaryPage;
    }

    /**
     * Opens the corresponding wizard to modify an existing account given by the
     * <tt>protocolProvider</tt> parameter.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for the
     *            account to modify.
     */
    public void modifyAccount(ProtocolProviderService protocolProvider)
    {
        AccountRegistrationWizard wizard = getProtocolWizard(protocolProvider);

        this.setCurrentWizard(wizard);

        wizard.setModification(true);
        this.summaryPage.setModification(protocolProvider);

        if(protocolProvider != null
            && protocolProvider.getAccountID().isReadOnly())
        {
            // cardPanel is the main panel of the wizard pages
            setDisabled(cardPanel.getComponents());
        }

        wizard.loadAccount(protocolProvider);
    }

    /**
     * Disables all editable components. Used to enable readonly accounts
     * to only show their values and forbid the user to edit.
     *
     * @param components the components to disable
     */
    private void setDisabled(Component[] components)
    {
        for(Component c : components)
        {
            if(c instanceof JTextComponent)
                ((JTextComponent)c).setEditable(false);
            else if(c instanceof JComboBox
                    || c instanceof AbstractButton)
                c.setEnabled(false);

            if(c instanceof Container)
                setDisabled(((Container)c).getComponents());
        }
    }

    /**
     * Returns the wizard corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * corresponding wizard we're looking for
     * @return the corresponding wizard
     */
    public AccountRegistrationWizard getProtocolWizard(
                                    ProtocolProviderService protocolProvider)
    {
        AccountRegistrationWizard res
            = registeredWizards.get(protocolProvider.getProtocolDisplayName());

        // compatibility check, some protocols have changed name
        // and when they have those name saved in configuration
        // cannot be edited, so lets check whether there is a wizard
        // with the same protocol name like the one of its provider
        if(res == null)
        {
            //lets find matching protocol name in registered wizards
            Iterator<AccountRegistrationWizard> iter =
                registeredWizards.values().iterator();
            while(iter.hasNext())
            {
                AccountRegistrationWizard wizard = iter.next();
                if(wizard.getProtocolName()
                    .equals(protocolProvider.getProtocolName()))
                {
                    res = wizard;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Saves the (protocol provider, wizard) pair in through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to save
     * @param wizard the wizard to save
     */
    public void saveAccountWizard(ProtocolProviderService protocolProvider,
        AccountRegistrationWizard wizard)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts =
            configService.getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(
                    protocolProvider.getAccountID().getAccountUniqueID()))
            {
                configService.setProperty(accountRootPropName + ".wizard",
                    wizard.getClass().getName().replace('.', '_'));

                savedAccount = true;
            }
        }

        if (!savedAccount)
        {
            String accountPackage
                = prefix + ".acc" + Long.toString(System.currentTimeMillis());

            configService.setProperty(accountPackage, protocolProvider
                .getAccountID().getAccountUniqueID());

            configService.setProperty(accountPackage + ".wizard", wizard);
        }
    }

    /**
     * Returns the currently used <tt>AccountRegistrationWizard</tt>.
     *
     * @return the currently used <tt>AccountRegistrationWizard</tt>
     */
    public AccountRegistrationWizard getCurrentWizard()
    {
        return currentWizard;
    }

    /**
     * Sets the currently used <tt>AccountRegistrationWizard</tt>.
     *
     * @param wizard the <tt>AccountRegistrationWizard</tt> to set as
     *            current one
     */
    public void setCurrentWizard(AccountRegistrationWizard wizard)
    {
        this.currentWizard = wizard;

        if (wizard instanceof DesktopAccountRegistrationWizard)
            summaryPage.setPreferredSize(
                ((DesktopAccountRegistrationWizard) currentWizard).getSize());

        Iterator<WizardPage> i = wizard.getPages();

        while(i.hasNext())
        {
            WizardPage page = i.next();

            this.registerWizardPage(page.getIdentifier(), page);
        }

        this.setCurrentPage(wizard.getFirstPageIdentifier());

        try {
            this.setWizzardIcon(
                ImageIO.read(new ByteArrayInputStream(wizard.getPageImage())));
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Unregisters all pages added by the current wizard.
     */
    public void unregisterWizardPages()
    {
        Iterator<WizardPage> i = this.getCurrentWizard().getPages();

        while (i.hasNext())
        {
            WizardPage page = i.next();

            this.unregisterWizardPage(page.getIdentifier());
        }
    }

    /**
     * Handles registration of a new account wizard.
     */
    public void serviceChanged(ServiceEvent event)
    {
        if(!GuiActivator.isStarted)
            return;

        ServiceReference<?> serRef = event.getServiceReference();
        Object sService = GuiActivator.bundleContext.getService(serRef);

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof AccountRegistrationWizard))
            return;

        String protocolName
            = (String) serRef.getProperty(ProtocolProviderFactory.PROTOCOL);
        AccountRegistrationWizard wizard = (AccountRegistrationWizard) sService;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            logger.info("Handling registration of a new Account Wizard.");
            addAccountRegistrationWizard(protocolName, wizard);
            break;

        case ServiceEvent.UNREGISTERING:
            removeAccountRegistrationWizard(protocolName, wizard);
            break;
        }
    }

    /**
     * Implements the <tt>SIPCommDialog</tt> close method.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        summaryPage.dispose();
    }
}
