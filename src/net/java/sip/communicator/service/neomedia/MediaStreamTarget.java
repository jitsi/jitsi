/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;

/**
 * The <tt>MediaStreamTarget</tt> contains a pair of host:port couples
 * indicating data (RTP) and control (RTCP) locations.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class MediaStreamTarget
{
    /**
     * The data (RTP) address of the target.
     */
    private final InetSocketAddress rtpTarget;

    /**
     * The control (RTCP) address of the target.
     */
    private final InetSocketAddress rtcpTarget;

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
     * Determines whether two specific <tt>InetSocketAddress</tt> instances are
     * equal.
     *
     * @param addr1 one of the <tt>InetSocketAddress</tt> instances to be
     * compared
     * @param addr2 the other <tt>InetSocketAddress</tt> instance to be compared
     * @return <tt>true</tt> if <tt>addr1</tt> is equal to <tt>addr2</tt>;
     * otherwise, <tt>false</tt>
     */
    private boolean addressesAreEqual(
            InetSocketAddress addr1,
            InetSocketAddress addr2)
    {
        return (addr1 == null) ? (addr2 == null) : addr1.equals(addr2);
    }

    /**
     * Determines whether this <tt>MediaStreamTarget</tt> is equal to a specific
     * <tt>Object</tt>.
     *
     * @param obj the <tt>Object</tt> to be compared to this
     * <tt>MediaStreamTarget</tt>
     * @return <tt>true</tt> if this <tt>MediaStreamTarget</tt> is equal to the
     * specified <tt>obj</tt>; otherwise, <tt>false</tt>
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (!getClass().isInstance(obj))
            return false;

        MediaStreamTarget mediaStreamTarget = (MediaStreamTarget) obj;

        return
            addressesAreEqual(
                    getControlAddress(),
                    mediaStreamTarget.getControlAddress())
                && addressesAreEqual(
                        getDataAddress(),
                        mediaStreamTarget.getDataAddress());
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

    /**
     * Returns a hash code for this <tt>MediaStreamTarget</tt> instance which is
     * suitable for use in hash tables.
     *
     * @return a hash code for this <tt>MediaStreamTarget</tt> instance which is
     * suitable for use in hash tables
     */
    @Override
    public int hashCode()
    {
        int hashCode = 0;

        InetSocketAddress controlAddress = getControlAddress();
        if (controlAddress != null)
            hashCode |= controlAddress.hashCode();

        InetSocketAddress dataAddress = getDataAddress();
        if (dataAddress != null)
            hashCode |= dataAddress.hashCode();

        return hashCode;
    }

    /**
     * Returns a human-readable representation of this
     * <tt>MediaStreamTarget</tt> instance in the form of a <tt>String</tt>
     * value.
     *
     * @return a <tt>String</tt> value which gives a human-readable
     * representation of this <tt>MediaStreamTarget</tt> instance
     */
    @Override
    public String toString()
    {
        return
            getClass().getSimpleName()
                + " with dataAddress "
                + getDataAddress()
                + " and controlAddress "
                + getControlAddress();
    }
}
