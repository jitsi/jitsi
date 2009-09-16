/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;

/**
 * The <tt>MediaStreamTarget</tt> contains a pair of host:port couples
 * indicating a data (RTP) and control (RTCP) locations.
 *
 * @author Emil Ivov
 */
public class MediaStreamTarget
{
    /**
     * The data (RTP) address of the target.
     */
    private InetSocketAddress rtpTarget = null;

    /**
     * The control (RTCP) address of the target.
     */
    private InetSocketAddress rtcpTarget = null;

    /**
     * Creates an instance of this <tt>MediaStreamTarget</tt> containing the
     * specified RTP and RTCP target host:port couples.
     *
     * @param rtpTarget the <tt>InetSocketAddress</tt> that this
     * <tt>MediaStreamTarget</tt> is supposed to indicate as a data address.
     * @param rtcpTarget the <tt>InetSocketAddress</tt> that this
     * <tt>MediaStreamTarget</tt> is supposed to indicate as a control address.
     */
    public MediaStreamTarget(InetSocketAddress rtpTarget,
                             InetSocketAddress rtcpTarget)
    {
        this.rtpTarget = rtpTarget;
        this.rtcpTarget = rtcpTarget;
    }

    /**
     * Returns the <tt>InetSocketAddress</tt> that this <tt>MediaTarget</tt> is
     * pointing to for all media (RTP) traffic.
     *
     * @return the <tt>InetSocketAddress</tt> that this <tt>MediaTarget</tt> is
     * pointing to for all media (RTP) traffic.
     */
    public InetSocketAddress getDataAddress()
    {
        return rtpTarget;
    }

    /**
     * Returns the <tt>InetSocketAddress</tt> that this <tt>MediaTarget</tt> is
     * pointing to for all media (RTP) traffic.
     *
     * @return the <tt>InetSocketAddress</tt> that this <tt>MediaTarget</tt> is
     * pointing to for all media (RTP) traffic.
     */
    public InetSocketAddress getControlAddress()
    {
        return rtcpTarget;
    }
}
