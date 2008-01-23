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

/**
 * 
 * @author Adam Goldstein
 */
public class ContactInfoMenuItem
    extends JMenuItem
    implements  ContactAwareComponent,
                ActionListener
{
    private MetaContact metaContact;

    /**
     * Creates a <tt>ContactInfoMenuItem</tt>.
     */
    public ContactInfoMenuItem()
    {
        super(  Resources.getString("contactInfo"),
                Resources.getImage("infoIcon"));

        this.addActionListener(this);
    }

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
}