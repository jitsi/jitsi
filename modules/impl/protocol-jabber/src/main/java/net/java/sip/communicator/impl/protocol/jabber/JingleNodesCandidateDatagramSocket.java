/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.util.*;
import org.ice4j.*;
import org.ice4j.socket.*;
import org.ice4j.stack.*;

/**
 * Represents an application-purposed (as opposed to an ICE-specific)
 * <tt>DatagramSocket</tt> for a <tt>JingleNodesCandidate</tt>.
 *
 * @author Sebastien Vincent
 */
public class JingleNodesCandidateDatagramSocket extends DatagramSocket
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>JingleNodesCandidateDatagramSocket</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger
            = Logger.getLogger(JingleNodesCandidateDatagramSocket.class);

    /**
     * Determines whether a packet should be logged, given the number of sent
     * or received packets.
     *
     * @param numOfPacket the number of packets sent or received.
     */
    private static boolean logPacket(long numOfPacket)
    {
        return (numOfPacket == 1)
                || (numOfPacket == 300)
                || (numOfPacket == 500)
                || (numOfPacket == 1000)
                || ((numOfPacket % 5000) == 0);
    }

    /**
     * Logs information about RTP losses if there is more then 5% of RTP packet
     * lost (at most every 5 seconds).
     *
     * @param totalNbLost The total number of lost packet since the beginning of
     * this stream.
     * @param totalNbReceived The total number of received packet since the
     * beginning of this stream.
     * @param lastLogTime The last time we have logged information about RTP
     * losses.
     *
     * @return the last log time updated if this function as log new information
     * about RTP losses. Otherwise, returns the same last log time value as
     * given in parameter.
     */
    private static long logRtpLosses(
            long totalNbLost,
            long totalNbReceived,
            long lastLogTime)
    {
        double percentLost = ((double) totalNbLost)
                / ((double) (totalNbLost + totalNbReceived));
        // Log the information if the loss rate is upper 5% and if the last
        // log is before 5 seconds.
        if(percentLost > 0.05)
        {
            long currentTime = System.currentTimeMillis();
            if(currentTime - lastLogTime >= 5000)
            {
                logger.info("RTP lost > 5%: " + percentLost);
                return currentTime;
            }
        }
        return lastLogTime;
    }

    /**
     * Return the number of loss between the 2 last RTP packets received.
     *
     * @param lastRtpSequenceNumber The previous RTP sequence number.
     * @param newSeq The current RTP sequence number.
     *
     * @return the number of loss between the 2 last RTP packets received.
     */
    private static long getNbLost(
            long lastRtpSequenceNumber,
            long newSeq)
    {
        long newNbLost = 0;
        if(lastRtpSequenceNumber <= newSeq)
        {
            newNbLost = newSeq - lastRtpSequenceNumber;
        }
        else
        {
            newNbLost = (0xffff - lastRtpSequenceNumber) + newSeq;
        }

        if(newNbLost > 1)
        {
            if(newNbLost < 0x00ff)
            {
                return newNbLost - 1;
            }
            // Else the paquet is desequenced, then count it as a
            // single loss.
            else
            {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Determines the sequence number of an RTP packet.
     *
     * @param p the last RTP packet received.
     * @return The last RTP sequence number.
     */
    private static long getRtpSequenceNumber(DatagramPacket p)
    {
        // The sequence number is contained in the third and fourth bytes of the
        // RTP header (stored in big endian).
        byte[] data = p.getData();
        int offset = p.getOffset();
        long seq_high = data[offset + 2] & 0xff;
        long seq_low = data[offset + 3] & 0xff;

        return seq_high << 8 | seq_low;
    }

    /**
     * <tt>TransportAddress</tt> of the Jingle Nodes relay where we will send
     * our packet.
     */
    private TransportAddress localEndPoint = null;

    /**
     * The <tt>JingleNodesCandidate</tt>.
     */
    private JingleNodesCandidate jingleNodesCandidate;

    /**
     * The number of RTP packets received for this socket.
     */
    private long nbReceivedRtpPackets = 0;

    /**
     * The number of RTP packets sent for this socket.
     */
    private long nbSentRtpPackets = 0;

    /**
     * The number of RTP packets lost (not received) for this socket.
     */
    private long nbLostRtpPackets = 0;

    /**
     * The last RTP sequence number received for this socket.
     */
    private long lastRtpSequenceNumber = -1;

    /**
     * The last time an information about packet lost has been logged.
     */
    private long lastLostPacketLogTime = 0;

    /**
     * Initializes a new <tt>JingleNodesdCandidateDatagramSocket</tt> instance
     * which is to be the <tt>socket</tt> of a specific
     * <tt>JingleNodesCandidate</tt>.
     *
     * @param jingleNodesCandidate the <tt>JingleNodesCandidate</tt> which is to
     * use the new instance as the value of its <tt>socket</tt> property
     * @param localEndPoint <tt>TransportAddress</tt> of the Jingle Nodes relay
     * where we will send our packet.
     * @throws SocketException if anything goes wrong while initializing the new
     * <tt>JingleNodesCandidateDatagramSocket</tt> instance
     */
    public JingleNodesCandidateDatagramSocket(
            JingleNodesCandidate jingleNodesCandidate,
            TransportAddress localEndPoint)
        throws SocketException
    {
        super(/* bindaddr */ (SocketAddress) null);
        this.jingleNodesCandidate = jingleNodesCandidate;
        this.localEndPoint = localEndPoint;
    }

    /**
     * Sends a datagram packet from this socket. The <tt>DatagramPacket</tt>
     * includes information indicating the data to be sent, its length, the IP
     * address of the remote host, and the port number on the remote host.
     *
     * @param p the <tt>DatagramPacket</tt> to be sent
     * @throws IOException if an I/O error occurs
     * @see DatagramSocket#send(DatagramPacket)
     */
    @Override
    public void send(DatagramPacket p)
        throws IOException
    {
        byte data[] = p.getData();
        int dataLen = p.getLength();
        int dataOffset = p.getOffset();

        /* send to Jingle Nodes relay address on local port */
        DatagramPacket packet = new DatagramPacket(
                data,
                dataOffset,
                dataLen,
                new InetSocketAddress(
                    localEndPoint.getAddress(),
                    localEndPoint.getPort()));

        //XXX reuse an existing DatagramPacket ?
        super.send(packet);

        if (logPacket(++nbSentRtpPackets))
        {
            StunStack.logPacketToPcap(
                    packet,
                    true,
                    super.getLocalAddress(),
                    super.getLocalPort());
        }
    }

    /**
     * Receives a <tt>DatagramPacket</tt> from this socket. The DatagramSocket
     * is overridden to log the received packet into the "pcap" (packet capture)
     * log.
     *
     * @param p <tt>DatagramPacket</tt>
     * @throws IOException if something goes wrong
     */
    @Override
    public void receive(DatagramPacket p)
        throws IOException
    {
        super.receive(p);

        if (logPacket(++nbReceivedRtpPackets))
        {
            StunStack.logPacketToPcap(
                    p,
                    false,
                    super.getLocalAddress(),
                    super.getLocalPort());
        }
        // Log RTP losses if > 5%.
        updateRtpLosses(p);
    }

    /**
     * Gets the local address to which the socket is bound.
     * <tt>JingleNodesCandidateDatagramSocket</tt> returns the <tt>address</tt>
     * of its <tt>localSocketAddress</tt>.
     * <p>
     * If there is a security manager, its <tt>checkConnect</tt> method is first
     * called with the host address and <tt>-1</tt> as its arguments to see if
     * the operation is allowed.
     * </p>
     *
     * @return the local address to which the socket is bound, or an
     * <tt>InetAddress</tt> representing any local address if either the socket
     * is not bound, or the security manager <tt>checkConnect</tt> method does
     * not allow the operation
     * @see #getLocalSocketAddress()
     * @see DatagramSocket#getLocalAddress()
     */
    @Override
    public InetAddress getLocalAddress()
    {
        return getLocalSocketAddress().getAddress();
    }

    /**
     * Returns the port number on the local host to which this socket is bound.
     * <tt>JingleNodesCandidateDatagramSocket</tt> returns the <tt>port</tt> of
     * its <tt>localSocketAddress</tt>.
     *
     * @return the port number on the local host to which this socket is bound
     * @see #getLocalSocketAddress()
     * @see DatagramSocket#getLocalPort()
     */
    @Override
    public int getLocalPort()
    {
        return getLocalSocketAddress().getPort();
    }

    /**
     * Returns the address of the endpoint this socket is bound to, or
     * <tt>null</tt> if it is not bound yet. Since
     * <tt>JingleNodesCandidateDatagramSocket</tt> represents an
     * application-purposed <tt>DatagramSocket</tt> relaying data to and from a
     * Jingle Nodes relay, the <tt>localSocketAddress</tt> is the
     * <tt>transportAddress</tt> of respective <tt>JingleNodesCandidate</tt>.
     *
     * @return a <tt>SocketAddress</tt> representing the local endpoint of this
     * socket, or <tt>null</tt> if it is not bound yet
     * @see DatagramSocket#getLocalSocketAddress()
     */
    @Override
    public InetSocketAddress getLocalSocketAddress()
    {
        return jingleNodesCandidate.getTransportAddress();
    }

    /**
     * Updates and Logs information about RTP losses if there is more then 5% of
     * RTP packet lost (at most every 5 seconds).
     *
     * @param p The last packet received.
     */
    private void updateRtpLosses(DatagramPacket p)
    {
        // If this is not a STUN/TURN packet, then this is a RTP packet.
        if(!StunDatagramPacketFilter.isStunPacket(p))
        {
            long newSeq = getRtpSequenceNumber(p);
            if(this.lastRtpSequenceNumber != -1)
            {
                nbLostRtpPackets +=
                        getNbLost(this.lastRtpSequenceNumber, newSeq);
            }
            this.lastRtpSequenceNumber = newSeq;

            this.lastLostPacketLogTime
                = logRtpLosses(
                    this.nbLostRtpPackets,
                    this.nbReceivedRtpPackets,
                    this.lastLostPacketLogTime);
        }
    }
}
