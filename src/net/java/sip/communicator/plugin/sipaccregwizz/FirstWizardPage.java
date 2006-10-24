/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the uin
 * and the password of the account.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class FirstWizardPage extends JPanel
    implements  WizardPage,
                DocumentListener,
                ItemListener
{

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";
    
    private String defaultPortValue = "5060";

    private JPanel uinPassPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JLabel uinLabel = new JLabel(Resources.getString("uin"));

    private JLabel passLabel = new JLabel(Resources.getString("password"));

    private JTextField uinField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox = new JCheckBox(
            Resources.getString("rememberPassword"));

    private JPanel advancedOpPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsAdvOpPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesAdvOpPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JCheckBox enableAdvOpButton = new JCheckBox(
        Resources.getString("ovverideServerOps"), false);

    private JLabel serverLabel = new JLabel(Resources.getString("registrar"));
    
    private JLabel proxyLabel = new JLabel(Resources.getString("proxy"));
    
    private JLabel serverPortLabel = new JLabel(Resources.getString("serverPort"));
    
    private JLabel proxyPortLabel = new JLabel(Resources.getString("proxyPort"));
    
    private JLabel transportLabel
        = new JLabel(Resources.getString("preferredTransport"));
    
    private JTextField serverField = new JTextField();
    
    private JTextField proxyField = new JTextField();
    
    private JTextField serverPortField = new JTextField(defaultPortValue);
    
    private JTextField proxyPortField = new JTextField(defaultPortValue);
    
    private JComboBox transportCombo = new JComboBox(
            new Object[]{"UDP", "TLS", "TCP"});
    
    private JPanel mainPanel = new JPanel();

    private SIPAccountRegistration registration;

    private WizardContainer wizardContainer;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * @param registration the <tt>SIPAccountRegistration</tt>, where
     * all data through the wizard are stored
     * @param wizardContainer the wizardContainer, where this page will
     * be added
     */
    public FirstWizardPage(SIPAccountRegistration registration,
            WizardContainer wizardContainer) {

        super(new BorderLayout());

        this.wizardContainer = wizardContainer;

        this.registration = registration;

        this.setPreferredSize(new Dimension(300, 250));

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init() {
        this.uinField.getDocument().addDocumentListener(this);
        this.transportCombo.addItemListener(this);
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

        serverField.setEditable(false);
        serverPortField.setEditable(false);
        proxyField.setEditable(false);
        proxyPortField.setEditable(false);
        transportCombo.setEnabled(false);

        enableAdvOpButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt) {
            // Perform action
            JCheckBox cb = (JCheckBox)evt.getSource();

            serverField.setEditable(cb.isSelected());
            serverPortField.setEditable(cb.isSelected());
            proxyField.setEditable(cb.isSelected());
            proxyPortField.setEditable(cb.isSelected());
            transportCombo.setEnabled(cb.isSelected());            
        }});
        
        transportCombo.setSelectedItem("UDP");

        labelsAdvOpPanel.add(serverLabel);
        labelsAdvOpPanel.add(serverPortLabel);
        labelsAdvOpPanel.add(proxyLabel);
        labelsAdvOpPanel.add(proxyPortLabel);
        labelsAdvOpPanel.add(transportLabel);
        
        valuesAdvOpPanel.add(serverField);
        valuesAdvOpPanel.add(serverPortField);
        valuesAdvOpPanel.add(proxyField);
        valuesAdvOpPanel.add(proxyPortField);
        valuesAdvOpPanel.add(transportCombo);
        
        advancedOpPanel.add(enableAdvOpButton, BorderLayout.NORTH);
        advancedOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        advancedOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);

        advancedOpPanel.setBorder(BorderFactory
                .createTitledBorder(Resources.getString("advancedOptions")));

        mainPanel.add(advancedOpPanel);

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

        registration.setServerAddress(serverField.getText());
        registration.setServerPort(serverPortField.getText());
        registration.setProxy(proxyField.getText());
        registration.setProxyPort(proxyPortField.getText());
        System.out.println("TRANSPORT=================" + transportCombo.getSelectedItem().toString());
        registration.setPreferredTransport(
                transportCombo.getSelectedItem().toString());
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
        this.setServerFieldAccordingToUIN();
    }

    /**
     * Handles the <tt>DocumentEvent</tt> triggered when user deletes letters
     * from the UIN field. Enables or disables the "Next" wizard button
     * according to whether the UIN field is empty.
     */
    public void removeUpdate(DocumentEvent e) {
        this.setNextButtonAccordingToUIN();
        this.setServerFieldAccordingToUIN();
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
    public void loadAccount(ProtocolProviderService protocolProvider) {
        AccountID accountID = protocolProvider.getAccountID();
        String password = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PASSWORD);
        
        String serverAddress = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_ADDRESS);
    
        String serverPort = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.SERVER_PORT);
        
        String proxyAddress = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PROXY_ADDRESS);
        
        String proxyPort = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PROXY_PORT);
        
        String preferredTransport = (String)accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PREFERRED_TRANSPORT);
        
        this.uinField.setText(accountID.getUserID());

        if(password != null) {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }
        
        serverField.setText(serverAddress);
        serverPortField.setText(serverPort);
        proxyField.setText(proxyAddress);
        proxyPortField.setText(proxyPort);
        transportCombo.setSelectedItem(preferredTransport);
    }

    /**
     * Parse the server part from the sip id and set it to server
     * as default value. If Advanced option is enabled Do nothing.
     */
    private void setServerFieldAccordingToUIN()
    {
        if(!enableAdvOpButton.isSelected())
        {
            String uin = uinField.getText();
            int delimIndex = uin.indexOf("@");
            if (delimIndex != -1)
            {
                String newServerAddr = uin.substring(delimIndex + 1);
                
                serverField.setText(newServerAddr);
                proxyField.setText(newServerAddr);
            }
        }
    }

    /**
     * Disables Next Button if Port field value is incorrect
     */
    private void setNextButtonAccordingToPort()
    {
        try
        {
            wizardContainer.setNextFinishButtonEnabled(true);
        }
        catch (NumberFormatException ex)
        {
             wizardContainer.setNextFinishButtonEnabled(false);
        }
    }

    public void itemStateChanged(ItemEvent e)
    {
        if(e.getStateChange() == ItemEvent.SELECTED
                && e.getItem().equals("TLS"))
        {
            serverPortField.setText("5061");
            proxyPortField.setText("5061");
        }
        else {           
            serverPortField.setText("5060");
            proxyPortField.setText("5060");
        }
    }
}
