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
package net.java.sip.communicator.impl.sysactivity;

import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class SystemActivityNotifications
{
    /**
     * The <tt>Logger</tt> used by the <tt>SystemActivityNotifications</tt>
     * class to log debugging information.
     */
    private static final Logger logger
        = Logger.getLogger(SystemActivityNotifications.class);

    /**
     * Computer display has stand by.
     */
    public static final int NOTIFY_DISPLAY_SLEEP = 2;

    /**
     * Computer display wakes up after stand by.
     */
    public static final int NOTIFY_DISPLAY_WAKE = 3;

    /**
     * A change in dns configuration has occurred.
     */
    public static final int NOTIFY_DNS_CHANGE = 10;

    /**
     * All processes have been informed about ending session, now notify for
     * the actual end session.
     */
    public static final int NOTIFY_ENDSESSION = 12;

    /**
     * A change in network configuration has occurred.
     */
    public static final int NOTIFY_NETWORK_CHANGE = 9;

    /**
     * Notifies for start of process of ending desktop session,
     * logoff or shutdown.
     */
    public static final int NOTIFY_QUERY_ENDSESSION = 11;

    /**
     * Screen has been locked.
     */
    public static final int NOTIFY_SCREEN_LOCKED = 7;

    /**
     * Screen has been unlocked.
     */
    public static final int NOTIFY_SCREEN_UNLOCKED = 8;

    /**
     * Screensaver has been started.
     */
    public static final int NOTIFY_SCREENSAVER_START = 4;

    /**
     * Screensaver has been stopped.
     */
    public static final int NOTIFY_SCREENSAVER_STOP = 6;

    /**
     * Screensaver will stop.
     */
    public static final int NOTIFY_SCREENSAVER_WILL_STOP = 5;

    /**
     * Notify that computers is going to sleep.
     */
    public static final int NOTIFY_SLEEP = 0;

    /**
     * Notify that computer is wakeing up after stand by.
     */
    public static final int NOTIFY_WAKE = 1;

    /**
     * The native instance.
     */
    private static long ptr;

    /**
     * Init native library.
     */
    static
    {
        try
        {
            // Don't load native library on Android to prevent the exception
            if(!org.jitsi.util.OSUtils.IS_ANDROID)
            {
                System.loadLibrary("sysactivitynotifications");

                ptr = allocAndInit();
                if (ptr == -1)
                    ptr = 0;
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.warn("Failed to initialize native counterpart", t);
        }
    }

    /**
     * Allocate native resources and gets a pointer.
     *
     * @return
     */
    private static native long allocAndInit();

    /**
     * Returns the when was last input in milliseconds. The time when there was
     * any activity on the computer.
     *
     * @return the last input in milliseconds
     */
    public static native long getLastInput();

    /**
     * Whether native library is loaded.
     *
     * @return whether native library is loaded.
     */
    public static boolean isLoaded()
    {
        return (ptr != 0);
    }

    /**
     * Release native resources.
     *
     * @param ptr
     */
    private static native void release(long ptr);

    /**
     * Sets notifier delegate.
     *
     * @param ptr
     * @param delegate
     */
    public static native void setDelegate(
            long ptr,
            NotificationsDelegate delegate);

    /**
     * Sets delegate.
     *
     * @param delegate
     */
    public static void setDelegate(NotificationsDelegate delegate)
    {
        if (ptr != 0)
            setDelegate(ptr, delegate);
    }

    /**
     * Start.
     */
    public static void start()
    {
        if (ptr != 0)
            start(ptr);
    }

    /**
     * Start processing.
     *
     * @param ptr
     */
    private static native void start(long ptr);

    /**
     * Stop.
     */
    public static void stop()
    {
        if (ptr != 0)
        {
            stop(ptr);
            release(ptr);
            ptr = 0;
        }
    }

    /**
     * Stop processing.
     *
     * @param ptr
     */
    private static native void stop(long ptr);

    /**
     * Delegate class to be notified about changes.
     */
    public interface NotificationsDelegate
    {
        /**
         * Callback method when receiving notifications.
         *
         * @param type
         */
        public void notify(int type);

        /**
         * Callback method when receiving special network notifications.
         *
         * @param family family of network change (ipv6, ipv4)
         * @param luidIndex unique index of interface
         * @param name name of the interface
         * @param type of the interface
         * @param connected whether interface is connected or not.
         */
        public void notifyNetworkChange(
                int family,
                long luidIndex,
                String name,
                long type,
                boolean connected);
    }
}
