/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the uin
 * and the password of the account.
 *
 * @author Yana Stamcheva
 */
public class FirstWizardPage extends JPanel
    implements WizardPage, DocumentListener {

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel uinPassPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JLabel uinLabel = new JLabel(Resources.getString("uin"));

    private JLabel passLabel = new JLabel(Resources.getString("password"));

    private JTextField uinField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox = new JCheckBox(
            Resources.getString("rememberPassword"));

    private JPanel registerPanel = new JPanel(new GridLayout(0, 1));

    private JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER));

    private JTextArea registerArea = new JTextArea(
            Resources.getString("registerNewAccountText"));

    private JButton registerButton = new JButton(
            Resources.getString("registerNewAccount"));

    private JPanel mainPanel = new JPanel();

    private JabberAccountRegistration registration;

    private WizardContainer wizardContainer;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * @param registration the <tt>JabberAccountRegistration</tt>, where
     * all data through the wizard are stored
     * @param wizardContainer the wizardContainer, where this page will
     * be added
     */
    public FirstWizardPage(JabberAccountRegistration registration,
            WizardContainer wizardContainer) {

        super(new BorderLayout());

        this.wizardContainer = wizardContainer;

        this.registration = registration;

        this.setPreferredSize(new Dimension(300, 150));

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init() {
        this.uinField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(true);

        labelsPanel.add(uinLabel);
        labelsPanel.add(passLabel);

        valuesPanel.add(uinField);
        valuesPanel.add(passField);

        uinPassPanel.add(labelsPanel, BorderLayout.WEST);
        uinPassPanel.add(valuesPanel, BorderLayout.CENTER);
        uinPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        uinPassPanel.setBorder(BorderFactory
                .createTitledBorder(Resources.getString("uinAndPassword")));

        mainPanel.add(uinPassPanel);

        this.buttonPanel.add(registerButton);

        this.registerArea.setEditable(false);
        this.registerArea.setLineWrap(true);
        this.registerArea.setWrapStyleWord(true);

        this.registerPanel.add(registerArea);
        this.registerPanel.add(buttonPanel);

        this.registerPanel.setBorder(BorderFactory
                .createTitledBorder(Resources.getString("registerNewAccount")));

        mainPanel.add(registerPanel);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return
     * this page identifier.
     */
    public Object getIdentifier() {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     */
    public Object getNextPageIdentifier() {
        return WizardPage.SUMMARY_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the next back identifier - the default page.
     */
    public Object getBackPageIdentifier() {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return
     * this panel.
     */
    public Object getWizardForm() {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the UIN field is empty.
     */
    public void pageShowing() {
        this.setNextButtonAccordingToUIN();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void pageNext() {
        registration.setUin(uinField.getText());
        registration.setPassword(new String(passField.getPassword()));
        registration.setRememberPassword(rememberPassBox.isSelected());
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * UIN field is empty.
     */
    private void setNextButtonAccordingToUIN() {
        if (uinField.getText() == null || uinField.getText().equals("")) {
            wizardContainer.setNextFinishButtonEnabled(false);
        }
        else {
            wizardContainer.setNextFinishButtonEnabled(true);
        }
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the
     * UIN field. Enables or disables the "Next" wizard button according to
     * whether the UIN field is empty.
     */
    public void insertUpdate(DocumentEvent e) {
        this.setNextButtonAccordingToUIN();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field. Enables or disables the "Next" wizard button
     * according to whether the UIN field is empty.
     */
    public void removeUpdate(DocumentEvent e) {
        this.setNextButtonAccordingToUIN();
    }

    public void changedUpdate(DocumentEvent e) {
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    public void pageBack() {
    }

    /**
     * Fills the UIN and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider,
            boolean rememberPassword) {
        AccountID accountID = protocolProvider.getAccountID();
        String password = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PASSWORD);

        this.uinField.setText(accountID.getUserID());
        this.passField.setText(password);

        this.rememberPassBox.setSelected(rememberPassword);
    }
}
