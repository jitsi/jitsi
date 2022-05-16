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
package net.java.sip.communicator.plugin.exampleplugin;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;

/**
 * The <tt>ExamplePluginMenuItem</tt> is a <tt>JMenuItem</tt> that implements
 * the <tt>PluginComponent</tt> interface. The <tt>PluginComponent</tt>
 * interface allows us to add this menu item in the user interface bundle by
 * registering it through the the bundle context
 * (see {@link ExamplePluginActivator#start(org.osgi.framework.BundleContext)}).
 *
 * @author Yana Stamcheva
 */
public class ExamplePluginMenuItem
    extends AbstractPluginComponent
    implements ActionListener
{
    private JMenuItem menuItem;

    private MetaContact metaContact;

    /**
     * Creates an instance of <tt>ExamplePluginMenuItem</tt>.
     */
    public ExamplePluginMenuItem(PluginComponentFactory parentFactory)
    {
        super(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU, parentFactory);
    }

    /**
     * Listens for events triggered by user clicks on this menu item. Opens
     * the <tt>PluginDialog</tt>.
     */
    public void actionPerformed(ActionEvent e)
    {
        PluginDialog pluginDialog = new PluginDialog(metaContact);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        pluginDialog
            .setLocation(
                screenSize.width/2 - pluginDialog.getWidth()/2,
                screenSize.height/2 - pluginDialog.getHeight()/2);

        pluginDialog.setVisible(true);
    }

    /*
     * Implements PluginComponent#getComponent().
     */
    public Object getComponent()
    {
        if (menuItem == null)
        {
            menuItem = new JMenuItem(getName());
            menuItem.addActionListener(this);
        }
        return menuItem;
    }

    /*
     * Implements PluginComponent#getName().
     */
    public String getName()
    {
        return "Example plugin";
    }

    /**
     * Sets the current <tt>MetaContact</tt>. This in the case of the contact
     * right button menu container would be the underlying contact in the
     * contact list.
     *
     * @param metaContact the <tt>MetaContact</tt> to set.
     *
     * @see PluginComponent#setCurrentContact(MetaContact)
     */
    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }
}
