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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The GroupRightButtonMenu is the menu, opened when user clicks with the
 * right mouse button on a group in the contact list. Through this menu the
 * user could add a contact to a group.
 * 
 * @author Yana Stamcheva
 */
public class GroupRightButtonMenu extends JPopupMenu
    implements ActionListener {

    private JMenu addContactMenu = new JMenu(Messages.getString("addContact"));
    
    private JMenuItem removeGroupItem = new JMenuItem(Messages
            .getString("removeGroup"), new ImageIcon(ImageLoader
                    .getImage(ImageLoader.DELETE_16x16_ICON)));
    
    private JMenuItem renameGroupItem = new JMenuItem(Messages
            .getString("renameGroup"), new ImageIcon(ImageLoader
            .getImage(ImageLoader.RENAME_16x16_ICON)));

    
    private MetaContactGroup group;
    
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of GroupRightButtonMenu.
     * 
     * @param mainFrame The parent <tt>MainFrame</tt> window. 
     * @param group The <tt>MetaContactGroup</tt> for which the menu is opened.
     */
    public GroupRightButtonMenu(MainFrame mainFrame,
            MetaContactGroup group) {
        
        this.group = group;
        this.mainFrame = mainFrame;
        
        this.addContactMenu.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.add(addContactMenu);
        
        Iterator providers = mainFrame.getProtocolProviders();
        while(providers.hasNext()) {
            ProtocolProviderService pps 
                = (ProtocolProviderService)providers.next();
            
            String protocolName = pps.getProtocolName();
            
            JMenuItem menuItem = new JMenuItem(pps.getAccountID()
                    .getUserID(),
                    new ImageIcon(Constants.getProtocolIcon(protocolName)));
            
            menuItem.setName(protocolName);
            menuItem.addActionListener(this);
            
            this.addContactMenu.add(menuItem);
        }
        
        this.addSeparator();
        
        this.add(renameGroupItem);
        this.add(removeGroupItem);
        
        this.renameGroupItem.setName("renameGroup");
        this.removeGroupItem.setName("removeGroup");
        
        this.renameGroupItem.addActionListener(this);
        this.removeGroupItem.addActionListener(this);        
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. The choosen menu item should correspond
     * to an account, where the new contact will be added. We obtain here the
     * protocol provider corresponding to the choosen account and show the
     * dialog, where the user could add the contact.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem item = (JMenuItem)e.getSource();
        String itemText = item.getText();
        String itemName = item.getName();
        
        if(itemName.equals("removeGroup")) {
            
            if(group != null) 
                new RemoveGroupThread(group).start();                
        }
        else if(itemName.equals("renameGroup")) {
            
            RenameGroupDialog dialog = new RenameGroupDialog(
                    mainFrame, group);

            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2
                        - 200,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2
                        - 50
                    );

            dialog.setVisible(true);
            
            dialog.requestFocusInFiled();
        }
        else if(mainFrame.getProtocolProviderForAccount(itemText) != null) {
            ProtocolProviderService pps 
                = mainFrame.getProtocolProviderForAccount(itemText);
            
            AddContactDialog dialog = new AddContactDialog(
                    mainFrame, group, pps);
            
            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2 
                        - 250,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2 
                        - 100
                    );
            
            dialog.setVisible(true);
        }
    }
    
    /**
     * Removes a group from the contact list in a separate thread.
     */
    private class RemoveGroupThread extends Thread
    {
        private MetaContactGroup group;
        
        public RemoveGroupThread(MetaContactGroup group) {
            this.group = group;
        }
        public void run() {
            if(Constants.REMOVE_CONTACT_ASK) {
                String message = "<HTML>Are you sure you want to remove <B>"
                    + this.group.getGroupName()
                    + "</B><BR>from your contact list?</html>";

                MessageDialog dialog = new MessageDialog(mainFrame,
                        message, Messages.getString("remove"));

                int returnCode = dialog.showDialog();
                
                if (returnCode == MessageDialog.OK_RETURN_CODE) {
                    mainFrame.getContactList()
                                .removeMetaContactGroup(group);                    
                }
                else if (returnCode == MessageDialog.OK_DONT_ASK_CODE) {
                    mainFrame.getContactList()
                            .removeMetaContactGroup(group);
                    
                    Constants.REMOVE_CONTACT_ASK = false;
                }
            }
            else {
                mainFrame.getContactList().removeMetaContactGroup(group);
            }
        }
    }
}
