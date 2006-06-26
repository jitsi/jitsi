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
import net.java.sip.communicator.service.contactlist.MetaContactListService;

public class AddContactWizardPage2
    extends WizardPanelDescriptor 
        implements CellEditorListener {

    public static final String IDENTIFIER = "SELECT_GROUP_PANEL";
    
    private SelectGroupPanel selectGroupPanel;
    
    public AddContactWizardPage2(NewContact newContact,
            Iterator groupsList) {
                
        selectGroupPanel = new SelectGroupPanel(newContact, groupsList);
        
        selectGroupPanel.addCheckBoxCellListener(this);
        
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(selectGroupPanel);
    }
    
    public Object getNextPanelDescriptor() {
        return AddContactWizardPage3.IDENTIFIER;
    }
    
    public Object getBackPanelDescriptor() {
        return AddContactWizardPage1.IDENTIFIER;
    }
        
    public void aboutToDisplayPanel() {
        setNextButtonAccordingToCheckBox();
    }    
    
    private void setNextButtonAccordingToCheckBox() {
        if (selectGroupPanel.isCheckBoxSelected())
            getWizard().setNextFinishButtonEnabled(true);
        else
            getWizard().setNextFinishButtonEnabled(false);
    }

    public void editingCanceled(ChangeEvent e) {
        setNextButtonAccordingToCheckBox();
    }

    public void editingStopped(ChangeEvent e) {
        setNextButtonAccordingToCheckBox();
    }
}
