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

import org.xbill.DNS.*;

/**
 * @author Emil Ivov
 */
public class ParallelResolver implements Resolver
{
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

        ParallelResolverListener parallelResolveListener
                                = new ParallelResolverListener();

        defaultResolver.sendAsync(query, parallelResolveListener);

        if(!panicMode)
        {
            Message response = parallelResolveListener
                                                .waitForResponse(patience);

            if (response != null)
                return response;
        }

        panicMode = true;


        return null;
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



    private class ParallelResolverListener implements ResolverListener
    {
        /**
         * The callback used by an asynchronous resolver
         * @param id The identifier returned by Resolver.sendAsync()
         * @param m The response message as returned by the Resolver
         */
        public void receiveMessage(Object id, Message m);

        /**
         * The callback used by an asynchronous resolver when an exception is
         * thrown
         *
         * @param id The identifier returned by Resolver.sendAsync()
         * @param e The thrown exception
         */
        public void handleException(Object id, Exception e);

    }
}
