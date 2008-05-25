/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.util.*;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AccountRegSummaryPage</tt> is the last page of the account
 * registration wizard.
 * 
 * @author Yana Stamcheva
 */
public class AccountRegSummaryPage
    extends JScrollPane
    implements WizardPage
{
    private String backPageIdentifier;

    private JPanel keysPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JPanel valuesPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

    private JPanel wrapPanel = new JPanel(new BorderLayout());

    private AccountRegWizardContainerImpl wizardContainer;

    private JLabel pageTitleLabel =
        new JLabel(Messages.getI18NString("summary").getText(), JLabel.CENTER);

    /**
     * Creates an <tt>AccountRegSummaryPage</tt>.
     * 
     * @param wizardContainer The account registration wizard container where
     *            this summary page is registered.
     */
    public AccountRegSummaryPage(AccountRegWizardContainerImpl wizardContainer)
    {
        super();

        this.wizardContainer = wizardContainer;

        this.pageTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        this.mainPanel.add(pageTitleLabel, BorderLayout.NORTH);
        this.mainPanel.add(keysPanel, BorderLayout.WEST);
        this.mainPanel.add(valuesPanel, BorderLayout.CENTER);

        this.wrapPanel.add(mainPanel, BorderLayout.NORTH);

        this.getViewport().add(wrapPanel);

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Initializes the summary with the data.
     * 
     * @param summaryData The data to insert in the summary page.
     */
    private void init(Iterator summaryData)
    {
        while (summaryData.hasNext())
        {
            Map.Entry entry = (Map.Entry) summaryData.next();

            JLabel keyLabel = new JLabel(entry.getKey().toString() + ":");
            JLabel valueLabel = new JLabel(entry.getValue().toString());

            keysPanel.add(keyLabel);
            valuesPanel.add(valueLabel);
        }
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> method.
     * 
     * @return the page identifier, which in this case is the
     *         SUMMARY_PAGE_IDENTIFIER
     */
    public Object getIdentifier()
    {
        return WizardPage.SUMMARY_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> method.
     * 
     * @return the FINISH_PAGE_IDENTIFIER to indicate that this is the last
     *         wizard page
     */
    public Object getNextPageIdentifier()
    {
        return WizardPage.FINISH_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> method.
     * 
     * @return the previous page
     */
    public Object getBackPageIdentifier()
    {
        return wizardContainer.getCurrentWizard().getLastPageIdentifier();
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> method.
     * 
     * @return this panel
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Before the panel is displayed obtains the summary data from the current
     * wizard.
     */
    public void pageShowing()
    {
        AccountRegistrationWizard wizard =
            this.wizardContainer.getCurrentWizard();

        this.keysPanel.removeAll();
        this.valuesPanel.removeAll();

        this.init(wizard.getSummary());
    }

    /**
     * Implements the <tt>WizardPage.pageNext</tt> method, which is invoked
     * from the wizard container when user clicks the "Next" button. We invoke
     * here the wizard finish method.
     */
    public void pageNext()
    {
        AccountRegistrationWizard wizard =
            this.wizardContainer.getCurrentWizard();

        ProtocolProviderService protocolProvider = wizard.signin();

        if (protocolProvider != null)
            this.wizardContainer.saveAccountWizard(protocolProvider, wizard);

        this.wizardContainer.unregisterWizardPages();
        this.wizardContainer.removeWizzardIcon();
    }

    public void pageBack()
    {
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }
}
