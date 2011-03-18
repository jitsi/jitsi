/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.net.*;
import java.text.*;

/**
 * This class describes an DNS'S SRV record.
 *
 * @author Sebastien Vincent
 */
public class SRVRecord
{
    /**
     * DNSJava SRVRecord.
     */
    org.xbill.DNS.SRVRecord record;

    /**
     * Constructor.
     *
     * @param record DNSJava SRVRecord
     */
    public SRVRecord(org.xbill.DNS.SRVRecord record)
    {
        this.record = record;
    }

    /**
     * Get <tt>InetSocketAddress</tt> for this record.
     *
     * @return <tt>InetSocketAddress</tt>
     * @throws ParseException if an error occurred during address parsing
     */
    public InetSocketAddress getInetSocketAddress()
        throws ParseException
    {
        InetSocketAddress address =
            NetworkUtils.getARecord(getTarget(), getPort());
        return address;
    }

    /**
     * Get port.
     *
     * @return port
     */
    public int getPort()
    {
        return record.getPort();
    }

    /**
     * Get target.
     *
     * @return target
     */
    public String getTarget()
    {
        return record.getTarget().toString();
    }

    /**
     * Get priority.
     *
     * @return priority
     */

    public int getPriority()
    {
        return record.getPriority();
    }

    /**
     * Get weight.
     *
     * @return weight
     */
    public int getWeight()
    {
        return record.getWeight();
    }

    /**
     * Get DNS TTL.
     *
     * @return DNS TTL
     */

    public long getTTL()
    {
        return record.getTTL();
    }

    /**
     * Get domain name.
     *
     * @return domain name
     */
    public String getName()
    {
        return record.getName().toString();
    }
}
