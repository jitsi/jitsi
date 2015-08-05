/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff Rick Blair
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
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.util.*;

// REMIND: multiple IP addresses

/**
 * mDNS implementation in Java.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Rick Blair, Jeff Sonstein,
 * Werner Randelshofer, Pierre Frisch, Scott Lewis
 * @author Christian Vincenot
 */
public class JmDNS
{
    private static final Logger logger
        = Logger.getLogger(JmDNS.class);

    /**
     * The version of JmDNS.
     */
    public static String VERSION = "2.0";

    /**
     * This is the multicast group, we are listening to for
     * multicast DNS messages.
     */
    private InetAddress group;
    /**
     * This is our multicast socket.
     */
    private MulticastSocket socket;

    /**
     * Used to fix live lock problem on unregester.
     */

    protected boolean closed = false;

    /**
     * Holds instances of JmDNS.DNSListener.
     * Must by a synchronized collection, because it is updated from
     * concurrent threads.
     */
    private List<DNSListener> listeners;
    /**
     * Holds instances of ServiceListener's.
     * Keys are Strings holding a fully qualified service type.
     * Values are LinkedList's of ServiceListener's.
     */
    private Map<String, List<ServiceListener>> serviceListeners;
    /**
     * Holds instances of ServiceTypeListener's.
     */
    private List<ServiceTypeListener> typeListeners;


    /**
     * Cache for DNSEntry's.
     */
    private DNSCache cache;

    /**
     * This hashtable holds the services that have been registered.
     * Keys are instances of String which hold an all lower-case version of the
     * fully qualified service name.
     * Values are instances of ServiceInfo.
     */
    Map<String, ServiceInfo> services;

    /**
     * This hashtable holds the service types that have been registered or
     * that have been received in an incoming datagram.
     * Keys are instances of String which hold an all lower-case version of the
     * fully qualified service type.
     * Values hold the fully qualified service type.
     */
    Map<String, String> serviceTypes;

    /**
     * Handle on the local host
     */
    HostInfo localHost;

    private Thread incomingListener = null;

    /**
     * Throttle count.
     * This is used to count the overall number of probes sent by JmDNS.
     * When the last throttle increment happened .
     */
    private int throttle;
    /**
     * Last throttle increment.
     */
    private long lastThrottleIncrement;

    /**
     * The timer is used to dispatch all outgoing messages of JmDNS.
     * It is also used to dispatch maintenance tasks for the DNS cache.
     */
    private Timer timer;

    /**
     * The source for random values.
     * This is used to introduce random delays in responses. This reduces the
     * potential for collisions on the network.
     */
    private final static Random random = new Random();

    /**
     * This lock is used to coordinate processing of incoming and outgoing
     * messages. This is needed, because the Rendezvous Conformance Test
     * does not forgive race conditions.
     */
    private Object ioLock = new Object();

    /**
     * If an incoming package which needs an answer is truncated, we store it
     * here. We add more incoming DNSRecords to it, until the JmDNS.Responder
     * timer picks it up.
     * Remind: This does not work well with multiple planned answers for packages
     * that came in from different clients.
     */
    private DNSIncoming plannedAnswer;

    // State machine
    /**
     * The state of JmDNS.
     * <p/>
     * For proper handling of concurrency, this variable must be
     * changed only using methods advanceState(), revertState() and cancel().
     */
    private DNSState state = DNSState.PROBING_1;

    /**
     * Timer task associated to the host name.
     * This is used to prevent from having multiple tasks associated to the host
     * name at the same time.
     */
    TimerTask task;

    /**
     * This hashtable is used to maintain a list of service types being collected
     * by this JmDNS instance.
     * The key of the hashtable is a service type name, the value is an instance
     * of JmDNS.ServiceCollector.
     *
     * @see #list
     */
    private HashMap<String, ServiceCollector> serviceCollectors = new HashMap<String, ServiceCollector>();

