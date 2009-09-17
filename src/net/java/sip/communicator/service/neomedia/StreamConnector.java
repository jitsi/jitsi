/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;

/**
 * The <tt>StreamConnector</tt> interface represents a pair of datagram sockets
 * that a media stream could use for RTP and RTCP traffic.
 * <p>
 * The reason why this media service makes sockets visible through this
 * <tt>StreamConnector</tt> is so that they could be shared among media
 * and other libraries that may need to use them like an ICE implementation for
 * example.
 * 
 * @author Emil Ivov
 */
public interface StreamConnector
{
    /**
     * Returns a reference to the <tt>DatagramSocket</tt> that a stream should
     * use for data (e.g. RTP) traffic.
     *
     * @return a reference to the <tt>DatagramSocket</tt> that a stream should
     * use for data (e.g. RTP) traffic.
     */
    public DatagramSocket getDataSocket();

    /**
     * Returns a reference to the <tt>DatagramSocket</tt> that a stream should
     * use for control data (e.g. RTCP).
     *
     * @return a reference to the <tt>DatagramSocket</tt> that a stream should
     * use for control data (e.g. RTCP).
     */
    public DatagramSocket getControlSocket();
}
