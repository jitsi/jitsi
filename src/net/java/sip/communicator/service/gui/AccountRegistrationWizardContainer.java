/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.gui.event.AccountRegistrationListener;

/**
 * The <tt>AccountRegistrationWizardContainer</tt> is meant to be implemented
 * by the UI service implementation in order to allow other modules to add
 * <tt>AccountRegistrationWizard</tt>s to the GUI. Each wizard is made for
 * a given protocol and should provide a sequence of forms through which the
 * user could registrate a new account.
 * <p>
 * In other words the <tt>AccountRegistrationWizardContainer</tt> should store
 * all registered <tt>AccountRegistrationWizard</tt>s. Note that it is the
 * UI Service implementation, which should provide a wizard ui control,
 * which should manage all the events, panels and buttons, etc.
 * 
 * @author Yana Stamcheva
 */
public interface AccountRegistrationWizardContainer extends WizardContainer {
    /**
     * Adds the given <tt>AccountRegistrationWizard</tt> to this container.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt> to add
     */
    public void addAccountRegistrationWizard(AccountRegistrationWizard wizard);
    
    /**
     * Removes the given <tt>AccountRegistrationWizard</tt> from this container.
     * 
     * @param wizard the <tt>AccountRegistrationWizard</tt> to remove
     */
    public void removeAccountRegistrationWizard(AccountRegistrationWizard wizard);
    
    /**
     * Adds a listener for <tt>AccountRegistrationEvent</tt>s.
     *
     * @param l the listener to add
     */
    public void addAccountRegistrationListener(AccountRegistrationListener l);

    /**
     * Removes a listener previously added with
     * <tt>addAccountRegistrationListener</tt>.
     *
     * @param l the listener to remove
     */
    public void removeAccountRegistrationListener(AccountRegistrationListener l);
   
}
