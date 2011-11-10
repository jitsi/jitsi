/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>JabberAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the Jabber protocol. It should allow
 * the user to create and configure a new Jabber account.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationWizard
    extends AccountRegistrationWizard
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(JabberAccountRegistrationWizard.class);

    /**
     * Account suffix for Google service.
     */
    private static final String GOOGLE_USER_SUFFIX = "gmail.com";

    /**
     * XMPP server for Google service.
     */
    private static final String GOOGLE_CONNECT_SRV = "talk.google.com";

    /**
     * The first wizard page.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * The registration object, where all properties related to the account
     * are stored.
     */
    private JabberAccountRegistration registration;

    /**
     * The <tt>ProtocolProviderService</tt> of this account.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The create account form.
     */
    private JabberAccountCreationForm createAccountService;

    /**
     * Creates an instance of <tt>JabberAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public JabberAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        setWizardContainer(wizardContainer);

        wizardContainer
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
        return getPages(new JabberAccountRegistration());
    }

    /**
     * Returns the set of pages contained in this wizard.
     *
     * @param registration the registration object
     * @return Iterator
     */
    public Iterator<WizardPage> getPages(JabberAccountRegistration registration)
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();

        // create new registration, our container needs the pages
        // this means this is a new wizard and we must reset all data
        // it will be invoked and when the wizard cleans and unregister
        // our pages, but this fix don't hurt in this situation.
        this.registration = registration;

        if (firstWizardPage == null)
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
        // if firstWizardPage is null we are requested sign-in from
        // initial account registration form we must init
        // firstWizardPage in order to init default values
        if(firstWizardPage == null)
        {
            firstWizardPage = new FirstWizardPage(this);
            AccountPanel accPanel =
                    (AccountPanel)firstWizardPage.getSimpleForm();
            accPanel.setUsername(userName);
            accPanel.setPassword(password);
            accPanel.setRememberPassword(true);
        }

        if(!firstWizardPage.isCommitted())
            firstWizardPage.commitPage();

        if(!firstWizardPage.isCommitted())
            throw new OperationFailedException("Could not confirm data.",
                OperationFailedException.GENERAL_ERROR);

        ProtocolProviderFactory factory
            = JabberAccRegWizzActivator.getJabberProtocolProviderFactory();

        return this.installAccount(
            factory,
            registration.getUserID(),  // The user id may get changed.Server
                                       // part can be added in the data
                                       // commit.
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
    protected ProtocolProviderService installAccount(
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

        accountProperties.put(ProtocolProviderFactory.IS_PREFERRED_PROTOCOL,
            Boolean.toString(isPreferredProtocol()));
        accountProperties.put(ProtocolProviderFactory.PROTOCOL, getProtocol());
        String protocolIconPath = getProtocolIconPath();
        if (protocolIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                                    protocolIconPath);

        String accountIconPath = getAccountIconPath();
        if (accountIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                                    accountIconPath);

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        accountProperties.put("SEND_KEEP_ALIVE",
                              String.valueOf(registration.isSendKeepAlive()));

        accountProperties.put("GMAIL_NOTIFICATIONS_ENABLED",
                    String.valueOf(registration.isGmailNotificationEnabled()));
        accountProperties.put("GOOGLE_CONTACTS_ENABLED",
                String.valueOf(registration.isGoogleContactsEnabled()));

        String serverName = null;
        if (registration.getServerAddress() != null
            && registration.getServerAddress().length() > 0)
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

        if (serverName == null || serverName.length() <= 0)
            throw new OperationFailedException(
                "Should specify a server for user name " + userName + ".",
                OperationFailedException.SERVER_NOT_SPECIFIED);

        if(userName.indexOf('@') < 0
           && registration.getDefaultUserSufix() != null)
            userName = userName + '@' + registration.getDefaultUserSufix();

        if(registration.getOverridePhoneSuffix() != null)
        {
            accountProperties.put("OVERRIDE_PHONE_SUFFIX",
                registration.getOverridePhoneSuffix());
        }

        accountProperties.put("BYPASS_GTALK_CAPABILITIES",
            String.valueOf(registration.getBypassGtalkCaps()));

        if(registration.getTelephonyDomainBypassCaps() != null)
        {
            accountProperties.put("TELEPHONY_BYPASS_GTALK_CAPS",
                registration.getTelephonyDomainBypassCaps());
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
            serverName);

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                            String.valueOf(registration.getPort()));

        accountProperties.put(ProtocolProviderFactory.AUTO_GENERATE_RESOURCE,
                        String.valueOf(registration.isResourceAutogenerated()));

        accountProperties.put(ProtocolProviderFactory.RESOURCE,
                            registration.getResource());

        accountProperties.put(ProtocolProviderFactory.RESOURCE_PRIORITY,
                            String.valueOf(registration.getPriority()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_ICE,
                            String.valueOf(registration.isUseIce()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_GOOGLE_ICE,
            String.valueOf(registration.isUseGoogleIce()));

        accountProperties.put(ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                            String.valueOf(registration.isAutoDiscoverStun()));

        accountProperties.put(ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                String.valueOf(registration.isUseDefaultStunServer()));

        String accountDisplayName = registration.getAccountDisplayName();

        if (accountDisplayName != null && accountDisplayName.length() > 0)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
                                    accountDisplayName);

        List<StunServerDescriptor> stunServers
            = registration.getAdditionalStunServers();

        int serverIndex = -1;

        for(StunServerDescriptor stunServer : stunServers)
        {
            serverIndex ++;

            stunServer.storeDescriptor(accountProperties,
                            ProtocolProviderFactory.STUN_PREFIX + serverIndex);
        }

        accountProperties.put(ProtocolProviderFactory.IS_USE_JINGLE_NODES,
                String.valueOf(registration.isUseJingleNodes()));

        accountProperties.put(
                ProtocolProviderFactory.AUTO_DISCOVER_JINGLE_NODES,
                String.valueOf(registration.isAutoDiscoverJingleNodes()));

        List<JingleNodeDescriptor> jnRelays
            = registration.getAdditionalJingleNodes();

        serverIndex = -1;
        for(JingleNodeDescriptor jnRelay : jnRelays)
        {
            serverIndex ++;

            jnRelay.storeDescriptor(accountProperties,
                            JingleNodeDescriptor.JN_PREFIX + serverIndex);
        }

        accountProperties.put(ProtocolProviderFactory.IS_USE_UPNP,
                String.valueOf(registration.isUseUPNP()));

        accountProperties.put(ProtocolProviderFactory.IS_ALLOW_NON_SECURE,
            String.valueOf(registration.isAllowNonSecure()));

        if (isModification())
        {
            providerFactory.modifyAccount(  protocolProvider,
                accountProperties);

            setModification(false);

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
                "Username, password or server is null.",
                OperationFailedException.ILLEGAL_ARGUMENT);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Throwable exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Failed to add account.",
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
        setModification(true);

        this.protocolProvider = protocolProvider;

        this.registration = new JabberAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
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
        if (registration == null)
            registration = new JabberAccountRegistration();

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
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return "Ex: johnsmith@jabber.org";
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
        // when creating first wizard page, create and new
        // AccountRegistration to avoid reusing old instances and
        // data left from old registrations
        return getSimpleForm(new JabberAccountRegistration(), isCreateAccount);
    }

    /**
     * Returns the first wizard page.
     *
     * @param registration the registration object
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return the first wizard page.
     */
    public Object getSimpleForm(JabberAccountRegistration registration,
                                boolean isCreateAccount)
    {
        this.registration = registration;

        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }

    /**
     * Returns the protocol name as listed in "ProtocolNames" or just the name
     * of the service.
     * @return the protocol name
     */
    public String getProtocol()
    {
        return ProtocolNames.JABBER;
    }

    /**
     * Returns the protocol icon path.
     * @return the protocol icon path
     */
    public String getProtocolIconPath()
    {
        return null;
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return null;
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    protected JabberAccountCreationFormService getCreateAccountService()
    {
        if (createAccountService == null)
            createAccountService = new JabberAccountCreationForm();

        return createAccountService;
    }

    /**
     * Returns the display label used for the jabber id field.
     * @return the jabber id display label string.
     */
    protected String getUsernameLabel()
    {
        return Resources.getString("plugin.jabberaccregwizz.USERNAME");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return Resources.getString(
            "plugin.jabberaccregwizz.NEW_ACCOUNT_TITLE");
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return Resources.getString(
            "plugin.jabberaccregwizz.REGISTER_NEW_ACCOUNT_TEXT");
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getExistingAccountLabel()
    {
        return Resources.getString("plugin.jabberaccregwizz.EXISTING_ACCOUNT");
    }

    /**
     * Return the string for home page link label.
     * @return the string for home page link label
     */
    protected String getHomeLinkLabel()
    {
        return null;
    }
}