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
package net.java.sip.communicator.plugin.yahooaccregwizz;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the uin
 * and the password of the account.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage,
                DocumentListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE
        = "Ex: johnsmith@yahoo.com or johnsmith";

    private JPanel uinPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel uinLabel = new JLabel(
        Resources.getString("plugin.yahooaccregwizz.USERNAME"));

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JPanel emptyPanel = new TransparentPanel();

    private JLabel uinExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JTextField userIDField = new TrimTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox =
        new SIPCommCheckBox(
            Resources.getString("service.gui.REMEMBER_PASSWORD"));

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private YahooAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(YahooAccountRegistrationWizard wizard)
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
        this.mainPanel.setOpaque(false);
        this.labelsPanel.setOpaque(false);
        this.valuesPanel.setOpaque(false);
        this.uinPassPanel.setOpaque(false);
        this.emptyPanel.setOpaque(false);

        this.userIDField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(true);

        this.uinExampleLabel.setForeground(Color.GRAY);
        this.uinExampleLabel.setFont(uinExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.uinExampleLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 0, 8, 0));

        labelsPanel.add(uinLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(userIDField);
        valuesPanel.add(uinExampleLabel);
        valuesPanel.add(passField);

        uinPassPanel.add(labelsPanel, BorderLayout.WEST);
        uinPassPanel.add(valuesPanel, BorderLayout.CENTER);
        uinPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        uinPassPanel.setBorder(BorderFactory.createTitledBorder(Resources
            .getString("plugin.yahooaccregwizz.USERID_AND_PASSWORD")));

        mainPanel.add(uinPassPanel);
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
        YahooAccountRegistration registration = wizard.getRegistration();

        String userID = userIDField.getText();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);
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
        if (userIDField.getText() == null || userIDField.getText().equals(""))
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
        String password = YahooAccRegWizzActivator
            .getYahooProtocolProviderFactory().loadPassword(accountID);

        this.userIDField.setEnabled(false);
        this.userIDField.setText(accountID.getUserID());

        if (password != null)
        {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }
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
