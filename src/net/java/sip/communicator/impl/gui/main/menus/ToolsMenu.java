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
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

import com.apple.eawt.*;

/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 * 
 * @author Yana Stamcheva
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

        super(Messages.getI18NString("tools").getText());

        this.setForeground(
            new Color(GuiActivator.getResources().
                getColor("service.gui.MAIN_MENU_FOREGROUND")));
        this.setMnemonic(Messages.getI18NString("tools").getMnemonic());
        this.setOpaque(false);

        registerConfigMenuItem();

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
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);;

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
    }

    private void configActionPerformed()
    {
        ExportedWindow configDialog =
            GuiActivator.getUIService().getExportedWindow(
                ExportedWindow.CONFIGURATION_WINDOW);

        configDialog.setVisible(true);
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

    private void registerConfigMenuItem()
    {
        UIService uiService = GuiActivator.getUIService();
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerConfigMenuItemMacOSX())
        {
            registerConfigMenuItemNonMacOSX();
        }
    }

    private boolean registerConfigMenuItemMacOSX()
    {
        Application application = Application.getApplication();
        if (application != null)
        {
            application.addPreferencesMenuItem();
            if (application.isPreferencesMenuItemPresent())
            {
                application.setEnabledPreferencesMenu(true);
                application.addApplicationListener(new ApplicationAdapter()
                {
                    public void handlePreferences(ApplicationEvent event)
                    {
                        configActionPerformed();
                        event.setHandled(true);
                    }
                });
                return true;
            }
        }
        return false;
    }

    private void registerConfigMenuItemNonMacOSX()
    {
        I18NString settingsString = Messages.getI18NString("settings");
        JMenuItem configMenuItem = new JMenuItem(settingsString.getText());
        this.add(configMenuItem);
        configMenuItem.setMnemonic(settingsString.getMnemonic());
        configMenuItem.setName("config");
        configMenuItem.addActionListener(this);
    }
}
