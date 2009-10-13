/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;

import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MediaStream</tt> class represents a (generally) bidirectional RTP
 * stream between exactly two parties. The class reflects one media stream, in
 * the SDP sense of the word. <tt>MediaStream</tt> instances are created through
 * the <tt>openMediaStream()</tt> method of the <tt>MediaService</tt>.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public interface MediaStream
{

    /**
     * The name of the property which indicates whether the remote SSRC is
     * currently available.
     */
    public static final String PNAME_LOCAL_SSRC_AVAILABLE
        = "localSSRCAvailable";

    /**
     * The name of the property which indicates whether the local SSRC is
     * currently available.
     */
    public static final String PNAME_REMOTE_SSRC_AVAILABLE
        = "remoteSSRCAvailable";

    /**
     * Starts capturing media from this stream's <tt>MediaDevice</tt> and then
     * streaming it through the local <tt>StreamConnector</tt> toward the
     * stream's target address and port. The method also puts the
     * <tt>MediaStream</tt> in a listening state that would make it play all
     * media received from the <tt>StreamConnector</tt> on the stream's
     * <tt>MediaDevice</tt>.
     */
    public void start();

    /**
     * Stops all streaming and capturing in this <tt>MediaStream</tt> and closes
     * and releases all open/allocated devices/resources. This method has no
     * effect on an already closed stream and is simply ignored.
     */
    public void stop();

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected.
     */
    public void close();

    /**
     * Sets the MediaFormat that this <tt>MediaStream</tt> should transmit in.
     *
     * @param format the <tt>MediaFormat</tt> that this <tt>MediaStream</tt>
     * should transmit in.
     */
    public void setFormat(MediaFormat format);

    /**
     * Returns the <tt>MediaFormat</tt> that this stream is currently
     * transmitting in.
     *
     * @return the <tt>MediaFormat</tt> that this stream is currently
     * transmitting in.
     */
    public MediaFormat getFormat();

    /**
     * Sets the device that this stream should use to play back and capture
     * media.
     *
     * @param device the <tt>MediaDevice</tt> that this stream should use
     * to play back and capture media.
     */
    public void setDevice(MediaDevice device);

    /**
     * Gets the device that this stream uses to play back and capture media.
     *
     * @return the <tt>MediaDevice</tt> that this stream uses to play back and
     * capture media.
     */
    public MediaDevice getDevice();

    /**
     * Returns the synchronization source (SSRC) identifier of the remote
     * participant or null if that identifier is not yet known at this point.
     *
     * @return  the synchronization source (SSRC) identifier of the remote
     * participant or null if that identifier is not yet known at this point.
     */
    public String getRemoteSourceID();

    /**
     * Returns the synchronization source (SSRC) identifier of the remote
     * participant or <tt>null</tt> if that identifier is not yet known at this
     * point.
     *
     * @return  the synchronization source (SSRC) identifier of the local
     * participant or <tt>null</tt> if that identifier is not yet known at this
     * point.
     */
    public String getLocalSourceID();

    /**
     * Returns the address that this stream is sending RTCP traffic to.
     *
     * @return an <tt>InetSocketAddress</tt> instance indicating the address
     * that we are sending RTCP packets to.
     */
    public InetSocketAddress getRemoteControlAddress();

    /**
     * Returns the address that this stream is sending RTP traffic to.
     *
     * @return an <tt>InetSocketAddress</tt> instance indicating the address
     * that we are sending RTP packets to.
     */
    public InetSocketAddress getRemoteDataAddress();

    /**
     * Adds a property change listener to this stream so that it would be
     * notified upon property change events like for example an SSRC ID which
     * becomes known.
     *
     * @param listener the listener that we'd like to register for
     * <tt>PropertyChangeEvent</tt>s
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified property change <tt>listener</tt> from this stream
     * so that it won't receive further property change events.
     *
     * @param listener the listener that we'd like to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Sets the target of this <tt>MediaStream</tt> to which it is to send and
     * from which it is to receive data (e.g. RTP) and control data (e.g. RTCP).
     *
     * @param target the <tt>MediaStreamTarget</tt> describing the data
     * (e.g. RTP) and the control data (e.g. RTCP) locations to which this
     * <tt>MediaStream</tt> is to send and from which it is to receive
     */
    public void setTarget(MediaStreamTarget target);
}
