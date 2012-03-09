/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

/**
 * Implements an About menu item for the Help menu of the application in the
 * form of a <tt>PluginComponent</tt>.
 *
 * @author Lyubomir Marinov
 */
public class AboutWindowPluginComponent
    extends AbstractPluginComponent
{

    /**
     * Invokes the default action associated with Help > About regardless of the
     * specifics of its visual representation.
     */
    public static void actionPerformed()
    {
        AboutWindow.showAboutWindow();
    }

    private JMenuItem aboutMenuItem;

    /**
     * Constructor.
     *
     * @param container parent container
     */
    public AboutWindowPluginComponent(Container container)
    {
        super(container);
    }

    public Object getComponent()
    {
        if (aboutMenuItem == null)
        {
            aboutMenuItem = new JMenuItem(getName());
            aboutMenuItem
                .setMnemonic(
                    BrandingActivator
                        .getResources()
                            .getI18nMnemonic(
                                "plugin.branding.ABOUT_MENU_ENTRY"));

            aboutMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    AboutWindowPluginComponent.actionPerformed();
                }
            });
        }
        return aboutMenuItem;
    }

    public String getName()
    {
        return
            BrandingActivator
                .getResources()
                    .getI18NString("plugin.branding.ABOUT_MENU_ENTRY");
    }

    /**
     * Implements {@link PluginComponent#getPositionIndex()}. Returns
     * <tt>Integer#MAX_VALUE</tt> in order to indicate that the About menu item
     * in the Help menu is conventionally displayed at the very bottom.
     *
     * @return <tt>Integer#MAX_VALUE</tt> in order to indicate that the About
     * menu item in the Help menu is conventionally displayed at the very bottom
     * @see AbstractPluginComponent#getPositionIndex()
     */
    @Override
    public int getPositionIndex()
    {
        return Integer.MAX_VALUE;
    }
}
