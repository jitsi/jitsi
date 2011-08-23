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
               ServiceListener
{
    /**
     * Our class logger.
     */
    private static  Logger logger =
        Logger.getLogger(NetworkConfigurationWatcher.class);

    /**
     * Listers for network configuration changes.
     */
    private final List<NetworkConfigurationChangeListener> listeners =
        new ArrayList<NetworkConfigurationChangeListener>();

    /**
     * The current active interfaces.
     */
    private List<NetworkInterface> activeInterfaces
            = new ArrayList<NetworkInterface>();

    /**
     * Service we use to listen for network changes.
     */
    private SystemActivityNotificationsService
            systemActivityNotificationsService = null;

    /**
     * Inits configuration watcher.
     */
    NetworkConfigurationWatcher()
    {
        checkNetworkInterfaces(false);
    }

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

        NetaddrActivator.getBundleContext().addServiceListener(this);

        this.systemActivityNotificationsService
            = ServiceUtils.getService(
                    NetaddrActivator.getBundleContext(),
                    SystemActivityNotificationsService.class);
        this.systemActivityNotificationsService
            .addSystemActivityChangeListener(this);
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

                    this.systemActivityNotificationsService =
                        (SystemActivityNotificationsService)sService;
                    systemActivityNotificationsService
                        .addSystemActivityChangeListener(this);
                    break;
                case ServiceEvent.UNREGISTERING:
                    ((SystemActivityNotificationsService)sService)
                        .addSystemActivityChangeListener(this);
                    break;
            }

            return;
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
     * Stop.
     */
    void stop()
    {
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

    public void activityChanged(SystemActivityEvent event)
    {
        if(event.getEventID() == SystemActivityEvent.EVENT_SLEEP)
        {
            // oo standby lets fire down to all interfaces
            // so they can reconnect
            Iterator<NetworkInterface> iter = activeInterfaces.iterator();
            while (iter.hasNext())
            {
                NetworkInterface niface = iter.next();
                fireChangeEvent(new ChangeEvent(niface,
                        ChangeEvent.IFACE_DOWN, true));
            }
            activeInterfaces.clear();
        }
        else if(event.getEventID() == SystemActivityEvent.EVENT_NETWORK_CHANGE)
        {
            checkNetworkInterfaces(true);
        }
    }

    private void checkNetworkInterfaces(boolean fireEvents)
    {
        try
        {
            Enumeration<NetworkInterface> e =
                NetworkInterface.getNetworkInterfaces();

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
                    if(fireEvents)
                        fireChangeEvent(new ChangeEvent(iface,
                            ChangeEvent.IFACE_DOWN, false));

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

                if(fireEvents)
                    fireChangeEvent(new ChangeEvent(iface,
                        ChangeEvent.IFACE_UP, false));
                activeInterfaces.add(iface);
            }
        } catch (SocketException e)
        {
            logger.error("Error checking network interfaces", e);
        }
    }
}
