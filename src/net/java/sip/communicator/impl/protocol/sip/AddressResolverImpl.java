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
 * @author Alan Kelly
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
            String transport = inputAddress.getTransport();
            String hostAddress = inputAddress.getHost();

            if (transport == null)
                transport = ListeningPoint.UDP;

            InetSocketAddress host;

            // if it is a textual IP address, do no try to resolve it
            if(NetworkUtils.isValidIPAddress(hostAddress))
            {
                byte[] addr = null;

                addr = IPAddressUtil.textToNumericFormatV4(hostAddress);

                // not an IPv4, try IPv6
                if (addr == null)
                {
                    addr = IPAddressUtil.textToNumericFormatV6(hostAddress);
                }

                host = new InetSocketAddress(
                        InetAddress.getByAddress(hostAddress, addr),
                        inputAddress.getPort());
            }
            else if (transport.equalsIgnoreCase(ListeningPoint.TLS))
            {
                host = NetworkUtils.getSRVRecord(
                        "sips", ListeningPoint.TCP, hostAddress);
            }
            else
            {
                host = NetworkUtils.getSRVRecord(
                        "sip", transport, hostAddress);
            }

            if (host != null)
            {
                if(logger.isTraceEnabled())
                    logger.trace("Returning hop as follows"
                                    + " host= " + host.getHostName()
                                    + " port= " + host.getPort()
                                    + " transport= " + transport);

                return
                    new HopImpl(host.getHostName(), host.getPort(), transport);
            }
        }
        catch (Exception ex)
        {
            //could mean there was no SRV record
            if(logger.isDebugEnabled())
                logger.debug("Domain "+ inputAddress
                                +" could not be resolved " + ex.getMessage());
            //show who called us
            if(logger.isTraceEnabled())
                logger.trace("Printing SRV resolution stack trace", ex);
        }

        Hop returnHop;

        if (inputAddress.getPort()  != -1)
        {
            returnHop = inputAddress;
        }
        else
        {
            String transport = inputAddress.getTransport();

            returnHop
                = new HopImpl(
                        inputAddress.getHost(),
                        MessageProcessor.getDefaultPort(transport),
                        transport);
        }

        if(logger.isDebugEnabled())
            logger.debug("Returning hop: " + returnHop);

        return returnHop;
    }
}
