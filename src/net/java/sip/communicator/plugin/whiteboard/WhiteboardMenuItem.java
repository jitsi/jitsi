/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.whiteboard;

import java.awt.event.*;

import java.util.*;
import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * WhiteboardMenuItem
 * 
 * @author Julien Waechter
 */
public class WhiteboardMenuItem
    extends JMenu
    implements  ContactAwareComponent,
                ActionListener
{
    /**
     * The current meta contact
     */
    private MetaContact metaContact;

    /**
     * The Whiteboard session manager
     */
    private WhiteboardSessionManager session;

    /**
     * WhiteboardMenuItem constructor.
     *
     * @param session the whiteboard session manager
     */
    public WhiteboardMenuItem (WhiteboardSessionManager session)
    {
        super (Resources.getString("whiteboardMenuItemText"));
        this.session = session;
        this.setIcon (Resources.getImage ("mpenIcon"));
    }

    /**
     * Sets the current meta contact.
     *
     * @param metaContact the current meta contact
     */
    public void setCurrentContact (MetaContact metaContact)
    {
        this.metaContact = metaContact;
        
        this.removeAll();
        
        Iterator iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact contact = (Contact)iter.next();
            ProtocolProviderService pps = contact.getProtocolProvider();
            
            OperationSetWhiteboarding opSetWb = (OperationSetWhiteboarding)
                pps.getOperationSet(OperationSetWhiteboarding.class);

            String contactDisplayName = contact.getDisplayName();
            
            JMenuItem contactItem = new JMenuItem(contactDisplayName);
            contactItem.setName(contact.getDisplayName() + pps.getProtocolName());

            if (opSetWb != null)
            {
                contactItem.addActionListener(this);
            }
            else
            {
                contactItem.setEnabled(false);
                contactItem.setToolTipText(
                        Resources.getString("whiteboardMenuItemNotSupportedTooltip"));
            }
            
            this.add(contactItem);
        }
    }

    /**
     * Sets the current meta group.
     *
     * @param metaGroup the current meta contact group
     */
    public void setCurrentContactGroup (MetaContactGroup metaGroup)
    {
    }

    /**
     * Invoked when an action occurs: user start a whiteboard session.
     * 
     * @param e event
     */
    public void actionPerformed (ActionEvent e)
    {
        String itemID = ((JMenuItem)e.getSource()).getName();
        Iterator i = this.metaContact.getContacts();

        while(i.hasNext()) 
        {
            Contact contact = (Contact)i.next();

            String id = contact.getAddress()
                + contact.getProtocolProvider().getProtocolName();

            if(itemID.equals(id)) 
                session.initWhiteboard (contact);
        }
    }
}