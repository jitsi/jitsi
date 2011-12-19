/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.net;

import static javax.sip.ListeningPoint.*;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.*;

import java.net.*;
import java.text.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.dns.*;

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
    public ManualProxyConnection(SipAccountID account)
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
