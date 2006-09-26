/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The GroupRightButtonMenu is the menu, opened when user clicks with the
 * right mouse button on a group in the contact list. Through this menu the
 * user could add a contact to a group.
 * 
 * @author Yana Stamcheva
 */
public class CommonRightButtonMenu extends JPopupMenu
    implements ActionListener {

    private JMenuItem addContactItem
        = new JMenuItem(Messages.getString("addContact"));
    
    private JMenuItem createGroupItem
        = new JMenuItem(Messages.getString("createGroup"));
    
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of GroupRightButtonMenu.
     * 
     * @param mainFrame The parent <tt>MainFrame</tt> window. 
     * @param group The <tt>MetaContactGroup</tt> for which the menu is opened.
     */
    public CommonRightButtonMenu(MainFrame mainFrame) {
    
        this.mainFrame = mainFrame;
        
        this.addContactItem.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.add(addContactItem);
        this.add(createGroupItem);
        
        this.addContactItem.setName("addContact");
        this.createGroupItem.setName("createGroup");
        
        this.addContactItem.addActionListener(this);
        this.createGroupItem.addActionListener(this);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. The choosen menu item should correspond
     * to an account, where the new contact will be added. We obtain here the
     * protocol provider corresponding to the choosen account and show the
     * dialog, where the user could add the contact.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem item = (JMenuItem)e.getSource();
        String itemName = item.getName();
        
        if(itemName.equals("createGroup")) {
            CreateGroupDialog dialog = new CreateGroupDialog(mainFrame);
            
            dialog.setVisible(true);
        }
        else if(itemName.equals("addContact")) {
            AddContactWizard wizard = new AddContactWizard(mainFrame);
            
            wizard.showModalDialog();
        }
    }
}
