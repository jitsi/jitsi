/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.AddContactDialog;
import net.java.sip.communicator.impl.gui.main.customcontrols.MessageDialog;
import net.java.sip.communicator.impl.gui.main.history.HistoryWindow;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * The ContactRightButtonMenu is the menu, which the user could open by clicking
 * on a contact in the contact list with the right button of the mouse.
 * 
 * @author Yana Stamcheva
 */
public class ContactRightButtonMenu extends JPopupMenu implements
        ActionListener {

    private JMenu moveToMenu = new JMenu(Messages.getString("moveToGroup"));

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
                    .getAccountUserID(),
                    new ImageIcon(Constants.getProtocolIcon(protocolName)));
            
            menuItem.setName(protocolName);
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
            
            menuItem.setName(group.getMetaUID());
            menuItem.addActionListener(this);
            
            this.moveToMenu.add(menuItem);
        }
        
        //Initialize removeContact menu.
        Iterator contacts = contactItem.getContacts();
        
        if (contactItem.getContactCount() > 1) {
           JMenuItem allItem = new JMenuItem(Messages.getString("allContacts"));
           
           allItem.addActionListener(this);
           
           allItem.setName("allContacts");           
           this.removeContactMenu.add(allItem);
           
           this.removeContactMenu.addSeparator();
        }
            
        while (contacts.hasNext()) {
            Contact contact = (Contact)contacts.next();
         
            JMenuItem contactItem = new JMenuItem(contact.getDisplayName());
            
            contactItem.setName(contact.getAddress()
                    + contact.getProtocolProvider().getProtocolName());
            
            contactItem.addActionListener(this);
            
            this.removeContactMenu.add(contactItem);
        }
        
        this.add(sendMessageItem);
        this.add(sendFileItem);

        this.addSeparator();

        this.add(moveToMenu);

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
        this.viewHistoryItem.setEnabled(false);
        this.userInfoItem.setEnabled(false);        
    }

    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        String itemText = menuItem.getText();
        
        if(mainFrame.getProtocolProviderForAccount(itemText) != null) {
            ProtocolProviderService pps 
                = mainFrame.getProtocolProviderForAccount(itemText);
            
            AddContactDialog dialog = new AddContactDialog(
                    mainFrame.getContactList(),
                    contactItem, pps);
            
            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2 
                        - 250,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2 
                        - 100
                    );
            
            dialog.setVisible(true);
        }        
        else if (itemName.equalsIgnoreCase("sendMessage")) {
            ContactListPanel clistPanel = mainFrame.getTabbedPane()
                    .getContactListPanel();
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
        } 
        else if (itemName.equalsIgnoreCase("viewHistory")) {

            HistoryWindow history = new HistoryWindow();

            history.setContact(this.contactItem);
            history.setVisible(true);
        } 
        else if (itemName.equalsIgnoreCase("userInfo")) {

        }
        else if (mainFrame.getGroupByID(itemName) != null) {
            MetaContactGroup group = mainFrame.getGroupByID(itemName);
           
            mainFrame.getContactList().moveMetaContact(contactItem, group);
        }
        else if (getContactFromMetaContact(itemName) != null) {            
            Contact contact = getContactFromMetaContact(itemName);
            
            if(Constants.REMOVE_CONTACT_ASK) {
                String message = "<HTML>Are you sure you want to remove <B>"
                    + this.contactItem.getDisplayName()
                    + "</B><BR>from your contact list?</html>";
                
                MessageDialog dialog = new MessageDialog(this.mainFrame,
                        message, Messages.getString("remove"));
    
                int returnCode = dialog.showDialog();
                
                if (returnCode == MessageDialog.OK_RETURN_CODE) {
                    this.mainFrame.getContactList().removeContact(contact);
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                    this.mainFrame.getContactList().removeContact(contact);
                    
                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else {
                this.mainFrame.getContactList().removeContact(contact);
            }
        }
        else if (itemName.equals("allContacts")) {
            if(Constants.REMOVE_CONTACT_ASK) {
                String message = "<HTML>Are you sure you want to remove <B>"
                    + this.contactItem.getDisplayName()
                    + "</B><BR>from your contact list?</html>";
                
                MessageDialog dialog = new MessageDialog(this.mainFrame,
                        message, Messages.getString("remove"));
    
                int returnCode = dialog.showDialog();
                
                if (returnCode == MessageDialog.OK_RETURN_CODE) {
                    this.mainFrame.getContactList().removeMetaContact(contactItem);
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                    this.mainFrame.getContactList().removeMetaContact(contactItem);
                    
                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else {
                this.mainFrame.getContactList().removeMetaContact(contactItem);
            }
        }
    }
    
    private Contact getContactFromMetaContact(String itemID) {
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
}
