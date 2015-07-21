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
package net.java.sip.communicator.plugin.reconnectplugin;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the reconnect plug-in.
 *
 * @author Damian Minkov
 */
public class ReconnectPluginActivator
    implements BundleActivator,
               ServiceListener,
               NetworkConfigurationChangeListener,
               RegistrationStateChangeListener
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ReconnectPluginActivator.class);

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
     * Holds every protocol provider which is can be reconnected and
     * a list of the available and up interfaces when the provider was
     * registered. When a provider is unregistered it is removed
     * from this collection.
     * Providers REMOVED:
     *  - When provider is removed from osgi
     *  - When a provider is UNREGISTERED
     * Providers ADDED:
     *  - When a provider is REGISTERED
     */
    private final Map<ProtocolProviderService, List<String>>
        autoReconnEnabledProviders = new HashMap<ProtocolProviderService, List<String>>();

    /**
     * Holds the currently reconnecting providers and their reconnect tasks.
     * When they get connected they are removed from this collection.
     * Providers REMOVED:
     *  - When provider removed from osgi.
     *  - When interface is UP, we remove providers and schedule reconnect
     *  for them
     *  - When interface is DOWN, we remove all providers and schedule reconnect
     *  - When last interface is DOWN, we remove all providers and
     *  unregister them
     *  - On connection failed with no interface connected
     *  - Provider is Registered
     *  - Provider is Unregistered and is missing in unregistered providers list
     *  - After provider is unregistered just before reconnecting, and there
     *  are no connected interfaces
     * Providers ADDED:
     *  - Before unregister (in new thread) when scheduling a reconnect task
     *  - After provider is unregistered just before reconnecting
     */
    private final Map<ProtocolProviderService, ReconnectTask>
        currentlyReconnecting
            = new HashMap<ProtocolProviderService, ReconnectTask>();

    /**
     * If network is down we save here the providers which need
     * to be reconnected.
     * Providers REMOVED:
     * - When provider removed from osgi.
     * - Remove all providers when interface is up and we will reconnect them
     * Providers ADDED:
     * - Interface is down, and there are still active interfaces, add all
     * auto reconnect enabled and all currently reconnecting
     * - Provider in connection failed and there are no connected interfaces
     * - Provider is unregistered or connection failed and there are no
     * connected interfaces.
     */
    private Set<ProtocolProviderService> needsReconnection =
        new HashSet<ProtocolProviderService>();

    /**
     * A list of providers on which we have called unregister. This is a
     * way to differ our unregister calls from calls coming from user, wanting
     * to stop all reconnects.
     * Providers REMOVED:
     * - Provider is Connection failed.
     * - Provider is registered/unregistered
     * Providers ADDED:
     * - Provider is about to be unregistered
     */
    private Set<ProtocolProviderService> unregisteringProviders
        = new HashSet<ProtocolProviderService>();

    /**
     * A list of currently connected interfaces. If empty network is down.
     */
    private Set<String> connectedInterfaces = new HashSet<String>();

    /**
     * Timer for scheduling all reconnect operations.
     */
    private Timer timer = null;

    /**
     * Start of the delay interval when starting a reconnect.
     */
    private static final int RECONNECT_DELAY_MIN = 2; // sec

    /**
     * The end of the interval for the initial reconnect.
     */
    private static final int RECONNECT_DELAY_MAX = 4; // sec

    /**
     * Max value for growing the reconnect delay, all subsequent reconnects
     * use this maximum delay.
     */
    private static final int MAX_RECONNECT_DELAY = 300; // sec

    /**
     * Network notifications event type.
     */
    public static final String NETWORK_NOTIFICATIONS = "NetworkNotifications";

    /**
     *
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

    /**
     * Starts this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this bundle is
     * to be started
     * @throws Exception if anything goes wrong while starting this bundle
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            logger.logEntry();
            ReconnectPluginActivator.bundleContext = bundleContext;
        }
        finally
        {
            logger.logExit();
        }

        bundleContext.addServiceListener(this);

        if(timer == null)
            timer = new Timer("Reconnect timer", true);

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
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which this bundle is
     * to be stopped
     * @throws Exception if anything goes wrong while stopping this bundle
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if(timer != null)
        {
            timer.cancel();
            timer = null;
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
            logger.trace("New protocol provider is comming "
            + provider.getProtocolName());

        provider.addRegistrationStateChangeListener(this);
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
            logger.trace("Provider modified forget every instance of it");

        if(hasAtLeastOneSuccessfulConnection(provider))
        {
            setAtLeastOneSuccessfulConnection(provider, false);
        }

        provider.removeRegistrationStateChangeListener(this);

        autoReconnEnabledProviders.remove(provider);
        needsReconnection.remove(provider);

        if(currentlyReconnecting.containsKey(provider))
        {
            currentlyReconnecting.remove(provider).cancel();
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
            if(connectedInterfaces.isEmpty())
            {
                onNetworkUp();

                Iterator<ProtocolProviderService> iter =
                    needsReconnection.iterator();
                while (iter.hasNext())
                {
                    ProtocolProviderService pp = iter.next();
                    if(currentlyReconnecting.containsKey(pp))
                    {
                        // now lets cancel it and schedule it again
                        // so it will use this iface
                        currentlyReconnecting.remove(pp).cancel();
                    }

                    reconnect(pp);
                }

                needsReconnection.clear();
            }

            connectedInterfaces.add((String)event.getSource());
        }
        else if(event.getType() == ChangeEvent.IFACE_DOWN)
        {
            String ifaceName = (String)event.getSource();

            connectedInterfaces.remove(ifaceName);

            // one is down and at least one more is connected
            if(connectedInterfaces.size() > 0)
            {
                // lets reconnect all that was connected when this one was
                // available, cause they maybe using it
                Iterator<Map.Entry<ProtocolProviderService, List<String>>> iter =
                    autoReconnEnabledProviders.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry<ProtocolProviderService, List<String>> entry
                        = iter.next();

                    if(entry.getValue().contains(ifaceName))
                    {
                        ProtocolProviderService pp = entry.getKey();
                        // hum someone is reconnecting, lets cancel and
                        // schedule it again
                        if(currentlyReconnecting.containsKey(pp))
                        {
                            currentlyReconnecting.remove(pp).cancel();
                        }

                        reconnect(pp);
                    }
                }
            }
            else
            {
                // we must disconnect every pp and put all to be need of reconnecting
                needsReconnection.addAll(autoReconnEnabledProviders.keySet());
                // there can by and some that are currently going to reconnect
                // must take care of them too, cause there is no net and they won't succeed
                needsReconnection.addAll(currentlyReconnecting.keySet());

                Iterator<ProtocolProviderService> iter =
                    needsReconnection.iterator();
                while (iter.hasNext())
                {
                    ProtocolProviderService pp = iter.next();

                    // if provider is scheduled for reconnect,
                    // cancel it there is no network
                    if(currentlyReconnecting.containsKey(pp))
                    {
                        currentlyReconnecting.remove(pp).cancel();
                    }

                    // don't reconnect just unregister if needed.
                    unregister(pp, false, null, null);
                }

                connectedInterfaces.clear();

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
     * Unregisters the ProtocolProvider. Make sure to do it in separate thread
     * so we don't block other processing.
     * @param pp the protocol provider to unregister.
     * @param reconnect if the protocol provider does not need unregistering
     *      shall we trigger reconnect. Its true when call called from
     *      reconnect.
     * @param listener the listener used in reconnect method.
     * @param task the task to use for reconnection.
     */
    private void unregister(final ProtocolProviderService pp,
                            final boolean reconnect,
                            final RegistrationStateChangeListener listener,
                            final ReconnectTask task)
    {
        unregisteringProviders.add(pp);

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    // getRegistrationState() for some protocols(icq) can trigger
                    // registrationStateChanged so make checks here
                    // to prevent synchronize in registrationStateChanged
                    // and deadlock
                    if(pp.getRegistrationState().equals(
                            RegistrationState.UNREGISTERING)
                       || pp.getRegistrationState().equals(
                            RegistrationState.UNREGISTERED)
                       || pp.getRegistrationState().equals(
                            RegistrationState.CONNECTION_FAILED))
                    {
                        if(reconnect)
                        {
                            if(listener != null)
                                pp.removeRegistrationStateChangeListener(
                                        listener);

                            if(timer == null || task == null)
                                 return;

                            // cancel any existing task before overriding it
                            if(currentlyReconnecting.containsKey(pp))
                                currentlyReconnecting.remove(pp).cancel();

                            currentlyReconnecting.put(pp, task);

                            if (logger.isInfoEnabled())
                                logger.info("Reconnect " +
                                    pp.getAccountID().getDisplayName()
                                    + " after " + task.delay + " ms.");

                            timer.schedule(task, task.delay);
                        }
                        return;
                    }

                    pp.unregister();
                }
                catch(Throwable t)
                {
                    logger.error("Error unregistering pp:" + pp, t);
                }
            }
        }).start();
    }

    /**
     * Trace prints of current status of the lists with protocol providers,
     * that are currently in interest of the reconnect plugin.
     */
    private void traceCurrentPPState()
    {
        logger.trace("connectedInterfaces: " + connectedInterfaces);
        logger.trace("autoReconnEnabledProviders: "
            + autoReconnEnabledProviders.keySet());
        logger.trace("currentlyReconnecting: "
            + currentlyReconnecting.keySet());
        logger.trace("needsReconnection: " + needsReconnection);
        logger.trace("unregisteringProviders: " + unregisteringProviders);
        logger.trace("----");
    }

    /**
     * Sends network notification.
     * @param title the title.
     * @param i18nKey the resource key of the notification.
     * @param params and parameters in any.
     * @param tag extra notification tag object
     */
    private void notify(String title, String i18nKey, String[] params,
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
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     *
     * @param evt the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        // we don't care about protocol providers that don't support
        // reconnection and we are interested only in few state changes
        if(!(evt.getSource() instanceof ProtocolProviderService)
            ||
            !(evt.getNewState().equals(RegistrationState.REGISTERED)
              || evt.getNewState().equals(RegistrationState.UNREGISTERED)
              || evt.getNewState().equals(RegistrationState.CONNECTION_FAILED)))
            return;

        synchronized(this) {
        try
        {
            ProtocolProviderService pp = (ProtocolProviderService)evt.getSource();

            boolean isServerReturnedErroneousInputEvent =
                evt.getNewState().equals(RegistrationState.CONNECTION_FAILED)
                && evt.getReasonCode() == RegistrationStateChangeEvent
                    .REASON_SERVER_RETURNED_ERRONEOUS_INPUT;

            if(evt.getNewState().equals(RegistrationState.CONNECTION_FAILED)
                && !isServerReturnedErroneousInputEvent)
            {
                if(!hasAtLeastOneSuccessfulConnection(pp))
                {
                    // ignore providers which haven't registered successfully
                    // till now, they maybe misconfigured
                    //String notifyMsg;

                    if(evt.getReasonCode() ==
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

                    return;
                }

                // if this pp is already in needsReconnection, it means
                // we got conn failed cause the pp has tried to unregister
                // with sending network packet
                // but this unregister is scheduled from us so skip
                if(needsReconnection.contains(pp))
                    return;

                if(connectedInterfaces.isEmpty())
                {
                    needsReconnection.add(pp);

                    if(currentlyReconnecting.containsKey(pp))
                        currentlyReconnecting.remove(pp).cancel();
                }
                else
                {
                    // network is up but something happen and cannot reconnect
                    // strange lets try again after some time
                    reconnect(pp);
                }

                // unregister can finish and with connection failed,
                // the protocol is unable to unregister
                unregisteringProviders.remove(pp);

                if(logger.isTraceEnabled())
                {
                    logger.trace("Got Connection Failed for " + pp,
                        new Exception("tracing exception"));
                    traceCurrentPPState();
                }
            }
            else if(evt.getNewState().equals(RegistrationState.REGISTERED))
            {
                if(!hasAtLeastOneSuccessfulConnection(pp))
                {
                    setAtLeastOneSuccessfulConnection(pp, true);
                }

                autoReconnEnabledProviders.put(
                    pp,
                    new ArrayList<String>(connectedInterfaces));

                if(currentlyReconnecting.containsKey(pp))
                    currentlyReconnecting.remove(pp).cancel();

                unregisteringProviders.remove(pp);

                if(logger.isTraceEnabled())
                {
                    logger.trace("Got Registered for " + pp);
                    traceCurrentPPState();
                }
            }
            else if(evt.getNewState().equals(RegistrationState.UNREGISTERED)
                    || isServerReturnedErroneousInputEvent)
            {
                // Removes from list of autoreconnect only if the unregister
                // event is by user request
                if(evt.isUserRequest()
                    || isServerReturnedErroneousInputEvent)
                    autoReconnEnabledProviders.remove(pp);

                if(!unregisteringProviders.contains(pp)
                    && currentlyReconnecting.containsKey(pp))
                {
                    currentlyReconnecting.remove(pp).cancel();
                }
                unregisteringProviders.remove(pp);

                if(logger.isTraceEnabled())
                {
                    logger.trace("Got Unregistered for " + pp);

                    if(!currentlyReconnecting.containsKey(pp)
                        && !needsReconnection.contains(pp)
                        && logger.isTraceEnabled())
                    {
                        // provider is not present in any collection
                        // it will be no longer reconnected, maybe user request
                        // to unregister lets trace check
                        logger.trace(
                            "Provider is unregistered and will not " +
                            "be reconnected (maybe on user request): " + pp
                            + " / reason:" + evt.getReason()
                            + " / reasonCode:" + evt.getReasonCode()
                            + " / oldState:" + evt.getOldState(),
                            new Exception("Trace exception."));
                    }
                    traceCurrentPPState();
                }
            }
        }
        catch(Throwable ex)
        {
            logger.error("Error dispatching protocol registration change", ex);
        }
        }
    }

    /**
     * Method to schedule a reconnect for a protocol provider.
     * @param pp the provider.
     */
    private void reconnect(final ProtocolProviderService pp)
    {
        long delay;

        if(currentlyReconnecting.containsKey(pp))
        {
            delay = currentlyReconnecting.get(pp).delay;

            // we never stop trying
            //if(delay == MAX_RECONNECT_DELAY*1000)
            //    return;

            delay = Math.min(delay * 2, MAX_RECONNECT_DELAY*1000);
        }
        else
        {
            delay = (long)(RECONNECT_DELAY_MIN
                + Math.random() * RECONNECT_DELAY_MAX)*1000;
        }

        final ReconnectTask task = new ReconnectTask(pp);
        task.delay = delay;

        // start registering after the pp has unregistered
        RegistrationStateChangeListener listener =
            new RegistrationStateChangeListener()
        {
            public void registrationStateChanged(RegistrationStateChangeEvent evt)
            {
                 if(evt.getSource() instanceof ProtocolProviderService)
                 {
                    if(evt.getNewState().equals(
                                            RegistrationState.UNREGISTERED)
                        || evt.getNewState().equals(
                                        RegistrationState.CONNECTION_FAILED))
                     {
                         synchronized(this)
                         {
                             pp.removeRegistrationStateChangeListener(this);

                             if(timer == null)
                                 return;

                             if(connectedInterfaces.size() == 0)
                             {
                                 // well there is no network we just need
                                 // this provider in needs reconnection when
                                 // there is one
                                 // means we started unregistering while
                                 // network was going down and meanwhile there
                                 // were no connected interface, this happens
                                 // when we have more than one connected
                                 // interface and we got 2 events for down iface
                                 needsReconnection.add(pp);

                                 if(currentlyReconnecting.containsKey(pp))
                                    currentlyReconnecting.remove(pp).cancel();

                                 return;
                             }

                             // cancel any existing task before overriding it
                             if(currentlyReconnecting.containsKey(pp))
                                 currentlyReconnecting.remove(pp).cancel();

                             currentlyReconnecting.put(pp, task);

                             if (logger.isInfoEnabled())
                                 logger.info("Reconnect " +
                                     pp.getAccountID().getDisplayName() +
                                     " after " + task.delay + " ms.");

                             timer.schedule(task, task.delay);
                         }
                     }
                     /*
                     this unregister one way or another, and will end
                     with unregister or connection failed
                     if we remove listener when unregister come
                     we will end up with unregistered provider without reconnect
                     else if(evt.getNewState().equals(
                                            RegistrationState.REGISTERED))
                     {
                         pp.removeRegistrationStateChangeListener(this);
                     }*/
                 }
            }
        };
        pp.addRegistrationStateChangeListener(listener);

        // as we will reconnect, lets unregister
        unregister(pp, true, listener, task);
    }

    /**
     * The task executed by the timer when time for reconnect comes.
     */
    private class ReconnectTask
        extends TimerTask
    {
        /**
         * The provider to reconnect.
         */
        private ProtocolProviderService provider;

        /**
         * The delay with which was this task scheduled.
         */
        private long delay;

        /**
         * The thread to execute this task.
         */
        private Thread thread = null;

        /**
         * Creates the task.
         *
         * @param provider the <tt>ProtocolProviderService</tt> to reconnect
         */
        public ReconnectTask(ProtocolProviderService provider)
        {
            this.provider = provider;
        }

        /**
         * Reconnects the provider.
         */
        @Override
        public void run()
        {
            if(thread == null || !Thread.currentThread().equals(thread))
            {
                thread = new Thread(this);
                thread.start();
            }
            else
            {
                try
                {
                    if (logger.isInfoEnabled())
                        logger.info("Start reconnecting "
                            + provider.getAccountID().getDisplayName());

                    provider.register(
                        getUIService().getDefaultSecurityAuthority(provider));
                } catch (OperationFailedException ex)
                {
                    logger.error("cannot re-register provider will keep going",
                                ex);
                }
            }
        }
    }

    /**
     * Check does the supplied protocol has the property set for at least
     * one successful connection.
     * @param pp the protocol provider
     * @return true if property exists.
     */
    private boolean hasAtLeastOneSuccessfulConnection(ProtocolProviderService pp)
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
    private void setAtLeastOneSuccessfulConnection(
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
