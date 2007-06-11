/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ZeroconfAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Zeroconf protocol. It allows
 * the user to create and configure a new Zeroconf account.
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class ZeroconfAccountRegistrationWizard
    implements AccountRegistrationWizard
{

    /**
     * The first page of the zeroconf account registration wizard.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The object that we use to store details on an account that we will be
     * creating.
     */
    private ZeroconfAccountRegistration registration
        = new ZeroconfAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private String propertiesPackage
        = "net.java.sip.communicator.plugin.zeroconfaccregwizz";

    private boolean isModification;

    /**
     * Creates an instance of <tt>ZeroconfAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public ZeroconfAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.ZEROCONF_LOGO);
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
     * @return String
     */
    public String getProtocolName()
    {
        return Resources.getString("protocolName");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString("protocolDescription");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator getPages()
    {
        ArrayList pages = new ArrayList();
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    public Iterator getSummary()
    {
        Hashtable summaryTable = new Hashtable();

        summaryTable.put("User ID", registration.getUserID());
        summaryTable.put("First Name", registration.getFirst());
        summaryTable.put("Last Name", registration.getLast());
        summaryTable.put("Mail Address", registration.getMail());
        summaryTable.put("Remember Bonjour contacts?", 
                Boolean.toString(registration.isRememberContacts()));
        
        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
    public ProtocolProviderService finish()
    {
        firstWizardPage = null;
        ProtocolProviderFactory factory
            = ZeroconfAccRegWizzActivator.getZeroconfProtocolProviderFactory();

        return this.installAccount(factory,
                                   registration.getUserID());
    }

    /**
     * Creates an account for the given user and password.
     * 
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String user)
    {

        Hashtable accountProperties = new Hashtable();
        
        accountProperties.put("first", registration.getFirst());
        accountProperties.put("last", registration.getLast());
        accountProperties.put("mail", registration.getMail());

        accountProperties.put("rememberContacts", 
            new Boolean(registration.isRememberContacts()).toString());

        try
        {
            AccountID accountID = providerFactory.installAccount(
                user, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                ZeroconfAccRegWizzActivator.bundleContext
                .getService(serRef);
        }
        catch (IllegalArgumentException exc)
        {
            new ErrorDialog(null, exc.getMessage(), exc).showDialog();
        }
        catch (IllegalStateException exc)
        {
            new ErrorDialog(null, exc.getMessage(), exc).showDialog();
        }

        return protocolProvider;
    }

    /**
     * Fills the UserID and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {

        this.protocolProvider = protocolProvider;

        this.firstWizardPage.loadAccount(protocolProvider);

        isModification = true;
    }
}
