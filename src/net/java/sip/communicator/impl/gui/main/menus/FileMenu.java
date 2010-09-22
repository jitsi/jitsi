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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class FileMenu
    extends SIPCommMenu
    implements  ActionListener,
                Skinnable
{
    /**
     * The <tt>Logger</tt> used by the <tt>FileMenu</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger = Logger.getLogger(FileMenu.class);

    private final MainFrame parentWindow;

    /**
     * Add new account menu item.
     */
    private JMenuItem newAccountMenuItem;

    /**
     * Add new contact menu item.
     */
    private JMenuItem addContactItem;

    /**
     * Create group menu item.
     */
    private JMenuItem createGroupItem;

    /**
     * Chat rooms menu item.
     */
    private JMenuItem myChatRoomsItem;

    /**
     * Close menu item.
     */
    private JMenuItem closeMenuItem;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(MainFrame parentWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.FILE"));

        ResourceManagementService resources = GuiActivator.getResources();
        newAccountMenuItem = new JMenuItem(
            resources.getI18NString("service.gui.NEW_ACCOUNT"));
        addContactItem = new JMenuItem(
            resources.getI18NString("service.gui.ADD_CONTACT") + "...");
        createGroupItem = new JMenuItem(
            resources.getI18NString("service.gui.CREATE_GROUP"));
        myChatRoomsItem = new JMenuItem(
            resources.getI18NString("service.gui.MY_CHAT_ROOMS"));

        this.setOpaque(false);

        this.parentWindow = parentWindow;

        this.add(newAccountMenuItem);

        this.addSeparator();

        this.add(addContactItem);
        this.add(createGroupItem);

        this.addSeparator();

        this.add(myChatRoomsItem);

        registerCloseMenuItem();

        // All items are now instantiated and could safely load the skin.
        loadSkin();

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
     * Loads icons.
     */
    public void loadSkin()
    {
        newAccountMenuItem.setIcon(
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.ADD_ACCOUNT_MENU_ICON)));
        addContactItem.setIcon(
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.ADD_CONTACT_16x16_ICON)));
        createGroupItem.setIcon(
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.GROUPS_16x16_ICON)));
        myChatRoomsItem.setIcon(
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.CHAT_ROOM_16x16_ICON)));

        if(closeMenuItem != null)
        {
            closeMenuItem.setIcon(
                new ImageIcon(ImageLoader.getImage(
                    ImageLoader.QUIT_16x16_ICON)));
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
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

    /**
     * Indicates that the close menu has been selected.
     */
    void closeActionPerformed()
    {
        GuiActivator.getUIService().beginShutdown();
    }

    /**
     * Registers the close menu item.
     */
    private void registerCloseMenuItem()
    {
        UIService uiService = GuiActivator.getUIService();
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerCloseMenuItemMacOSX())
        {
            registerCloseMenuItemNonMacOSX();
        }
    }

    /**
     * Registers the close menu item for the MacOSX platform.
     * @return <tt>true</tt> if the operation succeeded, <tt>false</tt> -
     * otherwise
     */
    private boolean registerCloseMenuItemMacOSX()
    {
        return registerMenuItemMacOSX("Quit", this);
    }

    /**
     * Registers the close menu item for the MacOSX platform.
     * @param menuItemText the name of the item
     * @param userData the user data
     * @return <tt>true</tt> if the operation succeeded, <tt>false</tt> -
     * otherwise
     */
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

    /**
     * Registers the close menu item for all NON-MacOSX platforms.
     */
    private void registerCloseMenuItemNonMacOSX()
    {
        closeMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString("service.gui.QUIT"));

        this.addSeparator();
        this.add(closeMenuItem);
        closeMenuItem.setName("close");
        closeMenuItem.addActionListener(this);
        closeMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.QUIT"));
    }
}
