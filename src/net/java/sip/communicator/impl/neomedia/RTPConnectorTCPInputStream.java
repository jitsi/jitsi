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
import net.java.sip.communicator.util.*;

/**
 * RTPConnectorInputStream implementation for TCP protocol.
 *
 * @author Sebastien Vincent
 */
public class RTPConnectorTCPInputStream
    extends RTPConnectorInputStream
{
    /**
     * The <tt>Logger</tt> used by instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(RTPConnectorTCPInputStream.class);

    /**
     * TCP socket used to receive data.
     */
    private final Socket socket;

    /**
     * Initializes a new <tt>RTPConnectorInputStream</tt> which is to receive
     * packet data from a specific TCP socket.
     *
     * @param socket the TCP socket the new instance is to receive data from
     */
    public RTPConnectorTCPInputStream(Socket socket)
    {
        this.socket = socket;

        if(socket != null)
        {
            try
            {
                socket.setReceiveBufferSize(65535);
            }
            catch(Throwable t)
            {
            }

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
            try
            {
                socket.close();
            }
            catch(IOException e)
            {
            }
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
                    (p.getAddress() != null)
                            ? p.getAddress().getAddress()
                            : new byte[] { 0,0,0,0 },
                    p.getPort(),
                    socket.getLocalAddress().getAddress(),
                    socket.getLocalPort(),
                    PacketLoggingService.TransportName.TCP,
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
        int len = -1;
        byte data[] = null;

        try
        {
            data = p.getData();
            InputStream stream = socket.getInputStream();
            len = stream.read(data);
        }
        catch(Exception e)
        {
            logger.info("problem read: " + e);
        }

        if(len > 0)
        {
            p.setData(data);
            p.setLength(len);
            p.setAddress(socket.getInetAddress());
            p.setPort(socket.getPort());
        }
        else
        {
            throw new IOException("Failed to read on TCP socket");
        }
    }
}
