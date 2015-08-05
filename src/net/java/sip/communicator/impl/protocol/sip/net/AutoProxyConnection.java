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

import static javax.sip.ListeningPoint.TCP;
import static javax.sip.ListeningPoint.TLS;
import static javax.sip.ListeningPoint.UDP;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.SERVER_ADDRESS;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.USER_ID;

import java.net.*;
import java.text.*;

import javax.sip.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of the autodetect proxy connection. Tries to resolve a SIP-
 * server by querying DNS in this order: NAPTR-SRV-A; SRV-A; A.
 *
 * @author Ingo Bauersachs
 */
public class AutoProxyConnection
    extends ProxyConnection
{
    private enum State
    {
        New,
        Naptr,
        NaptrSrv,
        NaptrSrvHosts,
        NaptrSrvHostIPs,
        Srv,
        SrvHosts,
        SrvHostIPs,
        Hosts,
        IP
    }

    /**
     * Wrapper around {@link NetworkUtils} to support Unit Tests.
     */
    protected static class LocalNetworkUtils
    {
        public InetAddress getInetAddress(String address)
            throws UnknownHostException
        {
            return NetworkUtils.getInetAddress(address);
        }

        public String[][] getNAPTRRecords(String address)
            throws ParseException, DnssecException
        {
            return NetworkUtils.getNAPTRRecords(address);
        }

        public SRVRecord[] getSRVRecords(String service, String proto,
            String address) throws ParseException, DnssecException
        {
            return NetworkUtils.getSRVRecords(service, proto, address);
        }

        public InetSocketAddress[] getAandAAAARecords(String target, int port)
            throws ParseException, DnssecException
        {
            return NetworkUtils.getAandAAAARecords(target, port);
        }

        public boolean isValidIPAddress(String address)
        {
            return NetworkUtils.isValidIPAddress(address);
        }

        public SRVRecord[] getSRVRecords(String domain)
            throws ParseException, DnssecException
        {
            return NetworkUtils.getSRVRecords(domain);
        }
    }

    private final static Logger logger
        = Logger.getLogger(AutoProxyConnection.class);

    private State state;
    private String address;
    private int port;
    private final String defaultTransport;
    private LocalNetworkUtils nu = new LocalNetworkUtils();

    private final static String[] transports = new String[]
    {
        ListeningPoint.TLS,
        ListeningPoint.TCP,
        ListeningPoint.UDP
    };
    private boolean hadSrvResults;
    private String[][] naptrRecords;
    private int naptrIndex;
    private SRVRecord[] srvRecords;
    private int srvRecordsIndex;
    private int srvTransportIndex;
    private InetSocketAddress socketAddresses[];
    private int socketAddressIndex;

    /**
     * Creates a new instance of this class. Uses the server from the account.
     *
     * @param account the account of this SIP protocol instance
     * @param defaultTransport the default transport to use when DNS does not
     *            provide a protocol through NAPTR or SRV
     */
    public AutoProxyConnection( SipAccountIDImpl account,
                                String defaultTransport )
    {
        super(account);
        port = ListeningPoint.PORT_5060;
        this.defaultTransport = defaultTransport;
        reset();
    }

    /**
     * Creates a new instance of this class. Uses the supplied address instead
     * of the server address from the account.
     *
     * @param account the account of this SIP protocol instance
     * @param address the domain on which to perform autodetection
     * @param port the destination socket port
     * @param defaultTransport the default transport to use when DNS does not
     *            provide a protocol through NAPTR or SRV
     */
    public AutoProxyConnection( SipAccountIDImpl account, String address,
                                int port, String defaultTransport )
    {
        super(account);
        this.defaultTransport = defaultTransport;
        this.port = port;
        reset();
        this.address = address;
        if (nu.isValidIPAddress(this.address))
        {
            this.state = State.IP;
        }
    }

    /**
     * Sets the NetworkUtils wrapper. Used for Unit-Testing.
     * @param nu the the NetworkUtils wrapper.
     */
    protected void setNetworkUtils(LocalNetworkUtils nu)
    {
        this.nu = nu;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#
     * getNextAddressFromDns()
     */
    @Override
    protected boolean getNextAddressFromDns()
        throws DnssecException
    {
        try
        {
            return getNextAddressInternal();
        }
        catch(ParseException ex)
        {
            logger.error("Unable to get DNS data for <" + address
                + "> in state" + state, ex);
        }
        return false;
    }

    /**
     * Gets the next address from DNS.
     *
     * @throws DnssecException When a DNSSEC failure occured during the lookup.
     * @throws ParseException When a domain name (possibly returned from DNS
     *             itself) is invalid.
     */
    private boolean getNextAddressInternal()
        throws DnssecException, ParseException
    {
        switch(state)
        {
            case New:
                state = State.Naptr;
                return getNextAddressFromDns();
            case IP:
                if(socketAddressIndex == 0)
                {
                    socketAddressIndex++;
                    try
                    {
                        socketAddress = new InetSocketAddress(
                            nu.getInetAddress(address),
                            ListeningPoint.TLS.equalsIgnoreCase(transport)
                                ? ListeningPoint.PORT_5061
                                : ListeningPoint.PORT_5060
                        );
                    }
                    catch (UnknownHostException e)
                    {
                        //this is not supposed to happen
                        logger.error("invalid IP address: " + address, e);
                        return false;
                    }
                    transport = defaultTransport;
                    return true;
                }
                return false;
            case Naptr:
                naptrRecords = nu.getNAPTRRecords(address);
                if(naptrRecords != null && naptrRecords.length > 0)
                {
                    state = State.NaptrSrv;
                    naptrIndex = 0;
                }
                else
                {
                    hadSrvResults = false;
                    state = State.Srv;
                    srvTransportIndex = 0;
                }

                return getNextAddressFromDns();
            case NaptrSrv:
                for(; naptrIndex < naptrRecords.length; naptrIndex++)
                {
                    srvRecords = nu.getSRVRecords(
                        naptrRecords[naptrIndex][2]);
                    if(srvRecords != null && srvRecords.length > 0)
                    {
                        state = State.NaptrSrvHosts;
                        if(TLS.equalsIgnoreCase(naptrRecords[naptrIndex][1]))
                            transport = TLS;
                        else if(TCP.equalsIgnoreCase(naptrRecords[naptrIndex][1]))
                            transport = TCP;
                        else
                            transport = UDP;
                        srvRecordsIndex = 0;
                        if(getNextAddressFromDns())
                        {
                            naptrIndex++;
                            return true;
                        }
                    }
                }
                return false; //no more naptr's
            case NaptrSrvHosts:
                for(; srvRecordsIndex < srvRecords.length; srvRecordsIndex++)
                {
                    socketAddresses = nu.getAandAAAARecords(
                        srvRecords[srvRecordsIndex].getTarget(),
                        srvRecords[srvRecordsIndex].getPort());
                    if(socketAddresses != null && socketAddresses.length > 0)
                    {
                        state = State.NaptrSrvHostIPs;
                        socketAddressIndex = 0;
                        if(getNextAddressFromDns())
                        {
                            srvRecordsIndex++;
                            return true;
                        }
                    }
                }
                state = State.NaptrSrv;
                return getNextAddressFromDns(); //backtrack to next naptr
            case NaptrSrvHostIPs:
                if(socketAddressIndex >= socketAddresses.length)
                {
                    state = State.NaptrSrvHosts;
                    return getNextAddressFromDns(); //backtrack to next srv
                }
                socketAddress = socketAddresses[socketAddressIndex];
                socketAddressIndex++;
                return true;
            case Srv:
                for(;srvTransportIndex < transports.length; srvTransportIndex++)
                {
                    srvRecords = nu.getSRVRecords(
                        (TLS.equals(transports[srvTransportIndex])
                            ? "sips"
                            : "sip"),
                        (UDP.equalsIgnoreCase(transports[srvTransportIndex])
                            ? UDP
                            : TCP),
                        address);
                    if(srvRecords != null && srvRecords.length > 0)
                    {
                        hadSrvResults = true;
                        state = State.SrvHosts;
                        srvRecordsIndex = 0;
                        transport = transports[srvTransportIndex];
                        if(getNextAddressFromDns())
                        {
                            srvTransportIndex++;
                            return true;
                        }
                    }
                }
                if(!hadSrvResults)
                {
                    state = State.Hosts;
                    socketAddressIndex = 0;
                    return getNextAddressFromDns();
                }
                return false;
            case SrvHosts:
                if(srvRecordsIndex >= srvRecords.length)
                {
                    state = State.Srv;
                    return getNextAddressFromDns(); //backtrack to next srv record
                }
                for(; srvRecordsIndex < srvRecords.length; srvRecordsIndex++)
                {
                    socketAddresses = nu.getAandAAAARecords(
                        srvRecords[srvRecordsIndex].getTarget(),
                        srvRecords[srvRecordsIndex].getPort());
                    if(socketAddresses != null && socketAddresses.length > 0)
                    {
                        state = State.SrvHostIPs;
                        socketAddressIndex = 0;
                        if(getNextAddressFromDns())
                        {
                            srvRecordsIndex++;
                            return true;
                        }
                    }
                }
                return false;
            case SrvHostIPs:
                if(socketAddressIndex >= socketAddresses.length)
                {
                    state = State.SrvHosts;
                    return getNextAddressFromDns();
                }
                socketAddress = socketAddresses[socketAddressIndex];
                socketAddressIndex++;
                return true;
            case Hosts:
                transport = defaultTransport;

                if(socketAddresses == null)
                {
                    socketAddresses = nu.getAandAAAARecords(
                        address,
                        port);
                }

                if(socketAddresses != null && socketAddresses.length > 0
                    && socketAddressIndex < socketAddresses.length)
                {
                    socketAddress = socketAddresses[socketAddressIndex++];
                    return true;
                }
                return false;
        }
        return false;
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
        state = State.New;

        //determine the hostname of the proxy for autodetection:
        //1) server part of the user ID
        //2) name of the registrar when the user ID contains no domain
        String userID =  account.getAccountPropertyString(USER_ID);
        int domainIx = userID.indexOf("@");
        if(domainIx > 0)
        {
            address = userID.substring(domainIx + 1);
        }
        else
        {
            address = account.getAccountPropertyString(SERVER_ADDRESS);
            if(address == null || address.trim().length() == 0)
            {
                //registrarless account
                return;
            }
        }
        if(nu.isValidIPAddress(address))
        {
            state = State.IP;
            socketAddressIndex = 0;
        }
    }
}
