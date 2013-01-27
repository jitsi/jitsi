/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.dns;

import java.io.*;
import java.net.*;
import java.util.*;

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
public interface ParallelResolver
    extends Resolver
{
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
     * The name of the property that enables or disables the DNSSEC resolver
     * (instead of a normal, non-validating local resolver).
     */
    public static final String PNAME_DNSSEC_RESOLVER_ENABLED
        = "net.java.sip.communicator.util.dns.DNSSEC_ENABLED";

    /**
     * Default value of @see PNAME_DNSSEC_RESOLVER_ENABLED.
     */
    public static final boolean PDEFAULT_DNSSEC_RESOLVER_ENABLED = false;

    /**
     * Replaces the default resolver used by this class. Mostly meant for
     * debugging.
     *
     * @param resolver the resolver we'd like to use by default from now on.
     */
    public void setDefaultResolver(Resolver resolver);

    /**
     * Returns the default resolver used by this class. Mostly meant for
     * debugging.
     *
     * @return  the resolver this class consults first.
     */
    public Resolver getDefaultResolver();

    /**
     * Creates a <tt>ParallelResolver</tt> that would use the specified array
     * of <tt>backupServers</tt> if the default DNS doesn't seem to be doing
     * that well.
     *
     * @param backupServers the list of backup DNS servers that we should use
     * if, and only if, the default servers don't seem to work that well.
     */
    public void setBackupServers(InetSocketAddress[] backupServers);

    /**
     * Sends a message and waits for a response.
     *
     * @param query The query to send.
     * @return The response
     *
     * @throws IOException An error occurred while sending or receiving.
     */
    public Message send(Message query)
        throws IOException;
    /**
     * Supposed to asynchronously send messages but not currently implemented.
     *
     * @param query The query to send
     * @param listener The object containing the callbacks.
     * @return An identifier, which is also a parameter in the callback
     */
    public Object sendAsync(final Message query,
                            final ResolverListener listener);

    /**
     * Sets the port to communicate on with the default servers.
     *
     * @param port The port to send messages to
     */
    public void setPort(int port);

    /**
     * Sets whether TCP connections will be sent by default with the default
     * resolver. Backup servers would always be contacted the same way.
     *
     * @param flag Indicates whether TCP connections are made
     */
    public void setTCP(boolean flag);

    /**
     * Sets whether truncated responses will be ignored.  If not, a truncated
     * response over UDP will cause a retransmission over TCP. Backup servers
     * would always be contacted the same way.
     *
     * @param flag Indicates whether truncated responses should be ignored.
     */
    public void setIgnoreTruncation(boolean flag);

    /**
     * Sets the EDNS version used on outgoing messages.
     *
     * @param level The EDNS level to use.  0 indicates EDNS0 and -1 indicates no
     * EDNS.
     * @throws IllegalArgumentException An invalid level was indicated.
     */
    public void setEDNS(int level);

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
    public void setEDNS(int level, int payloadSize, int flags, List options);

    /**
     * Specifies the TSIG key that messages will be signed with
     * @param key The key
     */
    public void setTSIGKey(TSIG key);

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param secs The number of seconds to wait.
     * @param msecs The number of milliseconds to wait.
     */
    public void setTimeout(int secs, int msecs);

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param secs The number of seconds to wait.
     */
    public void setTimeout(int secs);

    /**
     * Resets resolver configuration and populate our default resolver
     * with the newly configured servers.
     */
    public void reset();

    /**
     * Sets a DNSSEC resolver as default resolver on lookup when DNSSEC is
     * enabled; creates a standard lookup otherwise.
     */
    public void refreshResolver();
}
