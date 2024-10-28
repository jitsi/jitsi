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
package net.java.sip.communicator.impl.dns;

import lombok.extern.slf4j.*;
import org.jitsi.service.packetlogging.*;
import org.xbill.DNS.*;

import java.net.*;

/**
 * Custom logger that will log packages using packet logging service.
 *
 * @author Damian Minkov
 */
@Slf4j
public class DnsJavaLogger
    implements PacketLogger
{
    private final PacketLoggingService packetLoggingService;

    public DnsJavaLogger(PacketLoggingService packetLoggingService)
    {
        this.packetLoggingService = packetLoggingService;
    }

    @Override
    public void log(String prefix,
                    SocketAddress local,
                    SocketAddress remote,
                    byte[] data)
    {
        // make sure that error here will not stop further processing
        try
        {
            logInternal(local, remote, prefix, data);
        }
        catch(Throwable t)
        {
            logger.error("Error saving packet", t);
        }
    }

    /**
     * Logs the dns packet, checking its prefix message to distinguish
     * incoming and outgoing messages and the transport used TCP or UDP.
     *
     * @param local the local address
     * @param remote the remote address
     * @param prefix the prefix used by the dns lib
     * @param data the data that is send or received through the wire
     */
    private void logInternal(SocketAddress local,
                    SocketAddress remote,
                    String prefix, byte[] data)
    {
        if(packetLoggingService == null
            || !(local instanceof InetSocketAddress && remote instanceof InetSocketAddress)
            || !packetLoggingService.isLoggingEnabled(PacketLoggingService.ProtocolName.DNS))
        {
            return;
        }

        InetSocketAddress localAddress = (InetSocketAddress)local;
        InetSocketAddress remoteAddress = (InetSocketAddress)remote;

        PacketLoggingService.TransportName transportName
            = PacketLoggingService.TransportName.UDP;

        if(prefix.contains("TCP"))
            transportName = PacketLoggingService.TransportName.TCP;

        boolean isSender = !prefix.contains("read");

        byte[] srcAddr;
        int srcPort;
        byte[] dstAddr;
        int dstPort;

        if(isSender)
        {
            srcAddr = localAddress.getAddress().getAddress();
            srcPort = localAddress.getPort();
            dstAddr = remoteAddress.getAddress().getAddress();
            dstPort = remoteAddress.getPort();
        }
        else
        {
            dstAddr = localAddress.getAddress().getAddress();
            dstPort = localAddress.getPort();
            srcAddr = remoteAddress.getAddress().getAddress();
            srcPort = remoteAddress.getPort();
        }

        packetLoggingService.logPacket(
            PacketLoggingService.ProtocolName.DNS,
            srcAddr,
            srcPort,
            dstAddr,
            dstPort,
            transportName,
            isSender,
            data,
            0,
            data.length);
    }
}
