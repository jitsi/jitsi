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
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user ID
 * and the password of the account.
 *
 * @author Emil Ivov/Jean-Albert Vescovo
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage,
                DocumentListener
{
    /**
     * An Eclipse generated UID.
     */
    private static final long serialVersionUID = -4099426006855229937L;

    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel labelsPanel = new TransparentPanel();

    private JLabel infoTitle= new JLabel(
        Resources.getString("plugin.rssaccregwizz.ACCOUNT_INFO_TITLE"));

    private JPanel infoTitlePanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JLabel existingAccountLabel = new JLabel(
        Resources.getString("plugin.rssaccregwizz.ERROR_ACCOUNT_EXISTS"));

    private JPanel existingAccountPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

    private JTextArea accountInfoArea = new JTextArea(
        Resources.getString("plugin.rssaccregwizz.ACCOUNT_INFO"));

    private JLabel accountInfoAttentionArea = new JLabel(
        Resources.getString("plugin.rssaccregwizz.ACCOUNT_ATTENTION"));

    private JPanel accountInfoAttentionPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private RssAccountRegistration registration = null;

    private WizardContainer wizardContainer;

    private boolean isCommitted = false;

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

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.labelsPanel.setLayout(
            new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));

        this.add(labelsPanel, BorderLayout.NORTH);

        this.infoTitle.setFont(
            infoTitle.getFont().deriveFont(Font.BOLD, 14.0f));

        this.infoTitlePanel.add(infoTitle);

        this.labelsPanel.add(infoTitlePanel);

        this.accountInfoAttentionArea.setFont(
            infoTitle.getFont().deriveFont(Font.BOLD, 14.0f));

        this.accountInfoAttentionPanel.add(accountInfoAttentionArea);

        this.accountInfoArea.setWrapStyleWord(true);
        this.accountInfoArea.setLineWrap(true);
        this.accountInfoArea.setEditable(false);
        this.accountInfoArea.setOpaque(false);

        this.existingAccountLabel.setForeground(Color.RED);
        this.existingAccountPanel.add(existingAccountLabel);

        if(!isExistingAccount("rss"))
        {
            labelsPanel.remove(existingAccountPanel);
            labelsPanel.add(accountInfoAttentionPanel);
            labelsPanel.add(accountInfoArea);

            setNextButtonAccordingToUserID(true);
        }
        else
        {
            labelsPanel.remove(accountInfoAttentionPanel);
            labelsPanel.remove(accountInfoArea);
            labelsPanel.add(existingAccountPanel);

            setNextButtonAccordingToUserID(false);
        }
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
        if(isExistingAccount("RSS")) setNextButtonAccordingToUserID(false);
        else setNextButtonAccordingToUserID(true);
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;

        registration.setUserID("RSS");

        isCommitted = true;
    }

    /**
     * Enables or disables the "Next" wizard button according to whether the
     * User ID field is empty.
     *
     * @param newOne true if the Next/Finish button should be enabled and false
     * otherwise.
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

        ArrayList<AccountID> registeredAccounts 
            = factory.getRegisteredAccounts();

        for (int i = 0; i < registeredAccounts.size(); i++)
        {
            AccountID accountID = registeredAccounts.get(i);

            if (userID.equalsIgnoreCase(accountID.getUserID()))
            {
                return true;
            }
        }
        return false;
    }

    public Object getSimpleForm()
    {
        return labelsPanel;
    }

    public boolean isCommitted()
    {
        return isCommitted;
    }
}
