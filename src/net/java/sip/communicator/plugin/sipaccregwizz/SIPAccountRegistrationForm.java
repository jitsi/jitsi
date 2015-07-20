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
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.wizard.*;

import net.java.sip.communicator.service.protocol.sip.*;
import org.jitsi.util.*;

/**
 * The <tt>SIPAccountRegistrationForm</tt>.
 *
 * @author Yana Stamcheva
 * @author Grogorii Balutsel
 * @author Pawel Domas
 */
public class SIPAccountRegistrationForm
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final AccountPanel accountPanel;
    private final ConnectionPanel connectionPanel;
    private final SecurityPanel securityPanel;
    private final PresencePanel presencePanel;

    /**
     * The panel for encoding settings
     */
    private final EncodingsPanel encodingsPanel;

    private boolean isModification;

    private final SIPAccountRegistrationWizard wizard;

    private final JTabbedPane tabbedPane = new SIPCommTabbedPane();

    /**
     * The panels which value needs validation before we continue.
     */
    private List<ValidatingPanel> validatingPanels =
            new ArrayList<ValidatingPanel>();

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
        securityPanel = new SecurityPanel(
                this.getRegistration().getSecurityRegistration(),
                true);
        presencePanel = new PresencePanel(this);

        encodingsPanel = new EncodingsPanel();
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        accountPanel.initAdvancedForm();

        SIPAccountCreationFormService createService = getCreateAccountService();
        if (createService != null)
            createService.clear();

        if (!SIPAccRegWizzActivator.isAdvancedAccountConfigDisabled())
        {
            if (accountPanel.getParent() != tabbedPane)
                tabbedPane.addTab(  Resources.getString("service.gui.ACCOUNT"),
                                    accountPanel);

            if (connectionPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.CONNECTION"),
                                    connectionPanel);

            if (securityPanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.SECURITY"),
                    securityPanel);

            if (presencePanel.getParent() != tabbedPane)
                tabbedPane.addTab(Resources.getString("service.gui.PRESENCE"),
                                    presencePanel);

            if (encodingsPanel.getParent() != tabbedPane)
                tabbedPane.addTab(
                      Resources.getString("plugin.jabberaccregwizz.ENCODINGS"),
                      encodingsPanel);


            if (tabbedPane.getParent() != this)
                this.add(tabbedPane, BorderLayout.NORTH);

            tabbedPane.setSelectedIndex(0);
        }
        else
            add(accountPanel, BorderLayout.NORTH);
    }

    /**
     * Parse the server part from the sip id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     * @param userName the account user name
     * @return the server address
     */
    String setServerFieldAccordingToUIN(String userName)
    {
        String serverAddress = SipAccountID.getServerFromUserName(userName);

        connectionPanel.setServerFieldAccordingToUIN(serverAddress);

        return serverAddress;
    }

    /**
     * Enables/disables the next/finish button of the parent wizard.
     * @param isEnabled <tt>true</tt> to enable the next button, <tt>false</tt>
     * otherwise
     */
    private void setNextFinishButtonEnabled(boolean isEnabled)
    {
        SIPAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
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
     * Return the server part of the sip user name.
     *
     * @param userName the username.
     * @return the server part of the sip user name.
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
     * @param registration the SIPAccountRegistration
     * @return
     */
    public boolean commitPage(SIPAccountRegistration registration)
    {
        String userID = null;
        char[] password = null;
        String serverAddress = null;
        String proxyAddress = null;
        String xcapRoot = null;
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
                xcapRoot = newAccount.getXcapRoot();

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
            if(SipAccountID.getServerFromUserName(userID) == null
                && registration.getDefaultDomain() != null)
            {
                // we have only a username and we want to add
                // a defautl domain
                userID = userID + "@" + registration.getDefaultDomain();
                setServerFieldAccordingToUIN(userID);
            }

            password = accountPanel.getPassword();
            serverAddress = connectionPanel.getServerAddress();
            proxyAddress = connectionPanel.getProxy();
        }

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);

        registration.setRememberPassword(accountPanel.isRememberPassword());

        registration.setPassword(new String(password));

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

        registration.setProxyAutoConfigure(
            connectionPanel.isProxyAutoConfigureEnabled());

        registration.setEnablePresence(
            presencePanel.isPresenceEnabled());
        registration.setForceP2PMode(
            presencePanel.isForcePeerToPeerMode());
        registration.setTlsClientCertificate(
            connectionPanel.getCertificateId());
        registration.setPollingPeriod(
            presencePanel.getPollPeriod());
        registration.setSubscriptionExpiration(
            presencePanel.getSubscriptionExpiration());

        registration.setKeepAliveMethod(
                connectionPanel.getKeepAliveMethod());

        registration.setKeepAliveInterval(
            connectionPanel.getKeepAliveInterval());

        registration.setDTMFMethod(
            connectionPanel.getDTMFMethod());
        registration.setDtmfMinimalToneDuration(
            connectionPanel.getDtmfMinimalToneDuration());

        SIPAccRegWizzActivator.getUIService().getAccountRegWizardContainer()
            .setBackButtonEnabled(true);

        securityPanel.commitPanel(registration.getSecurityRegistration());

        if(xcapRoot != null)
        {
            registration.setXCapEnable(true);
            registration.setClistOptionServerUri(xcapRoot);
        }
        else
        {
            registration.setXCapEnable(presencePanel.isXCapEnable());
            registration.setXiVOEnable(presencePanel.isXiVOEnable());
            registration.setClistOptionServerUri(
                    presencePanel.getClistOptionServerUri());
        }

        registration.setClistOptionUseSipCredentials(
                presencePanel.isClistOptionUseSipCredentials());
        registration.setClistOptionUser(presencePanel.getClistOptionUser());
        registration.setClistOptionPassword(
            new String(presencePanel.getClistOptionPassword()));
        registration.setMessageWaitingIndications(
            connectionPanel.isMessageWaitingEnabled());
        registration.setVoicemailURI(connectionPanel.getVoicemailURI());
        registration.setVoicemailCheckURI(connectionPanel.getVoicemailCheckURI());

        encodingsPanel.commitPanel(registration.getEncodingsRegistration());

        return true;
    }

    /**
     * Loads given account registration object.
     * @param sipAccReg the account registration object to load.
     */
    public void loadAccount(SIPAccountRegistration sipAccReg)
    {
        String password = sipAccReg.getPassword();

        String serverAddress = sipAccReg.getServerAddress();

        String displayName = sipAccReg.getAccountDisplayName();

        String authName = sipAccReg.getAuthorizationName();

        String serverPort = sipAccReg.getServerPort();

        String proxyAddress = sipAccReg.getProxy();

        String proxyPort = sipAccReg.getProxyPort();

        String preferredTransport = sipAccReg.getPreferredTransport();

        boolean enablePresence = sipAccReg.isEnablePresence();

        boolean forceP2P = sipAccReg.isForceP2PMode();

        String clientTlsCertificateId = sipAccReg.getTlsClientCertificate();

        boolean proxyAutoConfigureEnabled = sipAccReg.isProxyAutoConfigure();

        String pollingPeriod = sipAccReg.getPollingPeriod();

        String subscriptionPeriod = sipAccReg.getSubscriptionExpiration();

        String keepAliveMethod = sipAccReg.getKeepAliveMethod();

        String keepAliveInterval = sipAccReg.getKeepAliveInterval();

        String dtmfMethod = sipAccReg.getDTMFMethod();
        String dtmfMinimalToneDuration = sipAccReg.getDtmfMinimalToneDuration();

        String voicemailURI = sipAccReg.getVoicemailURI();
        String voicemailCheckURI = sipAccReg.getVoicemailCheckURI();

        boolean xCapEnable = sipAccReg.isXCapEnable();
        boolean xivoEnable = sipAccReg.isXiVOEnable();

        boolean isServerOverridden = sipAccReg.isServerOverridden();

        connectionPanel.setServerOverridden(isServerOverridden);

        accountPanel.setUserIDEnabled(false);
        accountPanel.setUserID(sipAccReg.getId());

        if (password != null)
        {
            accountPanel.setPassword(password);
            accountPanel.setRememberPassword(true);
        }
        else
        {
            accountPanel.setPassword("");
            accountPanel.setRememberPassword(false);
        }

        connectionPanel.setServerAddress(serverAddress);
        connectionPanel.setServerEnabled(isServerOverridden);

        if (displayName != null && displayName.length() > 0)
            accountPanel.setDisplayName(displayName);

        if(authName != null && authName.length() > 0)
            connectionPanel.setAuthenticationName(authName);
        connectionPanel.setCertificateId(clientTlsCertificateId);

        connectionPanel.enablesProxyAutoConfigure(proxyAutoConfigureEnabled);
        connectionPanel.setServerPort(serverPort);
        connectionPanel.setProxy(proxyAddress);

        // The order of the next two fields is important, as a change listener
        // of the transportCombo sets the proxyPortField to its default
        connectionPanel.setSelectedTransport(preferredTransport);
        connectionPanel.setProxyPort(proxyPort);

        securityPanel.loadAccount(sipAccReg.getSecurityRegistration());

        presencePanel.reinit();
        presencePanel.setPresenceEnabled(enablePresence);
        presencePanel.setForcePeerToPeerMode(forceP2P);
        presencePanel.setPollPeriod(pollingPeriod);
        presencePanel.setSubscriptionExpiration(subscriptionPeriod);

        if (!enablePresence)
        {
            presencePanel.setPresenceOptionsEnabled(enablePresence);
        }

        connectionPanel.setKeepAliveMethod(keepAliveMethod);
        connectionPanel.setKeepAliveInterval(keepAliveInterval);

        connectionPanel.setDTMFMethod(dtmfMethod);
        connectionPanel.setDtmfMinimalToneDuration(dtmfMinimalToneDuration);

        boolean mwiEnabled = sipAccReg.isMessageWaitingIndicationsEnabled();
        connectionPanel.setMessageWaitingIndications(mwiEnabled);

        if(!StringUtils.isNullOrEmpty(voicemailURI))
            connectionPanel.setVoicemailURI(voicemailURI);

        if(!StringUtils.isNullOrEmpty(voicemailCheckURI))
            connectionPanel.setVoicemailCheckURI(voicemailCheckURI);

        if(xCapEnable)
        {
            presencePanel.setXCapEnable(xCapEnable);
            presencePanel.setClistOptionEnableEnabled(xCapEnable);
        }
        else if(xivoEnable)
        {
            presencePanel.setXiVOEnable(xivoEnable);
            presencePanel.setClistOptionEnableEnabled(xivoEnable);
        }

        boolean clistUseSipCredentials
                = sipAccReg.isClistOptionUseSipCredentials();

        presencePanel.setClistOptionUseSipCredentials(
                clistUseSipCredentials);
        presencePanel.setClistOptionUseSipCredentialsEnabled(
                clistUseSipCredentials);
        presencePanel.setClistOptionServerUri(
                sipAccReg.getClistOptionServerUri());
        presencePanel.setClistOptionUser(
                sipAccReg.getClistOptionUser());
        presencePanel.setClistOptionPassword(
                sipAccReg.getClistOptionPassword());

        encodingsPanel.loadAccount(sipAccReg.getEncodingsRegistration());
    }

    /**
     * Returns a simple version of this registration form.
     * @return the simple form component
     */
    public Component getSimpleForm()
    {
        SIPAccountCreationFormService createAccountService
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
     * Returns the sign up link name.
     * @return the sign up link name
     */
    public String getWebSignupLinkName()
    {
        return wizard.getWebSignupLinkName();
    }

    /**
     * Returns the forgot password link name.
     *
     * @return the forgot password link name
     */
    public String getForgotPasswordLinkName()
    {
        return wizard.getForgotPasswordLinkName();
    }

    /**
     * Returns the forgot password link.
     *
     * @return the forgot password link
     */
    public String getForgotPasswordLink()
    {
        return wizard.getForgotPasswordLink();
    }

    /**
     * Returns an instance of <tt>CreateAccountService</tt> through which the
     * user could create an account. This method is meant to be implemented by
     * specific protocol provider wizards.
     * @return an instance of <tt>CreateAccountService</tt>
     */
    public SIPAccountCreationFormService getCreateAccountService()
    {
         return wizard.getCreateAccountService();
    }

    /**
     * Returns the display label used for the sip id field.
     * @return the sip id display label string.
     */
    protected String getUsernameLabel()
    {
        return wizard.getUsernameLabel();
    }

    /**
     * Returns the current sip registration holding all values.
     * @return sip registration.
     */
    public SIPAccountRegistration getRegistration()
    {
        return wizard.getRegistration();
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
     * Return the string for create new account button.
     * @return the string for create new account button.
     */
    protected String getCreateAccountLabel()
    {
        return wizard.getCreateAccountLabel();
    }

    /**
     * Selects the create account button.
     */
    void setCreateButtonSelected()
    {
        accountPanel.setCreateButtonSelected();
    }
}
