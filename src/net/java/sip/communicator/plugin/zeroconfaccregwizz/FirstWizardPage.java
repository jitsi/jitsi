/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

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
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel userPassPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new TransparentPanel();

    private JPanel valuesPanel = new TransparentPanel();

    private JLabel userID = new JLabel(Resources.getString("userID"));

    /* TEMPORARY : HARD CODED !! Should be added to Resource */
    private JLabel firstLabel = new JLabel("Firstname:");
    private JLabel lastLabel = new JLabel("Lastname:");
    private JLabel mailLabel = new JLabel("Mail address:");

    private JLabel existingAccountLabel
        = new JLabel(Resources.getString("existingAccount"));

    private JPanel emptyPanel = new TransparentPanel();
    private JPanel emptyPanel2 = new TransparentPanel();
    private JPanel emptyPanel3 = new TransparentPanel();
    private JPanel emptyPanel4 = new TransparentPanel();

    private JLabel userIDExampleLabel = new JLabel("Ex: Bill@microsoft");
    private JLabel firstExampleLabel = new JLabel("Ex: Bill");
    private JLabel lastExampleLabel = new JLabel("Ex: Gates");
    private JLabel mailExampleLabel = new JLabel("Ex: Bill@microsoft.com");

    private JTextField userIDField = new JTextField();
    private JTextField firstField = new JTextField();
    private JTextField lastField = new JTextField();
    private JTextField mailField = new JTextField();

    private JCheckBox rememberContacts =
        new SIPCommCheckBox("Remember Bonjour contacts?");

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

        this.existingAccountLabel.setForeground(Color.RED);

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

        userPassPanel.setBorder(BorderFactory
                                .createTitledBorder(Resources.getString(
                                    "userAndPassword")));

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
    public void commitPage()
    {
        String userID = userIDField.getText();

        // TODO: isExistingAccount blocks (probably badly/not implemented) !!!!
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

            ZeroconfAccountRegistration registration
                = wizard.getRegistration();

            registration.setUserID(userIDField.getText());
            registration.setFirst(firstField.getText());
            registration.setLast(lastField.getText());
            registration.setMail(mailField.getText());

            registration.setRememberContacts(rememberContacts.isSelected());
        }
       
       isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID()
    {
        if (userIDField.getText() == null || userIDField.getText().equals("")
           || firstField.getText() == null || firstField.getText().equals(""))
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
        this.firstField.setText((String)accountID.getAccountProperties()
                                .get("first"));
        this.lastField.setText((String)accountID.getAccountProperties()
                                .get("last"));
        this.mailField.setText((String)accountID.getAccountProperties()
                                .get("mail"));
        Boolean remember = (Boolean)accountID.getAccountProperties()
                                .get("rememberContacts");
        if (remember.booleanValue()) this.rememberContacts.setSelected(true);

    }

    /**
     * Verifies whether there is already an account installed with the same
     * details as the one that the user has just entered.
     *
     * @param userID the name of the user that the account is registered for
     * @return true if there is already an account for this userID and false
     * otherwise.
     */
    private boolean isExistingAccount(String userID)
    {
        ProtocolProviderFactory factory
            = ZeroconfAccRegWizzActivator.getZeroconfProtocolProviderFactory();

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
    
    public Object getSimpleForm()
    {
        JPanel simplePanel = new TransparentPanel(new BorderLayout());

        simplePanel.add(userID, BorderLayout.WEST);
        simplePanel.add(userIDField, BorderLayout.CENTER);

        return simplePanel;
    }
    
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
