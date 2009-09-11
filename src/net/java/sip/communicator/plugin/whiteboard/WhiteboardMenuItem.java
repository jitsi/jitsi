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
    implements  PluginComponent,
                ActionListener
{
    private JMenu whiteboardMenu
        = new JMenu(Resources.getString("plugin.whiteboard.MENU_ITEM"));

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
        this.session = session;
        this.whiteboardMenu.setIcon (
            Resources.getImage ("plugin.whiteboard.MPEN_ICON"));
    }

    public void setCurrentContact (Contact contact)
    {
    }
    
    /**
     * Sets the current meta contact.
     *
     * @param metaContact the current meta contact
     */
    public void setCurrentContact (MetaContact metaContact)
    {
        this.metaContact = metaContact;

        this.whiteboardMenu.removeAll();

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
                        Resources.getString("plugin.whiteboard.NOT_SUPPORTED"));
            }

            this.whiteboardMenu.add(contactItem);
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

    public String getConstraints()
    {
        return null;
    }

    public Container getContainer()
    {
        return Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU;
    }

    public Object getComponent()
    {
        if(metaContact == null)
        {
            whiteboardMenu.setEnabled(false);
            return whiteboardMenu;
        }

        Iterator iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact contact = (Contact)iter.next();
            ProtocolProviderService pps = contact.getProtocolProvider();

            OperationSetWhiteboarding opSetWb = (OperationSetWhiteboarding)
                pps.getOperationSet(OperationSetWhiteboarding.class);

            if (opSetWb != null)
            {
                whiteboardMenu.setEnabled(true);
                return whiteboardMenu;
            }
        }

        whiteboardMenu.setEnabled(false);
        return whiteboardMenu;
    }

    public String getName()
    {
        return whiteboardMenu.getText();
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