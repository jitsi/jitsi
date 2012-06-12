/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.IOException;
import java.net.*;

import org.ice4j.*;
import org.ice4j.stack.*;
import org.ice4j.socket.*;

/**
 * Represents an application-purposed (as opposed to an ICE-specific)
 * <tt>DatagramSocket</tt> for a <tt>JingleNodesCandidate</tt>.
 *
 * @author Sebastien Vincent
 */
public class JingleNodesCandidateDatagramSocket extends DatagramSocket
{
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

        // no exception packet is successfully sent, log it.
        ++nbSentRtpPackets;
        DelegatingDatagramSocket.logPacketToPcap(
                packet,
                this.nbSentRtpPackets,
                true,
                super.getLocalAddress(),
                super.getLocalPort());
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

        // no exception packet is successfully received, log it.
        ++nbReceivedRtpPackets;
        DelegatingDatagramSocket.logPacketToPcap(
                p,
                this.nbReceivedRtpPackets,
                false,
                super.getLocalAddress(),
                super.getLocalPort());
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
    public void updateRtpLosses(DatagramPacket p)
    {
        // If this is not a STUN/TURN packet, then this is a RTP packet.
        if(!StunDatagramPacketFilter.isStunPacket(p))
        {
            long newSeq = DelegatingDatagramSocket.getRtpSequenceNumber(p);
            if(this.lastRtpSequenceNumber != -1)
            {
                nbLostRtpPackets += DelegatingDatagramSocket
                    .getNbLost(this.lastRtpSequenceNumber, newSeq);
            }
            this.lastRtpSequenceNumber = newSeq;

            this.lastLostPacketLogTime = DelegatingDatagramSocket.logRtpLosses(
                    this.nbLostRtpPackets,
                    this.nbReceivedRtpPackets,
                    this.lastLostPacketLogTime);
        }
    }
}
