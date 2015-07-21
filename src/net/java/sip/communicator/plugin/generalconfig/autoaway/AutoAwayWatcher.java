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
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Listens for idle events from SystemActivityNotifications Service.
 *
 * @author Damian Minkov
 */
public class AutoAwayWatcher
    implements ServiceListener,
               RegistrationStateChangeListener
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(AutoAwayWatcher.class);

    /**
     * The states of providers before going to away.
     */
    private final Map<ProtocolProviderService, PresenceStatus> lastStates
        = new HashMap<ProtocolProviderService, PresenceStatus>();

    /**
     * Listens for idle events.
     */
    private IdleListener idleListener = null;

    /**
     * Creates AutoAway handler.
     */
    public AutoAwayWatcher()
    {
        if (Preferences.isEnabled())
        {
            start();
        }

        Preferences.addEnableChangeListener(
                new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent evt)
                    {
                        if(Boolean.parseBoolean((String) evt.getNewValue()))
                            start();
                        else
                            stopInner();
                    }
                }
        );

        // listens for changes in configured value.
        Preferences.addTimerChangeListener(
            new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    stopInner();
                    start();
                }
            }
        );
    }

    /**
     * Starts and add needed listeners.
     */
    private void start()
    {
        if(idleListener == null)
        {

            idleListener = new IdleListener();

            SystemActivityNotificationsService
                systemActivityNotificationsService
                    =  getSystemActivityNotificationsService();

            systemActivityNotificationsService.addIdleSystemChangeListener(
                    Preferences.getTimer() * 60 * 1000,
                    idleListener);
            systemActivityNotificationsService
                .addSystemActivityChangeListener(idleListener);

            startListeningForNewProviders();
        }
    }

    /**
     * Start listening for new providers and their registration states.
     */
    private void startListeningForNewProviders()
    {
        // listen for new providers
        GeneralConfigPluginActivator.bundleContext.addServiceListener(this);

        // lets check current providers
        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = GeneralConfigPluginActivator.bundleContext
                .getServiceReferences(ProtocolProviderService.class.getName(),
                    null);
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
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService)
                    GeneralConfigPluginActivator.bundleContext
                        .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Stop listening for new providers and their registration states.
     */
    private void stopListeningForNewProviders()
    {
        // stop listen for new providers
        GeneralConfigPluginActivator.bundleContext.removeServiceListener(this);

        // lets check current providers and remove registration state listener
        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = GeneralConfigPluginActivator.bundleContext
                .getServiceReferences(ProtocolProviderService.class.getName(),
                    null);
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
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService)
                    GeneralConfigPluginActivator.bundleContext
                        .getService(protocolProviderRefs[i]);

                this.handleProviderRemoved(provider);
            }
        }
    }

    /**
     * Stops and removes the listeners.
     */
    public void stop()
    {
        GeneralConfigPluginActivator.bundleContext.removeServiceListener(this);
        stopInner();
    }

    /**
     * Stops and removes the listeners.
     */
    private void stopInner()
    {
        if(idleListener != null)
        {
            SystemActivityNotificationsService
                systemActivityNotificationsService
                    =  getSystemActivityNotificationsService();

            systemActivityNotificationsService.removeIdleSystemChangeListener(
                    idleListener);
            systemActivityNotificationsService
                .removeSystemActivityChangeListener(idleListener);

            stopListeningForNewProviders();

            idleListener = null;
        }
    }

    /**
     * Change protocol to away saving status so it can be set again when
     * out of idle state.
     */
    private void changeProtocolsToAway()
    {
        for (ProtocolProviderService protocolProvider
                : GeneralConfigPluginActivator.getProtocolProviders())
        {
            OperationSetPresence presence
                = protocolProvider.getOperationSet(
                        OperationSetPresence.class);

            if(presence == null)
                continue;

            PresenceStatus status = presence.getPresenceStatus();

            if (status.getStatus() < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                // already (manually) set to away or lower
                continue;
            }

            PresenceStatus newStatus
                = StatusUpdateThread.findAwayStatus(presence);

            try
            {
                if (newStatus != null && !status.equals(newStatus))
                {
                    addProviderToLastStates(protocolProvider, status);

                    presence.publishPresenceStatus(
                            newStatus,
                            newStatus.getStatusName());
                }
            }
            catch (IllegalArgumentException e)
            {
            }
            catch (IllegalStateException e)
            {
            }
            catch (OperationFailedException e)
            {
            }
        }
    }

    /**
     * Back to status which was already saved before going to idle.
     */
    private void changeProtocolsToPreviousState()
    {
        for (ProtocolProviderService protocolProvider
                : GeneralConfigPluginActivator.getProtocolProviders())
        {
            PresenceStatus lastState = lastStates.get(protocolProvider);

            if (lastState != null)
            {
                OperationSetPresence presence
                    = protocolProvider.getOperationSet(
                            OperationSetPresence.class);
                try
                {
                    presence.publishPresenceStatus(lastState, "");
                }
                catch (IllegalArgumentException e)
                {
                }
                catch (IllegalStateException e)
                {
                }
                catch (OperationFailedException e)
                {
                }
                removeProviderFromLastStates(protocolProvider);
            }
        }
    }

    /**
     * The SystemActivityNotifications Service.
     * @return the SystemActivityNotifications Service.
     */
    private SystemActivityNotificationsService
        getSystemActivityNotificationsService()
    {
        return
            ServiceUtils.getService(
                    GeneralConfigPluginActivator.bundleContext,
                    SystemActivityNotificationsService.class);
    }

    /**
     * When new protocol provider is registered we add our
     * registration change listener.
     * If unregistered remove reference to the provider and the
     * registration change listener.
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object service
            = GeneralConfigPluginActivator.bundleContext.getService(
                    serviceEvent.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (service instanceof ProtocolProviderService)
        {
            int serviceEventType = serviceEvent.getType();

            if (serviceEventType == ServiceEvent.REGISTERED)
                handleProviderAdded((ProtocolProviderService) service);
            else if (serviceEventType == ServiceEvent.UNREGISTERING)
                handleProviderRemoved((ProtocolProviderService) service);
        }
    }

    /**
     * Used to set registration state change listener.
     *
     * @param provider ProtocolProviderService
     */
    private synchronized void handleProviderAdded(
            ProtocolProviderService provider)
    {
        provider.addRegistrationStateChangeListener(this);
    }

    /**
     * Removes the registration change listener.
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        provider.removeRegistrationStateChangeListener(this);
    }

    /**
     * Remove provider from list with last statuses.
     * If this is the last provider stop listening for idle events.
     * @param provider
     */
    private synchronized void removeProviderFromLastStates(
        ProtocolProviderService provider)
    {
        lastStates.remove(provider);
    }

    /**
     * Remember provider's last status, normally before setting it to away.
     * If needed start listening for idle events.
     * @param provider the provider.
     * @param status the status to save.
     */
    private synchronized void addProviderToLastStates(
            ProtocolProviderService provider,
            PresenceStatus status)
    {
        if(lastStates.size() == 0)
            start();

        lastStates.put(provider, status);
    }

    /**
     * Listens for provider states.
     * @param evt
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
         if(evt.getSource() instanceof ProtocolProviderService)
         {
            if(evt.getNewState().equals(RegistrationState.UNREGISTERED)
               || evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
            {
                removeProviderFromLastStates(evt.getProvider());
            }
            else if(evt.getNewState().equals(
                                RegistrationState.REGISTERED))
            {
                // we have at least one provider, so lets start listening
                if(lastStates.size() == 0)
                {
                    start();
                }
                else
                {
                    // or check are we away
                    if(getSystemActivityNotificationsService()
                        .getTimeSinceLastInput()
                            > Preferences.getTimer()*60*1000)
                    {
                        // we are away, so update the newly registered provider
                        // do it in new thread to give the provider
                        // time dispatch his status
                        new Thread(new Runnable()
                        {
                            public void run()
                            {
                                try{
                                Thread.sleep(1000);
                                }
                                catch(Throwable t){}

                                changeProtocolsToAway();
                            }
                        }).start();
                    }
                }
            }
         }
    }

    /**
     * Listener waiting for idle state change.
     */
    private class IdleListener
        implements SystemActivityChangeListener
    {
        /**
         * Listens for activities and set corresponding statuses.
         *
         * @param event the <tt>NotificationActionTypeEvent</tt>, which is
         */
        public void activityChanged(SystemActivityEvent event)
        {
            switch(event.getEventID())
            {
                case SystemActivityEvent.EVENT_DISPLAY_SLEEP:
                case SystemActivityEvent.EVENT_SCREEN_LOCKED:
                case SystemActivityEvent.EVENT_SCREENSAVER_START:
                case SystemActivityEvent.EVENT_SYSTEM_IDLE:
                    changeProtocolsToAway();
                    break;
                case SystemActivityEvent.EVENT_DISPLAY_WAKE:
                case SystemActivityEvent.EVENT_SCREEN_UNLOCKED:
                case SystemActivityEvent.EVENT_SCREENSAVER_STOP:
                case SystemActivityEvent.EVENT_SYSTEM_IDLE_END:
                    changeProtocolsToPreviousState();
                    break;
            }
        }
    }
}
