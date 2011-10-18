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
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;

import java.beans.*;
import java.util.*;

/**
 * Listens for idle events from SystemActivityNotifications Service.
 * @author Damian Minkov
 */
public class AutoAwayWatcher
    implements SystemActivityChangeListener
{
    /**
     * The states of providers before going to away.
     */
    private final Map<ProtocolProviderService, PresenceStatus> lastStates
        = new HashMap<ProtocolProviderService, PresenceStatus>();

    /**
     * Creates AutoAway handler.
     * @param configurationService the config service.
     */
    public AutoAwayWatcher(ConfigurationService configurationService)
    {
        // if enabled start
        if(configurationService.getBoolean(Preferences.ENABLE, false))
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
                            stop();
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
                        stop();
                        start();
                    }
                }
        );
    }

    /**
     * Starts and add needed listeners.
     */
    public void start()
    {
        getSystemActivityNotificationsService()
            .addIdleSystemChangeListener(
                StatusUpdateThread.getTimer()*60*1000, this);
        getSystemActivityNotificationsService()
            .addSystemActivityChangeListener(this);
    }

    /**
     * Stops and removes the listeners.
     */
    public void stop()
    {
        getSystemActivityNotificationsService()
            .removeIdleSystemChangeListener(this);
        getSystemActivityNotificationsService()
            .removeSystemActivityChangeListener(this);
    }

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

            lastStates.put(protocolProviderService, status);

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
            if (lastStates.get(protocolProviderService) != null)
            {
                PresenceStatus lastState
                    = lastStates.get(protocolProviderService);
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
                lastStates.remove(protocolProviderService);
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
}
