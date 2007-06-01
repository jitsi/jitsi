/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.rssaccregwizz;

import java.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Emil Ivov/Jean-Albert Vescovo
 */
public class FirstWizardPage
    extends JPanel implements WizardPage, DocumentListener
{

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel userPassPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel labelsPanel = new JPanel();
    
    private JLabel existingAccountLabel = 
        new JLabel("RSS account already exists !");

    private JLabel creatingAccountLabel = 
        new JLabel("Press next to creat your RSS account...");

    private JTextField userIDField = new JTextField();

    private JPanel mainPanel = new JPanel();

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private RssAccountRegistration registration = null;

    private WizardContainer wizardContainer;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     * @param registration the <tt>RssAccountRegistration</tt>, where
     * all data through the wizard are stored
     * @param wizardContainer the wizardContainer, where this page will
     * be added
     */
    public FirstWizardPage(RssAccountRegistration registration,
                           WizardContainer wizardContainer)
    {

        super(new BorderLayout());

        this.wizardContainer = wizardContainer;

        this.registration = registration;

        this.setPreferredSize(new Dimension(300, 150));

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.init();

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
    }

    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        this.userIDField.getDocument().addDocumentListener(this);

        this.existingAccountLabel.setForeground(Color.RED);

        this.creatingAccountLabel.setForeground(Color.BLUE);

        labelsPanel.add(creatingAccountLabel);
        
        if(!isExistingAccount("rss")){
            labelsPanel.remove(existingAccountLabel);
            labelsPanel.add(creatingAccountLabel);
            setNextButtonAccordingToUserID(true);
        }
        else{
            labelsPanel.remove(creatingAccountLabel);
            labelsPanel.add(existingAccountLabel);
            setNextButtonAccordingToUserID(false);
        }
        
        userPassPanel.add(labelsPanel, BorderLayout.CENTER);

        userPassPanel.setBorder(BorderFactory
                                .createTitledBorder("RSS account creation..."));

        this.add(userPassPanel, BorderLayout.CENTER);
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
        if(isExistingAccount("Rss")) setNextButtonAccordingToUserID(false);
        else setNextButtonAccordingToUserID(true);
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void pageNext()
    {
        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
        userPassPanel.remove(existingAccountLabel);
        registration.setUserID("Rss");
        registration.setPassword("rss");
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     */
    private void setNextButtonAccordingToUserID(boolean newOne)
    {
        if(!newOne)
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
            = RssAccRegWizzActivator.getRssProtocolProviderFactory();

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
