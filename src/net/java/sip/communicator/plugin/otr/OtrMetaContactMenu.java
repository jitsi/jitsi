/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author George Politis
 */
public class OtrMetaContactMenu
    extends AbstractPluginComponent
{

    /**
     * The <code>JMenu</code> which is the component of this plugin.
     */
    private JMenu menu;

    public OtrMetaContactMenu(Container container)
    {
        super(container);
    }

    /*
     * Implements PluginComponent#getComponent(). Returns the JMenu which is
     * the component of this plugin creating it first if it doesn't exist.
     */
    public Component getComponent()
    {
        return getMenu();
    }

    /**
     * Gets the <code>JMenu</code> which is the component of this plugin. If it
     * still doesn't exist, it's created.
     * 
     * @return the <code>JMenu</code> which is the component of this plugin
     */
    private JMenu getMenu()
    {
        if (menu == null)
        {
            menu = new JMenu();

            if (Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU
                    .equals(getContainer()))
            {
                Icon icon
                    = OtrActivator
                        .resourceService
                            .getImage("plugin.otr.MENU_ITEM_ICON_16x16");

                if (icon != null)
                    menu.setIcon(icon);
            }
            
            menu.setText(getName());
        }
        return menu;
    }

    /*
     * Implements PluginComponent#getName().
     */
    public String getName()
    {
        return
            OtrActivator
                .resourceService
                    .getI18NString("plugin.otr.menu.TITLE");
    }

    public void setCurrentContact(MetaContact metaContact)
    {
        // Rebuild menu.
        if (menu != null)
            menu.removeAll();

        if (metaContact == null)
            return;

        JMenu menu = getMenu();

        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext())
        {
            menu.add(new OtrContactMenu(contacts.next()));
        }

        menu.addSeparator();

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
        menu.add(whatsThis);
    }
}
