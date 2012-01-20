/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.debugger;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.service.packetlogging.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

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
    private byte[] localAddress;

    /**
     * Remote address for the connection.
     */
    private byte[] remoteAddress;

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
                    PacketLoggingService.ProtocolName.JABBER)
                && packet != null && connection.getSocket() != null)
            {
                if(remoteAddress == null)
                {
                    remoteAddress = connection.getSocket()
                        .getInetAddress().getAddress();
                    localAddress = connection.getSocket()
                        .getLocalAddress().getAddress();
                }

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
