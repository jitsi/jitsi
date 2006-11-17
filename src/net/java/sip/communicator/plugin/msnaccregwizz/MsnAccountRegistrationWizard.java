/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msnaccregwizz;

import java.util.*;

import javax.swing.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>MsnAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Msn protocol. It should allow
 * the user to create and configure a new Msn account.
 *
 * @author Yana Stamcheva
 */
public class MsnAccountRegistrationWizard implements AccountRegistrationWizard {

    private FirstWizardPage firstWizardPage;

    private MsnAccountRegistration registration
        = new MsnAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private String propertiesPackage = "net.java.sip.communicator.plugin.msnaccregwizz";
    
    private boolean isModification;

    /**
     * Creates an instance of <tt>MsnAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public MsnAccountRegistrationWizard(WizardContainer wizardContainer) {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon() {
        return Resources.getImage(Resources.MSN_LOGO);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    public String getProtocolName() {
        return Resources.getString("protocolName");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription() {
        return Resources.getString("protocolDescription");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator getPages() {
        ArrayList pages = new ArrayList();
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
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
     * @return ProtocolProviderService
     */
    public ProtocolProviderService finish() {
        firstWizardPage = null;
        ProtocolProviderFactory factory
            = MsnAccRegWizzActivator.getMsnProtocolProviderFactory();

        return this.installAccount(factory,
                registration.getUin(), registration.getPassword());
    }

    /**
     * Creates an account for the given user and password.
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
            ProtocolProviderFactory providerFactory,
            String user,
            String passwd) {

        Hashtable accountProperties = new Hashtable();

        if(registration.isRememberPassword()) {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        if(isModification) {
            providerFactory.uninstallAccount(protocolProvider.getAccountID());
            this.protocolProvider = null;
        }

        try {
            AccountID accountID = providerFactory.installAccount(
                    user, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);
    
            protocolProvider
                = (ProtocolProviderService) MsnAccRegWizzActivator.bundleContext
                    .getService(serRef);
        }
        catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());            
        }
        catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }    

        return protocolProvider;
    }

    /**
     * Fills the UIN and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider) {

        this.protocolProvider = protocolProvider;

        this.firstWizardPage.loadAccount(protocolProvider);
        
        this.isModification = true;
    }
}
