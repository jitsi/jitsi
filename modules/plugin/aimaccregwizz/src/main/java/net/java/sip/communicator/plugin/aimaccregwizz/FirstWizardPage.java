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
package net.java.sip.communicator.plugin.aimaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

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
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The first page identifier.
     */
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    /**
     * The user name example.
     */
    public static final String USER_NAME_EXAMPLE = "Ex: johnsmith";

    /**
     * The panel containing the user name and the password.
     */
    private final Component uinPassPanel;

    private JTextField uinField = new TrimTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox =
        new SIPCommCheckBox(Resources.getString(
            "service.gui.REMEMBER_PASSWORD"));

    private JPanel buttonPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JButton registerButton =
        new JButton(Resources.getString(
            "plugin.aimaccregwizz.REGISTER_NEW_ACCOUNT"));

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

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.uinPassPanel = createUinPassPanel();

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Initializes the advanced panel.
     */
    private void initAdvancedPanel()
    {
        mainPanel.add(uinPassPanel);

        mainPanel.add(createRegisterPanel());
    }

    /**
     * Creates the user name and password panel.
     * @return the created component
     */
    private Component createUinPassPanel()
    {
        JPanel uinPassPanel = new TransparentPanel(new BorderLayout(10, 10));

        JPanel labelsPanel = new TransparentPanel();

        JPanel valuesPanel = new TransparentPanel();

        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

        JLabel uinLabel = new JLabel(
            Resources.getString("plugin.aimaccregwizz.USERNAME"));

        JPanel emptyPanel = new TransparentPanel();

        JLabel uinExampleLabel = new JLabel(USER_NAME_EXAMPLE);

        JLabel passLabel = new JLabel(
            Resources.getString("service.gui.PASSWORD"));

        this.uinField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(
                wizard.getRegistration().isRememberPassword());

        uinExampleLabel.setForeground(Color.GRAY);
        uinExampleLabel.setFont(uinExampleLabel.getFont().deriveFont(8));
        emptyPanel.setMaximumSize(new Dimension(40, 35));
        uinExampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

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

        return uinPassPanel;
    }

    /**
     * Creates the register panel.
     * @return the created panel
     */
    private Component createRegisterPanel()
    {
        JPanel registerPanel = new TransparentPanel(new GridLayout(0, 1));

        this.registerButton.addActionListener(this);

        this.buttonPanel.add(registerButton);

        JTextArea registerArea = new JTextArea(Resources.getString(
            "plugin.aimaccregwizz.REGISTER_NEW_ACCOUNT_TEXT"));

        registerArea.setEditable(false);
        registerArea.setLineWrap(true);
        registerArea.setWrapStyleWord(true);
        registerArea.setOpaque(false);

        registerPanel.add(registerArea);
        registerPanel.add(buttonPanel);

        registerPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.aimaccregwizz.REGISTER_NEW_ACCOUNT")));

        return registerPanel;
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
     * the next identifier - the summary page.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier, which is null as this is the first wizard page.
     * @return the identifier of the previous page
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     * @return the advanced wizard form
     */
    public Object getWizardForm()
    {
        initAdvancedPanel();

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

    public void changedUpdate(DocumentEvent e) {}

    public void pageHiding() {}

    public void pageShown() {}

    public void pageBack() {}

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
        String password = AimAccRegWizzActivator.getAimProtocolProviderFactory()
            .loadPassword(accountID);

        this.uinField.setEnabled(false);
        this.uinField.setText(accountID.getUserID());

        if (password != null)
        {
            this.passField.setText(password);

            this.rememberPassBox.setSelected(
                    wizard.getRegistration().isRememberPassword());
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        wizard.webSignup();
    }

    /**
     * The simple form for this wizard.
     * @return the simple form for this wizard.
     */
    public Object getSimpleForm()
    {
        return uinPassPanel;
    }

    /**
     * Whether is committed.
     * @return <tt>true</tt> if the form is committed, <tt>false</tt>
     * otherwise
     */
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
