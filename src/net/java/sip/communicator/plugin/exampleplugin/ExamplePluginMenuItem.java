/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    public ExamplePluginMenuItem()
    {
        super(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
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
    public void setCurrentContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }
}
