/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A Runnable, which permanently looks at the mouse position. If the mouse is
 * not moved, all accounts are set to "Away" or similar states.
 * 
 * @author Thomas Hofer
 */
public class StatusUpdateThread
    implements Runnable
{
    private boolean run = false;
    private Point lastPosition = null;
    private final Map<ProtocolProviderService, PresenceStatus> lastStates
        = new HashMap<ProtocolProviderService, PresenceStatus>();

    private static final int IDLE_TIMER = 3000;
    private static final int AWAY_DEFAULT_STATUS = 40;

    public void run()
    {
        run = true;
        int timer = 0;
        do
        {
            try
            {
                if (MouseInfo.getPointerInfo() != null)
                {
                    PointerInfo info = MouseInfo.getPointerInfo();
                    Point currentPosition
                        = (info != null) ? info.getLocation() : new Point(0, 0);

                    if (!isNear(lastPosition, currentPosition))
                    {
                        // position has changed
                        // check, if a minor state has been automatically set
                        // and
                        // reset this state to the former state.
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
                        timer = getTimer() * 1000 * 60;
                    } else
                    {
                        // position has not changed!
                        // get all protocols and set them to away
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

                            PresenceStatus newStatus = findAwayStatus(presence);

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

                        timer = IDLE_TIMER;
                    }
                    lastPosition = currentPosition;
                }
                Thread.sleep(timer);
            } catch (InterruptedException e)
            {
            }
        } while (run && timer > 0);
    }

    public void stop()
    {
        run = false;
    }

    /**
     * Finds the Away-Status of the protocols
     * 
     * @param presence
     * @return
     */
    static PresenceStatus findAwayStatus(OperationSetPresence presence)
    {
        Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();
        PresenceStatus status = null;

        while (statusSet.hasNext())
        {
            PresenceStatus possibleState = statusSet.next();
            int possibleStatus = possibleState.getStatus();

            if ((possibleStatus < PresenceStatus.AVAILABLE_THRESHOLD)
                    && (possibleStatus >= PresenceStatus.ONLINE_THRESHOLD))
            {
                if (status == null
                        || (Math.abs(possibleStatus - AWAY_DEFAULT_STATUS)
                                < Math.abs(
                                        status.getStatus()
                                            - AWAY_DEFAULT_STATUS)))
                {
                    status = possibleState;
                }
            }
        }
        return status;
    }

    static int getTimer()
    {
        ConfigurationService configService
            = GeneralConfigPluginActivator.getConfigurationService();

        return
            configService.getBoolean(Preferences.ENABLE, false)
                ? configService.getInt(Preferences.TIMER, 0)
                : 0;
    }

    public boolean isRunning()
    {
        return run;
    }

    private boolean isNear(Point p1, Point p2)
    {
        return
            (p1 != null)
                && (p2 != null)
                && (Math.abs(p1.x - p2.x) <= 10)
                && (Math.abs(p1.y - p2.y) <= 10);
    }
}
