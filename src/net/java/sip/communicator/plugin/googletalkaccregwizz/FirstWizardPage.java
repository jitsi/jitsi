/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user
 * ID and the password of the account.
 *
 * @author Lubomir Marinov
 */
public class FirstWizardPage
    extends TransparentPanel
    implements WizardPage
{
    private static final Logger logger
        = Logger.getLogger(FirstWizardPage.class);

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE = "Ex: johnsmith@gmail.com";

    private JPanel userIDPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JLabel userIDLabel = new JLabel(
        Resources.getString("plugin.googletalkaccregwizz.USERNAME"));

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JLabel userIDExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JTextField userIDField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox
        = new SIPCommCheckBox(Resources
            .getString("service.gui.REMEMBER_PASSWORD"));

    private JPanel advancedOpPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsAdvOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesAdvOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JCheckBox sendKeepAliveBox
        = new SIPCommCheckBox(Resources
            .getString("plugin.jabberaccregwizz.ENABLE_KEEP_ALIVE"));

    private JLabel resourceLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.RESOURCE"));

    private JTextField resourceField
        = new JTextField(GoogleTalkAccountRegistration.DEFAULT_RESOURCE);

    private JLabel priorityLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.PRIORITY"));

    private JTextField priorityField
        = new JTextField(
                Integer.toString(GoogleTalkAccountRegistration.DEFAULT_PRIORITY));

    private JLabel serverLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.SERVER"));

    private JTextField serverField = new JTextField();

    private JLabel portLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.PORT"));

    private JTextField portField
        = new JTextField(
                Integer.toString(GoogleTalkAccountRegistration.DEFAULT_PORT));

    private JPanel registerPanel = new TransparentPanel(new GridLayout(0, 1));

    private JPanel buttonPanel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JTextArea registerArea = new JTextArea(Resources
        .getString("plugin.googletalkaccregwizz.REGISTER_NEW_ACCOUNT_TEXT"));

    private JButton registerButton = new JButton(Resources
        .getString("plugin.googletalkaccregwizz.NEW_ACCOUNT_TITLE"));

    private JPanel mainPanel = new TransparentPanel();

    private final GoogleTalkAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    private boolean isServerOverridden = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(GoogleTalkAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.mainPanel.setOpaque(false);
        this.userIDPassPanel.setOpaque(false);

        this.userIDField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToUserIDAndResource();

                setServerFieldAccordingToUserID();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToUserIDAndResource();

                setServerFieldAccordingToUserID();
            }

            public void changedUpdate(DocumentEvent evt) {}
        });

        this.rememberPassBox.setSelected(true);

        this.userIDExampleLabel.setForeground(Color.GRAY);
        this.userIDExampleLabel.setFont(userIDExampleLabel.getFont()
            .deriveFont(8));
        this.userIDExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0,
            8, 0));

        JPanel emptyPanel = new TransparentPanel();
        emptyPanel.setMaximumSize(new Dimension(40, 35));

        JPanel labelsPanel = new TransparentPanel();
        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
        labelsPanel.add(userIDLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        JPanel valuesPanel = new TransparentPanel();
        valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));
        valuesPanel.add(userIDField);
        valuesPanel.add(userIDExampleLabel);
        valuesPanel.add(passField);

        userIDPassPanel.add(labelsPanel, BorderLayout.WEST);
        userIDPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userIDPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        userIDPassPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.jabberaccregwizz.USERNAME_AND_PASSWORD")));

        mainPanel.add(userIDPassPanel);

        portField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent evt)
            {
            }

            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }
        });

        priorityField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent evt)
            {
            }

            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }
        });

        labelsAdvOpPanel.add(serverLabel);
        labelsAdvOpPanel.add(portLabel);
        labelsAdvOpPanel.add(resourceLabel);
        labelsAdvOpPanel.add(priorityLabel);

        valuesAdvOpPanel.add(serverField);
        valuesAdvOpPanel.add(portField);
        valuesAdvOpPanel.add(resourceField);
        valuesAdvOpPanel.add(priorityField);

        JPanel checkBoxesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        checkBoxesPanel.add(sendKeepAliveBox);

        advancedOpPanel.add(checkBoxesPanel, BorderLayout.NORTH);
        advancedOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        advancedOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);

        advancedOpPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.jabberaccregwizz.ADVANCED_OPTIONS")));

        mainPanel.add(advancedOpPanel);

        registerButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                logger.debug("Reg OK");

                /*
                 * We don't have our own implementation of registering/signing
                 * up with Google Talk so we'll just use webSignup.
                 */
                FirstWizardPage.this.wizard.webSignup();

                logger.debug("Reg End");
            }
        });

        buttonPanel.add(registerButton);

        registerArea.setEditable(false);
        registerArea.setOpaque(false);
        registerArea.setLineWrap(true);
        registerArea.setWrapStyleWord(true);

        registerPanel.add(registerArea);
        registerPanel.add(buttonPanel);

        registerPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.googletalkaccregwizz.NEW_ACCOUNT_TITLE")));

        mainPanel.add(registerPanel);

        this.add(mainPanel, BorderLayout.NORTH);

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return the id of the first wizard page.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return the id of the next wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return SUMMARY_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     *
     * @return the id of the previous wizard page.
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     *
     * @return this wizard page.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the User ID field is empty.
     */
    public void pageShowing()
    {
        this.setNextButtonAccordingToUserIDAndResource();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        GoogleTalkAccountRegistration registration = wizard.getRegistration();

        String userID = userIDField.getText();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);

        registration.setPassword(new String(passField.getPassword()));
        registration.setRememberPassword(rememberPassBox.isSelected());

        registration.setServerAddress(serverField.getText());
        registration.setSendKeepAlive(sendKeepAliveBox.isSelected());
        registration.setResource(resourceField.getText());

        if (portField.getText() != null)
            registration.setPort(Integer.parseInt(portField.getText()));

        if (priorityField.getText() != null)
            registration.setPriority(
                Integer.parseInt(priorityField.getText()));

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * UserID field is empty.
     */
    private void setNextButtonAccordingToUserIDAndResource()
    {
        String userID = userIDField.getText();
        boolean enabled;

        if ((userID == null) || userID.equals(""))
            enabled = false;
        else
        {
            String resource = resourceField.getText();

            enabled = ((resource != null) && !resource.equals(""));
        }
        wizard.getWizardContainer().setNextFinishButtonEnabled(enabled);
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }

    public void pageBack()
    {
    }

    /**
     * Fills the User ID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        Map<String, String> accountProperties
            = accountID.getAccountProperties();

        String password
            = accountProperties.get(ProtocolProviderFactory.PASSWORD);

        this.userIDField.setEnabled(false);
        this.userIDField.setText(accountID.getUserID());

        if (password != null)
        {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }

        String serverAddress
            = accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);

        serverField.setText(serverAddress);

        String serverPort
            = accountProperties.get(ProtocolProviderFactory.SERVER_PORT);

        portField.setText(serverPort);

        boolean keepAlive
            = Boolean.parseBoolean(accountProperties.get("SEND_KEEP_ALIVE"));

        sendKeepAliveBox.setSelected(keepAlive);

        String resource
            = accountProperties.get(ProtocolProviderFactory.RESOURCE);

        resourceField.setText(resource);

        String priority
            = accountProperties.get(ProtocolProviderFactory.RESOURCE_PRIORITY);

        priorityField.setText(priority);

        this.isServerOverridden = accountID.getAccountPropertyBoolean(
            ProtocolProviderFactory.IS_SERVER_OVERRIDDEN, false);
    }

    /**
     * Parse the server part from the Google Talk id and set it to server as
     * default value. If Advanced option is enabled, do nothing.
     */
    private void setServerFieldAccordingToUserID()
    {
        if (!wizard.isModification() || !isServerOverridden)
        {
            String userId = userIDField.getText();

            serverField.setText(wizard.getServerFromUserName(userId));
        }
    }

    /**
     * Disables Next Button if Port field value is incorrect
     */
    private void setNextButtonAccordingToPortAndPriority()
    {
        boolean enabled;

        try
        {
            Integer.parseInt(portField.getText());
            Integer.parseInt(priorityField.getText());
            enabled = true;
        }
        catch (NumberFormatException ex)
        {
            enabled = false;
        }
        wizard.getWizardContainer().setNextFinishButtonEnabled(enabled);
    }

    public Object getSimpleForm()
    {
        return userIDPassPanel;
    }

    public boolean isCommitted()
    {
        return isCommitted;
    }
}
