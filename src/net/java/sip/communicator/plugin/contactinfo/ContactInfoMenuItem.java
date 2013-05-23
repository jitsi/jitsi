/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.contactinfo;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;

/**
 *
 * @author Adam Goldstein
 */
public class ContactInfoMenuItem
    extends AbstractPluginComponent
    implements ActionListener
{
    private JMenuItem menuItem = null;

    private MetaContact metaContact;

    /**
     * Creates a <tt>ContactInfoMenuItem</tt>.
     */
    public ContactInfoMenuItem()
    {
        super(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
    }

    /**
     * Sets the currently selected <tt>MetaContact</tt>.
     * @param metaContact the currently selected meta contact
     */
    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /**
     * Initializes and shows the contact details dialog.
     */
    public void actionPerformed(ActionEvent e)
    {
        ContactInfoDialog cinfoDialog = new ContactInfoDialog(metaContact);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        cinfoDialog.setLocation(
            screenSize.width/2 - cinfoDialog.getWidth()/2,
            screenSize.height/2 - cinfoDialog.getHeight()/2);
        cinfoDialog.setVisible(true);
    }

    public Object getComponent()
    {
        return getMenuItem();
    }

    public String getName()
    {
        return getMenuItem().getText();
    }

    private JMenuItem getMenuItem()
    {
        if(menuItem == null)
        {
            menuItem =
                new JMenuItem(Resources.getString("service.gui.CONTACT_INFO"),
                    new ImageIcon(Resources.getImage(
                        "plugin.contactinfo.CONTACT_INFO_ICON")));
            menuItem.addActionListener(this);
        }

        return menuItem;
    }
}
