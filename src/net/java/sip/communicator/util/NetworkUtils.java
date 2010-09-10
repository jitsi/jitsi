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
 * @author Alan Kelly
 */
public class NetworkUtils
{
    private static final Logger logger
        = Logger.getLogger(NetworkUtils.class);

    /**
     * A string containing the "any" local address for IPv6.
     */
    public static final String IN6_ADDR_ANY = "::0";

    /**
     * A string containing the "any" local address for IPv4.
     */
    public static final String IN4_ADDR_ANY = "0.0.0.0";

    /**
     * A string containing the "any" local address.
     */
    public static final String IN_ADDR_ANY = determineAnyAddress();

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
     * Verifies whether <tt>address</tt> could be an IPv4 address string.
     *
     * @param address the String that we'd like to determine as an IPv4 address.
     *
     * @return true if the address contained by <tt>address</tt> is an IPv4
     * address and false otherwise.
     */
    public static boolean isIPv4Address(String address)
    {
        return IPAddressUtil.isIPv4LiteralAddress(address);
    }

    /**
     * Verifies whether <tt>address</tt> could be an IPv6 address string.
     *
     * @param address the String that we'd like to determine as an IPv6 address.
     *
     * @return true if the address contained by <tt>address</tt> is an IPv6
     * address and false otherwise.
     */
    public static boolean isIPv6Address(String address)
    {
        return IPAddressUtil.isIPv6LiteralAddress(address);
    }

