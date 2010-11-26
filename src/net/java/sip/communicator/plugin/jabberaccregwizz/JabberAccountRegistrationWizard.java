/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.*;

/**
 * The <tt>JabberAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Jabber protocol. It should allow
 * the user to create and configure a new Jabber account.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationWizard
    implements AccountRegistrationWizard
{
    private static final Logger logger =
        Logger.getLogger(JabberAccountRegistrationWizard.class);

    private static final String GOOGLE_USER_SUFFIX = "gmail.com";

    private static final String GOOGLE_CONNECT_SRV = "talk.google.com";

    private FirstWizardPage firstWizardPage;

    private JabberAccountRegistration registration
        = new JabberAccountRegistration();

    private final WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    /**
     * Creates an instance of <tt>JabberAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public JabberAccountRegistrationWizard(WizardContainer wizardContainer)
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
        return Resources.getImage(Resources.PROTOCOL_ICON);
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
        return Resources.getString("plugin.jabberaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources
            .getString("plugin.jabberaccregwizz.PROTOCOL_DESCRIPTION");
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
    public Iterator<Map.Entry<String,String>> getSummary()
    {
        Hashtable<String,String> summaryTable = new Hashtable<String,String>();

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.USERNAME"),
            registration.getUserID());

        summaryTable.put(
            Resources.getString("service.gui.REMEMBER_PASSWORD"),
            Boolean.toString(registration.isRememberPassword()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.SERVER"),
            registration.getServerAddress());

        summaryTable.put(
            Resources.getString("service.gui.PORT"),
            String.valueOf(registration.getPort()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.ENABLE_KEEP_ALIVE"),
            String.valueOf(registration.isSendKeepAlive()));

        summaryTable.put(
            Resources.getString(
                        "plugin.jabberaccregwizz.ENABLE_GMAIL_NOTIFICATIONS"),
            String.valueOf(registration.isGmailNotificationEnabled()));

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.RESOURCE"),
            registration.getResource());

        summaryTable.put(
            Resources.getString("plugin.jabberaccregwizz.PRIORITY"),
            String.valueOf(registration.getPriority()));

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account defined in this wizard.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(  registration.getUserID(),
                        registration.getPassword());
    }

    /**
     * Installs the account defined in this wizard.
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
            = JabberAccRegWizzActivator.getJabberProtocolProviderFactory();

        return this.installAccount(factory,
                                   userName,
                                   password);
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param userName the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String userName,
        String passwd)
        throws OperationFailedException
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Preparing to install account for user " + userName);
        }
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/jabber/logo32x32.png");

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        accountProperties.put("SEND_KEEP_ALIVE",
                              String.valueOf(registration.isSendKeepAlive()));

        accountProperties.put("GMAIL_NOTIFICATIONS_ENABLED",
                    String.valueOf(registration.isGmailNotificationEnabled()));

        String serverName = null;
        if (registration.getServerAddress() != null)
        {
            serverName = registration.getServerAddress();

            if (userName.indexOf(serverName) < 0)
                accountProperties.put(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    Boolean.toString(true));
        }
        else
        {
            serverName = getServerFromUserName(userName);
        }
        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
            serverName);

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                            String.valueOf(registration.getPort()));

        accountProperties.put(ProtocolProviderFactory.RESOURCE,
                            registration.getResource());

        accountProperties.put(ProtocolProviderFactory.RESOURCE_PRIORITY,
                            String.valueOf(registration.getPriority()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_ICE,
                            String.valueOf(registration.isUseIce()));

        accountProperties.put(ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                            String.valueOf(registration.isAutoDiscoverStun()));

        accountProperties.put(ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                String.valueOf(registration.isUseDefaultStunServer()));

        List<StunServerDescriptor> stunServers
            = registration.getAdditionalStunServers();

        int serverIndex = -1;

        for(StunServerDescriptor stunServer : stunServers)
        {
            serverIndex ++;

            stunServer.storeDescriptor(accountProperties,
                            ProtocolProviderFactory.STUN_PREFIX + serverIndex);
        }

        if (isModification)
        {
            providerFactory.modifyAccount(  protocolProvider,
                accountProperties);

            this.isModification  = false;

            return protocolProvider;
        }

        try
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Will install account for user " + userName
                             + " with the following properties."
                             + accountProperties);
            }

            AccountID accountID = providerFactory.installAccount(
                userName,
                accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                JabberAccRegWizzActivator.bundleContext
                .getService(serRef);
        }
        catch (IllegalArgumentException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Username or password is null.",
                OperationFailedException.ILLEGAL_ARGUMENT);
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
     * Fills the User ID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new JabberAccountRegistration();

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
    public JabberAccountRegistration getRegistration()
    {
        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(300, 480);
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
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return AccountPanel.USER_NAME_EXAMPLE;
    }

    /**
     * Enables the simple "Sign in" form.
     *
     * @return <tt>true</tt> if the simple form is enabled and <tt>false</tt>
     * otherwise.
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     *
     * @param userName the full JID that we'd like to parse.
     *
     * @return returns the server part of a full JID
     */
    protected String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            String newServerAddr = userName.substring(delimIndex + 1);
            if (newServerAddr.equals(GOOGLE_USER_SUFFIX))
            {
                return GOOGLE_CONNECT_SRV;
            }
            else
            {
                return newServerAddr;
            }
        }

        return null;
    }

    /**
     * Opens the Gmail signup URI in the OS's default browser.
     */
    public void webSignup()
    {
        JabberAccRegWizzActivator.getBrowserLauncher()
            .openURL("http://mail.google.com/mail/signup");
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return true;
    }

    /**
     * Returns the first wizard page.
     *
     * @return the first wizard page.
     */
    public Object getSimpleForm()
    {
        firstWizardPage = new FirstWizardPage(this);
        return firstWizardPage.getSimpleForm();
    }
}
