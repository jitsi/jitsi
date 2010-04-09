/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

import java.awt.*;
import java.net.*;
import java.text.*;
import java.beans.*;

import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A CallSession contains parameters associated with a particular Call such as
 * ports used for transmitting and sending media (audio video), a reference to
 * the call itself and others. Call session instances are created through the
 * <tt>openCallSession(Call)</tt> method of the MediaService.
 * <p>
 * One <tt>CallSession</tt> pertains to a single <tt>Call</tt> instance and a
 * single <tt>Call</tt> may only be associated one <tt>CallSession</tt>
 * instance.
 * <p>
 * A call session also allows signaling protocols to generate SDP offers and
 * construct sdp answers.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Emanuel Onica
 */
public interface CallSession
{

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee.
     *
     * @return a String containing an SDP offer describing parameters of the
     * <tt>Call</tt> associated with this session.
     * @throws MediaException code INTERNAL_ERROR if generating the offer fails
     * for some reason.
     */
    public String createSdpOffer()
        throws MediaException;

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. The intendedDestination
     * parameter, may contain the address that the offer is to be sent to. In
     * case it is null we'll try our best to determine a default local address.
     *
     * @param intendedDestination the address of the call peer that the
     * descriptions is to be sent to.
     * @return a new SDP description String advertising all params of
     * <tt>callSession</tt>.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     */
    public String createSdpOffer(InetAddress intendedDestination)
        throws MediaException;

    /**
     * Creates a SDP description including the current state of the local media
     * setup and in accord with a specific SDP description of a call peer
     * (who is to be offered the created SDP description, for example, as part
     * of a re-INVITE).
     *
     * @param peerSdpDescription the SDP description (of a call
     *            peer) to have the created SDP description in accord
     *            with
     * @return a SDP description including the current state of the local media
     *         setup and in accord with the specified SDP description of a call
     *         peer
     */
    public String createSdpOffer(String peerSdpDescription)
        throws MediaException;

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an in-dialog invitation to a remote callee to put her
     * on/off hold or to send an answer to an offer to be put on/off hold.
     *
     * @param peerSdpDescription the last SDP description of the remote
     *            callee
     * @param onHold <tt>true</tt> if the SDP description should offer the remote
     *            callee to be put on hold or answer an offer from the remote
     *            callee to be put on hold; <tt>false</tt> to work in the
     *            context of a put-off-hold offer
     * @return an SDP description <tt>String</tt> which offers the remote
     *         callee to be put her on/off hold or answers an offer from the
     *         remote callee to be put on/off hold
     * @throws MediaException
     */
    public String createSdpDescriptionForHold(String peerSdpDescription,
        boolean onHold) throws MediaException;

    /**
     * The media flag which signals that the other side of the call has put this
     * on hold.
     */
    public static final byte ON_HOLD_REMOTELY = 1 << 1;

    /**
     * The media flag which signals that audio streams being received are to be
     * handled (e.g. played).
     */
    public static final byte RECEIVE_AUDIO = 1 << 2;

    /**
     * The media flag which signals that video streams being received are to be
     * handled (e.g. played).
     */
    public static final byte RECEIVE_VIDEO = 1 << 3;

    /**
     * Determines whether a specific SDP description <tt>String</tt> offers
     * this party to be put on hold and which media types are offered to be
     * received.
     *
     * @param sdpOffer the SDP description <tt>String</tt> to be examined for
     *            an offer to this party to be put on hold and media types to be
     *            received
     * @return an <tt>int</tt> bit mask containing
     *         <code>ON_HOLD_REMOTELY</code> if the specified SDP description
     *         offers this party to be put on hold, <code>RECEIVE_AUDIO</code>
     *         and/or <code>RECEIVE_VIDEO</code> if audio and/or video,
     *         respectively, are to be received
     * @throws MediaException
     */
    public int getSdpOfferMediaFlags(String sdpOffer) throws MediaException;

    /**
     * Modifies the current setup of the stream receiving in accord with a
     * specific set of media flags (which are usually obtained through
     * {@link #getSdpOfferMediaFlags(String)}. For example, if
     * <code>RECEIVE_VIDEO</code> isn't present and video is currently being
     * received and played, stops its receiving and playback.
     *
     * @param mediaFlags an <code>int</code> bit mask containing any of the
     *            media-related flags such as <code>RECEIVE_AUDIO</code> and
     *            <code>RECEIVE_VIDEO</code> and thus specifying which media
     *            types are to be received
     */
    public void setReceiveStreaming(int mediaFlags);

