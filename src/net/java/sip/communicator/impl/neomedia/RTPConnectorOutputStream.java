/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.packetlogging.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import javax.media.rtp.*;

/**
 *
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public abstract class RTPConnectorOutputStream
    implements OutputDataStream
{

    /**
     * The maximum number of packets to be sent to be kept in the queue of
     * <tt>MaxPacketsPerMillisPolicy</tt>. When the maximum is reached, the next
     * attempt to write a new packet in the queue will block until at least one
     * packet from the queue is sent. Defined in order to prevent
     * <tt>OutOfMemoryError</tt>s which, technically, may arise if the capacity
     * of the queue is unlimited.
     */
    private static final int
        MAX_PACKETS_PER_MILLIS_POLICY_PACKET_QUEUE_CAPACITY
            = 256;

    /**
     * The functionality which allows this <tt>OutputDataStream</tt> to control
     * how many RTP packets it sends through its <tt>DatagramSocket</tt> per a
     * specific number of milliseconds.
     */
    private MaxPacketsPerMillisPolicy maxPacketsPerMillisPolicy;

    /**
     * Stream targets' ip addresses and ports.
     */
    protected final List<InetSocketAddress> targets
        = new LinkedList<InetSocketAddress>();

    /**
     * List of available raw packets.
     */
    private final LinkedBlockingQueue<RawPacket> availRawPackets
    = new LinkedBlockingQueue<RawPacket>();

    /**
     * Used for debugging. As we don't log every packet
     * we must count them and decide which to log.
     */
    private long numberOfPackets = 0;

    /**
     * Initializes a new <tt>RTPConnectorOutputStream</tt> which is to send
     * packet data out through a specific socket.
     */
    public RTPConnectorOutputStream()
    {
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
     * Close this output stream.
     */
    public void close()
    {
        if (maxPacketsPerMillisPolicy != null)
        {
            maxPacketsPerMillisPolicy.close();
        }
        maxPacketsPerMillisPolicy = null;
        removeTargets();
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
        RawPacket pkt = availRawPackets.poll();
        if (pkt == null || pkt.getBuffer().length < length)
        {
            byte[] buf = new byte[length];
            pkt = new RawPacket();
            pkt.setBuffer(buf);
        }
        System.arraycopy(buffer, offset, pkt.getBuffer(), 0, length);
        pkt.setLength(length);
        pkt.setOffset(0);
        return pkt;
    }

    /**
     * Remove a target from stream targets list
     *
     * @param remoteAddr target ip address
     * @param remotePort target port
     * @return <tt>true</tt> if the target is in stream target list and can be
     * removed; <tt>false</tt>, otherwise
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

    /**
     * We don't log every rtp traffic.
     * We log only first then 300,500 and 1000 packets and
     * then every 5000 packet.
     *
     * @param numOfPacket current packet number.
     * @return wether we should log the current packet.
     */
    static boolean logPacket(long numOfPacket)
    {
        if(numOfPacket == 1
            || numOfPacket == 300
            || numOfPacket == 500
            || numOfPacket == 1000
            || numOfPacket%5000 == 0)
            return true;
        else
            return false;
    }

    /**
     * Send the packet from this <tt>OutputStream</tt>.
     *
     * @param packet packet to sent
     * @param target the target
     * @throws IOException if something goes wrong during sending
     */
    protected abstract void sendToTarget(RawPacket packet,
        InetSocketAddress target) throws IOException;

    /**
     * Log the packet.
     *
     * @param packet packet to log
     */
    protected abstract void doLogPacket(RawPacket packet,
        InetSocketAddress target);

    /**
     * Returns whether or not this <tt>RTPConnectorOutputStream</tt> has a valid
     * socket.
     *
     * @returns true if this <tt>RTPConnectorOutputStream</tt> has a valid
     * socket, false otherwise
     */
    protected abstract boolean isSocketValid();

    /**
     * Sends a specific RTP packet through the <tt>DatagramSocket</tt> of this
     * <tt>OutputDataSource</tt>.
     *
     * @param packet the RTP packet to be sent through the
     * <tt>DatagramSocket</tt> of this <tt>OutputDataSource</tt>
     * @return <tt>true</tt> if the specified <tt>packet</tt> was successfully
     * sent; otherwise, <tt>false</tt>
     */
    private boolean send(RawPacket packet)
    {
        if(!isSocketValid())
        {
            return false;
        }

        numberOfPackets++;
        for (InetSocketAddress target : targets)
        {
            try
            {
                sendToTarget(packet, target);

                if(logPacket(numberOfPackets)
                    && NeomediaActivator.getPacketLogging().isLoggingEnabled(
                            PacketLoggingService.ProtocolName.RTP))
                {
                    doLogPacket(packet, target);
                }
            }
            catch (IOException ex)
            {
                availRawPackets.offer(packet);
                // TODO error handling
                return false;
            }
        }
        availRawPackets.offer(packet);
        return true;
    }

    /**
     * Sets the maximum number of RTP packets to be sent by this
     * <tt>OutputDataStream</tt> through its <tt>DatagramSocket</tt> per
     * a specific number of milliseconds.
     *
     * @param maxPackets the maximum number of RTP packets to be sent by this
     * <tt>OutputDataStream</tt> through its <tt>DatagramSocket</tt> per the
     * specified number of milliseconds; <tt>-1</tt> if no maximum is to be set
     * @param perMillis the number of milliseconds per which <tt>maxPackets</tt>
     * are to be sent by this <tt>OutputDataStream</tt> through its
     * <tt>DatagramSocket</tt>
     */
    public void setMaxPacketsPerMillis(int maxPackets, long perMillis)
    {
        if (maxPacketsPerMillisPolicy == null)
        {
            if (maxPackets > 0)
            {
                if (perMillis < 1)
                    throw new IllegalArgumentException("perMillis");

                maxPacketsPerMillisPolicy
                    = new MaxPacketsPerMillisPolicy(maxPackets, perMillis);
            }
        }
        else
        {
            maxPacketsPerMillisPolicy
                .setMaxPacketsPerMillis(maxPackets, perMillis);
        }
    }

    /**
     * Implements {@link OutputDataStream#write(byte[], int, int)}.
     *
     * @param buffer the <tt>byte[]</tt> that we'd like to copy the content
     * of the packet to.
     * @param offset the position where we are supposed to start writing in
     * <tt>buffer</tt>.
     * @param length the number of <tt>byte</tt>s available for writing in
     * <tt>inBuffer</tt>.
     *
     * @return the number of bytes read
     */
    public int write(byte[] buffer, int offset, int length)
    {
        RawPacket packet = createRawPacket(buffer, offset, length);

        /*
         * If we got extended, the delivery of the packet may have been
         * canceled.
         */
        if (packet != null)
        {
            if (maxPacketsPerMillisPolicy == null)
            {
                if (!send(packet))
                    return -1;
            }
            else
                maxPacketsPerMillisPolicy.write(packet);
        }
        return length;
    }

    /**
     * Changes current thread priority.
     * @param priority the new priority.
     */
    public void setPriority(int priority)
    {
        // currently no priority is set
//        if(maxPacketsPerMillisPolicy != null &&
//            maxPacketsPerMillisPolicy.sendThread != null)
//            maxPacketsPerMillisPolicy.sendThread.setPriority(priority);
    }

    /**
     * Implements the functionality which allows this <tt>OutputDataStream</tt>
     * to control how many RTP packets it sends through its
     * <tt>DatagramSocket</tt> per a specific number of milliseconds.
     */
    private class MaxPacketsPerMillisPolicy
    {

        /**
         * The maximum number of RTP packets to be sent by this
         * <tt>OutputDataStream</tt> through its <tt>DatagramSocket</tt> per
         * {@link #perNanos} nanoseconds.
         */
        private int maxPackets = -1;

        /**
         * The time stamp in nanoseconds of the start of the current
         * <tt>perNanos</tt> interval.
         */
        private long millisStartTime = 0;

        /**
         * The list of RTP packets to be sent through the
         * <tt>DatagramSocket</tt> of this <tt>OutputDataSource</tt>.
         */
        private final ArrayBlockingQueue<RawPacket> packetQueue
            = new ArrayBlockingQueue<RawPacket>(
                    MAX_PACKETS_PER_MILLIS_POLICY_PACKET_QUEUE_CAPACITY);

        /**
         * The number of RTP packets already sent during the current
         * <tt>perNanos</tt> interval.
         */
        private long packetsSentInMillis = 0;

        /**
         * The time interval in nanoseconds during which {@link #maxPackets}
         * number of RTP packets are to be sent through the
         * <tt>DatagramSocket</tt> of this <tt>OutputDataSource</tt>.
         */
        private long perNanos = -1;

        /**
         * The <tt>Thread</tt> which is to send the RTP packets in
         * {@link #packetQueue} through the <tt>DatagramSocket</tt> of this
         * <tt>OutputDataSource</tt>.
         */
        private Thread sendThread;

        /**
         * To signal run or stop condition to send thread.
         */
        private boolean sendRun = true;

        /**
         * Initializes a new <tt>MaxPacketsPerMillisPolicy</tt> instance which
         * is to control how many RTP packets this <tt>OutputDataSource</tt> is
         * to send through its <tt>DatagramSocket</tt> per a specific number of
         * milliseconds.
         *
         * @param maxPackets the maximum number of RTP packets to be sent per
         * <tt>perMillis</tt> milliseconds through the <tt>DatagramSocket</tt>
         * of this <tt>OutputDataStream</tt>
         * @param perMillis the number of milliseconds per which a maximum of
         * <tt>maxPackets</tt> RTP packets are to be sent through the
         * <tt>DatagramSocket</tt> of this <tt>OutputDataStream</tt>
         */
        public MaxPacketsPerMillisPolicy(int maxPackets, long perMillis)
        {
            setMaxPacketsPerMillis(maxPackets, perMillis);
            synchronized (this) {
                if (sendThread == null)
                {
                    sendThread
                        = new Thread(getClass().getName())
                        {
                            @Override
                            public void run()
                            {
                                runInSendThread();
                            }
                        };
                    sendThread.setDaemon(true);
                    sendThread.start();
                }
            }
        }

        /**
         * Closes the connector.
         */
        synchronized void close()
        {
            if (!sendRun)
                return;
            sendRun = false;
            // just offer a new packet to wakeup thread in case it waits for
            // a packet.
            packetQueue.offer(new RawPacket(null, 0, 0));
        }

        /**
         * Sends the RTP packets in {@link #packetQueue} in accord with
         * {@link #maxPackets} and {@link #perNanos}.
         */
        private void runInSendThread()
        {
            try
            {
                while (sendRun)
                {
                    RawPacket packet = null;

                    while (true)
                    {
                        try
                        {
                            packet = packetQueue.take();
                            break;
                        }
                        catch (InterruptedException iex)
                        {
                            continue;
                        }
                    }
                    if (!sendRun)
                        break;

                    long time = System.nanoTime();
                    long millisRemainingTime = time - millisStartTime;

                    if ((perNanos < 1)
                            || (millisRemainingTime >= perNanos))
                    {
                        millisStartTime = time;
                        packetsSentInMillis = 0;
                    }
                    else if ((maxPackets > 0)
                            && (packetsSentInMillis >= maxPackets))
                    {
                        while (true)
                        {
                            millisRemainingTime = System.nanoTime()
                                    - millisStartTime;
                            if (millisRemainingTime >= perNanos)
                                break;
                            LockSupport.parkNanos(millisRemainingTime);
                        }
                        millisStartTime = System.nanoTime();
                        packetsSentInMillis = 0;
                    }

                    send(packet);
                    packetsSentInMillis++;
                }
            }
            finally
            {
                packetQueue.clear();
                synchronized (packetQueue)
                {
                    if (Thread.currentThread().equals(sendThread))
                        sendThread = null;
                }
            }
        }

        /**
         * Sets the maximum number of RTP packets to be sent by this
         * <tt>OutputDataStream</tt> through its <tt>DatagramSocket</tt> per
         * a specific number of milliseconds.
         *
         * @param maxPackets the maximum number of RTP packets to be sent by
         * this <tt>OutputDataStream</tt> through its <tt>DatagramSocket</tt>
         * per the specified number of milliseconds; <tt>-1</tt> if no maximum
         * is to be set
         * @param perMillis the number of milliseconds per which
         * <tt>maxPackets</tt> are to be sent by this <tt>OutputDataStream</tt>
         * through its <tt>DatagramSocket</tt>
         */
        public void setMaxPacketsPerMillis(int maxPackets, long perMillis)
        {
            if (maxPackets < 1)
            {
                this.maxPackets = -1;
                this.perNanos = -1;
            }
            else
            {
                if (perMillis < 1)
                    throw new IllegalArgumentException("perMillis");

                this.maxPackets = maxPackets;
                this.perNanos = perMillis * 1000000;
            }
        }

        /**
         * Queues a specific RTP packet to be sent through the
         * <tt>DatagramSocket</tt> of this <tt>OutputDataStream</tt>.
         *
         * @param packet the RTP packet to be queued for sending through the
         * <tt>DatagramSocket</tt> of this <tt>OutputDataStream</tt>
         */
        public void write(RawPacket packet)
        {
            while (true)
            {
                try
                {
                    packetQueue.put(packet);
                    break;
                }
                catch (InterruptedException iex)
                {
                    continue;
                }
            }
        }
    }
}
