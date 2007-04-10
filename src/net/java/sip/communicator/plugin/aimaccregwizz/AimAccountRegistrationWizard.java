/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.aimaccregwizz;

import java.util.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.gibberishaccregwizz.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * The <tt>AimAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the AIM protocol. It should allow
 * the user to create and configure a new AIM account.
 *
 * @author Yana Stamcheva
 */
public class AimAccountRegistrationWizard implements AccountRegistrationWizard
{
    private FirstWizardPage firstWizardPage;

    private AimAccountRegistration registration
        = new AimAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    /**
     * Creates an instance of <tt>AimAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public AimAccountRegistrationWizard(WizardContainer wizardContainer) {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     */
    public byte[] getIcon() {
        return Resources.getImage(Resources.AIM_LOGO);
    }
    
    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code> method.
     * Returns the image used to decorate the wizard page
     * 
     * @return byte[] the image used to decorate the wizard page
     */
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
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
        ArrayList pages = new ArrayList();
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);

        pages.add(firstWizardPage);

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
        
        if(registration.getProxy() != null)
            summaryTable.put(Resources.getString("proxy"),
                            registration.getProxy());
        
        if(registration.getProxyPort() != null)
            summaryTable.put(Resources.getString("proxyPort"),
                            registration.getProxyPort());
        
        if(registration.getProxyType() != null)
            summaryTable.put(Resources.getString("proxyType"),
                            registration.getProxyType());
        
        if(registration.getProxyPort() != null)
            summaryTable.put(Resources.getString("proxyUsername"),
                            registration.getProxyPort());
        
        if(registration.getProxyType() != null)
            summaryTable.put(Resources.getString("proxyPassword"),
                            registration.getProxyType());
        
        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     */
    public ProtocolProviderService finish() {
        firstWizardPage = null;
        ProtocolProviderFactory factory
            = AimAccRegWizzActivator.getAimProtocolProviderFactory();

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

        if(registration.getProxyType() != null)
        {
            accountProperties.put(ProtocolProviderFactory.PROXY_ADDRESS,
                registration.getProxy());

            accountProperties.put(ProtocolProviderFactory.PROXY_PORT,
                registration.getProxyPort());

            accountProperties.put(ProtocolProviderFactory.PROXY_TYPE,
                registration.getProxyType());

            accountProperties.put(ProtocolProviderFactory.PROXY_USERNAME,
                registration.getProxyUsername());

            accountProperties.put(ProtocolProviderFactory.PROXY_PASSWORD,
                registration.getProxyPassword());
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
                = (ProtocolProviderService) AimAccRegWizzActivator.bundleContext
                    .getService(serRef);
        }
        catch (IllegalArgumentException e) {
            new ErrorDialog(null, e.getMessage(), e).showDialog();
        }
        catch (IllegalStateException e) {
            new ErrorDialog(null, e.getMessage(), e).showDialog();
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
