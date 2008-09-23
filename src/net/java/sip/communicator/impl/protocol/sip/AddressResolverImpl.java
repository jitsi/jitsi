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
            InetSocketAddress host = null;

            String transport = inputAddress.getTransport();

            if (transport == null)
                transport = ListeningPoint.UDP;


            if (transport.equalsIgnoreCase(ListeningPoint.TLS))
            {
                host = NetworkUtils.getSRVRecord(
                        "sips", ListeningPoint.TCP, inputAddress.getHost());
            }
            else
            {
                host = NetworkUtils.getSRVRecord(
                        "sip", transport, inputAddress.getHost());
            }

            if(logger.isTraceEnabled())
                logger.trace("Returning hop as follows"
                                + " host= " + host.getHostName()
                                + " port= " + host.getPort()
                                + " transport= " + transport);


            return new HopImpl(host.getHostName(), host.getPort(), transport);

        }
        catch (Exception ex)
        {
            logger.error("Domain not resolved " + ex.getMessage());
        }

        if (inputAddress.getPort()  != -1)
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
