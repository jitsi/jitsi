/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.net.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * Utility methods and fields to use when working with network addresses.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class NetworkUtils
{
    /**
     * A string containing the "any" local address.
     */
    public static final String IN_ADDR_ANY = "0.0.0.0";

    /**
     * The maximum int value that could correspond to a port nubmer.
     */
    public static final int    MAX_PORT_NUMBER = 65535;

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
        return portNumberGenerator.nextInt(MAX_PORT_NUMBER-1024) + 1024;
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
     * @param domain String
     * @return String[]
     */
    public static String[] getSRVRecords(String domain)
        throws NamingException
    {
        InitialDirContext iDirC = new InitialDirContext();
        Attributes attributes =
            iDirC.getAttributes("dns:/" + domain, new String[]{"SRV"});
        Attribute attributeMX = attributes.get("SRV");

        String[][] pvhn = new String[attributeMX.size()][2];
        for(int i = 0; i < attributeMX.size(); i++)
        {
            pvhn[i] = ("" + attributeMX.get(i)).split("\\s+");
        }

        // sort the SRV RRs by RR value (lower is preferred)
        Arrays.sort(pvhn, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                return(Integer.parseInt(((String[])o1)[0]) -
                       Integer.parseInt(((String[])o2)[0]));
            }
        });

        // put sorted host names in an array, get rid of any trailing '.'
        String[] sortedHostNames = new String[pvhn.length];
        for(int i = 0; i < pvhn.length; i++)
        {
            sortedHostNames[i] = pvhn[i][3].endsWith(".") ?
                pvhn[i][3].substring(0, pvhn[i][3].length() - 1) : pvhn[i][3];
        }

       return sortedHostNames;
    }
}
