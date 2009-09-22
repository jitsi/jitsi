/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

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

    private final Object menu;

    /**
     * Creates an instance of <tt>StatusSimpleSelector</tt>
     * 
     * @param protocolProvider the protocol provider
     * @param swing
     */
    public StatusSimpleSelector(
        ProtocolProviderService protocolProvider,
        boolean swing)
    {
        this.provider = protocolProvider;

        /* the parent item */
        String text = provider.getAccountID().getUserID();
        this.menu = swing ? new JMenu(text) : new Menu(text);
        updateStatus();

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
            RegistrationState registrationState
                = provider.getRegistrationState();

            if(!RegistrationState.UNREGISTERED.equals(registrationState)
                    && !RegistrationState.UNREGISTERING
                            .equals(registrationState))
            {
                new ProviderUnRegistration(this.provider).start();
            }
        }
    }

    public void updateStatus()
    {
        if (menu instanceof AbstractButton)
        {
            byte[] iconBytes
                = provider
                    .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if ((iconBytes != null) && (iconBytes.length > 0))
            {
                ImageIcon icon = new ImageIcon(iconBytes);

                if (!provider.isRegistered())
                    icon
                        = new ImageIcon(
                                LightGrayFilter
                                    .createDisabledImage(icon.getImage()));
                ((AbstractButton) menu).setIcon(icon);
            }
        }
    }
}
