/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.packetlogging.*;

/**
 * RTPConnectorOutputStream implementation for UDP protocol.
 *
 * @author Sebastien Vincent
 */
public class RTPConnectorUDPOutputStream
    extends RTPConnectorOutputStream
{
    /**
     * UDP socket used to send packet data
     */
    private final DatagramSocket socket;

    /**
     * Initializes a new <tt>RTPConnectorUDPOutputStream</tt>.
     *
     * @param socket a <tt>DatagramSocket</tt>
     */
    public RTPConnectorUDPOutputStream(DatagramSocket socket)
    {
        this.socket = socket;
    }

    /**
     * Send the packet from this <tt>OutputStream</tt>.
     *
     * @param packet packet to sent
     * @param target the target
     * @throws IOException if something goes wrong during sending
     */
    @Override
    protected void sendToTarget(RawPacket packet,
        InetSocketAddress target)
        throws IOException
    {
        socket.send(new DatagramPacket(
            packet.getBuffer(),
            packet.getOffset(),
            packet.getLength(),
            target.getAddress(),
            target.getPort()));
    }

    /**
     * Log the packet.
     *
     * @param packet packet to log
     */
    @Override
    protected void doLogPacket(RawPacket packet, InetSocketAddress target)
    {
        NeomediaActivator.getPacketLogging()
            .logPacket(
                PacketLoggingService.ProtocolName.RTP,
                socket.getLocalAddress().getAddress(),
                socket.getLocalPort(),
                target.getAddress().getAddress(),
                target.getPort(),
                PacketLoggingService.TransportName.UDP,
                true,
                packet.getBuffer(),
                packet.getOffset(),
                packet.getLength());
    }

    /**
     * Returns whether or not this <tt>RTPConnectorOutputStream</tt> has a valid
     * socket.
     *
     * @returns true if this <tt>RTPConnectorOutputStream</tt> has a valid
     * socket, false otherwise
     */
    @Override
    protected boolean isSocketValid()
    {
        return (socket != null);
    }
}
