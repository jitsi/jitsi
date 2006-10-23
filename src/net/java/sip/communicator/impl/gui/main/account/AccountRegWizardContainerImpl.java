/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.io.*;
import java.util.*;

import javax.imageio.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The implementation of the <tt>AccountRegistrationWizardContainer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class AccountRegWizardContainerImpl extends Wizard
    implements AccountRegistrationWizardContainer {

    private static final Logger logger = Logger
        .getLogger(AccountRegWizardContainerImpl.class);
    
    private AccountRegFirstPage firstPage;
    
    private AccountRegSummaryPage summaryPage;
        
    private AccountRegistrationWizard currentWizard;
    
    ConfigurationService configService
            = GuiActivator.getConfigurationService();
    /**
     * Listeners interested in events dispatched upon modifications
     * in the account registrations list.
     */
    private Vector accountRegListeners = new Vector();
    
    public AccountRegWizardContainerImpl() {
        this.getDialog().setTitle(
                Messages.getString("accountRegistrationWizard"));
        
        this.firstPage = new AccountRegFirstPage(this);
        
        this.summaryPage = new AccountRegSummaryPage(this);
        
        this.addAccountRegistrationListener(firstPage);
    }
            
    /**
     * Adds the given <tt>AccountRegistrationWizard</tt> to the list of
     * containing wizards.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt> to add
     */
    public void addAccountRegistrationWizard(
            AccountRegistrationWizard wizard) {    
        
        this.fireAccountRegistrationEvent(wizard,
                AccountRegistrationEvent.REGISTRATION_ADDED);        
    }

    /**
     * Removes the given <tt>AccountRegistrationWizard</tt> from the list of
     * containing wizards.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt> to remove
     */
    public void removeAccountRegistrationWizard(
            AccountRegistrationWizard wizard) {
        
        this.fireAccountRegistrationEvent(wizard,
                AccountRegistrationEvent.REGISTRATION_REMOVED);
    }
    
    /**
     * Adds a listener for <tt>AccountRegistrationEvent</tt>s.
     *
     * @param l the listener to add
     */
    public void addAccountRegistrationListener(
            AccountRegistrationListener l) {
        synchronized (accountRegListeners)
        {
            this.accountRegListeners.add(l);
        }
    }

    /**
     * Removes a listener for <tt>AccountRegistrationEvent</tt>s.
     *
     * @param l the listener to remove
     */
    public void removeAccountRegistrationListener(
            AccountRegistrationListener l) {
        synchronized (accountRegListeners)
        {
            this.accountRegListeners.remove(l);
        }
    }
    
    /**
     * Creates the corresponding <tt>AccountRegistrationEvent</tt> instance and
     * notifies all <tt>AccountRegistrationListener</tt>s that an account
     * registration wizard has been added or removed from this container.
     * 
     * @param wizard The wizard that has caused the event.
     * @param eventID one of the REGISTRATION_XXX static fields indicating the
     *            nature of the event.
     */
    private void fireAccountRegistrationEvent(AccountRegistrationWizard wizard,
            int eventID)
    {
        AccountRegistrationEvent evt
            = new AccountRegistrationEvent(wizard, eventID);

        logger.trace("Will dispatch the following mcl event: "
                + evt);

        synchronized (accountRegListeners)
        {
            Iterator listeners = this.accountRegListeners
                .iterator();

            while (listeners.hasNext())
            {
                AccountRegistrationListener l 
                    = (AccountRegistrationListener) listeners.next();
                
                switch (evt.getEventID())
                {
                    case AccountRegistrationEvent.REGISTRATION_ADDED:
                        l.accountRegistrationAdded(evt);
                        break;
                    case AccountRegistrationEvent.REGISTRATION_REMOVED:
                        l.accountRegistrationRemoved(evt);
                        break;
                    default:
                        logger.error("Unknown event type " + evt.getEventID());
                }   
            }
        }
    }

    /**
     * Returns the first wizard page.
     * @return the first wizard page
     */
    public AccountRegFirstPage getFirstPage() {
        return firstPage;
    }
    
    /**
     * Returns the summary wizard page.
     * @return the summary wizard page
     */
    public AccountRegSummaryPage getSummaryPage() {
        return summaryPage;
    }
    
    /**
     * Opens a wizard for creating a new account.
     */
    public void newAccount() {
        this.newAccount(firstPage.getIdentifier());        
    }
    
    /**
     * Opens a wizard for creating a new account.
     */
    public void newAccount(Object currentPageIdentifier) {
        this.registerWizardPage(firstPage.getIdentifier(), firstPage);
                
        this.registerWizardPage(summaryPage.getIdentifier(), summaryPage);
        
        this.setCurrentPage(currentPageIdentifier);        
    }
    
    /**
     * Opens the corresponding wizard to modify an existing account given by
     * the <tt>protocolProvider</tt> parameter.
     * 
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for the
     * account to modify.
     */
    public void modifyAccount(ProtocolProviderService protocolProvider) {
        
        String wizardClassName = null;
        
        String prefix = "net.java.sip.communicator.impl.ui.accounts";
                        
        List accounts = this.configService
                .getPropertyNamesByPrefix(prefix, true);
        
        Iterator accountsIter = accounts.iterator();
        
        while(accountsIter.hasNext()) {            
            String accountRootPropName = (String) accountsIter.next();

            String accountUID = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {
                                
                wizardClassName = configService.getString(
                        accountRootPropName + ".wizard");
                break;
            }
        }
        
        AccountRegistrationWizard wizard
            = getWizardFromClassName(wizardClassName);
        
        this.registerWizardPage(summaryPage.getIdentifier(), summaryPage);
        
        this.setCurrentWizard(wizard);
        
        Iterator i = wizard.getPages();
        
        Object identifier = null;
        boolean firstPage = true;
        
        while(i.hasNext()) {
            WizardPage page = (WizardPage)i.next();
            
            identifier = page.getIdentifier();
            
            this.registerWizardPage(identifier, page);
            
            if(firstPage) {
                this.setCurrentPage(identifier);
                firstPage = false;
            }
        }
        
        wizard.loadAccount(protocolProvider);
        
        this.getSummaryPage()
            .setPreviousPageIdentifier(identifier);
        
        try {
            this.setWizzardIcon(
                ImageIO.read(new ByteArrayInputStream(wizard.getIcon())));
        }
        catch (IOException e1) {         
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
    public void addAccountWizard(
            ProtocolProviderService protocolProvider,
            AccountRegistrationWizard wizard)
    {
        String prefix = "net.java.sip.communicator.impl.ui.accounts";

        List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;
        Iterator accountsIter = accounts.iterator();

        while(accountsIter.hasNext()) {
            String accountRootPropName
                = (String) accountsIter.next();

            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {

                configService.setProperty(
                        accountRootPropName + ".wizard",
                        wizard.getClass().getName().replace('.', '_'));

                savedAccount = true;
            }
        }

        if(!savedAccount) {
            String accNodeName
                = "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage
                = "net.java.sip.communicator.impl.ui.accounts."
                        + accNodeName;

            configService.setProperty(accountPackage,
                    protocolProvider.getAccountID().getAccountUniqueID());

            configService.setProperty(
                    accountPackage+".wizard",
                    wizard);
        }
    }

    /**
     * Returns the currently used <tt>AccountRegistrationWizard</tt>.
     * @return the currently used <tt>AccountRegistrationWizard</tt>
     */
    public AccountRegistrationWizard getCurrentWizard() {
        return currentWizard;
    }

    /**
     * Sets the currently used <tt>AccountRegistrationWizard</tt>.
     * @param currentWizard the <tt>AccountRegistrationWizard</tt> to set
     * as current one
     */
    public void setCurrentWizard(AccountRegistrationWizard currentWizard) {
        this.currentWizard = currentWizard;
    }
    
    /**
     * Returns the <tt>AccountRegistrationWizard</tt> corresponding to the
     * given class name.
     * @param wizardClassName the class name of the searched wizard
     * @return the <tt>AccountRegistrationWizard</tt> corresponding to the
     * given class name
     */
    private AccountRegistrationWizard getWizardFromClassName(
            String wizardClassName) {
                
        Iterator i = this.firstPage.getWizardsList();
        
        while(i.hasNext()) {
            AccountRegistrationWizard wizard
                = (AccountRegistrationWizard)i.next();
            
            String wizardClassName1 = wizard.getClass()
                .getName().replace('.', '_');
            
            if(wizardClassName1.equals(wizardClassName)) {
                return wizard;
            }
        }
        return null;
    }
    
    /**
     * Unregisters all pages added by the current wizard.
     */
    public void unregisterWizardPages() {
        Iterator i = this.getCurrentWizard().getPages();
        
        Object identifier = null;
        
        while(i.hasNext()) {
            WizardPage page = (WizardPage)i.next();
            
            identifier = page.getIdentifier();
            
            this.unregisterWizardPage(identifier);
        }
    }
}
