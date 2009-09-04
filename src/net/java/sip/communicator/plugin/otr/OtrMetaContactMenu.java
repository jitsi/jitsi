/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.Component; /* Explicit import required */
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author George Politis
 * 
 */
@SuppressWarnings("serial")
public class OtrMetaContactMenu
    extends JMenu
    implements PluginComponent
{

    private Container container;

    public OtrMetaContactMenu(Container container)
    {

        this.container = container;
        if (Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.equals(container))
        {
            Icon icon =
                OtrActivator.resourceService
                    .getImage("plugin.otr.MENU_ITEM_ICON_16x16");

            if (icon != null)
                this.setIcon(icon);
        }
        
        this.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.TITLE"));
    }

    public String getConstraints()
    {
        return null;
    }

    public Component getComponent()
    {
        return this;
    }

    public Container getContainer()
    {
        return this.container;
    }

    public int getPositionIndex()
    {
        return -1;
    }

    public boolean isNativeComponent()
    {
        return false;
    }

    public void setCurrentContact(MetaContact metaContact)
    {
        // Rebuild menu.
        this.removeAll();

        if (metaContact == null)
            return;

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            this.add(new OtrContactMenu(contacts.next()));
        }

        this.addSeparator();

        JMenuItem whatsThis = new JMenuItem();
        whatsThis.setIcon(OtrActivator.resourceService
            .getImage("plugin.otr.HELP_ICON_15x15"));
        whatsThis.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.WHATS_THIS"));
        whatsThis.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrActivator.scOtrEngine.launchHelp();
            }
        });
        this.add(whatsThis);
    }

    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }
}