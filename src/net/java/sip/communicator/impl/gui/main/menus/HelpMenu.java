/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>HelpMenu</tt> is a menu in the main application menu bar.
 * 
 * @author Yana Stamcheva
 * @author Thomas Hofer
 * @author Lyubomir Marinov
 */
public class HelpMenu 
    extends SIPCommMenu
    implements ActionListener,
               PluginComponentListener
{
    /**
     * The <tt>PluginContainer</tt> which implements the logic related to
     * dealing with <tt>PluginComponent</tt>s on behalf of this
     * <tt>HelpMenu</tt>.
     */
    private final PluginContainer pluginContainer;

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

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_HELP_MENU);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is
     * selected.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        pluginContainer.pluginComponentAdded(event);
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        pluginContainer.pluginComponentRemoved(event);
    }
}
