/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.AddContactDialog;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

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
                    .getAccountUserID(),
                    new ImageIcon(Constants.getProtocolIcon(protocolName)));
            
            menuItem.setName(protocolName);
            menuItem.addActionListener(this);
            
            this.addContactMenu.add(menuItem);
        }
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
        
        if(mainFrame.getProtocolProviderForAccount(itemText) != null) {
            ProtocolProviderService pps 
                = mainFrame.getProtocolProviderForAccount(itemText);
            
            AddContactDialog dialog = new AddContactDialog(
                    mainFrame.getContactList(), group, pps);
            
            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2 
                        - 250,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2 
                        - 100
                    );
            
            dialog.setVisible(true);
        }
    }
}
