/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 * 
 * @author Yana Stamcheva
 */
public class FileMenu
    extends JMenu 
    implements ActionListener
{

    private Logger logger = Logger.getLogger(FileMenu.class.getName());
    
    private JMenuItem newAccountMenuItem
        = new JMenuItem(Messages.getString("newAccount"));
    
    private JMenuItem addContactItem
        = new JMenuItem(Messages.getString("addContact"), new ImageIcon(
                ImageLoader.getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));
    
    private JMenuItem createGroupItem
        = new JMenuItem(Messages.getString("createGroup"), new ImageIcon(
                ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));

    private JMenuItem closeMenuItem
        = new JMenuItem(Messages.getString("close"));

    private MainFrame parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(MainFrame parentWindow) {

        super(Messages.getString("file"));
        
        this.parentWindow = parentWindow;

        this.add(newAccountMenuItem);
        
        this.addSeparator();
        
        this.add(addContactItem);
        this.add(createGroupItem);
        
        this.addSeparator();
        
        this.add(closeMenuItem);
        
        //this.addContactItem.setIcon(new ImageIcon(ImageLoader
        //        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));
        
        this.newAccountMenuItem.setName("newAccount");
        this.closeMenuItem.setName("close");
        this.addContactItem.setName("addContact");
        this.createGroupItem.setName("createGroup");
        
        this.newAccountMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);
        this.addContactItem.addActionListener(this);
        this.createGroupItem.addActionListener(this);
        
        this.setMnemonic(Messages.getString("file").charAt(0));
        this.closeMenuItem.setMnemonic(
                Messages.getString("mnemonic.close").charAt(0));
        this.newAccountMenuItem.setMnemonic(
                Messages.getString("mnemonic.newAccount").charAt(0));
        this.addContactItem.setMnemonic(
                Messages.getString("mnemonic.addContact").charAt(0));
        this.createGroupItem.setMnemonic(
                Messages.getString("mnemonic.createGroup").charAt(0));
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
                Messages.getString("accountRegistrationWizard"));
    
            wizard.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - 250,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - 100
            );
            
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
                logger.error("Failed to gently shutdown Oscar", ex);
            }
            parentWindow.dispose();
        }
    }
}
