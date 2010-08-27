/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    /**
     * The <tt>Logger</tt> used by the <tt>FileMenu</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger = Logger.getLogger(FileMenu.class);

    private final MainFrame parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(MainFrame parentWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.FILE"));

        ResourceManagementService resources = GuiActivator.getResources();
        JMenuItem newAccountMenuItem = new JMenuItem(
            resources.getI18NString("service.gui.NEW_ACCOUNT"),
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.ADD_ACCOUNT_MENU_ICON)));
        JMenuItem addContactItem = new JMenuItem(
            resources.getI18NString("service.gui.ADD_CONTACT") + "...",
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.ADD_CONTACT_16x16_ICON)));
        JMenuItem createGroupItem = new JMenuItem(
            resources.getI18NString("service.gui.CREATE_GROUP"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.GROUPS_16x16_ICON)));
        JMenuItem myChatRoomsItem = new JMenuItem(
            resources.getI18NString("service.gui.MY_CHAT_ROOMS"),
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.CHAT_ROOM_16x16_ICON)));

        this.setOpaque(false);

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

        newAccountMenuItem.setName("newAccount");
        addContactItem.setName("addContact");
        createGroupItem.setName("createGroup");
        myChatRoomsItem.setName("myChatRooms");

        newAccountMenuItem.addActionListener(this);
        addContactItem.addActionListener(this);
        createGroupItem.addActionListener(this);
        myChatRoomsItem.addActionListener(this);

        this.setMnemonic(resources
            .getI18nMnemonic("service.gui.FILE"));
        newAccountMenuItem.setMnemonic(resources
            .getI18nMnemonic("service.gui.NEW_ACCOUNT"));
        addContactItem.setMnemonic(resources
            .getI18nMnemonic("service.gui.ADD_CONTACT"));
        createGroupItem.setMnemonic(resources
            .getI18nMnemonic("service.gui.CREATE_GROUP"));
        myChatRoomsItem.setMnemonic(resources
            .getI18nMnemonic("service.gui.MY_CHAT_ROOMS"));
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals("newAccount"))
        {
            NewAccountDialog.showNewAccountDialog();
        }
        else if (itemName.equals("addContact"))
        {
            AddContactDialog dialog = new AddContactDialog(parentWindow);

            dialog.setVisible(true);
        }
        else if (itemName.equals("createGroup"))
        {
            CreateGroupDialog dialog = new CreateGroupDialog(parentWindow);

            dialog.setVisible(true);
        }
        else if (itemName.equals("close"))
        {
            closeActionPerformed();
        }
        else if (itemName.equals("myChatRooms"))
        {
            ChatRoomTableDialog.showChatRoomTableDialog();
        }
    }

    void closeActionPerformed()
    {
        GuiActivator.getUIService().beginShutdown();
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
            Class<?> clazz = Class.forName(
                "net.java.sip.communicator.impl.gui.main.menus.MacOSX"
                + menuItemText + "Registration");
            Method method = clazz.getMethod("run", new Class[]
            { Object.class });
            Object result = method.invoke(null, new Object[]
            { userData });

            if (result instanceof Boolean)
                return (Boolean) result;
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
        JMenuItem closeMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString("service.gui.QUIT"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.QUIT_16x16_ICON)));

        this.addSeparator();
        this.add(closeMenuItem);
        closeMenuItem.setName("close");
        closeMenuItem.addActionListener(this);
        closeMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.QUIT"));
    }
}
