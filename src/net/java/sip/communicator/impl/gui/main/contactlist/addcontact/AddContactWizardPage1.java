/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.util.*;

import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>AddContactWizardPage1</tt> is the first page of the "Add Contact"
 * wizard. Contains the <tt>SelectAccountPanel</tt>, where the user should
 * select the account, where the new contact will be created.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizardPage1
    implements WizardPage, CellEditorListener
{

    public static final String IDENTIFIER = "SELECT_ACCOUNT_PANEL";

    private SelectAccountPanel selectAccountPanel;

    private WizardContainer wizard;

    /**
     * Creates an instance of <tt>AddContactWizardPage1</tt>.
     * 
     * @param newContact An object that collects all user choices through the
     *            wizard.
     * @param protocolProvidersList The list of available
     *            <tt>ProtocolProviderServices</tt>, from which the user
     *            could select.
     */
    public AddContactWizardPage1(   WizardContainer wizard,
                                    NewContact newContact,
                                    Iterator protocolProvidersList)
    {

        this.wizard = wizard;

        selectAccountPanel
            = new SelectAccountPanel(newContact, protocolProvidersList);
        selectAccountPanel.addCheckBoxCellListener(this);
    }

    /**
     * Before the panel is displayed checks the selections and enables the next
     * button if a checkbox is already selected or disables it if nothing is
     * selected.
     */
    public void pageShowing()
    {
        setNextButtonAccordingToCheckBox();
    }

    /**
     * Enables the next button when the user makes a choise and disables it if
     * nothing is selected.
     */
    private void setNextButtonAccordingToCheckBox()
    {
        if (selectAccountPanel.isCheckBoxSelected())
            this.wizard.setNextFinishButtonEnabled(true);
        else
            this.wizard.setNextFinishButtonEnabled(false);
    }

    /**
     * When user canceled editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingCanceled(ChangeEvent e)
    {
        setNextButtonAccordingToCheckBox();
    }

    /**
     * When user stopped editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingStopped(ChangeEvent e)
    {
        setNextButtonAccordingToCheckBox();
    }

    public Object getIdentifier()
    {
        return IDENTIFIER;
    }

    public Object getNextPageIdentifier()
    {
        return AddContactWizardPage2.IDENTIFIER;
    }

    public Object getBackPageIdentifier()
    {
        return IDENTIFIER;
    }

    public Object getWizardForm()
    {
        return selectAccountPanel;
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }

    public void commitPage()
    {
        selectAccountPanel.setSelectedAccounts();
    }

    public void pageBack()
    {
    }
}
