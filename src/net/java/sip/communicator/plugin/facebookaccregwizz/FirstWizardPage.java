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
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the email
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

    public static final String USER_NAME_EXAMPLE = "Ex: username@email.com";
    
    private JPanel userPassPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel emailLabel
        = new JLabel(Resources.getString("plugin.facebookaccregwizz.USERNAME"));

    private JLabel passLabel
        = new JLabel(Resources.getString("service.gui.PASSWORD"));

    private JLabel existingAccountLabel
        = new JLabel(Resources.getString("service.gui.EXISTING_ACCOUNT_ERROR"));

    private JPanel emptyPanel = new TransparentPanel();

    private JLabel emailExampleLabel = new JLabel(USER_NAME_EXAMPLE);

    private JTextField emailField = new JTextField();

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

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        this.valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        this.emailField.getDocument().addDocumentListener(this);
        this.rememberPassBox.setSelected(true);
        this.rememberPassBox.setOpaque(false);

        this.existingAccountLabel.setForeground(Color.RED);

        this.emailExampleLabel.setForeground(Color.GRAY);
        this.emailExampleLabel.setFont(
                emailExampleLabel.getFont().deriveFont(8));
        this.emptyPanel.setMaximumSize(new Dimension(40, 35));
        this.emailExampleLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8,0));

        labelsPanel.add(emailLabel);
        labelsPanel.add(emptyPanel);
        labelsPanel.add(passLabel);

        valuesPanel.add(emailField);
        valuesPanel.add(emailExampleLabel);
        valuesPanel.add(passField);

        JLabel experimentalWarningLabel
            = new JLabel(
                    Resources.getString(
                        "plugin.facebookaccregwizz.EXPERIMENTAL_WARNING"));
        experimentalWarningLabel.setForeground(Color.RED);
        setPreferredWidthInCharCount(experimentalWarningLabel, 50);

        userPassPanel.add(experimentalWarningLabel, BorderLayout.NORTH);
        userPassPanel.add(labelsPanel, BorderLayout.WEST);
        userPassPanel.add(valuesPanel, BorderLayout.CENTER);
        userPassPanel.add(rememberPassBox, BorderLayout.SOUTH);

        userPassPanel.setBorder(
            BorderFactory.createTitledBorder(
                Resources.getString(
                    "plugin.facebookaccregwizz.USERNAME_AND_PASSWORD")));

        this.add(userPassPanel, BorderLayout.NORTH);
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
        this.setNextButtonAccordingToEmail();
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        String userID = emailField.getText().trim();

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

            registration.setEmail(emailField.getText());

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
    private void setNextButtonAccordingToEmail()
    {
        if (emailField.getText() == null || emailField.getText().equals(""))
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
        this.setNextButtonAccordingToEmail();
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
        this.setNextButtonAccordingToEmail();
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
     * Fills the UserID and Password fields in this panel with the data comming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        AccountID accountID = protocolProvider.getAccountID();
        String password = accountID.getAccountProperties()
            .get(ProtocolProviderFactory.PASSWORD);

        this.emailField.setEnabled(false);
        this.emailField.setText(accountID.getUserID());

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
     * @param email the name of the user that the account is registered for
     * @return true if there is already an account for this userID and false
     * otherwise.
     */
    private boolean isExistingAccount(String email)
    {
        ProtocolProviderFactory factory
            = FacebookAccRegWizzActivator.getFacebookProtocolProviderFactory();

        Iterable<AccountID> registeredAccounts = factory.getRegisteredAccounts();

        for (AccountID accountID : registeredAccounts)
            if (email.equalsIgnoreCase(accountID.getUserID()))
                return true;
        return false;
    }

    public Object getSimpleForm()
    {
        return userPassPanel;
    }
    
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
