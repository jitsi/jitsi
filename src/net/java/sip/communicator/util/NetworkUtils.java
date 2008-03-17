/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.net.*;
import java.util.*;
import org.xbill.DNS.*;
import java.text.*;

/**
 * Utility methods and fields to use when working with network addresses.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 * @author Vincent Lucas
 */
public class NetworkUtils
{
    private static final Logger logger
        = Logger.getLogger(NetworkUtils.class);
    /**
     * A string containing the "any" local address.
     */
    public static final String IN_ADDR_ANY = "0.0.0.0";

    /**
     * The maximum int value that could correspond to a port nubmer.
     */
    public static final int    MAX_PORT_NUMBER = 65535;

    /**
     * The minimum int value that could correspond to a port nubmer bindable
     * by the SIP Communicator.
     */
    public static final int    MIN_PORT_NUMBER = 1024;


    /**
     * The random port number generator that we use in getRandomPortNumer()
     */
    private static Random portNumberGenerator = new Random();

    /**
     * Determines whether the address is the result of windows auto configuration.
     * (i.e. One that is in the 169.254.0.0 network)
     * @param add the address to inspect
     * @return true if the address is autoconfigured by windows, false otherwise.
     */
    public static boolean isWindowsAutoConfiguredIPv4Address(InetAddress add)
    {
        return (add.getAddress()[0] & 0xFF) == 169
            && (add.getAddress()[1] & 0xFF) == 254;
    }

    /**
     * Determines whether the address is an IPv4 link local address. IPv4 link
     * local addresses are those in the following networks:
     *
     * 10.0.0.0    to 10.255.255.255
     * 172.16.0.0  to 172.31.255.255
     * 192.168.0.0 to 192.168.255.255
     *
     * @param add the address to inspect
     * @return true if add is a link local ipv4 address and false if not.
     */
    public static boolean isLinkLocalIPv4Address(InetAddress add)
    {
        if (add instanceof Inet4Address)
        {
            byte address[] = add.getAddress();
            if ( (address[0] & 0xFF) == 10)
                return true;
            if ( (address[0] & 0xFF) == 172
                && (address[1] & 0xFF) >= 16 && address[1] <= 31)
                return true;
            if ( (address[0] & 0xFF) == 192
                && (address[1] & 0xFF) == 168)
                return true;
            return false;
        }
        return false;
    }

    /**
     * Returns a random local port number that user applications could bind to.
     * (i.e. above 1024).
     * @return a random int located between 1024 and 65 535.
     */
    public static int getRandomPortNumber()
    {
        return getRandomPortNumber(MIN_PORT_NUMBER, MAX_PORT_NUMBER);
    }

    /**
     * Returns a random local port number, greater than min and lower than max.
     *
     * @param min the minimum allowed value for the returned port number.
     * @param max the maximum allowed value for the returned port number.
     *
     * @return a random int located between greater than min and lower than max.
     */
    public static int getRandomPortNumber(int min, int max)
    {
        return portNumberGenerator.nextInt(max - min) + min;
    }

    /**
     * Returns a random local port number, greater than min and lower than max.
     * If the pair flag is set to true, then the returned port number is
     * guaranteed to be pair. This is useful for protocols that require this
     * such as RTP
     *
     * @param min the minimum allowed value for the returned port number.
     * @param max the maximum allowed value for the returned port number.
     * @param pair specifies whether the caller would like the returned port to
     * be pair.
     *
     * @return a random int located between greater than min and lower than max.
     */
    public static int getRandomPortNumber(int min, int max, boolean pair)
    {
        if(pair)
        {
            int delta = max - min;
            delta /= 2;
            int port = getRandomPortNumber(min, min + delta);
            return port * 2;
        }
        else
        {
            return getRandomPortNumber(min, max);
        }
    }

    /**
     * Verifies whether <tt>address</tt> could be an IPv6 address string.
     *
     * @param address the String that we'd like to determine as an IPv6 address.
     *
     * @return true if the address containaed by <tt>address</tt> is an ipv6
     * address and falase otherwise.
     */
    public static boolean isIPv6Address(String address)
    {
        return (address != null && address.indexOf(':') != -1);
    }

    /**
     * Returns array of hosts from the SRV record of the specified domain.
     * The records are ordered against the SRV record priority
     * @param domain the name of the domain we'd like to resolve (_proto._tcp
     * included).
     * @return an array of InetSocketAddress containing records returned by the DNS
     * server - address and port .
     * @throws ParseException if <tt>domain</tt> is not a valid domain name.
     */
    public static InetSocketAddress[] getSRVRecords(String domain)
        throws ParseException
    {
        Record[] records = null;
        try
        {
            Lookup lookup = new Lookup(domain, Type.SRV);
            records = lookup.run();
        }
        catch (TextParseException tpe)
        {
            logger.error("Failed to parse domain="+domain, tpe);
            throw new ParseException(tpe.getMessage(), 0);
        }
        if (records == null)
        {
            return null;
        }

        String[][] pvhn = new String[records.length][4];
        for (int i = 0; i < records.length; i++)
        {
            SRVRecord srvRecord = (SRVRecord) records[i];
            pvhn[i][0] = "" + srvRecord.getPriority();
            pvhn[i][1] = "" + srvRecord.getWeight();
            pvhn[i][2] = "" + srvRecord.getPort();
            pvhn[i][3] = srvRecord.getTarget().toString();
            if (pvhn[i][3].endsWith("."))
            {
                pvhn[i][3] = pvhn[i][3].substring(0, pvhn[i][3].length() - 1);
            }
        }

        /* sort the SRV RRs by RR value (lower is preferred) */
        Arrays.sort(pvhn, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                return (Integer.parseInt( ( (String[]) o1)[0])
                        - Integer.parseInt( ( (String[]) o2)[0]));
            }
        });

        /* put sorted host names in an array, get rid of any trailing '.' */
        InetSocketAddress[] sortedHostNames = new InetSocketAddress[pvhn.length];
        for (int i = 0; i < pvhn.length; i++)
        {
            sortedHostNames[i] = 
                new InetSocketAddress(pvhn[i][3], Integer.valueOf(pvhn[i][2]));
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("DNS SRV query for domain " + domain + " returned:");
            for (int i = 0; i < sortedHostNames.length; i++)
            {
                logger.trace(sortedHostNames[i]);
            }
        }
        return sortedHostNames;

    }
}
