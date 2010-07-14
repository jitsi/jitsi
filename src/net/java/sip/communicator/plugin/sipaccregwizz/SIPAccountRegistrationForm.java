package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>SIPAccountRegistrationForm</tt>.
 *
 * @author Yana Stamcheva
 */
public class SIPAccountRegistrationForm
    extends TransparentPanel
{
    private final AccountPanel accountPanel;

    private final ConnectionPanel connectionPanel;

    private final PresencePanel presencePanel;

    private boolean isModification;

    private final SIPAccountRegistrationWizard wizard;

    private final JTabbedPane tabbedPane = new SIPCommTabbedPane(false, false);

    /**
     * Creates an instance of <tt>SIPAccountRegistrationForm</tt>.
     * @param wizard the parent wizard
     */
    public SIPAccountRegistrationForm(SIPAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        accountPanel = new AccountPanel(this);

        connectionPanel = new ConnectionPanel(this);

        presencePanel = new PresencePanel();
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        accountPanel.initAdvancedForm();

        CreateAccountService createService = getCreateAccountService();
        if (createService != null)
            createService.clear();

        if (accountPanel.getParent() != tabbedPane)
            tabbedPane.addTab(  Resources.getString("service.gui.ACCOUNT"),
                                accountPanel);

        if (connectionPanel.getParent() != tabbedPane)
            tabbedPane.addTab(Resources.getString("service.gui.CONNECTION"),
                                connectionPanel);

        if (presencePanel.getParent() != tabbedPane)
            tabbedPane.addTab(Resources.getString("service.gui.PRESENCE"),
                                presencePanel);

        if (tabbedPane.getParent() != this)
            this.add(tabbedPane, BorderLayout.NORTH);

        tabbedPane.setSelectedIndex(0);
    }

    /**
     * Parse the server part from the sip id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     * @param userName the account user name
     * @return the server address
     */
    String setServerFieldAccordingToUIN(String userName)
    {
        String serverAddress = getServerFromUserName(userName);

        connectionPanel.setServerFieldAccordingToUIN(serverAddress);

        return serverAddress;
    }

    /**
     * Enables/disables the next/finish button of the parent wizard.
     * @param isEnabled <tt>true</tt> to enable the next button, <tt>false</tt>
     * otherwise
     */
    void setNextFinishButtonEnabled(boolean isEnabled)
    {
        SIPAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
            .setNextFinishButtonEnabled(isEnabled);
    }

    /**
     * Return the server part of the sip user name.
     *
     * @param userName the username.
     * @return the server part of the sip user name.
     */
    private String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(delimIndex + 1);
        }

        return null;
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
     * Saves the user input when the "Next" wizard buttons is clicked.
     * @param registration the SIPAccountRegistration
     */
    public boolean commitPage(SIPAccountRegistration registration)
    {
        String userID = null;
        char[] password = null;
        String serverAddress = null;
        String proxyAddress = null;
        if (accountPanel.isCreateAccount())
        {
            NewAccount newAccount
                = getCreateAccountService().createAccount();
            if (newAccount != null)
            {
                userID = newAccount.getUserName();
                password = newAccount.getPassword();
                serverAddress = newAccount.getServerAddress();
                proxyAddress = newAccount.getProxyAddress();

                if (serverAddress == null)
                    serverAddress = setServerFieldAccordingToUIN(userID);

                if (proxyAddress == null)
                    proxyAddress = serverAddress;
            }
            else
            {
                // If we didn't succeed to create our new account, we have
                // nothing more to do here.
                return false;
            }
        }
        else
        {
            userID = accountPanel.getUserID();
            password = accountPanel.getPassword();
            serverAddress = connectionPanel.getServerAddress();
            proxyAddress = connectionPanel.getProxy();
        }

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);

        if (password != null)
            registration.setPassword(new String(password));

        registration.setRememberPassword(accountPanel.isRememberPassword());

        registration.setServerAddress(serverAddress);
        registration.setProxy(proxyAddress);

        String displayName = accountPanel.getDisplayName();
        registration.setDisplayName(displayName);

        String authName = connectionPanel.getAuthenticationName();
        if(authName != null && authName.length() > 0)
            registration.setAuthorizationName(authName);

        registration.setServerPort(connectionPanel.getServerPort());
        registration.setProxyPort(connectionPanel.getProxyPort());

        registration.setPreferredTransport(
            connectionPanel.getSelectedTransport());

        registration.setEnablePresence(
            presencePanel.isPresenceEnabled());
        registration.setForceP2PMode(
            presencePanel.isForcePeerToPeerMode());
        registration.setDefaultEncryption(
            connectionPanel.isDefaultEncryptionEnabled());
        registration.setSipZrtpAttribute(
            connectionPanel.isSipZrtpEnabled());
        registration.setPollingPeriod(
            presencePanel.getPollPeriod());
        registration.setSubscriptionExpiration(
            presencePanel.getSubscriptionExpiration());
        registration.setKeepAliveMethod(
            connectionPanel.getKeepAliveMethod());
        registration.setKeepAliveInterval(
            connectionPanel.getKeepAliveInterval());

        SIPAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
            .setBackButtonEnabled(true);

        return true;
    }

    /**
     * Loads the account with the given identifier.
     * @param accountID the account identifier
     */
    public void loadAccount(AccountID accountID)
    {
        String password = accountID.getAccountPropertyString(
            ProtocolProviderFactory.PASSWORD);

        String serverAddress = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SERVER_ADDRESS);

        String displayName = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.DISPLAY_NAME);

        String authName = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.AUTHORIZATION_NAME);

        String serverPort = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SERVER_PORT);

        String proxyAddress = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.PROXY_ADDRESS);

        String proxyPort = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.PROXY_PORT);

        String preferredTransport = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.PREFERRED_TRANSPORT);

        boolean enablePresence = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_PRESENCE_ENABLED, false);

        boolean forceP2P = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.FORCE_P2P_MODE, false);

        boolean enabledDefaultEncryption = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION, true);

        boolean enabledSipZrtpAttribute = accountID.getAccountPropertyBoolean(
        ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE, true);

        String pollingPeriod = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.POLLING_PERIOD);

        String subscriptionPeriod = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION);

        String keepAliveMethod =
        accountID.getAccountPropertyString("KEEP_ALIVE_METHOD");

        String keepAliveInterval =
        accountID.getAccountPropertyString("KEEP_ALIVE_INTERVAL");

        connectionPanel.setServerOverridden(
            accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, false));

        accountPanel.setUserIDEnabled(false);
        accountPanel.setUserID((serverAddress == null) ? accountID.getUserID()
            : (accountID.getUserID() + "@" + serverAddress));

        setNextFinishButtonEnabled(accountPanel.getUserID() != null);

        if (password != null)
        {
            accountPanel.setPassword(password);
            accountPanel.setRememberPassword(true);
        }

        connectionPanel.setServerAddress(serverAddress);
        connectionPanel.setServerEnabled(false);

        if (displayName != null && displayName.length() > 0)
            accountPanel.setDisplayName(displayName);

        if(authName != null && authName.length() > 0)
            connectionPanel.setAuthenticationName(authName);

        connectionPanel.setServerPort(serverPort);
        connectionPanel.setProxy(proxyAddress);

        // The order of the next two fields is important, as a change listener
        // of the transportCombo sets the proxyPortField to its default
        connectionPanel.setSelectedTransport(preferredTransport);
        connectionPanel.setProxyPort(proxyPort);

        presencePanel.setPresenceEnabled(enablePresence);
        presencePanel.setForcePeerToPeerMode(forceP2P);

        connectionPanel.enablesDefaultEncryption(enabledDefaultEncryption);
        connectionPanel.setSipZrtpEnabled(  enabledSipZrtpAttribute,
                                            enabledDefaultEncryption);

        presencePanel.setPollPeriod(pollingPeriod);
        presencePanel.setSubscriptionExpiration(subscriptionPeriod);

        if (!enablePresence)
        {
            presencePanel.setPresenceOptionsEnabled(enablePresence);
        }

        connectionPanel.setKeepAliveMethod(keepAliveMethod);
        connectionPanel.setKeepAliveInterval(keepAliveInterval);
    }

    /**
     * Returns a simple version of this registration form.
     * @return the simple form component
     */
    public Component getSimpleForm()
    {
        CreateAccountService createAccountService = getCreateAccountService();
        if (createAccountService != null)
            createAccountService.clear();

        return accountPanel;
    }

    /**
     * Sets the isModification property.
     * @param isModification indicates if this form is created for modification
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }

    /**
     * Returns the username example.
     * @return the username example string
     */
    public String getUsernameExample()
    {
        return wizard.getUserNameExample();
    }

    /**
     * Sign ups through the web.
     */
    public void webSignup()
    {
        wizard.webSignup();
    }

    /**
     * Returns the sign up link name.
     * @return the sign up link name
     */
    public String getWebSignupLinkName()
    {
        return wizard.getWebSignupLinkName();
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    public CreateAccountService getCreateAccountService()
    {
         return wizard.getCreateAccountService();
    }
}
