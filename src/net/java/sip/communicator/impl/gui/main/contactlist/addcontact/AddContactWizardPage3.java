/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import net.java.sip.communicator.impl.gui.main.customcontrols.wizard.WizardPanelDescriptor;

/**
 * The <tt>AddContactWizardPage3</tt> is the last page of the "Add Contact"
 * wizard. Contains the <tt>AddContactPanel</tt>, where the user should
 * enter the identifier of the contact to add.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizardPage3 extends WizardPanelDescriptor {

    public static final String IDENTIFIER = "ADD_CONTACT_PANEL";
    
    private AddContactPanel addContactPanel;
    
    private NewContact newContact;
    
    /**
     * Creates an instance of <tt>AddContactWizardPage3</tt>.
     * @param newContact An object that collects all user choices through the
     * wizard.
     */
    public AddContactWizardPage3(NewContact newContact) {
        this.addContactPanel = new AddContactPanel();
        
        this.newContact = newContact;
        
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(addContactPanel);
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     */
    public Object getNextPanelDescriptor() {
        return FINISH;
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the previous wizard page.
     */
    public Object getBackPanelDescriptor() {
        return AddContactWizardPage2.IDENTIFIER;
    }
    
    /**
     * Before finishing the wizard sets the identifier entered by the user
     * to the <tt>NewContact</tt> object.
     */
    public void aboutToHidePanel() {
        newContact.setUin(addContactPanel.getUIN());
    }
}
