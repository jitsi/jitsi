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

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.ContactAwareComponent;

public class ExamplePluginMenuItem
    extends JMenuItem
    implements  ContactAwareComponent,
                ActionListener
{
    private MetaContact metaContact;
    
    public ExamplePluginMenuItem()
    {
        super("Example plugin");
        
        this.addActionListener(this);        
    }
    
    public void setCurrentContact(MetaContact metaContact)
    {   
        this.metaContact = metaContact;
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {}
    
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
}
