/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.gibberishaccregwizz;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>GibberishAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Gibberish protocol. It allows
 * the user to create and configure a new Gibberish account.
 *
 * @author Emil Ivov
 */
public class GibberishAccountRegistrationWizard
    implements AccountRegistrationWizard
{

    /**
     * The first page of the gibberish account registration wizard.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The object that we use to store details on an account that we will be
     * creating.
     */
    private GibberishAccountRegistration registration
        = new GibberishAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private String propertiesPackage
        = "net.java.sip.communicator.plugin.gibberishaccregwizz";

    private boolean isModification;

    /**
     * Creates an instance of <tt>GibberishAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public GibberishAccountRegistrationWizard(WizardContainer wizardContainer)
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
        return Resources.getImage(Resources.GIBBERISH_LOGO);
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
            = GibberishAccRegWizzActivator.getGibberishProtocolProviderFactory();

        return this.installAccount(factory,
                                   registration.getUserID());
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
        String user)
    {

        Hashtable accountProperties = new Hashtable();

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD
                                  , registration.getPassword());
        }

        try
        {
            AccountID accountID = providerFactory.installAccount(
                user, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                GibberishAccRegWizzActivator.bundleContext
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
