/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.util.Iterator;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import net.java.sip.communicator.service.gui.WizardContainer;
import net.java.sip.communicator.service.gui.WizardPage;

/**
 * The <tt>AddContactWizardPage2</tt> is the second page of the "Add Contact"
 * wizard. Contains the <tt>SelectGroupPanel</tt>, where the user should
 * select the group, where the new contact will be added.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizardPage2     
        implements WizardPage, CellEditorListener {

    public static final String IDENTIFIER = "SELECT_GROUP_PANEL";
    
    private SelectGroupPanel selectGroupPanel;
    
    private WizardContainer wizard;
    /**
     * Creates an instance of <tt>AddContactWizardPage2</tt>.
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param groupsList The list of all <tt>MetaContactGroup</tt>s, from which
     * the user could select.
     */
    public AddContactWizardPage2(WizardContainer wizard, 
            NewContact newContact,
            Iterator groupsList) {
                
        this.wizard = wizard;
        
        selectGroupPanel = new SelectGroupPanel(newContact, groupsList);
        
        selectGroupPanel.addCheckBoxCellListener(this);        
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     */
    public Object getNextPageIdentifier() {
        return AddContactWizardPage3.IDENTIFIER;
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the previous wizard page.
     */
    public Object getBackPageIdentifier() {
        return AddContactWizardPage1.IDENTIFIER;
    }
        
    /**
     * Before the panel is displayed checks the selections and enables the
     * next button if a checkbox is already selected or disables it if 
     * nothing is selected.
     */
    public void pageShowing() {
        setNextButtonAccordingToCheckBox();
    }    
    
    /**
     * Enables the next button when the user makes a choise and disables it 
     * if nothing is selected.
     */
    private void setNextButtonAccordingToCheckBox() {
        if (selectGroupPanel.isCheckBoxSelected())
            wizard.setNextFinishButtonEnabled(true);
        else
            wizard.setNextFinishButtonEnabled(false);
    }

    /**
     * When user canceled editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingCanceled(ChangeEvent e) {
        setNextButtonAccordingToCheckBox();
    }

    /**
     * When user stopped editing the next button is enabled or disabled
     * depending on if the user has selected a check box or not.
     */
    public void editingStopped(ChangeEvent e) {
        setNextButtonAccordingToCheckBox();
    }

    public Object getIdentifier() {
        return IDENTIFIER;
    }

    public Object getWizardForm() {
        return selectGroupPanel;
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    public void pageNext() {
    }

    public void pageBack() {
    }
}
