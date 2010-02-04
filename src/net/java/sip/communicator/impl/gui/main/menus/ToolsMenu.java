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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ToolsMenu
    extends SIPCommMenu
    implements  ActionListener,
                PluginComponentListener
{
    private final Logger logger = Logger.getLogger(ToolsMenu.class);

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public ToolsMenu(MainFrame parentWindow) {

        super(GuiActivator.getResources().getI18NString("service.gui.TOOLS"));

        this.setForeground(
            new Color(GuiActivator.getResources().
                getColor("service.gui.MAIN_MENU_FOREGROUND")));
        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.TOOLS"));

        this.setOpaque(false);

        this.registerMenuItems();

        this.initPluginComponents();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_TOOLS_MENU.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRef);

                this.add((Component)component.getComponent());
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equalsIgnoreCase("config"))
        {
            configActionPerformed();
        }
        else if (itemName.equals("conference"))
        {
            ConferenceInviteDialog confInviteDialog
                = new ConferenceInviteDialog();

            confInviteDialog.setVisible(true);
        }
        else if (itemName.equals("showHideOffline"))
        {
            boolean isShowOffline = ConfigurationManager.isShowOffline();

            TreeContactList.presenceFilter.setShowOffline(!isShowOffline);

            GuiActivator.getContactList()
                .applyFilter(TreeContactList.presenceFilter);

            ConfigurationManager.setShowOffline(!isShowOffline);

            String itemTextKey = !isShowOffline
                    ? "service.gui.HIDE_OFFLINE_CONTACTS"
                    : "service.gui.SHOW_OFFLINE_CONTACTS";

            menuItem.setText(
                GuiActivator.getResources().getI18NString(itemTextKey));
        }
        else if (itemName.equals("sound"))
        {
            boolean isMute = GuiActivator.getAudioNotifier().isMute();

            GuiActivator.getAudioNotifier().setMute(!isMute);

            String itemTextKey = !isMute
                    ? "service.gui.SOUND_ON"
                    : "service.gui.SOUND_OFF";

            menuItem.setText(
                GuiActivator.getResources().getI18NString(itemTextKey));
        }
    }

    /**
     * Shows the configuration window.
     */
    void configActionPerformed()
    {
        GuiActivator.getUIService().setConfigurationWindowVisible(true);
    }

    /**
     * Adds the plugin component contained in the event to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Indicates that a plugin component has been removed. Removes it from this
     * container if it is contained in it.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            this.remove((Component) c.getComponent());
        }
    }

    /**
     * Registers all menu items.
     */
    private void registerMenuItems()
    {
        UIService uiService = GuiActivator.getUIService();
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerConfigMenuItemMacOSX())
        {
            registerConfigMenuItemNonMacOSX();
        }

        // Marks this feature as an ongoing work until its completed and fully
        // tested.
        JMenuItem conferenceMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(
                "service.gui.CREATE_CONFERENCE_CALL"),
            GuiActivator.getResources().getImage(
                "service.gui.icons.CHAT_ROOM_16x16_ICON"));

        conferenceMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CREATE_CONFERENCE_CALL"));
        conferenceMenuItem.setName("conference");
        conferenceMenuItem.addActionListener(this);
        this.add(conferenceMenuItem);

        // Show/hide offline contacts menu item.
        String offlineTextKey = ConfigurationManager.isShowOffline()
                            ? "service.gui.HIDE_OFFLINE_CONTACTS"
                            : "service.gui.SHOW_OFFLINE_CONTACTS";

        JMenuItem hideOfflineMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(offlineTextKey),
            GuiActivator.getResources().getImage(
                "service.gui.icons.SHOW_HIDE_OFFLINE_ICON"));

        hideOfflineMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic(offlineTextKey));
        hideOfflineMenuItem.setName("showHideOffline");
        hideOfflineMenuItem.addActionListener(this);
        this.add(hideOfflineMenuItem);

        // Sound on/off menu item.
        String soundTextKey = GuiActivator.getAudioNotifier().isMute()
                            ? "service.gui.SOUND_ON"
                            : "service.gui.SOUND_OFF";

        JMenuItem soundMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString(soundTextKey),
            GuiActivator.getResources().getImage(
                "service.gui.icons.SOUND_MENU_ICON"));

        soundMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic(soundTextKey));
        soundMenuItem.setName("sound");
        soundMenuItem.addActionListener(this);
        this.add(soundMenuItem);
    }

    /**
     * Registers the preferences item in the MacOS X menu.
     * @return <tt>true</tt> if the operation succeeds, otherwise - returns
     * <tt>false</tt>
     */
    private boolean registerConfigMenuItemMacOSX()
    {
        return FileMenu.registerMenuItemMacOSX("Preferences", this);
    }

    /**
     * Registers the settings item in the MacOS X menu.
     */
    private void registerConfigMenuItemNonMacOSX()
    {
        JMenuItem configMenuItem = new JMenuItem(
            GuiActivator.getResources().getI18NString("service.gui.SETTINGS"));

        this.add(configMenuItem);
        configMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.SETTINGS"));
        configMenuItem.setName("config");
        configMenuItem.addActionListener(this);
    }
}
