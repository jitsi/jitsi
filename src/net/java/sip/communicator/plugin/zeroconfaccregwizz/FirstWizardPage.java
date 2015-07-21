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
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class FirstWizardPage
    extends TransparentPanel
    implements WizardPage,
               DocumentListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel userPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel userID = new JLabel(
        Resources.getString("plugin.zeroaccregwizz.USERID"));

    /* TEMPORARY : HARD CODED !! Should be added to Resource */
    private JLabel firstLabel
        = new JLabel(Resources.getString("plugin.zeroaccregwizz.FIRST_NAME"));
    private JLabel lastLabel
        = new JLabel(Resources.getString("plugin.zeroaccregwizz.LAST_NAME"));
    private JLabel mailLabel
        = new JLabel(Resources.getString("plugin.zeroaccregwizz.EMAIL"));

    private JPanel emptyPanel = new TransparentPanel();
    private JPanel emptyPanel2 = new TransparentPanel();
    private JPanel emptyPanel3 = new TransparentPanel();
    private JPanel emptyPanel4 = new TransparentPanel();

    private JLabel userIDExampleLabel = new JLabel("Ex: Bill@microsoft");
    private JLabel firstExampleLabel = new JLabel("Ex: Bill");
    private JLabel lastExampleLabel = new JLabel("Ex: Gates");
    private JLabel mailExampleLabel = new JLabel("Ex: Bill@microsoft.com");

    private JTextField userIDField = new TrimTextField();
    private JTextField firstField = new JTextField();
    private JTextField lastField = new JTextField();
    private JTextField mailField = new JTextField();

    private JCheckBox rememberContacts =
        new SIPCommCheckBox(Resources.getString(
            "plugin.zeroaccregwizz.REMEMBER_CONTACTS"));

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private ZeroconfAccountRegistrationWizard wizard;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(ZeroconfAccountRegistrationWizard wizard)
    {

        super(new BorderLayout());

        this.wizard = wizard;

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel.setLayout(
            new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        this.valuesPanel.setLayout(
            new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        this.userIDField.getDocument().addDocumentListener(this);
        this.firstField.getDocument().addDocumentListener(this);
        this.rememberContacts.setSelected(false);

        // not used so disable it for the moment
        this.rememberContacts.setEnabled(false);

        this.userIDExampleLabel.setForeground(Color.GRAY);
        this.userIDExampleLabel.setFont(
                userIDExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.userIDExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        this.firstExampleLabel.setForeground(Color.GRAY);
        this.firstExampleLabel.setFont(
                firstExampleLabel.getFont().deriveFont(8));
        this.emptyPanel2.setMaximumSize(new Dimension(40, 35));
        this.firstExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        this.lastExampleLabel.setForeground(Color.GRAY);
        this.lastExampleLabel.setFont(
                lastExampleLabel.getFont().deriveFont(8));
        this.emptyPanel3.setMaximumSize(new Dimension(40, 35));
        this.lastExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        this.mailExampleLabel.setForeground(Color.GRAY);
        this.mailExampleLabel.setFont(
                mailExampleLabel.getFont().deriveFont(8));
        this.emptyPanel4.setMaximumSize(new Dimension(40, 35));
        this.mailExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        labelsPanel.add(userID);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(firstLabel);
        labelsPanel.add(emptyPanel2);
        labelsPanel.add(lastLabel);
        labelsPanel.add(emptyPanel3);
        labelsPanel.add(mailLabel);

        valuesPanel.add(userIDField);
        valuesPanel.add(userIDExampleLabel);
        valuesPanel.add(firstField);
        valuesPanel.add(firstExampleLabel);
        valuesPanel.add(lastField);
        valuesPanel.add(lastExampleLabel);
        valuesPanel.add(mailField);
        valuesPanel.add(mailExampleLabel);

        userPassPanel.add(labelsPanel, BorderLayout.WEST);
        userPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userPassPanel.add(rememberContacts, BorderLayout.SOUTH);

        userPassPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.zeroaccregwizz.USERID_AND_PASSWORD")));

        this.add(userPassPanel, BorderLayout.NORTH);
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
        ZeroconfAccountRegistration registration
            = wizard.getRegistration();

        String userID = userIDField.getText();

        if(userID == null || userID.trim().length() == 0)
            throw new IllegalStateException("No user ID provided.");

        registration.setUserID(userID);
        registration.setFirst(firstField.getText());
        registration.setLast(lastField.getText());
        registration.setMail(mailField.getText());

        registration.setRememberContacts(rememberContacts.isSelected());

        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID()
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

    /**
     * Implemented from Wizard interface
     * @param event Event that happened
     */
    public void changedUpdate(DocumentEvent event)
    {
    }

    /**
     * Created to
     */
    public void pageHiding()
    {
    }

    /**
     * Implemented from Wizard interface
     */
    public void pageShown()
    {
    }

    /**
     * Implemented from Wizard interface
     */
    public void pageBack()
    {
    }

    /**
     * Fills the UserID field in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();

        this.userIDField.setEnabled(false);
        this.userIDField.setText(accountID.getUserID());
        this.firstField.setText(accountID.getAccountPropertyString("first"));
        this.lastField.setText(accountID.getAccountPropertyString("last"));
        this.mailField.setText(accountID.getAccountPropertyString("mail"));

        boolean remember = accountID
                .getAccountPropertyBoolean("rememberContacts", true);
        if (remember)
            this.rememberContacts.setSelected(true);
    }

    public Object getSimpleForm()
    {
        JPanel simplePanel = new TransparentPanel(new BorderLayout(10, 10));

        simplePanel.add(userID, BorderLayout.WEST);
        simplePanel.add(userIDField, BorderLayout.CENTER);

        return simplePanel;
    }

    public boolean isCommitted()
    {
        return isCommitted;
    }
}
