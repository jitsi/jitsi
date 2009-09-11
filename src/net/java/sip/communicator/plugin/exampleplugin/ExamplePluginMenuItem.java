/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.exampleplugin;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

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
    extends JMenuItem
    implements  PluginComponent,
                ActionListener
{
    private MetaContact metaContact;

    /**
     * Creates an instance of <tt>ExamplePluginMenuItem</tt>.
     */
    public ExamplePluginMenuItem()
    {
        super("Example plugin");

        this.addActionListener(this);
    }

    public void setCurrentContact(Contact contact)
    {}
    
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

    /**
     * Sets the current <tt>MetaContactGroup</tt>.
     * 
     * @see PluginComponent#setCurrentContactGroup(MetaContactGroup)
     */
    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {}

    /**
     * Listens for events triggered by user clicks on this menu item. Opens
     * the <tt>PluginDialog</tt>.
     */
    public void actionPerformed(ActionEvent e)
    {
        PluginDialog pluginDialog = new PluginDialog(metaContact);

        pluginDialog.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width/2
                - pluginDialog.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                - pluginDialog.getHeight()/2
            );

        pluginDialog.setVisible(true);
    }

    /**
     * No constraints to return.
     * 
     * @see PluginComponent#getConstraints()
     */
    public String getConstraints()
    {
        return null;
    }

    /**
     * Returns the container where we would like to add this menu item. In our
     * case this is the contact right button menu.
     * 
     * @see PluginComponent#getContainer()
     */
    public Container getContainer()
    {
        return Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU;
    }

    /**
     * Returns -1 as a position index, to indicate that the order in which this
     * menu item will be added in the parent container is not important.
     * 
     * @see PluginComponent#getPositionIndex()
     */
    public int getPositionIndex()
    {
        return -1;
    }

    /**
     * Returns <code>false</code> to indicate that this is not a native
     * component.
     * 
     * @see PluginComponent#isNativeComponent()
     */
    public boolean isNativeComponent()
    {
        return false;
    }
}
