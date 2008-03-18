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
    private JMenuItem aboutMenuItem
        = new JMenuItem(Resources.getString("aboutMenuEntry"));

    public AboutWindowPluginComponent()
    {
        aboutMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AboutWindow aboutWindow = new AboutWindow(null);
                aboutWindow.setVisible(true);
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
        return Container.CONTAINER_HELP_MENU;
    }

    public String getName()
    {
        return Resources.getString("aboutMenuEntry");
    }

    public void setCurrentContact(MetaContact metaContact)
    {
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }
}
