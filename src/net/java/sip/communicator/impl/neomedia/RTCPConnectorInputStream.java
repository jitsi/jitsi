/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * @author Bing SU (nova.su@gmail.com)
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class RTCPConnectorInputStream
    extends RTPConnectorUDPInputStream
{
    /**
     * List of feedback listeners;
     */
    private final List<RTCPFeedbackListener> rtcpFeedbackListeners
        = new ArrayList<RTCPFeedbackListener>();

    /**
     * Initializes a new <tt>RTCPConnectorInputStream</tt> which is to receive
     * packet data from a specific UDP socket.
     *
     * @param socket the UDP socket the new instance is to receive data from
     */
    public RTCPConnectorInputStream(DatagramSocket socket)
    {
        super(socket);
    }

    /**
     * Add an <tt>RTCPFeedbackListener</tt>.
     *
     * @param listener object that will listen to incoming RTCP feedback
     * messages.
     */
    public void addRTCPFeedbackListener(RTCPFeedbackListener listener)
    {
        if(!rtcpFeedbackListeners.contains(listener))
            rtcpFeedbackListeners.add(listener);
    }

    /**
     * Remove an <tt>RTCPFeedbackListener</tt>.
     *
     * @param listener object to remove from listening RTCP feedback messages.
     */
    public void removeRTCPFeedbackListener(RTCPFeedbackListener listener)
    {
        rtcpFeedbackListeners.remove(listener);
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

        /* check if RTCP feedback message */

        /* Feedback message size is minimum 12 bytes:
         * Version/Padding/Feedback message type: 1 byte
         * Payload type: 1 byte
         * Length: 2 bytes
         * SSRC of packet sender: 4 bytes
         * SSRC of media source: 4 bytes
         */
        if(pktLength >= 12)
        {
            byte data[] = pkt.getBuffer();
            int fmt = 0;
            int pt = 0;

            /* get FMT field (last 5 bits of first byte) */
            fmt = (data[0] & 0x1F);
            pt |= (data[1] & 0xFF);

            RTCPFeedbackEvent evt = new RTCPFeedbackEvent(this, fmt, pt);

            /* notify feedback listeners */
            for(RTCPFeedbackListener l : rtcpFeedbackListeners)
                l.feedbackReceived(evt);
        }

        System.arraycopy(
                pkt.getBuffer(), pkt.getOffset(), inBuffer, offset, pktLength);

        return pktLength;
    }
}
