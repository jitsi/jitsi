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

import net.java.sip.communicator.impl.gui.main.customcontrols.wizard.WizardPanelDescriptor;

/**
 * The <tt>AddContactWizardPage1</tt> is the first page of the "Add Contact"
 * wizard. Contains the <tt>SelectAccountPanel</tt>, where the user should
 * select the account, where the new contact will be created.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizardPage1 
    extends WizardPanelDescriptor 
        implements CellEditorListener {

    public static final String IDENTIFIER = "SELECT_ACCOUNT_PANEL";
    
    private SelectAccountPanel selectAccountPanel;
    
    /**
     * Creates an instance of <tt>AddContactWizardPage1</tt>.
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param protocolProvidersList The list of available 
     * <tt>ProtocolProviderServices</tt>, from which the user could select.
     */
    public AddContactWizardPage1(NewContact newContact, 
            Iterator protocolProvidersList) {
        
        selectAccountPanel = new SelectAccountPanel(
                newContact, protocolProvidersList);
        selectAccountPanel.addCheckBoxCellListener(this);
        
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(selectAccountPanel);
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     */
    public Object getNextPanelDescriptor() {
        return AddContactWizardPage2.IDENTIFIER;
    }
        
    /**
     * Before the panel is displayed checks the selections and enables the
     * next button if a checkbox is already selected or disables it if 
     * nothing is selected.
     */
    public void aboutToDisplayPanel() {
        setNextButtonAccordingToCheckBox();
    }    
    
    /**
     * Enables the next button when the user makes a choise and disables it 
     * if nothing is selected.
     */
    private void setNextButtonAccordingToCheckBox() {
        if (selectAccountPanel.isCheckBoxSelected())
            getWizard().setNextFinishButtonEnabled(true);
        else
            getWizard().setNextFinishButtonEnabled(false);
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
}
