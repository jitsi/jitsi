/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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

    private AccountRegFirstPage defaultPage;

    private AccountRegSummaryPage summaryPage;

    private AccountRegistrationWizard currentWizard;

    ConfigurationService configService = GuiActivator.getConfigurationService();

    private Hashtable registeredWizards = new Hashtable();

    public AccountRegWizardContainerImpl(MainFrame mainFrame)
    {
        super(mainFrame);

        this.setTitle(Messages.getI18NString("accountRegistrationWizard")
            .getText());

        this.defaultPage = new AccountRegFirstPage(this);

        this.summaryPage = new AccountRegSummaryPage(this);

        this.registerWizardPage(defaultPage.getIdentifier(), defaultPage);

        this.registerWizardPage(summaryPage.getIdentifier(), summaryPage);

        ServiceReference[] accountWizardRefs = null;
        try
        {
            accountWizardRefs = GuiActivator.bundleContext
                .getServiceReferences(
                    AccountRegistrationWizard.class.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, add them in this container.
        if (accountWizardRefs != null)
        {
            logger.debug("Found "
                         + accountWizardRefs.length
                         + " already installed providers.");
            for (int i = 0; i < accountWizardRefs.length; i++)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard) GuiActivator.bundleContext
                        .getService(accountWizardRefs[i]);

                this.addAccountRegistrationWizard(wizard);
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
    public void addAccountRegistrationWizard(AccountRegistrationWizard wizard)
    {
        synchronized (registeredWizards)
        {
            registeredWizards.put(wizard.getClass().getName(), wizard);
        }

        Set set = GuiActivator.getProtocolProviderFactories().entrySet();
        Iterator iter = set.iterator();

        boolean hasRegisteredAccounts = false;

        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();

            ProtocolProviderFactory providerFactory
                = (ProtocolProviderFactory) entry.getValue();

            ArrayList accountsList = providerFactory.getRegisteredAccounts();

            AccountID accountID;
            for (int i = 0; i < accountsList.size(); i++)
            {
                accountID = (AccountID) accountsList.get(i);
                boolean isHidden = accountID.getAccountProperties().
                    get("HIDDEN_PROTOCOL") != null;
                
                if(!isHidden)
                {
                    hasRegisteredAccounts = true;
                    break;
                }
            }
        }

        defaultPage.addAccountRegistrationWizard(wizard);
    }

    /**
     * Removes the given <tt>AccountRegistrationWizard</tt> from the list of
     * containing wizards.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt> to remove
     */
    public void removeAccountRegistrationWizard(AccountRegistrationWizard wizard)
    {
        synchronized (registeredWizards)
        {
            registeredWizards.remove(wizard.getClass().getName());
        }

        defaultPage.removeAccountRegistrationWizard(wizard);
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
     * Opens a wizard for creating a new account.
     */
    public void newAccount()
    {
        this.setCurrentPage(defaultPage.getIdentifier());
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
        String wizardClassName = null;

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List accounts =
            this.configService.getPropertyNamesByPrefix(prefix, true);

        Iterator accountsIter = accounts.iterator();

        while (accountsIter.hasNext())
        {
            String accountRootPropName = (String) accountsIter.next();

            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID()
                .getAccountUniqueID()))
            {

                wizardClassName =
                    configService.getString(accountRootPropName + ".wizard");
                break;
            }
        }

        AccountRegistrationWizard wizard =
            getWizardFromClassName(wizardClassName);

        this.setCurrentWizard(wizard);

        wizard.setModification(true);

        Iterator i = wizard.getPages();

        Object identifier = null;
        boolean firstPage = true;

        while (i.hasNext())
        {
            WizardPage page = (WizardPage) i.next();

            identifier = page.getIdentifier();

            this.registerWizardPage(identifier, page);

            if (firstPage)
            {
                this.setCurrentPage(identifier);
                firstPage = false;
            }
        }

        wizard.loadAccount(protocolProvider);

        try
        {
            this.setWizzardIcon(ImageIO.read(new ByteArrayInputStream(wizard
                .getPageImage())));
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
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

        List accounts = configService.getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;
        Iterator accountsIter = accounts.iterator();

        while (accountsIter.hasNext())
        {
            String accountRootPropName = (String) accountsIter.next();

            String accountUID = configService.getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID()
                .getAccountUniqueID()))
            {

                configService.setProperty(accountRootPropName + ".wizard",
                    wizard.getClass().getName().replace('.', '_'));

                savedAccount = true;
            }
        }

        if (!savedAccount)
        {
            String accNodeName =
                "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage =
                "net.java.sip.communicator.impl.gui.accounts." + accNodeName;

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

        Dimension wizardSize = this.currentWizard.getSize();

        summaryPage.setPreferredSize(wizardSize);

        Iterator i = wizard.getPages();

        while(i.hasNext())
        {
            WizardPage page = (WizardPage)i.next();

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
     * Returns the <tt>AccountRegistrationWizard</tt> corresponding to the
     * given class name.
     * 
     * @param wizardClassName the class name of the searched wizard
     * @return the <tt>AccountRegistrationWizard</tt> corresponding to the
     *         given class name
     */
    private AccountRegistrationWizard getWizardFromClassName(
        String wizardClassString)
    {
        String wizardClassName = wizardClassString.replace('_', '.');

        synchronized (registeredWizards)
        {
            return (AccountRegistrationWizard)
                registeredWizards.get(wizardClassName);
        }
    }

    /**
     * Unregisters all pages added by the current wizard.
     */
    public void unregisterWizardPages()
    {
        Iterator i = this.getCurrentWizard().getPages();

        Object identifier = null;

        while (i.hasNext())
        {
            WizardPage page = (WizardPage) i.next();

            identifier = page.getIdentifier();

            this.unregisterWizardPage(identifier);
        }
    }

    /**
     * Handles registration of a new account wizard.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = GuiActivator.bundleContext.getService(
            event.getServiceReference());

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof AccountRegistrationWizard))
        {
            return;
        }

        AccountRegistrationWizard wizard
            = (AccountRegistrationWizard) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger
                .info("Handling registration of a new Account Wizard.");

            this.addAccountRegistrationWizard(wizard);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            this.removeAccountRegistrationWizard(wizard);
        }
    }
}
