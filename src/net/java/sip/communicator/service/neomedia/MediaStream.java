/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.net.*;
import java.util.*;

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
     * Sets the <tt>MediaFormat</tt> that this <tt>MediaStream</tt> should
     * transmit in.
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
     * @param device the <tt>MediaDevice</tt> that this stream should use to
     * play back and capture media.
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
     * participant or <tt>null</tt> if that identifier is not yet known at this
     * point.
     *
     * @return  the synchronization source (SSRC) identifier of the remote
     * participant or <tt>null</tt> if that identifier is not yet known at this
     * point.
     */
    public String getRemoteSourceID();

    /**
     * Returns the synchronization source (SSRC) identifier of the local
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

    /**
     * Adds a new association in this <tt>MediaStream</tt> of the specified RTP
     * payload type with the specified <tt>MediaFormat</tt> in order to allow it
     * to report <tt>rtpPayloadType</tt> in RTP flows sending and receiving
     * media in <tt>format</tt>. Usually, <tt>rtpPayloadType</tt> will be in the
     * range of dynamic RTP payload types.
     *
     * @param rtpPayloadType the RTP payload type to be associated in this
     * <tt>MediaStream</tt> with the specified <tt>MediaFormat</tt>
     * @param format the <tt>MediaFormat</tt> to be associated in this
     * <tt>MediaStream</tt> with <tt>rtpPayloadType</tt>
     */
    public void addDynamicRTPPayloadType(
            byte rtpPayloadType,
            MediaFormat format);

    /**
     * Gets the existing associations in this <tt>MediaStream</tt> of RTP
     * payload types to <tt>MediaFormat</tt>s. The returned <tt>Map</tt>
     * only contains associations previously added in this instance with
     * {@link #addDynamicRTPPayloadType(int, MediaFormat)} and not globally or
     * well-known associations reported by
     * {@link MediaFormat#getRTPPayloadType()}.
     *
     * @return a <tt>Map</tt> of RTP payload type expressed as <tt>Byte</tt> to
     * <tt>MediaFormat</tt> describing the existing (dynamic) associations in
     * this instance of RTP payload types to <tt>MediaFormat</tt>s. The
     * <tt>Map</tt> represents a snapshot of the existing associations at the
     * time of the <tt>getDynamicRTPPayloadTypes()</tt> method call and
     * modifications to it are not reflected on the internal storage
     */
    public Map<Byte, MediaFormat> getDynamicRTPPayloadTypes();

    /**
     * Sets the direction in which media in this <tt>MediaStream</tt> is to be
     * streamed. If this <tt>MediaStream</tt> is not currently started, calls to
     * {@link #start()} later on will start it only in the specified
     * <tt>direction</tt>. If it is currently started in a direction different
     * than the specified, directions other than the specified will be stopped.
     *
     * @param direction the <tt>MediaDirection</tt> in which this
     * <tt>MediaStream</tt> is to stream media when it is started
     */
    public void setDirection(MediaDirection direction);

    /**
     * Gets the direction in which this <tt>MediaStream</tt> is allowed to
     * stream media.
     *
     * @return the <tt>MediaDirection</tt> in which this <tt>MediaStream</tt> is
     * allowed to stream media
     */
    public MediaDirection getDirection();

    /**
     * Determines whether {@link #start()} has been called on this
     * <tt>MediaStream</tt> without {@link #stop()} or {@link #close()}
     * afterwards.
     *
     * @return <tt>true</tt> if {@link #start()} has been called on this
     * <tt>MediaStream</tt> without {@link #stop()} or {@link #close()}
     * afterwards
     */
    public boolean isStarted();

    /**
     * Causes this <tt>MediaStream</tt> to stop transmitting the media being fed
     * from this stream's <tt>MediaDevice</tt> and transmit "silence" instead.
     * "Silence" for video is understood as video data which is not the captured
     * video data and may represent, for example, a black image.
     *
     * @param mute <tt>true</tt> if we are to start transmitting "silence" and
     * <tt>false</tt> if we are to use media from this stream's
     * <tt>MediaDevice</tt> again.
     */
    public void setMute(boolean mute);

    /**
     * Determines whether this <tt>MediaStream</tt> is set to transmit "silence"
     * instead of the media being fed from its <tt>MediaDevice</tt>. "Silence"
     * for video is understood as video data which is not the captured video
     * data and may represent, for example, a black image.
     *
     * @return <tt>true</tt> if this <tt>MediaStream</tt> is set to transmit
     * "silence" instead of the media fed from its <tt>MediaDevice</tt>;
     * <tt>false</tt>, otherwise
     */
    public boolean isMute();
}
