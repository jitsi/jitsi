package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

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
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JabberAccountCreationFormService createService = getCreateAccountService();
        if (createService != null)
            createService.clear();

        if (accountPanel.getParent() != tabbedPane)
            tabbedPane.addTab(  Resources.getString("service.gui.ACCOUNT"),
                                accountPanel);

        if (connectionPanel.getParent() != tabbedPane)
            tabbedPane.addTab(Resources.getString("service.gui.CONNECTION"),
                                connectionPanel);

        if (iceConfigPanel.getParent() != tabbedPane)
            tabbedPane.addTab(Resources.getString("service.gui.PRESENCE"),
                                iceConfigPanel);

        if (tabbedPane.getParent() != this)
            this.add(tabbedPane, BorderLayout.NORTH);

        tabbedPane.setSelectedIndex(0);
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
        String userID = accountPanel.getUsername();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        if(userID.indexOf('@') < 0
           && registration.getDefaultUserSufix() != null)
            userID = userID + '@' + registration.getDefaultUserSufix();

        registration.setUserID(userID);
        registration.setPassword(new String(accountPanel.getPassword()));
        registration.setRememberPassword(accountPanel.isRememberPassword());

        registration.setServerAddress(connectionPanel.getServerAddress());
        registration.setSendKeepAlive(connectionPanel.isSendKeepAlive());
        registration.setGmailNotificationEnabled(
            connectionPanel.isGmailNotificationsEnabled());
        registration.setGoogleContactsEnabled(
                connectionPanel.isGoogleContactsEnabled());
        registration.setResource(connectionPanel.getResource());

        String serverPort = connectionPanel.getServerPort();
        if (serverPort != null)
            registration.setPort(Integer.parseInt(serverPort));

        String priority = connectionPanel.getPriority();
        if (priority != null)
            registration.setPriority(Integer.parseInt(priority));

        registration.setUseIce(iceConfigPanel.isUseIce());
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

	String useGC = 
		accountProperties.get("GOOGLE_CONTACTS_ENABLED");
        boolean googleContactsEnabled = Boolean.parseBoolean(
		(useGC != null && useGC.length() != 0) ? useGC : "true"); 

        connectionPanel.setGoogleContactsEnabled(googleContactsEnabled);

        String resource
            = accountProperties.get(ProtocolProviderFactory.RESOURCE);

        connectionPanel.setResource(resource);

        String priority
            = accountProperties.get(ProtocolProviderFactory.RESOURCE_PRIORITY);

        connectionPanel.setPriority(priority);

        String useIce =
            accountProperties.get(ProtocolProviderFactory.IS_USE_ICE);
        boolean isUseIce = Boolean.parseBoolean(
                (useIce != null && useIce.length() != 0) ? useIce : "true");

        iceConfigPanel.setUseIce(isUseIce);

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
                (useUPNP != null && useUPNP.length() != 0) ? useUPNP : "false");

        iceConfigPanel.setUseUPNP(isUseUPNP);

        wizard.getRegistration().setServerOverridden(
            accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    false));
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
     * Return the string for home page link label.
     * @return the string for home page link label
     */
    protected String getHomeLinkLabel()
    {
        return wizard.getHomeLinkLabel();
    }
}
