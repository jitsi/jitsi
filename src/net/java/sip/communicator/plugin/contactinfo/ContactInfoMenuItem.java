package net.java.sip.communicator.plugin.contactinfo;
/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author Adam Goldstein
 */
public class ContactInfoMenuItem
    implements  PluginComponent,
                ActionListener
{
    private JMenuItem menuItem
        = new JMenuItem(Resources.getString("service.gui.CONTACT_INFO"),
            new ImageIcon(Resources.getImage(
                "plugin.contactinfo.CONTACT_INFO_ICON")));

    private MetaContact metaContact;

    /**
     * Creates a <tt>ContactInfoMenuItem</tt>.
     */
    public ContactInfoMenuItem()
    {
        menuItem.addActionListener(this);
    }

    public void setCurrentContact(Contact contact)
    {}
    
    /**
     * Sets the currently selected <tt>MetaContact</tt>.
     * @param metaContact the currently selected meta contact
     */
    public void setCurrentContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {}

    /**
     * Initializes and shows the contact details dialog.
     */
    public void actionPerformed(ActionEvent e)
    {
        ContactInfoDialog cinfoDialog = new ContactInfoDialog(metaContact);

        cinfoDialog.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width/2
                - cinfoDialog.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                - cinfoDialog.getHeight()/2
            );

        cinfoDialog.setVisible(true);
    }

    public Object getComponent()
    {
        return menuItem;
    }

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU;
    }

    public String getName()
    {
        return menuItem.getText();
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