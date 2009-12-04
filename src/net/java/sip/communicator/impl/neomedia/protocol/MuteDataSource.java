/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

/**
 * All datasources that support muting functionality implement
 * <tt>MuteDataSource</tt>.
 *
 * @author Damian Minkov
 */
public interface MuteDataSource
{
    /**
     * Determines whether this <tt>DataSource</tt> is mute.
     *
     * @return <tt>true</tt> if this <tt>DataSource</tt> is mute; otherwise,
     *         <tt>false</tt>
     */
    public boolean isMute();

    /**
     * Sets the mute state of this <tt>DataSource</tt>.
     *
     * @param mute <tt>true</tt> to mute this <tt>DataSource</tt>; otherwise,
     *            <tt>false</tt>
     */
    public void setMute(boolean mute);
}
