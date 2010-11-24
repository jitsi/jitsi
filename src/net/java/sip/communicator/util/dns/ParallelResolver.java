/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.dns;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.util.*;

import org.xbill.DNS.*;

/**
 * The purpose of this class is to help avoid the significant delays that occur
 * in networks where DNS servers would ignore SRV and NAPTR queries (i.e.
 * without even sending an error response).
 * <p>
 * We achieve this by entering a redundant mode whenever we detect an abnormal
 * delay (longer than <tt>DNS_PATIENCE</tt>) while waiting for a DNS answer.
 * Once we enter redundant mode, we start duplicating all queries and sending
 * them to both our primary and backup resolvers (in case we have any). We then
 * always return the first response we get.
 * <p>
 * We exit redundant mode after receiving <tt>DNS_REDEMPTION</tt> consecutive
 * timely responses from our primary resolver.
 * <p>
 * Note that this class does not attempt to fix everything that may be wrong
 * with local DNS servers. For example, some DNS servers would return
 * <tt>NXDOMAIN</tt> responses for all SRV queries even if the domain does have
 * an SRV record. We don't try to fix this case here. In case we receive
 * different responses from our primary and backup resolvers it is hard to trust
 * one more than the other. Just as local DNS servers might be systematically
 * returning flawed responses, so could our backup-resolvers. This is especially
 * likely to happen in enterprise network where firewalls block any DNS traffic
 * other than the one bound to a locally configured DNS server.
 * <p>
 * Besides, returning <tt>NXDOMAIN</tt> is not that much of an issue as we try
 * to handle that at the provider level where we assume that absence of an SRV
 * record simply means we should try with A/AAAA.
 * <p>
 * In other words, the goal of this class is to <b>only</b> help eliminate the
 * significant lag that may occur with servers that drop SRV and NAPTR request
 * without answering ... nothing more.
 *
 * @author Emil Ivov
 */
public class ParallelResolver implements Resolver
{
    /**
     * The <tt>Logger</tt> used by the <tt>ParallelResolver</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(ParallelResolver.class.getName());

    /**
     * Indicates whether we are currently in a mode where all DNS queries are
     * sent to both the primary and the backup DNS servers.
     */
    private static boolean redundantMode = false;

    /**
     * The default number of milliseconds it takes us to get into redundant
     * mode while waiting for a DNS query response.
     */
    public static final long DNS_PATIENCE = 1500;

    /**
     * The name of the System property that allows us to override the default
     * <tt>DNS_PATIENCE</tt> value.
     */
    public static final String PNAME_DNS_PATIENCE
        = "net.java.sip.communicator.util.dns.DNS_PATIENCE";

    /**
     * The currently configured number of milliseconds that we need to wait
     * before entering redundant mode.
     */
    private static long currentDnsPatience = DNS_PATIENCE;

    /**
     * The default number of times that the primary DNS would have to provide a
     * faster response than the backup resolver before we consider it safe
     * enough to exit redundant mode.
     */
    public static final int DNS_REDEMPTION = 3;


    /**
     * The name of the System property that allows us to override the default
     * <tt>DNS_REDEMPTION</tt> value.
     */
    public static final String PNAME_DNS_REDEMPTION
        = "net.java.sip.communicator.util.dns.DNS_REDEMPTION";

    /**
     * The currently configured number of times that the primary DNS would have
     * to provide a faster response than the backup resolver before we consider
     * it safe enough to exit redundant mode.
     */
    public static int currentDnsRedemption = DNS_REDEMPTION;

    /**
     * The number of fast responses that we need to get from the primary
     * resolver before we exit redundant mode. <tt>0</tt> indicates that we are
     * no longer in redundant mode
     */
    private static int redemptionStatus = 0;

    /**
     * A lock that we use while determining whether we've completed redemption
     * and can exit redundant mode.
     */
    private final static Object redemptionLock = new Object();

    /**
     * The default resolver that we use if everything works properly.
     */
    private final static Resolver defaultResolver;

    static
    {
        try
        {
            defaultResolver = new ExtendedResolver();
        }
        catch (UnknownHostException e)
        {
            //should never happen
            throw new RuntimeException("Failed to initialize resolver");
        }

        initProperties();
    }

    /**
     * Default resolver property initialisation
     */
    private static void initProperties()
    {
        try
        {
            currentDnsPatience
                = Long.getLong(PNAME_DNS_PATIENCE, DNS_PATIENCE);
            currentDnsRedemption
                = Integer.getInteger(PNAME_DNS_REDEMPTION, DNS_REDEMPTION);
        }
        catch(Throwable t)
        {
            //we don't want messed up properties to screw up DNS resolution
            //so we just log.
            logger.info("Failed to initialize DNS resolver properties", t);
        }

    }

    /**
     * A list of backup resolvers that we use only if the default resolver
     * doesn't seem to work that well.
     */
    private final List<Resolver> backupResolvers
                                = new LinkedList<Resolver>();

