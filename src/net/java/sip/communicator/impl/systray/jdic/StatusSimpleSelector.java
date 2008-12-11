/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.systray.jdic;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>StatusSimpleSelector</tt> is a submenu which allow to select a status
 * for a protocol provider which does not support the OperationSetPresence
 * 
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class StatusSimpleSelector
    implements ActionListener
{

    /**
     * The protocol provider
     */
    private final ProtocolProviderService provider;
    
    /**
     * The logger for this class.
     */
    private final Logger logger = Logger.getLogger(StatusSimpleSelector.class);

    private final Object menu;

    /**
     * Creates an instance of <tt>StatusSimpleSelector</tt>
     * 
     * @param protocolProvider the protocol provider
     */
    public StatusSimpleSelector(ProtocolProviderService protocolProvider,
        boolean swing)
    {
        this.provider = protocolProvider;

        /* the parent item */
        String text = provider.getAccountID().getUserID();
        if (swing)
        {
            JMenu menu = new JMenu(text);

            ImageIcon icon =
                new ImageIcon(protocolProvider.getProtocolIcon().getIcon(
                    ProtocolIcon.ICON_SIZE_16x16));
            if (!provider.isRegistered())
            {
                icon =
                    new ImageIcon(LightGrayFilter.createDisabledImage(icon
                        .getImage()));
            }
            menu.setIcon(icon);

            this.menu = menu;
        }
        else
        {
            this.menu = new Menu(text);
        }

        /* the menu itself */
        createMenuItem("impl.systray.ONLINE_STATUS", "online");
        createMenuItem("impl.systray.OFFLINE_STATUS", "offline");
    }

    private void createMenuItem(String textKey, String name)
    {
        String text = Resources.getString(textKey);
        if (menu instanceof JMenu)
        {
            JMenuItem menuItem = new JMenuItem(text);
            menuItem.setName(name);
            menuItem.addActionListener(this);
            ((JMenu) menu).add(menuItem);
        }
        else
        {
            MenuItem menuItem = new MenuItem(text);
            menuItem.setName(name);
            menuItem.addActionListener(this);
            ((Menu) menu).add(menuItem);
        }
    }

    public Object getMenu()
    {
        return menu;
    }

    /**
     * Change the status of the protocol according to
     * the menu item selected
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();
        String itemName;
        if (source instanceof Component)
            itemName = ((Component) source).getName();
        else
            itemName = ((MenuComponent) source).getName();

        if(itemName.equals("online")) 
        {
            if(!this.provider.isRegistered()) 
            {
                new ProviderRegistration(provider).start();
            }
        }
        else 
        {
            if(    !this.provider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERED)
                && !this.provider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERING))
            {
                new ProviderUnRegistration(this.provider).start();
            }
        }
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        logger.trace("Systray update status for provider: "
            + provider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        if (menu instanceof AbstractButton)
            ((AbstractButton) menu).setIcon(new ImageIcon(presenceStatus
                .getStatusIcon()));
    }
}
