/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.util.ArrayList;

import net.java.sip.communicator.impl.gui.main.customcontrols.wizard.WizardPanelDescriptor;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class AddContactWizardPage3 extends WizardPanelDescriptor {

    public static final String IDENTIFIER = "ADD_CONTACT_PANEL";
    
    private AddContactPanel addContactPanel;
    
    private NewContact newContact;
    
    public AddContactWizardPage3(NewContact newContact) {        
        this.addContactPanel = new AddContactPanel();
        
        this.newContact = newContact;
        
        setPanelDescriptorIdentifier(IDENTIFIER);
        setPanelComponent(addContactPanel);
    }
    
    public Object getNextPanelDescriptor() {
        return FINISH;
    }
    
    public Object getBackPanelDescriptor() {
        return AddContactWizardPage2.IDENTIFIER;
    }
    
    public void aboutToHidePanel() {
        newContact.setUin(addContactPanel.getUIN());
    }
}
