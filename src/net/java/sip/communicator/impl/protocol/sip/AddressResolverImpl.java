/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.core.net.*;
import gov.nist.javax.sip.stack.*;

import java.net.*;

import javax.sip.*;
import javax.sip.address.*;

import net.java.sip.communicator.util.*;

/**
 * Lookup for SRV records for given host. If nothing found
 * the original host is returned this way when a Socket
 * is constructed another dns lookup will be made for the A record.
 *
 * @author Damian Minkov
 * @author Alan Kelly
 * @author Emil Ivov
 */
public class AddressResolverImpl
    implements AddressResolver
{
    /**
     * Our class logger
     */
    private static final Logger logger
        = Logger.getLogger(AddressResolverImpl.class);

    /**
     * Implements the actual resolving. This is where we do the DNS queries.
     *
     * @param inputAddress the unresolved <tt>Hop</tt> that we'd need to find
     * an address for.
     *
     * @return the newly created <tt>Hop</tt> containing the resolved
     * destination.
     */
    public Hop resolveAddress(Hop inputAddress)
    {
        try
        {
            String transport = inputAddress.getTransport();
            String hostAddress = inputAddress.getHost();

            if (transport == null)
                transport = ListeningPoint.UDP;

            String host = null;
            int port = 0;

            // if it is a textual IP address, do no try to resolve it
            if(NetworkUtils.isValidIPAddress(hostAddress))
            {
                byte[] addr = null;

                addr = NetworkUtils.strToIPv4(hostAddress);

                // not an IPv4, try IPv6
                if (addr == null)
                {
                    addr = NetworkUtils.strToIPv6(hostAddress);
                }

                InetSocketAddress hostSocketAddress = new InetSocketAddress(
                        InetAddress.getByAddress(hostAddress, addr),
                        inputAddress.getPort());
                return new HopImpl(hostSocketAddress.getHostName(),
                        inputAddress.getPort(),
                        transport);
            }
            else if (transport.equalsIgnoreCase(ListeningPoint.TLS))
            {
                SRVRecord srvRecord = NetworkUtils.getSRVRecord(
                        "sips", ListeningPoint.TCP, hostAddress);
                if(srvRecord != null)
                {
                    host = srvRecord.getTarget();
                    port = srvRecord.getPort();
                }
            }
            else
            {
                SRVRecord srvRecord = NetworkUtils.getSRVRecord(
                        "sip", transport, hostAddress);
                if(srvRecord != null)
                {
                    host = srvRecord.getTarget();
                    port = srvRecord.getPort();
                }
            }

            if (host != null)
            {
                if(logger.isTraceEnabled())
                    logger.trace("Returning hop as follows"
                                    + " host= " + host
                                    + " port= " + port
                                    + " transport= " + transport);

                return
                    new HopImpl(host, port, transport);
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
