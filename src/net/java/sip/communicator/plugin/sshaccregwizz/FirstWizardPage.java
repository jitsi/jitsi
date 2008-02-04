/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * FirstWizardPage.java
 *
 * Created on 22 May, 2007, 8:44 AM
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 */

package net.java.sip.communicator.plugin.sshaccregwizz;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.impl.protocol.ssh.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Shobhit Jindal
 */
public class FirstWizardPage
        extends JPanel implements WizardPage, DocumentListener
{
    
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";
    
    private JPanel accountPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel labelsPanel = new JPanel();
    
    private JPanel valuesPanel = new JPanel();
    
    private JLabel accountID = new JLabel(Resources.getString("accountID"));
    
    private JLabel identityFile = new JLabel(Resources.getString(
                                        "identityFile"));
    
    private JLabel knownHostsFile = new JLabel(Resources.getString(
                                        "knownHosts"));
    
    private JLabel existingAccountLabel
            = new JLabel(Resources.getString("existingAccount"));
    
    private JPanel emptyPanel1 = new JPanel();
    
    private JPanel emptyPanel2 = new JPanel();
    
    private JPanel emptyPanel3 = new JPanel();
    
    private JPanel emptyPanel4 = new JPanel();
    
    private JPanel emptyPanel5 = new JPanel();
    
    private JPanel emptyPanel6 = new JPanel();
    
    private JPanel emptyPanel7 = new JPanel();
    
    private JPanel emptyPanel8 = new JPanel();
    
    private JPanel emptyPanel9 = new JPanel();
    
    private JPanel emptyPanel10 = new JPanel();
    
    private JPanel emptyPanel11 = new JPanel();
    
    private JTextField accountIDField = new JTextField();
    
    private JTextField identityFileField = new JTextField("Optional");
    
    private JButton identityFileButton = new JButton("Browse");
    
    private JFileChooser identityFileChooser = new JFileChooser();
    
    private JPanel identityFilePanel = new JPanel();
    
    private JTextField knownHostsFileField = new JTextField("Optional");
    
    private JButton knownHostsFileButton = new JButton("Browse");
    
    private JFileChooser knownHostsFileChooser = new JFileChooser();
    
    private JPanel knownHostsFilePanel = new JPanel();
    
    private JPanel mainPanel = new JPanel();
    
    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;
    
    private SSHAccountRegistration registration = null;
    
    private WizardContainer wizardContainer;
    
    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * @param registration the <tt>SSHAccountRegistration</tt>, where
     * all data through the wizard are stored
     * @param wizardContainer the wizardContainer, where this page will
     * be added
     */
    public FirstWizardPage(SSHAccountRegistration registration,
            WizardContainer wizardContainer)
    {
        
        super(new BorderLayout());
        
        this.wizardContainer = wizardContainer;
        
        this.registration = registration;
        
        this.setPreferredSize(new Dimension(300, 150));
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        identityFileField.setEditable(false);
        
        knownHostsFileField.setEditable(false);
        
        identityFileChooser.setFileHidingEnabled(false);
        
        knownHostsFileChooser.setFileHidingEnabled(false);
        
        this.init();
        
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        this.labelsPanel.setLayout(new BoxLayout(labelsPanel,
                BoxLayout.Y_AXIS));
        
        this.valuesPanel.setLayout(new BoxLayout(valuesPanel,
                BoxLayout.Y_AXIS));
        
        this.identityFilePanel.setLayout(new BoxLayout(identityFilePanel,
                BoxLayout.X_AXIS));
        
        this.knownHostsFilePanel.setLayout(new BoxLayout(knownHostsFilePanel, 
                BoxLayout.X_AXIS));
    }
    
    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        
        this.accountIDField.getDocument().addDocumentListener(this);
        this.existingAccountLabel.setForeground(Color.RED);
        
        /*
         * Following empty panels cover the space needed between key labels
         * WRT Height 2 key lables = 1 text field
         */
        this.emptyPanel1.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel2.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel3.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel4.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel5.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel6.setMaximumSize(new Dimension(40, 35));
        this.emptyPanel7.setMaximumSize(new Dimension(40, 35));
        
        identityFilePanel.add(identityFileField);
        identityFilePanel.add(identityFileButton);
        
        knownHostsFilePanel.add(knownHostsFileField);
        knownHostsFilePanel.add(knownHostsFileButton);
        
        labelsPanel.add(emptyPanel1);
        labelsPanel.add(accountID);
        labelsPanel.add(emptyPanel2);
        labelsPanel.add(emptyPanel3);
        labelsPanel.add(identityFile);
        labelsPanel.add(emptyPanel4);
        labelsPanel.add(emptyPanel5);
        labelsPanel.add(knownHostsFile);
        labelsPanel.add(emptyPanel6);
        
        valuesPanel.add(accountIDField);
        valuesPanel.add(emptyPanel7);
        valuesPanel.add(identityFilePanel);
        valuesPanel.add(emptyPanel8);
        valuesPanel.add(knownHostsFilePanel);
        labelsPanel.add(emptyPanel9);
        
        accountPanel.add(labelsPanel, BorderLayout.WEST);
        accountPanel.add(valuesPanel, BorderLayout.CENTER);
        
        identityFileButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                int returnVal = identityFileChooser.showDialog
                        (FirstWizardPage.this, "Select Identify File");
                
                if(returnVal == JFileChooser.APPROVE_OPTION)
                    identityFileField.setText(identityFileChooser
                            .getSelectedFile().getAbsolutePath());
            }
        }
        );
        
        knownHostsFileButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                int returnVal = knownHostsFileChooser.showDialog
                        (FirstWizardPage.this, "Select SSH Known Hosts File");
                
                if(returnVal == JFileChooser.APPROVE_OPTION)
                    knownHostsFileField.setText(knownHostsFileChooser
                            .getSelectedFile().getAbsolutePath());
            }
        }
        );
        
        accountPanel.setBorder(BorderFactory
                .createTitledBorder(Resources.getString(
                "accountDetails")));
        
        this.add(accountPanel, BorderLayout.NORTH);
    }
    
    /**
     * Fills the Account ID, Identity File and Known Hosts File fields in this
     * panel with the data comming from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        ProtocolProviderFactorySSH protocolProviderSSH =
                (ProtocolProviderFactorySSH)protocolProvider;

        AccountID accountID = protocolProvider.getAccountID();

        String identityFile = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactorySSH.IDENTITY_FILE);

        String knownHostsFile = (String) accountID.getAccountProperties()
            .get(ProtocolProviderFactorySSH.KNOWN_HOSTS_FILE);

        this.accountIDField.setText(accountID.getUserID());

        this.identityFileField.setText(identityFile);

        this.knownHostsFileField.setText(knownHostsFile);
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
     * the next back identifier - the default page.
     *
     * @return the identifier of the default wizard page.
     */
    public Object getBackPageIdentifier()
    {
        return WizardPage.DEFAULT_PAGE_IDENTIFIER;
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
    public void pageNext()
    {
        String userID = accountIDField.getText();
        
        if (isExistingAccount(userID))
        {
            nextPageIdentifier = FIRST_PAGE_IDENTIFIER;
            accountPanel.add(existingAccountLabel, BorderLayout.NORTH);
            this.revalidate();
        }
        else
        {
            nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
            accountPanel.remove(existingAccountLabel);
            
            registration.setAccountID(accountIDField.getText());
            registration.setIdentityFile(identityFileField.getText());
            registration.setKnownHostsFile(knownHostsFileField.getText());
        }
    }
    
    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID()
    {
        if (accountIDField.getText() == null || accountIDField.getText()
                .equals(""))
        {
            wizardContainer.setNextFinishButtonEnabled(false);
        }
        else
        {
            wizardContainer.setNextFinishButtonEnabled(true);
        }
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
    
    public void changedUpdate(DocumentEvent event)
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
     * Verifies whether there is already an account installed with the same
     * details as the one that the user has just entered.
     *
     * @param accountID the name of the user that the account is registered for
     * @return true if there is already an account for this accountID and false
     * otherwise.
     */
    private boolean isExistingAccount(String userID)
    {
        ProtocolProviderFactory factory
                = SSHAccRegWizzActivator.getSSHProtocolProviderFactory();
        
        ArrayList registeredAccounts = factory.getRegisteredAccounts();
        
        for (int i = 0; i < registeredAccounts.size(); i++)
        {
            AccountID accountID = (AccountID) registeredAccounts.get(i);
            
            if (userID.equalsIgnoreCase(accountID.getUserID()))
            {
                return true;
            }
        }
        return false;
    }
}
