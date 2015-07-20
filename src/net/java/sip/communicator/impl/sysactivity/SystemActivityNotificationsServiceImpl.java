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

import java.util.*;

import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * Service implementation listens for computer changes as sleeping, network
 * change, inactivity.
 *
 * @author Damian Minkov
 */
public class SystemActivityNotificationsServiceImpl
    implements SystemActivityNotifications.NotificationsDelegate,
               SystemActivityNotificationsService,
               Runnable
{
    /**
     * The <tt>Logger</tt> used by this
     * <tt>SystemActivityNotificationsServiceImpl</tt> for logging output.
     */
    private final Logger logger
        = Logger.getLogger(SystemActivityNotificationsServiceImpl.class);

    /**
     * The thread dispatcher of network change events.
     */
    private final SystemActivityEventDispatcher eventDispatcher
        = new SystemActivityEventDispatcher();

    /**
     * A list of listeners registered for idle events.
     */
    private final Map<SystemActivityChangeListener,Long> idleChangeListeners
        = new HashMap<SystemActivityChangeListener, Long>();

    /**
     * Listeners which are fired for idle state and which will be fired
     * with idle end when needed.
     */
    private final List<SystemActivityChangeListener> listenersInIdleState
        = new ArrayList<SystemActivityChangeListener>();

    /**
     * The interval between checks when not idle.
     */
    private static final int CHECK_FOR_IDLE_DEFAULT = 30 * 1000;

    /**
     * The interval between checks when idle. The interval is shorter
     * so we can react almost immediately when we are active again.
     */
    private static final int CHECK_FOR_IDLE_WHEN_IDLE = 1000;

    /**
     * The time in milliseconds between two checks for system idle.
     */
    private static int idleStateCheckDelay = CHECK_FOR_IDLE_DEFAULT;

    /**
     * Whether current service is started or stopped.
     */
    private boolean running = false;

    /**
     * The time when we received latest network change event.
     */
    private long lastNetworkChange = -1;

    /**
     * Sometimes (on windows) we got several network change events
     * this is the time after which latest event we will skip next events.
     */
    private static final long NETWORK_EVENT_SILENT_TIME = 10*1000;

    /**
     * Whether network is currently connected.
     */
    private Boolean networkIsConnected = null;

    /**
     * The linux impl class name.
     */
    private static final String SYSTEM_ACTIVITY_MANAGER_LINUX_CLASS
        = "net.java.sip.communicator.impl.sysactivity.NetworkManagerListenerImpl";

    /**
     * The android impl class name.
     */
    private static final String SYSTEM_ACTIVITY_MANAGER_ANDROID_CLASS
        = "net.java.sip.communicator.impl.sysactivity.ConnectivityManagerListenerImpl";

    /**
     * The currently instantiated and working manager.
     */
    private SystemActivityManager currentRunningManager = null;

    /**
     * Init and start notifications.
     */
    public void start()
    {
        running = true;

        // set the delegate and start notification in new thread
        // make sure we don't block startup process
        Thread notifystartThread
            = new Thread(
                    new Runnable()
                    {
                        public void run()
                        {
                            SystemActivityNotifications.setDelegate(
                                SystemActivityNotificationsServiceImpl.this);
                            SystemActivityNotifications.start();
                        }
                    },
                    "SystemActivityNotificationsServiceImpl");
        notifystartThread.setDaemon(true);
        notifystartThread.start();

        if(isSupported(SystemActivityEvent.EVENT_SYSTEM_IDLE))
        {
            // a thread periodically checks system idle state and if it pass the
            // idle time for a particular listener, will inform it.
            Thread idleNotifyThread = new Thread(
                this,
                "SystemActivityNotificationsServiceImpl.IdleNotifyThread");
            idleNotifyThread.setDaemon(true);
            idleNotifyThread.start();
        }

        if (getCurrentRunningManager() != null)
            getCurrentRunningManager().start();
    }

    /**
     * Stop notifications.
     */
    public void stop()
    {
        SystemActivityNotifications.stop();

        if (getCurrentRunningManager() != null)
            getCurrentRunningManager().stop();

        eventDispatcher.stop();

        running = false;

        synchronized(this)
        {
            this.notifyAll();
        }
    }

    /**
     * Registers a listener that would be notified of changes that have occurred
     * in the underlying system.
     *
     * @param listener the listener that we'd like to register for changes in
     * the underlying system.
     */
    public void addSystemActivityChangeListener(
            SystemActivityChangeListener listener)
    {
        eventDispatcher.addSystemActivityChangeListener(listener);
    }

    /**
     * Remove the specified listener so that it won't receive further
     * notifications of changes that occur in the underlying system
     *
     * @param listener the listener to remove.
     */
    public void removeSystemActivityChangeListener(
            SystemActivityChangeListener listener)
    {
        eventDispatcher.removeSystemActivityChangeListener(listener);
    }

    /**
     * Registers a listener that would be notified for idle of the system
     * for <tt>idleTime</tt>.
     *
     * @param idleTime the time in milliseconds after which we will consider
     * system to be idle. This doesn't count when system seems idle as
     * monitor is off or screensaver is on, or desktop is locked.
     * @param listener the listener that we'd like to register for changes in
     * the underlying system.
     */
    public void addIdleSystemChangeListener(
            long idleTime,
            SystemActivityChangeListener listener)
    {
        synchronized (idleChangeListeners)
        {
            if (idleTime > 0
                && !idleChangeListeners.containsKey(listener))
                idleChangeListeners.put(listener, idleTime);
        }
    }

    /**
     * Remove the specified listener so that it won't receive further
     * notifications for idle system.
     *
     * @param listener the listener to remove.
     */
    public void removeIdleSystemChangeListener(
            SystemActivityChangeListener listener)
    {
        synchronized (idleChangeListeners)
        {
            idleChangeListeners.remove(listener);
        }
    }

    /**
     * The time since last user input. The time the system has been idle.
     * @return time the system has been idle.
     */
    public long getTimeSinceLastInput()
    {
        if(SystemActivityNotifications.isLoaded())
            return SystemActivityNotifications.getLastInput();
        else
            return -1;
    }

    /**
     * Callback method when receiving notifications.
     *
     * @param type type of the notification.
     */
    public void notify(int type)
    {
        SystemActivityEvent evt = null;
        switch(type)
        {
            case SystemActivityNotifications.NOTIFY_SLEEP :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_SLEEP);
                break;
            case SystemActivityNotifications.NOTIFY_WAKE :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_WAKE);
                break;
            case SystemActivityNotifications.NOTIFY_DISPLAY_SLEEP :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_DISPLAY_SLEEP);
                break;
            case SystemActivityNotifications.NOTIFY_DISPLAY_WAKE :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_DISPLAY_WAKE);
                break;
            case SystemActivityNotifications.NOTIFY_SCREENSAVER_START :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_SCREENSAVER_START);
                break;
            case SystemActivityNotifications.NOTIFY_SCREENSAVER_WILL_STOP :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_SCREENSAVER_WILL_STOP);
                break;
            case SystemActivityNotifications.NOTIFY_SCREENSAVER_STOP :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_SCREENSAVER_STOP);
                break;
            case SystemActivityNotifications.NOTIFY_SCREEN_LOCKED :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_SCREEN_LOCKED);
                break;
            case SystemActivityNotifications.NOTIFY_SCREEN_UNLOCKED :
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_SCREEN_UNLOCKED);
                break;
            case SystemActivityNotifications.NOTIFY_NETWORK_CHANGE :
            {
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_NETWORK_CHANGE);
                break;
            }
            case SystemActivityNotifications.NOTIFY_DNS_CHANGE :
            {
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_DNS_CHANGE);
                break;
            }
            case SystemActivityNotifications.NOTIFY_QUERY_ENDSESSION :
            {
                // both events QUERY_ENDSESSION and ENDSESSION
                // depend on the result one after another
                // we don't put them in new thread in order to give control
                // in the bundles using this events.
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_QUERY_ENDSESSION);
                eventDispatcher.fireSystemActivityEventCurrentThread(evt);

                return;
            }
            case SystemActivityNotifications.NOTIFY_ENDSESSION :
            {
                // both events QUERY_ENDSESSION and ENDSESSION
                // depend on the result one after another
                // we don't put them in new thread in order to give control
                // in the bundles using this events.
                evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_ENDSESSION);
                eventDispatcher.fireSystemActivityEventCurrentThread(evt);

                return;
            }
        }

        if (evt != null)
            fireSystemActivityEvent(evt);
    }

    /**
     * Callback method when receiving special network notifications.
     *
     * @param family family of network change (ipv6, ipv4)
     *  AF_UNSPEC = 0 (The address family is unspecified.)
     *  AF_INET = 2 (The Internet Protocol version 4 (IPv4) address family)
     *  AF_INET6 = 23 (The Internet Protocol version 6 (IPv6) address family)
     * @param luidIndex unique index of interface
     * @param name name of the interface
     * @param type of the interface
     *  Possible values for the interface type are listed in the Ipifcons.h file.
     *  common values:
     *  IF_TYPE_OTHER = 1 (Some other type of network interface.)
     *  IF_TYPE_ETHERNET_CSMACD = 6 (An Ethernet network interface.)
     *  IF_TYPE_ISO88025_TOKENRING = 9 (A token ring network interface.)
     *  IF_TYPE_PPP = 23 (A PPP network interface.)
     *  IF_TYPE_SOFTWARE_LOOPBACK = 24 (A software loopback network interface.)
     *  IF_TYPE_IEEE80211 = 71 (An IEEE 802.11 wireless network interface.)
     *  IF_TYPE_TUNNEL = 131 (A tunnel type encapsulation network interface.)
     *  IF_TYPE_IEEE1394 = 144 (An IEEE 1394 (Firewire) high performance
     *                          serial bus network interface.)
     * @param connected whether interface is connected or not.
     */
    public void notifyNetworkChange(
            int family,
            long luidIndex,
            String name,
            long type,
            boolean connected)
    {
        long current = System.currentTimeMillis();
        if(current - lastNetworkChange <= NETWORK_EVENT_SILENT_TIME
           && (networkIsConnected != null && networkIsConnected.equals(connected)))
        {
            networkIsConnected = connected;
            return;
        }

        lastNetworkChange = current;
        networkIsConnected = connected;

        SystemActivityEvent evt = new SystemActivityEvent(this,
                    SystemActivityEvent.EVENT_NETWORK_CHANGE);
        fireSystemActivityEvent(evt);
    }

    /**
     * The thread run method that handles idle notifies.
     *
     * @see Thread#run()
     */
    public void run()
    {
        while(running)
        {
            try
            {
                long idleTime = 0;
                if(idleChangeListeners.size() > 0)
                {
                    // check
                    idleTime = SystemActivityNotifications.getLastInput();

                    if((idleTime < idleStateCheckDelay)
                            && (listenersInIdleState.size() > 0))
                    {
                        for(SystemActivityChangeListener l
                                : listenersInIdleState)
                        {
                            fireSystemIdleEndEvent(l);
                        }
                        listenersInIdleState.clear();
                    }

                    for(Map.Entry<SystemActivityChangeListener, Long> entry
                            : idleChangeListeners.entrySet())
                    {
                        SystemActivityChangeListener listener =
                            entry.getKey();

                        if(!listenersInIdleState.contains(listener)
                                && (entry.getValue() <= idleTime))
                        {
                            fireSystemIdleEvent(listener);

                            listenersInIdleState.add(listener);
                        }
                    }
                }

                // if the minimum check for idle is X minutes
                // we will wait before checking (X - Y + 1sec)
                // where Y is the last idle time returned by OS
                if(listenersInIdleState.size() > 0)
                {
                    idleStateCheckDelay = CHECK_FOR_IDLE_WHEN_IDLE;
                }
                else if(idleTime != 0)
                {
                    long minIdleSetting = CHECK_FOR_IDLE_DEFAULT;

                    if(!idleChangeListeners.isEmpty())
                        minIdleSetting =
                            Collections.min(idleChangeListeners.values());

                    int newSetting = (int)(minIdleSetting - idleTime) + 1000;

                    if(newSetting > 0)
                        idleStateCheckDelay = newSetting;
                    else
                        idleStateCheckDelay = CHECK_FOR_IDLE_DEFAULT;
                }
                else
                {
                    idleStateCheckDelay = CHECK_FOR_IDLE_DEFAULT;
                }

                // wait for the specified time
                synchronized(this)
                {
                    this.wait(idleStateCheckDelay);
                }
            }
            catch(UnsatisfiedLinkError t)
            {
                logger.error("Missing native impl", t);
                return;
            }
            catch(Throwable t)
            {
                logger.error("Error checking for idle", t);
            }
        }
    }

    /**
     * Delivers the specified event to all registered listeners.
     *
     * @param evt the <tt>SystemActivityEvent</tt> that we'd like delivered to
     * all registered message listeners.
     */
    protected void fireSystemActivityEvent(SystemActivityEvent evt)
    {
        int eventID = evt.getEventID();

        // Add network activity info to track wake up problems.
        if (logger.isInfoEnabled()
                && ((eventID == SystemActivityEvent.EVENT_NETWORK_CHANGE)
                    || (eventID == SystemActivityEvent.EVENT_DNS_CHANGE)))
        {
            logger.info("Received system activity event: " + evt);
        }

        if (eventID == SystemActivityEvent.EVENT_NETWORK_CHANGE)
        {
            // Give time to Java to dispatch same event and populate its network
            // interfaces.
            eventDispatcher.fireSystemActivityEvent(evt, 500);
        }
        else
            eventDispatcher.fireSystemActivityEvent(evt);
    }

    /**
     * Delivers the specified event to all registered listeners.
     *
     * @param listener listener to inform
     */
    protected void fireSystemIdleEvent(SystemActivityChangeListener listener)
    {
        SystemActivityEvent evt
            = new SystemActivityEvent(
                    this,
                    SystemActivityEvent.EVENT_SYSTEM_IDLE);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching SystemActivityEvent evt=" + evt);

        try
        {
            listener.activityChanged(evt);
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.error("Error delivering event", t);
        }
    }

    /**
     * Delivers the specified event to listener.
     *
     * @param listener listener to inform
     */
    protected void fireSystemIdleEndEvent(
            SystemActivityChangeListener listener)
    {
        SystemActivityEvent evt
            = new SystemActivityEvent(
                    this,
                    SystemActivityEvent.EVENT_SYSTEM_IDLE_END);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching SystemActivityEvent evt=" + evt);

        try
        {
            listener.activityChanged(evt);
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.error("Error delivering event", t);
        }
    }

    /**
     * Can check whether an event id is supported on
     * current operation system.
     * Simple return what is implemented in native, and checks
     * are made when possible, for example linux cannot connect
     * to NM through dbus.
     * @param eventID the event to check.
     * @return whether the supplied event id is supported.
     */
    public boolean isSupported(int eventID)
    {
        if(OSUtils.IS_WINDOWS)
        {
            switch(eventID)
            {
            case SystemActivityEvent.EVENT_SLEEP:
            case SystemActivityEvent.EVENT_WAKE:
            case SystemActivityEvent.EVENT_NETWORK_CHANGE:
            case SystemActivityEvent.EVENT_SYSTEM_IDLE:
            case SystemActivityEvent.EVENT_SYSTEM_IDLE_END:
                return SystemActivityNotifications.isLoaded();
            default:
                return false;
            }
        }
        else if(OSUtils.IS_MAC)
        {
            return SystemActivityNotifications.isLoaded();
        }
        else if(OSUtils.IS_LINUX)
        {
            switch(eventID)
            {
            case SystemActivityEvent.EVENT_SLEEP:
            case SystemActivityEvent.EVENT_NETWORK_CHANGE:
                {
                    SystemActivityManager currentRunningManager
                        = getCurrentRunningManager();

                    return
                        (currentRunningManager == null)
                            ? false
                            : currentRunningManager.isConnected();
                }
            case SystemActivityEvent.EVENT_SYSTEM_IDLE:
            case SystemActivityEvent.EVENT_SYSTEM_IDLE_END:
                return SystemActivityNotifications.isLoaded();
            default:
                return false;
            }
        }
        else if(OSUtils.IS_ANDROID)
        {
            return (eventID == SystemActivityEvent.EVENT_NETWORK_CHANGE);
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns or instantiate the manager.
     * @return
     */
    private SystemActivityManager getCurrentRunningManager()
    {
        if(currentRunningManager == null)
        {
            try
            {
                String className = null;
                if(OSUtils.IS_LINUX)
                {
                    className = SYSTEM_ACTIVITY_MANAGER_LINUX_CLASS;
                }
                else if(OSUtils.IS_ANDROID)
                {
                    className = SYSTEM_ACTIVITY_MANAGER_ANDROID_CLASS;
                }

                if(className != null)
                    currentRunningManager = (SystemActivityManager)
                        Class.forName(className).newInstance();
            }
            catch(Throwable t)
            {
                logger.error("Error creating manager", t);
            }
        }

        return currentRunningManager;
    }
}
