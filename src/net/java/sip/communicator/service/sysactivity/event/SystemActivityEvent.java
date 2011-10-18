/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * @return
     */
    public int getEventID()
    {
        return this.eventID;
    }
}
