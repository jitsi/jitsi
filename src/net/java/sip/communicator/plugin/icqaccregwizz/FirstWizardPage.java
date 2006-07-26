/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.java.sip.communicator.service.gui.WizardContainer;
import net.java.sip.communicator.service.gui.WizardPage;

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
    
    private IcqAccountRegistration registration;
    
    private WizardContainer wizardContainer;
    
    public FirstWizardPage(IcqAccountRegistration registration,
            WizardContainer wizardContainer) {
        
        super(new BorderLayout());
    
        this.wizardContainer = wizardContainer;
        
        this.registration = registration;
        
        this.setPreferredSize(new Dimension(300, 150));
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        this.init();
    }
    
    private void init() {
        this.uinField.getDocument().addDocumentListener(this);
        
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
        
        this.registerArea.setLineWrap(true);
        this.registerArea.setWrapStyleWord(true);
        
        this.registerPanel.add(registerArea);
        this.registerPanel.add(buttonPanel);
        
        this.registerPanel.setBorder(BorderFactory
                .createTitledBorder(Resources.getString("registerNewAccount")));
        
        mainPanel.add(registerPanel);
        
        this.add(mainPanel, BorderLayout.NORTH);
    }
    
    public Object getIdentifier() {
        return FIRST_PAGE_IDENTIFIER;
    }

    public Object getNextPageIdentifier() {
        return WizardPage.SUMMARY_PAGE_IDENTIFIER;
    }

    public Object getBackPageIdentifier() {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
    }

    public Object getWizardForm() {
        return this;
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    public void pageShowing() {
        this.setNextButtonAccordingToUIN();
    }

    public void pageNext() {
        registration.setUin(uinField.getText());
        registration.setPassword(passField.getPassword().toString());
        registration.setRememberPassword(rememberPassBox.isSelected());
    }

    public void pageBack() {
    }

    private void setNextButtonAccordingToUIN() {
        if (uinField.getText() == null || uinField.getText().equals("")) {
            wizardContainer.setNextFinishButtonEnabled(false);
        }
        else {
            wizardContainer.setNextFinishButtonEnabled(true);
        }
    }

    public void insertUpdate(DocumentEvent e) {
        this.setNextButtonAccordingToUIN();
    }

    public void removeUpdate(DocumentEvent e) {
        this.setNextButtonAccordingToUIN();
    }

    public void changedUpdate(DocumentEvent e) {
    }
}
