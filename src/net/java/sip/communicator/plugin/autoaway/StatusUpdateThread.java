/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.autoaway;

import java.awt.*;
import java.util.*;

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
    private Map<ProtocolProviderService, PresenceStatus> lastStates = new HashMap<ProtocolProviderService, PresenceStatus>();

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
                    Point currentPosition = null;

                    if (info != null)
                    {
                        currentPosition = info.getLocation();
                    } else
                    {
                        // mouse cannot be determined
                        currentPosition = new Point(0, 0);
                    }
                    if (!isNear(lastPosition, currentPosition))
                    {
                        // position has changed
                        // check, if a minor state has been automatically set
                        // and
                        // reset this state to the former state.
                        ProtocolProviderService[] pps = AutoAwayActivator
                                .getProtocolProviders();

                        for (ProtocolProviderService protocolProviderService : pps)
                        {
                            if (lastStates.get(protocolProviderService) != null)
                            {
                                PresenceStatus lastState = lastStates
                                        .get(protocolProviderService);
                                OperationSetPresence presence = (OperationSetPresence) protocolProviderService
                                        .getOperationSet(OperationSetPresence.class);
                                try
                                {
                                    presence.publishPresenceStatus(lastState,
                                            "");
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

                        ProtocolProviderService[] pps = AutoAwayActivator
                                .getProtocolProviders();

                        for (ProtocolProviderService protocolProviderService : pps)
                        {
                            OperationSetPresence presence = (OperationSetPresence) protocolProviderService
                                    .getOperationSet(OperationSetPresence.class);

                            PresenceStatus status = presence
                                    .getPresenceStatus();

                            if (status.getStatus() < PresenceStatus.AVAILABLE_THRESHOLD)
                            {
                                // already (manually) set to away or lower
                                continue;
                            }

                            lastStates.put(protocolProviderService, presence
                                    .getPresenceStatus());

                            PresenceStatus newStatus = findAwayStatus(presence);

                            try
                            {
                                if (newStatus != null)
                                {
                                    presence.publishPresenceStatus(newStatus,
                                            newStatus.getStatusName());
                                }
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
    private PresenceStatus findAwayStatus(OperationSetPresence presence)
    {
        Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

        PresenceStatus status = null;

        while (statusSet.hasNext())
        {
            PresenceStatus possibleState = statusSet.next();

            if (possibleState.getStatus() < PresenceStatus.AVAILABLE_THRESHOLD
                    && possibleState.getStatus() >= PresenceStatus.ONLINE_THRESHOLD)
            {
                if (status == null
                        || (Math.abs(possibleState.getStatus()
                                - AWAY_DEFAULT_STATUS) < Math.abs(status
                                .getStatus()
                                - AWAY_DEFAULT_STATUS)))
                {
                    status = possibleState;
                }
            }
        }
        return status;
    }

    private int getTimer()
    {
        ConfigurationService configService = AutoAwayActivator
                .getConfigService();

        String e = (String) configService.getProperty(Preferences.ENABLE);
        if (e == null)
        {
            return 0;
        }
        try
        {
            boolean enabled = Boolean.parseBoolean(e);
            if (!enabled)
            {
                return 0;
            }
        } catch (NumberFormatException ex)
        {
            return 0;
        }

        String t = configService.getString(Preferences.TIMER);
        int timer = 0;
        try
        {
            timer = Integer.parseInt(t);
        } catch (NumberFormatException ex)
        {
            return 0;
        }
        return timer;
    }

    public boolean isRunning()
    {
        return run;
    }

    private boolean isNear(Point p1, Point p2)
    {
        if (p1 == null)
        {
            return false;
        }
        if (p2 == null)
        {
            return false;
        }
        if (Math.abs(p1.x - p2.x) > 10)
        {
            return false;
        }
        if (Math.abs(p1.y - p2.y) > 10)
        {
            return false;
        }
        return true;

    }
}
