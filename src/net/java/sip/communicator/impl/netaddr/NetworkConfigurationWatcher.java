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
package net.java.sip.communicator.impl.netaddr;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Periodically checks the current network interfaces to track changes
 * and fire events on those changes.
 *
 * @author Damian Minkov
 */
public class NetworkConfigurationWatcher
    implements SystemActivityChangeListener,
               ServiceListener,
               Runnable
{
    /**
     * Our class logger.
     */
    private static  Logger logger =
        Logger.getLogger(NetworkConfigurationWatcher.class);

    /**
     * The current active interfaces.
     */
    private Map<String, List<InetAddress>> activeInterfaces
            = new HashMap<String, List<InetAddress>>();

    /**
     * Interval between check of network configuration.
     */
    private static final int CHECK_INTERVAL = 3000; // 3 sec.

    /**
     * Whether thread checking for network notifications is running.
     */
    private boolean isRunning = false;

    /**
     * Service we use to listen for network changes.
     */
    private SystemActivityNotificationsService
            systemActivityNotificationsService = null;

    /**
     * The thread dispatcher of network change events.
     */
    private NetworkEventDispatcher eventDispatcher =
            new NetworkEventDispatcher();

    /**
     * Inits configuration watcher.
     */
    NetworkConfigurationWatcher()
    {
        try
        {
            checkNetworkInterfaces(false, 0, true);
        } catch (SocketException e)
        {
            logger.error("Error checking network interfaces", e);
        }
    }

    /**
     * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
     * be informed for network configuration changes.
     * @param listener the listener.
     */
    void addNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        eventDispatcher.addNetworkConfigurationChangeListener(listener);

        initialFireEvents(listener);

        NetaddrActivator.getBundleContext().addServiceListener(this);

        if(this.systemActivityNotificationsService == null)
        {
            SystemActivityNotificationsService systActService
                = ServiceUtils.getService(
                        NetaddrActivator.getBundleContext(),
                        SystemActivityNotificationsService.class);

            handleNewSystemActivityNotificationsService(systActService);
        }
    }

    /**
     * Used to fire initial events to newly added listers.
     * @param listener the listener to fire.
     */
    private void initialFireEvents(
            NetworkConfigurationChangeListener listener)
    {
        try
        {
            Enumeration<NetworkInterface> e =
            NetworkInterface.getNetworkInterfaces();

            while (e.hasMoreElements())
            {
                NetworkInterface networkInterface = e.nextElement();

                if(networkInterface.isLoopback())
                    continue;

                // if interface is up and has some valid(non-local) address
                // add it to currently active
                if(networkInterface.isUp())
                {
                    Enumeration<InetAddress> as =
                        networkInterface.getInetAddresses();
                    boolean hasAddress = false;
                    while (as.hasMoreElements())
                    {
                        InetAddress inetAddress = as.nextElement();
                        if(inetAddress.isLinkLocalAddress())
                            continue;

                        hasAddress = true;
                        NetworkEventDispatcher.fireChangeEvent(
                            new ChangeEvent(
                                    networkInterface.getName(),
                                    ChangeEvent.ADDRESS_UP,
                                    inetAddress,
                                    false,
                                    true),
                            listener);
                    }

                    if(hasAddress)
                        NetworkEventDispatcher.fireChangeEvent(
                            new ChangeEvent(networkInterface.getName(),
                                ChangeEvent.IFACE_UP, null, false, true),
                            listener);
                }
            }


        } catch (SocketException e)
        {
            logger.error("Error checking network interfaces", e);
        }
    }

    /**
     * Saves the reference for the service and
     * add a listener if the desired events are supported. Or start
     * the checking thread otherwise.
     * @param newService
     */
    private void handleNewSystemActivityNotificationsService(
            SystemActivityNotificationsService newService)
    {
        if(newService == null)
            return;

        this.systemActivityNotificationsService = newService;

        if(this.systemActivityNotificationsService
                    .isSupported(SystemActivityEvent.EVENT_NETWORK_CHANGE))
        {
            this.systemActivityNotificationsService
                .addSystemActivityChangeListener(this);
        }
        else
        {
            if(!isRunning)
            {
                isRunning = true;
                Thread th = new Thread(this);
                // set to max priority to prevent detecting sleep if the cpu is
                // overloaded
                th.setPriority(Thread.MAX_PRIORITY);
                th.start();
            }
        }
    }

    /**
     * Remove <tt>NetworkConfigurationChangeListener</tt>.
     * @param listener the listener.
     */
    void removeNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        eventDispatcher.removeNetworkConfigurationChangeListener(listener);
    }

    /**
     * When new protocol provider is registered we add needed listeners.
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference serviceRef = serviceEvent.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know we are shutting down
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object sService = NetaddrActivator.getBundleContext()
                .getService(serviceRef);

        if(sService instanceof SystemActivityNotificationsService)
        {
            switch (serviceEvent.getType())
            {
                case ServiceEvent.REGISTERED:
                    if(this.systemActivityNotificationsService != null)
                        break;

                    handleNewSystemActivityNotificationsService(
                        (SystemActivityNotificationsService)sService);
                    break;
                case ServiceEvent.UNREGISTERING:
                    ((SystemActivityNotificationsService)sService)
                        .removeSystemActivityChangeListener(this);
                    break;
            }

            return;
        }
    }

    /**
     * Stop.
     */
    void stop()
    {
        if(isRunning)
        {
            synchronized(this)
            {
                isRunning = false;
                notifyAll();
            }
        }

        if(eventDispatcher != null)
            eventDispatcher.stop();
    }

    /**
     * This method gets called when a notification action for a particular event
     * type has been changed. We are interested in sleep and network
     * changed events.
     *
     * @param event the <tt>NotificationActionTypeEvent</tt>, which is
     * dispatched when an action has been changed.
     */
    public void activityChanged(SystemActivityEvent event)
    {
        if(event.getEventID() == SystemActivityEvent.EVENT_SLEEP)
        {
            // oo standby lets fire down to all interfaces
            // so they can reconnect
            downAllInterfaces();
        }
        else if(event.getEventID() == SystemActivityEvent.EVENT_NETWORK_CHANGE)
        {
            try
            {
                checkNetworkInterfaces(true, 0, true);
            } catch (SocketException e)
            {
                logger.error("Error checking network interfaces", e);
            }
        }
        else if(event.getEventID() == SystemActivityEvent.EVENT_DNS_CHANGE)
        {
            try
            {
                eventDispatcher.fireChangeEvent(
                    new ChangeEvent(event.getSource(), ChangeEvent.DNS_CHANGE));
            }
            catch(Throwable t)
            {
                logger.error("Error dispatching dns change.");
            }
        }
    }

    /**
     * Down all interfaces and fire events for it.
     */
    private void downAllInterfaces()
    {
        Iterator<String> iter = activeInterfaces.keySet().iterator();
        while (iter.hasNext())
        {
            String niface = iter.next();
            eventDispatcher.fireChangeEvent(new ChangeEvent(niface,
                    ChangeEvent.IFACE_DOWN, true));
        }
        activeInterfaces.clear();
    }

    /**
     * Checks current interfaces configuration against the last saved
     * active interfaces.
     * @param fireEvents whether we will fire events when we detect
     * that interface is changed. When we start we query the interfaces
     * just to check which are online, without firing events.
     * @param waitBeforeFiringUpEvents milliseconds to wait before
     * firing events for interfaces up, sometimes we must wait a little bit
     * and give time for interfaces to configure fully (dns on linux).
     * @param printDebugInfo whether to print debug info, do not print
     * anything if we are constantly checking as it will flood logs and made
     * them unusable.
     */
    private void checkNetworkInterfaces(
            boolean fireEvents,
            int waitBeforeFiringUpEvents,
            boolean printDebugInfo)
        throws SocketException
    {
        Enumeration<NetworkInterface> e =
            NetworkInterface.getNetworkInterfaces();

        Map<String, List<InetAddress>> currentActiveInterfaces =
            new HashMap<String, List<InetAddress>>();

        while (e.hasMoreElements())
        {
            NetworkInterface networkInterface = e.nextElement();

            if(networkInterface.isLoopback())
                continue;

            // if interface is up and has some valid(non-local) address
            // add it to currently active
            if(networkInterface.isUp())
            {
                List<InetAddress> addresses =
                    new ArrayList<InetAddress>();

                Enumeration<InetAddress> as =
                    networkInterface.getInetAddresses();
                while (as.hasMoreElements())
                {
                    InetAddress inetAddress = as.nextElement();
                    if(inetAddress.isLinkLocalAddress())
                        continue;

                    addresses.add(inetAddress);
                }

                if(addresses.size() > 0)
                    currentActiveInterfaces.put(
                        networkInterface.getName(), addresses);
            }
        }

        // add network debug info, to track wake up problems
        if(logger.isInfoEnabled() && printDebugInfo)
        {
            for(Map.Entry<String, List<InetAddress>> en :
                activeInterfaces.entrySet())
            {
                logger.info("Previously Active " + en.getKey()
                    + ":" + en.getValue());
            }

            for(Map.Entry<String, List<InetAddress>> en :
                currentActiveInterfaces.entrySet())
            {
                logger.info("Currently Active " + en.getKey()
                    + ":" + en.getValue());
            }
        }

        // search for down interface
        List<String> inactiveActiveInterfaces =
            new ArrayList<String>(activeInterfaces.keySet());
        List<String> currentActiveInterfacesSet
            = new ArrayList<String>(currentActiveInterfaces.keySet());
        inactiveActiveInterfaces.removeAll(currentActiveInterfacesSet);

        // fire that interface has gone down
        for (int i = 0; i < inactiveActiveInterfaces.size(); i++)
        {
            String iface = inactiveActiveInterfaces.get(i);

            if(!currentActiveInterfacesSet.contains(iface))
            {
                if(fireEvents)
                    eventDispatcher.fireChangeEvent(new ChangeEvent(iface,
                        ChangeEvent.IFACE_DOWN));

                activeInterfaces.remove(iface);
            }
        }

        // now look at the addresses of the connected interfaces
        // if something has gown down
        Iterator<Map.Entry<String, List<InetAddress>>>
                activeEntriesIter = activeInterfaces.entrySet().iterator();
        while(activeEntriesIter.hasNext())
        {
            Map.Entry<String, List<InetAddress>>
                entry = activeEntriesIter.next();
            Iterator<InetAddress> addrIter = entry.getValue().iterator();
            while(addrIter.hasNext())
            {
                InetAddress addr = addrIter.next();

                // if address is missing in current active interfaces
                // it means it has gone done
                List<InetAddress> addresses =
                        currentActiveInterfaces.get(entry.getKey());

                if(addresses != null && !addresses.contains(addr))
                {
                    if(fireEvents)
                        eventDispatcher.fireChangeEvent(
                            new ChangeEvent(entry.getKey(),
                                    ChangeEvent.ADDRESS_DOWN, addr));

                    addrIter.remove();
                }
            }
        }

        if(waitBeforeFiringUpEvents > 0
            && currentActiveInterfaces.size() != 0)
        {
            // calm for a while, we sometimes receive those events and
            // configuration has not yet finished (dns can be the old one)
            synchronized(this)
            {
                try{
                    wait(waitBeforeFiringUpEvents);
                }catch(InterruptedException ex){}
            }
        }

        // now look at the addresses of the connected interfaces
        // if something has gown up
        activeEntriesIter = currentActiveInterfaces.entrySet().iterator();
        while(activeEntriesIter.hasNext())
        {
            Map.Entry<String, List<InetAddress>>
                entry = activeEntriesIter.next();
            for(InetAddress addr : entry.getValue())
            {
                // if address is missing in active interfaces
                // it means it has gone up
                List<InetAddress> addresses =
                        activeInterfaces.get(entry.getKey());
                if(addresses != null && !addresses.contains(addr))
                {
                    if(fireEvents)
                        eventDispatcher.fireChangeEvent(
                                new ChangeEvent(entry.getKey(),
                                                ChangeEvent.ADDRESS_UP,
                                                addr));

                    addresses.add(addr);
                }
            }
        }

        // now we leave with only with the new and up interfaces
        // in currentActiveInterfaces Map
        Iterator<String> ifaceIter
                = activeInterfaces.keySet().iterator();
        while(ifaceIter.hasNext())
        {
            currentActiveInterfaces.remove(ifaceIter.next());
        }

        // fire that interface has gone up
        activeEntriesIter = currentActiveInterfaces.entrySet().iterator();
        while(activeEntriesIter.hasNext())
        {
            final Map.Entry<String, List<InetAddress>>
                entry = activeEntriesIter.next();
            for(InetAddress addr : entry.getValue())
            {
                if(fireEvents)
                    eventDispatcher.fireChangeEvent(
                            new ChangeEvent(entry.getKey(),
                                            ChangeEvent.ADDRESS_UP,
                                            addr));
            }

            if(fireEvents)
            {
                // if we haven't waited before, lets wait here
                // and give time to underlying os to configure fully the
                // network interface (receive and store dns config)
                int wait = waitBeforeFiringUpEvents;
                if(wait == 0)
                {
                    wait = 500;
                }

                eventDispatcher.fireChangeEvent(
                        new ChangeEvent(entry.getKey(), ChangeEvent.IFACE_UP),
                        wait);
            }

            activeInterfaces.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Main loop of this thread.
     */
    public void run()
    {
        long last = 0;
        boolean isAfterStandby = false;

        while(isRunning)
        {
            long curr = System.currentTimeMillis();

            // if time spent between checks is more than 4 times
            // longer than the check interval we consider it as a
            // new check after standby
            if(!isAfterStandby && last != 0)
                isAfterStandby = (last + 4*CHECK_INTERVAL - curr) < 0;

            if(isAfterStandby)
            {
                // oo standby lets fire down to all interfaces
                // so they can reconnect
                downAllInterfaces();

                // we have fired events for standby, make it to false now
                // so we can calculate it again next time
                isAfterStandby = false;

                last = curr;

                // give time to interfaces
                synchronized(this)
                {
                    try{
                        wait(CHECK_INTERVAL);
                    }
                    catch (Exception e){}
                }

                continue;
            }

            try
            {
                boolean networkIsUP = activeInterfaces.size() > 0;

                checkNetworkInterfaces(true, 1000, false);

                // fire that network has gone up
                if(!networkIsUP && activeInterfaces.size() > 0)
                {
                    isAfterStandby = false;
                }

                // save the last time that we checked
                last = System.currentTimeMillis();
            } catch (SocketException e)
            {
                logger.error("Error checking network interfaces", e);
            }

            synchronized(this)
            {
                try{
                    wait(CHECK_INTERVAL);
                }
                catch (Exception e){}
            }
        }
    }
}
