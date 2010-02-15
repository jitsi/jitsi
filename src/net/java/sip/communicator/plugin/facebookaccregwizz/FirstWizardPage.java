/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.facebookaccregwizz;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the username
 * and the password of the account.
 *
 * @author Dai Zhiwei
 * @author Lubomir Marinov
 */
public class FirstWizardPage
    extends TransparentPanel
    implements WizardPage,
               DocumentListener
{
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    public static final String USER_NAME_EXAMPLE = "Ex: username";
    
    private JPanel userPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JLabel usernameLabel
        = new JLabel(Resources.getString("plugin.facebookaccregwizz.USERNAME"));

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JLabel existingAccountLabel
        = new JLabel(Resources.getString("service.gui.EXISTING_ACCOUNT_ERROR"));

    private JPanel emptyPanel = new TransparentPanel();

    private JLabel usernameExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JTextField usernameField = new JTextField();

    private JPasswordField passField = new JPasswordField();

    private JCheckBox rememberPassBox
        = new JCheckBox(Resources.getString("service.gui.REMEMBER_PASSWORD"));

    private JPanel mainPanel = new TransparentPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private FacebookAccountRegistrationWizard wizard;
    
    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * @param wizard the parent wizard
     */
    public FirstWizardPage(FacebookAccountRegistrationWizard wizard)
    {

        super(new BorderLayout());

        this.wizard = wizard;

        mainPanel.setLayout(new BorderLayout());

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        this.usernameField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(true);
        this.rememberPassBox.setOpaque(false);

        this.existingAccountLabel.setForeground(Color.RED);

        this.usernameExampleLabel.setForeground(Color.GRAY);
        this.usernameExampleLabel.setFont(
                usernameExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.usernameExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        JPanel labelsPanel = new TransparentPanel();
        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
        labelsPanel.add(usernameLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        JPanel valuesPanel = new TransparentPanel();
        valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));
        valuesPanel.add(usernameField);
        valuesPanel.add(usernameExampleLabel);
        valuesPanel.add(passField);

        JPanel descriptionPanel = new TransparentPanel(new BorderLayout());
        descriptionPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JEditorPane descriptionValue = new JEditorPane();
        descriptionValue.setAlignmentX(JEditorPane.CENTER_ALIGNMENT);
        descriptionValue.setOpaque(false);
        descriptionValue.setContentType("text/html");
        descriptionValue.setEditable(false);
        descriptionValue.setText(
            Resources.getString(
                "plugin.facebookaccregwizz.DESCRIPTION"));
        descriptionValue.addHyperlinkListener(new HyperlinkListener()
            {
                public void hyperlinkUpdate(HyperlinkEvent e)
                {
                    if (e.getEventType()
                            .equals(HyperlinkEvent.EventType.ACTIVATED))
                    {
                        FacebookAccRegWizzActivator
                            .getBrowserLauncher().openURL(e.getURL().toString());
                    }
                }
            });
        descriptionPanel.add(descriptionValue, BorderLayout.CENTER);

        userPassPanel.add(labelsPanel, BorderLayout.WEST);
        userPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        userPassPanel.setBorder(
            BorderFactory.createTitledBorder(
                Resources.getString(
                    "plugin.facebookaccregwizz.USERNAME_AND_PASSWORD")));

        mainPanel.add(userPassPanel, BorderLayout.CENTER);
        mainPanel.add(descriptionPanel, BorderLayout.SOUTH);

        this.add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Sets the preferred width of a specific <code>JLabel</code> to a value
     * which is likely to cause it to display a specific number of characters
     * per line. Because setting the preferred width requires also setting the
     * preferred height, the preferred height is set to a value which is likely
     * to cause the specified <code>JLabel</code> to display its whole text.
     * 
     * @param label
     *            the <code>JLabel</code> to set the preferred width of
     * @param charCount
     *            the number of characters per line to be displayed after
     *            setting the preferred width
     */
    private void setPreferredWidthInCharCount(JLabel label, int charCount)
    {
        FontMetrics fontMetrics = label.getFontMetrics(label.getFont());
        String text = label.getText();
        int textWidth = fontMetrics.stringWidth(text);
        int labelWidth = charCount * textWidth / text.length();

        label.setPreferredSize(
            new Dimension(
                    labelWidth,
                    fontMetrics.getHeight()
                        * (textWidth / labelWidth
                                + (textWidth % labelWidth > 0 ? 1 : 0))));
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
        this.setNextButtonAccordingToUsername();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {

        String userID = usernameField.getText().trim();

        // add server part to username
        if(userID.indexOf("@") == -1)
            userID += "@" + FacebookAccountRegistrationWizard.SERVER_ADDRESS;

        if (!wizard.isModification() && isExistingAccount(userID))
        {
            nextPageIdentifier = FIRST_PAGE_IDENTIFIER;
            userPassPanel.add(existingAccountLabel, BorderLayout.NORTH);
            this.revalidate();
        }
        else
        {
            nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
            userPassPanel.remove(existingAccountLabel);

            FacebookAccountRegistration registration
                = wizard.getRegistration();

            registration.setUsername(userID);

            if (passField.getPassword() != null)
                registration.setPassword(new String(passField.getPassword()));

            registration.setRememberPassword(rememberPassBox.isSelected());
        }
        
        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUsername()
    {
        if (usernameField.getText() == null || usernameField.getText().equals(""))
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
        this.setNextButtonAccordingToUsername();
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
        this.setNextButtonAccordingToUsername();
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
     * Fills the UserID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        String password = accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PASSWORD);

        this.usernameField.setEnabled(false);
        this.usernameField.setText(accountID.getUserID());

        if (password != null)
        {
            this.passField.setText(password);
            this.rememberPassBox.setSelected(true);
        }
    }

    /**
     * Verifies whether there is already an account installed with the same
     * details as the one that the user has just entered.
     *
     * @param username the name of the user that the account is registered for
     * @return true if there is already an account for this userID and false
     * otherwise.
     */
    private boolean isExistingAccount(String username)
    {
        ProtocolProviderFactory factory
            = FacebookAccRegWizzActivator.getFacebookProtocolProviderFactory();

        Iterable<AccountID> registeredAccounts = factory.getRegisteredAccounts();

        for (AccountID accountID : registeredAccounts)
            if (username.equalsIgnoreCase(accountID.getUserID()))
                return true;
        return false;
    }

    /**
     * Get the simple form.
     * @return form with user and password field
     */
    public Object getSimpleForm()
    {
        return mainPanel;
    }

    /**
     * Is committed.
     * @return Is committed.
     */
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
