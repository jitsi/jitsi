/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.util.*;

/**
 * Periodically checks the current network interfaces to track changes
 * and fire events on those changes.
 * 
 * @author Damian Minkov
 */
public class NetworkConfigurationWatcher
    implements Runnable
{
    /**
     * Our class logger.
     */
    private static  Logger logger =
        Logger.getLogger(NetworkConfigurationWatcher.class);

    /**
     * Interval between check of network configuration.
     */
    private static final int CHECK_INTERVAL = 3000; // 3 sec.

    /**
     * Listers for network configuration changes.
     */
    private final List<NetworkConfigurationChangeListener> listeners =
        new ArrayList<NetworkConfigurationChangeListener>();

    /**
     * Whether current thread is running.
     */
    private boolean isRunning = false;

    /**
     * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
     * be informed for network configuration changes.
     * @param listener the listener.
     */
    void addNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        synchronized(listeners)
        {
            listeners.add(listener);
        }

        if(!isRunning)
        {
            isRunning = true;
            new Thread(this).start();
        }
    }

    /**
     * Remove <tt>NetworkConfigurationChangeListener</tt>.
     * @param listener the listener.
     */
    void removeNetworkConfigurationChangeListener(
        NetworkConfigurationChangeListener listener)
    {
        synchronized(listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Main loop of this thread.
     */
    public void run()
    {
        long last = 0;
        boolean isAfterStandby = false;

        List<NetworkInterface> activeInterfaces =
            new ArrayList<NetworkInterface>();

        while(isRunning)
        {
            long curr = System.currentTimeMillis();

            // if time spent between checks is more than 2 times
            // longer than the check interval we consider it as a
            // new check after standby
            if(!isAfterStandby && last != 0)
                isAfterStandby = (last + 2*CHECK_INTERVAL - curr) < 0;

            if(isAfterStandby)
            {
                // oo standby lets fire down to all interfaces
                // so they can reconnect
                Iterator<NetworkInterface> iter = activeInterfaces.iterator();
                while (iter.hasNext())
                {
                    NetworkInterface niface = iter.next();
                    fireChangeEvent(new ChangeEvent(niface,
                            ChangeEvent.IFACE_DOWN, isAfterStandby));
                }
                activeInterfaces.clear();

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
                Enumeration<NetworkInterface> e =
                    NetworkInterface.getNetworkInterfaces();

                boolean networkIsUP = activeInterfaces.size() > 0;

                List<NetworkInterface> currentActiveInterfaces =
                    new ArrayList<NetworkInterface>();

                while (e.hasMoreElements())
                {
                    NetworkInterface networkInterface = e.nextElement();

                    if(isInterfaceLoopback(networkInterface))
                        continue;

                    // if interface is up and has some valid(non-local) address
                    if(isInterfaceUp(networkInterface) 
                        && hasValidAddress(networkInterface))
                    {
                        currentActiveInterfaces.add(networkInterface);
                    }
                }

                List<NetworkInterface> inactiveActiveInterfaces =
                    new ArrayList<NetworkInterface>(activeInterfaces);
                inactiveActiveInterfaces.removeAll(currentActiveInterfaces);

                // fire that interface has gone down
                for (int i = 0; i < inactiveActiveInterfaces.size(); i++)
                {
                    NetworkInterface iface = inactiveActiveInterfaces.get(i);

                    if(!containsInterfaceWithName(
                        currentActiveInterfaces, iface.getName()))
                    {
                        fireChangeEvent(new ChangeEvent(iface,
                            ChangeEvent.IFACE_DOWN, isAfterStandby));
                        
                        activeInterfaces.remove(iface);
                    }
                }

                // now we leave with only with the new and up interfaces
                // in currentActiveInterfaces list
                currentActiveInterfaces.removeAll(activeInterfaces);

                // fire that interface has gone up
                for (int i = 0; i < currentActiveInterfaces.size(); i++)
                {
                    NetworkInterface iface = currentActiveInterfaces.get(i);

                    fireChangeEvent(new ChangeEvent(iface,
                        ChangeEvent.IFACE_UP, isAfterStandby));
                    activeInterfaces.add(iface);
                }

                // fire that network has gone up
                if(!networkIsUP && activeInterfaces.size() > 0)
                {
                    isAfterStandby = false;
                }

                last = curr;
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

    /**
     * Fire ChangeEvent.
     * @param evt the event to fire.
     */
    private void fireChangeEvent(ChangeEvent evt)
    {
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                NetworkConfigurationChangeListener nCChangeListener
                        = listeners.get(i);
                try
                {
                    nCChangeListener.configurationChanged(evt);
                } catch (Throwable e)
                {
                    logger.warn("Error delivering event:" + evt + ", to:"
                        + nCChangeListener);
                }
            }
        }
    }


    /**
     * Stop current running thread.
     */
    void stop()
    {
        synchronized(this)
        {
            isRunning = false;
            notifyAll();
        }
    }

    /**
     * Checks whether the supplied network interface name is in the list.
     * @param ifaces the list of interfaces.
     * @param name the name to check.
     * @return whether name is found in the list of interfaces.
     */
    private boolean containsInterfaceWithName(
        List<NetworkInterface> ifaces, String name)
    {
        for (int i = 0; i < ifaces.size(); i++)
        {
            NetworkInterface networkInterface = ifaces.get(i);
            if(networkInterface.getName().equals(name))
                return true;
        }

        return false;
    }

    /**
     * Whether the supplied interface has a valid non-local address.
     * @param iface interface.
     * @return has a valid address.
     */
    private static boolean hasValidAddress(NetworkInterface iface)
    {
        Enumeration<InetAddress> as =
            iface.getInetAddresses();
        while (as.hasMoreElements())
        {
            InetAddress inetAddress = as.nextElement();
            if(inetAddress.isLinkLocalAddress())
                continue;

            return true;
        }

        return false;
    }

    /**
     * Determines whether or not the <tt>iface</tt> interface is a loopback
     * interface. We use this method as a replacement to the
     * <tt>NetworkInterface.isLoopback()</tt> method that only comes with
     * java 1.6.
     *
     * @param iface the interface that we'd like to determine as loopback or not.
     *
     * @return true if <tt>iface</tt> contains at least one loopback address
     * and <tt>false</tt> otherwise.
     */
    public static boolean isInterfaceLoopback(NetworkInterface iface)
    {
        try
        {
            Method method = iface.getClass().getMethod("isLoopback");

            return ((Boolean)method.invoke(iface, new Object[]{}))
                        .booleanValue();
        }
        catch(Throwable t)
        {
            //apparently we are not running in a JVM that supports the
            //is Loopback method. we'll try another approach.
        }
        Enumeration<InetAddress> addresses = iface.getInetAddresses();

        return addresses.hasMoreElements()
            && addresses.nextElement().isLoopbackAddress();
    }

    /**
     * Determines, if possible, whether or not the <tt>iface</tt> interface is
     * up. We use this method so that we could use {@link
     * java.net.NetworkInterface}'s <tt>isUp()</tt> when running a JVM that
     * supports it and return a default value otherwise.
     *
     * @param iface the interface that we'd like to determine as Up or Down.
     *
     * @return <tt>false</tt> if <tt>iface</tt> is known to be down and
     * <tt>true</tt> if the <tt>iface</tt> is Up or in case we couldn't
     * determine.
     */
    public static boolean isInterfaceUp(NetworkInterface iface)
    {
        try
        {
            Method method = iface.getClass().getMethod("isUp");

            return ((Boolean)method.invoke(iface)).booleanValue();
        }
        catch(Throwable t)
        {
            //apparently we are not running in a JVM that supports the
            //isUp method. returning default value.
        }

        return true;
    }
}
