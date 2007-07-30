/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.createforms.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;
/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 *
 * @author Yana Stamcheva
 */
public class FileMenu
    extends SIPCommMenu
    implements ActionListener
{

    private Logger logger = Logger.getLogger(FileMenu.class.getName());

    private I18NString newAccountString = Messages.getI18NString("newAccount");
    
    private I18NString addContactString = Messages.getI18NString("addContact");
    
    private I18NString closeString = Messages.getI18NString("quit");
    
    private I18NString createGroupString = Messages.getI18NString("createGroup");
    
    private I18NString fileString = Messages.getI18NString("file");
    
    private I18NString createChatRoomString
        = Messages.getI18NString("createChatRoom");

    private I18NString searchForChatRoomsString
        = Messages.getI18NString("searchForChatRooms");


    private JMenuItem newAccountMenuItem
        = new JMenuItem(newAccountString.getText());

    private JMenuItem addContactItem
        = new JMenuItem(addContactString.getText(), 
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.ADD_CONTACT_16x16_ICON)));

    private JMenuItem createGroupItem
        = new JMenuItem(createGroupString.getText(),
            new ImageIcon(ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));

    private JMenuItem closeMenuItem
        = new JMenuItem(closeString.getText());

    private JMenuItem createChatRoomItem = new JMenuItem(
        createChatRoomString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CHAT_ROOM_16x16_ICON)));

    private JMenuItem searchForChatRoomsItem = new JMenuItem(
        searchForChatRoomsString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.SEARCH_ICON_16x16)));

    private MainFrame parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(MainFrame parentWindow) {

        super(Messages.getI18NString("file").getText());

        this.parentWindow = parentWindow;

        this.add(newAccountMenuItem);

        this.addSeparator();

        this.add(addContactItem);
        this.add(createGroupItem);

        this.addSeparator();

        this.add(createChatRoomItem);
        this.add(searchForChatRoomsItem);
        
        this.addSeparator();
        
        this.add(closeMenuItem);

        //this.addContactItem.setIcon(new ImageIcon(ImageLoader
        //        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.newAccountMenuItem.setName("newAccount");
        this.closeMenuItem.setName("close");
        this.addContactItem.setName("addContact");
        this.createGroupItem.setName("createGroup");
        this.createChatRoomItem.setName("createChatRoom");
        this.searchForChatRoomsItem.setName("searchForChatRooms");
        
        this.newAccountMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);
        this.addContactItem.addActionListener(this);
        this.createGroupItem.addActionListener(this);
        this.createChatRoomItem.addActionListener(this);
        this.searchForChatRoomsItem.addActionListener(this);

        this.setMnemonic(fileString.getMnemonic());
        this.closeMenuItem.setMnemonic(closeString.getMnemonic());
        this.newAccountMenuItem.setMnemonic(newAccountString.getMnemonic());
        this.addContactItem.setMnemonic(addContactString.getMnemonic());
        this.createGroupItem.setMnemonic(createGroupString.getMnemonic());
        this.createChatRoomItem
            .setMnemonic(createChatRoomString.getMnemonic());
        this.searchForChatRoomsItem
            .setMnemonic(searchForChatRoomsString.getMnemonic());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals("newAccount")) {
            AccountRegWizardContainerImpl wizard
                = (AccountRegWizardContainerImpl)GuiActivator.getUIService()
                    .getAccountRegWizardContainer();

            wizard.setTitle(
                Messages.getI18NString("accountRegistrationWizard").getText());

            wizard.newAccount();

            wizard.showDialog(false);
        }
        else if (itemName.equals("addContact")) {
            AddContactWizard wizard = new AddContactWizard(parentWindow);

            wizard.showDialog(false);
        }
        else if (itemName.equals("createGroup")) {
            CreateGroupDialog dialog = new CreateGroupDialog(parentWindow);

            dialog.setVisible(true);
        }
        else if (itemName.equals("close")) {
            try {
                GuiActivator.bundleContext.getBundle(0).stop();
            } catch (BundleException ex) {
                logger.error("Failed to gently shutdown Felix", ex);
                System.exit(0);
            }
            parentWindow.dispose();
            //stopping a bundle doesn't leave the time to the felix thread to
            //properly end all bundles and call their Activator.stop() methods.
            //if this causes problems don't uncomment the following line but
            //try and see why felix isn't exiting (suggesting: is it running
            //in embedded mode?)
            //System.exit(0);
        }
        else if (itemName.equals("createChatRoom"))
        {
            CreateChatRoomWizard createChatRoomWizard
                = new CreateChatRoomWizard(parentWindow);
            
            createChatRoomWizard.showDialog(false);
        }
        else if (itemName.equals("searchForChatRooms"))
        {
        
        }
    }
}
