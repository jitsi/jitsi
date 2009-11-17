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
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

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
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("config"))
        {
            configActionPerformed();
        }
        else if (itemText.equals("conference"))
        {
            ConferenceInviteDialog confInviteDialog
                = new ConferenceInviteDialog();

            confInviteDialog.setVisible(true);
        }
    }

    /**
     * Shows the configuration window.
     */
    void configActionPerformed()
    {
        GuiActivator.getUIService().setConfigurationWindowVisible(true);
    }

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

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_TOOLS_MENU))
        {
            this.remove((Component) c.getComponent());
        }
    }

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
                "service.gui.CREATE_CONFERENCE_CALL") + " (coming soon)");

        conferenceMenuItem.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.CREATE_CONFERENCE_CALL"));
        conferenceMenuItem.setName("conference");
        conferenceMenuItem.addActionListener(this);
        this.add(conferenceMenuItem);
    }

    private boolean registerConfigMenuItemMacOSX()
    {
        return FileMenu.registerMenuItemMacOSX("Preferences", this);
    }

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
