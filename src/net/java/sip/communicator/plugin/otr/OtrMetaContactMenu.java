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
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;

/**
 * @author George Politis
 * @author Lubomir Marinov
 */
public class OtrMetaContactMenu
    extends AbstractPluginComponent
    implements ActionListener,
               PopupMenuListener
{

    /**
     * The last known <tt>MetaContact</tt> to be currently selected and to be
     * depicted by this instance and the <tt>OtrContactMenu</tt>s it contains.
     */
    private MetaContact currentContact;

    /**
     * The indicator which determines whether the <tt>JMenu</tt> of this
     * <tt>OtrMetaContactMenu</tt> is displayed in the Mac OS X screen menu bar
     * and thus should work around the known problem of PopupMenuListener not
     * being invoked.
     */
    private final boolean inMacOSXScreenMenuBar;

    /**
     * The <tt>JMenu</tt> which is the component of this plug-in.
     */
    private JMenu menu;

    /**
     * The "What's this?" <tt>JMenuItem</tt> which launches help on the subject
     * of off-the-record messaging.
     */
    private JMenuItem whatsThis;

    public OtrMetaContactMenu(Container container,
                              PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);

        inMacOSXScreenMenuBar =
            Container.CONTAINER_CHAT_MENU_BAR.equals(container)
                && OtrActivator.uiService.useMacOSXScreenMenuBar();
    }

    /*
     * Implements ActionListener#actionPerformed(ActionEvent). Handles the
     * invocation of the whatsThis menu item i.e. launches help on the subject
     * of off-the-record messaging.
     */
    public void actionPerformed(ActionEvent e)
    {
        OtrActivator.scOtrEngine.launchHelp();
    }

    /**
     * Creates an {@link OtrContactMenu} for each {@link Contact} contained in
     * the <tt>metaContact</tt>.
     *
     * @param metaContact The {@link MetaContact} this
     *            {@link OtrMetaContactMenu} refers to.
     */
    private void createOtrContactMenus(MetaContact metaContact)
    {
        JMenu menu = getMenu();

        // Remove any existing OtrContactMenu items.
        menu.removeAll();

        // Create the new OtrContactMenu items.
        if (metaContact != null)
        {
            Iterator<Contact> contacts = metaContact.getContacts();

            if (metaContact.getContactCount() == 1)
            {
                Contact contact = contacts.next();
                Collection<ContactResource> resources = contact.getResources();
                if (contact.supportResources() &&
                    resources != null &&
                    resources.size() > 0)
                {
                    for (ContactResource resource : resources)
                    {
                        new OtrContactMenu(
                            OtrContactManager.getOtrContact(contact, resource),
                            inMacOSXScreenMenuBar,
                            menu,
                            true);
                    }
                }
                else
                    new OtrContactMenu(
                        OtrContactManager.getOtrContact(contact, null),
                        inMacOSXScreenMenuBar,
                        menu,
                        false);
            }
            else
                while (contacts.hasNext())
                {
                    Contact contact = contacts.next();
                    Collection<ContactResource> resources =
                        contact.getResources();
                    if (contact.supportResources() &&
                        resources != null &&
                        resources.size() > 0)
                    {
                        for (ContactResource resource : resources)
                        {
                            new OtrContactMenu(
                                OtrContactManager.getOtrContact(
                                    contact, resource),
                                inMacOSXScreenMenuBar,
                                menu,
                                true);
                        }
                    }
                    else
                        new OtrContactMenu(
                            OtrContactManager.getOtrContact(contact, null),
                            inMacOSXScreenMenuBar,
                            menu,
                            true);
                }
        }
    }

    /*
     * Implements PluginComponent#getComponent(). Returns the JMenu which is the
     * component of this plug-in creating it first if it doesn't exist.
     */
    public Component getComponent()
    {
        return getMenu();
    }

    /**
     * Gets the <tt>JMenu</tt> which is the component of this plug-in. If it
     * still doesn't exist, it's created.
     *
     * @return the <tt>JMenu</tt> which is the component of this plug-in
     */
    private JMenu getMenu()
    {
        if (menu == null)
        {
            menu = new SIPCommMenu();
            menu.setText(getName());

            if (Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU
                .equals(getContainer()))
            {
                Icon icon =
                    OtrActivator.resourceService
                        .getImage("plugin.otr.MENU_ITEM_ICON_16x16");

                if (icon != null)
                    menu.setIcon(icon);
            }

            if (!inMacOSXScreenMenuBar)
                menu.getPopupMenu().addPopupMenuListener(this);
        }
        return menu;
    }

    /*
     * Implements PluginComponent#getName().
     */
    public String getName()
    {
        return OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.TITLE");
    }

    /*
     * Implements PopupMenuListener#popupMenuCanceled(PopupMenuEvent).
     */
    public void popupMenuCanceled(PopupMenuEvent e)
    {
        createOtrContactMenus(null);
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeInvisible(
     * PopupMenuEvent).
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        popupMenuCanceled(e);
    }

    /*
     * Implements PopupMenuListener#popupMenuWillBecomeVisible(PopupMenuEvent).
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
        createOtrContactMenus(currentContact);

        JMenu menu = getMenu();

        menu.addSeparator();

        whatsThis = new JMenuItem();
        whatsThis.setIcon(
                OtrActivator.resourceService.getImage(
                        "plugin.otr.HELP_ICON_15x15"));
        whatsThis.setText(
                OtrActivator.resourceService.getI18NString(
                        "plugin.otr.menu.WHATS_THIS"));
        whatsThis.addActionListener(this);
        menu.add(whatsThis);
    }

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        if (this.currentContact != metaContact)
        {
            this.currentContact = metaContact;

            if (inMacOSXScreenMenuBar)
                popupMenuWillBecomeVisible(null);
            else if ((menu != null) && menu.isPopupMenuVisible())
                createOtrContactMenus(currentContact);
        }
    }
}
