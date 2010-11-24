/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net;

import java.net.*;
import java.text.*;
import java.util.*;

import org.xbill.DNS.*;

import net.java.sip.communicator.util.*;

/**
 * A simple DNS utility that works.
 *
 * @author Emil Ivov
 */
public class Host
{
    /**
     * The <tt>Logger</tt> used by the <tt>Host</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(Host.class.getName());

    /**
     * Returns array of hosts from the SRV record of the specified domain.
     * The records are ordered against the SRV record priority
     * @param domain the name of the domain we'd like to resolve (_proto._tcp
     * included).
     * @return an array of InetSocketAddress containing records returned by the
     * DNS server - address and port .
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
        ArrayList<InetSocketAddress> sortedHostNames
            = new ArrayList<InetSocketAddress>();
        for (int i = 0; i < pvhn.length; i++)
        {
            try
            {
                sortedHostNames.add(new InetSocketAddress(
                        Address.getByName(pvhn[i][3]),
                        Integer.valueOf(pvhn[i][2])));
            }
            catch(UnknownHostException e)
            {
                logger.warn("Unknown host: " + pvhn[i][3], e);
            }
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("DNS SRV query for domain " + domain + " returned:");
            for (int i = 0; i < sortedHostNames.size(); i++)
            {
                if (logger.isTraceEnabled())
                    logger.trace(sortedHostNames.get(i));
            }
        }
        return sortedHostNames.toArray(new InetSocketAddress[0]);
    }

    /**
     * Queries DNS servers
     *
     * @param args args
     *
     * @throws Throwable if anything goes wrong
     */
    public static void main(String[] args) throws Throwable
    {
        InetSocketAddress[] addrs
                            = getSRVRecords("_xmpp-client._tcp.emcho.com");

        for (InetSocketAddress addr : addrs)
        {
            //System.out.println("addr=" + addr.getAddress().getHostName());
        }
    }

}
