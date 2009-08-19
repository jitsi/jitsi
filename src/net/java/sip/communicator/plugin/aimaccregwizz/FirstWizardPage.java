/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.aimaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the uin
 * and the password of the account.
 *
 * @author Yana Stamcheva
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage,
                DocumentListener,
                ActionListener
{
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE = "Ex: 83378997";

    private JPanel uinPassPanel =
        new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JPanel advancedOpPanel =
        new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsAdvOpPanel =
        new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesAdvOpPanel =
        new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private JLabel uinLabel = new JLabel(
        Resources.getString("plugin.aimaccregwizz.USERNAME"));

    private JPanel emptyPanel = new TransparentPanel();

    private JLabel uinExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JLabel passLabel = new JLabel(
        Resources.getString("service.gui.PASSWORD"));

    private JTextField uinField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox =
        new SIPCommCheckBox(Resources.getString(
            "service.gui.REMEMBER_PASSWORD"));

    private JPanel registerPanel = new TransparentPanel(new GridLayout(0, 1));

    private JPanel buttonPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JTextArea registerArea =
        new JTextArea(Resources.getString(
            "plugin.aimaccregwizz.REGISTER_NEW_ACCOUNT_TEXT"));

    private JButton registerButton =
        new JButton(Resources.getString(
            "plugin.aimaccregwizz.REGISTER_NEW_ACCOUNT"));

    private JLabel proxyLabel = new JLabel(
        Resources.getString("plugin.aimaccregwizz.PROXY"));

    private JLabel proxyPortLabel =
        new JLabel(Resources.getString("plugin.aimaccregwizz.PROXY"));

    private JLabel proxyUsernameLabel =
        new JLabel(Resources.getString("plugin.aimaccregwizz.PROXY_USERNAME"));

    private JLabel proxyPasswordLabel =
        new JLabel(Resources.getString("plugin.aimaccregwizz.PROXY_PASSWORD"));

    private JLabel proxyTypeLabel =
        new JLabel(Resources.getString("plugin.aimaccregwizz.PROXY_TYPE"));

    private JTextField proxyField = new JTextField();

    private JTextField proxyPortField = new JTextField();

    private JTextField proxyUsernameField = new JTextField();

    private JPasswordField proxyPassField = new JPasswordField();

    private JComboBox proxyTypeCombo = new JComboBox(new Object[]
    { "http", "socks5", "socks4" });

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private final AimAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage( AimAccountRegistrationWizard wizard)
    {

        super(new BorderLayout());

        this.wizard = wizard;

        this.setPreferredSize(new Dimension(600, 500));

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
        this.mainPanel.setOpaque(false);
        this.labelsPanel.setOpaque(false);
        this.valuesPanel.setOpaque(false);
        this.uinPassPanel.setOpaque(false);
        this.emptyPanel.setOpaque(false);

        this.registerButton.addActionListener(this);
        this.uinField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(
                wizard.getRegistration().isRememberPassword());

        this.uinExampleLabel.setForeground(Color.GRAY);
        this.uinExampleLabel.setFont(uinExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.uinExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8,
            0));

        labelsPanel.add(uinLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(uinField);
        valuesPanel.add(uinExampleLabel);
        valuesPanel.add(passField);

        uinPassPanel.add(labelsPanel, BorderLayout.WEST);
        uinPassPanel.add(valuesPanel, BorderLayout.CENTER);
        uinPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        uinPassPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.aimaccregwizz.USERNAME_AND_PASSWORD")));

        mainPanel.add(uinPassPanel);

        proxyTypeCombo.setSelectedItem(wizard.getRegistration().getProxyType());

        labelsAdvOpPanel.add(proxyLabel);
        labelsAdvOpPanel.add(proxyPortLabel);
        labelsAdvOpPanel.add(proxyTypeLabel);
        labelsAdvOpPanel.add(proxyUsernameLabel);
        labelsAdvOpPanel.add(proxyPasswordLabel);

        valuesAdvOpPanel.add(proxyField);
        valuesAdvOpPanel.add(proxyPortField);
        valuesAdvOpPanel.add(proxyTypeCombo);
        valuesAdvOpPanel.add(proxyUsernameField);
        valuesAdvOpPanel.add(proxyPassField);

        advancedOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        advancedOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);

        advancedOpPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.aimaccregwizz.ADVANCED_OPTIONS")));

        mainPanel.add(advancedOpPanel);

        this.buttonPanel.add(registerButton);

        this.registerArea.setEditable(false);
        this.registerArea.setLineWrap(true);
        this.registerArea.setWrapStyleWord(true);
        this.registerArea.setOpaque(false);

        this.registerPanel.add(registerArea);
        this.registerPanel.add(buttonPanel);

        this.registerPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.aimaccregwizz.REGISTER_NEW_ACCOUNT")));

        mainPanel.add(registerPanel);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the next back identifier - the default page.
     */
    public Object getBackPageIdentifier()
    {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the UIN field is empty.
     */
    public void pageShowing()
    {
        this.setNextButtonAccordingToUIN();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        String uin = uinField.getText();

        if(uin == null || uin.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        AimAccountRegistration registration = wizard.getRegistration();

        registration.setUin(uin);
        registration.setPassword(new String(passField.getPassword()));
        registration.setRememberPassword(rememberPassBox.isSelected());

        registration.setProxy(proxyField.getText());
        registration.setProxyPort(proxyPortField.getText());
        registration.setProxyUsername(proxyUsernameField.getText());

        if (proxyTypeCombo.getSelectedItem() != null)
            registration.setProxyType(proxyTypeCombo.getSelectedItem()
                .toString());

        if (proxyPassField.getPassword() != null)
            registration.setProxyPassword(new String(proxyPassField
                .getPassword()));

        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the UIN
     * field is empty.
     */
    private void setNextButtonAccordingToUIN()
    {
        if (uinField.getText() == null || uinField.getText().equals(""))
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(false);
        }
        else
        {
            wizard.getWizardContainer().setNextFinishButtonEnabled(true);
        }
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user types in the UIN
     * field. Enables or disables the "Next" wizard button according to whether
     * the UIN field is empty.
     */
    public void insertUpdate(DocumentEvent e)
    {
        this.setNextButtonAccordingToUIN();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field. Enables or disables the "Next" wizard button
     * according to whether the UIN field is empty.
     */
    public void removeUpdate(DocumentEvent e)
    {
        this.setNextButtonAccordingToUIN();
    }

    public void changedUpdate(DocumentEvent e)
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
     * Fills the UIN and Password fields in this panel with the data coming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        String password =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PASSWORD);

        this.uinField.setEnabled(false);
        this.uinField.setText(accountID.getUserID());

        if (password != null)
        {
            this.passField.setText(password);

            this.rememberPassBox.setSelected(
                    wizard.getRegistration().isRememberPassword());
        }

        String proxyAddress =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PROXY_ADDRESS);

        String proxyPort =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PROXY_PORT);

        String proxyType =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PROXY_TYPE);

        String proxyUsername =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PROXY_USERNAME);

        String proxyPassword =
            accountID
                .getAccountPropertyString(ProtocolProviderFactory.PROXY_PASSWORD);

        proxyField.setText(proxyAddress);
        proxyPortField.setText(proxyPort);
        proxyTypeCombo.setSelectedItem(proxyType);
        proxyUsernameField.setText(proxyUsername);
        proxyPassField.setText(proxyPassword);
    }

    public void actionPerformed(ActionEvent e)
    {
        wizard.webSignup();
    }

    public Object getSimpleForm()
    {
        return uinPassPanel;
    }

    public boolean isCommitted()
    {
        return isCommitted;
    }
}
