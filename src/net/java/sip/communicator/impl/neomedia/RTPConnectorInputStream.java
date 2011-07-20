/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.packetlogging.*;

import java.io.*;
import java.net.*;

import javax.media.protocol.*;

/**
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public abstract class RTPConnectorInputStream
    implements PushSourceStream,
               Runnable
{
    /**
     * The value of the property <tt>controls</tt> of
     * <tt>RTPConnectorInputStream</tt> when there are no controls. Explicitly
     * defined in order to reduce unnecessary allocations.
     */
    private static final Object[] EMPTY_CONTROLS = new Object[0];

    /**
     * The size of the buffers receiving packets coming from network.
     */
    private static final int PACKET_RECEIVE_BUFFER = 4000;

    /**
     * Packet receive buffer
     */
    private final byte[] buffer = new byte[PACKET_RECEIVE_BUFFER];

    /**
     * Whether this stream is closed. Used to control the termination of worker
     * thread.
     */
    protected boolean closed;

    /**
     * Caught an IO exception during read from socket
     */
    protected boolean ioError = false;

    /**
     * The packet data to be read out of this instance through its
     * {@link #read(byte[], int, int)} method.
     */
    protected RawPacket pkt;

    /**
     * SourceTransferHandler object which is used to read packets.
     */
    private SourceTransferHandler transferHandler;

    /**
     * The Thread receiving packets.
     */
    protected Thread receiverThread = null;

    /**
     * Used for debugging. As we don't log every packet
     * we must count them and decide which to log.
     */
    private long numberOfPackets = 0;

    /**
     * Initializes a new <tt>RTPConnectorInputStream</tt> which is to receive
     * packet data from a specific UDP socket.
     */
    public RTPConnectorInputStream()
    {
    }

    /**
     * Close this stream, stops the worker thread.
     */
    public synchronized void close()
    {
    }

    /**
     * Creates a new <tt>RawPacket</tt> from a specific <tt>DatagramPacket</tt>
     * in order to have this instance receive its packet data through its
     * {@link #read(byte[], int, int)} method. Allows extenders to intercept the
     * packet data and possibly filter and/or modify it.
     *
     * @param datagramPacket the <tt>DatagramPacket</tt> containing the packet
     * data
     * @return a new <tt>RawPacket</tt> containing the packet data of the
     * specified <tt>DatagramPacket</tt> or possibly its modification;
     * <tt>null</tt> to ignore the packet data of the specified
     * <tt>DatagramPacket</tt> and not make it available to this instance
     * through its {@link #read(byte[], int, int)} method
     */
    protected RawPacket createRawPacket(DatagramPacket datagramPacket)
    {
        if (pkt == null)
        {
            return new RawPacket(
                    datagramPacket.getData(),
                    datagramPacket.getOffset(),
                    datagramPacket.getLength());
        }
        pkt.setBuffer(datagramPacket.getData());
        pkt.setLength(datagramPacket.getLength());
        pkt.setOffset(datagramPacket.getOffset());
        return pkt;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnectorInputStream#endOfStream()} that always returns
     * <tt>false</tt>.
     *
     * @return <tt>false</tt>, no matter what.
     */
    public boolean endOfStream()
    {
        return false;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnectorInputStream#getContentDescriptor()} that always returns
     * <tt>null</tt>.
     *
     * @return <tt>null</tt>, no matter what.
     */
    public ContentDescriptor getContentDescriptor()
    {
        return null;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnectorInputStream#getContentLength()} that always returns
     * <tt>LENGTH_UNKNOWN</tt>.
     *
     * @return <tt>LENGTH_UNKNOWN</tt>, no matter what.
     */
    public long getContentLength()
    {
        return pkt.getLength();
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnectorInputStream#getControl(String)} that always returns
     * <tt>null</tt>.
     *
     * @param controlType ignored.
     *
     * @return <tt>null</tt>, no matter what.
     */
    public Object getControl(String controlType)
    {
        return null;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnectorInputStream#getControls()} that always returns
     * <tt>EMPTY_CONTROLS</tt>.
     *
     * @return <tt>EMPTY_CONTROLS</tt>, no matter what.
     */
    public Object[] getControls()
    {
        return EMPTY_CONTROLS;
    }

    /**
     * Provides a dummy implementation to {@link
     * RTPConnectorInputStream#getMinimumTransferSize()} that always returns
     * <tt>2 * 1024</tt>.
     *
     * @return <tt>2 * 1024</tt>, no matter what.
     */
    public int getMinimumTransferSize()
    {
        return 2 * 1024; // twice the MTU size, just to be safe.
    }

    /**
     * Copies the content of the most recently received packet into
     * <tt>inBuffer</tt>.
     *
     * @param inBuffer the <tt>byte[]</tt> that we'd like to copy the content
     * of the packet to.
     * @param offset the position where we are supposed to start writing in
     * <tt>inBuffer</tt>.
     * @param length the number of <tt>byte</tt>s available for writing in
     * <tt>inBuffer</tt>.
     *
     * @return the number of bytes read
     *
     * @throws IOException if <tt>length</tt> is less than the size of the
     * packet.
     */
    public int read(byte[] inBuffer, int offset, int length)
        throws IOException
    {
        if (ioError)
            return -1;

        int pktLength = pkt.getLength();

        if (length < pktLength)
            throw
                new IOException("Input buffer not big enough for " + pktLength);

        System.arraycopy(
                pkt.getBuffer(), pkt.getOffset(), inBuffer, offset, pktLength);

        return pktLength;
    }

    /**
     * Log the packet.
     *
     * @param packet packet to log
     */
    protected abstract void doLogPacket(DatagramPacket packet);

    /**
     * Receive packet.
     *
     * @param p packet for receiving
     * @throws IOException if something goes wrong during receiving
     */
    protected abstract void receivePacket(DatagramPacket p)
        throws IOException;

    /**
     * Listens for incoming datagrams, stores them for reading by the
     * <tt>read</tt> method and notifies the local <tt>transferHandler</tt>
     * that there's data to be read.
     */
    public void run()
    {
        while (!closed)
        {
            DatagramPacket p = new DatagramPacket(
                buffer, 0, PACKET_RECEIVE_BUFFER);

            try
            {
                receivePacket(p);
                numberOfPackets++;
            }
            catch (IOException e)
            {
                ioError = true;
                break;
            }

            if(RTPConnectorOutputStream.logPacket(numberOfPackets)
                && NeomediaActivator.getPacketLogging().isLoggingEnabled(
                    PacketLoggingService.ProtocolName.RTP))
            {
                doLogPacket(p);
            }

            pkt = createRawPacket(p);

            /*
             * If we got extended, the delivery of the packet may have been
             * canceled.
             */
            if ((pkt != null) && (transferHandler != null) && !closed)
                transferHandler.transferData(this);
        }
    }

    /**
     * Sets the <tt>transferHandler</tt> that this connector should be notifying
     * when new data is available for reading.
     *
     * @param transferHandler the <tt>transferHandler</tt> that this connector
     * should be notifying when new data is available for reading.
     */
    public void setTransferHandler(SourceTransferHandler transferHandler)
    {
        if (!closed)
            this.transferHandler = transferHandler;
    }

    /**
     * Changes current thread priority.
     * @param priority the new priority.
     */
    public void setPriority(int priority)
    {
        // currently no priority is set
//        if(receiverThread != null)
//        {
//            receiverThread.setPriority(priority);
//        }
    }
}
