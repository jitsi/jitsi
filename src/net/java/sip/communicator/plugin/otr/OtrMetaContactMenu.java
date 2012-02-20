/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

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

    public OtrMetaContactMenu(Container container)
    {
        super(container);

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
                new OtrContactMenu(
                    contacts.next(), inMacOSXScreenMenuBar, menu, false);
            }
            else
                while (contacts.hasNext())
                {
                    new OtrContactMenu(
                        contacts.next(), inMacOSXScreenMenuBar, menu, true);
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
        whatsThis.setIcon(OtrActivator.resourceService
            .getImage("plugin.otr.HELP_ICON_15x15"));
        whatsThis.setText(OtrActivator.resourceService
            .getI18NString("plugin.otr.menu.WHATS_THIS"));
        whatsThis.addActionListener(this);
        menu.add(whatsThis);
    }

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
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
