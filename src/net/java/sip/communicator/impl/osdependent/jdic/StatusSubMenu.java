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
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.plugin.desktoputil.presence.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>StatusSubMenu</tt> provides a menu which allow to select the status
 * for each of the protocol providers registered when the menu appears
 *
 * @author Nicolas Chamouard
 * @author Lyubomir Marinov
 */
public class StatusSubMenu
    implements ProviderPresenceStatusListener,
               RegistrationStateChangeListener,
               ActionListener,
               ItemListener
{
    /**
     * Contains all accounts and corresponding menus.
     */
    private final Map<AccountID, Object> accountSelectors =
        new Hashtable<AccountID, Object>();

    private final Object menu;

    /**
     * Hide accounts from accounts status list.
     */
    private static boolean hideAccountStatusSelectors = false;

    /**
     * Initializes a new <tt>StatusSubMenu</tt> instance.
     *
     * @param swing <tt>true</tt> to represent this instance with a Swing
     * <tt>JMenu</tt>; <tt>false</tt> to use an AWT <tt>Menu</tt>
     */
    public StatusSubMenu(boolean swing)
    {
        String text = Resources.getString("impl.systray.SET_STATUS");

        if (swing)
        {
            JMenu menu = new JMenu(text);

            menu.setIcon(
                    Resources.getImage("service.systray.STATUS_MENU_ICON"));
            /* makes the menu look better */
            menu.setPreferredSize(new java.awt.Dimension(28, 24));

            this.menu = menu;
        }
        else
        {
            this.menu = new Menu(text);
        }

        {
            String hideAccountStatusSelectorsProperty
                = "impl.gui.HIDE_ACCOUNT_STATUS_SELECTORS";
            String hideAccountsStatusDefaultValue =
                OsDependentActivator.getResources()
                    .getSettingsString(hideAccountStatusSelectorsProperty);

            if(hideAccountsStatusDefaultValue != null)
                hideAccountStatusSelectors = Boolean.parseBoolean(
                    hideAccountsStatusDefaultValue);

            hideAccountStatusSelectors =
                OsDependentActivator.getConfigurationService()
                    .getBoolean(
                        hideAccountStatusSelectorsProperty,
                        hideAccountStatusSelectors);
        }

        PresenceStatus offlineStatus = null;
        // creates menu item entry for every global status
        for(GlobalStatusEnum status : GlobalStatusEnum.globalStatusSet)
        {
            createMenuItem(status, swing);

            if(status.getStatus() < 1)
                offlineStatus = status;
        }
        // initially it is offline
        selectItemFromStatus(offlineStatus.getStatus());

        this.addSeparator();

        addMenuItem(menu, new GlobalStatusMessageMenu(swing).getMenu());

        if(!hideAccountStatusSelectors)
            this.addSeparator();

        this.init();
    }

    public Object getMenu()
    {
        return menu;
    }

    /**
     * Creates a menu item with the given <tt>textKey</tt>, <tt>iconID</tt> and
     * <tt>name</tt>.
     * @return the created <tt>JCheckBoxMenuItem</tt>
     */
    private Object createMenuItem(
        GlobalStatusEnum status,
        boolean swing)
    {
        Object menuItem = null;

        if(swing)
        {
            JCheckBoxMenuItem mItem = new JCheckBoxMenuItem(
                GlobalStatusEnum.getI18NStatusName(status),
                new ImageIcon(status.getStatusIcon()));

            mItem.setName(status.getStatusName());
            mItem.addActionListener(this);
            menuItem = mItem;
        }
        else
        {
            CheckboxMenuItem mItem = new CheckboxMenuItem(
                GlobalStatusEnum.getI18NStatusName(status));
            mItem.setName(status.getStatusName());
            mItem.addItemListener(this);
            menuItem = mItem;
        }
        addMenuItem(getMenu(), menuItem);


        return menuItem;
    }

    /**
     * Adds separator to underlying menu implementation.
     */
    private void addSeparator()
    {
        if (menu instanceof JMenu)
            ((JMenu) menu).addSeparator();
        else
            ((Menu) menu).addSeparator();
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
        if(protocolProvider.getAccountID().isStatusMenuHidden())
            return;

        OperationSetPresence presence
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        boolean swing = (menu instanceof JComponent);

        if (presence == null)
        {
            StatusSimpleSelector simpleSelector =
                new StatusSimpleSelector(protocolProvider, swing);

            this.accountSelectors.put(protocolProvider.getAccountID(),
                simpleSelector);
            if(!hideAccountStatusSelectors)
                addMenuItem(menu, simpleSelector.getMenu());

            protocolProvider.addRegistrationStateChangeListener(this);
        }
        else
        {
            StatusSelector statusSelector
                = new StatusSelector(protocolProvider, presence, swing);

            this.accountSelectors.put(protocolProvider.getAccountID(),
                statusSelector);
            if(!hideAccountStatusSelectors)
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

        // no such provider added
        if(selector == null)
            return;

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
            = protocolProvider.getOperationSet(OperationSetPresence.class);

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

        for(ProtocolProviderService provider : getProtocolProviders())
        {
            if(!provider.getAccountID().isHidden())
                this.addAccount(provider);
        }
    }

    /**
     * Obtains all currently registered ProtocolProviderServices.
     * @return all currently registered ProtocolProviderServices.
     */
    private List<ProtocolProviderService> getProtocolProviders()
    {
        BundleContext bundleContext = OsDependentActivator.bundleContext;
        Collection<ServiceReference<ProtocolProviderService>> ppsRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderService.class);
        List<ProtocolProviderService> protocolProviders
            = new ArrayList<ProtocolProviderService>();

        // in case we found any
        if ((ppsRefs != null) && !ppsRefs.isEmpty())
        {
            for (ServiceReference<ProtocolProviderService> ppsRef : ppsRefs)
                protocolProviders.add(bundleContext.getService(ppsRef));
        }
        return protocolProviders;
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

        this.updateGlobalStatus();
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

        this.updateGlobalStatus();
    }

    /**
     * Updates the global status by picking the most connected protocol provider
     * status.
     */
    private void updateGlobalStatus()
    {
        int status = 0;
        boolean hasAvailableProvider = false;

        for(ProtocolProviderService protocolProvider : getProtocolProviders())
        {
            // We do not show hidden protocols in our status bar, so we do not
            // care about their status here.
            if (protocolProvider.getAccountID().isHidden())
                continue;

            if (!protocolProvider.isRegistered())
                continue;

            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            if(presence == null)
            {
                hasAvailableProvider = true;
                continue;
            }

            int presenceStatus
                = (presence == null)
                    ? PresenceStatus.AVAILABLE_THRESHOLD
                    : presence.getPresenceStatus().getStatus();

            if (status < presenceStatus)
                status = presenceStatus;
        }

        // if we have at least one online provider
        if(status == 0 && hasAvailableProvider)
            status = PresenceStatus.AVAILABLE_THRESHOLD;

        selectItemFromStatus(status);
    }

    /**
     * Selects the menu item corresponding to the given status.
     * For status constants we use here the values defined in the
     * <tt>PresenceStatus</tt>, but this is only for convenience.
     *
     * @param status the status to which the item should correspond
     */
    private void selectItemFromStatus(int status)
    {
        String nameToSelect;
        if(status < PresenceStatus.ONLINE_THRESHOLD)
        {
            nameToSelect = GlobalStatusEnum.OFFLINE_STATUS;
        }
        else if(status < PresenceStatus.EXTENDED_AWAY_THRESHOLD)
        {
            nameToSelect = GlobalStatusEnum.DO_NOT_DISTURB_STATUS;
        }
        else if(status < PresenceStatus.AWAY_THRESHOLD)
        {
            nameToSelect = GlobalStatusEnum.EXTENDED_AWAY_STATUS;
        }
        else if(status < PresenceStatus.AVAILABLE_THRESHOLD)
        {
            nameToSelect = GlobalStatusEnum.AWAY_STATUS;
        }
        else if(status < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
        {
            nameToSelect = GlobalStatusEnum.ONLINE_STATUS;
        }
        else if(status < PresenceStatus.MAX_STATUS_VALUE)
        {
            nameToSelect = GlobalStatusEnum.FREE_FOR_CHAT_STATUS;
        }
        else
        {
            nameToSelect = GlobalStatusEnum.OFFLINE_STATUS;
        }

        if(menu instanceof Menu)
        {
            Menu theMenu = (Menu) menu;
            for(int i =0; i < theMenu.getItemCount(); i++)
            {
                MenuItem item = theMenu.getItem(i);

                if(item instanceof CheckboxMenuItem)
                {
                    if(item.getName().equals(nameToSelect))
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
                    if(item.getName().equals(nameToSelect))
                        item.setSelected(true);
                    else
                        item.setSelected(false);
                }
            }
        }
    }

    /**
     * Change the status of the protocol according to the menu item selected
     *
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();
        if(source instanceof JMenuItem)
            changeStatusFromName(((JMenuItem)source).getName());
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
                changeStatusFromName(((CheckboxMenuItem)sourceItem).getName());
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

    /**
     * Changes global status from selected item name.
     * @param itemName the item name that was selected.
     */
    private void changeStatusFromName(String itemName)
    {
        OsDependentActivator.getGlobalStatusService().publishStatus(
                    GlobalStatusEnum.getStatusByName(itemName));
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
         * BasicInstantMessaging implementation)
         *
         * @param event ServiceEvent
         */
        public void serviceChanged(ServiceEvent event)
        {
            //if the event is caused by a bundle being stopped, we don't want to
            //know
            ServiceReference<?> serviceRef = event.getServiceReference();

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
