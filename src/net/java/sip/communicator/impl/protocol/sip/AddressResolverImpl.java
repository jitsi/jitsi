/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import javax.sip.*;
import javax.sip.address.*;
import gov.nist.core.net.*;
import gov.nist.javax.sip.stack.*;
import net.java.sip.communicator.util.*;


/**
 * Lookup for SRV records for given host. If nothing found
 * the original host is returned this way when a Socket
 * is constructed another dns lookup will be made for the A record.
 *
 * @author Damian Minkov
 */
public class AddressResolverImpl
    implements AddressResolver
{
    private static final Logger logger
        = Logger.getLogger(AddressResolverImpl.class);

    public Hop resolveAddress(Hop inputAddress)
    {
        try
        {
            String lookupStr = null;

            if(inputAddress.getTransport().equalsIgnoreCase(ListeningPoint.UDP))
                lookupStr = "_sip._udp." + inputAddress.getHost();
            else if(inputAddress.getTransport().equalsIgnoreCase(ListeningPoint.TCP))
                lookupStr = "_sip._tcp." + inputAddress.getHost();
            else if(inputAddress.getTransport().equalsIgnoreCase(ListeningPoint.TLS))
                lookupStr = "_sips._tcp." + inputAddress.getHost();

            InetSocketAddress hosts[] = NetworkUtils.getSRVRecords(lookupStr);

            if(hosts != null && hosts.length > 0)
            {
                logger.trace("Will set server address from SRV records "
                   + hosts[0]);

                return new HopImpl(
                    hosts[0].getHostName(),
                    hosts[0].getPort(),
                    inputAddress.getTransport());
            }
        }
        catch (Exception ex)
        {
            logger.error("Domain not resolved " + ex.getMessage());
        }

        if  (inputAddress.getPort()  != -1)
        {
            return inputAddress;
        }
        else
        {
            return new HopImpl(inputAddress.getHost(),
                MessageProcessor.getDefaultPort(
                    inputAddress.getTransport()),inputAddress.getTransport());
        }
    }
}
