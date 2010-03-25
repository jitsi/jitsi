/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>TrayMenu</tt> is the menu that appears when the user right-click
 * on the Systray icon.
 *
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public final class TrayMenuFactory
{
    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param evt the event containing the menu item name
     */
    private static void actionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();
        String itemName;
        if (source instanceof JMenuItem)
        {
            JMenuItem menuItem = (JMenuItem) source;
            itemName = menuItem.getName();
        }
        else
        {
            MenuItem menuItem = (MenuItem) source;
            itemName = menuItem.getName();
        }

        if (itemName.equals("settings"))
        {
            OsDependentActivator.getUIService()
                .setConfigurationWindowVisible(true);
        }
        else if (itemName.equals("service.gui.CLOSE"))
        {
            OsDependentActivator.getShutdownService().beginShutdown();
        }
        else if (itemName.equals("addContact"))
        {
            ExportedWindow dialog =
                OsDependentActivator.getUIService().getExportedWindow(
                    ExportedWindow.ADD_CONTACT_WINDOW);

            if (dialog != null)
                dialog.setVisible(true);
            else
                OsDependentActivator.getUIService().getPopupDialog()
                    .showMessagePopupDialog(Resources.getString(
                        "impl.systray.FAILED_TO_OPEN_ADD_CONTACT_DIALOG"));
        }
        else if (itemName.equals("service.gui.SHOW"))
        {
            OsDependentActivator.getUIService().setVisible(true);

            changeTrayMenuItem(source, "service.gui.HIDE",
                "service.gui.HIDE", "service.gui.icons.SEARCH_ICON_16x16");
        }
        else if (itemName.equals("service.gui.HIDE"))
        {
            OsDependentActivator.getUIService().setVisible(false);

            changeTrayMenuItem(source, "service.gui.SHOW",
                "service.gui.SHOW", "service.gui.icons.SEARCH_ICON_16x16");
        }
    }

    /**
     * Adds the given <tt>trayMenuItem</tt> to the given <tt>trayMenu</tt>.
     * @param trayMenu the tray menu to which to add the item
     * @param trayMenuItem the item to add
     */
    private static void add(Object trayMenu, Object trayMenuItem)
    {
        if (trayMenu instanceof JPopupMenu)
            ((JPopupMenu) trayMenu).add((JMenuItem) trayMenuItem);
        else
            ((PopupMenu) trayMenu).add((MenuItem) trayMenuItem);
    }

    /**
     * Adds a <tt>PopupMenuListener</tt> to the given <tt>trayMenu</tt>.
     * @param trayMenu the tray menu to which to add a popup listener
     * @param listener the listener to add
     */
    public static void addPopupMenuListener(Object trayMenu,
        PopupMenuListener listener)
    {
        if (trayMenu instanceof JPopupMenu)
            ((JPopupMenu) trayMenu).addPopupMenuListener(listener);
    }

    /**
     * Adds a separator to the given <tt>trayMenu</tt>.
     * @param trayMenu the tray menu to which to add a separator
     */
    private static void addSeparator(Object trayMenu)
    {
        if (trayMenu instanceof JPopupMenu)
            ((JPopupMenu) trayMenu).addSeparator();
        else
            ((PopupMenu) trayMenu).addSeparator();
    }

    /**
     * Creates a tray menu for the given system tray.
     * @param tray the system tray for which we're creating a menu
     * @param swing indicates if we should create a Swing or an AWT menu
     * @return a tray menu for the given system tray
     */
    public static Object createTrayMenu(SystrayServiceJdicImpl tray,
                                        boolean swing)
    {
        // Enable swing for java 1.6 except for the mac version
        if (!swing && !OSUtils.IS_MAC)
            swing = true;

        Object trayMenu = swing ? new JPopupMenu() : new PopupMenu();
        ActionListener listener = new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                TrayMenuFactory.actionPerformed(event);
            }
        };

        add(trayMenu, createTrayMenuItem("settings", "service.gui.SETTINGS",
            "service.systray.CONFIGURE_ICON", listener, swing));
        add(trayMenu, createTrayMenuItem("addContact",
            "service.gui.ADD_CONTACT",
            "service.gui.icons.ADD_CONTACT_16x16_ICON", listener, swing));
        addSeparator(trayMenu);
        add(trayMenu, new StatusSubMenu(tray, swing).getMenu());
        addSeparator(trayMenu);

        String showHideName;
        String showHideTextId;
        String showHideIconId;

        if (OsDependentActivator.getUIService().isVisible())
        {
            showHideName = "service.gui.HIDE";
            showHideTextId = "service.gui.HIDE";
            showHideIconId = "service.gui.icons.SEARCH_ICON_16x16";
        }
        else
            showHideName = "service.gui.SHOW";
            showHideTextId = "service.gui.SHOW";
            showHideIconId = "service.gui.icons.SEARCH_ICON_16x16";

        final Object showHideMenuItem = createTrayMenuItem( showHideName,
                                                            showHideTextId,
                                                            showHideIconId,
                                                            listener,
                                                            swing);

        add(trayMenu, showHideMenuItem);

        add(trayMenu, createTrayMenuItem("service.gui.CLOSE",
            "service.gui.CLOSE", "service.systray.CLOSE_MENU_ICON", listener,
            swing));

        OsDependentActivator.getUIService().addWindowListener(
            new WindowAdapter()
            {
                /**
                 * Invoked when a window is activated.
                 */
                public void windowActivated(WindowEvent e)
                {
                    changeTrayMenuItem( showHideMenuItem,
                                        "service.gui.HIDE",
                                        "service.gui.HIDE",
                                        "service.gui.icons.SEARCH_ICON_16x16");
                }

                /**
                 * Invoked when a window is de-activated.
                 */
                public void windowDeactivated(WindowEvent e)
                {
                    changeTrayMenuItem( showHideMenuItem,
                                        "service.gui.SHOW",
                                        "service.gui.SHOW",
                                        "service.gui.icons.SEARCH_ICON_16x16");
                }
            });

        return trayMenu;
    }

    /**
     * Creates a tray menu with the given <tt>name</tt>, text given by
     * <tt>textID</tt> and icon given by <tt>iconID</tt>. The <tt>listener</tt>
     * is handling item events and the <tt>swing</tt> value indicates if we
     * should create a Swing menu item or and an AWT item.
     * @param name the name of the item
     * @param textID the identifier of the text in the localization resources
     * @param iconID the identifier of the icon in the image resources
     * @param listener the <tt>ActionListener</tt> handling action events
     * @param swing indicates if we should create a Swing menu item or an AWT
     * item
     * @return a reference to the newly created item
     */
    private static Object createTrayMenuItem(   String name,
                                                String textID,
                                                String iconID,
                                                ActionListener listener,
                                                boolean swing)
    {
        String text = Resources.getString(textID);
        Object trayMenuItem;
        if (swing)
        {
            JMenuItem menuItem = 
                new JMenuItem(text, Resources.getImage(iconID));
            menuItem.setName(name);
            menuItem.addActionListener(listener);
            trayMenuItem = menuItem;
        }
        else
        {
            MenuItem menuItem = new MenuItem(text);
            menuItem.setName(name);
            menuItem.addActionListener(listener);
            trayMenuItem = menuItem;
        }
        return trayMenuItem;
    }

    /**
     * Changes the tray menu item properties, like name, text and icon.
     * @param trayItem the tray menu item to change
     * @param name the new name of the item
     * @param textID the new text identifier
     * @param iconID the new icon string identifier
     */
    private static void changeTrayMenuItem( Object trayItem,
                                            String name,
                                            String textID,
                                            String iconID)
    {
        String text = Resources.getString(textID);

        if (trayItem instanceof JMenuItem)
        {
            JMenuItem jmenuItem = (JMenuItem) trayItem;
            jmenuItem.setName(name);
            jmenuItem.setText(text);
            jmenuItem.setIcon(Resources.getImage(iconID));
        }
        else if (trayItem instanceof MenuItem)
        {
            MenuItem menuItem = (MenuItem) trayItem;
            menuItem.setName(name);
            menuItem.setLabel(text);
        }
    }

    /**
     * Returns <tt>true</tt> if the given <tt>trayMenu</tt> is visible,
     * otherwise returns <tt>false</tt>.
     * @param trayMenu the <tt>TrayMenu</tt> to check
     * @return <tt>true</tt> if the given <tt>trayMenu</tt> is visible,
     * otherwise returns <tt>false</tt>
     */
    public static boolean isVisible(Object trayMenu)
    {
        if (trayMenu instanceof JPopupMenu)
            return ((JPopupMenu) trayMenu).isVisible();
        return false;
    }
}
