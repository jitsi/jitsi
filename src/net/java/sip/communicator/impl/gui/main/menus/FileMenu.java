/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class FileMenu
    extends SIPCommMenu
    implements ActionListener
{
    private static final Logger logger =
        Logger.getLogger(FileMenu.class.getName());

    private I18NString newAccountString = Messages.getI18NString("newAccount");

    private I18NString addContactString = Messages.getI18NString("addContact");

    private I18NString createGroupString = Messages.getI18NString("createGroup");

    private I18NString fileString = Messages.getI18NString("file");

    private I18NString myChatRoomsString
        = Messages.getI18NString("myChatRooms");

    private JMenuItem newAccountMenuItem
        = new JMenuItem(newAccountString.getText());

    private JMenuItem addContactItem
        = new JMenuItem(addContactString.getText(), 
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.ADD_CONTACT_16x16_ICON)));

    private JMenuItem createGroupItem
        = new JMenuItem(createGroupString.getText(),
            new ImageIcon(ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));

    private JMenuItem myChatRoomsItem = new JMenuItem(
        myChatRoomsString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CHAT_ROOM_16x16_ICON)));

    private MainFrame parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(MainFrame parentWindow)
    {
        super(Messages.getI18NString("file").getText());

        this.setOpaque(false);

        this.setForeground(
            new Color(GuiActivator.getResources().getColor("service.gui.MAIN_MENU_FOREGROUND")));

        this.parentWindow = parentWindow;

        this.add(newAccountMenuItem);

        this.addSeparator();

        this.add(addContactItem);
        this.add(createGroupItem);

        this.addSeparator();

        this.add(myChatRoomsItem);

        registerCloseMenuItem();

        //this.addContactItem.setIcon(new ImageIcon(ImageLoader
        //        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        this.newAccountMenuItem.setName("newAccount");
        this.addContactItem.setName("addContact");
        this.createGroupItem.setName("createGroup");
        this.myChatRoomsItem.setName("myChatRooms");

        this.newAccountMenuItem.addActionListener(this);
        this.addContactItem.addActionListener(this);
        this.createGroupItem.addActionListener(this);
        this.myChatRoomsItem.addActionListener(this);

        this.setMnemonic(fileString.getMnemonic());
        this.newAccountMenuItem.setMnemonic(newAccountString.getMnemonic());
        this.addContactItem.setMnemonic(addContactString.getMnemonic());
        this.createGroupItem.setMnemonic(createGroupString.getMnemonic());
        this.myChatRoomsItem.setMnemonic(myChatRoomsString.getMnemonic());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals("newAccount"))
        {
            NewAccountDialog dialog = new NewAccountDialog();

            dialog.setVisible(true);
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
            closeActionPerformed();
        }
        else if (itemName.equals("myChatRooms"))
        {
            ChatRoomListDialog chatRoomsDialog
                = new ChatRoomListDialog(parentWindow);

            chatRoomsDialog.setPreferredSize(new Dimension(500, 400));
            chatRoomsDialog.setVisible(true);
        }
    }

    void closeActionPerformed()
    {
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

    private void registerCloseMenuItem()
    {
        UIService uiService = GuiActivator.getUIService();
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerCloseMenuItemMacOSX())
        {
            registerCloseMenuItemNonMacOSX();
        }
    }

    private boolean registerCloseMenuItemMacOSX()
    {
        return registerMenuItemMacOSX("Quit", this);
    }

    static boolean registerMenuItemMacOSX(String menuItemText, Object userData)
    {
        Exception exception = null;
        try
        {
            Class<?> clazz =
                Class
                    .forName("net.java.sip.communicator.impl.gui.main.menus.MacOSX"
                        + menuItemText + "Registration");
            Method method = clazz.getMethod("run", new Class[]
            { Object.class });
            Object result = method.invoke(null, new Object[]
            { userData });

            if (result instanceof Boolean)
                return ((Boolean) result).booleanValue();
        }
        catch (ClassNotFoundException ex)
        {
            exception = ex;
        }
        catch (IllegalAccessException ex)
        {
            exception = ex;
        }
        catch (InvocationTargetException ex)
        {
            exception = ex;
        }
        catch (NoSuchMethodException ex)
        {
            exception = ex;
        }
        if (exception != null)
            logger.error("Failed to register Mac OS X-specific " + menuItemText
                + " handling.", exception);
        return false;
    }

    private void registerCloseMenuItemNonMacOSX()
    {
        I18NString closeString = Messages.getI18NString("quit");
        JMenuItem closeMenuItem = new JMenuItem(closeString.getText());
        this.addSeparator();
        this.add(closeMenuItem);
        closeMenuItem.setName("close");
        closeMenuItem.addActionListener(this);
        closeMenuItem.setMnemonic(closeString.getMnemonic());
    }
}
