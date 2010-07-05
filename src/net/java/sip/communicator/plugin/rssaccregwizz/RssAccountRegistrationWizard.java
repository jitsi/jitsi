/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.rssaccregwizz;

import java.awt.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>RssAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Rss protocol. It allows
 * the user to create and configure a new Rss account.
 *
 * @author Emil Ivov
 */
public class RssAccountRegistrationWizard
    implements AccountRegistrationWizard
{
    private final Logger logger
        = Logger.getLogger(RssAccountRegistrationWizard.class);

    /**
     * The first page of the rss account registration wizard.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The object that we use to store details on an account that we will be
     * creating.
     */
    private RssAccountRegistration registration
        = new RssAccountRegistration();

    private final WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    /**
     * Creates an instance of <tt>RssAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public RssAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.ACTIVATE"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.RSS_LOGO);
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
        return Resources.getString("plugin.rssaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.rssaccregwizz.PROTICOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    public Iterator<Map.Entry<String, String>> getSummary()
    {
        Hashtable<String, String> summaryTable 
            = new Hashtable<String, String>();

        summaryTable.put("User ID", registration.getUserID());

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
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory
            = RssAccRegWizzActivator.getRssProtocolProviderFactory();

        return this.installAccount(factory,
                                   userName);
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
            "resources/images/protocol/rss/rss32x32.png");

        accountProperties.put(
            ProtocolProviderFactory.NO_PASSWORD_REQUIRED,
            Boolean.toString(true));

        try
        {
            AccountID accountID = providerFactory.installAccount(
                user, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                RssAccRegWizzActivator.bundleContext
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

        this.protocolProvider = protocolProvider;

        isModification = true;
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
     * 
     * @return the identifier of the page to show last in the wizard.
     */
    public Object getLastPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Indicates if this wizard is modifying an existing account or is creating
     * a new one.
     * 
     * @return <code>true</code> to indicate that this wizard is currently in
     * modification mode, <code>false</code> - otherwise.
     */
    public boolean isModification()
    {
        return isModification;
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
     * Disables the simple "Sign in" form.
     */
    public boolean isSimpleFormEnabled()
    {
        return false;
    }

    /**
     * Nothing to do for RSS here.
     */
    public void webSignup()
    {
        throw new UnsupportedOperationException(
            "The web sign up is not supported by the RSS wizard.");
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
        firstWizardPage = new FirstWizardPage(registration, wizardContainer);
        return firstWizardPage.getSimpleForm();
    }
}
