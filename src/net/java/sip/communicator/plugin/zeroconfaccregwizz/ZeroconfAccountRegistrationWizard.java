/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.awt.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
    private Logger logger
        = Logger.getLogger(ZeroconfAccountRegistrationWizard.class);

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

    private final WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    /**
     * Creates an instance of <tt>ZeroconfAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public ZeroconfAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.SIGN_IN"));
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
        return Resources.getString("plugin.zeroaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.zeroaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();

        // create new registration, our container needs the pages
        // this means this is a new wizard and we must reset all data
        // it will be invoked and when the wizard cleans and unregister
        // our pages, but this fix don't hurt in this situation.
        this.registration = new ZeroconfAccountRegistration();

        firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Hashtable<String, String> summaryTable = new Hashtable<String, String>();

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
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(registration.getUserID(), null);
    }

    /**
     * Installs the account with the given user name and password.
     * 
     * @return the <tt>ProtocolProviderService</tt> corresponding to the newly
     * created account.
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory
            = ZeroconfAccRegWizzActivator.getZeroconfProtocolProviderFactory();

        return this.installAccount(factory,
                                   userName);
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
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/zeroconf/zeroconf32x32.png");

        accountProperties.put("first", registration.getFirst());
        accountProperties.put("last", registration.getLast());
        accountProperties.put("mail", registration.getMail());

        accountProperties.put(
            ProtocolProviderFactory.NO_PASSWORD_REQUIRED,
            new Boolean(true).toString());

        accountProperties.put("rememberContacts", 
            new Boolean(registration.isRememberContacts()).toString());

        if (isModification)
        {
            providerFactory.uninstallAccount(protocolProvider.getAccountID());
            this.protocolProvider = null;
            this.isModification = false;
        }

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
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Exception exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Failed to add account",
                OperationFailedException.GENERAL_ERROR);
        }


        return protocolProvider;
    }

    /**
     * Fills the UserID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new ZeroconfAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Indicates if this wizard is opened for modification or for creating a
     * new account.
     * 
     * @return <code>true</code> if this wizard is opened for modification and
     * <code>false</code> otherwise.
     */
    public boolean isModification()
    {
        return isModification;
    }

    /**
     * Returns the wizard container, where all pages are added.
     * 
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     * 
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public ZeroconfAccountRegistration getRegistration()
    {
        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }
    
    /**
     * Returns the identifier of the page to show first in the wizard.
     * @return the identifier of the page to show first in the wizard.
     */
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Returns the identifier of the page to show last in the wizard.
     * @return the identifier of the page to show last in the wizard.
     */
    public Object getLastPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     * 
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account. 
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }
    
    /**
     * Returns the password label for the simplified account registration form.
     * @return the password label for the simplified account registration form.
     */
    public String getPasswordLabel()
    {
        return Resources.getString("service.gui.PASSWORD");
    }

    /**
     * Returns the user name label for the simplified account registration
     * form.
     * 
     * @return the user name label for the simplified account registration
     * form.
     */
    public String getUserNameLabel()
    {
        return Resources.getString("userID");
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return null;
    }

    /**
     * Enables the simple "Sign in" form.
     */
    public boolean isSimpleFormEnabled()
    {
        return false;
    }

    /**
     * Nothing to do here in the case of Bonjour.
     */
    public void webSignup()
    {
        throw new UnsupportedOperationException(
            "The web sign up is not supported by the Zeroconf wizard.");
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return false;
    }

    public Object getSimpleForm()
    {
        // when creating first wizard page, create and new
        // AccountRegistration to avoid reusing old instances and
        // data left from old registrations
        this.registration = new ZeroconfAccountRegistration();

        firstWizardPage = new FirstWizardPage(this);
        return firstWizardPage.getSimpleForm();
    }
}
