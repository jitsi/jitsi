/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>AccountRegSummaryPage</tt> is the last page of the account
 * registration wizard.
 *
 * @author Yana Stamcheva
 */
public class AccountRegSummaryPage
    extends SCScrollPane
    implements WizardPage
{
    private final Logger logger = Logger.getLogger(AccountRegSummaryPage.class);

    private final JPanel keysPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JPanel valuesPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final AccountRegWizardContainerImpl wizardContainer;

    /**
     * Creates an <tt>AccountRegSummaryPage</tt>.
     *
     * @param wizardContainer The account registration wizard container where
     *            this summary page is registered.
     */
    public AccountRegSummaryPage(AccountRegWizardContainerImpl wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        JLabel pageTitleLabel = new JLabel(
                GuiActivator.getResources().getI18NString("service.gui.SUMMARY"),
                JLabel.CENTER);
        Font font = getFont();
        pageTitleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize() + 6));

        JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));
        mainPanel.add(pageTitleLabel, BorderLayout.NORTH);
        mainPanel.add(keysPanel, BorderLayout.WEST);
        mainPanel.add(valuesPanel, BorderLayout.CENTER);

        JPanel wrapPanel = new TransparentPanel(new BorderLayout());
        wrapPanel.add(mainPanel, BorderLayout.NORTH);

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setOpaque(false);
        setViewportView(wrapPanel);
    }

    /**
     * Initializes the summary with the data.
     *
     * @param summaryData The data to insert in the summary page.
     */
    private void init(Iterator<Map.Entry<String, String>> summaryData)
    {
        while (summaryData.hasNext())
        {
            Map.Entry<String, String> entry = summaryData.next();

            JLabel keyLabel = new JLabel(entry.getKey().toString() + ":");

            //apparently value could be null ....
            if( entry.getValue() == null)
                continue;

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
    public void commitPage()
    {
        AccountRegistrationWizard wizard =
            this.wizardContainer.getCurrentWizard();

        try
        {
            ProtocolProviderService protocolProvider = wizard.signin();

            if (protocolProvider != null)
                this.wizardContainer.saveAccountWizard(protocolProvider, wizard);

            this.wizardContainer.unregisterWizardPages();
            this.wizardContainer.removeWizzardIcon();
        }
        catch (OperationFailedException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("The sign in operation has failed.");

            if (e.getErrorCode()
                    == OperationFailedException.ILLEGAL_ARGUMENT)
            {
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.USERNAME_NULL"));
            }
            else if (e.getErrorCode()
                    == OperationFailedException.IDENTIFICATION_CONFLICT)
            {
                new ErrorDialog(
                    GuiActivator.getUIService().getMainFrame(),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    GuiActivator.getResources()
                        .getI18NString("service.gui.USER_EXISTS_ERROR"));
            }
            else
                throw new RuntimeException(e.getMessage(), e);
        }
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
