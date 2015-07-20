/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

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

    private final Frame parentWindow;

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
     * Indicates if this menu is shown for the chat window or the contact list
     * window.
     */
    private boolean isChatMenu;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(Frame parentWindow)
    {
        this(parentWindow, false);
    }

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     *
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     * @param isChatMenu indicates if this menu would be shown for a chat
     * window
     */
    public FileMenu(Frame parentWindow, boolean isChatMenu)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.FILE"));

        this.isChatMenu = isChatMenu;

        ResourceManagementService resources = GuiActivator.getResources();
        createGroupItem = new JMenuItem(
            resources.getI18NString("service.gui.CREATE_GROUP"));
        myChatRoomsItem = new JMenuItem(
            resources.getI18NString("service.gui.MY_CHAT_ROOMS"));

        this.parentWindow = parentWindow;

        // whether the last item added was a separator
        boolean endsWithSeparator = false;

        if (ConfigurationUtils.isShowAccountConfig())
        {
            newAccountMenuItem = new JMenuItem(
                resources.getI18NString("service.gui.NEW_ACCOUNT"));

            this.add(newAccountMenuItem);

            newAccountMenuItem.setName("newAccount");

            newAccountMenuItem.addActionListener(this);

            newAccountMenuItem.setMnemonic(resources
                .getI18nMnemonic("service.gui.NEW_ACCOUNT"));

            // add separator only if there are other items enabled
            if(!ConfigurationUtils.isAddContactDisabled()
               || !ConfigurationUtils.isCreateGroupDisabled()
               || !ConfigurationUtils.isGoToChatroomDisabled())
            {
                this.addSeparator();
                endsWithSeparator = true;
            }
        }

        if (!isChatMenu && !ConfigurationUtils.isAddContactDisabled())
        {
            addContactItem = new JMenuItem(
                resources.getI18NString("service.gui.ADD_CONTACT") + "...");

            this.add(addContactItem);

            addContactItem.setName("addContact");

            addContactItem.addActionListener(this);

            addContactItem.setMnemonic(resources
                .getI18nMnemonic("service.gui.ADD_CONTACT"));

            // if next item is disabled add separator here
            // only if there is something next
            if(ConfigurationUtils.isCreateGroupDisabled()
               && !ConfigurationUtils.isGoToChatroomDisabled())
            {
                this.addSeparator();
                endsWithSeparator = true;
            }
            else
                endsWithSeparator = false;
        }

        if (!isChatMenu && !ConfigurationUtils.isCreateGroupDisabled())
        {
            this.add(createGroupItem);

            // add separator if there is something next
            if(!ConfigurationUtils.isGoToChatroomDisabled())
            {
                this.addSeparator();
                endsWithSeparator = true;
            }
        }

        if (!ConfigurationUtils.isGoToChatroomDisabled()
            && !GuiActivator.getConfigurationService()
                    .getBoolean(MUCService.DISABLED_PROPERTY, false))
        {
            this.add(myChatRoomsItem);
            endsWithSeparator = false;
        }

        registerCloseMenuItem(!endsWithSeparator);

        // All items are now instantiated and could safely load the skin.
        loadSkin();

        //this.addContactItem.setIcon(new ImageIcon(ImageLoader
        //        .getImage(ImageLoader.ADD_CONTACT_16x16_ICON)));

        createGroupItem.setName("createGroup");
        myChatRoomsItem.setName("myChatRooms");

        createGroupItem.addActionListener(this);
        myChatRoomsItem.addActionListener(this);

        this.setMnemonic(resources
            .getI18nMnemonic("service.gui.FILE"));

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
        if (newAccountMenuItem != null)
            newAccountMenuItem.setIcon(
                new ImageIcon(ImageLoader.getImage(
                    ImageLoader.ADD_ACCOUNT_MENU_ICON)));

        if (addContactItem != null)
            addContactItem.setIcon(
                new ImageIcon(ImageLoader.getImage(
                    ImageLoader.ADD_CONTACT_16x16_ICON)));

        createGroupItem.setIcon(
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.GROUPS_16x16_ICON)));
        myChatRoomsItem.setIcon(
            new ImageIcon(ImageLoader.getImage(
                ImageLoader.CHAT_ROOM_MENU_ICON)));

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
     * @param addSeparator whether we should add separator before the menu item.
     */
    private void registerCloseMenuItem(boolean addSeparator)
    {
        UIService uiService = GuiActivator.getUIService();
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerCloseMenuItemMacOSX())
        {
            registerCloseMenuItemNonMacOSX(addSeparator);
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
     * @param addSeparator whether we should add separator before the menu item.
     */
    private void registerCloseMenuItemNonMacOSX(boolean addSeparator)
    {
        closeMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString("service.gui.QUIT"));

        if(addSeparator)
            this.addSeparator();

        if (!isChatMenu)
        {
            this.add(closeMenuItem);
            closeMenuItem.setName("close");
            closeMenuItem.addActionListener(this);
            closeMenuItem.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.QUIT"));
        }
    }
}
