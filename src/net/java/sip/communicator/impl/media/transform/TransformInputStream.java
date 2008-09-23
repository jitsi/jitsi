/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform;

import java.io.*;
import java.net.*;

import javax.media.protocol.*;

/**
 * TransformInputStream implements PushSourceStream. It is used by RTPManager to
 * receive RTP/RTCP packet datas.
 * 
 * In this implementation, we use UDP sockets to receive RTP/RTCP. We listen on
 * the address / port specified by local session address. When one packet is
 * received, it is first reverse transformed through PacketTransformer defined
 * by user. And then returned as normal RTP/RTCP packets to RTPManager.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class TransformInputStream 
    implements PushSourceStream, 
               Runnable 
{
    /**
     * UDP socket used to receive data.
     */
    private DatagramSocket socket;

    /**
     * User defined PacketTransformer, which is used to reverse transform
     * packets.
     */
    private PacketTransformer transformer;

    /**
     * SourceTransferHandler object which is used to read packets.
     */
    private SourceTransferHandler transferHandler;

    /**
     * Whether this stream is closed. Used to control the termination of worker
     * thread.
     */
    private boolean closed;

    /**
     * Worker thread we use to call transfer handle to received the data
     */
    private Thread recvThread;

    /**
     * Packet receive buffer
     */
    private byte[] buffer = new byte[65535];

    /**
     * Caught an IO exception during read from socket
     */
    private boolean ioError = false;

    private RawPacket pkt = null;

    /**
     * Construct a TransformInputStream based on the receiving socket and
     * PacketTransformer
     * 
     * @param socket
     *            data receiving socket
     * @param transformer
     *            packet transformer used
     */
    public TransformInputStream(DatagramSocket socket,
            PacketTransformer transformer) 
    {
        this.socket = socket;

        this.transformer = transformer;

        this.closed = false;

        this.recvThread = new Thread(this);
        this.recvThread.start();
    }

    /**
     * Close this stream, stops the worker thread.
     */
    public synchronized void close() 
    {
        this.closed = true;
        this.socket.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.PushSourceStream#read(byte[], int, int)
     */
    public int read(byte[] inBuffer, int offset, int length) 
        throws IOException 
    {
        if (ioError) 
        {
            return -1;
        }
        if (length < pkt.getLength()) {
            throw new IOException("Input buffer not big enough for "
                    + String.valueOf(pkt.getLength()));
        }

        System.arraycopy(pkt.getBuffer(), pkt.getOffset(), inBuffer, offset,
                pkt.getLength());
        return pkt.getLength();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.PushSourceStream#setTransferHandler
     *      (javax.media.protocol.SourceTransferHandler)
     */
    public void setTransferHandler(SourceTransferHandler handler) 
    {
        if (this.closed)
            return;

        this.transferHandler = handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.PushSourceStream#getMinimumTransferSize()
     */
    public int getMinimumTransferSize() 
    {
        return 2 * 1024; // twice the MTU size, just to be safe.
    }

    // ----- Not applicable methods -----

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.SourceStream#endOfStream()
     */
    public boolean endOfStream() 
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.SourceStream#getContentDescriptor()
     */
    public ContentDescriptor getContentDescriptor() 
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.SourceStream#getContentLength()
     */
    public long getContentLength() 
    {
        return LENGTH_UNKNOWN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlType) 
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() 
    {
        return new Object[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() 
    {
        while (!this.closed) 
        {
            DatagramPacket p = new DatagramPacket(this.buffer, 0, 65535);

            try 
            {
                this.socket.receive(p);
            } 
            catch (IOException e) 
            {
                ioError = true;
                break;
            }
            pkt = new RawPacket(this.buffer, 0, p.getLength());
            pkt = this.transformer.reverseTransform(pkt);

            // If the reverse transformed result is not valid,
            // then we will not deliver this packet.
            if (pkt == null) 
            {
                continue;
            }
            if (this.transferHandler != null && !this.closed) 
            {
                this.transferHandler.transferData(TransformInputStream.this);
            }
        }
    }
}
