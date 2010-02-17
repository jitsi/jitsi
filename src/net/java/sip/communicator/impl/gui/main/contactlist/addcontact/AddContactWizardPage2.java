/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>AddContactWizardPage2</tt> is the second page of the "Add Contact"
 * wizard. Contains the <tt>SelectGroupPanel</tt>, where the user should
 * select the group, where the new contact will be added.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizardPage2
        implements  WizardPage,
                    CellEditorListener
{
    public static final String IDENTIFIER = "SELECT_GROUP_PANEL";
    
    private final SelectGroupPanel selectGroupPanel;

    private final NewContact newContact;
    
    /**
     * Creates an instance of <tt>AddContactWizardPage2</tt>.
     * @param wizard the parent wizard, where this page is contained
     * @param newContact An object that collects all user choices through the
     * wizard.
     */
    public AddContactWizardPage2(   AddContactWizard wizard, 
                                    NewContact newContact)
    {
        this.newContact = newContact;

        selectGroupPanel = new SelectGroupPanel(wizard, newContact);
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return AddContactWizardPage3.IDENTIFIER;
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the previous wizard page.
     */
    public Object getBackPageIdentifier()
    {
        return AddContactWizardPage1.IDENTIFIER;
    }

    /**
     * Before the panel is displayed checks the selections and enables the
     * next button if a checkbox is already selected or disables it if 
     * nothing is selected.
     */
    public void pageShowing()
    {
        selectGroupPanel.setNextButtonAccordingToComboBox();
    }
    
    /**
     * When user canceled editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingCanceled(ChangeEvent e)
    {
        selectGroupPanel.setNextButtonAccordingToComboBox();
    }

    /**
     * When user stopped editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingStopped(ChangeEvent e)
    {
        selectGroupPanel.setNextButtonAccordingToComboBox();
    }

    public Object getIdentifier()
    {
        return IDENTIFIER;
    }

    public Object getWizardForm()
    {
        return selectGroupPanel;
    }

    public void pageHiding()
    {
    }

    public void pageShown()
    {
    }

    public void commitPage()
    {
        this.newContact.setGroup(selectGroupPanel.getSelectedGroup());
    }

    public void pageBack()
    {
    }

    /**
     * The number of groups.
     * @return The number of available groups.
     */
    public int countGroups()
    {
        return selectGroupPanel.countGroups();
    }
}
