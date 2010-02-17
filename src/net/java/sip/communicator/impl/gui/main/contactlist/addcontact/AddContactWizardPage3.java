/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>AddContactWizardPage3</tt> is the last page of the "Add Contact"
 * wizard. Contains the <tt>AddContactPanel</tt>, where the user should
 * enter the identifier of the contact to add.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizardPage3
    implements WizardPage
{
    public static final String IDENTIFIER = "ADD_CONTACT_PANEL";

    private AddContactPanel addContactPanel;

    private NewContact newContact;

    /**
     * By default we are back to wizard page #2 but this can be changed.
     */
    private String backPageIdentifier = AddContactWizardPage2.IDENTIFIER;

    /**
     * Creates an instance of <tt>AddContactWizardPage3</tt>.
     * @param wizard the parent wizard
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param contactAddress the address of the contact to add
     */
    public AddContactWizardPage3(Wizard wizard, NewContact newContact,
                                String contactAddress)
    {
        this.addContactPanel = new AddContactPanel(wizard, contactAddress);

        this.newContact = newContact;
    }

    /**
     * Creates an instance of <tt>AddContactWizardPage3</tt>.
     * @param wizard the parent wizard
     * @param newContact An object that collects all user choices through the
     * wizard.
     */
    public AddContactWizardPage3(Wizard wizard, NewContact newContact)
    {
        this.addContactPanel = new AddContactPanel(wizard);

        this.newContact = newContact;
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the next wizard page.
     */
    public Object getNextPageIdentifier() {
        return WizardPage.FINISH_PAGE_IDENTIFIER;
    }
    
    /**
     * Implements the <tt>WizardPanelDescriptor</tt> method to return the
     * identifier of the previous wizard page.
     */
    public Object getBackPageIdentifier() {
        return backPageIdentifier;
    }
    
    /**
     * Before finishing the wizard sets the identifier entered by the user
     * to the <tt>NewContact</tt> object.
     */
    public void pageHiding() {
        newContact.setUin(addContactPanel.getUIN());
    }

    public Object getIdentifier() {
        return IDENTIFIER;
    }

    public Object getWizardForm() {
        return addContactPanel;
    }

    public void pageShown() {
        this.addContactPanel.requestFocusInField();
    }

    public void pageShowing() {
        this.addContactPanel.setNextFinishButtonAccordingToUIN();
    }

    public void commitPage() {
    }

    public void pageBack() {
    }
    
    public void setUIN(String uin)
    {
        addContactPanel.setUIN(uin);
    }

    /**
     * Changes the back page for the current wizard page.
     * @param identifier the new back page identifier.
     */
    public void setBackPageIdentifier(String identifier)
    {
        this.backPageIdentifier = identifier;
    }
}
