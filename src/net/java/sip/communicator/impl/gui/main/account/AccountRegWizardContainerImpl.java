/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import net.java.sip.communicator.impl.gui.customcontrols.wizard.Wizard;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.gui.AccountRegistrationWizardContainer;
import net.java.sip.communicator.service.gui.event.AccountRegistrationEvent;
import net.java.sip.communicator.service.gui.event.AccountRegistrationListener;
import net.java.sip.communicator.util.Logger;

/**
 * The implementation of the <tt>AccountRegistrationWizardContainer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class AccountRegWizardContainerImpl extends Wizard
    implements AccountRegistrationWizardContainer {

    private static final Logger logger = Logger
        .getLogger(AccountRegWizardContainerImpl.class);
    
    private ArrayList accountRegsList = new ArrayList();
    
    private AccountRegFirstPage firstPage;
    
    private AccountRegSummaryPage summaryPage;
    
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
        
        this.registerWizardPage(firstPage.getIdentifier(), firstPage);
                
        this.registerWizardPage(summaryPage.getIdentifier(), summaryPage);
        
        this.setCurrentPage(firstPage.getIdentifier());        
    }
            
    /**
     * Adds the given <tt>AccountRegistrationWizard</tt> to the list of
     * containing wizards.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt> to add
     */
    public void addAccountRegistrationWizard(
            AccountRegistrationWizard wizard) {
    
        accountRegsList.add(wizard);
        
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
        
        accountRegsList.remove(wizard);
        
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

    public AccountRegFirstPage getFirstPage() {
        return firstPage;
    }
    
    public AccountRegSummaryPage getSummaryPage() {
        return summaryPage;
    }
}
