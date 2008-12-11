/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.systray.jdic;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>StatusSubMenu</tt> provides a menu which allow to select the status
 * for each of the protocol providers registered when the menu appears
 * 
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class StatusSubMenu
{
    /**
     * A reference of <tt>Systray</tt>
     */
    private final SystrayServiceJdicImpl parentSystray;

    /**
     * Contains all accounts and corresponding menus.
     */
    private final Map<AccountID, Object> accountSelectors =
        new Hashtable<AccountID, Object>();

    private final Logger logger = Logger.getLogger(StatusSubMenu.class);

    private final Object menu;

    /**
     * Creates an instance of <tt>StatusSubMenu</tt>.
     * 
     * @param tray a reference of the parent <tt>Systray</tt>
     * @param swing <tt>true</tt> to represent this instance with a Swing
     *            <code>JMenu</code>; <tt>false</tt> to use an AWT
     *            <code>Menu</code>
     */
    public StatusSubMenu(SystrayServiceJdicImpl tray, boolean swing)
    {
        parentSystray = tray;

        String text = Resources.getString("impl.systray.SET_STATUS");
        if (swing)
        {
            JMenu menu = new JMenu(text);
            menu
                .setIcon(Resources.getImage("service.systray.STATUS_MENU_ICON"));

            /* makes the menu look better */
            menu.setPreferredSize(new java.awt.Dimension(28, 24));

            this.menu = menu;
        }
        else
        {
            this.menu = new Menu(text);
        }

        this.init();
    }

    public Object getMenu()
    {
        return menu;
    }

    /**
     * Adds the account corresponding to the given protocol provider to this
     * menu.
     * 
     * @param protocolProvider the protocol provider corresponding to the
     *            account to add
     */
    private void addAccount(ProtocolProviderService protocolProvider)
    {
        OperationSetPresence presence =
            (OperationSetPresence) protocolProvider
                .getOperationSet(OperationSetPresence.class);
        boolean swing = (menu instanceof JComponent);

        if (presence == null)
        {
            StatusSimpleSelector simpleSelector =
                new StatusSimpleSelector(protocolProvider, swing);

            this.accountSelectors.put(protocolProvider.getAccountID(),
                simpleSelector);
            addMenuItem(menu, simpleSelector.getMenu());
        }
        else
        {
            StatusSelector statusSelector =
                new StatusSelector(parentSystray, protocolProvider, presence,
                    swing);

            this.accountSelectors.put(protocolProvider.getAccountID(),
                statusSelector);
            addMenuItem(menu, statusSelector.getMenu());

            presence
                .addProviderPresenceStatusListener(new SystrayProviderPresenceStatusListener());
        }
    }

    static void addMenuItem(Object menu, Object menuItem)
    {
        if (menu instanceof Container)
            ((Container) menu).add((Component) menuItem);
        else
            ((Menu) menu).add((MenuItem) menuItem);
    }

    /**
     * Removes the account corresponding to the given protocol provider from
     * this menu.
     * 
     * @param protocolProvider the protocol provider corresponding to the
     * account to remove.
     */
    private void removeAccount(ProtocolProviderService protocolProvider)
    {
        Object selector =
            this.accountSelectors.get(protocolProvider.getAccountID());

        if (menu instanceof Container)
            ((Container) menu).remove((Component) selector);
        else
            ((MenuContainer) menu).remove((MenuComponent) selector);
    }

    /**
     * We fill the protocolProviderTable with all
     * running protocol providers at the start of
     * the bundle.
     */
    private void init()
    {
        SystrayActivator.bundleContext
            .addServiceListener(new ProtocolProviderServiceListener());

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs
                = SystrayActivator.bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(),null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger .error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {

            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) SystrayActivator.bundleContext
                        .getService(protocolProviderRefs[i]);

                boolean isHidden = 
                    provider.getAccountID().getAccountProperties().
                        get(ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;
                
                if(!isHidden)
                    this.addAccount(provider);
            }
        }
    }

    /**
     * Listens for <tt>ServiceEvent</tt>s indicating that a
     * <tt>ProtocolProviderService</tt> has been registered and completes the
     * account status menu.
     */
    private class ProtocolProviderServiceListener implements ServiceListener
    {
        /**
         * When a service is registered or unregistered, we update
         * the provider tables and add/remove listeners (if it supports
         * BasicInstantMessenging implementation)
         *
         * @param event ServiceEvent
         */
        public void serviceChanged(ServiceEvent event)
        {
            //if the event is caused by a bundle being stopped, we don't want to
            //know
            if(event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
            {
                return;
            }

            Object service = SystrayActivator.bundleContext
                .getService( event.getServiceReference());

            if (! (service instanceof ProtocolProviderService))
                return;

            ProtocolProviderService provider = (ProtocolProviderService)service;

            switch (event.getType())
            {
            case ServiceEvent.REGISTERED:
                addAccount(provider);
                break;

            case ServiceEvent.UNREGISTERING:
                removeAccount(provider);
                break;
            }
        }
    }

    /**
     * Listens for all providerStatusChanged and providerStatusMessageChanged
     * events in order to refresh the account status panel, when a status is
     * changed.
     */
    private class SystrayProviderPresenceStatusListener
        implements ProviderPresenceStatusListener
    {
        /**
         * Fired when an account has changed its status. We update the icon
         * in the menu.
         */
        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
        {
            ProtocolProviderService pps = evt.getProvider();

            StatusSelector selectorBox 
                = (StatusSelector) accountSelectors.get(pps.getAccountID());
        
            if(selectorBox == null)
                return;

            selectorBox.updateStatus(evt.getNewStatus());
        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt)
        {
        }
    }
}