    /**
     * Puts the media of this <tt>CallSession</tt> on/off hold depending on
     * the origin of the request.
     * <p>
     * For example, a remote request to have this party put off hold cannot
     * override an earlier local request to put the remote party on hold.
     * </p>
     *
     * @param on <tt>true</tt> to request the media of this
     *            <tt>CallSession</tt> be put on hold; <tt>false</tt>,
     *            otherwise
     * @param here <tt>true</tt> if the request comes from this side of the
     *            call; <tt>false</tt> if the remote party is the issuer of
     *            the request i.e. it's the result of a remote offer
     */
    public void putOnHold(boolean on, boolean here);

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. Apart
     * from simply generating an SDP response description, the method records
     * details
     *
     * @param sdpOffer the SDP offer that we'd like to create an answer for.
     * @param offerer the peer that has sent the offer.
     *
     * @return a String containing an SDP answer describing parameters of the
     * <tt>Call</tt> associated with this session and matching those advertised
     * by the caller in their <tt>sdpOffer</tt>.
     *
     * @throws MediaException code INTERNAL_ERROR if processing the offer and/or
     * generating the answer fail for some reason.
     * @throws ParseException if <tt>sdpOfferStr</tt> does not contain a valid
     * sdp string.
     */
    public String processSdpOffer(CallPeer offerer, String sdpOffer)
        throws MediaException, ParseException;

    /**
     * The method is meant for use by protocol service implementations upon
     * reception of an SDP answer in response to an offer sent by us earlier.
     *
     * @param sdpAnswer the SDP answer that we'd like to handle.
     * @param responder the peer that has sent the answer.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     * @throws ParseException if <tt>sdpAnswerStr</tt> does not contain a valid
     * sdp string.
     */
    public void processSdpAnswer(CallPeer responder, String sdpAnswer)
        throws MediaException, ParseException;

    /**
     * Returns the port that we are using for receiving video data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving video data in this
     * <tt>CallSession</tt>.
     */
    public int getVideoPort();

    /**
     * Returns the port that we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     */
    public int getAudioPort();

    /**
     * Returns a URL pointing to a location with call control information for
     * this call or <tt>null</tt> if no such URL is available.
     *
     * @return a URL link to a location with call information or a call control
     * web interface for the specified peer or <tt>null</tt> if no such
     * URL is available.
     */
    public URL getCallInfoURL();

    /**
     * Determines whether the audio of this session is (set to) mute.
     *
     * @return <tt>true</tt> if the audio of this session is (set to) mute;
     *         otherwise, <tt>false</tt>
     */
    public boolean isMute();

    /**
     * Sets the mute state of the audio of this session.
     *
     * @param mute <tt>true</tt> to mute the audio of this session; otherwise,
     *            <tt>false</tt>
     */
    public void setMute(boolean mute);

    /**
     * Starts the streaming of the local media (to the remote destinations).
     */
    public void startStreaming()
        throws MediaException;

    /**
     * Calls {@link #startStreaming()} in order to start the streaming of the
     * local media and then begins processing the received and sent media
     * streams.
     */
    public void startStreamingAndProcessingMedia()
        throws MediaException;

    /**
     * Stops and closes the audio and video streams flowing through this
     * session.
     *
     * @return <tt>true</tt> if there was an actual change in the streaming i.e.
     *         the streaming wasn't already stopped before this request;
     *         <tt>false</tt>, otherwise
     */
    public boolean stopStreaming();

    /**
     * Gets the default secure/unsecure communication status for the supported
     * call sessions.
     *
     * @return default secure communication status for the supported call sessions
     */
    public boolean getSecureCommunicationStatus();

    /**
     * Sets the SAS verification
     *
     * @return True if SAS functions were called in ZrtpEngine
     */
    public boolean setZrtpSASVerification(boolean verified);

    /**
     * Gets the call associated with this session
     *
     * @return the call associated with this session
     */
    public Call getCall();

