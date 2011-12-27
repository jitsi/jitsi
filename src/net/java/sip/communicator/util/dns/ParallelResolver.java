/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
 * in networks where DNS servers would ignore SRV, NAPTR, and sometimes even
 * A/AAAA queries (i.e. without even sending an error response). We also try to
 * handle cases where DNS servers may return empty responses to some records.
 * <p>
 * We achieve this by entering a redundant mode whenever we detect an abnormal
 * delay (longer than <tt>DNS_PATIENCE</tt>)  while waiting for a DNS resonse,
 * or when that response is not considered satisfying.
 * <p>
 * Once we enter redundant mode, we start duplicating all queries and sending
 * them to both our primary and backup resolvers (in case we have any). We then
 * always return the first response we get, regardless of who sent it.
 * <p>
 * We exit redundant mode after receiving <tt>DNS_REDEMPTION</tt> consecutive
 * timely and correct responses from our primary resolver.
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
    public static final int DNS_PATIENCE = 1500;

    /**
     * The name of the property that allows us to override the default
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
     * The name of the property that allows us to override the default
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
    private static Resolver defaultResolver;

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
            currentDnsPatience = UtilActivator.getConfigurationService()
                .getLong(PNAME_DNS_PATIENCE, DNS_PATIENCE);
            currentDnsRedemption = UtilActivator.getConfigurationService()
                .getInt(PNAME_DNS_REDEMPTION, DNS_REDEMPTION);
        }
        catch(Throwable t)
        {
            //we don't want messed up properties to screw up DNS resolution
            //so we just log.
            logger.info("Failed to initialize DNS resolver properties", t);
        }

    }

    /**
     * Replaces the default resolver used by this class. Mostly meant for
     * debugging.
     *
     * @param resolver the resolver we'd like to use by default from now on.
     */
    public static void setDefaultResolver(Resolver resolver)
    {
        defaultResolver = resolver;
    }

    /**
     * Returns the default resolver used by this class. Mostly meant for
     * debugging.
     *
     * @return  the resolver this class consults first.
     */
    public static Resolver getDefaultResolver()
    {
        return defaultResolver;
    }

    /**
     * An extended resolver that would be encapsulating all backup resolvers.
     */
    private final ExtendedResolver backupResolver;

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
        try
        {
            backupResolver = new ExtendedResolver(new SimpleResolver[]{});
            for(InetSocketAddress backupServer : backupServers )
            {
                SimpleResolver sr = new SimpleResolver();
                sr.setAddress(backupServer);

                backupResolver.addResolver(sr);
            }
        }
        catch (UnknownHostException e)
        {
            //this shouldn't be thrown since we don't do any DNS querying
            //in here. this is why we take an InetSocketAddress as a param.
            throw new IllegalStateException("The impossible just happened: "
                        +"we could not initialize our backup DNS resolver");
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
    @SuppressWarnings("rawtypes") // that's the way it is in dnsjava
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
     * Resets resolver configuration and populate our default resolver
     * with the newly configured servers.
     */
    public void reset()
    {
        ExtendedResolver resolver = (ExtendedResolver)defaultResolver;

        // remove old ones
        for(Resolver r : resolver.getResolvers())
        {
            resolver.deleteResolver(r);
        }

        // populate with new servers after refreshing configuration
        try
        {
            String [] servers = ResolverConfig.getCurrentConfig().servers();
            if (servers != null)
            {
                for (int i = 0; i < servers.length; i++)
                {
                    Resolver r = new SimpleResolver(servers[i]);
                    //r.setTimeout(quantum);
                    resolver.addResolver(r);
                }
            }
            else
                resolver.addResolver(new SimpleResolver());
        }
        catch (UnknownHostException e)
        {
            //should never happen
            throw new RuntimeException("Failed to initialize resolver");
        }
    }

    /**
     * Determines if <tt>response</tt> can be considered a satisfactory DNS
     * response and returns accordingly.
     * <p>
     * We consider non-satisfactory responses that may indicate that the local
     * DNS does not work properly and that we may hence need to fall back to
     * the backup resolver.
     * <p>
     * Basically the goal here is to be able to go into redundant mode when we
     * come across DNS servers that send empty responses to SRV and NAPTR
     * requests.
     *
     * @param response the dnsjava {@link Message} that we'd like to inspect.
     *
     * @return <tt>true</tt> if <tt>response</tt> appears as a satisfactory
     * response and <tt>false</tt> otherwise.
     */
    private boolean isResponseSatisfactory(Message response)
    {
        if ( response == null )
            return false;

        Record[] answerRR = response.getSectionArray(Section.ANSWER);
        Record[] authorityRR = response.getSectionArray(Section.AUTHORITY);
        Record[] additionalRR = response.getSectionArray(Section.ADDITIONAL);

        if (    (answerRR     != null && answerRR.length > 0)
             || (authorityRR  != null && authorityRR.length > 0)
             || (additionalRR != null && additionalRR.length > 0))
        {
            return true;
        }

        //we didn't find any responses and unless the answer is NXDOMAIN then
        //we may want to check with the backup resolver for a second opinion
        if(response.getRcode() == Rcode.NXDOMAIN)
            return true;

        //if we received NODATA (same as NOERROR and no response records) for
        // an AAAA or a NAPTR query then it makes sense since many existing
        //domains come without those two.
        if( response.getRcode() == Rcode.NOERROR
            && (response.getQuestion().getType() == Type.AAAA
                || response.getQuestion().getType() == Type.NAPTR))
        {
            return true;
        }

        //nope .. this doesn't make sense ...
        return false;
    }

    /**
     * The class that listens for responses to any of the queries we send to
     * our default and backup servers and returns as soon as we get one or until
     * our default resolver fails.
     */
    private class ParallelResolution extends Thread
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
            super("ParallelResolutionThread");
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
                logger.info("Exception occurred during parallel DNS resolving" +
                        exc, exc);
                this.exception = exc;
            }
            synchronized(this)
            {
                //if the backup resolvers had already replied we ignore the
                //reply of the primary one whatever it was.
                if(done)
                    return;

                //if there was a response we're only done if it is satisfactory
                if(    localResponse != null
                    && isResponseSatisfactory(localResponse))
                {
                    response = localResponse;
                    done = true;
                }
                notify();
            }
        }

        /**
         * Asynchronously sends this collector's query to all backup resolvers.
         */
        public void sendBackupQueries()
        {
            logger.info("Send DNS queries to backup resolvers");

            //yes. a second thread in the thread ... it's ugly but it works
            //and i do want to keep code simple to read ... this whole parallel
            //resolving is complicated enough as it is.
            new Thread(){
                public void run()
                {
                    synchronized(ParallelResolution.this)
                    {
                        if (done)
                            return;
                        Message localResponse = null;
                        try
                        {
                            localResponse = backupResolver.send(query);
                        }
                        catch (Throwable exc)
                        {
                            logger.info("Exception occurred during backup "
                                        +"DNS resolving" + exc);

                            //keep this so that we can rethrow it
                            exception = exc;
                        }
                        //if the default resolver has already replied we
                        //ignore the reply of the backup ones.
                        if(done)
                            return;

                        //contrary to responses from the  primary resolver,
                        //in this case we don't care whether the response is
                        //satisfying: if it isn't, there's nothing we can do
                        response = localResponse;
                        done = true;

                        ParallelResolution.this.notify();
                    }
                }
            }.start();
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
         * during resolution or if there was no response.
         */
        public Message returnResponseOrThrowUp()
            throws IOException, RuntimeException, IllegalArgumentException
        {
            if(!done)
                waitForResponse(0);

            if(response != null)
            {
                return response;
            }
            else if (exception instanceof IOException)
            {
                logger.warn("IO exception while using DNS resolver", exception);
                throw (IOException) exception;
            }
            else if (exception instanceof RuntimeException)
            {
                logger.warn("RunTimeException while using DNS resolver",
                        exception);
                throw (RuntimeException) exception;
            }
            else if (exception instanceof Error)
            {
                logger.warn("Error while using DNS resolver", exception);
                throw (Error) exception;
            }
            else
            {
                logger.warn("Received a bad response from primary DNS resolver",
                        exception);
                throw new IllegalStateException("ExtendedResolver failure");
            }
        }
    }
}
