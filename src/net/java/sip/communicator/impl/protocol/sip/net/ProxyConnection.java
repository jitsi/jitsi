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

import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.PROXY_AUTO_CONFIG;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.dns.*;

/**
 * Abstract class for the determining the address for the SIP proxy.
 *
 * @author Ingo Bauersachs
 */
public abstract class ProxyConnection
{
    private List<String> returnedAddresses = new LinkedList<String>();

    protected String transport;
    protected InetSocketAddress socketAddress;
    protected final SipAccountIDImpl account;

    /**
     * Creates a new instance of this class.
     * @param account the account of this SIP protocol instance
     */
    protected ProxyConnection(SipAccountIDImpl account)
    {
        this.account = account;
    }

    /**
     * Gets the address to use for the next connection attempt.
     * @return the address of the last lookup.
     */
    public final InetSocketAddress getAddress()
    {
        return socketAddress;
    }

    /**
     * Gets the transport to use for the next connection attempt.
     * @return the transport of the last lookup.
     */
    public final String getTransport()
    {
        return transport;
    }

    /**
     * In case we are using an outbound proxy this method returns
     * a suitable string for use with Router.
     * The method returns <tt>null</tt> otherwise.
     *
     * @return the string of our outbound proxy if we are using one and
     * <tt>null</tt> otherwise.
     */
    public final String getOutboundProxyString()
    {
        if(socketAddress == null)
            return null;

        InetAddress proxyAddress = socketAddress.getAddress();
        StringBuilder proxyStringBuffer
            = new StringBuilder(proxyAddress.getHostAddress());

        if(proxyAddress instanceof Inet6Address)
        {
            proxyStringBuffer.insert(0, '[');
            proxyStringBuffer.append(']');
        }

        proxyStringBuffer.append(':');
        proxyStringBuffer.append(socketAddress.getPort());
        proxyStringBuffer.append('/');
        proxyStringBuffer.append(transport);

        return proxyStringBuffer.toString();
    }

    /**
     * Compares an InetAddress against the active outbound proxy. The comparison
     * is by reference, not equals.
     *
     * @param addressToTest The addres to test.
     * @return True when the InetAddress is the same as the outbound proxy.
     */
    public final boolean isSameInetAddress(InetAddress addressToTest)
    {
        // if the proxy is not yet initialized then this is not the provider
        // that caused this comparison
        if(socketAddress == null)
            return false;
        return addressToTest == socketAddress.getAddress();
    }

    /**
     * Retrieves the next address to use from DNS. Duplicate results are
     * suppressed.
     *
     * @return True if a new address is available through {@link #getAddress()},
     *         false if the last address was reached. A new lookup from scratch
     *         can be started by calling {@link #reset()}.
     * @throws DnssecException if there is a problem related to DNSSEC
     */
    public final boolean getNextAddress() throws DnssecException
    {
        boolean result;
        String key = null;
        do
        {
            result = getNextAddressFromDns();
            if(result && socketAddress != null)
            {
                key = getOutboundProxyString();
                if(!returnedAddresses.contains(key))
                {
                    returnedAddresses.add(key);
                    break;
                }
            }
        }
        while(result && returnedAddresses.contains(key));
        return result;
    }

    /**
     * Implementations must use this method to get the next address, but do not
     * have to care about duplicate addresses.
     *
     * @return True when a further address was available.
     * @throws DnssecException when a DNSSEC validation failure occured.
     */
    protected abstract boolean getNextAddressFromDns()
        throws DnssecException;

    /**
     * Resets the lookup to it's initial state. Overriders methods have to call
     * this method through a super-call.
     */
    public void reset()
    {
        returnedAddresses.clear();
    }

    /**
     * Factory method to create a proxy connection based on the account settings
     * of the protocol provider.
     *
     * @param pps the protocol provider that needs a SIP server connection.
     * @return An instance of a derived class.
     */
    public static ProxyConnection create(ProtocolProviderServiceSipImpl pps)
    {
        if (pps.getAccountID().getAccountPropertyBoolean(PROXY_AUTO_CONFIG,
            true))
            return new AutoProxyConnection((SipAccountIDImpl) pps.getAccountID(),
                pps.getDefaultTransport());
        else
            return new ManualProxyConnection((SipAccountIDImpl) pps.getAccountID());
    }
}
