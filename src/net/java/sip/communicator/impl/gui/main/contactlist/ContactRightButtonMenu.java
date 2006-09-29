/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.message.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * right mouse button on a contact in the contact list. Through this menu the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 */
public class ContactRightButtonMenu
    extends JPopupMenu
    implements  ActionListener,
                ContactListListener
{

    private JMenu moveToMenu = new JMenu(Messages.getString("moveToGroup"));

    private JMenu moveSubcontactMenu
        = new JMenu(Messages.getString("moveSubcontact"));
    
    private JMenu addSubcontactMenu = new JMenu(Messages
            .getString("addSubcontact"));

    private JMenu removeContactMenu = new JMenu(Messages
            .getString("removeContact"));

    private JMenuItem sendMessageItem = new JMenuItem(Messages
            .getString("sendMessage"), new ImageIcon(ImageLoader
            .getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

    private JMenuItem sendFileItem = new JMenuItem(Messages
            .getString("sendFile"), new ImageIcon(ImageLoader
            .getImage(ImageLoader.SEND_FILE_16x16_ICON)));

    private JMenuItem renameContactItem = new JMenuItem(Messages
            .getString("renameContact"), new ImageIcon(ImageLoader
            .getImage(ImageLoader.RENAME_16x16_ICON)));

    private JMenuItem userInfoItem = new JMenuItem(Messages
            .getString("userInfo"), new ImageIcon(ImageLoader
            .getImage(ImageLoader.INFO_16x16_ICON)));

    private JMenuItem viewHistoryItem = new JMenuItem(Messages
            .getString("viewHistory"), new ImageIcon(ImageLoader
            .getImage(ImageLoader.HISTORY_16x16_ICON)));

    private MetaContact contactItem;

    private MainFrame mainFrame;
    
    private String moveToPrefix = "moveTo:";
    
    private String removeContactPrefix = "removeContact:";
    
    private String addSubcontactPrefix = "addSubcontact:";
    
    private String moveSubcontactPrefix = "moveSubcontact:";
    
    private Contact contactToMove;
    
    private boolean moveAllContacts = false;
    
    private MoveSubcontactMessageDialog moveDialog;

    /**
     * Creates an instance of ContactRightButtonMenu.
     * @param mainFrame The parent MainFrame window.
     * @param contactItem The MetaContact for which the menu is opened.
     */
    public ContactRightButtonMenu(MainFrame mainFrame,
                                MetaContact contactItem) {
        super();

        this.mainFrame = mainFrame;

        this.contactItem = contactItem;

        this.setLocation(getLocation());

        this.init();
        this.initMnemonics();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init() {

        this.moveToMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.GROUPS_16x16_ICON)));

        this.addSubcontactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.removeContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.DELETE_16x16_ICON)));

        //Initialize the addSubcontact menu.
        Iterator providers = this.mainFrame.getProtocolProviders();

        if(providers.hasNext()) {
            JLabel infoLabel = new JLabel(Messages.getString("selectAccount"));

            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            infoLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

            this.addSubcontactMenu.add(infoLabel);
            this.addSubcontactMenu.addSeparator();
        }

        while (providers.hasNext()) {
            ProtocolProviderService pps
                = (ProtocolProviderService)providers.next();

            String protocolName = pps.getProtocolName();

            JMenuItem menuItem = new JMenuItem(pps.getAccountID()
                    .getUserID(),
                    new ImageIcon(Constants.getProtocolIcon(protocolName)));

            menuItem.setName(addSubcontactPrefix + protocolName);
            menuItem.addActionListener(this);

            this.addSubcontactMenu.add(menuItem);
        }

        //Initialize moveTo menu.
        Iterator groups = this.mainFrame.getAllGroups();

        if(groups.hasNext()) {
            JLabel infoLabel = new JLabel(Messages.getString("selectGroup"));

            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            infoLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

            this.moveToMenu.add(infoLabel);
            this.moveToMenu.addSeparator();
        }

        while (groups.hasNext()) {
            MetaContactGroup group = (MetaContactGroup)groups.next();

            JMenuItem menuItem = new JMenuItem(group.getGroupName());

            menuItem.setName(moveToPrefix + group.getMetaUID());
            menuItem.addActionListener(this);

            this.moveToMenu.add(menuItem);
        }

        //Initialize removeContact menu.
        Iterator contacts = contactItem.getContacts();

        if (contactItem.getContactCount() > 1) {
           JMenuItem allItem = new JMenuItem(Messages.getString("allContacts"));
           JMenuItem allItem1 = new JMenuItem(Messages.getString("allContacts"));
           
           allItem.addActionListener(this);
           allItem1.addActionListener(this);
           
           allItem.setName(removeContactPrefix + "allContacts");
           allItem1.setName(moveSubcontactPrefix + "allContacts");
           
           this.removeContactMenu.add(allItem);
           this.moveSubcontactMenu.add(allItem1);
           this.removeContactMenu.addSeparator();
           this.moveSubcontactMenu.addSeparator();
        }

        while (contacts.hasNext()) {
            Contact contact = (Contact)contacts.next();

            JMenuItem contactItem = new JMenuItem(contact.getDisplayName());
            JMenuItem contactItem1 = new JMenuItem(contact.getDisplayName());

            contactItem.setName(removeContactPrefix + contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName());

            contactItem1.setName(moveSubcontactPrefix + contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName());
            
            contactItem.addActionListener(this);
            contactItem1.addActionListener(this);
            
            this.removeContactMenu.add(contactItem);
            this.moveSubcontactMenu.add(contactItem1);
        }

        this.add(sendMessageItem);
        this.add(sendFileItem);

        this.addSeparator();

        this.add(moveToMenu);
        this.add(moveSubcontactMenu);

        this.addSeparator();

        this.add(addSubcontactMenu);

        this.addSeparator();

        this.add(removeContactMenu);
        this.add(renameContactItem);

        this.addSeparator();

        this.add(viewHistoryItem);
        this.add(userInfoItem);

        this.sendMessageItem.setName("sendMessage");
        this.sendFileItem.setName("sendFile");
        this.moveToMenu.setName("moveToGroup");
        this.addSubcontactMenu.setName("addSubcontact");
        this.renameContactItem.setName("renameContact");
        this.viewHistoryItem.setName("viewHistory");
        this.userInfoItem.setName("userInfo");

        this.sendMessageItem.addActionListener(this);
        this.sendFileItem.addActionListener(this);
        this.renameContactItem.addActionListener(this);
        this.viewHistoryItem.addActionListener(this);
        this.userInfoItem.addActionListener(this);

        // Disable all menu items that do nothing.
        this.sendFileItem.setEnabled(false);
    }
    
    private void initMnemonics() {
        this.sendMessageItem.setMnemonic(
                Messages.getString("mnemonic.sendMessage").charAt(0));
        this.sendFileItem.setMnemonic(
                Messages.getString("mnemonic.sendFile").charAt(0));
        this.moveToMenu.setMnemonic(
                Messages.getString("mnemonic.moveTo").charAt(0));
        this.addSubcontactMenu.setMnemonic(
                Messages.getString("mnemonic.addSubcontact").charAt(0));
        this.removeContactMenu.setMnemonic(
                Messages.getString("mnemonic.removeContact").charAt(0));
        this.renameContactItem.setMnemonic(
                Messages.getString("mnemonic.renameContact").charAt(0));
        this.viewHistoryItem.setMnemonic(
                Messages.getString("mnemonic.viewHistory").charAt(0));
        this.userInfoItem.setMnemonic(
                Messages.getString("mnemonic.userInfo").charAt(0));
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and makes the appropriate operations.
     */
    public void actionPerformed(ActionEvent e){

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        String itemText = menuItem.getText();

        if (itemName.startsWith(addSubcontactPrefix)) {
            
            ProtocolProviderService pps
                = mainFrame.getProtocolProviderForAccount(itemText);

            if(pps != null) {
                AddContactDialog dialog = new AddContactDialog(
                        mainFrame,
                        contactItem, pps);
    
                dialog.setLocation(
                        Toolkit.getDefaultToolkit().getScreenSize().width/2
                            - 250,
                        Toolkit.getDefaultToolkit().getScreenSize().height/2
                            - 100
                        );
    
                dialog.setVisible(true);
            }
        }
        else if (itemName.equalsIgnoreCase("sendMessage")) {
            ContactListPanel clistPanel = mainFrame.getContactListPanel();
            SwingUtilities.invokeLater(clistPanel.new RunMessageWindow(
                    contactItem));
        }
        else if (itemName.equalsIgnoreCase("sendFile")) {
            // disabled
        }
        else if (itemName.equalsIgnoreCase("renameContact")) {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame.getContactList(), contactItem);

            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2
                        - 200,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2
                        - 50
                    );

            dialog.setVisible(true);
            
            dialog.requestFocusInFiled();
        }
        else if (itemName.equalsIgnoreCase("viewHistory")) {

            HistoryWindow history = new HistoryWindow(
                    this.mainFrame, this.contactItem);

            history.setVisible(true);
        }
        else if (itemName.equalsIgnoreCase("userInfo")) {
            Contact defaultContact = contactItem.getDefaultContact();

            ProtocolProviderService defaultProvider
                = defaultContact.getProtocolProvider();

            OperationSetWebContactInfo wContactInfo
                = mainFrame.getWebContactInfo(defaultProvider);

            CrossPlatformBrowserLauncher.openURL(
                    wContactInfo.getWebContactInfo(defaultContact)
                        .toString());
        }
        else if (itemName.startsWith(moveToPrefix)) {
            
            MetaContactGroup group
                = mainFrame.getGroupByID(
                        itemName.substring(moveToPrefix.length()));

            if(group != null) {
                mainFrame.getContactList().moveMetaContact(contactItem, group);
            }
        }
        else if (itemName.startsWith(removeContactPrefix)) {
            
            Contact contact = getContactFromMetaContact(
                    itemName.substring(removeContactPrefix.length()));

            if(contact != null) {
                if(Constants.REMOVE_CONTACT_ASK) {
                    String message = "<HTML>Are you sure you want to remove <B>"
                        + contact.getDisplayName()
                        + "</B><BR>from your contact list?</html>";
    
                    MessageDialog dialog = new MessageDialog(this.mainFrame,
                            message, Messages.getString("remove"));
    
                    int returnCode = dialog.showDialog();
    
                    if (returnCode == MessageDialog.OK_RETURN_CODE) {
                        new RemoveContactThread(contact).start();
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                        new RemoveContactThread(contact).start();
    
                        Constants.REMOVE_CONTACT_ASK = false;
                    }
                }
                else {
                    new RemoveContactThread(contact).start();
                }
            }
            else {
                if(Constants.REMOVE_CONTACT_ASK) {
                    String message = "<HTML>Are you sure you want to remove <B>"
                        + Messages.getString("allContacts")
                        + "</B><BR>from your contact list?</html>";
    
                    MessageDialog dialog = new MessageDialog(this.mainFrame,
                            message, Messages.getString("remove"));
    
                    int returnCode = dialog.showDialog();
    
                    if (returnCode == MessageDialog.OK_RETURN_CODE) {
                        new RemoveAllContactsThread().start();
                    }
                    else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                        new RemoveAllContactsThread().start();
    
                        Constants.REMOVE_CONTACT_ASK = false;
                    }
                }
                else {
                    new RemoveAllContactsThread().start();
                }
            }
        }
        else if(itemName.startsWith(moveSubcontactPrefix)) {
            Contact contact = getContactFromMetaContact(
                    itemName.substring(moveSubcontactPrefix.length()));

            mainFrame.getContactListPanel()
                .getContactList().addExcContactListListener(this);
            
            this.moveDialog = new MoveSubcontactMessageDialog(mainFrame);
            
            this.moveDialog.setVisible(true);
                        
            if(contact != null) {
                this.contactToMove = contact;                
            }
            else {
                this.moveAllContacts = true;
            }
        }
    }   

    /**
     * Obtains the <tt>Contact</tt> corresponding to the given address
     * identifier.
     *
     * @param itemID The address of the <tt>Contact</tt>.
     * @return the <tt>Contact</tt> corresponding to the given address
     * identifier.
     */
    private Contact getContactFromMetaContact(String itemID)
    {
        Iterator i = contactItem.getContacts();

        while(i.hasNext()) {
            Contact contact = (Contact)i.next();

            String id = contact.getAddress()
                + contact.getProtocolProvider().getProtocolName();

            if(itemID.equals(id)) {
                return contact;
            }
        }
        return null;
    }
    
    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private class RemoveContactThread extends Thread
    {
        private Contact contact;
        public RemoveContactThread(Contact contact) {
            this.contact = contact;
        }
        public void run() {
            mainFrame.getContactList().removeContact(contact);
        }
    }
    
    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private class RemoveAllContactsThread extends Thread
    {
        public void run() {
            mainFrame.getContactList().removeMetaContact(contactItem);
        }
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the choosen subcontact when a meta contact is selected.
     */
    public void contactSelected(ContactListEvent evt)
    {
        this.moveContact(evt.getSourceContact());
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the choosen subcontact when a meta contact is selected.
     */
    public void protocolContactSelected(ContactListEvent evt)
    {
        this.moveContact(evt.getSourceContact());
    }
    
    /**
     * Moves the previously choosen subcontact in the given toMetaContact.
     * 
     * @param toMetaContact the MetaContact, where to move the previously
     * choosen subcontact.
     */
    private void moveContact(MetaContact toMetaContact)
    {        
        this.moveDialog.dispose();
        
        if(toMetaContact.equals(contactItem)) {
            JOptionPane.showMessageDialog(this.mainFrame,
                    Messages.getString("moveSubcontactInSameContact"),
                    Messages.getString("moveSubcontact"),
                    JOptionPane.WARNING_MESSAGE);
        }
        else {
            this.mainFrame.getContactListPanel().getContactList()
                .removeExcContactListListener(this);
            
            if(moveAllContacts) {
                new MoveAllSubcontactsThread(toMetaContact).start();
            }
            else if(contactToMove != null) {
                new MoveSubcontactThread(toMetaContact).start();
            }
        }
    }
    
    /**
     * 
     */
    private class MoveSubcontactThread extends Thread
    {
        private MetaContact metaContact;
        
        public MoveSubcontactThread(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }
        
        public void run()
        {
            mainFrame.getContactList()
                .moveContact(contactToMove, metaContact);
        }
    }
    
    /**
     * 
     */
    private class MoveAllSubcontactsThread extends Thread
    {
        private MetaContact metaContact;
        
        public MoveAllSubcontactsThread(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }
        
        public void run()
        {
            Iterator i = contactItem.getContacts();
            
            while(i.hasNext()) {
                Contact contact = (Contact) i.next();
                mainFrame.getContactList()
                    .moveContact(contact, metaContact);
            }
        }
    }
}
