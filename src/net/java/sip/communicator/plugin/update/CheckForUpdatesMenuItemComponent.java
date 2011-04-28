/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.update;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

/**
 * Implements <tt>PluginComponent</tt> for the "Check for Updates" menu
 * item.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class CheckForUpdatesMenuItemComponent
    extends AbstractPluginComponent
{
    /**
     * The "Check for Updates" menu item.
     */
    private final JMenuItem checkForUpdatesMenuItem;

    /**
     * Initializes a new "Check for Updates" menu item.
     *
     * @param container the container of the update menu component
     */
    public CheckForUpdatesMenuItemComponent(Container container)
    {
        super(container);

        checkForUpdatesMenuItem
            = new JMenuItem(
                    Resources.getResources().getI18NString(
                            "plugin.updatechecker.UPDATE_MENU_ENTRY"));
        checkForUpdatesMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Update.checkForUpdates(true);
            }
        });
    }

    /**
     * Gets the UI <tt>Component</tt> of this <tt>PluginComponent</tt>.
     *
     * @return the UI <tt>Component</tt> of this <tt>PluginComponent</tt>
     * @see PluginComponent#getComponent()
     */
    public JMenuItem getComponent()
    {
        return checkForUpdatesMenuItem;
    }

    /**
     * Gets the name of this <tt>PluginComponent</tt>.
     *
     * @return the name of this <tt>PluginComponent</tt>
     * @see PluginComponent#getName()
     */
    public String getName()
    {
        return
            Resources.getResources().getI18NString(
                    "plugin.updatechecker.UPDATE_MENU_ENTRY");
    }
}
