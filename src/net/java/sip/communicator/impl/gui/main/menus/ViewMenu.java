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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

import javax.swing.*;
import java.awt.*;

/**
 * The <tt>ViewMenu</tt> is a menu in the main application menu bar.
 *
 * @author Yana Stamcheva
 */
public class ViewMenu
    extends SIPCommMenu
    implements PluginComponentListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(ViewMenu.class);

    /**
     * Creates an instance of <tt>ViewMenu</tt>.
     * @param mainFrame The parent <tt>MainFrame</tt> window.
     */
    public ViewMenu(MainFrame mainFrame)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.VIEW"));

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.VIEW"));

        this.initPluginComponents();
    }

    /**
     * Searches for already registered plugins.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_VIEW_MENU.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponentFactory.class.getName(),
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
                final PluginComponentFactory f = (PluginComponentFactory) GuiActivator
                    .bundleContext.getService(serRef);

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        add((Component) f.getPluginComponentInstance(
                            ViewMenu.this).getComponent());
                    }
                });
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Adds the plugin component contained in the event to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory c = event.getPluginComponentFactory();

        if(c.getContainer().equals(Container.CONTAINER_VIEW_MENU))
        {
            this.add((Component)c.getPluginComponentInstance(ViewMenu.this)
                            .getComponent());

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
        PluginComponentFactory c = event.getPluginComponentFactory();

        if(c.getContainer().equals(Container.CONTAINER_VIEW_MENU))
        {
            this.remove((Component) c.getPluginComponentInstance(ViewMenu.this)
                    .getComponent());
        }
    }
}
