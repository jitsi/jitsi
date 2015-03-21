/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.dns;

import net.java.sip.communicator.util.*;
import org.jitsi.service.packetlogging.*;
import org.xbill.DNS.*;

import java.net.*;

/**
 * Custom logger that will log packages using packet logging service.
 *
 * @author Damian Minkov
 */
public class DnsJavaLogger
    implements PacketLogger
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(DnsJavaLogger.class);

    /**
     * The packet logging service.
     */
    private PacketLoggingService packetLoggingService = null;

    /**
     * Obtain packet logging service.
     * @return
     */
    private PacketLoggingService getPacketLoggingService()
    {
        if(packetLoggingService == null
            && UtilActivator.bundleContext != null)
        {
            packetLoggingService = ServiceUtils.getService(
                UtilActivator.bundleContext,
                PacketLoggingService.class);
        }

        return packetLoggingService;
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
        if(getPacketLoggingService() == null
            || !(local instanceof InetSocketAddress
                && remote instanceof InetSocketAddress))
        {
            return;
        }

        InetSocketAddress localAddress = (InetSocketAddress)local;
        InetSocketAddress remoteAddress = (InetSocketAddress)remote;

        PacketLoggingService.TransportName transportName
            = PacketLoggingService.TransportName.UDP;

        if(prefix.contains("TCP"))
            transportName = PacketLoggingService.TransportName.TCP;

        boolean isSender = true;
        if(prefix.contains("read"))
            isSender = false;

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

        getPacketLoggingService().logPacket(
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
