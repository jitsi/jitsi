/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.rtp.*;

/**
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public class RTPConnectorOutputStream
    implements OutputDataStream
{
    /**
     * UDP socket used to send packet data
     */
    private final DatagramSocket socket;

    /**
     * Stream targets' ip addresses and ports.
     */
    protected final List<InetSocketAddress> targets
        = new LinkedList<InetSocketAddress>();

    /**
     * Initializes a new <tt>RTPConnectorOutputStream</tt> which is to send
     * packet data out through a specific UDP socket.
     *
     * @param socket the UDP socket used to send packet data out
     */
    public RTPConnectorOutputStream(DatagramSocket socket)
    {
        this.socket = socket;
    }

    /**
     * Add a target to stream targets list
     *
     * @param remoteAddr target ip address
     * @param remotePort target port
     */
    public void addTarget(InetAddress remoteAddr, int remotePort)
    {
        targets.add(new InetSocketAddress(remoteAddr, remotePort));
    }

    /**
     * Creates a new <tt>RawPacket</tt> from a specific <tt>byte[]</tt> buffer
     * in order to have this instance send its packet data through its
     * {@link #write(byte[], int, int)} method. Allows extenders to intercept
     * the packet data and possibly filter and/or modify it.
     *
     * @param buffer the packet data to be sent to the targets of this instance
     * @param offset the offset of the packet data in <tt>buffer</tt>
     * @param length the length of the packet data in <tt>buffer</tt>
     * @return a new <tt>RawPacket</tt> containing the packet data of the
     * specified <tt>byte[]</tt> buffer or possibly its modification;
     * <tt>null</tt> to ignore the packet data of the specified <tt>byte[]</tt>
     * buffer and not send it to the targets of this instance through its
     * {@link #write(byte[], int, int)} method
     */
    protected RawPacket createRawPacket(byte[] buffer, int offset, int length)
    {
        return new RawPacket(buffer, offset, length);
    }

    /**
     * Remove a target from stream targets list
     *
     * @param remoteAddr target ip address
     * @param remotePort target port
     * @return true if the target is in stream target list and can be removed
     *         false if not
     */
    public boolean removeTarget(InetAddress remoteAddr, int remotePort)
    {
        for (Iterator<InetSocketAddress> targetIter = targets.iterator();
                targetIter.hasNext();)
        {
            InetSocketAddress target = targetIter.next();

            if (target.getAddress().equals(remoteAddr)
                    && (target.getPort() == remotePort))
            {
                targetIter.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all stream targets from this session.
     */
    public void removeTargets()
    {
        targets.clear();
    }

    /*
     * Implements OutputDataStream#write(byte[], int, int).
     */
    public int write(byte[] buffer, int offset, int length)
    {
        RawPacket pkt = createRawPacket(buffer, offset, length);

        /*
         * If we got extended, the delivery of the packet may have been
         * canceled.
         */
        if (pkt == null)
            return length;

        for (InetSocketAddress target : targets)
            try
            {
                socket
                    .send(
                        new DatagramPacket(
                                pkt.getBuffer(),
                                pkt.getOffset(),
                                pkt.getLength(),
                                target.getAddress(),
                                target.getPort()));
            }
            catch (IOException ex)
            {
                // TODO error handling
                return -1;
            }
        return length;
    }
}
