package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>JabberAccountRegistrationForm</tt>.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final AccountPanel accountPanel;

    private final ConnectionPanel connectionPanel;

    private final IceConfigPanel iceConfigPanel;

    private final TelephonyConfigPanel telephonyConfigPanel;

    private boolean isModification;

    private final JabberAccountRegistrationWizard wizard;

    private final JTabbedPane tabbedPane = new SIPCommTabbedPane();

    /**
     * The panels which value needs validation before we continue.
     */
    private List<ValidatingPanel> validatingPanels =
            new ArrayList<ValidatingPanel>();

    /**
     * Creates an instance of <tt>JabberAccountRegistrationForm</tt>.
     * @param wizard the parent wizard
     */
    public JabberAccountRegistrationForm(JabberAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        accountPanel = new AccountPanel(this);

        connectionPanel = new ConnectionPanel(this);

        iceConfigPanel = new IceConfigPanel();

        telephonyConfigPanel = new TelephonyConfigPanel();
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JabberAccountCreationFormService createService =
            getCreateAccountService();
        if (createService != null)
            createService.clear();

        if (!JabberAccRegWizzActivator.isAdvancedAccountConfigDisabled())
        {
            // Indicate that this panel is opened in a simple form.
            accountPanel.setSimpleForm(false);

            if (accountPanel.getParent() != tabbedPane)
                tabbedPane.addTab(  Resources.getString("service.gui.ACCOUNT"),
                                    accountPanel);

            if (connectionPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.CONNECTION"),
                                    connectionPanel);

            if (iceConfigPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.ICE"),
                                    iceConfigPanel);

            if (telephonyConfigPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.TELEPHONY"),
                                    telephonyConfigPanel);

            if (tabbedPane.getParent() != this)
                this.add(tabbedPane, BorderLayout.NORTH);

            tabbedPane.setSelectedIndex(0);
        }
        else
            add(accountPanel, BorderLayout.NORTH);
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     * @param userName the account user name
     */
    void setServerFieldAccordingToUIN(String userName)
    {
        if (!wizard.isModification()
            && !wizard.getRegistration().isServerOverridden())
        {
            connectionPanel.setServerAddress(getServerFromUserName(userName));
        }
    }

    /**
     * Enables/disables the next/finish button of the parent wizard.
     * @param isEnabled <tt>true</tt> to enable the next button, <tt>false</tt>
     * otherwise
     */
    private void setNextFinishButtonEnabled(boolean isEnabled)
    {
        JabberAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
            .setNextFinishButtonEnabled(isEnabled);
    }

    /**
     * Call this to trigger revalidation of all the input values
     * and change the state of next/finish button.
     */
    void reValidateInput()
    {
        for(ValidatingPanel panel : validatingPanels)
        {
            if(!panel.isValidated())
            {
                setNextFinishButtonEnabled(false);
                return;
            }
        }

        setNextFinishButtonEnabled(true);
    }

    /**
     * Adds panel to the list of panels with values which need validation.
     * @param panel ValidatingPanel.
     */
    public void addValidatingPanel(ValidatingPanel panel)
    {
        validatingPanels.add(panel);
    }

    /**
     * Return the server part of the jabber user name.
     *
     * @param userName the username.
     * @return the server part of the jabber user name.
     */
    static String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(delimIndex + 1);
        }

        return null;
    }

    /**
     * Return the user part of the user name (i.e. the string before the @
     * sign).
     *
     * @param userName the username.
     * @return the user part of the jabber user name.
     */
    public static String getUserFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(0, delimIndex);
        }

        return userName;
    }

    /**
     * Returns the server address.
     *
     * @return the server address
     */
    String getServerAddress()
    {
        return wizard.getRegistration().getServerAddress();
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
     *
     * @param registration the JabberAccountRegistration
     * @return <tt>true</tt> if the page is correctly committed
     */
    public boolean commitPage(JabberAccountRegistration registration)
    {
        String userID = null;
        char[] password = null;
        String serverAddress = null;
        String serverPort = null;

        if (accountPanel.isCreateAccount())
        {
            NewAccount newAccount
                = getCreateAccountService().createAccount();

            if (newAccount != null)
            {
                userID = newAccount.getUserName();
                password = newAccount.getPassword();
                serverAddress = newAccount.getServerAddress();
                serverPort = newAccount.getServerPort();

                if (serverAddress == null)
                    setServerFieldAccordingToUIN(userID);
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
            userID = accountPanel.getUsername();

            if(userID == null || userID.trim().length() == 0)
                throw new IllegalStateException("No user ID provided.");

            if(userID.indexOf('@') < 0
               && registration.getDefaultUserSufix() != null)
                userID = userID + '@' + registration.getDefaultUserSufix();

            password = accountPanel.getPassword();
            serverAddress = connectionPanel.getServerAddress();
            serverPort = connectionPanel.getServerPort();
        }

        registration.setUserID(userID);
        registration.setPassword(new String(password));
        registration.setRememberPassword(accountPanel.isRememberPassword());
        registration.setServerAddress(serverAddress);
        registration.setServerOverridden(connectionPanel.isServerOverridden());
        registration.setSendKeepAlive(connectionPanel.isSendKeepAlive());
        registration.setGmailNotificationEnabled(
            connectionPanel.isGmailNotificationsEnabled());
        registration.setGoogleContactsEnabled(
                connectionPanel.isGoogleContactsEnabled());
        registration.setResourceAutogenerated(
            connectionPanel.isAutogenerateResourceEnabled());
        registration.setResource(connectionPanel.getResource());

        if (serverPort != null)
            registration.setPort(Integer.parseInt(serverPort));

        String priority = connectionPanel.getPriority();
        if (priority != null)
            registration.setPriority(Integer.parseInt(priority));

        registration.setDTMFMethod(connectionPanel.getDTMFMethod());

        registration.setUseIce(iceConfigPanel.isUseIce());
        registration.setUseGoogleIce(iceConfigPanel.isUseGoogleIce());
        registration.setAutoDiscoverStun(iceConfigPanel.isAutoDiscoverStun());
        registration.setUseDefaultStunServer(
                iceConfigPanel.isUseDefaultStunServer());

        //we will be reentering all stun servers so let's make sure we clear
        //the servers vector in case we already did that with a "Next".
        registration.getAdditionalStunServers().clear();

        List<StunServerDescriptor> stunServers
            = iceConfigPanel.getAdditionalStunServers();

        for (StunServerDescriptor descriptor : stunServers)
            registration.addStunServer(descriptor);

        registration.setUseJingleNodes(iceConfigPanel.isUseJingleNodes());
        registration.setAutoDiscoverJingleNodes(
                iceConfigPanel.isAutoDiscoverJingleNodes());

        //we will be reentering all Jingle nodes so let's make sure we clear
        //the servers vector in case we already did that with a "Next".
        registration.getAdditionalJingleNodes().clear();

        List<JingleNodeDescriptor> jingleNodes
            = iceConfigPanel.getAdditionalJingleNodes();

        for (JingleNodeDescriptor descriptor : jingleNodes)
            registration.addJingleNodes(descriptor);

        registration.setUseUPNP(iceConfigPanel.isUseUPNP());

        registration.setAllowNonSecure(connectionPanel.isAllowNonSecure());

        registration.setTelephonyDomainBypassCaps(
            telephonyConfigPanel.getTelephonyDomainBypassCaps());
        registration.setOverridePhoneSufix(
            telephonyConfigPanel.getTelephonyDomain());
        return true;
    }

    /**
     * Loads the account with the given identifier.
     * @param accountID the account identifier
     */
    public void loadAccount(AccountID accountID)
    {
        Map<String, String> accountProperties
            = accountID.getAccountProperties();

        String password
            = JabberAccRegWizzActivator.getJabberProtocolProviderFactory()
                .loadPassword(accountID);

        accountPanel.setRememberPassword(false);
        accountPanel.setUsername(accountID.getUserID());

        if (password != null)
        {
            accountPanel.setPassword(password);
            accountPanel.setRememberPassword(true);
        }

        String serverAddress
            = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        connectionPanel.setServerAddress(serverAddress);

        String serverPort
            = accountProperties.get(ProtocolProviderFactory.SERVER_PORT);

        connectionPanel.setServerPort(serverPort);

        boolean keepAlive
            = Boolean.parseBoolean(accountProperties.get("SEND_KEEP_ALIVE"));

        connectionPanel.setSendKeepAlive(keepAlive);

        boolean gmailNotificationEnabled
            = Boolean.parseBoolean(
                    accountProperties.get("GMAIL_NOTIFICATIONS_ENABLED"));

        connectionPanel.setGmailNotificationsEnabled(gmailNotificationEnabled);

        String useGC = accountProperties.get("GOOGLE_CONTACTS_ENABLED");

        boolean googleContactsEnabled = Boolean.parseBoolean(
            (useGC != null && useGC.length() != 0) ? useGC : "true");

        connectionPanel.setGoogleContactsEnabled(googleContactsEnabled);

        String resource
            = accountProperties.get(ProtocolProviderFactory.RESOURCE);

        connectionPanel.setResource(resource);

        String autoGenerateResourceValue = accountProperties.get(
            ProtocolProviderFactory.AUTO_GENERATE_RESOURCE);

        boolean autoGenerateResource =
            JabberAccountRegistration.DEFAULT_RESOURCE_AUTOGEN;

        if(autoGenerateResourceValue != null)
            autoGenerateResource = Boolean.parseBoolean(
                autoGenerateResourceValue);

        connectionPanel.setAutogenerateResource(autoGenerateResource);

        String priority
            = accountProperties.get(ProtocolProviderFactory.RESOURCE_PRIORITY);

        connectionPanel.setPriority(priority);

        connectionPanel.setDTMFMethod(
            accountID.getAccountPropertyString("DTMF_METHOD"));

        String useIce =
            accountProperties.get(ProtocolProviderFactory.IS_USE_ICE);
        boolean isUseIce = Boolean.parseBoolean(
                (useIce != null && useIce.length() != 0) ? useIce : "true");

        iceConfigPanel.setUseIce(isUseIce);

        String useGoogleIce =
            accountProperties.get(ProtocolProviderFactory.IS_USE_GOOGLE_ICE);
        boolean isUseGoogleIce = Boolean.parseBoolean(
                (useGoogleIce != null && useGoogleIce.length() != 0) ?
                    useGoogleIce : "true");

        iceConfigPanel.setUseGoogleIce(isUseGoogleIce);

        String useAutoDiscoverStun
            = accountProperties.get(
                    ProtocolProviderFactory.AUTO_DISCOVER_STUN);
        boolean isUseAutoDiscoverStun = Boolean.parseBoolean(
                (useAutoDiscoverStun != null &&
                        useAutoDiscoverStun.length() != 0) ?
                                useAutoDiscoverStun : "true");

        iceConfigPanel.setAutoDiscoverStun(isUseAutoDiscoverStun);

        String useDefaultStun
            = accountProperties.get(
                ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER);
        boolean isUseDefaultStun = Boolean.parseBoolean(
            (useDefaultStun != null &&
                    useDefaultStun.length() != 0) ?
                            useDefaultStun : "true");

        iceConfigPanel.setUseDefaultStunServer(isUseDefaultStun);

        iceConfigPanel.removeAllStunServer();
        for (int i = 0; i < StunServerDescriptor.MAX_STUN_SERVER_COUNT; i ++)
        {
            StunServerDescriptor stunServer
                = StunServerDescriptor.loadDescriptor(
                    accountProperties, ProtocolProviderFactory.STUN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (stunServer == null)
                break;

            String stunPassword = loadStunPassword(accountID,
                    ProtocolProviderFactory.STUN_PREFIX + i);

            if(stunPassword != null)
            {
                stunServer.setPassword(stunPassword);
            }

            iceConfigPanel.addStunServer(stunServer);
        }

        String useJN =
            accountProperties.get(ProtocolProviderFactory.IS_USE_JINGLE_NODES);
        boolean isUseJN = Boolean.parseBoolean(
                (useJN != null && useJN.length() != 0) ? useJN : "true");

        iceConfigPanel.setUseJingleNodes(isUseJN);

        String useAutoDiscoverJN
            = accountProperties.get(
                    ProtocolProviderFactory.AUTO_DISCOVER_JINGLE_NODES);
        boolean isUseAutoDiscoverJN = Boolean.parseBoolean(
                (useAutoDiscoverJN != null &&
                        useAutoDiscoverJN.length() != 0) ?
                                useAutoDiscoverJN : "true");

        iceConfigPanel.setAutoDiscoverJingleNodes(isUseAutoDiscoverJN);

        iceConfigPanel.removeAllJingleNodes();
        for (int i = 0; i < JingleNodeDescriptor.MAX_JN_RELAY_COUNT ; i ++)
        {
            JingleNodeDescriptor jn
                = JingleNodeDescriptor.loadDescriptor(
                    accountProperties, JingleNodeDescriptor.JN_PREFIX + i);

            // If we don't find a stun server with the given index, it means
            // that there're no more servers left in the table so we've nothing
            // more to do here.
            if (jn == null)
                break;

            iceConfigPanel.addJingleNodes(jn);
        }

        String useUPNP =
            accountProperties.get(ProtocolProviderFactory.IS_USE_UPNP);
        boolean isUseUPNP = Boolean.parseBoolean(
                (useUPNP != null && useUPNP.length() != 0) ? useUPNP : "true");

        iceConfigPanel.setUseUPNP(isUseUPNP);

        String allowNonSecure =
            accountProperties.get(ProtocolProviderFactory.IS_ALLOW_NON_SECURE);
        boolean isAllowNonSecure = Boolean.parseBoolean(
                (allowNonSecure != null && allowNonSecure.length() != 0)
                ? allowNonSecure : "false");

        connectionPanel.setAllowNonSecure(isAllowNonSecure);

        boolean isServerOverriden =
            accountID.getAccountPropertyBoolean(
                                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                                false);

        connectionPanel.setServerOverridden(isServerOverriden);

        String telephonyDomain = accountProperties.get("OVERRIDE_PHONE_SUFFIX");
        telephonyConfigPanel.setTelephonyDomain(telephonyDomain);

        String bypassCapsDomain = accountProperties.get(
            "TELEPHONY_BYPASS_GTALK_CAPS");
        telephonyConfigPanel.setTelephonyDomainBypassCaps(bypassCapsDomain);
    }

    /**
     * Returns a simple version of this registration form.
     * @return the simple form component
     */
    public Component getSimpleForm()
    {
        JabberAccountCreationFormService createAccountService
            = getCreateAccountService();

        if (createAccountService != null)
            createAccountService.clear();

        // Indicate that this panel is opened in a simple form.
        accountPanel.setSimpleForm(true);

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
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return wizard.isWebSignupSupported();
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    public JabberAccountCreationFormService getCreateAccountService()
    {
         return wizard.getCreateAccountService();
    }

    /**
     * Returns the display label used for the jabber id field.
     * @return the jabber id display label string.
     */
    protected String getUsernameLabel()
    {
        return wizard.getUsernameLabel();
    }

    /**
     * Returns the current jabber registration holding all values.
     * @return jabber registration.
     */
    public JabberAccountRegistration getRegistration()
    {
        return wizard.getRegistration();
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getCreateAccountButtonLabel()
    {
        return wizard.getCreateAccountButtonLabel();
    }

    /**
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return wizard.getCreateAccountLabel();
    }

    /**
     * Return the string for add existing account button.
     * @return the string for add existing account button.
     */
    protected String getExistingAccountLabel()
    {
        return wizard.getExistingAccountLabel();
    }

    /**
     * Return the string for home page link label.
     * @return the string for home page link label
     */
    protected String getHomeLinkLabel()
    {
        return wizard.getHomeLinkLabel();
    }

    /**
     * Load password for this STUN descriptor.
     *
     * @param accountID account ID
     * @param namePrefix name prefix
     * @return password or null if empty
     */
    private static String loadStunPassword(AccountID accountID,
            String namePrefix)
    {
        ProtocolProviderFactory providerFactory =
            JabberAccRegWizzActivator.getJabberProtocolProviderFactory();
        String password = null;
        String className = providerFactory.getClass().getName();
        String packageSourceName = className.substring(0,
                className.lastIndexOf('.'));

        String accountPrefix = ProtocolProviderFactory.findAccountPrefix(
                JabberAccRegWizzActivator.bundleContext,
                accountID, packageSourceName);

        CredentialsStorageService credentialsService =
            JabberAccRegWizzActivator.getCredentialsService();

        try
        {
            password = credentialsService.
                loadPassword(accountPrefix + "." + namePrefix);
        }
        catch(Exception e)
        {
            return null;
        }

        return password;
    }
}
