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
    extends AbstractPluginComponent
    implements ActionListener
{
    private final JMenu whiteboardMenu
        = new JMenu(Resources.getString("plugin.whiteboard.MENU_ITEM"));

    /**
     * The current meta contact
     */
    private MetaContact metaContact;

    /**
     * The Whiteboard session manager
     */
    private final WhiteboardSessionManager session;

    /**
     * WhiteboardMenuItem constructor.
     *
     * @param session the whiteboard session manager
     */
    public WhiteboardMenuItem (WhiteboardSessionManager session,
                               PluginComponentFactory parentFactory)
    {
        super(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU, parentFactory);

        this.session = session;
        this.whiteboardMenu.setIcon (
            Resources.getImage ("plugin.whiteboard.MPEN_ICON"));
    }

    @Override
    public void setCurrentContact (Contact contact)
    {
    }

    /**
     * Sets the current meta contact.
     *
     * @param metaContact the current meta contact
     */
    @Override
    public void setCurrentContact (MetaContact metaContact)
    {
        this.metaContact = metaContact;

        this.whiteboardMenu.removeAll();

        Iterator<Contact> iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact contact = iter.next();
            ProtocolProviderService pps = contact.getProtocolProvider();

            OperationSetWhiteboarding opSetWb
                = pps.getOperationSet(OperationSetWhiteboarding.class);

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
    @Override
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
        Iterator<Contact> i = this.metaContact.getContacts();

        while(i.hasNext())
        {
            Contact contact = i.next();

            String id = contact.getAddress()
                + contact.getProtocolProvider().getProtocolName();

            if(itemID.equals(id))
                session.initWhiteboard (contact);
        }
    }

    public Object getComponent()
    {
        if(metaContact == null)
        {
            whiteboardMenu.setEnabled(false);
            return whiteboardMenu;
        }

        Iterator<Contact> iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact contact = iter.next();
            ProtocolProviderService pps = contact.getProtocolProvider();

            OperationSetWhiteboarding opSetWb
                = pps.getOperationSet(OperationSetWhiteboarding.class);

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
}
