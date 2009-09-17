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
 * <p>
 *
 * @author Emil Ivov
 */
public interface MediaStream
{
    /**
     * The name of the property containing the number of binds that a Media
     * Service Implementation should execute in case a port is already
     * bound to (each retry would be on a new port in the allowed boundaries).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.BIND_RETRIES";

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MIN_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MIN_PORT_NUMBER";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MAX_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MAX_PORT_NUMBER";

    /**
     * The default number of binds that a Media Service Implementation should
     * execute in case a port is already bound to (each retry would be on a
     * new random port).
     */
    public static final int BIND_RETRIES_DEFAULT_VALUE = 50;

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
     * Sets the MediaFormat that this <tt>MediaStream</tt> should be
     * transmitting in.
     *
     * @param fmt the <tt>MediaFormat</tt> that this <tt>MediaStream</tt> should
     * be transmitting in.
     */
    public void setFormat(MediaFormat fmt);

    /**
     * Returns the <tt>MediaFormat</tt> that this stream is currently
     * transmitting in.
     *
     * @return the <tt>MediaFormat</tt> that this stream is currently
     * transmitting in.
     */
    public MediaFormat getFormat();

    /**
     * Sets the device that this stream should be using to playback and capture
     * media.
     *
     * @param device the <tt>MediaDevice</tt> that this stream should use
     * to playback and capture media.
     */
    public void setDevice(MediaDevice device);

    /**
     * Returns the device that this stream should be using to playback and
     * capture media.
     *
     * @return the <tt>MediaDevice</tt> that this stream should use
     * to playback and capture media.
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
}
