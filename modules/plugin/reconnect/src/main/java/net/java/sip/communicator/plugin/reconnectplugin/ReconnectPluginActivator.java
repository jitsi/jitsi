/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2018 - present 8x8, Inc.
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
package net.java.sip.communicator.plugin.reconnectplugin;

import java.util.*;
import java.util.stream.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the reconnect plug-in.
 *
 * @author Damian Minkov
 */
public class ReconnectPluginActivator
    extends DependentActivator
    implements ServiceListener,
               NetworkConfigurationChangeListener
{
    /**
     * Logger of this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReconnectPluginActivator.class);

    /**
     * The current BundleContext.
     */
    private static BundleContext bundleContext = null;

    /**
     * The ui service.
     */
    private static UIService uiService;

    /**
     * The resources service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * Notification service.
     */
    private static NotificationService notificationService;

    /**
     * Network address manager service will inform us for changes in
     * network configuration.
     */
    private NetworkAddressManagerService networkAddressManagerService = null;

    /**
     * Holds every protocol provider wrapper which can be reconnected and
     * a list of the available and up interfaces when the provider was
     * registered. When a provider is unregistered by user request it is removed
     * from this collection. Or when the provider service is removed from OSGi.
     * Or if provider failed registering and there were yet no successful
     * connections of this provider.
     * Providers REMOVED:
     *  - When provider is removed from osgi
     *  - When a provider is UNREGISTERED by user request
     * Providers ADDED:
     *  - When a provider is REGISTERED
     */
    private static final Map<PPReconnectWrapper, List<String>>
        reconnectEnabledProviders = new HashMap<>();

    /**
     * A list of currently connected interfaces. If empty network is down.
     */
    private static final Set<String> connectedInterfaces = new HashSet<>();

    /**
     * Start of the delay interval when starting a reconnect.
     */
    static final int RECONNECT_DELAY_MIN = 2; // sec

    /**
     * The end of the interval for the initial reconnect.
     */
    static final int RECONNECT_DELAY_MAX = 4; // sec

    /**
     * Max value for growing the reconnect delay, all subsequent reconnects
     * use this maximum delay.
     */
    static final int MAX_RECONNECT_DELAY = 300; // sec

    /**
     * Network notifications event type.
     */
    public static final String NETWORK_NOTIFICATIONS = "NetworkNotifications";

    /**
     * Whether the provider connected at least once, which means settings are
     * correct, otherwise it maybe server address wrong or username/password
     * and there is no point of reconnecting.
     */
    public static final String ATLEAST_ONE_CONNECTION_PROP =
        "net.java.sip.communicator.plugin.reconnectplugin." +
            "ATLEAST_ONE_SUCCESSFUL_CONNECTION";

    /**
     * Timer used to filter out too frequent "network down" notifications
     * on Android.
     */
    private Timer delayedNetworkDown;

    /**
     * Delay used for filtering out "network down" notifications.
     */
    private static final long NETWORK_DOWN_THRESHOLD = 30 * 1000;

    public ReconnectPluginActivator()
    {
        super(
            UIService.class,
            ResourceManagementService.class,
            ConfigurationService.class,
            NotificationService.class,
            NetworkAddressManagerService.class
        );
    }

    /**
     * Starts this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this bundle is
     * to be started
     */
    @Override
    public void startWithServices(BundleContext bundleContext)
    {
        ReconnectPluginActivator.bundleContext = bundleContext;
        bundleContext.addServiceListener(this);

        this.networkAddressManagerService
            = ServiceUtils.getService(
                    bundleContext,
                    NetworkAddressManagerService.class);
        this.networkAddressManagerService
            .addNetworkConfigurationChangeListener(this);

        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService) bundleContext
                        .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
    }

    /**
     * Returns resource service.
     * @return the resource service.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext
                                        .getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if (notificationService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(serviceReference);

            notificationService.registerDefaultNotificationForEvent(
                NETWORK_NOTIFICATIONS,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);
        }

        return notificationService;
    }

    /**
     * When new protocol provider is registered we add needed listeners.
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference serviceRef = serviceEvent.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know we are shutting down
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object sService = bundleContext.getService(serviceRef);

        if(sService instanceof NetworkAddressManagerService)
        {
            switch (serviceEvent.getType())
            {
                case ServiceEvent.REGISTERED:
                    if(this.networkAddressManagerService != null)
                        break;

                    this.networkAddressManagerService =
                        (NetworkAddressManagerService)sService;
                    networkAddressManagerService
                        .addNetworkConfigurationChangeListener(this);
                    break;
                case ServiceEvent.UNREGISTERING:
                    ((NetworkAddressManagerService)sService)
                        .removeNetworkConfigurationChangeListener(this);
                    break;
            }

            return;
        }

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        switch (serviceEvent.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService)sService);
            break;

        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved( (ProtocolProviderService) sService);
            break;
        }
    }

    /**
     * Add listeners to newly registered protocols.
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        if (logger.isTraceEnabled())
            logger.trace("New protocol provider is coming " + provider);

        // we just create the instance, if the instance successfully registers
        // will use addReconnectEnabledProvider to add itself to those we will
        // handle
        new PPReconnectWrapper(provider);
    }

    /**
     * Stop listening for events as the provider is removed.
     * Providers are removed this way only when there are modified
     * in the configuration. So as the provider is modified we will erase
     * every instance we got.
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        if (logger.isTraceEnabled())
            logger.trace("Provider modified forget every instance of it "
                + provider);

        if(hasAtLeastOneSuccessfulConnection(provider))
        {
            setAtLeastOneSuccessfulConnection(provider, false);
        }

        synchronized(reconnectEnabledProviders)
        {
            Iterator<PPReconnectWrapper> iter
                = reconnectEnabledProviders.keySet().iterator();
            while(iter.hasNext())
            {
                PPReconnectWrapper wrapper = iter.next();
                if (wrapper.getProvider().equals(provider))
                {
                    iter.remove();
                    wrapper.clear();
                }
            }
        }
    }

    /**
     * Adds a wrapper to the list of the wrappers we will handle reconnecting.
     * Marks one successful connection if needed.
     * @param wrapper the provider wrapper.
     */
    static void addReconnectEnabledProvider(PPReconnectWrapper wrapper)
    {
        ProtocolProviderService pp = wrapper.getProvider();

        synchronized(reconnectEnabledProviders)
        {
            if (!hasAtLeastOneSuccessfulConnection(pp))
            {
                setAtLeastOneSuccessfulConnection(pp, true);
            }

            reconnectEnabledProviders.put(
                wrapper, new ArrayList<>(connectedInterfaces));
        }
    }

    /**
     * Removes the wrapper from the list, will not reconnect it.
     * @param wrapper the wrapper to remove.
     */
    static void removeReconnectEnabledProviders(PPReconnectWrapper wrapper)
    {
        synchronized(reconnectEnabledProviders)
        {
            reconnectEnabledProviders.remove(wrapper);
        }
    }

    /**
     * Fired when a change has occurred in the computer network configuration.
     *
     * @param event the change event.
     */
    public synchronized void configurationChanged(ChangeEvent event)
    {
        if(event.getType() == ChangeEvent.IFACE_UP)
        {
            // no connection so one is up, lets connect
            if (!anyConnectedInterfaces())
            {
                onNetworkUp();

                List<PPReconnectWrapper> wrappers;
                synchronized (reconnectEnabledProviders)
                {
                    wrappers  = new LinkedList<>(
                        reconnectEnabledProviders.keySet());
                }

                wrappers.stream().forEach(PPReconnectWrapper::reconnect);
            }

            connectedInterfaces.add((String)event.getSource());
        }
        else if(event.getType() == ChangeEvent.IFACE_DOWN)
        {
            String ifaceName = (String)event.getSource();

            connectedInterfaces.remove(ifaceName);

            // one is down and at least one more is connected
            if (anyConnectedInterfaces())
            {
                // lets reconnect all that were connected when this one was
                // available, cause they maybe using it
                List<PPReconnectWrapper> wrappers;
                synchronized (reconnectEnabledProviders)
                {
                    wrappers = reconnectEnabledProviders.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(ifaceName))
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toList());
                }

                wrappers.stream().forEach(PPReconnectWrapper::reconnect);
            }
            else
            {
                // we must disconnect every pp that is trying to reconnect
                // and they will reconnect when network is back
                List<PPReconnectWrapper> wrappers;
                synchronized (reconnectEnabledProviders)
                {
                    wrappers  = new LinkedList<>(
                        reconnectEnabledProviders.keySet());
                }

                wrappers.stream().forEach(PPReconnectWrapper::unregister);

                onNetworkDown();
            }
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Event received " + event
                    + " src=" + event.getSource());
            traceCurrentPPState();
        }
    }

    /**
     * Whether we have any connected interface.
     * @return <tt>true</tt> when there is at least one connected interface at
     * the moment.
     */
    static boolean anyConnectedInterfaces()
    {
        return !connectedInterfaces.isEmpty();
    }

    /**
     * Trace prints of current status of the lists with protocol providers,
     * that are currently in interest of the reconnect plugin.
     */
    static void traceCurrentPPState()
    {
        logger.trace("connectedInterfaces: " + connectedInterfaces);
        synchronized(reconnectEnabledProviders)
        {
            logger.trace("reconnectEnabledProviders: " + reconnectEnabledProviders.keySet());
        }
        logger.trace("----");
    }

    /**
     * Sends network notification.
     * @param title the title.
     * @param i18nKey the resource key of the notification.
     * @param params and parameters in any.
     * @param tag extra notification tag object
     */
    private static void notify(String title, String i18nKey, String[] params,
                        Object tag)
    {
        Map<String,Object> extras = new HashMap<String,Object>();

        extras.put(
                NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA,
                tag);

        getNotificationService().fireNotification(
                    NETWORK_NOTIFICATIONS,
                    title,
                    getResources().getI18NString(i18nKey, params),
                    null,
                    extras);
    }

    /**
     * Notifies for connection failed or failed to non existing user.
     * @param evt the event to notify for.
     */
    static void notifyConnectionFailed(RegistrationStateChangeEvent evt)
    {
        if (!evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            return;
        }

        ProtocolProviderService pp = (ProtocolProviderService)evt.getSource();

        if (evt.getReasonCode() ==
            RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID)
        {
            notify(
                getResources().getI18NString("service.gui.ERROR"),
                "service.gui.NON_EXISTING_USER_ID",
                new String[]{pp.getAccountID().getService()},
                pp.getAccountID());
        }
        else
        {
            notify(
                getResources().getI18NString("service.gui.ERROR"),
                "plugin.reconnectplugin.CONNECTION_FAILED_MSG",
                new String[]
                    {   pp.getAccountID().getUserID(),
                        pp.getAccountID().getService() },
                pp.getAccountID());
        }
    }

    /**
     * Check does the supplied protocol has the property set for at least
     * one successful connection.
     * @param pp the protocol provider
     * @return true if property exists.
     */
    static boolean hasAtLeastOneSuccessfulConnection(
        ProtocolProviderService pp)
    {
       String value = (String)getConfigurationService().getProperty(
           ATLEAST_ONE_CONNECTION_PROP + "."
           + pp.getAccountID().getAccountUniqueID());

       if(value == null || !value.equals(Boolean.TRUE.toString()))
           return false;
       else
           return true;
    }

    /**
     * Changes the property about at least one successful connection.
     * @param pp the protocol provider
     * @param value the new value true or false.
     */
    private static void setAtLeastOneSuccessfulConnection(
        ProtocolProviderService pp, boolean value)
    {
       getConfigurationService().setProperty(
           ATLEAST_ONE_CONNECTION_PROP + "."
            + pp.getAccountID().getAccountUniqueID(),
           Boolean.valueOf(value).toString());
    }

    /**
     * Called when first connected interface is added to
     * {@link #connectedInterfaces} list.
     */
    private void onNetworkUp()
    {
        if(delayedNetworkDown != null)
        {
            delayedNetworkDown.cancel();
            delayedNetworkDown = null;
        }
    }

    /**
     * Called when first there are no more connected interface present in
     * {@link #connectedInterfaces} list.
     */
    private void onNetworkDown()
    {
        if(!org.jitsi.util.OSUtils.IS_ANDROID)
        {
            notifyNetworkDown();
        }
        else
        {
            // Android never keeps two active connection at the same time
            // and it may take some time to attach next connection
            // even if it was already enabled by user
            if(delayedNetworkDown == null)
            {
                delayedNetworkDown = new Timer();
                delayedNetworkDown.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        notifyNetworkDown();
                    }
                }, NETWORK_DOWN_THRESHOLD);
            }
        }
    }

    /**
     * Posts "network is down" notification.
     */
    private void notifyNetworkDown()
    {
        if (logger.isTraceEnabled())
            logger.trace("Network is down!");
        notify("", "plugin.reconnectplugin.NETWORK_DOWN",
               new String[0], this);
    }
}