    /**
     * Checks whether <tt>address</tt> is a valid IP address string.
     *
     * @param address the address that we'd like to check
     * @return true if address is an IPv4 or IPv6 address and false otherwise.
     */
    public static boolean isValidIPAddress(String address)
    {
        // empty string
        if (address == null || address.length() == 0)
        {
            return false;
        }

        // look for IPv6 brackets and remove brackets for parsing
        boolean ipv6Expected = false;
        if (address.charAt(0) == '[')
        {
            // This is supposed to be an IPv6 litteral
            if (address.length() > 2
                            && address.charAt(address.length() - 1) == ']')
            {
                // remove brackets from IPv6
                address = address.substring(1, address.length() - 1);
                ipv6Expected = true;
            }
            else
            {
                return false;
            }
        }

        // look for IP addresses
        if (Character.digit(address.charAt(0), 16) != -1
                        || (address.charAt(0) == ':'))
        {
            byte[] addr = null;

            // see if it is IPv4 address
            addr = IPAddressUtil.textToNumericFormatV4(address);
            // if not, see if it is IPv6 address
            if (addr == null)
            {
                addr = IPAddressUtil.textToNumericFormatV6(address);
            }
            // if IPv4 is found when IPv6 is expected
            else if (ipv6Expected)
            {
                // invalid address: IPv4 address surrounded with brackets!
                return false;
            }
            // if an IPv4 or IPv6 address is found
            if (addr != null)
            {
                // is an IP address
                return true;
            }
        }
        // no matches found
        return false;
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
        Arrays.sort(pvhn, new Comparator<String[]>()
        {
            public int compare(String array1[], String array2[])
            {
                return (Integer.parseInt(   array1[0])
                        - Integer.parseInt( array2[0]));
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
                if (logger.isTraceEnabled())
                    logger.trace(sortedHostNames[i]);
            }
        }
        return sortedHostNames;
    }

    /**
     * Returns an <tt>InetSocketAddress</tt> representing the first SRV
     * record available for the specified domain or <tt>null</tt> if there are
     * not SRV records for <tt>domain</tt>.
     *
     * @param domain the name of the domain we'd like to resolve.
     * @param service the service that we are trying to get a record for.
     * @param proto the protocol that we'd like <tt>service</tt> on.
     *
     * @return the first InetSocketAddress containing records returned by the
     * DNS server - address and port .
     * @throws ParseException if <tt>domain</tt> is not a valid domain name.
     */
    public static InetSocketAddress getSRVRecord(  String service,
                                                   String proto,
                                                   String domain)
        throws ParseException
    {
        InetSocketAddress[] records = getSRVRecords("_" + service
                                                 + "._" + proto
                                                 + "."  + domain);

        if(records == null || records.length == 0)
            return null;

        return records[0];
    }

    /**
     * Returns an <tt>InetSocketAddress</tt> representing the first SRV
     * record available for the specified domain or <tt>null</tt> if there are
     * not SRV records for <tt>domain</tt>.
     *
     * @param domain the name of the domain we'd like to resolve.
     * @param service the service that we are trying to get a record for.
     * @param proto the protocol that we'd like <tt>service</tt> on.
     *
     * @return the InetSocketAddress[] containing records returned by the
     * DNS server - address and port .
     * @throws ParseException if <tt>domain</tt> is not a valid domain name.
     */
    public static InetSocketAddress[] getSRVRecords(String service,
                                                   String proto,
                                                   String domain)
        throws ParseException
    {
        InetSocketAddress[] records = getSRVRecords("_" + service
                                                 + "._" + proto
                                                 + "."  + domain);

        if(records == null || records.length == 0)
            return null;

        return records;
    }

    /**
     * Creates an InetAddress from the specified <tt>hostAddress</tt>. The point
     * of using the method rather than creating the address by yourself is that
     * it would first check whether the specified <tt>hostAddress</tt> is indeed
     * a valid ip address. It this is the case, the method would create the
     * <tt>InetAddress</tt> using the <tt>InetAddress.getByAddress()</tt>
     * method so that no DNS resolution is attempted by the JRE. Otherwise
     * it would simply use <tt>InetAddress.getByName()</tt> so that we would an
     * <tt>InetAddress</tt> instance even at the cost of a potential DNS
     * resolution.
     *
     * @param hostAddress the <tt>String</tt> representation of the address
     * that we would like to create an <tt>InetAddress</tt> instance for.
     *
     * @return an <tt>InetAddress</tt> instance corresponding to the specified
     * <tt>hostAddress</tt>.
     *
     * @throws UnknownHostException if any of the <tt>InetAddress</tt> methods
     * we are using throw an exception.
     */
    public static InetAddress getInetAddress(String hostAddress)
        throws UnknownHostException
    {
        //is null
        if (hostAddress == null || hostAddress.length() == 0)
        {
            throw new UnknownHostException(
                            hostAddress + " is not a valid host address");
        }

        //transform IPv6 literals into normal addresses
        if (hostAddress.charAt(0) == '[')
        {
            // This is supposed to be an IPv6 literal
            if (hostAddress.length() > 2
                && hostAddress.charAt(hostAddress.length()-1) == ']')
            {
                hostAddress = hostAddress.substring(1, hostAddress.length() -1);
            }
            else
            {
                // This was supposed to be a IPv6 address, but it's not!
                throw new UnknownHostException(hostAddress);
            }
        }


        if (NetworkUtils.isValidIPAddress(hostAddress))
        {
            byte[] addr = null;

            // attempt parse as IPv4 address
            addr = IPAddressUtil.textToNumericFormatV4(hostAddress);

            // if not IPv4, parse as IPv6 address
            if (addr == null)
            {
                addr = IPAddressUtil.textToNumericFormatV6(hostAddress);
            }
            return InetAddress.getByAddress(hostAddress, addr);
        }
        else
        {
            return InetAddress.getByName(hostAddress);
        }
    }

    /**
     * Returns array of hosts from the SRV record of the specified domain.
     * The records are ordered against the SRV record priority
     * @param domain the name of the domain we'd like to resolve (_proto._tcp
     * included).
     * @param port 
     * @return an array of InetSocketAddress containing records returned by the DNS
     * server - address and port .
     * @throws ParseException if <tt>domain</tt> is not a valid domain name.
     */
    public static InetSocketAddress getARecord(String domain, int port)
        throws ParseException
    {
        Record[] records = null;
        try
        {
            Lookup lookup = new Lookup(domain, Type.A);
            records = lookup.run();
        }
        catch (TextParseException tpe)
        {
            logger.error("Failed to parse domain="+domain, tpe);
            throw new ParseException(tpe.getMessage(), 0);
        }
        if (records != null && records.length > 0)
        {
            return new InetSocketAddress(
                ((ARecord)records[0]).getAddress(),
                port);
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns array of hosts from the SRV record of the specified domain.
     * The records are ordered against the SRV record priority
     * @param domain the name of the domain we'd like to resolve (_proto._tcp
     * included).
     * @param port 
     * @return an array of InetSocketAddress containing records returned by the DNS
     * server - address and port .
     * @throws ParseException if <tt>domain</tt> is not a valid domain name.
     */
    public static InetSocketAddress getAAAARecord(String domain, int port)
        throws ParseException
    {
        Record[] records = null;
        try
        {
            Lookup lookup = new Lookup(domain, Type.AAAA);
            records = lookup.run();
        }
        catch (TextParseException tpe)
        {
            logger.error("Failed to parse domain="+domain, tpe);
            throw new ParseException(tpe.getMessage(), 0);
        }
        if (records != null && records.length > 0)
        {
            return new InetSocketAddress(
                ((AAAARecord)records[0]).getAddress(),
                port);
        }
        else
        {
            return null;
        }
    }

    /**
     * Tries to determine if this host supports IPv6 addresses (i.e. has at
     * least one IPv6 address) and returns IN6_ADDR_ANY or IN4_ADDR_ANY
     * accordingly. This method is only used to initialize IN_ADDR_ANY so that
     * it could be used when binding sockets. The reason we need it is because
     * on mac (contrary to lin or win) binding a socket on 0.0.0.0 would make
     * it deaf to IPv6 traffic. Binding on ::0 does the trick but that would
     * fail on hosts that have no IPv6 support. Using the result of this method
     * provides an easy way to bind sockets in cases where we simply want any
     * IP packets coming on the port we are listening on (regardless of IP
     * version).
     *
     * @return IN6_ADDR_ANY or IN4_ADDR_ANY if this host supports or not IPv6.
     */
    private static String determineAnyAddress()
    {
        Enumeration<NetworkInterface> ifaces;
        try
        {
            ifaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Couldn't retrieve local interfaces.", e);
            return IN4_ADDR_ANY;
        }

        while(ifaces.hasMoreElements())
        {
            Enumeration<InetAddress> addrs
                                = ifaces.nextElement().getInetAddresses();
            while (addrs.hasMoreElements())
            {
                if(addrs.nextElement() instanceof Inet6Address)
                    return IN6_ADDR_ANY;
            }
        }

        return IN4_ADDR_ANY;
    }

    /**
     * Determines whether <tt>port</tt> is a valid port number bindable by an
     * application (i.e. an integer between 1024 and 65535).
     *
     * @param port the port number that we'd like verified.
     *
     * @return <tt>true</tt> if port is a valid and bindable port number and
     * <tt>alse</tt> otherwise.
     */
    public static boolean isValidPortNumber(int port)
    {
        return MIN_PORT_NUMBER < port && port < MAX_PORT_NUMBER;

    }
}
