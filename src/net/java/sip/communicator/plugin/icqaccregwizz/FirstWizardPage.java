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
package net.java.sip.communicator.plugin.icqaccregwizz;

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
    implements WizardPage, DocumentListener, ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE = "Ex: 83378997";

    private JPanel uinPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel uinLabel
        = new JLabel(Resources.getString("plugin.icqaccregwizz.USERNAME"));

    private JPanel emptyPanel = new TransparentPanel();

    private JLabel uinExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JTextField uinField = new TrimTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox =
        new SIPCommCheckBox(Resources.getString("service.gui.REMEMBER_PASSWORD"));

    private JPanel registerPanel = new TransparentPanel(new GridLayout(0, 1));

    private JPanel buttonPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JTextArea registerArea = new JTextArea(
        Resources.getString("plugin.icqaccregwizz.REGISTER_NEW_ACCOUNT_TEXT"));

    private JButton registerButton = new JButton(
        Resources.getString("plugin.icqaccregwizz.REGISTER_NEW_ACCOUNT"));

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private IcqAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(IcqAccountRegistrationWizard wizard)
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
        this.rememberPassBox.setSelected(true);

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
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     * @return the identifier of the previous wizard page
     */
    public Object getBackPageIdentifier()
    {
        return null;
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

        IcqAccountRegistration registration = wizard.getRegistration();

        if(uin == null || uin.trim().length() == 0)
            throw new IllegalStateException("No UIN provided.");

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
     * Fills the UIN and Password fields in this panel with the data comming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        String password = IcqAccRegWizzActivator.getIcqProtocolProviderFactory()
            .loadPassword(accountID);

        this.uinField.setText(accountID.getUserID());

        this.uinField.setEnabled(false);

        if (password != null)
        {
            this.passField.setText(password);

            this.rememberPassBox.setSelected(true);
        }
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
