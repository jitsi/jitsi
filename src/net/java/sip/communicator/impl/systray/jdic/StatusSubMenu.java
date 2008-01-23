/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.impl.systray.jdic;

import java.awt.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>StatusSubMenu</tt> provides a menu which allow
 * to select the status for each of the protocol providers
 * registered when the menu appears
 * 
 * @author Nicolas Chamouard
 *
 */

public class StatusSubMenu
    extends JMenu
{
    /**
     * A reference of <tt>Systray</tt>
     */
    private SystrayServiceJdicImpl parentSystray;

    /**
     * Contains all accounts and corresponding menus.
     */
    private Hashtable accountSelectors = new Hashtable();

    private Logger logger = Logger.getLogger(StatusSubMenu.class);

    /**
     * Creates an instance of <tt>StatusSubMenu</tt>.
     * @param tray a reference of the parent <tt>Systray</tt>
     */
    public StatusSubMenu(SystrayServiceJdicImpl tray)
    {
        
        parentSystray = tray;

        this.setText(Resources.getString("setStatus"));
        this.setIcon(Resources.getImage("statusMenuIcon"));

        /* makes the menu look better */
        this.setPreferredSize(new java.awt.Dimension(28, 24));

        this.init();
    }

    /**
     * Adds the account corresponding to the given protocol provider to this
     * menu.
     * 
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     */
    private void addAccount(ProtocolProviderService protocolProvider)
    {
        OperationSetPresence presence = (OperationSetPresence)
            protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presence == null)
        {
            StatusSimpleSelector simpleSelector = 
                new StatusSimpleSelector(parentSystray, protocolProvider);

            this.accountSelectors.put(  protocolProvider.getAccountID(),
                                        simpleSelector);
            this.add(simpleSelector);
        }
        else
        {
            StatusSelector statusSelector = 
                new StatusSelector( parentSystray,
                                    protocolProvider,
                                    presence);

            this.accountSelectors.put(  protocolProvider.getAccountID(),
                                        statusSelector);
            this.add(statusSelector);

            presence.addProviderPresenceStatusListener(
                new SystrayProviderPresenceStatusListener());
        }
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
        Component c = (Component) this.accountSelectors
            .get(protocolProvider.getAccountID());

        this.remove(c);
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
                        get("HIDDEN_PROTOCOL") != null;
                
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

            if (event.getType() == ServiceEvent.REGISTERED)
                addAccount(provider);

            if (event.getType() == ServiceEvent.UNREGISTERING)
               removeAccount(provider);
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