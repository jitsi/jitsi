/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;

public class AboutWindowPluginComponent
    implements PluginComponent
{

    /**
     * Invokes the default action associated with Help > About regardless of the
     * specifics of its visual representation.
     */
    public static void actionPerformed()
    {
        AboutWindow.showAboutWindow();
    }

    private final JMenuItem aboutMenuItem;

    private final Container container;

    public AboutWindowPluginComponent(Container c)
    {
        this.container = c;

        aboutMenuItem = new JMenuItem(getName());
        aboutMenuItem
            .setMnemonic(
                BrandingActivator
                    .getResources()
                        .getI18nMnemonic("plugin.branding.ABOUT_MENU_ENTRY"));

        aboutMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AboutWindowPluginComponent.actionPerformed();
            }
        });
    }

    public Object getComponent()
    {
        return aboutMenuItem;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return this.container;
    }

    public String getName()
    {
        return
            BrandingActivator
                .getResources()
                    .getI18NString("plugin.branding.ABOUT_MENU_ENTRY");
    }

    public void setCurrentContact(MetaContact metaContact)
    {
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }

    public int getPositionIndex()
    {
        return -1;
    }

    public boolean isNativeComponent()
    {
        return false;
    }
}
