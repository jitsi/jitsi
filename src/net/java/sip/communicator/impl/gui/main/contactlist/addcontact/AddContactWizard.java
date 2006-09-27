/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.wizard.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class AddContactWizard
    extends Wizard
    implements MetaContactListListener
{
    private Logger logger = Logger.getLogger(AddContactWizard.class.getName());
    
    private MainFrame mainFrame;
    
    private NewContact newContact = new NewContact();
    
    private AddContactWizardPage1 page1;
    
    private AddContactWizardPage2 page2;
    
    private AddContactWizardPage3 page3;
    
    public AddContactWizard(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
        
        this.getDialog().setTitle(Messages.getString("addContactWizard"));

        page1 = new AddContactWizardPage1(this, newContact,
                    mainFrame.getProtocolProviders());

        this.registerWizardPage(AddContactWizardPage1.IDENTIFIER, page1);

        page2 = new AddContactWizardPage2(this, newContact,
                    mainFrame.getAllGroups());

        this.registerWizardPage(AddContactWizardPage2.IDENTIFIER, page2);

        page3 = new AddContactWizardPage3(this, newContact);

        this.registerWizardPage(AddContactWizardPage3.IDENTIFIER, page3);

        this.setCurrentPage(AddContactWizardPage1.IDENTIFIER);
        
        this.centerWizard();
        
        this.mainFrame.getContactList().addMetaContactListListener(this);
    }
    
    
    /**
     * Sets the location of the wizard at the center of the screen.
     */
    public void centerWizard()
    {
        this.getDialog().setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - 250,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - 100
                );
    }
    
    /**
     * Overrides the Wizard.showModalDialog method.
     */
    public int showModalDialog() {
        int returnCode = super.showModalDialog();
        
        if(returnCode == 0) {
            
            if(newContact.getNewGroup() != null
                    && !newContact.getNewGroup().equals("")) {                
                new CreateGroup(mainFrame.getContactList(), 
                        newContact).start();
            }
            
            ArrayList ppList = newContact.getProtocolProviders();
            ArrayList groupList = newContact.getGroups();

            for(int i = 0; i < ppList.size(); i ++) {
                ProtocolProviderService pps
                    = (ProtocolProviderService)ppList.get(i);

                for(int j = 0; j < groupList.size(); j++) {
                    MetaContactGroup group
                        = (MetaContactGroup)groupList.get(j);
                    
                    new CreateContact(pps, group, newContact).start();
                }
            }
        }
        else if(returnCode == 1) {
            this.getDialog().dispose();
        }
        
        return returnCode;
    }
    
    /**
     * Creates a new meta contact in a separate thread.
     */
    private class CreateContact extends Thread {
        ProtocolProviderService pps;
        MetaContactGroup group;
        NewContact newContact;
        CreateContact(ProtocolProviderService pps,
                MetaContactGroup group,
                NewContact newContact) {
            this.pps = pps;
            this.group = group;
            this.newContact = newContact;
        }
        public void run() {
            try {
                mainFrame.getContactList()
                    .createMetaContact(
                    pps, group, newContact.getUin());
            }
            catch (MetaContactListException ex) {
                logger.error(ex);
                int errorCode = ex.getErrorCode();
                
                if (errorCode
                    == MetaContactListException.CODE_LOCAL_IO_ERROR) {
                    
                    JOptionPane.showMessageDialog(mainFrame,
                        Messages.getString(
                                "addContactError",
                                newContact.getNewGroup()),
                        Messages.getString(
                                "addContactErrorTitle"),
                        JOptionPane.WARNING_MESSAGE);
                }
                else if (errorCode
                        == MetaContactListException.CODE_NETWORK_ERROR) {
                    
                    JOptionPane.showMessageDialog(mainFrame,
                            Messages.getString(
                                    "addContactError",
                                    newContact.getNewGroup()),
                            Messages.getString(
                                    "addContactErrorTitle"),
                            JOptionPane.WARNING_MESSAGE);
                }
                else if (errorCode
                        == MetaContactListException.CODE_UNKNOWN_ERROR) {
                    
                    JOptionPane.showMessageDialog(mainFrame,
                            Messages.getString(
                                    "addContactError",
                                    newContact.getNewGroup()),
                            Messages.getString(
                                    "addContactErrorTitle"),
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Creates a new meta contact group in a separate thread.
     */
    private class CreateGroup extends Thread {
        MetaContactListService mcl;
        NewContact newContact;
        
        CreateGroup(MetaContactListService mcl,
                NewContact newContact) {
            this.mcl = mcl;
            this.newContact = newContact;
        }
        public void run() {
            try {
                mcl.createMetaContactGroup(
                    mcl.getRoot(), newContact.getNewGroup());
            }
            catch (MetaContactListException ex) {
                logger.error(ex);
                int errorCode = ex.getErrorCode();
                
                if (errorCode
                    == MetaContactListException.CODE_LOCAL_IO_ERROR) {
                    
                    JOptionPane.showMessageDialog(mainFrame,
                        Messages.getString(
                                "addGroupLocalError",
                                newContact.getNewGroup()),
                        Messages.getString(
                                "addGroupErrorTitle"),
                        JOptionPane.WARNING_MESSAGE);
                }
                else if (errorCode
                        == MetaContactListException.CODE_NETWORK_ERROR) {
                    
                    JOptionPane.showMessageDialog(mainFrame,
                            Messages.getString(
                                    "addGroupNetError",
                                    newContact.getNewGroup()),
                            Messages.getString(
                                    "addGroupErrorTitle"),
                            JOptionPane.WARNING_MESSAGE);
                }
                else if (errorCode
                        == MetaContactListException.CODE_UNKNOWN_ERROR) {
                    
                    JOptionPane.showMessageDialog(mainFrame,
                            Messages.getString(
                                    "addGroupError",
                                    newContact.getNewGroup()),
                            Messages.getString(
                                    "addGroupErrorTitle"),
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    public void metaContactAdded(MetaContactEvent evt)
    {}

    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {}

    public void protoContactAdded(ProtoContactEvent evt)
    {}

    public void protoContactRemoved(ProtoContactEvent evt)
    {}

    public void protoContactMoved(ProtoContactEvent evt)
    {}

    public void metaContactRemoved(MetaContactEvent evt)
    {}

    public void metaContactMoved(MetaContactMovedEvent evt)
    {}

    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        ArrayList ppList = newContact.getProtocolProviders();
        
        for(int i = 0; i < ppList.size(); i ++) {
            ProtocolProviderService pps
                = (ProtocolProviderService)ppList.get(i);
            
            MetaContactGroup group
                = (MetaContactGroup)evt.getSourceMetaContactGroup();
            
            new CreateContact(pps, group, newContact).start();
            
        }
    }

    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {}

    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {}

    public void childContactsReordered(MetaContactGroupEvent evt)
    {}
}
