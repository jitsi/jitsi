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
package net.java.sip.communicator.impl.protocol.sip.net;

import static javax.sip.ListeningPoint.PORT_5060;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.PREFERRED_TRANSPORT;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.PROXY_ADDRESS;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.PROXY_PORT;

import java.net.*;
import java.text.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of the manually configured SIP proxy connection. IP Address
 * lookups are performed using the account's proxy address.
 *
 * @author Ingo Bauersachs
 */
public class ManualProxyConnection
    extends ProxyConnection
{
    private final static Logger logger
        = Logger.getLogger(ManualProxyConnection.class);

    private String address;
    private int port;

    private InetSocketAddress[] lookups;
    private int lookupIndex;

    /**
     * Creates a new instance of this class. Uses the server from the account.
     *
     * @param account the account of this SIP protocol instance
     */
    public ManualProxyConnection(SipAccountIDImpl account)
    {
        super(account);
        reset();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#
     * getNextAddress()
     */
    @Override
    public boolean getNextAddressFromDns()
        throws DnssecException
    {
        if(lookups == null)
        {
            try
            {
                lookupIndex = 0;
                lookups = NetworkUtils.getAandAAAARecords(address, port);

                //no result found, reset state and indicate "out of addresses"
                if(lookups.length == 0)
                {
                    lookups = null;
                    return false;
                }
            }
            catch (ParseException e)
            {
                logger.error("Invalid address <" + address + ">", e);
                return false;
            }
        }

        //check if the available addresses are exhausted
        if(lookupIndex >= lookups.length)
        {
            if(logger.isDebugEnabled())
                logger.debug("No more addresses for " + account);
            lookups = null;
            return false;
        }

        //assign the next address and return lookup success
        if(logger.isDebugEnabled())
            logger.debug("Returning <" + socketAddress
                + "> as next address for " + account);
        socketAddress = lookups[lookupIndex];
        lookupIndex++;
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#reset()
     */
    @Override
    public void reset()
    {
        super.reset();
        address = account.getAccountPropertyString(PROXY_ADDRESS);
        port = account.getAccountPropertyInt(PROXY_PORT, PORT_5060);
        transport = account.getAccountPropertyString(PREFERRED_TRANSPORT);

        //check property sanity
        if(!ProtocolProviderServiceSipImpl.isValidTransport(transport))
            throw new IllegalArgumentException(
                transport + " is not a valid SIP transport");
    }
}
