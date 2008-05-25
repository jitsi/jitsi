/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user
 * ID and the password of the account.
 * 
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class FirstWizardPage
    extends JPanel
    implements  WizardPage,
                DocumentListener
{
    private static final Logger logger = Logger
        .getLogger(FirstWizardPage.class);

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE = "Ex: johnsmith@jabber.org";

    private JabberNewAccountDialog jabberNewAccountDialog;

    private JPanel userIDPassPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new JPanel();

    private JPanel valuesPanel = new JPanel();

    private JLabel userIDLabel = new JLabel(Resources.getString("username"));

    private JLabel passLabel = new JLabel(Resources.getString("password"));

    private JLabel existingAccountLabel = new JLabel(Resources
        .getString("existingAccount"));

    private JPanel emptyPanel = new JPanel();

    private JLabel userIDExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JTextField userIDField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox = new JCheckBox(Resources
        .getString("rememberPassword"));

    private JPanel advancedOpPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsAdvOpPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesAdvOpPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JCheckBox sendKeepAliveBox = new JCheckBox(Resources
        .getString("enableKeepAlive"));

    private JCheckBox enableAdvOpButton = new JCheckBox(Resources
        .getString("ovverideServerOps"), false);

    private JLabel resourceLabel = new JLabel(Resources.getString("resource"));

    private JTextField resourceField
        = new JTextField(JabberAccountRegistration.DEFAULT_RESOURCE);

    private JLabel priorityLabel = new JLabel(Resources.getString("priority"));

    private JTextField priorityField
        = new JTextField(JabberAccountRegistration.DEFAULT_PRIORITY);

    private JLabel serverLabel = new JLabel(Resources.getString("server"));

    private JTextField serverField = new JTextField();

    private JLabel portLabel = new JLabel(Resources.getString("port"));

    private JTextField portField
        = new JTextField(JabberAccountRegistration.DEFAULT_PORT);

    private JPanel registerPanel = new JPanel(new GridLayout(0, 1));

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JTextArea registerArea = new JTextArea(Resources
        .getString("registerNewAccountText"));

    private JButton registerButton = new JButton(Resources
        .getString("registerNewAccount"));

    private JPanel mainPanel = new JPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private JabberAccountRegistrationWizard wizard;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * 
     * @param wizard the parent wizard
     */
    public FirstWizardPage(JabberAccountRegistrationWizard wizard)
    {

        super(new BorderLayout());

        this.wizard = wizard;

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel
            .setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        this.valuesPanel
            .setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        this.userIDField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(true);

        this.existingAccountLabel.setForeground(Color.RED);

        this.userIDExampleLabel.setForeground(Color.GRAY);
        this.userIDExampleLabel.setFont(userIDExampleLabel.getFont()
            .deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.userIDExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0,
            8, 0));

        labelsPanel.add(userIDLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(userIDField);
        valuesPanel.add(userIDExampleLabel);
        valuesPanel.add(passField);

        userIDPassPanel.add(labelsPanel, BorderLayout.WEST);
        userIDPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userIDPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        userIDPassPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("userIDAndPassword")));

        mainPanel.add(userIDPassPanel);

        serverField.setEnabled(false);
        portField.setEnabled(false);
        resourceField.setEnabled(false);
        priorityField.setEnabled(false);

        enableAdvOpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                // Perform action
                JCheckBox cb = (JCheckBox) evt.getSource();

                if (!wizard.isModification())
                    serverField.setEnabled(cb.isSelected());

                portField.setEnabled(cb.isSelected());
                resourceField.setEnabled(cb.isSelected());
                priorityField.setEnabled(cb.isSelected());

                if(!cb.isSelected())
                {
                    setServerFieldAccordingToUserID();

                    portField.setText(
                        JabberAccountRegistration.DEFAULT_PORT);
                    resourceField.setText(
                        JabberAccountRegistration.DEFAULT_RESOURCE);
                    priorityField.setText(
                        JabberAccountRegistration.DEFAULT_PRIORITY);
                }
            }
        });

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

        JPanel checkBoxesPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        checkBoxesPanel.add(sendKeepAliveBox);
        checkBoxesPanel.add(enableAdvOpButton);

        advancedOpPanel.add(checkBoxesPanel, BorderLayout.NORTH);
        advancedOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        advancedOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);

        advancedOpPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("advancedOptions")));

        mainPanel.add(advancedOpPanel);

        registerButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                logger.debug("Reg OK");
                
                // Open the new account dialog.
                
                jabberNewAccountDialog = new JabberNewAccountDialog(); 
                
                if (jabberNewAccountDialog.isOK == true)
                {
                    serverField.setText(jabberNewAccountDialog.server);
                    portField.setText(jabberNewAccountDialog.port);

                    // This userIDField contains the username "@" the server.
                    userIDField.setText(jabberNewAccountDialog.userID + "@"
                        + jabberNewAccountDialog.server);

                    passField.setText(jabberNewAccountDialog.password);
                }
                logger.debug("Reg End");
            }
        });

        buttonPanel.add(registerButton);

        registerArea.setEnabled(false);
        registerArea.setOpaque(false);
        registerArea.setLineWrap(true);
        registerArea.setWrapStyleWord(true);

        registerPanel.add(registerArea);
        registerPanel.add(buttonPanel);

        registerPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("registerNewAccount")));

        mainPanel.add(registerPanel);

        this.add(mainPanel, BorderLayout.NORTH);
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
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the next back identifier - the default page.
     * 
     * @return the id of the default wizard page.
     */
    public Object getBackPageIdentifier()
    {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
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
    public void pageNext()
    {
        String userID = userIDField.getText();

        if (!wizard.isModification() && isExistingAccount(userID))
        {
            nextPageIdentifier = FIRST_PAGE_IDENTIFIER;
            userIDPassPanel.add(existingAccountLabel, BorderLayout.NORTH);
            this.revalidate();
        }
        else
        {
            nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
            userIDPassPanel.remove(existingAccountLabel);

            JabberAccountRegistration registration = wizard.getRegistration();

            registration.setUserID(userIDField.getText());
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
        }
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * UserID field is empty.
     */
    private void setNextButtonAccordingToUserIDAndResource()
    {
        if (userIDField.getText() == null
            || userIDField.getText().equals("")
            || resourceField.getText() == null
            || resourceField.getText().equals(""))
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(false);
        }
        else
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * UserID field. Enables or disables the "Next" wizard button according to
     * whether the UserID field is empty.
     * 
     * @param evt the document event that has triggered this method call.
     */
    public void insertUpdate(DocumentEvent evt)
    {
        this.setNextButtonAccordingToUserIDAndResource();

        this.setServerFieldAccordingToUserID();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the User ID field. Enables or disables the "Next" wizard button
     * according to whether the User ID field is empty.
     * 
     * @param evt the document event that has triggered this method call.
     */
    public void removeUpdate(DocumentEvent evt)
    {
        this.setNextButtonAccordingToUserIDAndResource();

        this.setServerFieldAccordingToUserID();
    }

    public void changedUpdate(DocumentEvent evt)
    {
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

        Map accountProperties = accountID.getAccountProperties();

        String password = (String) accountProperties.get(
            ProtocolProviderFactory.PASSWORD);

        this.userIDField.setEnabled(false);
        this.userIDField.setText(accountID.getUserID());

        if (password != null)
        {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }

        String serverAddress = (String) accountProperties
            .get(ProtocolProviderFactory.SERVER_ADDRESS);

        serverField.setText(serverAddress);

        String serverPort = (String) accountProperties
            .get(ProtocolProviderFactory.SERVER_PORT);

        portField.setText(serverPort);

        boolean keepAlive = new Boolean((String)accountProperties
            .get("SEND_KEEP_ALIVE")).booleanValue();

        sendKeepAliveBox.setSelected(keepAlive);

        String resource = (String) accountProperties.get(
            ProtocolProviderFactory.RESOURCE);

        resourceField.setText(resource);

        String priority = (String) accountProperties.get(
            ProtocolProviderFactory.RESOURCE_PRIORITY);

        priorityField.setText(priority);

        if (!serverPort.equals(JabberAccountRegistration.DEFAULT_PORT)
            || !resource.equals(JabberAccountRegistration.DEFAULT_RESOURCE)
            || !priority.equals(JabberAccountRegistration.DEFAULT_PRIORITY))
        {
            enableAdvOpButton.setSelected(true);

            // The server field should stay disabled in modification mode,
            // because the user should not be able to change anything concerning
            // the account identifier and server name is part of it.
            serverField.setEnabled(false);

            portField.setEnabled(true);
            resourceField.setEnabled(true);
            priorityField.setEnabled(true);
        }
    }

    /**
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     */
    private void setServerFieldAccordingToUserID()
    {
        if (!enableAdvOpButton.isSelected())
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
        try
        {
            new Integer(portField.getText());
            new Integer(priorityField.getText());
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
        catch (NumberFormatException ex)
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(false);
        }
    }

    /**
     * Checks if the accountName corresponds to an already existing account.
     * 
     * @param accountName the name of the account to check
     * @return TRUE if an account with the specified name already exists, FALSE -
     * otherwise. 
     */
    private boolean isExistingAccount(String accountName)
    {
        ProtocolProviderFactory factory = JabberAccRegWizzActivator
            .getJabberProtocolProviderFactory();

        ArrayList registeredAccounts = factory.getRegisteredAccounts();

        for (int i = 0; i < registeredAccounts.size(); i++)
        {
            AccountID accountID = (AccountID) registeredAccounts.get(i);

            if (accountName.equalsIgnoreCase(accountID.getUserID()))
            {
                return true;
            }
        }
        return false;
    }
}