    /**
     * Creates a <tt>ParallelResolver</tt> that would use the specified array
     * of <tt>backupServers</tt> if the default DNS doesn't seem to be doing
     * that well.
     *
     * @param backupServers the list of backup DNS servers that we should use
     * if, and only if, the default servers don't seem to work that well.
     */
    public ParallelResolver(InetSocketAddress[] backupServers)
    {
        for(InetSocketAddress server : backupServers )
        {
            SimpleResolver resolver;

            try
            {
                resolver = new SimpleResolver();
            }
            catch (UnknownHostException e)
            {
                //this shouldn't be thrown since we don't do any DNS querying
                //in here. this is why we take an InetSocketAddress as a param.
                return;
            }

            resolver.setAddress(server);
            backupResolvers.add(resolver);
        }
    }

    /**
     * Sends a message and waits for a response.
     *
     * @param query The query to send.
     * @return The response
     *
     * @throws IOException An error occurred while sending or receiving.
     */
    public Message send(Message query)
        throws IOException
    {

        ParallelResolution resolution = new ParallelResolution(query);

        resolution.sendFirstQuery();

        //make a copy of the redundant mode variable in case we are currently
        //completed a redemption that started earlier.
        boolean redundantModeCopy;

        synchronized(redemptionLock)
        {
            redundantModeCopy = redundantMode;
        }

        //if we are not in redundant mode we should wait a bit and see how this
        //goes. if we get a reply we could return bravely.
        if(!redundantModeCopy)
        {
            if(resolution.waitForResponse(currentDnsPatience))
            {
                //we are done.
                return resolution.returnResponseOrThrowUp();
            }
            else
            {
                synchronized(redemptionLock)
                {
                    redundantMode = true;
                    redemptionStatus = currentDnsRedemption;
                    logger.info("Primary DNS seems laggy as we got no "
                                +"response for " + currentDnsPatience + "ms. "
                                + "Enabling redundant mode.");
                }
            }
        }

        //we are definitely in redundant mode now
        resolution.sendBackupQueries();

        resolution.waitForResponse(0);

        //check if it is time to end redundant mode.
        synchronized(redemptionLock)
        {
            if(!resolution.primaryResolverRespondedFirst)
            {
                //primary DNS is still feeling shaky. we reinit redemption
                //status in case we were about to cut the server some slack
                redemptionStatus = currentDnsRedemption;
            }
            else
            {
                //primary server replied first. we let him redeem some dignity
                redemptionStatus --;

                //yup, it's now time to end DNS redundant mode;
                if(redemptionStatus <= 0)
                {
                    redundantMode = false;
                    logger.info("Primary DNS seems back in biz. "
                                    + "Disabling redundant mode.");
                }
            }
        }

        return resolution.returnResponseOrThrowUp();
    }

    /**
     * Supposed to asynchronously send messages but not currently implemented.
     *
     * @param query The query to send
     * @param listener The object containing the callbacks.
     * @return An identifier, which is also a parameter in the callback
     */
    public Object sendAsync(final Message query, final ResolverListener listener)
    {
        return null;
    }

    /**
     * Sets the port to communicate on with the default servers.
     *
     * @param port The port to send messages to
     */
    public void setPort(int port)
    {
        defaultResolver.setPort(port);
    }

    /**
     * Sets whether TCP connections will be sent by default with the default
     * resolver. Backup servers would always be contacted the same way.
     *
     * @param flag Indicates whether TCP connections are made
     */
    public void setTCP(boolean flag)
    {
        defaultResolver.setTCP(flag);
    }

    /**
     * Sets whether truncated responses will be ignored.  If not, a truncated
     * response over UDP will cause a retransmission over TCP. Backup servers
     * would always be contacted the same way.
     *
     * @param flag Indicates whether truncated responses should be ignored.
     */
    public void setIgnoreTruncation(boolean flag)
    {
        defaultResolver.setIgnoreTruncation(flag);
    }

    /**
     * Sets the EDNS version used on outgoing messages.
     *
     * @param level The EDNS level to use.  0 indicates EDNS0 and -1 indicates no
     * EDNS.
     * @throws IllegalArgumentException An invalid level was indicated.
     */
    public void setEDNS(int level)
    {
        defaultResolver.setEDNS(level);
    }

    /**
     * Sets the EDNS information on outgoing messages.
     *
     * @param level The EDNS level to use.  0 indicates EDNS0 and -1 indicates no
     * EDNS.
     * @param payloadSize The maximum DNS packet size that this host is capable
     * of receiving over UDP.  If 0 is specified, the default (1280) is used.
     * @param flags EDNS extended flags to be set in the OPT record.
     * @param options EDNS options to be set in the OPT record, specified as a
     * List of OPTRecord.Option elements.
     *
     * @throws IllegalArgumentException An invalid field was specified.
     * @see OPTRecord
     */
    @SuppressWarnings("unchecked")//that's the way it is in dnsjava
    public void setEDNS(int level, int payloadSize, int flags, List options)
    {
        defaultResolver.setEDNS(level, payloadSize, flags, options);
    }

