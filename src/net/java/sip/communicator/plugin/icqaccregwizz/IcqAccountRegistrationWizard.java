/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.gui.WizardContainer;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;

/**
 * The <tt>IcqAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the ICQ protocol. It should allow
 * the user to create and configure a new ICQ account. 
 * 
 * @author Yana Stamcheva
 */
public class IcqAccountRegistrationWizard implements AccountRegistrationWizard {

    private FirstWizardPage firstWizardPage;
    
    private ArrayList pages = new ArrayList();
    
    private IcqAccountRegistration registration
        = new IcqAccountRegistration();
    
    /**
     * Creates an instance of <tt>IcqAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public IcqAccountRegistrationWizard(WizardContainer wizardContainer) {
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);
        
        pages.add(firstWizardPage);        
    }
    
    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     */
    public byte[] getIcon() {
        return Resources.getImage(Resources.ICQ_LOGO);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     */
    public String getProtocolName() {
        return Resources.getString("protocolName");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     */
    public String getProtocolDescription() {
        return Resources.getString("protocolDescription");
    }

    /**
     * Returns the set of pages contained in this wizard.
     */
    public Iterator getPages() {
        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     */
    public Iterator getSummary() {
        Hashtable summaryTable = new Hashtable();
        
        summaryTable.put("UIN", registration.getUin());
        summaryTable.put("Remember password", 
                new Boolean(registration.isRememberPassword()));
        
        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     */
    public void finish() {
        ProtocolProviderFactory factory 
            = IcqAccRegWizzActivator.getIcqProtocolProviderFactory();
        
        this.installAccount(factory, 
                registration.getUin(), registration.getPassword());
    }
    
    /**
     * Creates an account for the given user and password.
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier
     * @param passwd the password
     */
    public void installAccount( ProtocolProviderFactory providerFactory,
            String user,
            String passwd) {

        Hashtable accountProperties = new Hashtable();
        
        if(registration.isRememberPassword()) {
            accountProperties.put(AccountProperties.PASSWORD, passwd);
        }
        
        providerFactory.installAccount(
                IcqAccRegWizzActivator.bundleContext, user,
                    accountProperties);
    }
}
