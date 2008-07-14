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
        = new JMenuItem(BrandingActivator.getResources().
            getI18NString("aboutMenuEntry"));

    private Container container;

    public AboutWindowPluginComponent(Container c)
    {
        this.container = c;

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
        return this.container;
    }

    public String getName()
    {
        return BrandingActivator.getResources().getI18NString("aboutMenuEntry");
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
}