    /**
     * Specifies the TSIG key that messages will be signed with
     * @param key The key
     */
    public void setTSIGKey(TSIG key)
    {
        defaultResolver.setTSIGKey(key);
    }

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param secs The number of seconds to wait.
     * @param msecs The number of milliseconds to wait.
     */
    public void setTimeout(int secs, int msecs)
    {
        defaultResolver.setTimeout(secs, msecs);
    }

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param secs The number of seconds to wait.
     */
    public void setTimeout(int secs)
    {
        defaultResolver.setTimeout(secs);
    }


    /**
     * The class that listens for responses to any of the queries we send to
     * our default and backup servers and returns as soon as we get one or until
     * our default resolver fails.
     */
    private class ParallelResolution extends Thread
                                implements ResolverListener
    {
        /**
         * The query that we have sent to the default and backup DNS servers.
         */
        private final Message query;

        /**
         * The field where we would store the first incoming response to our
         * query.
         */
        public Message response;

        /**
         * The field where we would store the first error we receive from a DNS
         * or a backup resolver.
         */
        private Throwable exception;

        /**
         * Indicates whether we are still waiting for an answer from someone
         */
        private boolean done = false;

        /**
         * Indicates that a response was received from the primary resolver.
         */
        private boolean primaryResolverRespondedFirst = true;

        /**
         * Creates a {@link ParallelResolution} for the specified <tt>query</tt>
         *
         * @param query the DNS query that we'd like to send to our primary
         * and backup resolvers.
         */
        public ParallelResolution(final Message query)
        {
            this.query = query;
        }

        /**
         * Starts this collector which would cause it to send its query to the
         * default resolver.
         */
        public void sendFirstQuery()
        {
            start();
        }

        /**
         * Sends this collector's query to the default resolver.
         */
        public void run()
        {
            Message localResponse = null;
            try
            {
                localResponse = defaultResolver.send(query);
            }
            catch (Throwable exc)
            {
                this.exception = exc;
            }
            synchronized(this)
            {
                //if the backup resolvers had already replied we ignore the
                //reply of the primary one whatever it was.
                if(done)
                    return;


                response = localResponse;
                done = true;

                notify();
            }
        }

        /**
         * Asynchronously sends this collector's query to all backup resolvers.
         */
        public void sendBackupQueries()
        {
            synchronized(this)
            {
                for (Resolver resolver : backupResolvers)
                {
                    if (done)
                        return;
                    resolver.sendAsync(query, this);
                }
            }
        }

        /**
         * Waits for a response or an error to occur during <tt>waitFor</tt>
         * milliseconds.If neither happens, we return false.
         *
         * @param waitFor the number of milliseconds to wait for a response or
         * an error or <tt>0</tt> if we'd like to wait until either of these
         * happen.
         *
         * @return <tt>true</tt> if we returned because we received a response
         * from a resolver or errors from everywhere, and <tt>false</tt> that
         * didn't happen.
         */
        public boolean waitForResponse(long waitFor)
        {
            synchronized(this)
            {
                if(done)
                    return done;
                try
                {
                    wait(waitFor);
                }
                catch (InterruptedException e)
                {
                    //we don't care
                }

                return done;
            }
        }

        /**
         * Waits for resolution to complete (if necessary) and then either
         * returns the response we received or throws whatever exception we
         * saw.
         *
         * @return the response {@link Message} we received from the DNS.
         *
         * @throws IOException if this resolution ended badly because of a
         * network IO error
         * @throws RuntimeException if something unexpected happened
         * during resolution.
         * @throws IllegalArgumentException if something unexpected happened
         * during resolution.
         */
        public Message returnResponseOrThrowUp()
            throws IOException, RuntimeException, IllegalArgumentException
        {
            if(!done)
                waitForResponse(0);

            if(response != null)
                return response;
            else if (exception instanceof IOException)
                throw (IOException) exception;
            else if (exception instanceof RuntimeException)
                throw (RuntimeException) exception;
            else if (exception instanceof Error)
                throw (Error) exception;
            else
                throw new IllegalStateException
                    ("ExtendedResolver failure");
        }

        /**
         * Records the message and causes the collector to stop waiting and
         * return.
         *
         * @param id The identifier returned by Resolver.sendAsync()
         * @param message The response message as returned by the Resolver
         */
        public void receiveMessage(Object id, Message message)
        {
            synchronized (this)
            {
                if (done)
                    return;

                this.response = message;
                this.primaryResolverRespondedFirst = false;

                done = true;
                notify();
            }
        }

        /**
         * Nothing to do here.
         *
         * @param id The identifier returned by Resolver.sendAsync()
         * @param e The thrown exception
         */
        public void handleException(Object id, Exception e)
        {
            //nothing to do here. this means that we won't notice if our backup
            //resolvers fail but that's not a problem as we need to wait for
            //our primary resolver anyway.
        }

    }
}
