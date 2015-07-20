/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.util.StringUtils;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.wizard.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;

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

    private final SecurityPanel securityPanel;

    private final IceConfigPanel iceConfigPanel;

    private final TelephonyConfigPanel telephonyConfigPanel;

    /**
     * The panel for encoding settings
     */
    private final EncodingsPanel encodingsPanel;

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

        securityPanel
            = new SecurityPanel(
                    this.getRegistration().getSecurityRegistration(), false);

        iceConfigPanel = new IceConfigPanel();

        telephonyConfigPanel = new TelephonyConfigPanel();

        encodingsPanel = new EncodingsPanel();
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

            if (securityPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.SECURITY"),
                    securityPanel);

            if (iceConfigPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.ICE"),
                                    iceConfigPanel);

            if (telephonyConfigPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.TELEPHONY"),
                                    telephonyConfigPanel);

            if (encodingsPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString(""
                        + "plugin.jabberaccregwizz.ENCODINGS"),
                                    encodingsPanel);

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
        return
            (delimIndex == -1) ? userName : userName.substring(0, delimIndex);
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

        registration.setRememberPassword(accountPanel.isRememberPassword());
        registration.setPassword(new String(password));
        registration.setTlsClientCertificate(
            connectionPanel.getClientTlsCertificateId());
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
            registration.setServerPort(serverPort);

        String priority = connectionPanel.getPriority();
        if (!StringUtils.isNullOrEmpty(priority))
            registration.setPriority(Integer.parseInt(priority));

        registration.setDTMFMethod(connectionPanel.getDTMFMethod());
        registration.setDtmfMinimalToneDuration(
            connectionPanel.getDtmfMinimalToneDuration());

        securityPanel.commitPanel(registration.getSecurityRegistration());

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

        registration.setAllowNonSecure(connectionPanel.isAllowNonSecure());
        registration.setDisableCarbon(connectionPanel.isCarbonDisabled());

        registration.setDisableJingle(
            telephonyConfigPanel.isJingleDisabled());
        registration.setTelephonyDomainBypassCaps(
            telephonyConfigPanel.getTelephonyDomainBypassCaps());
        registration.setOverridePhoneSufix(
            telephonyConfigPanel.getTelephonyDomain());

        encodingsPanel.commitPanel(registration.getEncodingsRegistration());
        return true;
    }

    /**
     * Loads given registration object.
     * @param accountReg the account registration object that will be loaded.
     */
    public void loadAccount(JabberAccountRegistration accountReg)
    {
        accountPanel.setUsername(accountReg.getUserID());
        accountPanel.userIDField.setEnabled(false);

        String password = accountReg.getPassword();
        accountPanel.setPassword(password);
        accountPanel.setRememberPassword(password != null);

        String serverAddress = accountReg.getServerAddress();

        accountPanel.showChangePasswordPanel(true);

        connectionPanel.setServerAddress(serverAddress);

        connectionPanel.setClientTlsCertificateId(
                accountReg.getTlsClientCertificate());

        String serverPort = String.valueOf(accountReg.getServerPort());

        connectionPanel.setServerPort(serverPort);

        connectionPanel.setSendKeepAlive(accountReg.isSendKeepAlive());

        connectionPanel.setGmailNotificationsEnabled(
                accountReg.isGmailNotificationEnabled());

        connectionPanel.setGoogleContactsEnabled(
                accountReg.isGoogleContactsEnabled());

        connectionPanel.setResource(accountReg.getResource());

        connectionPanel.setAutogenerateResource(
                accountReg.isResourceAutogenerated());

        connectionPanel.setPriority(String.valueOf(accountReg.getPriority()));

        connectionPanel.setDTMFMethod(accountReg.getDTMFMethod());

        connectionPanel.setDtmfMinimalToneDuration(
                accountReg.getDtmfMinimalToneDuration());

        securityPanel.loadAccount(accountReg.getSecurityRegistration());

        iceConfigPanel.setUseIce(accountReg.isUseIce());

        iceConfigPanel.setAutoDiscoverStun(accountReg.isAutoDiscoverStun());

        iceConfigPanel.setUseDefaultStunServer(
                accountReg.isUseDefaultStunServer());

        List<StunServerDescriptor> stunServers
                = accountReg.getAdditionalStunServers();

        iceConfigPanel.removeAllStunServer();
        for(StunServerDescriptor stunServer : stunServers)
        {
            iceConfigPanel.addStunServer(stunServer);
        }
        iceConfigPanel.setUseJingleNodes(accountReg.isUseJingleNodes());

        iceConfigPanel.setAutoDiscoverJingleNodes(
                accountReg.isAutoDiscoverJingleNodes());

        iceConfigPanel.removeAllJingleNodes();
        List<JingleNodeDescriptor> jingleNodes
                = accountReg.getAdditionalJingleNodes();
        for(JingleNodeDescriptor jingleNode : jingleNodes)
        {
            iceConfigPanel.addJingleNodes(jingleNode);
        }

        iceConfigPanel.setUseUPNP(accountReg.isUseUPNP());

        connectionPanel.setAllowNonSecure(accountReg.isAllowNonSecure());
        connectionPanel.setDisableCarbon(accountReg.isCarbonDisabled());

        connectionPanel.setServerOverridden(accountReg.isServerOverridden());

        telephonyConfigPanel.setDisableJingle(accountReg.isJingleDisabled());

        telephonyConfigPanel.setTelephonyDomain(
                accountReg.getOverridePhoneSuffix());

        telephonyConfigPanel.setTelephonyDomainBypassCaps(
                accountReg.getTelephonyDomainBypassCaps());

        encodingsPanel.loadAccount(accountReg.getEncodingsRegistration());
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

    /**
     * Returns the wizard that created the form
     * @return The form wizard
     */
    public JabberAccountRegistrationWizard getWizard()
    {
        return wizard;
    }
}
