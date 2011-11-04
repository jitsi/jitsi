/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.sysactivity;

import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class SystemActivityNotifications
{
    /**
     * Notify that computers is going to sleep.
     */
    public static final int NOTIFY_SLEEP = 0;

    /**
     * Notify that computer is wakeing up after stand by.
     */
    public static final int NOTIFY_WAKE = 1;

    /**
     * Computer display has stand by.
     */
    public static final int NOTIFY_DISPLAY_SLEEP = 2;

    /**
     * Computer display wakes up after stand by.
     */
    public static final int NOTIFY_DISPLAY_WAKE = 3;

    /**
     * Screensaver has been started.
     */
    public static final int NOTIFY_SCREENSAVER_START = 4;

    /**
     * Screensaver will stop.
     */
    public static final int NOTIFY_SCREENSAVER_WILL_STOP = 5;

    /**
     * Screensaver has been stopped.
     */
    public static final int NOTIFY_SCREENSAVER_STOP = 6;

    /**
     * Screen has been locked.
     */
    public static final int NOTIFY_SCREEN_LOCKED = 7;

    /**
     * Screen has been unlocked.
     */
    public static final int NOTIFY_SCREEN_UNLOCKED = 8;

    /**
     * A change in network configuration has occurred.
     */
    public static final int NOTIFY_NETWORK_CHANGE = 9;

    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(
        SystemActivityNotifications.class.getName());

    /**
     * The native instance.
     */
    private static long notifierInstance = -1;

    private static boolean loaded = false;

    /**
     * Init native library.
     */
    static
    {
        try
        {
            System.loadLibrary("sysactivitynotifications");

            notifierInstance = allocAndInit();

            loaded = true;
        }
        catch(Throwable t)
        {
            logger.warn("Error init native functions, " + t.getMessage());
        }
    }

    /**
     * Allocate native resources and gets a pointer.
     * @return
     */
    private static native long allocAndInit();

    /**
     * Release native resources.
     * @param ptr
     */
    private static native void release(long ptr);

    /**
     * Sets delegate.
     * @param delegate
     */
    public static void setDelegate(NotificationsDelegate delegate)
    {
        if(notifierInstance != -1)
        {
            setDelegate(notifierInstance, delegate);
        }
    }

    /**
     * Sets notifier delegate.
     * @param ptr
     * @param delegate
     */
    public static native void setDelegate(long ptr, NotificationsDelegate delegate);

    /**
     * Start processing.
     * @param ptr
     */
    private static native void start(long ptr);

    /**
     * Stop processing.
     * @param ptr
     */
    private static native void stop(long ptr);

    /**
     * Start.
     */
    public static void start()
    {
        if(notifierInstance != -1)
            start(notifierInstance);
    }

    /**
     * Stop.
     */
    public static void stop()
    {
        if(notifierInstance != -1)
        {
            stop(notifierInstance);
            release(notifierInstance);
            notifierInstance = -1;
        }
    }

    /**
     * Returns the when was last input in milliseconds. The time when
     * there was any activity on the computer.
     * @return
     */
    public static native long getLastInput();

    /**
     * Delegate class to be notified for changes.
     */
    public static abstract class NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications.
         *
         * @param type
         */
        public abstract void notify(int type);

        /**
         * Callback method when receiving special network notifications.
         *
         * @param family family of network change (ipv6, ipv4)
         * @param luidIndex unique index of interface
         * @param name name of the interface
         * @param type of the interface
         * @param connected whether interface is connected or not.
         */
        public abstract void notifyNetworkChange(
                int family,
                long luidIndex,
                String name,
                long type,
                boolean connected);
    }

    /**
     * Whether native library is loaded.
     * @return whether native library is loaded.
     */
    public static boolean isLoaded()
    {
        return loaded;
    }
}
