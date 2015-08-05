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
package net.java.sip.communicator.impl.dns;

import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.dns.*;
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
public class ParallelResolverImpl
    implements CustomResolver, PropertyChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ParallelResolver</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(ParallelResolverImpl.class);

    /**
     * Indicates whether we are currently in a mode where all DNS queries are
     * sent to both the primary and the backup DNS servers.
     */
    private volatile static boolean redundantMode = false;

    /**
     * The currently configured number of milliseconds that we need to wait
     * before entering redundant mode.
     */
    private static long currentDnsPatience = DNS_PATIENCE;

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
    private Resolver defaultResolver;

    /**
     * An extended resolver that would be encapsulating all backup resolvers.
     */
    private ExtendedResolver backupResolver;

    /** Thread pool that processes the backup queries. */
    private ExecutorService backupQueriesPool;

    /**
     * Creates a new instance of this class.
     */
    ParallelResolverImpl()
    {
        backupQueriesPool = Executors.newCachedThreadPool();
        DnsUtilActivator.getConfigurationService()
            .addPropertyChangeListener(this);
        initProperties();
        reset();
    }

    private void initProperties()
    {
        String rslvrAddrStr
            = DnsUtilActivator.getConfigurationService().getString(
                DnsUtilActivator.PNAME_BACKUP_RESOLVER,
                DnsUtilActivator.DEFAULT_BACKUP_RESOLVER);
        String customResolverIP
            = DnsUtilActivator.getConfigurationService().getString(
                DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP,
                DnsUtilActivator.getResources().getSettingsString(
                    DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP));

        InetAddress resolverAddress = null;
        try
        {
            resolverAddress = NetworkUtils.getInetAddress(rslvrAddrStr);
        }
        catch(UnknownHostException exc)
        {
            logger.warn(
                    "Seems like the primary DNS is down, trying fallback to "
                        + customResolverIP);
        }

        if(resolverAddress == null)
        {
            // name resolution failed for backup DNS resolver,
            // try with the IP address of the default backup resolver
            try
            {
                resolverAddress = NetworkUtils.getInetAddress(customResolverIP);
            }
            catch (UnknownHostException e)
            {
                // this shouldn't happen, but log anyway
                logger.error(e);
            }
        }

        int resolverPort = DnsUtilActivator.getConfigurationService().getInt(
            DnsUtilActivator.PNAME_BACKUP_RESOLVER_PORT,
            SimpleResolver.DEFAULT_PORT);

        InetSocketAddress resolverSockAddr
            = new InetSocketAddress(resolverAddress, resolverPort);

        setBackupServers(new InetSocketAddress[]{ resolverSockAddr });

        currentDnsPatience = DnsUtilActivator.getConfigurationService()
            .getLong(PNAME_DNS_PATIENCE, DNS_PATIENCE);

        currentDnsRedemption
            = DnsUtilActivator.getConfigurationService()
                .getInt(PNAME_DNS_REDEMPTION, DNS_REDEMPTION);
    }

    /**
     * Sets the specified array of <tt>backupServers</tt> used if the default
     * DNS doesn't seem to be doing that well.
     *
     * @param backupServers the list of backup DNS servers that we should use
     * if, and only if, the default servers don't seem to work that well.
     */
    private void setBackupServers(InetSocketAddress[] backupServers)
    {
        try
        {
            backupResolver = new ExtendedResolver(new SimpleResolver[0]);
            for(InetSocketAddress backupServer : backupServers)
            {
                SimpleResolver sr = new SimpleResolver();

                sr.setAddress(backupServer);
                backupResolver.addResolver(sr);
            }
        }
        catch (UnknownHostException e)
        {
            // this shouldn't be thrown since we don't do any DNS querying in
            // here. this is why we take an InetSocketAddress as a param.
            throw new IllegalStateException(
                    "The impossible just happened: we could not initialize our"
                        + " backup DNS resolver.");
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

        //if we are not in redundant mode we should wait a bit and see how this
        //goes. if we get a reply we could return bravely.
        if(!redundantMode)
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
                    logger.info("Primary DNS seems laggy: "
                        + "no response for " + query.getQuestion().getName()
                        + "/" + Type.string(query.getQuestion().getType())
                        + " after " + currentDnsPatience + "ms. "
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
        throw new UnsupportedOperationException("Not implemented");
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
    public final void reset()
    {
        Lookup.refreshDefault();

        // populate with new servers after refreshing configuration
        try
        {
            Lookup.setDefaultResolver(this);
            ExtendedResolver temp = new ExtendedResolver();
            temp.setTimeout(10);
            defaultResolver = temp;
        }
        catch (UnknownHostException e)
        {
            // should never happen
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

        int rcode = response.getRcode();
        //we didn't find any responses and the answer is NXDOMAIN then
        //we may want to check with the backup resolver for a second opinion
        if(rcode == Rcode.NXDOMAIN)
            return false;

        //if we received NODATA (same as NOERROR and no response records) for
        // an AAAA or a NAPTR query then it makes sense since many existing
        //domains come without those two.
        Record question = response.getQuestion();
        int questionType = (question == null) ? 0 : question.getType();
        if( rcode == Rcode.NOERROR
            && question != null
            && (questionType == Type.AAAA || questionType == Type.NAPTR))
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
    private class ParallelResolution implements Runnable
    {
        /**
         * The query that we have sent to the default and backup DNS servers.
         */
        private final Message query;

        /**
         * The field where we would store the first incoming response to our
         * query.
         */
        private volatile Message response;

        /**
         * The field where we would store the first error we receive from a DNS
         * or a backup resolver.
         */
        private Throwable exception;

        /**
         * Indicates whether we are still waiting for an answer from someone
         */
        private volatile boolean done = false;

        /**
         * Indicates that a response was received from the primary resolver.
         */
        private volatile boolean primaryResolverRespondedFirst = true;

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
            ParallelResolverImpl.this.backupQueriesPool.execute(this);
        }

        /**
         * Sends this collector's query to the default resolver.
         */
        @Override
        public void run()
        {
            Message localResponse = null;

            try
            {
                localResponse = defaultResolver.send(query);
            }
            catch (SocketTimeoutException exc)
            {
                logger.info("Default DNS resolver timed out.");
                exception = exc;
            }
            catch (Throwable exc)
            {
                logger.info("Default DNS resolver failed", exc);
                exception = exc;
            }

            //if the backup resolvers had already replied we ignore the
            //reply of the primary one whatever it was.
            if(done)
                return;

            synchronized(this)
            {
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
            //yes. a second thread in the thread ... it's ugly but it works
            //and i do want to keep code simple to read ... this whole parallel
            //resolving is complicated enough as it is.
            backupQueriesPool.execute(new Runnable(){
                @Override
                public void run()
                {
                    if (done)
                    {
                        return;
                    }

                    Message localResponse = null;
                    try
                    {
                        logger.info("Sending query for "
                            + query.getQuestion().getName() + "/"
                            + Type.string(query.getQuestion().getType())
                            + " to backup resolvers");
                        localResponse = backupResolver.send(query);
                    }
                    catch (Throwable exc)
                    {
                        logger.info(
                                "Exception occurred during backup DNS resolving "
                                    + exc);

                        //keep this so that we can rethrow it
                        exception = exc;
                    }
                    //if the default resolver has already replied we
                    //ignore the reply of the backup ones.
                    if(done)
                    {
                        return;
                    }

                    synchronized(ParallelResolution.this)
                    {
                        //contrary to responses from the  primary resolver,
                        //in this case we don't care whether the response is
                        //satisfying: if it isn't, there's nothing we can do
                        if (response == null)
                        {
                            response = localResponse;
                            primaryResolverRespondedFirst = false;
                        }

                        done = true;
                        ParallelResolution.this.notify();
                    }
                }
            });
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
            else if (exception instanceof SocketTimeoutException)
            {
                logger.warn("DNS resolver timed out");
                throw (IOException) exception;
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

    @SuppressWarnings("serial")
    private final Set<String> configNames = new HashSet<String>(5)
    {{
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER_ENABLED);
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER);
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP);
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER_PORT);
        add(CustomResolver.PNAME_DNS_PATIENCE);
        add(CustomResolver.PNAME_DNS_REDEMPTION);
    }};

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (!configNames.contains(evt.getPropertyName()))
        {
            return;
        }

        initProperties();
    }
}
