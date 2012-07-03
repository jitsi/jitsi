/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>StatusSelector</tt> is a submenu which allows to select a status for
 * a protocol provider which supports the OperationSetPresence.
 *
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class StatusSelector
    implements ActionListener,
               ItemListener
{
    /**
     * A reference of <tt>Systray</tt>
     */
    private final SystrayServiceJdicImpl parentSystray;
    /**
     * The protocol provider
     */
    private final ProtocolProviderService provider;
    /**
     * The presence status
     */
    private final OperationSetPresence presence;

    /**
     * The logger for this class.
     */
    private final Logger logger = Logger.getLogger(StatusSelector.class);

    private final Object menu;

    /**
     * Creates an instance of StatusSelector
     *
     * @param jdicSystray a reference of the parent <tt>Systray</tt>
     * @param provider the protocol provider
     * @param presence the presence status
     */
    public StatusSelector(SystrayServiceJdicImpl jdicSystray,
        ProtocolProviderService provider, OperationSetPresence presence,
        boolean swing)
    {
        this.parentSystray = jdicSystray;
        this.provider = provider;
        this.presence = presence;

        /* the parent item */
        {
            String text = provider.getAccountID().getDisplayName();
            if (swing)
            {
                JMenu menu = new JMenu(text);
                byte[] icBytes = presence.getPresenceStatus().getStatusIcon();
                if(icBytes != null)
                    menu.setIcon(new ImageIcon(icBytes));

                this.menu = menu;
            }
            else
            {
                this.menu = new Menu(text);
            }
        }

        /* the submenu itself */

        PresenceStatus offlineStatus = null;
        Iterator<PresenceStatus> statusIterator
            = this.presence.getSupportedStatusSet();

        while (statusIterator.hasNext())
        {
            PresenceStatus status = statusIterator.next();

            // if connectivity less then 1, this is offline one
            if(status.getStatus() < 1)
                offlineStatus = status;

            String text = status.getStatusName();

            if (menu instanceof Container)
            {
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(text);

                byte[] icBytes = status.getStatusIcon();
                if(icBytes != null)
                    item.setIcon(new ImageIcon(icBytes));

                item.addActionListener(this);

                ((Container) menu).add(item);
            }
            else
            {
                CheckboxMenuItem item = new CheckboxMenuItem(text);
                item.addItemListener(this);
                ((Menu) menu).add(item);
            }
        }

        addSeparator();

        StatusSubMenu.addMenuItem(menu, new StatusMessageMenu(provider, swing)
            .getMenu());

        if(offlineStatus != null)
            updateStatus(offlineStatus);
    }

    private void addSeparator()
    {
        if (menu instanceof JMenu)
            ((JMenu) menu).addSeparator();
        else
            ((Menu) menu).addSeparator();
    }

    public Object getMenu()
    {
        return menu;
    }

    /**
     * Change the status of the protocol according to the menu item selected
     *
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();
        String menuItemText;
        if (source instanceof AbstractButton)
            menuItemText = ((AbstractButton) source).getText();
        else
            menuItemText = ((MenuItem) source).getLabel();

        changeStatus(menuItemText);
    }

    /**
     * Searches for presence status with the supplied name and publish this
     * status to the provider.
     * @param statusName
     */
    private void changeStatus(String statusName)
    {
        Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

        while (statusSet.hasNext())
        {
            PresenceStatus status = statusSet.next();

            if (status.getStatusName().equals(statusName))
            {
                OsDependentActivator.getGlobalStatusService()
                    .publishStatus(provider, status, true);

                break;
            }
        }
    }

    public void updateStatus(PresenceStatus presenceStatus)
    {
        if (logger.isTraceEnabled())
            logger.trace("Systray update status for provider: "
            + provider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        if (menu instanceof AbstractButton)
            ((AbstractButton) menu).setIcon(new ImageIcon(presenceStatus
                .getStatusIcon()));

        if(menu instanceof Menu)
        {
            Menu theMenu = (Menu) menu;
            for(int i =0; i < theMenu.getItemCount(); i++)
            {
                MenuItem item = theMenu.getItem(i);

                if(item instanceof CheckboxMenuItem)
                {
                    if(item.getLabel().equals(presenceStatus.getStatusName()))
                    {
                        ((CheckboxMenuItem)item).setState(true);
                    }
                    else
                    {
                        ((CheckboxMenuItem)item).setState(false);
                    }
                }
            }
        }
        else if(menu instanceof JMenu)
        {
            JMenu theMenu = (JMenu) menu;
            for(int i =0; i < theMenu.getItemCount(); i++)
            {
                JMenuItem item = theMenu.getItem(i);

                if(item instanceof JCheckBoxMenuItem)
                {
                    if(item.getText().equals(presenceStatus.getStatusName()))
                        item.setSelected(true);
                    else
                        item.setSelected(false);
                }
            }
        }
    }

    /**
     * Listens for changes in item state (CheckboxMenuItem)s.
     * @param e the event.
     */
    public void itemStateChanged(ItemEvent e)
    {
        Object sourceItem = e.getSource();
        if(e.getStateChange() == ItemEvent.SELECTED)
        {
            if(sourceItem instanceof CheckboxMenuItem)
            {
                changeStatus(((CheckboxMenuItem)sourceItem).getLabel());
            }
        }
        else if(e.getStateChange() == ItemEvent.DESELECTED)
        {
            if(sourceItem instanceof CheckboxMenuItem)
            {
                ((CheckboxMenuItem)sourceItem).setState(true);
            }
        }
    }
}
