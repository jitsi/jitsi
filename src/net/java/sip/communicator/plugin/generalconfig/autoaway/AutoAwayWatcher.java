/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

import java.beans.*;
import java.util.*;

/**
 * Listens for idle events from SystemActivityNotifications Service.
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
     * @param configurationService the config service.
     */
    public AutoAwayWatcher(ConfigurationService configurationService)
    {
        // if enabled start
        String enabledDefault = GeneralConfigPluginActivator.getResources()
            .getSettingsString(Preferences.ENABLE);

        if(configurationService.getBoolean(Preferences.ENABLE,
                                           Boolean.valueOf(enabledDefault)))
            start();

        // listens for changes in configuration enable/disable
        configurationService.addPropertyChangeListener(
                Preferences.ENABLE,
                new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent evt)
                    {
                        if(Boolean.parseBoolean((String)evt.getNewValue()))
                            start();
                        else
                            stopInner();
                    }
                }
        );

        // listens for changes in configured value.
        configurationService.addPropertyChangeListener(
            Preferences.TIMER,
            new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    stopInner();
                    start();
                }
            }
        );

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
     * Starts and add needed listeners.
     */
    private void start()
    {
        if(idleListener == null)
        {
            idleListener = new IdleListener();

            getSystemActivityNotificationsService()
                .addIdleSystemChangeListener(
                    StatusUpdateThread.getTimer()*60*1000, idleListener);
            getSystemActivityNotificationsService()
                .addSystemActivityChangeListener(idleListener);
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
     * Stops and removes the listeners, without the global service listener.
     */
    private void stopInner()
    {
        if(idleListener == null)
            return;

        getSystemActivityNotificationsService()
            .removeIdleSystemChangeListener(idleListener);
        getSystemActivityNotificationsService()
            .removeSystemActivityChangeListener(idleListener);
        idleListener = null;
    }

    /**
     * Change protocol to away saving status so it can be set again when
     * out of idle state.
     */
    private void changeProtocolsToAway()
    {
        for (ProtocolProviderService protocolProviderService
                : GeneralConfigPluginActivator.getProtocolProviders())
        {
            OperationSetPresence presence
                = protocolProviderService
                    .getOperationSet(
                            OperationSetPresence.class);

            PresenceStatus status = presence
                    .getPresenceStatus();

            if (status.getStatus()
                    < PresenceStatus.AVAILABLE_THRESHOLD)
            {
                // already (manually) set to away or lower
                continue;
            }

            addProviderToLastStates(protocolProviderService, status);

            PresenceStatus newStatus =
                    StatusUpdateThread.findAwayStatus(presence);

            try
            {
                if (newStatus != null)
                    presence
                        .publishPresenceStatus(
                                newStatus,
                                newStatus.getStatusName());
            } catch (IllegalArgumentException e)
            {
            } catch (IllegalStateException e)
            {
            } catch (OperationFailedException e)
            {
            }
        }
    }

    /**
     * Back to status which was already saved before going to idle.
     */
    private void changeProtocolsToPreviousState()
    {
        for (ProtocolProviderService protocolProviderService
                : GeneralConfigPluginActivator.getProtocolProviders())
        {
            PresenceStatus lastState
                = lastStates.get(protocolProviderService);

            if (lastState != null)
            {
                OperationSetPresence presence
                    = protocolProviderService
                        .getOperationSet(
                            OperationSetPresence.class);
                try
                {
                    presence
                        .publishPresenceStatus(lastState, "");
                } catch (IllegalArgumentException e)
                {
                } catch (IllegalStateException e)
                {
                } catch (OperationFailedException e)
                {
                }
                removeProviderFromLastStates(protocolProviderService);
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
        return  ServiceUtils.getService(
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
        Object sService = GeneralConfigPluginActivator.bundleContext
            .getService(serviceEvent.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            this.handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved( (ProtocolProviderService) sService);
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

        if(lastStates.size() == 0)
        {
            stopInner();
        }
    }

    /**
     * Remember provider's last status, normally before setting it to away.
     * If needed start listening for idle events.
     * @param provider the provider.
     * @param status the status to save.
     */
    private synchronized void addProviderToLastStates(
        ProtocolProviderService provider, PresenceStatus status)
    {
        if(lastStates.size() == 0)
        {
            start();
        }

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
                            > StatusUpdateThread.getTimer()*60*1000)
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
