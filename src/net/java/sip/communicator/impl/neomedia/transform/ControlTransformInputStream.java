/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Implement control channel (RTCP) for <tt>TransformInputStream</tt>
 * which notify listeners when RTCP feedback messages are received.
 *
 * @author Bing SU (nova.su@gmail.com)
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class ControlTransformInputStream
    extends TransformUDPInputStream
{
    /**
     * The list of <tt>RTCPFeedbackListener</tt>.
     */
    private final List<RTCPFeedbackListener> listeners
        = new LinkedList<RTCPFeedbackListener>();

    /**
     * Initializes a new <tt>ControlTransformInputStream</tt> which is to
     * receive packet data from a specific UDP socket.
     *
     * @param socket the UDP socket the new instance is to receive data from
     */
    public ControlTransformInputStream(DatagramSocket socket)
    {
        super(socket);
    }

    /**
     * Adds an <tt>RTCPFeedbackListener</tt>.
     *
     * @param listener the <tt>RTCPFeedbackListener</tt> to add
     */
    public void addRTCPFeedbackListener(RTCPFeedbackListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        if(!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Removes an <tt>RTCPFeedbackListener</tt>.
     *
     * @param listener the <tt>RTCPFeedbackListener</tt> to remove
     */
    public void removeRTCPFeedbackListener(RTCPFeedbackListener listener)
    {
        listeners.remove(listener);
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
            throw new IOException(
                    "Input buffer not big enough for " + pktLength);

        /* check if RTCP feedback message */

        /* Feedback message size is minimum 12 bytes:
         * Version/Padding/Feedback message type: 1 byte
         * Payload type: 1 byte
         * Length: 2 bytes
         * SSRC of packet sender: 4 bytes
         * SSRC of media source: 4 bytes
         */
        if ((pktLength >= 12) && !listeners.isEmpty())
        {
            byte data[] = pkt.getBuffer();
            int fmt = 0;
            int pt = 0;

            /* get FMT field (last 5 bits of first byte) */
            fmt = (data[0] & 0x1F);
            pt |= (data[1] & 0xFF);

            RTCPFeedbackEvent evt = new RTCPFeedbackEvent(this, fmt, pt);

            /* notify feedback listeners */
            for(RTCPFeedbackListener l : listeners)
                l.feedbackReceived(evt);
        }

        System.arraycopy(
                pkt.getBuffer(), pkt.getOffset(),
                inBuffer, offset,
                pktLength);

        return pktLength;
    }
}
