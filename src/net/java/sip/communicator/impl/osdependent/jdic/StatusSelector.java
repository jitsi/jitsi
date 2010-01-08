/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
    implements ActionListener
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
            String text = provider.getAccountID().getUserID();
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

        Iterator<PresenceStatus> statusIterator
            = this.presence.getSupportedStatusSet();

        while (statusIterator.hasNext())
        {
            PresenceStatus status = statusIterator.next();
            String text = status.getStatusName();

            if (menu instanceof Container)
            {
                JMenuItem item = new JMenuItem(text);

                byte[] icBytes = status.getStatusIcon();
                if(icBytes != null)
                    item.setIcon(new ImageIcon(icBytes));

                item.addActionListener(this);

                ((Container) menu).add(item);
            }
            else
            {
                MenuItem item = new MenuItem(text);
                item.addActionListener(this);
                ((Menu) menu).add(item);
            }
        }

        addSeparator();

        StatusSubMenu.addMenuItem(menu, new StatusMessageMenu(provider, swing)
            .getMenu());
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

        Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

        while (statusSet.hasNext())
        {
            PresenceStatus status = statusSet.next();

            if (status.getStatusName().equals(menuItemText))
            {
                RegistrationState registrationState =
                    provider.getRegistrationState();

                if (registrationState == RegistrationState.REGISTERED
                    && !presence.getPresenceStatus().equals(status))
                {
                    if (status.isOnline())
                        new PublishPresenceStatusThread(status).start();
                    else
                        new ProviderUnRegistration(this.provider).start();
                }
                else if (registrationState != RegistrationState.REGISTERED
                    && registrationState != RegistrationState.REGISTERING
                    && registrationState != RegistrationState.AUTHENTICATING
                    && status.isOnline())
                {
                    new ProviderRegistration(provider).start();
                }
                else if (!status.isOnline()
                    && !(registrationState == RegistrationState.UNREGISTERING))
                {
                    new ProviderUnRegistration(this.provider).start();
                }

                parentSystray.saveStatusInformation(provider, status
                    .getStatusName());

                break;
            }
        }
    }

    public void updateStatus(PresenceStatus presenceStatus)
    {
        logger.trace("Systray update status for provider: "
            + provider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        if (menu instanceof AbstractButton)
            ((AbstractButton) menu).setIcon(new ImageIcon(presenceStatus
                .getStatusIcon()));
    }

    /**
     *  This class allow to use a thread to change the presence status.
     */
    private class PublishPresenceStatusThread extends Thread
    {
        private final PresenceStatus status;
        
        public PublishPresenceStatusThread(PresenceStatus status)
        {
            this.status = status;
        }

        public void run()
        {
            try {
                presence.publishPresenceStatus(status, "");
            }
            catch (IllegalArgumentException e1)
            {
                logger.error("Error - changing status", e1);
            }
            catch (IllegalStateException e1)
            {
                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {
                String msg;

                switch (e1.getErrorCode())
                {
                case OperationFailedException.GENERAL_ERROR:
                    msg
                        = "General error occured while "
                            + "publishing presence status.";
                    break;
                case OperationFailedException.NETWORK_FAILURE:
                    msg
                        = "Network failure occured while "
                            + "publishing presence status.";
                    break;
                case OperationFailedException.PROVIDER_NOT_REGISTERED:
                    msg
                        = "Protocol provider must be"
                            + "registered in order to change status.";
                    break;
                default:
                    return;
                }

                logger.error(msg, e1);
            }
        }
    }
}
