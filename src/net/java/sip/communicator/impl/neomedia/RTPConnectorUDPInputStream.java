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
 * RTPConnectorInputStream implementation for UDP protocol.
 *
 * @author Sebastien Vincent
 */
public class RTPConnectorUDPInputStream
    extends RTPConnectorInputStream
{
    /**
     * UDP socket used to receive data.
     */
    private final DatagramSocket socket;

    /**
     * Receive size configured flag.
     */
    private boolean receivedSizeFlag = false;

    /**
     * Initializes a new <tt>RTPConnectorInputStream</tt> which is to receive
     * packet data from a specific UDP socket.
     *
     * @param socket the UDP socket the new instance is to receive data from
     */
    public RTPConnectorUDPInputStream(DatagramSocket socket)
    {
        this.socket = socket;

        if(socket != null)
        {
            closed = false;
            receiverThread = new Thread(this);
            receiverThread.start();
        }
    }

    /**
     * Close this stream, stops the worker thread.
     */
    @Override
    public synchronized void close()
    {
        closed = true;
        if(socket != null)
        {
            socket.close();
        }
    }

    /**
     * Log the packet.
     *
     * @param p packet to log
     */
    protected void doLogPacket(DatagramPacket p)
    {
        if(socket.getLocalAddress() == null)
            return;

        PacketLoggingService packetLogging
            = NeomediaActivator.getPacketLogging();

        if (packetLogging != null)
            packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.RTP,
                    p.getAddress().getAddress(),
                    p.getPort(),
                    socket.getLocalAddress().getAddress(),
                    socket.getLocalPort(),
                    PacketLoggingService.TransportName.UDP,
                    false,
                    p.getData(),
                    p.getOffset(),
                    p.getLength());
    }

    /**
     * Receive packet.
     *
     * @param p packet for receiving
     * @throws IOException if something goes wrong during receiving
     */
    protected void receivePacket(DatagramPacket p)
        throws IOException
    {
        if(!receivedSizeFlag)
        {
            receivedSizeFlag = true;

            try
            {
                socket.setReceiveBufferSize(65535);
            }
            catch(Throwable t)
            {
            }
        }
        socket.receive(p);
    }
}
