/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The <tt>SIPAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new SIP account.
 *
 * @author Yana Stamcheva
 */
public class SIPAccountRegistrationWizard implements AccountRegistrationWizard {

    private FirstWizardPage firstWizardPage;

    private SIPAccountRegistration registration
        = new SIPAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private String propertiesPackage
        = "net.java.sip.communicator.plugin.sipaccregwizz";

    /**
     * Creates an instance of <tt>SIPAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public SIPAccountRegistrationWizard(WizardContainer wizardContainer) {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon() {
        return Resources.getImage(Resources.SIP_LOGO);
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
        
        boolean rememberPswd = new Boolean(registration.isRememberPassword())
            .booleanValue();
        
        String rememberPswdString;
        if(rememberPswd)
            rememberPswdString = Resources.getString("yes");
        else
            rememberPswdString = Resources.getString("no");

        summaryTable.put(Resources.getString("uin"),
                registration.getUin());
        summaryTable.put(Resources.getString("rememberPassword"),
                rememberPswdString);
        summaryTable.put(Resources.getString("registrar"),
                registration.getServerAddress());
        summaryTable.put(Resources.getString("serverPort"),
                registration.getServerPort());
        summaryTable.put(Resources.getString("proxy"),
                registration.getProxy());
        summaryTable.put(Resources.getString("proxyPort"),
                registration.getProxyPort());
        summaryTable.put(Resources.getString("preferredTransport"),
                registration.getPreferredTransport());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
    public ProtocolProviderService finish() {
        firstWizardPage = null;
        ProtocolProviderFactory factory
            = SIPAccRegWizzActivator.getSIPProtocolProviderFactory();

        ProtocolProviderService pps = null;
        if (factory != null)
            pps = this.installAccount(factory,
                registration.getUin(), registration.getPassword());
        
        return pps;
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

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                                  registration.getServerAddress());

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                registration.getServerPort());

        accountProperties.put(ProtocolProviderFactory.PROXY_ADDRESS,
                registration.getProxy());
        
        accountProperties.put(ProtocolProviderFactory.PROXY_PORT,
                registration.getProxyPort());
        
        accountProperties.put(ProtocolProviderFactory.PREFERRED_TRANSPORT,
                registration.getPreferredTransport());

        if(protocolProvider != null) {
            providerFactory.uninstallAccount(protocolProvider.getAccountID());
            this.protocolProvider = null;
        }

        AccountID accountID = providerFactory.installAccount(
                    user, accountProperties);

        ServiceReference serRef = providerFactory
            .getProviderForAccount(accountID);

        ProtocolProviderService protocolProvider
            = (ProtocolProviderService) SIPAccRegWizzActivator.bundleContext
                .getService(serRef);

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
    }
}
