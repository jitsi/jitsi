/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>IPPIAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new SIP account.
 *
 * @author Yana Stamcheva
 */
public class SIPAccountRegistrationWizard
    implements AccountRegistrationWizard
{
    /**
     * The first wizard page.
     */
    private FirstWizardPage firstWizardPage;

    /**
     * Account registration instance holding the data.
     */
    private SIPAccountRegistration registration
        = new SIPAccountRegistration();

    /**
     * The container.
     */
    private final WizardContainer wizardContainer;

    /**
     * The protocol provider.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * Is current wizard run as modification of an existing account.
     */
    private boolean isModification;

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(SIPAccountRegistrationWizard.class);

    /**
     * Creates an instance of <tt>IPPIAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public SIPAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer.setFinishButtonText(
            Resources.getString("service.gui.SIGN_IN"));
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
    public String getProtocolName() {
        return Resources.getString("plugin.sipaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription() {
        return Resources.getString("plugin.sipaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages() {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();

        // If the first wizard page was already created
        if (firstWizardPage == null)
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
        Hashtable<String, String> summaryTable
            = new Hashtable<String, String>();

        boolean rememberPswd = registration.isRememberPassword();
        String rememberPswdString = Resources.getString(
                rememberPswd ? "service.gui.YES" : "service.gui.NO");
        String displayName = registration.getDisplayName();
        if(displayName != null)
            summaryTable.put(
                        Resources.getString("plugin.sipaccregwizz.DISPLAY_NAME"),
                        displayName);
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.USERNAME"),
            registration.getId());
        summaryTable.put(
            Resources.getString("service.gui.REMEMBER_PASSWORD"),
            rememberPswdString);
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.REGISTRAR"),
            registration.getServerAddress());
        String authName = registration.getAuthorizationName();
        if(authName != null)
            summaryTable.put(
                        Resources.getString("plugin.sipaccregwizz.AUTH_NAME"),
                        authName);
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.SERVER_PORT"),
            registration.getServerPort());
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.PROXY"),
            registration.getProxy());
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.PROXY_PORT"),
            registration.getProxyPort());
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.PREFERRED_TRANSPORT"),
            registration.getPreferredTransport());

        if (registration.isEnablePresence())
        {
            summaryTable.put(
                Resources.getString("plugin.sipaccregwizz.ENABLE_PRESENCE"),
                Resources.getString("service.gui.YES"));
        }
        else
        {
            summaryTable.put(
                Resources.getString("plugin.sipaccregwizz.ENABLE_PRESENCE"),
                Resources.getString("service.gui.NO"));
        }

        if (registration.isForceP2PMode())
        {
            summaryTable.put(
                Resources.getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"),
                Resources.getString("service.gui.YES"));
        }
        else
        {
            summaryTable.put(
                Resources.getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"),
                Resources.getString("service.gui.NO"));
        }

        if (registration.isDefaultEncryption())
        {
            summaryTable.put(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
                Resources.getString("service.gui.YES"));
        }
        else
        {
            summaryTable.put(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
                Resources.getString("service.gui.NO"));
        }

        if (registration.isSipZrtpAttribute())
        {
            summaryTable.put(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
                Resources.getString("service.gui.YES"));
        }
        else
        {
            summaryTable.put(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
                Resources.getString("service.gui.NO"));
        }

        summaryTable.put(Resources.getString(
                "plugin.sipaccregwizz.OFFLINE_CONTACT_POLLING_PERIOD"),
                registration.getPollingPeriod());
        summaryTable.put(Resources.getString(
                "plugin.sipaccregwizz.SUBSCRIPTION_EXPIRATION"),
                registration.getSubscriptionExpiration());

        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_METHOD"),
            registration.getKeepAliveMethod());
        summaryTable.put(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_INTERVAL"),
            registration.getKeepAliveInterval());

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return ProtocolProviderService
     * @throws OperationFailedException problem signing in.
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        if (firstWizardPage.isCommitted())
            return signin(registration.getId(), registration.getPassword());

        return null;
    }

    /**
     * Installs the account with the given user name and password.
     * @param userName the account user name
     * @param password the password
     * @return the <tt>ProtocolProviderService</tt> corresponding to the newly
     * created account.
     * @throws OperationFailedException problem signing in.
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory
            = SIPAccRegWizzActivator.getSIPProtocolProviderFactory();

        ProtocolProviderService pps = null;
        if (factory != null)
            pps = this.installAccount(  factory,
                                        userName,
                                        password);

        return pps;
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param userName the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException problem installing account
     */
    private ProtocolProviderService installAccount(
            ProtocolProviderFactory providerFactory,
            String userName,
            String passwd)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.PROTOCOL, getProtocol());
        String protocolIconPath = getProtocolIconPath();
        if (protocolIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                                    protocolIconPath);

        String accountIconPath = getAccountIconPath();
        if (accountIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                                    accountIconPath);

        if(registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        String serverAddress = null;
        if (registration.getServerAddress() != null)
            serverAddress = registration.getServerAddress();

        if (serverAddress != null)
        {
            accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                serverAddress);

            if (userName.indexOf(serverAddress) < 0)
                accountProperties.put(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    Boolean.toString(true));
        }

        if(registration.getDisplayName() != null)
            accountProperties.put(ProtocolProviderFactory.DISPLAY_NAME,
                registration.getDisplayName());

        if(registration.getAuthorizationName() != null)
            accountProperties.put(ProtocolProviderFactory.AUTHORIZATION_NAME,
                registration.getAuthorizationName());

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                registration.getServerPort());

        String proxyAddress = null;
        if (registration.getProxy() != null)
            proxyAddress = registration.getProxy();

        if (proxyAddress != null)
            accountProperties.put(ProtocolProviderFactory.PROXY_ADDRESS,
                proxyAddress);

        accountProperties.put(ProtocolProviderFactory.PROXY_PORT,
                registration.getProxyPort());

        accountProperties.put(ProtocolProviderFactory.PREFERRED_TRANSPORT,
                registration.getPreferredTransport());

        accountProperties.put(ProtocolProviderFactory.IS_PRESENCE_ENABLED,
                Boolean.toString(registration.isEnablePresence()));

        // when we are creating registerless account make sure that
        // we don't use PA
        if(serverAddress != null)
        {
            accountProperties.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                Boolean.toString(registration.isForceP2PMode()));
        }
        else
        {
            accountProperties.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                Boolean.TRUE.toString());
        }

        accountProperties.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                Boolean.toString(registration.isDefaultEncryption()));

        accountProperties.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                Boolean.toString(registration.isSipZrtpAttribute()));

        accountProperties.put(ProtocolProviderFactory.POLLING_PERIOD,
                registration.getPollingPeriod());

        accountProperties.put(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION,
                registration.getSubscriptionExpiration());

        accountProperties.put("KEEP_ALIVE_METHOD",
                registration.getKeepAliveMethod());

        accountProperties.put("KEEP_ALIVE_INTERVAL",
            registration.getKeepAliveInterval());

        if(isModification)
        {
            accountProperties.put(ProtocolProviderFactory.USER_ID, userName);
            providerFactory.modifyAccount(  protocolProvider,
                                            accountProperties);

            this.isModification  = false;

            return protocolProvider;
        }

        try
        {
            AccountID accountID = providerFactory.installAccount(
                    userName, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider
                = (ProtocolProviderService) SIPAccRegWizzActivator.bundleContext
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
                exc.getMessage(),
                OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Fills the id and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new SIPAccountRegistration();

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
    public SIPAccountRegistration getRegistration()
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
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return "Ex: john@voiphone.net or simply \"john\" for no server";
    }

    /**
     * Enables the simple "Sign in" form.
     * @return <tt>true</tt> to indicate that the simple form is enabled
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Sign ups through the web.
     */
    public void webSignup() {}

    /**
     * Returns the name of the web sign up link.
     * @return the name of the web sign up link
     */
    public String getWebSignupLinkName()
    {
        return null;
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
     * Returns the simple form.
     * @return the simple form
     */
    public Object getSimpleForm()
    {
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
        return ProtocolNames.SIP;
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
    protected CreateAccountService getCreateAccountService()
    {
        return null;
    }
}
