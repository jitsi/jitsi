/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.EventListener;

/**
 * Listens for all events caused by adding or removing of an
 * <tt>AccountRegistrationWizard</tt> to or from an
 * <tt>AccountRegistrationWizardContainer</tt>.
 * 
 * @author Yana Stamcheva
 */
public interface AccountRegistrationListener extends EventListener {

    /**
     * Indicates that an <tt>AccountRegistrationWizard</tt> has been
     * successfully added to an <tt>AccountRegistrationWizardContainer</tt>.
     * 
     * @param event the AccountRegistrationEvent containing the corresponding
     * wizard.
     */
    public void accountRegistrationAdded(AccountRegistrationEvent event);
    
    /**
     * Indicates that an <tt>AccountRegistrationWizard</tt> has been
     * successfully removed from an <tt>AccountRegistrationWizardContainer</tt>
     * 
     * @param event the AccountRegistrationEvent containing the corresponding
     * wizard.
     */
    public void accountRegistrationRemoved(AccountRegistrationEvent event);
}
