/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.gibberishaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>GibberishAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Gibberish protocol. It allows
 * the user to create and configure a new Gibberish account.
 *
 * @author Emil Ivov
 */
public class GibberishAccountRegistrationWizard
    extends AccountRegistrationWizard
{
    private final Logger logger
        = Logger.getLogger(GibberishAccountRegistrationWizard.class);

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

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>GibberishAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public GibberishAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);

        wizardContainer.setFinishButtonText(
            Resources.getString("service.gui.SIGN_IN"));
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
        return Resources.getString("plugin.gibberishaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString(
            "plugin.gibberishaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
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
        Map<String, String> summaryTable = new Hashtable<String, String>();

        summaryTable.put("User ID", registration.getUserID());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(registration.getUserID(), null);
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory
            = GibberishAccRegWizzActivator.getGibberishProtocolProviderFactory();

        return this.installAccount(factory, userName);
    }

    /**
     * Creates an account for the given user and password.
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param user the user identifier 
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String user)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties 
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/gibberish/gibberish32x32.png");

        if (registration.isRememberPassword())
        {
            accountProperties.put(  ProtocolProviderFactory.PASSWORD,
                                    registration.getPassword());
        }

        if (isModification())
        {
            providerFactory.uninstallAccount(protocolProvider.getAccountID());
            this.protocolProvider = null;
            setModification(false);
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
     * Fills the UserID and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        setModification(true);

        this.protocolProvider = protocolProvider;

        this.registration = new GibberishAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     * 
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public GibberishAccountRegistration getRegistration()
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
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return FirstWizardPage.USER_NAME_EXAMPLE;
    }

    /**
     * Indicates whether this wizard enables the simple "sign in" form shown
     * when the user opens the application for the first time. The simple
     * "sign in" form allows user to configure her account in one click, just
     * specifying her username and password and leaving any other configuration
     * as by default.
     * @return <code>true</code> if the simple "Sign in" form is enabled or
     * <code>false</code> otherwise.
     */
    public boolean isSimpleFormEnabled()
    {
        return false;
    }

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @param isCreateAccount indicates if the simple form should be opened as 
     * a create account form or as a login form
     * @return a simple account registration form
     */
    public Object getSimpleForm(boolean isCreateAccount)
    {
        firstWizardPage = new FirstWizardPage(this);
        return firstWizardPage.getSimpleForm();
    }
}
