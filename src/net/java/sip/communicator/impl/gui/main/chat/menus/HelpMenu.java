/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;
/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class HelpMenu
    extends SIPCommMenu
    implements ActionListener,
               PluginComponentListener
{
    private final Logger logger = Logger.getLogger(HelpMenu.class.getName());

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * @param chatWindow The parent <tt>MainFrame</tt>.
     */
    public HelpMenu(ChatWindow chatWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.HELP"));

        this.setOpaque(false);

        this.setForeground(new Color(
            GuiActivator.getResources()
                .getColor("service.gui.CHAT_MENU_FOREGROUND")));

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.HELP"));

        this.initPluginComponents();
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        GuiActivator.getUIService().removePluginComponentListener(this);

        /*
         * Let go of all Components contributed by PluginComponents because the
         * latter will still live in the contribution store.
         */
        removeAll();
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
            + "="+Container.CONTAINER_CHAT_HELP_MENU.getID()+")";

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
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     */
    public void actionPerformed(ActionEvent e)
    {
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_HELP_MENU))
        {
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_HELP_MENU))
        {
            this.remove((Component) c.getComponent());
        }
    }

}
