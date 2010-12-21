/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 * 
 * @author Yana Stamcheva
 * @author Thomas Hofer
 */
public class HelpMenu 
    extends SIPCommMenu
    implements ActionListener,
               PluginComponentListener
{
    private static final Logger logger = Logger.getLogger(HelpMenu.class);

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * 
     * @param mainFrame the parent window
     */
    public HelpMenu(MainFrame mainFrame)
    {
        ResourceManagementService resources = GuiActivator.getResources();

        setMnemonic(resources.getI18nMnemonic("service.gui.HELP"));
        setText(resources.getI18NString("service.gui.HELP"));

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
            + "="+Container.CONTAINER_HELP_MENU.getID()+")";

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
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     */
    public void actionPerformed(ActionEvent e)
    {
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_HELP_MENU))
        {
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_HELP_MENU))
        {
            this.remove((Component) c.getComponent());
        }
    }
}
