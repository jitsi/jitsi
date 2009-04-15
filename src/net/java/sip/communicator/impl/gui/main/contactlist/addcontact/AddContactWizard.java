/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>AddContactWizard</tt> is the wizard the guides the user through the
 * process of adding a contact.
 * 
 * @author Yana Stamcheva
 */
public class AddContactWizard
    extends Wizard
    implements  WizardListener
{
    /**
     * An Eclipse generated serial version UID.
     */
    private static final long serialVersionUID = 6001213290904019062L;

    private Logger logger = Logger.getLogger(AddContactWizard.class.getName());

    private MainFrame mainFrame;

    private NewContact newContact = new NewContact();

    private AddContactWizardPage1 page1;

    private AddContactWizardPage2 page2;

    private AddContactWizardPage3 page3;

    public AddContactWizard(MainFrame mainFrame)
    {
        super(mainFrame);

        this.mainFrame = mainFrame;

        super.addWizardListener(this);

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT_WIZARD"));

        this.setFinishButtonText(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT"));

        Vector<ProtocolProviderService> pps 
            = new Vector<ProtocolProviderService>();
        Iterator<ProtocolProviderService> iter 
            = mainFrame.getProtocolProviders();
        while (iter.hasNext())
        {
            ProtocolProviderService p = (ProtocolProviderService)iter.next();

            boolean isHidden =
                p.getAccountID().getAccountProperty(
                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

            if(!isHidden)
                pps.add(p);
        }

        page1 = new AddContactWizardPage1(this, newContact,
                    pps.iterator());

        this.registerWizardPage(AddContactWizardPage1.IDENTIFIER, page1);

        page2 = new AddContactWizardPage2(this, newContact,
                    mainFrame.getAllGroups());

        this.registerWizardPage(AddContactWizardPage2.IDENTIFIER, page2);

        page3 = new AddContactWizardPage3(this, newContact);

        this.registerWizardPage(AddContactWizardPage3.IDENTIFIER, page3);

        this.setCurrentPage(AddContactWizardPage1.IDENTIFIER);
    }
    
    /**
     * Creates new wizard with already defined protocol and 
     * contact address
     * @param mainFrame 
     * @param newContactAddress the contact address to add
     * @param protocolProvider the protocol for the new contact
     */
    public AddContactWizard(MainFrame mainFrame, 
            String newContactAddress,
            ProtocolProviderService protocolProvider)
    {
        this(mainFrame);
        newContact.addProtocolProvider(protocolProvider);
        
        this.setCurrentPage(AddContactWizardPage2.IDENTIFIER);
        
        page3.setUIN(newContactAddress);
    }
    
    /**
     * Creates a new meta contact in a separate thread.
     */
    private class CreateContact extends Thread {
        ProtocolProviderService pps;
        MetaContactGroup group;
        NewContact newContact;
        
        CreateContact(ProtocolProviderService pps,
                NewContact newContact)
        {
            this.pps = pps;
            this.group = newContact.getGroup();
            this.newContact = newContact;
        }
        
        public void run() {
            try {
                mainFrame.getContactList()
                    .createMetaContact(
                    pps, group, newContact.getUin());
            }
            catch (MetaContactListException ex)
            {
                logger.error(ex);
                ex.printStackTrace();
                int errorCode = ex.getErrorCode();

                if (errorCode
                        == MetaContactListException
                            .CODE_CONTACT_ALREADY_EXISTS_ERROR)
                {
                        new ErrorDialog(mainFrame,
                            GuiActivator.getResources().getI18NString(
                            "addContactErrorTitle"),
                            GuiActivator.getResources().getI18NString(
                                    "service.gui.ADD_CONTACT_EXIST_ERROR",
                                    new String[]{newContact.getUin()}),
                            ex)
                        .showDialog();
                }
                else if (errorCode
                        == MetaContactListException.CODE_NETWORK_ERROR) {
                    
                    new ErrorDialog(mainFrame,
                        GuiActivator.getResources().getI18NString(
                        "addContactErrorTitle"),
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_NETWORK_ERROR",
                                new String[]{newContact.getUin()}),
                        ex)
                    .showDialog();
                }
                else {
                    
                    new ErrorDialog(mainFrame,
                        GuiActivator.getResources().getI18NString(
                        "addContactErrorTitle"),
                        GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_ERROR",
                                new String[]{newContact.getUin()}),
                        ex)
                    .showDialog();
                }
            }
            
            ConfigurationManager.setLastContactParent(group.getGroupName());
        }
    }

    public void wizardFinished(WizardEvent e)
    {
        if(e.getEventCode() == WizardEvent.SUCCESS) {
            
            ArrayList<ProtocolProviderService> ppList 
                = newContact.getProtocolProviders();
                
            for(int i = 0; i < ppList.size(); i ++) {
                ProtocolProviderService pps
                    = (ProtocolProviderService)ppList.get(i);

                new CreateContact(pps, newContact).start();
            }
        }
    }

    /**
     * Invokes the <tt>Wizard.showDialog</tt> method in order to perform
     * additional operations when visualizing this component.
     */
    public void setVisible(boolean isVisible)
    {
        if(isVisible)
            showDialog(false);
        else
            super.setVisible(false);
    }

    /**
     * Returns the main application window.
     * 
     * @return the main application window
     */
    public MainFrame getMainFrame()
    {
        return mainFrame;
    }
    
    /**
     * Sets the unique contact ID string in the wizard.
     *
     * @param UIN the unique contact ID string
     */
    void setUIN(String UIN)
    {
        this.page3.setUIN(UIN);
    }
}
