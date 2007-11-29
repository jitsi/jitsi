/*
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;

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

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>HelpMenu</tt>.
     * 
     * @param mainFrame
     *                the parent window
     */
    public HelpMenu(MainFrame mainFrame)
    {

        super(Messages.getI18NString("help").getText());

        this.mainFrame = mainFrame;

        this.setMnemonic(Messages.getI18NString("help").getMnemonic());

        this.initPluginComponents();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
                .getComponentsForContainer(UIService.CONTAINER_HELP_MENU);

        while (pluginComponents.hasNext())
        {
            Component o = (Component) pluginComponents.next();

            this.add(o);
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
        Component c = (Component) event.getSource();

        if (event.getContainerID().equals(UIService.CONTAINER_HELP_MENU))
        {
            this.add(c);

            this.revalidate();
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();

        if (event.getContainerID().equals(UIService.CONTAINER_HELP_MENU))
        {
            this.remove(c);
        }
    }

}
