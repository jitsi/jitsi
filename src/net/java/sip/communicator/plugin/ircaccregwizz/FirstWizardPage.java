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
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Lionel Ferreira & Michael Tarantino
 * @author Danny van Heumen
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage,
                DocumentListener,
                ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The identifier of this wizard page.
     */
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    /**
     * Example of IRC nick name.
     */
    public static final String USER_NAME_EXAMPLE = Resources
        .getString("plugin.ircaccregwizz.EXAMPLE_USERNAME");

    /**
     * Example of IRC server name.
     */
    public static final String SERVER_EXAMPLE = Resources
        .getString("plugin.ircaccregwizz.EXAMPLE_SERVER");

    private static final String DEFAULT_PLAINTEXT_PORT = "6667";

    private static final String DEFAULT_SECURE_PORT = "6697";

    private JPanel userPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel serverPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel optionsPanel = new TransparentPanel(new BorderLayout(10, 10));
    
    private JPanel saslPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JPanel labelsServerPanel = new TransparentPanel();

    private JPanel valuesServerPanel = new TransparentPanel();

    private JPanel labelsOptionsPanel = new TransparentPanel();

    private JPanel valuesOptionsPanel = new TransparentPanel();

    private JLabel infoPassword
        = new JLabel(Resources.getString("plugin.ircaccregwizz.INFO_PASSWORD"));

    private JLabel nick = new JLabel(
        Resources.getString("plugin.ircaccregwizz.USERNAME") + ":");

    private JLabel passLabel = new JLabel(
        Resources.getString("service.gui.PASSWORD") + ":");

    private JLabel server
        = new JLabel(Resources.getString("plugin.ircaccregwizz.HOST"));

    private JLabel port = new JLabel(Resources.getString("service.gui.PORT")
        + ":");

    private JPanel emptyPanel = new TransparentPanel();

    private JPanel emptyPanel2 = new TransparentPanel();

    private JLabel nickExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JLabel serverExampleLabel = new JLabel(SERVER_EXAMPLE);

    private JTextField userIDField = new TrimTextField();

    private JPasswordField passField = new JPasswordField();

    private JTextField serverField = new JTextField();

    private JTextField portField = new JTextField();

    private JCheckBox rememberPassBox
        = new SIPCommCheckBox(Resources.getString("service.gui.REMEMBER_PASSWORD"));

    private JCheckBox autoNickChange
        = new SIPCommCheckBox(
            Resources.getString("plugin.ircaccregwizz.AUTO_NICK_CHANGE"));

    private JCheckBox resolveDnsThroughProxy = new SIPCommCheckBox(
        Resources.getString("plugin.ircaccregwizz.RESOLVE_DNS_THROUGH_PROXY"));

    private JCheckBox defaultPort = new SIPCommCheckBox(
            Resources.getString("plugin.ircaccregwizz.USE_DEFAULT_PORT"));

    private JCheckBox passwordNotRequired = new SIPCommCheckBox(
            Resources.getString("plugin.ircaccregwizz.PASSWORD_NOT_REQUIRED"));

    private JCheckBox useSecureConnection = new SIPCommCheckBox(
        Resources.getString("plugin.ircaccregwizz.USE_SECURE_CONNECTION"));

    private JCheckBox enableContactPresenceTask = new SIPCommCheckBox(
        Resources.getString("plugin.ircaccregwizz.ENABLE_CONTACT_PRESENCE"));

    private JCheckBox enableChatRoomPresenceTask = new SIPCommCheckBox(
        Resources.getString("plugin.ircaccregwizz.ENABLE_CHAT_ROOM_PRESENCE"));

    private JCheckBox saslEnabled = new SIPCommCheckBox(
        Resources.getString("plugin.ircaccregwizz.ENABLE_SASL_AUTHENTICATION"));

    private JTextField saslUserIdField = new JTextField();

    private JTextField saslRoleField = new JTextField();

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private IrcAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(IrcAccountRegistrationWizard wizard, String userId, String server)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init(userId, server);

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel.setLayout(
            new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        this.valuesPanel.setLayout(
            new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

        this.labelsServerPanel.setLayout(
            new BoxLayout(labelsServerPanel, BoxLayout.Y_AXIS));

        this.valuesServerPanel.setLayout(
            new BoxLayout(valuesServerPanel, BoxLayout.Y_AXIS));

        this.labelsOptionsPanel.setLayout(
            new BoxLayout(labelsOptionsPanel, BoxLayout.Y_AXIS));

        this.valuesOptionsPanel.setLayout(
            new BoxLayout(valuesOptionsPanel, BoxLayout.Y_AXIS));

        this.portField.setEnabled(false);
        this.rememberPassBox.setEnabled(false);
        this.useSecureConnection.setEnabled(true);
        this.resolveDnsThroughProxy.setEnabled(true);
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init(String userId, String server)
    {
        this.mainPanel.setOpaque(false);
        this.labelsPanel.setOpaque(false);
        this.valuesPanel.setOpaque(false);
        this.emptyPanel.setOpaque(false);

        this.userIDField.getDocument().addDocumentListener(this);
        this.serverField.getDocument().addDocumentListener(this);
        this.passField.getDocument().addDocumentListener(this);
        this.defaultPort.addActionListener(this);
        this.passwordNotRequired.addActionListener(this);
        this.useSecureConnection.addActionListener(this);

        this.saslEnabled.addActionListener(this);

        this.userIDField.setText(userId);
        this.serverField.setText(server);
        this.passField.setEnabled(false);
        this.rememberPassBox.setSelected(true);
        this.autoNickChange.setSelected(true);
        this.resolveDnsThroughProxy.setSelected(true);
        this.defaultPort.setSelected(true);
        this.passwordNotRequired.setSelected(true);
        this.useSecureConnection.setSelected(true);
        this.enableContactPresenceTask.setSelected(true);
        this.enableChatRoomPresenceTask.setSelected(true);
        this.portField
            .setText(this.useSecureConnection.isSelected() ? DEFAULT_SECURE_PORT
                : DEFAULT_PLAINTEXT_PORT);

        this.nickExampleLabel.setForeground(Color.GRAY);
        this.nickExampleLabel.setFont(
                nickExampleLabel.getFont().deriveFont(8));
        this.serverExampleLabel.setForeground(Color.GRAY);
        this.serverExampleLabel.setFont(
                serverExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel2.setMaximumSize(new Dimension(40, 35));
        this.nickExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));
        this.serverExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        labelsPanel.add(nick);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(userIDField);
        valuesPanel.add(nickExampleLabel);
        valuesPanel.add(passField);

        userPassPanel.add(infoPassword, BorderLayout.NORTH);
        userPassPanel.add(labelsPanel, BorderLayout.WEST);
        userPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userPassPanel.add(passwordNotRequired, BorderLayout.SOUTH);

        userPassPanel.setBorder(BorderFactory
                                .createTitledBorder(Resources.getString(
                                "plugin.ircaccregwizz.USERNAME_AND_PASSWORD")));

        labelsServerPanel.add(this.server);
        labelsServerPanel.add(emptyPanel2);
        labelsServerPanel.add(port);

        valuesServerPanel.add(serverField);
        valuesServerPanel.add(serverExampleLabel);
        valuesServerPanel.add(portField);

        serverPanel.add(labelsServerPanel, BorderLayout.WEST);
        serverPanel.add(valuesServerPanel, BorderLayout.CENTER);

        JPanel serverSubPanel = new JPanel(new BorderLayout());
        serverSubPanel.setOpaque(false);
        serverSubPanel.add(defaultPort, BorderLayout.WEST);
        serverSubPanel.add(useSecureConnection, BorderLayout.EAST);
        serverPanel.add(serverSubPanel, BorderLayout.SOUTH);

        serverPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.ircaccregwizz.IRC_SERVER")));

        final JPanel optionsSubPanel = new TransparentPanel();
        optionsSubPanel.setLayout(new BoxLayout(optionsSubPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(optionsSubPanel, BorderLayout.WEST);
        optionsSubPanel.add(rememberPassBox);
        optionsSubPanel.add(autoNickChange);
        final JPanel partition = new TransparentPanel();
        optionsSubPanel.add(resolveDnsThroughProxy);
        optionsPanel.add(partition, BorderLayout.SOUTH);
        partition.setLayout(new BorderLayout());
        partition.add(enableContactPresenceTask, BorderLayout.WEST);
        partition.add(enableChatRoomPresenceTask, BorderLayout.EAST);

        optionsPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("service.gui.OPTIONS")));

        saslPanel.add(this.saslEnabled, BorderLayout.NORTH);

        TransparentPanel saslControlsPanel = new TransparentPanel();
        saslControlsPanel.setLayout(new BoxLayout(saslControlsPanel,
            BoxLayout.Y_AXIS));
        saslPanel.add(saslControlsPanel, BorderLayout.CENTER);

        JLabel saslUserLabel = new JLabel(
            Resources.getString("plugin.ircaccregwizz.SASL_USERNAME") + ":");
        saslControlsPanel.add(horizontal(100,
            saslUserLabel, this.saslUserIdField));
        JLabel saslPassLabel = new JLabel(
            Resources.getString("service.gui.PASSWORD") + ":");
        saslControlsPanel.add(horizontal(100, saslPassLabel, new JLabel(
            Resources.getString("plugin.ircaccregwizz.SASL_IRC_PASSWORD_USED"))));
        JLabel saslRoleLabel = new JLabel(
            Resources.getString("plugin.ircaccregwizz.SASL_AUTHZ_ROLE") + ":");
        saslControlsPanel
            .add(horizontal(100, saslRoleLabel, this.saslRoleField));

        saslPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.ircaccregwizz.SASL_AUTHENTICATION_TITLE")));

        mainPanel.add(userPassPanel);
        mainPanel.add(serverPanel);
        mainPanel.add(optionsPanel);
        mainPanel.add(saslPanel);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    private JPanel horizontal(int width, Component cmp1, Component cmp2) {
        TransparentPanel panel = new TransparentPanel(new BorderLayout(10, 10));
        cmp1.setPreferredSize(new Dimension(width, cmp1.getHeight()));
        panel.add(cmp1, BorderLayout.WEST);
        panel.add(cmp2, BorderLayout.CENTER);
        return panel;
    }
    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return
     * this page identifier.
     *
     * @return the Identifier of the first page in this wizard.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return the identifier of the page following this one.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     *
     * @return the identifier of the previous wizard page
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return
     * this panel.
     *
     * @return the component to be displayed in this wizard page.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the UserID field is empty.
     */
    public void pageShowing()
    {
        this.setNextButtonAccordingToUserID();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

        IrcAccountRegistration registration = wizard.getRegistration();

        String userID = userIDField.getText();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);

        if (passField.getPassword() != null)
            registration.setPassword(new String(passField.getPassword()));

        registration.setServer(serverField.getText());
        registration.setPort(portField.getText());
        registration.setRememberPassword(rememberPassBox.isSelected());
        registration.setAutoChangeNick(autoNickChange.isSelected());
        registration.setRequiredPassword(!passwordNotRequired.isSelected());
        registration.setSecureConnection(useSecureConnection.isSelected());
        registration
            .setContactPresenceTaskEnabled(this.enableContactPresenceTask
                .isSelected());
        registration.setChatRoomPresenceTaskEnabled(enableChatRoomPresenceTask
            .isSelected());
        registration.setSaslEnabled(!this.passwordNotRequired.isSelected()
            && this.saslEnabled.isSelected());
        registration.setSaslUser(this.saslUserIdField.getText());
        registration.setSaslRole(this.saslRoleField.getText());
        registration.setResolveDnsThroughProxy(this.resolveDnsThroughProxy
            .isSelected());

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID()
    {
        if (userIDField.getText() == null
                || userIDField.getText().equals("")
                || serverField.getText() == null
                || serverField.getText().equals("")
                || (!passwordNotRequired.isSelected() && isEmpty(passField)))
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(false);
        }
        else
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
    }

    private boolean isEmpty(JPasswordField passField)
    {
        if (passField.getDocument() != null)
        {
            char[] pass = passField.getPassword();

            if (pass != null)
            {

                /*
                 * The Javadoc of JPasswordField.getPassword() recommends
                 * clearing the returned character array for stronger security
                 * by setting each character to zero
                 */
                Arrays.fill(pass, '\0');
                return (pass.length <= 0);
            }
        }
        return true;
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * User ID field. Enables or disables the "Next" wizard button according to
     * whether the User ID field is empty.
     *
     * @param event the event containing the update.
     */
    public void insertUpdate(DocumentEvent event)
    {
        this.setNextButtonAccordingToUserID();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UserID field. Enables or disables the "Next" wizard button
     * according to whether the UserID field is empty.
     *
     * @param event the event containing the update.
     */
    public void removeUpdate(DocumentEvent event)
    {
        this.setNextButtonAccordingToUserID();
    }

    /**
     * Fills the UserID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();

        String password = IrcAccRegWizzActivator.getIrcProtocolProviderFactory()
            .loadPassword(accountID);

        String server =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.SERVER_ADDRESS);

        String port =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.SERVER_PORT);

        String autoNickChange =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.AUTO_CHANGE_USER_NAME);

        String noPasswordRequired =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.NO_PASSWORD_REQUIRED);

        boolean useSecureConnection =
            accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_ENCRYPTION, true);

        boolean resolveDnsThroughProxy =
            accountID.getAccountPropertyBoolean(
                IrcAccountRegistrationWizard.RESOLVE_DNS_THROUGH_PROXY, true);

        boolean contactPresenceTaskEnabled =
            accountID.getAccountPropertyBoolean(
                IrcAccountRegistrationWizard.CONTACT_PRESENCE_TASK, true);

        boolean chatRoomPresenceTaskEnabled =
            accountID.getAccountPropertyBoolean(
                IrcAccountRegistrationWizard.CHAT_ROOM_PRESENCE_TASK, true);

        final boolean enableSaslAuthentication =
            accountID.getAccountPropertyBoolean(
                IrcAccountRegistrationWizard.SASL_ENABLED, false);
        final String saslUser =
            accountID.getAccountPropertyString(
                IrcAccountRegistrationWizard.SASL_USERNAME, "");
        final String saslRole =
            accountID.getAccountPropertyString(
                IrcAccountRegistrationWizard.SASL_ROLE, "");

        this.userIDField.setEnabled(false);
        this.userIDField.setText(accountID.getUserID());
        this.serverField.setText(server);

        if (password != null)
        {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }

        if (port != null)
        {
            this.portField.setText(port);

            boolean defaultPort =
                (useSecureConnection && DEFAULT_SECURE_PORT.equals(port))
                    || DEFAULT_PLAINTEXT_PORT.equals(port);
            this.portField.setEnabled(!defaultPort);
            this.defaultPort.setSelected(defaultPort);
        }

        if (autoNickChange != null)
        {
            this.autoNickChange.setSelected(
                new Boolean(autoNickChange).booleanValue());
        }

        this.resolveDnsThroughProxy.setSelected(resolveDnsThroughProxy);

        if (noPasswordRequired != null)
        {
            boolean isPassRequired = !Boolean.valueOf(noPasswordRequired);

            this.passwordNotRequired.setSelected(!isPassRequired);
            this.rememberPassBox.setEnabled(isPassRequired);
            passField.setEnabled(isPassRequired);
        }

        this.useSecureConnection.setSelected(useSecureConnection);
        this.enableContactPresenceTask.setSelected(contactPresenceTaskEnabled);
        this.enableChatRoomPresenceTask
            .setSelected(chatRoomPresenceTaskEnabled);

        this.saslEnabled.setSelected(enableSaslAuthentication);
        this.saslUserIdField.setText(saslUser);
        this.saslRoleField.setText(saslRole);
    }

    /**
     * Indicates when the default port check box and the passwordNotRequired
     * check box are selected.
     */
    public void actionPerformed(ActionEvent event)
    {
        if (defaultPort.isSelected())
        {
            portField
                .setText(useSecureConnection.isSelected() ? DEFAULT_SECURE_PORT
                    : DEFAULT_PLAINTEXT_PORT);
            portField.setEnabled(false);
        }
        else
        {
            portField.setEnabled(true);
        }

        boolean passwordRequired = !this.passwordNotRequired.isSelected();
        if (passwordRequired)
        {
            passField.setEnabled(true);
            rememberPassBox.setEnabled(true);
            this.saslEnabled.setEnabled(true);
        }
        else
        {
            passField.setText("");
            passField.setEnabled(false);
            rememberPassBox.setEnabled(false);
            this.saslEnabled.setEnabled(false);
        }

        boolean enableSaslControls =
            passwordRequired && this.saslEnabled.isSelected();
        saslUserIdField.setEnabled(enableSaslControls);
        saslRoleField.setEnabled(enableSaslControls);

        setNextButtonAccordingToUserID();
    }

    public void changedUpdate(DocumentEvent event){}

    public void pageHiding(){}

    public void pageShown(){}

    public void pageBack(){}

    public Object getSimpleForm()
    {
        JPanel simplePanel = new TransparentPanel(new BorderLayout());
        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));
        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1));

        simplePanel.setOpaque(false);
        labelsPanel.setOpaque(false);
        valuesPanel.setOpaque(false);
        emptyPanel2.setOpaque(false);

        simplePanel.add(labelsPanel, BorderLayout.WEST);
        simplePanel.add(valuesPanel, BorderLayout.CENTER);

        labelsPanel.add(nick);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(server);
        labelsPanel.add(emptyPanel2);

        valuesPanel.add(userIDField);
        valuesPanel.add(nickExampleLabel);
        valuesPanel.add(serverField);
        valuesPanel.add(serverExampleLabel);

        return simplePanel;
    }

    public boolean isCommitted()
    {
        return isCommitted;
    }

    public String getCurrentUserId()
    {
        return this.userIDField.getText();
    }

    public String getCurrentServer()
    {
        return this.serverField.getText();
    }
}
