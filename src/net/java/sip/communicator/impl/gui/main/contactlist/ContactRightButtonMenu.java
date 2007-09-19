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
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The ContactRightButtonMenu is the menu, opened when user clicks with the
 * user could add a subcontact, remove a contact, send message, etc.
 *
 * @author Yana Stamcheva
 */
public class ContactRightButtonMenu
    extends JPopupMenu
    implements  ActionListener,
                PluginComponentListener,
                ContactListListener
{

    private I18NString allContactsString
        = Messages.getI18NString("allContacts");
    
    private I18NString moveToString
        = Messages.getI18NString("moveToGroup");
    
    private I18NString moveSubcontactString
        = Messages.getI18NString("moveSubcontact");
    
    private I18NString userInfoString
        = Messages.getI18NString("userInfo");
    
    private I18NString addSubcontactString
        = Messages.getI18NString("addSubcontact");
    
    private I18NString removeContactString
        = Messages.getI18NString("removeContact");

    private I18NString callString
        = Messages.getI18NString("call");

    private I18NString sendMessageString
        = Messages.getI18NString("sendMessage");
    
    private I18NString sendFileString
        = Messages.getI18NString("sendFile");
    
    private I18NString renameContactString
        = Messages.getI18NString("renameContact");
    
    private I18NString viewHistoryString
    = Messages.getI18NString("viewHistory");
    
    
    private SIPCommMenu moveToMenu = new SIPCommMenu(moveToString.getText());

    private SIPCommMenu moveSubcontactMenu
        = new SIPCommMenu(moveSubcontactString.getText());
    
    private SIPCommMenu userInfoMenu
        = new SIPCommMenu(userInfoString.getText());
    
    private SIPCommMenu addSubcontactMenu
        = new SIPCommMenu(addSubcontactString.getText());

    private SIPCommMenu removeContactMenu
        = new SIPCommMenu(removeContactString.getText());

    private JMenuItem callItem = new JMenuItem(
        callString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CALL_16x16_ICON)));

    private JMenuItem sendMessageItem = new JMenuItem(
        sendMessageString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_MESSAGE_16x16_ICON)));

    private JMenuItem sendFileItem = new JMenuItem(
        sendFileString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEND_FILE_16x16_ICON)));

    private JMenuItem renameContactItem = new JMenuItem(
        renameContactString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.RENAME_16x16_ICON)));

    private JMenuItem viewHistoryItem = new JMenuItem(
        viewHistoryString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.HISTORY_16x16_ICON)));

    private MetaContact contactItem;

    private MainFrame mainFrame;
    
    private String moveToPrefix = "moveTo:";
    
    private String removeContactPrefix = "removeContact:";
    
    private String addSubcontactPrefix = "addSubcontact:";
    
    private String moveSubcontactPrefix = "moveSubcontact:";
    
    private String infoSubcontactPrefix = "infoSubcontact:";
    
    private Contact contactToMove;
    
    private boolean moveAllContacts = false;
    
    private MoveSubcontactMessageDialog moveDialog;

    private ContactList guiContactList;
    /**
     * Creates an instance of ContactRightButtonMenu.
     * @param contactList The contact list over which this menu is shown.
     * @param contactItem The MetaContact for which the menu is opened.
     */
    public ContactRightButtonMenu(ContactList contactList,
                                MetaContact contactItem) {
        super();

        this.mainFrame = contactList.getMainFrame();

        this.guiContactList = contactList;
        
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
        
        this.moveSubcontactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.MOVE_CONTACT_ICON)));
        
        this.userInfoMenu.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.INFO_16x16_ICON)));

        //Initialize the addSubcontact menu.
        Iterator providers = this.mainFrame.getProtocolProviders();

        if(providers.hasNext()) {
            JLabel infoLabel = new JLabel(
                Messages.getI18NString("selectAccount").getText());

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
                    new ImageIcon(createAccountStatusImage(pps)));

            menuItem.setName(addSubcontactPrefix + protocolName);
            menuItem.addActionListener(this);

            this.addSubcontactMenu.add(menuItem);
        }

        //Initialize moveTo menu.
        Iterator groups = this.mainFrame.getAllGroups();

        if(groups.hasNext()) {
            JLabel infoLabel = new JLabel(
                Messages.getI18NString("selectGroup").getText());

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
            
            JMenuItem allItem = new JMenuItem(allContactsString.getText());
            JMenuItem allItem1 = new JMenuItem(allContactsString.getText());
           
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

            ProtocolProviderService protocolProvider
                = contact.getProtocolProvider();
            
            String contactDisplayName = contact.getDisplayName();
            
            JMenuItem contactItem = new JMenuItem(contactDisplayName);
            JMenuItem contactItem1 = new JMenuItem(contactDisplayName);
            JMenuItem contactItem2 = new JMenuItem(contactDisplayName);

            Icon protocolIcon = new ImageIcon(
                    createContactStatusImage(contact));
            
            contactItem.setIcon(protocolIcon);
            contactItem1.setIcon(protocolIcon);
            
            contactItem.setName(removeContactPrefix + contact.getAddress()
                    + protocolProvider.getProtocolName());

            contactItem1.setName(moveSubcontactPrefix + contact.getAddress()
                    + protocolProvider.getProtocolName());
            
            contactItem2.setName(infoSubcontactPrefix + contact.getAddress()
                    + protocolProvider.getProtocolName());
            
            contactItem.addActionListener(this);
            contactItem1.addActionListener(this);
            contactItem2.addActionListener(this);
            
            this.removeContactMenu.add(contactItem);
            this.moveSubcontactMenu.add(contactItem1);

            OperationSetWebContactInfo wContactInfo
                = mainFrame.getWebContactInfoOpSet(protocolProvider);
            
            if(wContactInfo == null) {
                contactItem2.setEnabled(false);
                contactItem2.setToolTipText(
                        Messages.getI18NString("dontSupportWebInfo").getText());
            }
            this.userInfoMenu.add(contactItem2);
        }

        this.add(sendMessageItem);
        this.add(callItem);
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
        this.add(userInfoMenu);
        
        this.initPluginComponents();

        this.sendMessageItem.setName("sendMessage");
        this.callItem.setName("call");
        this.sendFileItem.setName("sendFile");
        this.moveToMenu.setName("moveToGroup");
        this.addSubcontactMenu.setName("addSubcontact");
        this.renameContactItem.setName("renameContact");
        this.viewHistoryItem.setName("viewHistory");
        this.userInfoMenu.setName("userInfo");

        this.sendMessageItem.addActionListener(this);
        this.callItem.addActionListener(this);
        this.sendFileItem.addActionListener(this);
        this.renameContactItem.addActionListener(this);
        this.viewHistoryItem.addActionListener(this);
        this.userInfoMenu.addActionListener(this);

        // Disable all menu items that do nothing.
        if (contactItem.getDefaultContact(OperationSetFileTransfer.class)
                == null)
            this.sendFileItem.setEnabled(false);
        if (contactItem.getDefaultContact(OperationSetBasicTelephony.class)
                == null)
            this.callItem.setEnabled(false);
        if (contactItem.getDefaultContact(OperationSetBasicInstantMessaging.class)
                == null)
            this.sendMessageItem.setEnabled(false);        
    }
    
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
        
        if(pluginComponents.hasNext())
            this.addSeparator();
        
        while (pluginComponents.hasNext())
        {
            Component o = (Component)pluginComponents.next();
            
            this.add(o);
            
            if (o instanceof ContactAwareComponent)
                ((ContactAwareComponent)o).setCurrentContact(contactItem);
        }
        
        GuiActivator.getUIService().addPluginComponentListener(this);
    }
    
    private void initMnemonics()
    {
        this.sendMessageItem.setMnemonic(sendMessageString.getMnemonic());
        this.callItem.setMnemonic(sendMessageString.getMnemonic());
        this.sendFileItem.setMnemonic(sendFileString.getMnemonic());
        this.moveToMenu.setMnemonic(moveToString.getMnemonic());
        this.addSubcontactMenu.setMnemonic(addSubcontactString.getMnemonic());
        this.removeContactMenu.setMnemonic(removeContactString.getMnemonic());
        this.renameContactItem.setMnemonic(renameContactString.getMnemonic());
        this.viewHistoryItem.setMnemonic(viewHistoryString.getMnemonic());
        this.userInfoMenu.setMnemonic(userInfoString.getMnemonic());
        this.moveSubcontactMenu.setMnemonic(moveSubcontactString.getMnemonic());
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and makes the appropriate operations.
     */
    public void actionPerformed(ActionEvent e){

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        String itemText = menuItem.getText();
        Contact cont = null;

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
        else if (itemName.equalsIgnoreCase("sendMessage"))
        {
            ContactListPanel clistPanel = mainFrame.getContactListPanel();
            SwingUtilities.invokeLater(clistPanel.new RunMessageWindow(
                    contactItem));
        }
        else if (itemName.equalsIgnoreCase("call"))
        {
            cont = contactItem.getDefaultContact(
                    OperationSetBasicTelephony.class);
            if (cont != null)
            {
                Vector v = new Vector();
                v.add(cont);
                mainFrame.getCallManager().createCall(v);
                // wow, it's really tricky, I wonder there isn't a simple method
                // CallManager#createCall(Contact contact);
            }
        }
        else if (itemName.equalsIgnoreCase("sendFile"))
        {
            // disabled
        }
        else if (itemName.equalsIgnoreCase("renameContact"))
        {
            RenameContactDialog dialog = new RenameContactDialog(
                    mainFrame, contactItem);

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

            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = mainFrame.getHistoryWindowManager();

            if(historyWindowManager
                .containsHistoryWindowForContact(contactItem))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(contactItem);

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else {
                history = new HistoryWindow(
                    this.mainFrame, this.contactItem);

                historyWindowManager
                    .addHistoryWindowForContact(contactItem, history);

                history.setVisible(true);
            }
        }
        else if (itemName.startsWith(infoSubcontactPrefix)) {
                        
            Contact contact = getContactFromMetaContact(
                    itemName.substring(infoSubcontactPrefix.length()));
            
            ProtocolProviderService contactProvider
                = contact.getProtocolProvider();

            OperationSetWebContactInfo wContactInfo
                = mainFrame.getWebContactInfoOpSet(contactProvider);

            GuiActivator.getBrowserLauncher().openURL(
                    wContactInfo.getWebContactInfo(contact)
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
                new RemoveContactThread(contact).start();                
            }
            else {
                new RemoveAllContactsThread().start();
            }
        }
        else if(itemName.startsWith(moveSubcontactPrefix)) {
            Contact contact = getContactFromMetaContact(
                    itemName.substring(moveSubcontactPrefix.length()));

            guiContactList.addExcContactListListener(this);
            guiContactList.setCursor(
                    Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            
            this.moveDialog = new MoveSubcontactMessageDialog(mainFrame, this);
            
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
            if(Constants.REMOVE_CONTACT_ASK) {
                String message = "<HTML>Are you sure you want to remove <B>"
                    + contact.getDisplayName()
                    + "</B><BR>from your contact list?</html>";
    
                MessageDialog dialog = new MessageDialog(mainFrame,
                        message, Messages.getI18NString("remove").getText());
    
                int returnCode = dialog.showDialog();
                
                if (returnCode == MessageDialog.OK_RETURN_CODE) {
                    mainFrame.getContactList().removeContact(contact);
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                    mainFrame.getContactList().removeContact(contact);
    
                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else {
                mainFrame.getContactList().removeContact(contact);
            }
        }
    }
    
    /**
     * Removes a contact from a meta contact in a separate thread.
     */
    private class RemoveAllContactsThread extends Thread
    {
        public void run() {
            if(Constants.REMOVE_CONTACT_ASK) {
                String message
                    = "<HTML>Are you sure you want to remove <B>"
                    + allContactsString.getText()
                    + "</B><BR>from your contact list?</html>";
        
                MessageDialog dialog = new MessageDialog(mainFrame,
                        message, Messages.getI18NString("remove").getText());
        
                int returnCode = dialog.showDialog();
        
                if (returnCode == MessageDialog.OK_RETURN_CODE) {
                    mainFrame.getContactList().removeMetaContact(contactItem);
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                    mainFrame.getContactList().removeMetaContact(contactItem);
        
                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else {
                mainFrame.getContactList().removeMetaContact(contactItem);
            }   
        }
    }
    
    public void groupSelected(ContactListEvent evt)
    {   
        this.moveDialog.dispose();
        
        MetaContactGroup sourceGroup = evt.getSourceGroup();
        
        guiContactList.removeExcContactListListener(this);
        
        guiContactList.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        if(moveAllContacts) {
            mainFrame.getContactList()
                .moveMetaContact(contactItem, sourceGroup);
        }
        else if(contactToMove != null) {
            new MoveSubcontactThread(sourceGroup).start();
        }
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the choosen subcontact when a meta contact is selected.
     */
    public void contactClicked(ContactListEvent evt)
    {
        this.moveContact(evt.getSourceContact());
    }

    /**
     * Implements ContactListListener.contactSelected method in order
     * to move the choosen subcontact when a meta contact is selected.
     */
    public void protocolContactClicked(ContactListEvent evt)
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
            new ErrorDialog(this.mainFrame,
                    Messages.getI18NString("moveSubcontactInSameContact").getText(),
                    Messages.getI18NString("moveSubcontact").getText(),
                    ErrorDialog.WARNING)
                    .showDialog();
        }
        else {
            guiContactList.removeExcContactListListener(this);
            
            guiContactList.setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if(moveAllContacts) {
                new MoveAllSubcontactsThread(toMetaContact).start();
            }
            else if(contactToMove != null) {
                new MoveSubcontactThread(toMetaContact).start();
            }
        }
    }
    
    /**
     * Moves the previously choosen contact in the given meta group or meta
     * contact.
     */
    private class MoveSubcontactThread extends Thread
    {
        private MetaContact metaContact;
        
        private MetaContactGroup metaGroup;
        
        public MoveSubcontactThread(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }
        
        public MoveSubcontactThread(MetaContactGroup metaGroup)
        {
            this.metaGroup = metaGroup;
        }
        
        public void run()
        {
            if(metaContact != null) {
                mainFrame.getContactList()
                    .moveContact(contactToMove, metaContact);
            }
            else {
                mainFrame.getContactList()
                    .moveContact(contactToMove, metaGroup);
            }
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
    
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        if(event.getContainerID()
                .equals(UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            this.add(c);
            
            if (c instanceof ContactAwareComponent)
            {   
                ((ContactAwareComponent)c)
                    .setCurrentContact(contactItem);
            }
            
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        if(event.getContainerID()
                .equals(UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            this.remove(c);
        }
    }
    
    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param pps the protocol provider for which to create the image
     * @return the indexed status image
     */
    public Image createAccountStatusImage(ProtocolProviderService pps)
    {  
        Image statusImage;
        
        OperationSetPresence presence
            = this.mainFrame.getProtocolPresenceOpSet(pps);
        
        if(presence != null)
        {
            
            statusImage = ImageLoader.getBytesInImage(
                presence.getPresenceStatus().getStatusIcon()); 
        }
        else if (pps.isRegistered())
        {
            statusImage
                = ImageLoader.getBytesInImage(pps.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16));
        }
        else {
            statusImage
                =  LightGrayFilter.createDisabledImage(
                    ImageLoader.getBytesInImage(pps.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
        }
        
        int index = mainFrame.getProviderIndex(pps);

        Image img = null;
        if(index > 0) {
            BufferedImage buffImage = new BufferedImage(
                    22, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D)buffImage.getGraphics();
            AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(statusImage, 0, 0, null);
            g.setComposite(ac);
            g.drawString(new Integer(index+1).toString(), 14, 8);

            img = buffImage;
        }
        else {
            img = statusImage;
        }
        return img;
    }
    
    /**
     * Obtains the status icon for the given protocol contact and
     * adds to it the account index information.
     * @param protoContact the proto contact for which to create the image
     * @return the indexed status image
     */
    public Image createContactStatusImage(Contact protoContact)
    {
        Image statusImage = ImageLoader.getBytesInImage(
                protoContact.getPresenceStatus().getStatusIcon());

        int index = mainFrame.getProviderIndex(
            protoContact.getProtocolProvider());

        Image img = null;
        if(index > 0) {
            BufferedImage buffImage = new BufferedImage(
                    22, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D)buffImage.getGraphics();
            AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(statusImage, 0, 0, null);
            g.setComposite(ac);
            g.drawString(new Integer(index+1).toString(), 14, 8);

            img = buffImage;
        }
        else {
            img = statusImage;
        }
        return img;
    }
}
