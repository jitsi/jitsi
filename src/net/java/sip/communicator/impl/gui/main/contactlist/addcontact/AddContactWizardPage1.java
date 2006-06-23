/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import org.ungoverned.radical.util.editor.BooleanEditor;

import net.java.sip.communicator.impl.gui.main.customcontrols.wizard.WizardPanelDescriptor;

public class AddContactWizardPage1 
    extends WizardPanelDescriptor 
        implements CellEditorListener {

    public static final String IDENTIFIER = "SELECT_ACCOUNT_PANEL";
    
    private SelectAccountPanel selectAccountPanel;
    
    public AddContactWizardPage1(NewContact newContact, 
            ArrayList protocolProvidersList) {
        
        selectAccountPanel = new SelectAccountPanel(
                newContact, protocolProvidersList);
        selectAccountPanel.addCheckBoxCellListener(this);
        
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(selectAccountPanel);
    }
    
    public Object getNextPanelDescriptor() {
        return AddContactWizardPage2.IDENTIFIER;
    }
        
    public void aboutToDisplayPanel() {
        setNextButtonAccordingToCheckBox();
    }    
    
    private void setNextButtonAccordingToCheckBox() {
        if (selectAccountPanel.isCheckBoxSelected())
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
