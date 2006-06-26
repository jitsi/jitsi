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
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.AddContactDialog;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class GroupRightButtonMenu extends JPopupMenu
    implements ActionListener {

    private JMenu addContactMenu = new JMenu(Messages.getString("addContact"));
    
    private MetaContactGroup group;
    
    private MainFrame mainFrame;
    
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
    public void actionPerformed(ActionEvent e) {
        JMenuItem item = (JMenuItem)e.getSource();
        String itemText = item.getText();
        
        if(mainFrame.getProtocolProviderForAccount(itemText) != null) {
            ProtocolProviderService pps 
                = mainFrame.getProtocolProviderForAccount(itemText);
            
            AddContactDialog dialog = new AddContactDialog(mainFrame.getContactList(),
                    group, pps);
            
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
