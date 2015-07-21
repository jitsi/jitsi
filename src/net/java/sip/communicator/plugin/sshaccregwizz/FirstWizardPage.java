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
package net.java.sip.communicator.plugin.sshaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.protocol.ssh.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Shobhit Jindal
 */
public class FirstWizardPage
    extends TransparentPanel
    implements WizardPage, DocumentListener
{
    private static final long serialVersionUID = 8576006544813706541L;

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel accountPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel accountID
        = new JLabel(Resources.getString("plugin.sshaccregwizz.USERNAME"));

    private JLabel identityFile
        = new JLabel(Resources.getString("plugin.sshaccregwizz.IDENTITY_FILE"));

    private JLabel knownHostsFile
        = new JLabel(Resources.getString("plugin.sshaccregwizz.KNOWN_HOSTS"));

    private JPanel emptyPanel1 = new TransparentPanel();

    private JPanel emptyPanel2 = new TransparentPanel();

    private JPanel emptyPanel3 = new TransparentPanel();

    private JPanel emptyPanel4 = new TransparentPanel();

    private JPanel emptyPanel5 = new TransparentPanel();

    private JPanel emptyPanel6 = new TransparentPanel();

    private JPanel emptyPanel7 = new TransparentPanel();

    private JPanel emptyPanel8 = new TransparentPanel();

    private JPanel emptyPanel9 = new TransparentPanel();

    private JTextField userIDField = new TrimTextField();

    private JTextField identityFileField = new JTextField(
        Resources.getString("plugin.sshaccregwizz.OPTIONAL"));

    private JButton identityFileButton = new JButton(
        Resources.getString("service.gui.BROWSE"));

    private SipCommFileChooser identityFileChooser;

    private JPanel identityFilePanel = new TransparentPanel();

    private JTextField knownHostsFileField = new JTextField(
        Resources.getString("plugin.sshaccregwizz.KNOWN_HOSTS"));

    private JButton knownHostsFileButton = new JButton(
        Resources.getString("service.gui.BROWSE"));

    private SipCommFileChooser knownHostsFileChooser;

    private JPanel knownHostsFilePanel = new TransparentPanel();

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private SSHAccountRegistration registration = null;

    private WizardContainer wizardContainer;

    private boolean isCommitted = false;

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

        //identityFileChooser.setFileHidingEnabled(false);

        //knownHostsFileChooser.setFileHidingEnabled(false);

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
        this.mainPanel.setOpaque(false);
        this.labelsPanel.setOpaque(false);
        this.valuesPanel.setOpaque(false);
        this.accountPanel.setOpaque(false);
        this.identityFilePanel.setOpaque(false);
        this.knownHostsFilePanel.setOpaque(false);
        this.emptyPanel1.setOpaque(false);
        this.emptyPanel2.setOpaque(false);
        this.emptyPanel3.setOpaque(false);
        this.emptyPanel4.setOpaque(false);
        this.emptyPanel5.setOpaque(false);
        this.emptyPanel6.setOpaque(false);
        this.emptyPanel7.setOpaque(false);
        this.emptyPanel8.setOpaque(false);
        this.emptyPanel9.setOpaque(false);

        this.userIDField.getDocument().addDocumentListener(this);

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

        valuesPanel.add(userIDField);
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
                identityFileChooser = GenericFileDialog.create(
                    null, "Select Identify File",
                    SipCommFileChooser.LOAD_FILE_OPERATION);
                File f = identityFileChooser.getFileFromDialog();

                if(f != null)
                    identityFileField.setText(f.getAbsolutePath());
            }
        }
        );

        knownHostsFileButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                knownHostsFileChooser = GenericFileDialog.create(
                    null, "Select SSH Known Hosts File",
                    SipCommFileChooser.LOAD_FILE_OPERATION);
                File f = knownHostsFileChooser.getFileFromDialog();

                if(f != null)
                    knownHostsFileField.setText(f.getAbsolutePath());
            }
        }
        );

        accountPanel.setBorder(BorderFactory
                .createTitledBorder(Resources.getString(
                "plugin.sshaccregwizz.ACCOUNT_DETAILS")));

        this.add(accountPanel, BorderLayout.NORTH);
    }

    /**
     * Fills the Account ID, Identity File and Known Hosts File fields in this
     * panel with the data coming from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        if (!(protocolProvider instanceof ProtocolProviderServiceSSHImpl))
            throw new ClassCastException("protocolProvider");

        AccountID accountID = protocolProvider.getAccountID();

        String identityFile =
            accountID
                .getAccountPropertyString(ProtocolProviderFactorySSH.IDENTITY_FILE);

        String knownHostsFile =
            accountID
                .getAccountPropertyString(ProtocolProviderFactorySSH.KNOWN_HOSTS_FILE);

        this.userIDField.setText(accountID.getUserID());

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
        String userID = userIDField.getText();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);
        registration.setIdentityFile(identityFileField.getText());
        registration.setKnownHostsFile(knownHostsFileField.getText());

        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID()
    {
        if (userIDField.getText() == null || userIDField.getText()
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

    public Object getSimpleForm()
    {
        return accountPanel;
    }

    public boolean isCommitted()
    {
        return isCommitted;
    }
}
