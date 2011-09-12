/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.debugger;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

import java.net.*;

/**
 * The jabber packet listener that logs the packets to the packet logging
 * service.
 * @author Damian Minkov
 */
public class SmackPacketDebugger
    implements PacketListener,
               PacketInterceptor
{
    /**
     * The current jabber connection.
     */
    private XMPPConnection connection = null;

    /**
     * Local address for the connection.
     */
    private byte[] localAddress = new byte[4];

    /**
     * Remote address for the connection.
     */
    private byte[] remoteAddress = new byte[4];

    /**
     * Instance for the packet logging service.
     */
    private PacketLoggingService packetLogging = null;

    /**
     * Creates the SmackPacketDebugger instance. 
     */
    public SmackPacketDebugger()
    {
        packetLogging = JabberActivator.getPacketLogging();
    }

    /**
     * Sets current connection.
     * @param connection the connection.
     */
    public void setConnection(XMPPConnection connection)
    {
        this.connection = connection;

        try
        {
            InetSocketAddress inetAddress;

            if(Boolean.getBoolean("java.net.preferIPv6Addresses"))
            {
                inetAddress = NetworkUtils.getAAAARecord(
                        connection.getHost(), 0);
            }
            else
            {
                inetAddress = NetworkUtils.getARecord(
                        connection.getHost(), 0);
            }

            if(inetAddress != null)
                remoteAddress = inetAddress.getAddress().getAddress();

            // to create empty ipv6 address default is ipv4
            if(remoteAddress.length != localAddress.length)
                localAddress = new byte[remoteAddress.length];
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }

    /**
     * Process the packet that is about to be sent to the server. The intercepted
     * packet can be modified by the interceptor.<p>
     * <p/>
     * Interceptors are invoked using the same thread that requested the packet
     * to be sent, so it's very important that implementations of this method
     * not block for any extended period of time.
     *
     * @param packet the packet to is going to be sent to the server.
     */
    public void interceptPacket(Packet packet)
    {
        try
        {
            if(packetLogging.isLoggingEnabled(
                    PacketLoggingService.ProtocolName.JABBER))
            {
                packetLogging.logPacket(
                        PacketLoggingService.ProtocolName.JABBER,
                        localAddress,
                        connection.getSocket().getLocalPort(),
                        remoteAddress,
                        connection.getPort(),
                        PacketLoggingService.TransportName.TCP,
                        true,
                        packet.toXML().getBytes("UTF-8")
                    );
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }

    /**
     * Process the next packet sent to this packet listener.<p>
     * <p/>
     * A single thread is responsible for invoking all listeners, so
     * it's very important that implementations of this method not block
     * for any extended period of time.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        try
        {
            if(packetLogging.isLoggingEnabled(
                    PacketLoggingService.ProtocolName.JABBER)
                && packet != null && connection.getSocket() != null)
            {
                packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.JABBER,
                    remoteAddress,
                    connection.getPort(),
                    localAddress,
                    connection.getSocket().getLocalPort(),
                    PacketLoggingService.TransportName.TCP,
                    false,
                    packet.toXML().getBytes("UTF-8")
                );
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }
}
