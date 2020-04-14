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

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A <tt>Runnable</tt> which permanently looks at the mouse position. If the
 * mouse is not moved, all accounts are set to &quot;Away&quot; or similar
 * states.
 *
 * @author Thomas Hofer
 */
public class StatusUpdateThread
    implements Runnable
{
    private static final int AWAY_DEFAULT_STATUS = 40;

    private static final int IDLE_TIMER = 3000;

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

    private static boolean isNear(Point p1, Point p2)
    {
        return
            (p1 != null)
                && (p2 != null)
                && (Math.abs(p1.x - p2.x) <= 10)
                && (Math.abs(p1.y - p2.y) <= 10);
    }

    private Point lastPosition = null;

    private final Map<ProtocolProviderService, PresenceStatus> lastStates
        = new HashMap<ProtocolProviderService, PresenceStatus>();

    private boolean run = false;

    public boolean isRunning()
    {
        return run;
    }

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
                        // Position has changed check, if a minor state has been
                        // automatically set and reset this state to the former
                        // state.
                        for (ProtocolProviderService protocolProvider
                                : GeneralConfigPluginActivator.getProtocolProviders())
                        {
                            if (lastStates.get(protocolProvider) != null)
                            {
                                PresenceStatus lastState
                                    = lastStates.get(protocolProvider);
                                OperationSetPresence presence
                                    = protocolProvider.getOperationSet(
                                            OperationSetPresence.class);
                                try
                                {
                                    presence
                                        .publishPresenceStatus(lastState, "");
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
                                lastStates.remove(protocolProvider);
                            }
                        }
                        timer = Preferences.getTimer() * 1000 * 60;
                    }
                    else
                    {
                        // Position has not changed! Get all protocols and set
                        // them to away.
                        for (ProtocolProviderService protocolProvider
                                : GeneralConfigPluginActivator.getProtocolProviders())
                        {
                            OperationSetPresence presence
                                = protocolProvider.getOperationSet(
                                        OperationSetPresence.class);
                            PresenceStatus status
                                = presence.getPresenceStatus();

                            if (status.getStatus()
                                    < PresenceStatus.AVAILABLE_THRESHOLD)
                            {
                                // already (manually) set to away or lower
                                continue;
                            }

                            lastStates.put(protocolProvider, status);

                            PresenceStatus newStatus = findAwayStatus(presence);

                            try
                            {
                                if (newStatus != null)
                                {
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

                        timer = IDLE_TIMER;
                    }
                    lastPosition = currentPosition;
                }
                Thread.sleep(timer);
            }
            catch (InterruptedException e)
            {
            }
        }
        while (run && timer > 0);
    }

    public void stop()
    {
        run = false;
    }
}