    /**
     * Adds a specific <code>VideoListener</code> to this
     * <code>CallSession</code> in order to receive notifications when
     * visual/video <code>Component</code>s are being added and removed.
     *
     * @param listener the <code>VideoListener</code> to be notified when
     *            visual/video <code>Component</code>s are being added or
     *            removed in this <code>CallSession</code>
     */
    public void addVideoListener(VideoListener listener);

    /**
     * Creates a visual <code>Component</code> which represents the local video
     * streamed by this <code>CallSession</code> (to remote destinations). If
     * the synchronous creation of the <code>Component</code> isn't supported,
     * it will be carried out asynchronously and the progress of the operation
     * and its result will be delivered through a specific
     * <code>VideoListener</code>.
     *
     * @param listener the <code>VideoListener</code> to track the progress of
     *            the creation and deliver its result in case the operation is
     *            carried out asynchronously by this implementation
     * @return a visual <code>Component</code> which represents the local video
     *         if this implementation creates it synchronously; <tt>null</tt> if
     *         this implementation attempts asynchronous creation in which case
     *         the result will be delivered to the specified
     *         <code>VideoListener</code>
     */
    public Component createLocalVisualComponent(VideoListener listener)
        throws MediaException;

    /**
     * Disposes of a specific visual <code>Component</code> representing local
     * video which has been created by this instance with
     * {@link #createLocalVisualComponent(VideoListener)}.
     *
     * @param component the visual <code>Component</code> representing local
     *            video to be disposed
     */
    public void disposeLocalVisualComponent(Component component);

    /**
     * Gets the visual/video <code>Component</code>s available in this
     * <code>CallSession</code>.
     *
     * @return an array of the visual <code>Component</code>s available in this
     *         <code>CallSession</code>
     */
    public Component[] getVisualComponents();

    /**
     * Removes a specific <code>VideoListener</code> from this
     * <code>CallSession</code> in order to no longer have it receive
     * notifications when visual/video <code>Component</code>s are being added
     * and removed.
     *
     * @param listener the <code>VideoListener</code> to no longer be notified
     *            when visual/video <code>Component</code>s are being added or
     *            removed in this <code>CallSession</code>
     */
    public void removeVideoListener(VideoListener listener);

    /**
     * Sets the indicator which determines whether the streaming of local video
     * in this <code>CallSession</code> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     *            this <code>CallSession</code>; <tt>false</tt> to disallow it
     */
    public void setLocalVideoAllowed(boolean allowed)
        throws MediaException;

    /**
     * Gets the indicator which determines whether the streaming of local video
     * in this <code>CallSession</code> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @return <tt>true</tt> if the streaming of local video in this
     *          <code>CallSession</code> is allowed; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoAllowed();

    /**
     * Sets a <tt>SessionCreatorCallback</tt> that will listen for
     * security events.
     *
     * @param sessionCreatorCallBack the <tt>SessionCreatorCallback</tt> to
     * set
     */
    public void setSessionCreatorCallback(
        SessionCreatorCallback sessionCreatorCallBack);

    /**
     * Returns the <tt>SessionCreatorCallback</tt> which listens for
     * security events.
     *
     * @return the <tt>SessionCreatorCallback</tt> which listens for
     * security events
     */
    public SessionCreatorCallback getSessionCreatorCallback();

    /**
     * The property which indicates whether a <code>CallSession</code> is
     * currently streaming the local video (to a remote destination).
     */
    public static final String LOCAL_VIDEO_STREAMING = "LOCAL_VIDEO_STREAMING";

    /**
     * Gets the indicator which determines whether this <code>CallSession</code>
     * is currently streaming the local video (to a remote destination).
     *
     * @return <tt>true</tt> if this <code>CallSession</code> is currently
     *         streaming the local video (to a remote destination); otherwise,
     *         <tt>false</tt>
     */
    public boolean isLocalVideoStreaming();

    /**
     * Adds a specific <code>PropertyChangeListener</code> to the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with this
     * <code>CallSession</code> change their values.
     *
     * @param listener the <code>PropertyChangeListener</code> to be notified
     *            when the properties associated with this
     *            <code>CallSession</code> change their values
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a specific <code>PropertyChangeListener</code> from the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with this
     * <code>CallSession</code> change their values.
     *
     * @param listener the <code>PropertyChangeListener</code> to no longer be
     *            notified when the properties associated with this
     *            <code>CallSession</code> change their values
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
