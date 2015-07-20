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
package net.java.sip.communicator.service.sysactivity.event;

import java.util.*;

/**
 * An event class representing system activity that has occurred.
 * The event id indicates the exact reason for this event.
 * @author Damian Minkov
 */
public class SystemActivityEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Notify that computers is going to sleep.
     */
    public static final int EVENT_SLEEP = 0;

    /**
     * Notify that computer is wakeing up after stand by.
     */
    public static final int EVENT_WAKE = 1;

    /**
     * Computer display has stand by.
     */
    public static final int EVENT_DISPLAY_SLEEP = 2;

    /**
     * Computer display wakes up after stand by.
     */
    public static final int EVENT_DISPLAY_WAKE = 3;

    /**
     * Screensaver has been started.
     */
    public static final int EVENT_SCREENSAVER_START = 4;

    /**
     * Screensaver will stop.
     */
    public static final int EVENT_SCREENSAVER_WILL_STOP = 5;

    /**
     * Screensaver has been stopped.
     */
    public static final int EVENT_SCREENSAVER_STOP = 6;

    /**
     * Screen has been locked.
     */
    public static final int EVENT_SCREEN_LOCKED = 7;

    /**
     * Screen has been unlocked.
     */
    public static final int EVENT_SCREEN_UNLOCKED = 8;

    /**
     * A change in network configuration has occurred.
     */
    public static final int EVENT_NETWORK_CHANGE = 9;

    /**
     * A system idle event has occurred.
     */
    public static final int EVENT_SYSTEM_IDLE = 10;

    /**
     * A system was in idle state and now exits.
     */
    public static final int EVENT_SYSTEM_IDLE_END = 11;

    /**
     * A change in dns configuration has occurred.
     */
    public static final int EVENT_DNS_CHANGE = 12;

    /**
     * Informing that the machine is logging of or shutting down.
     */
    public static final int EVENT_QUERY_ENDSESSION = 13;

    /**
     * The log off or shutdown is in process for us, no matter
     * what other process has replied, whether one of them has canceled
     * or not the current end of session. It's like that cause we have answered
     * that we will shutdown.
     */
    public static final int EVENT_ENDSESSION = 14;

    /**
     * The type of the event.
     */
    private final int eventID;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param eventID the type of the event.
     * @throws IllegalArgumentException if source is null.
     */
    public SystemActivityEvent(Object source, int eventID)
    {
        super(source);

        this.eventID = eventID;
    }

    /**
     * Returns the type of the event.
     * @return the event ID
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns a String representation of this SystemActivityEvent object.
     *
     * @return  A a String representation of this SystemActivityEvent object.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[eventID=" + eventID + "]";
    }
}
