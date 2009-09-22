/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.*;
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
    implements ProviderPresenceStatusListener,
               RegistrationStateChangeListener
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
        OperationSetPresence presence
            = (OperationSetPresence)
                protocolProvider.getOperationSet(OperationSetPresence.class);
        boolean swing = (menu instanceof JComponent);

        if (presence == null)
        {
            StatusSimpleSelector simpleSelector =
                new StatusSimpleSelector(protocolProvider, swing);

            this.accountSelectors.put(protocolProvider.getAccountID(),
                simpleSelector);
            addMenuItem(menu, simpleSelector.getMenu());

            protocolProvider.addRegistrationStateChangeListener(this);
        }
        else
        {
            StatusSelector statusSelector =
                new StatusSelector(parentSystray, protocolProvider, presence,
                    swing);

            this.accountSelectors.put(protocolProvider.getAccountID(),
                statusSelector);
            addMenuItem(menu, statusSelector.getMenu());

            presence.addProviderPresenceStatusListener(this);
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
     *            account to remove.
     */
    private void removeAccount(ProtocolProviderService protocolProvider)
    {
        Object selector =
            this.accountSelectors.get(protocolProvider.getAccountID());
        Object selectorMenu;
        if (selector instanceof StatusSimpleSelector)
            selectorMenu = ((StatusSimpleSelector) selector).getMenu();
        else
            selectorMenu = ((StatusSelector) selector).getMenu();

        if (menu instanceof Container)
            ((Container) menu).remove((Component) selectorMenu);
        else
            ((MenuContainer) menu).remove((MenuComponent) selectorMenu);

        /*
         * Remove the listeners installed in
         * addAccount(ProtocolProviderService).
         */
        OperationSetPresence presence
            = (OperationSetPresence)
                protocolProvider.getOperationSet(OperationSetPresence.class);
        if (presence != null)
            presence.removeProviderPresenceStatusListener(this);
        else
            protocolProvider.removeRegistrationStateChangeListener(this);
    }

    /**
     * We fill the protocolProviderTable with all
     * running protocol providers at the start of
     * the bundle.
     */
    private void init()
    {
        OsDependentActivator.bundleContext
            .addServiceListener(new ProtocolProviderServiceListener());

        ServiceReference[] protocolProviderRefs;
        try
        {
            protocolProviderRefs
                = OsDependentActivator.bundleContext.getServiceReferences(
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

            for (ServiceReference protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService)
                        OsDependentActivator
                            .bundleContext.getService(protocolProviderRef);
                boolean isHidden
                    = provider
                            .getAccountID()
                                .getAccountProperty(
                                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN)
                        != null;
                
                if(!isHidden)
                    this.addAccount(provider);
            }
        }
    }

    /**
     * Fired when an account has changed its status. We update the icon
     * in the menu.
     * 
     * @param evt
     */
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {
        ProtocolProviderService pps = evt.getProvider();
        StatusSelector selectorBox 
            = (StatusSelector) accountSelectors.get(pps.getAccountID());
    
        if (selectorBox != null)
            selectorBox.updateStatus(evt.getNewStatus());
    }

    /*
     * ImplementsProviderPresenceStatusListener#providerStatusMessageChanged(
     * PropertyChangeEvent).
     */
    public void providerStatusMessageChanged(PropertyChangeEvent evt)
    {
    }

    /*
     * ImplementsRegistrationStateChangeListener#registrationStateChanged(
     * RegistrationStateChangeEvent). Updates the status of accounts which do
     * not support presence.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        ProtocolProviderService pps = evt.getProvider();
        StatusSimpleSelector selectorBox 
            = (StatusSimpleSelector) accountSelectors.get(pps.getAccountID());
    
        if (selectorBox != null)
            selectorBox.updateStatus();
    }

    /**
     * Listens for <tt>ServiceEvent</tt>s indicating that a
     * <tt>ProtocolProviderService</tt> has been registered and completes the
     * account status menu.
     */
    private class ProtocolProviderServiceListener
        implements ServiceListener
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
            ServiceReference serviceRef = event.getServiceReference();

            if(serviceRef.getBundle().getState() == Bundle.STOPPING)
                return;

            Object service
                = OsDependentActivator.bundleContext.getService(serviceRef);

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
}
