/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.io.*;
import java.net.*;

import javax.media.protocol.*;

/**
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public class RTPConnectorInputStream 
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
     * Packet receive buffer
     */
    private final byte[] buffer = new byte[65535];

    /**
     * Whether this stream is closed. Used to control the termination of worker
     * thread.
     */
    private boolean closed;

    /**
     * Caught an IO exception during read from socket
     */
    private boolean ioError = false;

    /**
     * The packet data to be read out of this instance through its
     * {@link #read(byte[], int, int)} method.
     */
    private RawPacket pkt;

    /**
     * UDP socket used to receive data.
     */
    private final DatagramSocket socket;

    /**
     * SourceTransferHandler object which is used to read packets.
     */
    private SourceTransferHandler transferHandler;

    /**
     * Initializes a new <tt>RTPConnectorInputStream</tt> which is to receive
     * packet data from a specific UDP socket.
     *
     * @param socket the UDP socket the new instance is to receive data from
     */
    public RTPConnectorInputStream(DatagramSocket socket) 
    {
        this.socket = socket;

        closed = false;
        new Thread(this).start();
    }

    /**
     * Close this stream, stops the worker thread.
     */
    public synchronized void close() 
    {
        closed = true;
        socket.close();
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
        return
            new RawPacket(
                    datagramPacket.getData(),
                    datagramPacket.getOffset(),
                    datagramPacket.getLength());
    }

    /*
     * Implements SourceStream#endOfStream().
     */
    public boolean endOfStream() 
    {
        return false;
    }

    /*
     * Implements SourceStream#getContentDescriptor().
     */
    public ContentDescriptor getContentDescriptor() 
    {
        return null;
    }

    /*
     * Implements SourceStream#getContentLength().
     */
    public long getContentLength() 
    {
        return LENGTH_UNKNOWN;
    }

    /*
     * Implements Controls#getControl(String).
     */
    public Object getControl(String controlType) 
    {
        return null;
    }

    /*
     * Implements Controls#getControls().
     */
    public Object[] getControls() 
    {
        return EMPTY_CONTROLS;
    }

    /*
     * Implements PushSourceStream#getMinimumTransferSize().
     */
    public int getMinimumTransferSize() 
    {
        return 2 * 1024; // twice the MTU size, just to be safe.
    }

    /*
     * Implements PushSourceStream#read(byte[], int, int).
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

        System
            .arraycopy(
                pkt.getBuffer(), pkt.getOffset(), inBuffer, offset, pktLength);
        return pktLength;
    }

    /*
     * Implements Runnable#run().
     */
    public void run() 
    {
        while (!closed) 
        {
            DatagramPacket p = new DatagramPacket(buffer, 0, 65535);

            try 
            {
                socket.receive(p);
            } 
            catch (IOException e) 
            {
                ioError = true;
                break;
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

    /*
     * Implements PushSourceStream#setTransferHandler(SourceTransferHandler).
     */
    public void setTransferHandler(SourceTransferHandler transferHandler) 
    {
        if (!closed)
            this.transferHandler = transferHandler;
    }
}