    /**
     * Create an instance of JmDNS.
     * @throws java.io.IOException
     */
    public JmDNS()
        throws IOException
    {
        //String SLevel = System.getProperty("jmdns.debug");

        if (logger.isDebugEnabled())
            logger.debug("JmDNS instance created");
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            // [PJYF Oct 14 2004] Why do we disallow the loopback address?
            init(addr.isLoopbackAddress() ? null : addr, addr.getHostName());
        }
        catch (IOException exc)
        {
            logger.error("Failed to get a reference to localhost", exc);
            init(null, "computer");
        }
    }

    /**
     * Create an instance of JmDNS and bind it to a
     * specific network interface given its IP-address.
     * @param addr
     * @throws java.io.IOException
     */
    public JmDNS(InetAddress addr)
        throws IOException
    {
        try
        {
            init(addr, addr.getHostName());
        }
        catch (IOException e)
        {
            init(null, "computer");
        }
    }

    /**
     * Initialize everything.
     *
     * @param address The interface to which JmDNS binds to.
     * @param name    The host name of the interface.
     */
    private void init(InetAddress address, String name) throws IOException
    {
        // A host name with "." is illegal.
        // so strip off everything and append .local.
        int idx = name.indexOf(".");
        if (idx > 0)
        {
            name = name.substring(0, idx);
        }
        name += ".local.";
        // localHost to IP address binding
        localHost = new HostInfo(address, name);

        cache = new DNSCache(100);

        listeners = Collections.synchronizedList(new ArrayList<DNSListener>());
        serviceListeners = new HashMap<String, List<ServiceListener>>();
        typeListeners = new ArrayList<ServiceTypeListener>();

        services = new Hashtable<String, ServiceInfo>(20);
        serviceTypes = new Hashtable<String, String>(20);

        // REMIND: If I could pass in a name for the Timer thread,
        //         I would pass 'JmDNS.Timer'.
        timer = new Timer();
        new RecordReaper().start();

        incomingListener = new Thread(
            new SocketListener(), "JmDNS.SocketListener");
        incomingListener.setDaemon(true);
        // Bind to multicast socket
        openMulticastSocket(localHost);
        start(services.values());
    }

    private void start(Collection<ServiceInfo> serviceInfos)
    {
        state = DNSState.PROBING_1;
        incomingListener.start();
        new Prober().start();
        for (ServiceInfo serviceInfo : serviceInfos)
        {
            try
            {
                registerService(new ServiceInfo(serviceInfo));
            }
            catch (Exception exception)
            {
                logger.warn("start() Registration exception ", exception);
            }
        }
    }

    private void openMulticastSocket(HostInfo hostInfo) throws IOException
    {
        if (group == null)
        {
            group = InetAddress.getByName(DNSConstants.MDNS_GROUP);
        }
        if (socket != null)
        {
            this.closeMulticastSocket();
        }
        socket = new MulticastSocket(DNSConstants.MDNS_PORT);
        if ((hostInfo != null) && (localHost.getInterface() != null))
        {
            socket.setNetworkInterface(hostInfo.getInterface());
        }
        socket.setTimeToLive(255);
        socket.joinGroup(group);
    }

    private void closeMulticastSocket()
    {
        if (logger.isDebugEnabled())
            logger.debug("closeMulticastSocket()");
        if (socket != null)
        {
            // close socket
            try
            {
                socket.leaveGroup(group);
                socket.close();
                if (incomingListener != null)
                {
                    incomingListener.join();
                }
            }
            catch (Exception exception)
            {
                logger.warn("closeMulticastSocket() Close socket exception ",
                            exception);
            }
            socket = null;
        }
    }

    // State machine
    /**
     * Sets the state and notifies all objects that wait on JmDNS.
     */
    synchronized void advanceState()
    {
        state = state.advance();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on JmDNS.
     */
    synchronized void revertState()
    {
        state = state.revert();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on JmDNS.
     */
    synchronized void cancel()
    {
        state = DNSState.CANCELED;
        notifyAll();
    }

    /**
     * Returns the current state of this info.
     */
    DNSState getState()
    {
        return state;
    }


    /**
     * Return the DNSCache associated with the cache variable
     */
    DNSCache getCache()
    {
        return cache;
    }

    /**
     * Return the HostName associated with this JmDNS instance.
     * Note: May not be the same as what started.  The host name is subject to
     * negotiation.
     * @return Return the HostName associated with this JmDNS instance.
     */
    public String getHostName()
    {
        return localHost.getName();
    }

    public HostInfo getLocalHost()
    {
        return localHost;
    }

    /**
     * Return the address of the interface to which this instance of JmDNS is
     * bound.
     * @return Return the address of the interface to which this instance
     *      of JmDNS is bound.
     * @throws java.io.IOException
     */
    public InetAddress getInterface()
        throws IOException
    {
        return socket.getInterface();
    }

    /**
     * Get service information. If the information is not cached, the method
     * will block until updated information is received.
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread.
     * You will make the user interface unresponsive.
     *
     * @param type fully qualified service type,
     *      such as <code>_http._tcp.local.</code> .
     * @param name unqualified service name, such as <code>foobar</code> .
     * @return null if the service information cannot be obtained
     */
    public ServiceInfo getServiceInfo(String type, String name)
    {
        return getServiceInfo(type, name, 3 * 1000);
    }

    /**
     * Get service information. If the information is not cached, the method
     * will block for the given timeout until updated information is received.
     * <p/>
     * Usage note: If you call this method from the AWT event dispatcher thread,
     * use a small timeout, or you will make the user interface unresponsive.
     *
     * @param type    full qualified service type,
     *      such as <code>_http._tcp.local.</code> .
     * @param name    unqualified service name, such as <code>foobar</code> .
     * @param timeout timeout in milliseconds
     * @return null if the service information cannot be obtained
     */
    public ServiceInfo getServiceInfo(String type, String name, int timeout)
    {
        ServiceInfo info = new ServiceInfo(type, name);
        new ServiceInfoResolver(info).start();

        try
        {
            long end = System.currentTimeMillis() + timeout;
            long delay;
            synchronized (info)
            {
                while (!info.hasData() &&
                        (delay = end - System.currentTimeMillis()) > 0)
                {
                    info.wait(delay);
                }
            }
        }
        catch (InterruptedException e)
        {
            // empty
        }

        return (info.hasData()) ? info : null;
    }

    /**
     * Request service information. The information about the service is
     * requested and the ServiceListener.resolveService method is called as soon
     * as it is available.
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread.
     * You will make the user interface unresponsive.
     *
     * @param type full qualified service type,
     *      such as <code>_http._tcp.local.</code> .
     * @param name unqualified service name, such as <code>foobar</code> .
     */
    public void requestServiceInfo(String type, String name)
    {
        requestServiceInfo(type, name, 3 * 1000);
    }

    /**
     * Request service information. The information about the service
     * is requested and the ServiceListener.resolveService method is
     * called as soon as it is available.
     *
     * @param type    full qualified service type,
     *      such as <code>_http._tcp.local.</code> .
     * @param name    unqualified service name, such as <code>foobar</code> .
     * @param timeout timeout in milliseconds
     */
    public void requestServiceInfo(String type, String name, int timeout)
    {
        registerServiceType(type);
        ServiceInfo info = new ServiceInfo(type, name);
        new ServiceInfoResolver(info).start();

        try
        {
            long end = System.currentTimeMillis() + timeout;
            long delay;
            synchronized (info)
            {
                while (!info.hasData() &&
                    (delay = end - System.currentTimeMillis()) > 0)
                {
                    info.wait(delay);
                }
            }
        }
        catch (InterruptedException e)
        {
            // empty
        }
    }

    void handleServiceResolved(ServiceInfo info)
    {
        List<ServiceListener> list = serviceListeners.get(info.type.toLowerCase());
        if (list != null)
        {
            ServiceEvent event =
                new ServiceEvent(this, info.type, info.getName(), info);
            // Iterate on a copy in case listeners will modify it
            List<ServiceListener>  listCopy
                = new ArrayList<ServiceListener> (list);
            for (ServiceListener serviceListener : listCopy)
                serviceListener.serviceResolved(event);
        }
    }

    /**
     * Listen for service types.
     *
     * @param listener listener for service types
     * @throws java.io.IOException
     */
    public void addServiceTypeListener(ServiceTypeListener listener)
        throws IOException
    {
        synchronized (this)
        {
            typeListeners.remove(listener);
            typeListeners.add(listener);
        }

        // report cached service types
        for (String serviceType : serviceTypes.values())
        {
            listener.serviceTypeAdded(
                new ServiceEvent(this, serviceType, null, null));
        }

        new TypeResolver().start();
    }

    /**
     * Remove listener for service types.
     *
     * @param listener listener for service types
     */
    public void removeServiceTypeListener(ServiceTypeListener listener)
    {
        synchronized (this)
        {
            typeListeners.remove(listener);
        }
    }

    /**
     * Listen for services of a given type. The type has to be a fully
     * qualified type name such as <code>_http._tcp.local.</code>.
     *
     * @param type     full qualified service type,
     *      such as <code>_http._tcp.local.</code>.
     * @param listener listener for service updates
     */
    public void addServiceListener(String type, ServiceListener listener)
    {
        String lotype = type.toLowerCase();
        removeServiceListener(lotype, listener);
        List<ServiceListener> list = null;
        synchronized (this)
        {
            list = serviceListeners.get(lotype);
            if (list == null)
            {
                list = Collections.synchronizedList(new LinkedList<ServiceListener>());
                serviceListeners.put(lotype, list);
            }
            list.add(listener);
        }

        // report cached service types
        for (Iterator<DNSCache.CacheNode> i = cache.iterator(); i.hasNext();)
        {
            for (DNSCache.CacheNode n = i.next(); n != null; n = n.next())
            {
                DNSRecord rec = (DNSRecord) n.getValue();
                if (rec.type == DNSConstants.TYPE_SRV)
                {
                    if (rec.name.endsWith(type))
                    {
                        listener.serviceAdded(
                            new ServiceEvent(
                                this,
                                type,
                                toUnqualifiedName(type, rec.name),
                                null));
                    }
                }
            }
        }
        new ServiceResolver(type).start();
    }

    /**
     * Remove listener for services of a given type.
     *
     * @param type of listener to be removed
     * @param listener listener for service updates
     */
    public void removeServiceListener(String type, ServiceListener listener)
    {
        type = type.toLowerCase();
        List<ServiceListener> list = serviceListeners.get(type);
        if (list != null)
        {
            synchronized (this)
            {
                list.remove(listener);
                if (list.size() == 0)
                {
                    serviceListeners.remove(type);
                }
            }
        }
    }

    /**
     * Register a service. The service is registered
     * for access by other jmdns clients.
     * The name of the service may be changed to make it unique.
     * @param info of service
     * @throws java.io.IOException
     */
    public void registerService(ServiceInfo info) throws IOException
    {
        registerServiceType(info.type);

        // bind the service to this address
        info.server = localHost.getName();
        info.addr = localHost.getAddress();

        synchronized (this)
        {
            makeServiceNameUnique(info);
            services.put(info.getQualifiedName().toLowerCase(), info);
        }

        new /*Service*/Prober().start();
        try
        {
            synchronized (info)
            {
                while (info.getState().compareTo(DNSState.ANNOUNCED) < 0)
                {
                    info.wait();
                }
            }
        }
        catch (InterruptedException e)
        {
            logger.error(e.getMessage(), e);
        }
        if (logger.isDebugEnabled())
            logger.debug("registerService() JmDNS registered service as " + info);
    }

    /**
     * Unregister a service. The service should have been registered.
     * @param info of service
     */
    public void unregisterService(ServiceInfo info)
    {
        synchronized (this)
        {
            services.remove(info.getQualifiedName().toLowerCase());
        }
        info.cancel();

        // Note: We use this lock object to synchronize on it.
        //       Synchronizing on another object (e.g. the ServiceInfo) does
        //       not make sense, because the sole purpose of the lock is to
        //       wait until the canceler has finished. If we synchronized on
        //       the ServiceInfo or on the Canceler, we would block all
        //       accesses to synchronized methods on that object. This is not
        //       what we want!
        Object lock = new Object();
        new Canceler(info, lock).start();

        // Remind: We get a deadlock here, if the Canceler does not run!
        try
        {
            synchronized (lock)
            {
                lock.wait();
            }
        }
        catch (InterruptedException e)
        {
            // empty
        }
    }

    /**
     * Unregister all services.
     */
    public void unregisterAllServices()
    {
        if (logger.isDebugEnabled())
            logger.debug("unregisterAllServices()");
        if (services.size() == 0)
        {
            return;
        }

        Collection<ServiceInfo> list;
        synchronized (this)
        {
            list = new LinkedList<ServiceInfo>(services.values());
            services.clear();
        }
        for (Iterator<ServiceInfo> iterator = list.iterator(); iterator.hasNext();)
        {
            iterator.next().cancel();
        }


        Object lock = new Object();
        new Canceler(list, lock).start();
        // Remind: We get a livelock here, if the Canceler does not run!
        try
        {
            synchronized (lock)
            {
                if (!closed)
                {
                    lock.wait();
                }
            }
        }
        catch (InterruptedException e)
        {
            // empty
        }
    }

    /**
     * Register a service type. If this service type was not already known,
     * all service listeners will be notified of the new service type.
     * Service types are automatically registered as they are discovered.
     * @param type of service
     */
    public void registerServiceType(String type)
    {
        String name = type.toLowerCase();
        if (serviceTypes.get(name) == null)
        {
            if ((type.indexOf("._mdns._udp.") < 0) &&
                !type.endsWith(".in-addr.arpa."))
            {
                Collection<ServiceTypeListener> list;
                synchronized (this)
                {
                    serviceTypes.put(name, type);
                    list = new LinkedList<ServiceTypeListener>(typeListeners);
                }
                for (ServiceTypeListener listener : list)
                    listener
                        .serviceTypeAdded(
                            new ServiceEvent(this, type, null, null));
            }
        }
    }

    /**
     * Generate a possibly unique name for a service using the information we
     * have in the cache.
     *
     * @return returns true, if the name of the service info had to be changed.
     */
    private boolean makeServiceNameUnique(ServiceInfo info)
    {
        String originalQualifiedName = info.getQualifiedName();
        long now = System.currentTimeMillis();

        boolean collision;
        do
        {
            collision = false;

            // Check for collision in cache
            for (DNSCache.CacheNode j = cache.find(
                                    info.getQualifiedName().toLowerCase());
                j != null;
                j = j.next())
            {
                DNSRecord a = (DNSRecord) j.getValue();
                if ((a.type == DNSConstants.TYPE_SRV) && !a.isExpired(now))
                {
                    DNSRecord.Service s = (DNSRecord.Service) a;
                    if (s.port != info.port || !s.server.equals(localHost.getName()))
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("makeServiceNameUnique() " +
                            "JmDNS.makeServiceNameUnique srv collision:" +
                            a + " s.server=" + s.server + " " +
                            localHost.getName() + " equals:" +
                            (s.server.equals(localHost.getName())));
                        info.setName(incrementName(info.getName()));
                        collision = true;
                        break;
                    }
                }
            }

            // Check for collision with other service infos published by JmDNS
            Object selfService =
                services.get(info.getQualifiedName().toLowerCase());
            if (selfService != null && selfService != info)
            {
                info.setName(incrementName(info.getName()));
                collision = true;
            }
        }
        while (collision);

        return !(originalQualifiedName.equals(info.getQualifiedName()));
    }

    String incrementName(String name)
    {
        try
        {
            int l = name.lastIndexOf('(');
            int r = name.lastIndexOf(')');
            if ((l >= 0) && (l < r))
            {
                name = name.substring(0, l) + "(" +
                    (Integer.parseInt(name.substring(l + 1, r)) + 1) + ")";
            }
            else
            {
                name += " (2)";
            }
        }
        catch (NumberFormatException e)
        {
            name += " (2)";
        }
        return name;
    }

    /**
     * Add a listener for a question. The listener will receive updates
     * of answers to the question as they arrive, or from the cache if they
     * are already available.
     * @param listener to be added
     * @param question - which the listener is responsible for.
     */
    public void addListener(DNSListener listener, DNSQuestion question)
    {
        long now = System.currentTimeMillis();

        // add the new listener
        synchronized (this)
        {
            listeners.add(listener);
        }

        // report existing matched records
        if (question != null)
        {
            for (DNSCache.CacheNode i = cache.find(question.name);
                i != null;
                i = i.next())
            {
                DNSRecord c = (DNSRecord) i.getValue();
                if (question.answeredBy(c) && !c.isExpired(now))
                {
                    listener.updateRecord(this, now, c);
                }
            }
        }
    }

    /**
     * Remove a listener from all outstanding questions.
     * The listener will no longer receive any updates.
     */
    void removeListener(DNSListener listener)
    {
        synchronized (this)
        {
            listeners.remove(listener);
        }
    }


    // Remind: Method updateRecord should receive a better name.
    /**
     * Notify all listeners that a record was updated.
     */
    void updateRecord(long now, DNSRecord rec)
    {
        // We do not want to block the entire DNS
        // while we are updating the record for each listener (service info)
        List<DNSListener> listenerList = null;
        synchronized (this)
        {
            listenerList = new ArrayList<DNSListener>(listeners);
        }

        //System.out.println("OUT OF MUTEX!!!!!");

        for (DNSListener listener : listenerList)
            listener.updateRecord(this, now, rec);

        if (rec.type == DNSConstants.TYPE_PTR ||
            rec.type == DNSConstants.TYPE_SRV)
        {
            List<ServiceListener> serviceListenerList = null;
            synchronized (this)
            {
                serviceListenerList = serviceListeners.get(rec.name.toLowerCase());
                // Iterate on a copy in case listeners will modify it
                if (serviceListenerList != null)
                {
                    serviceListenerList = new ArrayList<ServiceListener>(serviceListenerList);
                }
            }
            if (serviceListenerList != null)
            {
                boolean expired = rec.isExpired(now);
                String type = rec.getName();
                String name = ((DNSRecord.Pointer) rec).getAlias();
                // DNSRecord old = (DNSRecord)services.get(name.toLowerCase());
                if (!expired)
                {
                    // new record
                    ServiceEvent event =
                        new ServiceEvent(
                            this,
                            type,
                            toUnqualifiedName(type, name),
                            null);
                    for (Iterator<ServiceListener> iterator = serviceListenerList.iterator();
                        iterator.hasNext();)
                    {
                        iterator.next().serviceAdded(event);
                    }
                }
                else
                {
                    // expire record
                    ServiceEvent event =
                        new ServiceEvent(
                            this,
                            type,
                            toUnqualifiedName(type, name),
                            null);
                    for (Iterator<ServiceListener> iterator = serviceListenerList.iterator();
                        iterator.hasNext();)
                    {
                        iterator.next().serviceRemoved(event);
                    }
                }
            }
        }
    }

    /**
     * Handle an incoming response. Cache answers, and pass them on to
     * the appropriate questions.
     */
    private void handleResponse(DNSIncoming msg)
        throws IOException
    {
        long now = System.currentTimeMillis();

        boolean hostConflictDetected = false;
        boolean serviceConflictDetected = false;

        if (logger.isTraceEnabled())
            logger.trace("JMDNS/handleResponse received " +
            msg.answers.size()+ " messages");
        for (DNSRecord rec : msg.answers)
        {
          if (logger.isTraceEnabled())
              logger.trace("PRINT: "+ rec);
          //cache.add(rec);
        }

        for (DNSRecord rec : msg.answers)
        {
            boolean isInformative = false;
            boolean expired = rec.isExpired(now);

            if (logger.isTraceEnabled())
                logger.trace("JMDNS received : " + rec + " expired: "+expired);

            // update the cache
            DNSRecord c = (DNSRecord) cache.get(rec);
            if (c != null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("JMDNS has found "+rec+" in cache");
                if (expired)
                {
                    isInformative = true;
                    cache.remove(c);
                }
                else
                {
                    /* Special case for SIP Communicator.
                     * We want to be informed if a cache entry is modified
                     */
//                    if ((c.isUnique()
//                      && c.getType() == DNSConstants.TYPE_TXT
//                      && ((c.getClazz() & DNSConstants.CLASS_IN) != 0)))
//                            isInformative = true;
//                    c.resetTTL(rec);
//                    rec = c;
                    if (logger.isTraceEnabled())
                        logger.trace(
                        new Boolean(c.isUnique()).toString() +
                        c.getType()+c.getClazz() + "/" +
                        DNSConstants.TYPE_TXT + " "+DNSConstants.CLASS_IN);

                    if ((rec.isUnique()
                      && ((rec.getType() & DNSConstants.TYPE_TXT) != 0)
                      && ((rec.getClazz() & DNSConstants.CLASS_IN) != 0)))
                    {
                        if (logger.isTraceEnabled())
                            logger.trace("UPDATING CACHE !! ");
                        isInformative = true;
                        cache.remove(c);
                        cache.add(rec);
                    }
                    else
                    {
                        c.resetTTL(rec);
                        rec = c;
                    }
                }
            }
            else
            {
                if (!expired)
                {
                    isInformative = true;
                    if (logger.isTraceEnabled())
                        logger.trace("Adding "+rec+" to the cache");
                    cache.add(rec);
                }
            }
            switch (rec.type)
            {
                case DNSConstants.TYPE_PTR:
                    // handle _mdns._udp records
                    if (rec.getName().indexOf("._mdns._udp.") >= 0)
                    {
                        if (!expired &&
                            rec.name.startsWith("_services._mdns._udp."))
                        {
                            isInformative = true;
                            registerServiceType(((DNSRecord.Pointer)rec).alias);
                        }
                        continue;
                    }
                    registerServiceType(rec.name);
                    break;
            }


            if ((rec.getType() == DNSConstants.TYPE_A) ||
                (rec.getType() == DNSConstants.TYPE_AAAA))
            {
                hostConflictDetected |= rec.handleResponse(this);
            }
            else
            {
                serviceConflictDetected |= rec.handleResponse(this);
            }

            // notify the listeners
            if (isInformative)
            {
                updateRecord(now, rec);
            }


        }

        if (hostConflictDetected || serviceConflictDetected)
        {
            new Prober().start();
        }
    }

    /**
     * Handle an incoming query. See if we can answer any part of it
     * given our service infos.
     */
    private void handleQuery(DNSIncoming in, InetAddress addr, int port)
        throws IOException
    {
        // Track known answers
        boolean hostConflictDetected = false;
        boolean serviceConflictDetected = false;
        long expirationTime = System.currentTimeMillis() +
            DNSConstants.KNOWN_ANSWER_TTL;
        for (DNSRecord answer : in.answers)
        {
            if ((answer.getType() == DNSConstants.TYPE_A) ||
                (answer.getType() == DNSConstants.TYPE_AAAA))
            {
                hostConflictDetected |=
                    answer.handleQuery(this, expirationTime);
            }
            else
            {
                serviceConflictDetected |=
                    answer.handleQuery(this, expirationTime);
            }
        }

        if (plannedAnswer != null)
        {
            plannedAnswer.append(in);
        }
        else
        {
            if (in.isTruncated())
            {
                plannedAnswer = in;
            }

            new Responder(in, addr, port).start();
        }

        if (hostConflictDetected || serviceConflictDetected)
        {
            new Prober().start();
        }
    }

    /**
     * Add an answer to a question. Deal with the case when the
     * outgoing packet overflows
     */
    DNSOutgoing addAnswer(DNSIncoming in,
                          InetAddress addr,
                          int port,
                          DNSOutgoing out,
                          DNSRecord rec)
        throws IOException
    {
        if (out == null)
        {
            out = new DNSOutgoing(
                DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
        }
        try
        {
            out.addAnswer(in, rec);
        }
        catch (IOException e)
        {
            out.flags |= DNSConstants.FLAGS_TC;
            out.id = in.id;
            out.finish();
            send(out);

            out = new DNSOutgoing(
                DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
            out.addAnswer(in, rec);
        }
        return out;
    }


    /**
     * Send an outgoing multicast DNS message.
     */
    private void send(DNSOutgoing out) throws IOException
    {
        out.finish();
        if (!out.isEmpty())
        {
            DatagramPacket packet =
                new DatagramPacket(
                    out.data, out.off, group, DNSConstants.MDNS_PORT);

            try
            {
                DNSIncoming msg = new DNSIncoming(packet);
                if (logger.isTraceEnabled())
                    logger.trace("send() JmDNS out:" + msg.print(true));
            }
            catch (IOException exc)
            {
                logger.error(
                    "send(DNSOutgoing) - JmDNS can not parse what it sends!!!",
                    exc);
            }
            socket.send(packet);
        }
    }

    /**
     * Listen for multicast packets.
     */
    class SocketListener implements Runnable
    {
        public void run()
        {
            try
            {
                byte buf[] = new byte[DNSConstants.MAX_MSG_ABSOLUTE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (state != DNSState.CANCELED)
                {
                    packet.setLength(buf.length);
                    socket.receive(packet);
                    if (state == DNSState.CANCELED)
                    {
                        break;
                    }
                    try
                    {
                        if (localHost.shouldIgnorePacket(packet))
                        {
                            continue;
                        }

                        DNSIncoming msg = new DNSIncoming(packet);
                        if (logger.isTraceEnabled())
                            logger.trace("SocketListener.run() JmDNS in:" +
                            msg.print(true));

                        synchronized (ioLock)
                        {
                            if (msg.isQuery())
                            {
                                if (packet.getPort() != DNSConstants.MDNS_PORT)
                                {
                                    handleQuery(msg,
                                                packet.getAddress(),
                                                packet.getPort());
                                }
                                handleQuery(msg, group, DNSConstants.MDNS_PORT);
                            }
                            else
                            {
                                handleResponse(msg);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        logger.warn( "run() exception ", e);
                    }
                }
            }
            catch (IOException e)
            {
                if (state != DNSState.CANCELED)
                {
                    logger.warn( "run() exception ", e);
                    recover();
                }
            }
        }
    }


    /**
     * Periodicaly removes expired entries from the cache.
     */
    private class RecordReaper extends TimerTask
    {
        public void start()
        {
            timer.schedule( this,
                            DNSConstants.RECORD_REAPER_INTERVAL,
                            DNSConstants.RECORD_REAPER_INTERVAL);
        }

        @Override
        public void run()
        {
            synchronized (JmDNS.this)
            {
                if (state == DNSState.CANCELED)
                {
                    return;
                }
                if (logger.isTraceEnabled())
                    logger.trace("run() JmDNS reaping cache");

                // Remove expired answers from the cache
                // -------------------------------------
                // To prevent race conditions, we defensively copy all cache
                // entries into a list.
                List<DNSEntry> list = new ArrayList<DNSEntry>();
                synchronized (cache)
                {
                    for (Iterator<DNSCache.CacheNode> i = cache.iterator();
                            i.hasNext();)
                    {
                        for (DNSCache.CacheNode n = i.next();
                            n != null;
                            n = n.next())
                        {
                            list.add(n.getValue());
                        }
                    }
                }
                // Now, we remove them.
                long now = System.currentTimeMillis();
                for (Iterator<DNSEntry> i = list.iterator(); i.hasNext();)
                {
                    DNSRecord c = (DNSRecord)i.next();
                    if (c.isExpired(now))
                    {
                        updateRecord(now, c);
                        cache.remove(c);
                    }
                }
            }
        }
    }


    /**
     * The Prober sends three consecutive probes for all service infos
     * that needs probing as well as for the host name.
     * The state of each service info of the host name is advanced,
     * when a probe has been sent for it.
     * When the prober has run three times, it launches an Announcer.
     * <p/>
     * If a conflict during probes occurs, the affected service
     * infos (and affected host name) are taken away from the prober.
     * This eventually causes the prober tho cancel itself.
     */
    private class Prober extends TimerTask
    {
        /**
         * The state of the prober.
         */
        DNSState taskState = DNSState.PROBING_1;

        public Prober()
        {
            // Associate the host name to this, if it needs probing
            if (state == DNSState.PROBING_1)
            {
                task = this;
            }
            // Associate services to this, if they need probing
            synchronized (JmDNS.this)
            {
                for (Iterator<ServiceInfo> iterator = services.values().iterator();
                    iterator.hasNext();)
                {
                    ServiceInfo info = iterator.next();
                    if (info.getState() == DNSState.PROBING_1)
                    {
                        info.task = this;
                    }
                }
            }
        }


        public void start()
        {
            long now = System.currentTimeMillis();
            if (now - lastThrottleIncrement <
                DNSConstants.PROBE_THROTTLE_COUNT_INTERVAL)
            {
                throttle++;
            }
            else
            {
                throttle = 1;
            }
            lastThrottleIncrement = now;

            if (state == DNSState.ANNOUNCED &&
                throttle < DNSConstants.PROBE_THROTTLE_COUNT)
            {
                timer.schedule(this,
                           random.nextInt(1 + DNSConstants.PROBE_WAIT_INTERVAL),
                           DNSConstants.PROBE_WAIT_INTERVAL);
            }
            else
            {
                timer.schedule(this,
                           DNSConstants.PROBE_CONFLICT_INTERVAL,
                           DNSConstants.PROBE_CONFLICT_INTERVAL);
            }
        }

        @Override
        public boolean cancel()
        {
            // Remove association from host name to this
            if (task == this)
            {
                task = null;
            }

            // Remove associations from services to this
            synchronized (JmDNS.this)
            {
                for (Iterator<ServiceInfo> i = services.values().iterator();
                            i.hasNext();)
                {
                    ServiceInfo info = i.next();
                    if (info.task == this)
                    {
                        info.task = null;
                    }
                }
            }

            return super.cancel();
        }

        @Override
        public void run()
        {
            synchronized (ioLock)
            {
                DNSOutgoing out = null;
                try
                {
                    // send probes for JmDNS itself
                    if (state == taskState && task == this)
                    {
                        if (out == null)
                        {
                            out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                        }
                        out.addQuestion(
                            new DNSQuestion(
                                localHost.getName(),
                                DNSConstants.TYPE_ANY,
                                DNSConstants.CLASS_IN));
                        DNSRecord answer = localHost.getDNS4AddressRecord();
                        if (answer != null)
                        {
                            out.addAuthorativeAnswer(answer);
                        }
                        answer = localHost.getDNS6AddressRecord();
                        if (answer != null)
                        {
                            out.addAuthorativeAnswer(answer);
                        }
                        advanceState();
                    }
                    // send probes for services
                    // Defensively copy the services into a local list,
                    // to prevent race conditions with methods registerService
                    // and unregisterService.
                    List<ServiceInfo> list;
                    synchronized (JmDNS.this)
                    {
                        list = new LinkedList<ServiceInfo>(services.values());
                    }
                    for (Iterator<ServiceInfo> i = list.iterator(); i.hasNext();)
                    {
                        ServiceInfo info = i.next();

                        synchronized (info)
                        {
                            if (info.getState() == taskState &&
                                info.task == this)
                            {
                                info.advanceState();
                                if (logger.isDebugEnabled())
                                    logger.debug("run() JmDNS probing " +
                                    info.getQualifiedName() + " state " +
                                    info.getState());

                                if (out == null)
                                {
                                    out = new DNSOutgoing(
                                        DNSConstants.FLAGS_QR_QUERY);
                                    out.addQuestion(
                                        new DNSQuestion(
                                            info.getQualifiedName(),
                                            DNSConstants.TYPE_ANY,
                                            DNSConstants.CLASS_IN));
                                }
                                out.addAuthorativeAnswer(
                                    new DNSRecord.Service(
                                        info.getQualifiedName(),
                                        DNSConstants.TYPE_SRV,
                                        DNSConstants.CLASS_IN,
                                        DNSConstants.DNS_TTL,
                                        info.priority,
                                        info.weight,
                                        info.port,
                                        localHost.getName()));
                            }
                        }
                    }
                    if (out != null)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("run() JmDNS probing #" + taskState);
                        send(out);
                    }
                    else
                    {
                        // If we have nothing to send, another timer taskState
                        // ahead of us has done the job for us. We can cancel.
                        cancel();
                        return;
                    }
                }
                catch (Throwable e)
                {
                    logger.warn( "run() exception ", e);
                    recover();
                }

                taskState = taskState.advance();
                if (!taskState.isProbing())
                {
                    cancel();

                    new Announcer().start();
                }
            }
        }

    }

    /**
     * The Announcer sends an accumulated query of all announces, and advances
     * the state of all serviceInfos, for which it has sent an announce.
     * The Announcer also sends announcements and advances the state of JmDNS
     * itself.
     * <p/>
     * When the announcer has run two times, it finishes.
     */
    private class Announcer extends TimerTask
    {
        /**
         * The state of the announcer.
         */
        DNSState taskState = DNSState.ANNOUNCING_1;

        public Announcer()
        {
            // Associate host to this, if it needs announcing
            if (state == DNSState.ANNOUNCING_1)
            {
                task = this;
            }
            // Associate services to this, if they need announcing
            synchronized (JmDNS.this)
            {
                for (Iterator<ServiceInfo> s = services.values().iterator(); s.hasNext();)
                {
                    ServiceInfo info = s.next();
                    if (info.getState() == DNSState.ANNOUNCING_1)
                    {
                        info.task = this;
                    }
                }
            }
        }

        public void start()
        {
            timer.schedule(this,
                DNSConstants.ANNOUNCE_WAIT_INTERVAL,
                DNSConstants.ANNOUNCE_WAIT_INTERVAL);
        }

        @Override
        public boolean cancel()
        {
            // Remove association from host to this
            if (task == this)
            {
                task = null;
            }

            // Remove associations from services to this
            synchronized (JmDNS.this)
            {
                for (Iterator<ServiceInfo> i = services.values().iterator();
                        i.hasNext();)
                {
                    ServiceInfo info = i.next();
                    if (info.task == this)
                    {
                        info.task = null;
                    }
                }
            }

            return super.cancel();
        }

        @Override
        public void run()
        {
            DNSOutgoing out = null;
            try
            {
                // send probes for JmDNS itself
                if (state == taskState)
                {
                    if (out == null)
                    {
                        out = new DNSOutgoing(
                            DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                    }
                    DNSRecord answer = localHost.getDNS4AddressRecord();
                    if (answer != null)
                    {
                        out.addAnswer(answer, 0);
                    }
                    answer = localHost.getDNS6AddressRecord();
                    if (answer != null)
                    {
                        out.addAnswer(answer, 0);
                    }
                    advanceState();
                }
                // send announces for services
                // Defensively copy the services into a local list,
                // to prevent race conditions with methods registerService
                // and unregisterService.
                List<ServiceInfo> list;
                synchronized (JmDNS.this)
                {
                    list = new ArrayList<ServiceInfo>(services.values());
                }
                for (Iterator<ServiceInfo> i = list.iterator(); i.hasNext();)
                {
                    ServiceInfo info = i.next();
                    synchronized (info)
                    {
                        if (info.getState() == taskState && info.task == this)
                        {
                            info.advanceState();
                            if (logger.isDebugEnabled())
                                logger.debug("run() JmDNS announcing " +
                                info.getQualifiedName() +
                                " state " + info.getState());

                            if (out == null)
                            {
                                out = new DNSOutgoing(
                                    DNSConstants.FLAGS_QR_RESPONSE |
                                    DNSConstants.FLAGS_AA);
                            }
                            out.addAnswer(
                                new DNSRecord.Pointer(
                                    info.type,
                                    DNSConstants.TYPE_PTR,
                                    DNSConstants.CLASS_IN,
                                    DNSConstants.DNS_TTL,
                                    info.getQualifiedName()), 0);
                            out.addAnswer(
                                new DNSRecord.Service(
                                    info.getQualifiedName(),
                                    DNSConstants.TYPE_SRV,
                                    DNSConstants.CLASS_IN,
                                    DNSConstants.DNS_TTL,
                                    info.priority,
                                    info.weight,
                                    info.port,
                                    localHost.getName()), 0);
                            out.addAnswer(
                                new DNSRecord.Text(
                                    info.getQualifiedName(),
                                    DNSConstants.TYPE_TXT,
                                    DNSConstants.CLASS_IN,
                                    DNSConstants.DNS_TTL,
                                    info.text), 0);
                        }
                    }
                }
                if (out != null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("run() JmDNS announcing #" + taskState);
                    send(out);
                }
                else
                {
                    // If we have nothing to send, another timer taskState ahead
                    // of us has done the job for us. We can cancel.
                    cancel();
                }
            }
            catch (Throwable e)
            {
                logger.warn( "run() exception ", e);
                recover();
            }

            taskState = taskState.advance();
            if (!taskState.isAnnouncing())
            {
                cancel();

                new Renewer().start();
            }
        }
    }

    /**
     * The Renewer is there to send renewal announcment
     * when the record expire for ours infos.
     */
    private class Renewer extends TimerTask
    {
        /**
         * The state of the announcer.
         */
        DNSState taskState = DNSState.ANNOUNCED;

        public Renewer()
        {
            // Associate host to this, if it needs renewal
            if (state == DNSState.ANNOUNCED)
            {
                task = this;
            }
            // Associate services to this, if they need renewal
            synchronized (JmDNS.this)
            {
                for (Iterator<ServiceInfo> s = services.values().iterator(); s.hasNext();)
                {
                    ServiceInfo info = s.next();
                    if (info.getState() == DNSState.ANNOUNCED)
                    {
                        info.task = this;
                    }
                }
            }
        }

        public void start()
        {
            timer.schedule(this,
                DNSConstants.ANNOUNCED_RENEWAL_TTL_INTERVAL,
                DNSConstants.ANNOUNCED_RENEWAL_TTL_INTERVAL);
        }

        @Override
        public boolean cancel()
        {
            // Remove association from host to this
            if (task == this)
            {
                task = null;
            }

            // Remove associations from services to this
            synchronized (JmDNS.this)
            {
                for (Iterator<ServiceInfo> i = services.values().iterator();
                        i.hasNext();)
                {
                    ServiceInfo info = i.next();
                    if (info.task == this)
                    {
                        info.task = null;
                    }
                }
            }

            return super.cancel();
        }

        @Override
        public void run()
        {
            DNSOutgoing out = null;
            try
            {
                // send probes for JmDNS itself
                if (state == taskState)
                {
                    if (out == null)
                    {
                        out = new DNSOutgoing(
                            DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                    }
                    DNSRecord answer = localHost.getDNS4AddressRecord();
                    if (answer != null)
                    {
                        out.addAnswer(answer, 0);
                    }
                    answer = localHost.getDNS6AddressRecord();
                    if (answer != null)
                    {
                        out.addAnswer(answer, 0);
                    }
                    advanceState();
                }
                // send announces for services
                // Defensively copy the services into a local list,
                // to prevent race conditions with methods registerService
                // and unregisterService.
                List<ServiceInfo> list;
                synchronized (JmDNS.this)
                {
                    list = new ArrayList<ServiceInfo>(services.values());
                }
                for (Iterator<ServiceInfo> i = list.iterator(); i.hasNext();)
                {
                    ServiceInfo info = i.next();
                    synchronized (info)
                    {
                        if (info.getState() == taskState && info.task == this)
                        {
                            info.advanceState();
                            if (logger.isDebugEnabled())
                                logger.debug("run() JmDNS announced " +
                                info.getQualifiedName() + " state " + info.getState());

                            if (out == null)
                            {
                                out = new DNSOutgoing(
                                    DNSConstants.FLAGS_QR_RESPONSE |
                                    DNSConstants.FLAGS_AA);
                            }
                            out.addAnswer(
                                new DNSRecord.Pointer(
                                    info.type,
                                    DNSConstants.TYPE_PTR,
                                    DNSConstants.CLASS_IN,
                                    DNSConstants.DNS_TTL,
                                    info.getQualifiedName()), 0);
                            out.addAnswer(
                                new DNSRecord.Service(
                                    info.getQualifiedName(),
                                    DNSConstants.TYPE_SRV,
                                    DNSConstants.CLASS_IN,
                                    DNSConstants.DNS_TTL,
                                    info.priority,
                                    info.weight,
                                    info.port,
                                    localHost.getName()), 0);
                            out.addAnswer(
                                new DNSRecord.Text(
                                info.getQualifiedName(),
                                DNSConstants.TYPE_TXT,
                                DNSConstants.CLASS_IN,
                                DNSConstants.DNS_TTL,
                                info.text), 0);
                        }
                    }
                }
                if (out != null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("run() JmDNS announced");
                    send(out);
                }
                else
                {
                    // If we have nothing to send, another timer taskState ahead
                    // of us has done the job for us. We can cancel.
                    cancel();
                }
            }
            catch (Throwable e)
            {
                logger.warn( "run() exception ", e);
                recover();
            }

            taskState = taskState.advance();
            if (!taskState.isAnnounced())
            {
                cancel();

            }
        }
    }

    /**
     * The Responder sends a single answer for the specified service infos
     * and for the host name.
     */
    private class Responder extends TimerTask
    {
        private DNSIncoming in;
        private InetAddress addr;
        private int port;

        public Responder(DNSIncoming in, InetAddress addr, int port)
        {
            this.in = in;
            this.addr = addr;
            this.port = port;
        }

        public void start()
        {
            // According to draft-cheshire-dnsext-multicastdns.txt
            // chapter "8 Responding":
            // We respond immediately if we know for sure, that we are
            // the only one who can respond to the query.
            // In all other cases, we respond within 20-120 ms.
            //
            // According to draft-cheshire-dnsext-multicastdns.txt
            // chapter "7.2 Multi-Packet Known Answer Suppression":
            // We respond after 20-120 ms if the query is truncated.

            boolean iAmTheOnlyOne = true;
            for (DNSEntry entry : in.questions)
            {
                if (entry instanceof DNSQuestion)
                {
                    DNSQuestion q = (DNSQuestion) entry;
                    if (logger.isTraceEnabled())
                        logger.trace("start() question=" + q);
                    iAmTheOnlyOne &= (q.type == DNSConstants.TYPE_SRV
                            || q.type == DNSConstants.TYPE_TXT
                            || q.type == DNSConstants.TYPE_A
                            || q.type == DNSConstants.TYPE_AAAA
                            || localHost.getName().equalsIgnoreCase(q.name)
                            || services.containsKey(q.name.toLowerCase()));
                    if (!iAmTheOnlyOne)
                    {
                        break;
                    }
                }
            }
            int delay = (iAmTheOnlyOne && !in.isTruncated()) ?
                0 :
                DNSConstants.RESPONSE_MIN_WAIT_INTERVAL +
                    random.nextInt(
                        DNSConstants.RESPONSE_MAX_WAIT_INTERVAL -
                        DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + 1) -
                    in.elapseSinceArrival();
            if (delay < 0)
            {
                delay = 0;
            }
            if (logger.isTraceEnabled())
                logger.trace("start() Responder chosen delay=" + delay);
            timer.schedule(this, delay);
        }

        @Override
        public void run()
        {
            synchronized (ioLock)
            {
                if (plannedAnswer == in)
                {
                    plannedAnswer = null;
                }

                // We use these sets to prevent duplicate records
                // FIXME - This should be moved into DNSOutgoing
                HashSet<DNSQuestion> questions = new HashSet<DNSQuestion>();
                HashSet<DNSRecord> answers = new HashSet<DNSRecord>();


                if (state == DNSState.ANNOUNCED)
                {
                    try
                    {
                        boolean isUnicast = (port != DNSConstants.MDNS_PORT);


                        // Answer questions
                        for (Iterator<DNSEntry> iterator = in.questions.iterator();
                            iterator.hasNext();)
                        {
                            DNSEntry entry = iterator.next();
                            if (entry instanceof DNSQuestion)
                            {
                                DNSQuestion q = (DNSQuestion) entry;

                                // for unicast responses the question
                                // must be included
                                if (isUnicast)
                                {
                                    //out.addQuestion(q);
                                    questions.add(q);
                                }

                                int type = q.type;
                                if (type == DNSConstants.TYPE_ANY ||
                                    type == DNSConstants.TYPE_SRV)
                                { // I ama not sure of why there is a special
                                  // case here [PJYF Oct 15 2004]
                                    if (localHost.getName().
                                        equalsIgnoreCase(q.getName()))
                                    {
                                        // type = DNSConstants.TYPE_A;
                                        DNSRecord answer =
                                            localHost.getDNS4AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        answer = localHost.getDNS6AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        type = DNSConstants.TYPE_IGNORE;
                                    }
                                    else
                                    {
                                        if (serviceTypes.containsKey(
                                                q.getName().toLowerCase()))
                                        {
                                            type = DNSConstants.TYPE_PTR;
                                        }
                                    }
                                }

                                switch (type)
                                {
                                    case DNSConstants.TYPE_A:
                                    {
                                        // Answer a query for a domain name
                                        //out = addAnswer( in, addr, port, out, host );
                                        DNSRecord answer =
                                            localHost.getDNS4AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        break;
                                    }
                                    case DNSConstants.TYPE_AAAA:
                                    {
                                        // Answer a query for a domain name
                                        DNSRecord answer =
                                            localHost.getDNS6AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        break;
                                    }
                                    case DNSConstants.TYPE_PTR:
                                    {
                                        // Answer a query for services of a given type

                                        // find matching services
                                        for (Iterator<ServiceInfo> serviceIterator =
                                                    services.values().iterator();
                                            serviceIterator.hasNext();)
                                        {
                                            ServiceInfo info = serviceIterator.next();
                                            if (info.getState() == DNSState.ANNOUNCED)
                                            {
                                                if (q.name.equalsIgnoreCase(info.type))
                                                {
                                                    DNSRecord answer =
                                                        localHost.getDNS4AddressRecord();
                                                    if (answer != null)
                                                    {
                                                        answers.add(answer);
                                                    }
                                                    answer =
                                                        localHost.getDNS6AddressRecord();
                                                    if (answer != null)
                                                    {
                                                        answers.add(answer);
                                                    }
                                                    answers.add(
                                                        new DNSRecord.Pointer(
                                                            info.type,
                                                            DNSConstants.TYPE_PTR,
                                                            DNSConstants.CLASS_IN,
                                                            DNSConstants.DNS_TTL,
                                                            info.getQualifiedName()));
                                                    answers.add(
                                                        new DNSRecord.Service(
                                                            info.getQualifiedName(),
                                                            DNSConstants.TYPE_SRV,
                                                            DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                                                            DNSConstants.DNS_TTL,
                                                            info.priority,
                                                            info.weight,
                                                            info.port,
                                                            localHost.getName()));
                                                    answers.add(
                                                        new DNSRecord.Text(
                                                            info.getQualifiedName(),
                                                            DNSConstants.TYPE_TXT,
                                                            DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                                                            DNSConstants.DNS_TTL,
                                                            info.text));
                                                }
                                            }
                                        }
                                        if (q.name.equalsIgnoreCase("_services._mdns._udp.local."))
                                        {
                                            for (Iterator<String> serviceTypeIterator = serviceTypes.values().iterator();
                                                serviceTypeIterator.hasNext();)
                                            {
                                                answers.add(
                                                    new DNSRecord.Pointer(
                                                        "_services._mdns._udp.local.",
                                                        DNSConstants.TYPE_PTR,
                                                        DNSConstants.CLASS_IN,
                                                        DNSConstants.DNS_TTL,
                                                        serviceTypeIterator.next()));
                                            }
                                        }
                                        break;
                                    }
                                    case DNSConstants.TYPE_SRV:
                                    case DNSConstants.TYPE_ANY:
                                    case DNSConstants.TYPE_TXT:
                                    {
                                        ServiceInfo info = services.get(q.name.toLowerCase());
                                        if (info != null &&
                                            info.getState() == DNSState.ANNOUNCED)
                                        {
                                            DNSRecord answer =
                                                localHost.getDNS4AddressRecord();
                                            if (answer != null)
                                            {
                                                answers.add(answer);
                                            }
                                            answer =
                                                localHost.getDNS6AddressRecord();
                                            if (answer != null)
                                            {
                                                answers.add(answer);
                                            }
                                            answers.add(
                                                new DNSRecord.Pointer(
                                                    info.type,
                                                    DNSConstants.TYPE_PTR,
                                                    DNSConstants.CLASS_IN,
                                                    DNSConstants.DNS_TTL,
                                                    info.getQualifiedName()));
                                            answers.add(
                                                new DNSRecord.Service(
                                                    info.getQualifiedName(),
                                                    DNSConstants.TYPE_SRV,
                                                    DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                                                    DNSConstants.DNS_TTL,
                                                    info.priority,
                                                    info.weight,
                                                    info.port,
                                                    localHost.getName()));
                                            answers.add(
                                                new DNSRecord.Text(
                                                    info.getQualifiedName(),
                                                    DNSConstants.TYPE_TXT,
                                                    DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                                                    DNSConstants.DNS_TTL,
                                                    info.text));
                                        }
                                        break;
                                    }
                                    default :
                                    {
                                        //System.out.println("JmDNSResponder.unhandled query:"+q);
                                        break;
                                    }
                                }
                            }
                        }


                        // remove known answers, if the ttl is at least half of
                        // the correct value. (See Draft Cheshire chapter 7.1.).
                        for (DNSRecord knownAnswer : in.answers)
                        {
                            if (knownAnswer.ttl > DNSConstants.DNS_TTL / 2 &&
                                answers.remove(knownAnswer))
                            {
                                if (logger.isDebugEnabled())
                                    logger.debug(
                                    "JmDNS Responder Known Answer Removed");
                            }
                        }


                        // responde if we have answers
                        if (answers.size() != 0)
                        {
                            if (logger.isDebugEnabled())
                                logger.debug("run() JmDNS responding");
                            DNSOutgoing out = null;
                            if (isUnicast)
                            {
                                out = new DNSOutgoing(
                                    DNSConstants.FLAGS_QR_RESPONSE
                                    | DNSConstants.FLAGS_AA,
                                    false);
                            }

                            for (Iterator<DNSQuestion> i = questions.iterator();
                                    i.hasNext();)
                            {
                                out.addQuestion(i.next());
                            }
                            for (Iterator<DNSRecord> i = answers.iterator();
                                    i.hasNext();)
                            {
                                out = addAnswer(in, addr, port, out, i.next());
                            }
                            send(out);
                        }
                        this.cancel();
                    }
                    catch (Throwable e)
                    {
                        logger.warn( "run() exception ", e);
                        close();
                    }
                }
            }
        }
    }

    /**
     * Helper class to resolve service types.
     * <p/>
     * The TypeResolver queries three times consecutively for service types, and then
     * removes itself from the timer.
     * <p/>
     * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
     */
    private class TypeResolver extends TimerTask
    {
        public void start()
        {
            timer.schedule(this,
                           DNSConstants.QUERY_WAIT_INTERVAL,
                           DNSConstants.QUERY_WAIT_INTERVAL);
        }

        /**
         * Counts the number of queries that were sent.
         */
        int count = 0;

        @Override
        public void run()
        {
            try
            {
                if (state == DNSState.ANNOUNCED)
                {
                    if (count++ < 3)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("run() JmDNS querying type");
                        DNSOutgoing out =
                            new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                        out.addQuestion(
                            new DNSQuestion(
                                "_services._mdns._udp.local.",
                                DNSConstants.TYPE_PTR,
                                DNSConstants.CLASS_IN));
                        for (String serviceType : serviceTypes.values())
                        {
                            out.addAnswer(
                                new DNSRecord.Pointer(
                                    "_services._mdns._udp.local.",
                                    DNSConstants.TYPE_PTR,
                                    DNSConstants.CLASS_IN,
                                    DNSConstants.DNS_TTL,
                                    serviceType), 0);
                        }
                        send(out);
                    }
                    else
                    {
                        // After three queries, we can quit.
                        this.cancel();
                    }
                }
                else
                {
                    if (state == DNSState.CANCELED)
                    {
                        this.cancel();
                    }
                }
            }
            catch (Throwable e)
            {
                logger.warn( "run() exception ", e);
                recover();
            }
        }
    }

    /**
     * The ServiceResolver queries three times consecutively for services of
     * a given type, and then removes itself from the timer.
     * <p/>
     * The ServiceResolver will run only if JmDNS is in state ANNOUNCED.
     * REMIND: Prevent having multiple service resolvers for the same type in the
     * timer queue.
     */
    private class ServiceResolver extends TimerTask
    {
        /**
         * Counts the number of queries being sent.
         */
        int count = 0;
        private String type;

        public ServiceResolver(String type)
        {
            this.type = type;
        }

        public void start()
        {
            timer.schedule(this,
                           DNSConstants.QUERY_WAIT_INTERVAL,
                           DNSConstants.QUERY_WAIT_INTERVAL);
        }

        @Override
        public void run()
        {
            try
            {
                if (state == DNSState.ANNOUNCED)
                {
                    if (count++ < 3)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("run() JmDNS querying service");
                        long now = System.currentTimeMillis();
                        DNSOutgoing out =
                            new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                        out.addQuestion(
                            new DNSQuestion(
                                type,
                                DNSConstants.TYPE_PTR,
                                DNSConstants.CLASS_IN));
                        for (Iterator<ServiceInfo> s = services.values().iterator(); s.hasNext();)
                        {
                            final ServiceInfo info = s.next();
                            try
                            {
                                out.addAnswer(
                                    new DNSRecord.Pointer(
                                        info.type,
                                        DNSConstants.TYPE_PTR,
                                        DNSConstants.CLASS_IN,
                                        DNSConstants.DNS_TTL,
                                        info.getQualifiedName()), now);
                            }
                            catch (IOException ee)
                            {
                                break;
                            }
                        }
                        send(out);
                    }
                    else
                    {
                        // After three queries, we can quit.
                        this.cancel();
                    }
                }
                else
                {
                    if (state == DNSState.CANCELED)
                    {
                        this.cancel();
                    }
                }
            }
            catch (Throwable e)
            {
                logger.warn( "run() exception ", e);
                recover();
            }
        }
    }

    /**
     * The ServiceInfoResolver queries up to three times consecutively for
     * a service info, and then removes itself from the timer.
     * <p/>
     * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED.
     * REMIND: Prevent having multiple service resolvers for the same info in the
     * timer queue.
     */
    private class ServiceInfoResolver extends TimerTask
    {
        /**
         * Counts the number of queries being sent.
         */
        int count = 0;
        private ServiceInfo info;

        public ServiceInfoResolver(ServiceInfo info)
        {
            this.info = info;
            info.dns = JmDNS.this;
            addListener(info,
                new DNSQuestion(
                    info.getQualifiedName(),
                    DNSConstants.TYPE_ANY,
                    DNSConstants.CLASS_IN));
        }

        public void start()
        {
            timer.schedule(this,
                           DNSConstants.QUERY_WAIT_INTERVAL,
                           DNSConstants.QUERY_WAIT_INTERVAL);
        }

        @Override
        public void run()
        {
            try
            {
                if (state == DNSState.ANNOUNCED)
                {
                    if (count++ < 3 && !info.hasData())
                    {
                        long now = System.currentTimeMillis();
                        DNSOutgoing out =
                            new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                        out.addQuestion(
                            new DNSQuestion(
                                info.getQualifiedName(),
                                DNSConstants.TYPE_SRV,
                                DNSConstants.CLASS_IN));
                        out.addQuestion(
                            new DNSQuestion(
                                info.getQualifiedName(),
                                DNSConstants.TYPE_TXT,
                                DNSConstants.CLASS_IN));
                        if (info.server != null)
                        {
                            out.addQuestion(
                                new DNSQuestion(
                                    info.server,
                                    DNSConstants.TYPE_A,
                                    DNSConstants.CLASS_IN));
                        }
                        out.addAnswer((DNSRecord) cache.get(
                            info.getQualifiedName(),
                            DNSConstants.TYPE_SRV,
                            DNSConstants.CLASS_IN), now);
                        out.addAnswer((DNSRecord) cache.get(
                            info.getQualifiedName(),
                            DNSConstants.TYPE_TXT,
                            DNSConstants.CLASS_IN), now);
                        if (info.server != null)
                        {
                            out.addAnswer((DNSRecord) cache.get(
                                info.server,
                                DNSConstants.TYPE_A,
                                DNSConstants.CLASS_IN), now);
                        }
                        send(out);
                    }
                    else
                    {
                        // After three queries, we can quit.
                        this.cancel();
                        removeListener(info);
                    }
                }
                else
                {
                    if (state == DNSState.CANCELED)
                    {
                        this.cancel();
                        removeListener(info);
                    }
                }
            }
            catch (Throwable e)
            {
                logger.warn( "run() exception ", e);
                recover();
            }
        }
    }

    /**
     * The Canceler sends two announces with TTL=0 for the specified services.
     */
    /* TODO: Clarify whether 2 or 3 announces should be sent. The header says 2,
     *  run() uses the (misleading) ++count < 3 (while all other tasks use count++ < 3)
     *  and the comment in the else block in run() says: "After three successful..."
     */
    public class Canceler extends TimerTask
    {
        /**
         * Counts the number of announces being sent.
         */
        int count = 0;
        /**
         * The services that need cancelling.
         * Note: We have to use a local variable here, because the services
         * that are canceled, are removed immediately from variable JmDNS.services.
         */
        private ServiceInfo[] infos;
        /**
         * We call notifyAll() on the lock object, when we have canceled the
         * service infos.
         * This is used by method JmDNS.unregisterService() and
         * JmDNS.unregisterAllServices, to ensure that the JmDNS
         * socket stays open until the Canceler has canceled all services.
         * <p/>
         * Note: We need this lock, because ServiceInfos do the transition from
         * state ANNOUNCED to state CANCELED before we get here. We could get
         * rid of this lock, if we added a state named CANCELLING to DNSState.
         */
        private Object lock;
        int ttl = 0;

        public Canceler(ServiceInfo info, Object lock)
        {
            this.infos = new ServiceInfo[]{info};
            this.lock = lock;
            addListener(info,
                        new DNSQuestion(
                            info.getQualifiedName(),
                            DNSConstants.TYPE_ANY,
                            DNSConstants.CLASS_IN));
        }

        public Canceler(ServiceInfo[] infos, Object lock)
        {
            this.infos = infos;
            this.lock = lock;
        }

        public Canceler(Collection<ServiceInfo> infos, Object lock)
        {
            this.infos = infos.toArray(new ServiceInfo[infos.size()]);
            this.lock = lock;
        }

        public void start()
        {
            timer.schedule(this, 0, DNSConstants.ANNOUNCE_WAIT_INTERVAL);
        }

        @Override
        public void run()
        {
            try
            {
                if (++count < 3)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("run() JmDNS canceling service");
                    // announce the service
                    //long now = System.currentTimeMillis();
                    DNSOutgoing out =
                        new DNSOutgoing(
                            DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                    for (int i = 0; i < infos.length; i++)
                    {
                        ServiceInfo info = infos[i];
                        out.addAnswer(
                            new DNSRecord.Pointer(
                                info.type,
                                DNSConstants.TYPE_PTR,
                                DNSConstants.CLASS_IN,
                                ttl,
                                info.getQualifiedName()), 0);
                        out.addAnswer(
                            new DNSRecord.Service(
                                info.getQualifiedName(),
                                DNSConstants.TYPE_SRV,
                                DNSConstants.CLASS_IN,
                                ttl,
                                info.priority,
                                info.weight,
                                info.port,
                                localHost.getName()), 0);
                        out.addAnswer(
                            new DNSRecord.Text(
                                info.getQualifiedName(),
                                DNSConstants.TYPE_TXT,
                                DNSConstants.CLASS_IN,
                                ttl,
                                info.text), 0);
                        DNSRecord answer = localHost.getDNS4AddressRecord();
                        if (answer != null)
                        {
                            out.addAnswer(answer, 0);
                        }
                        answer = localHost.getDNS6AddressRecord();
                        if (answer != null)
                        {
                            out.addAnswer(answer, 0);
                        }
                    }
                    send(out);
                }
                else
                {
                    // After three successful announcements, we are finished.
                    synchronized (lock)
                    {
                        closed=true;
                        lock.notifyAll();
                    }
                    this.cancel();
                }
            }
            catch (Throwable e)
            {
                logger.warn( "run() exception ", e);
                recover();
            }
        }
    }

    /**
     * Recover jmdns when there is an error.
     */
    protected void recover()
    {
        if (logger.isDebugEnabled())
            logger.debug("recover()");
        // We have an IO error so lets try to recover if anything happens lets close it.
        // This should cover the case of the IP address changing under our feet
        if (DNSState.CANCELED != state)
        {
            synchronized (this)
            { // Synchronize only if we are not already in process to prevent dead locks
                //
                if (logger.isDebugEnabled())
                    logger.debug("recover() Cleanning up");
                // Stop JmDNS
                state = DNSState.CANCELED; // This protects against recursive calls

                // We need to keep a copy for reregistration
                Collection<ServiceInfo> oldServiceInfos = new ArrayList<ServiceInfo>(services.values());

                // Cancel all services
                unregisterAllServices();
                disposeServiceCollectors();
                //
                // close multicast socket
                closeMulticastSocket();
                //
                cache.clear();
                if (logger.isDebugEnabled())
                    logger.debug("recover() All is clean");
                //
                // All is clear now start the services
                //
                try
                {
                    openMulticastSocket(localHost);
                    start(oldServiceInfos);
                }
                catch (Exception exception)
                {
                    logger.warn(
                        "recover() Start services exception ", exception);
                }
                logger.warn( "recover() We are back!");
            }
        }
    }

    /**
     * Close down jmdns. Release all resources and unregister all services.
     */
    public void close()
    {
        if (state != DNSState.CANCELED)
        {
            synchronized (this)
            { // Synchronize only if we are not already in process to prevent dead locks
                // Stop JmDNS
                state = DNSState.CANCELED; // This protects against recursive calls

                unregisterAllServices();
                disposeServiceCollectors();

                // close socket
                closeMulticastSocket();

                // Stop the timer
                timer.cancel();
            }
        }
    }

    /**
     * List cache entries, for debugging only.
     */
    void print()
    {
        if (logger.isInfoEnabled())
            logger.info("---- cache ----\n");
        cache.print();
        if (logger.isInfoEnabled())
            logger.info("\n");
    }

    /**
     * List Services and serviceTypes.
     * Debugging Only
     */

    public void printServices()
    {
        if (logger.isInfoEnabled())
            logger.info(toString());
    }

    @Override
    public String toString()
    {
        StringBuffer aLog = new StringBuffer();
        aLog.append("\t---- Services -----");
        if (services != null)
        {
            for (Map.Entry<String, ServiceInfo> entry : services.entrySet())
            {
                aLog.append("\n\t\tService: " + entry.getKey() + ": "
                    + entry.getValue());
            }
        }
        aLog.append("\n");
        aLog.append("\t---- Types ----");
        if (serviceTypes != null)
        {
            for (Map.Entry<String, String> entry : serviceTypes.entrySet())
            {
                aLog.append("\n\t\tType: " + entry.getKey() + ": "
                    + entry.getValue());
            }
        }
        aLog.append("\n");
        aLog.append(cache.toString());
        aLog.append("\n");
        aLog.append("\t---- Service Collectors ----");
        if (serviceCollectors != null)
        {
            synchronized (serviceCollectors)
            {
                for (Map.Entry<String, ServiceCollector> entry
                        : serviceCollectors.entrySet())
                {
                    aLog.append("\n\t\tService Collector: " + entry.getKey()
                        + ": " + entry.getValue());
                }
                serviceCollectors.clear();
            }
        }
        return aLog.toString();
    }

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type Service type name, such as <code>_http._tcp.local.</code>.
     * @return An array of service instance names.
     */
    public ServiceInfo[] list(String type)
    {
        // Implementation note: The first time a list for a given type is
        // requested, a ServiceCollector is created which collects service
        // infos. This greatly speeds up the performance of subsequent calls
        // to this method. The caveats are, that 1) the first call to this method
        // for a given type is slow, and 2) we spawn a ServiceCollector
        // instance for each service type which increases network traffic a
        // little.

        ServiceCollector collector;

        boolean newCollectorCreated;
        synchronized (serviceCollectors)
        {
            collector = serviceCollectors.get(type);
            if (collector == null)
            {
                collector = new ServiceCollector(type);
                serviceCollectors.put(type, collector);
                addServiceListener(type, collector);
                newCollectorCreated = true;
            }
            else
            {
                newCollectorCreated = false;
            }
        }

        // After creating a new ServiceCollector, we collect service infos for
        // 200 milliseconds. This should be enough time, to get some service
        // infos from the network.
        if (newCollectorCreated)
        {
            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException e)
            {
            }
        }

        return collector.list();
    }

    /**
     * This method disposes all ServiceCollector instances which have been
     * created by calls to method <code>list(type)</code>.
     *
     * @see #list
     */
    private void disposeServiceCollectors()
    {
        if (logger.isDebugEnabled())
            logger.debug("disposeServiceCollectors()");
        synchronized (serviceCollectors)
        {
            for (Iterator<ServiceCollector> i = serviceCollectors.values().iterator(); i.hasNext();)
            {
                ServiceCollector collector = i.next();
                removeServiceListener(collector.type, collector);
            }
            serviceCollectors.clear();
        }
    }

    /**
     * Instances of ServiceCollector are used internally to speed up the
     * performance of method <code>list(type)</code>.
     *
     * @see #list
     */
    private static class ServiceCollector implements ServiceListener
    {

        /**
         * A set of collected service instance names.
         */
        private Map<String, ServiceInfo> infos = Collections.synchronizedMap(new HashMap<String, ServiceInfo>());

        public String type;

        public ServiceCollector(String type)
        {
            this.type = type;
        }

        /**
         * A service has been added.
         */
        public void serviceAdded(ServiceEvent event)
        {
            synchronized (infos)
            {
                event.getDNS().requestServiceInfo(
                    event.getType(), event.getName(), 0);
            }
        }

        /**
         * A service has been removed.
         */
        public void serviceRemoved(ServiceEvent event)
        {
            synchronized (infos)
            {
                infos.remove(event.getName());
            }
        }

        /**
         * A service hase been resolved. Its details are now available in the
         * ServiceInfo record.
         */
        public void serviceResolved(ServiceEvent event)
        {
            synchronized (infos)
            {
                infos.put(event.getName(), event.getInfo());
            }
        }

        /**
         * Returns an array of all service infos which have been collected by this
         * ServiceCollector.
         * @return
         */
        public ServiceInfo[] list()
        {
            synchronized (infos)
            {
                return infos.values().
                    toArray(new ServiceInfo[infos.size()]);
            }
        }

        @Override
        public String toString()
        {
            StringBuffer aLog = new StringBuffer();
            synchronized (infos)
            {
                for (Map.Entry<String, ServiceInfo> entry : infos.entrySet())
                {
                    aLog.append("\n\t\tService: " + entry.getKey() + ": "
                        + entry.getValue());
                }
            }
            return aLog.toString();
        }
    };

    private static String toUnqualifiedName(String type, String qualifiedName)
    {
        if (qualifiedName.endsWith(type))
        {
            return qualifiedName.substring(0,
                qualifiedName.length() - type.length() - 1);
        }
        else
        {
            return qualifiedName;
        }
    }

    /**
     * SC-Bonjour Implementation : Method used to update the corresponding DNS
     * entry in the cache of JmDNS with the new information in this ServiceInfo.
     * A call to getLocalService must first be issued to get the
     * ServiceInfo object to be modified.
     * THIS METHOD MUST BE USED INSTEAD OF ANY DIRECT ACCESS TO JMDNS' CACHE!!
     * This is used in the implementation of Zeroconf in SIP Communicator
     * to be able to change fields declared by the local contact (status, etc).
     * @param info Updated service data to be used to replace the old
     *  stuff contained in JmDNS' cache
     * @param old info bytes
     */
    public void updateInfos(ServiceInfo info, byte[] old)
    {

        DNSOutgoing out, out2;
        synchronized (JmDNS.this)
        {
            //list = new ArrayList(services.values());
            services.put(info.getQualifiedName().toLowerCase(), info);
        }

        synchronized (info)
        {
            if (logger.isDebugEnabled())
                logger.debug("updateInfos() JmDNS updating " +
                info.getQualifiedName() + " state " +
                info.getState());

            out = new DNSOutgoing(
                /*DNSConstants.FLAGS_QR_RESPONSE*/
                DNSConstants.FLAGS_RA | DNSConstants.FLAGS_AA);
            out2 = new DNSOutgoing(
                /*DNSConstants.FLAGS_QR_RESPONSE*/
                DNSConstants.FLAGS_RA | DNSConstants.FLAGS_AA);


            try
            {
                //out.addAnswer(new DNSRecord.Pointer(info.type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.getQualifiedName()), 0);
                //out.addAnswer(new DNSRecord.Service(info.getQualifiedName(), DNSConstants.TYPE_A, DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, DNSConstants.DNS_TTL, info.priority, info.weight, info.port, localHost.getName()), 0);
                //out.addAnswer(new DNSRecord.Service(info.getQualifiedName(), DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, DNSConstants.DNS_TTL, info.priority, info.weight, info.port, localHost.getName()), 0);
//                out.addAnswer(
//                    new DNSRecord.Text(
//                        info.getQualifiedName(),
//                        DNSConstants.TYPE_TXT,
//                        DNSConstants.CLASS_IN ,
//                        DNSConstants.DNS_TTL,
//                        info.text), 0);
                out.addAnswer(
                    new DNSRecord.Text(
                        info.getQualifiedName(),
                        DNSConstants.TYPE_TXT,
                        DNSConstants.CLASS_IN ,
                        0,
                        old), 0);
                out.addAnswer(
                    new DNSRecord.Text(
                        info.getQualifiedName(),
                        DNSConstants.TYPE_TXT,
                        DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                        DNSConstants.DNS_TTL,
                        info.text), 0);

                out2.addAnswer(
                    new DNSRecord.Text(
                        info.getQualifiedName(),
                        DNSConstants.TYPE_TXT,
                        DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                        DNSConstants.DNS_TTL,
                        info.text), 0);

                if (logger.isDebugEnabled())
                    logger.debug("updateInfos() JmDNS updated infos for "+info);

                send(out);
                Thread.sleep(1000);
                send(out2);
                Thread.sleep(2000);
                send(out2);
            }
            catch( Exception e)
            {
                logger.warn( "", e);
            }
        }
    }


    /**
     * SC-Bonjour Implementation: Method to retrieve the DNS Entry corresponding to a service
     * that has been declared and return it as a ServiceInfo structure.
     * It is used in the implementation of Bonjour in SIP Communicator to retrieve the information
     * concerning the service declared by the local contact. THIS METHOD MUST BE USED INSTEAD OF ANY
     * LOCAL COPY SAVED BEFORE SERVICE REGISTRATION!!
     * @return information corresponding to the specified service
     * @param FQN String representing the Fully Qualified name of the service we want info about
     */
    public ServiceInfo getLocalService(String FQN)
    {
        return services.get(FQN);
    }
}
