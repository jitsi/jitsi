/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.awt.*;
import java.beans.*;
import java.net.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.SizeChangeVideoEvent;
import net.java.sip.communicator.service.protocol.event.VideoEvent;
import net.java.sip.communicator.service.protocol.event.VideoListener;
import net.java.sip.communicator.util.*;

/**
 * A utility class implementing media control code shared between current
 * telephony implementations. This class is only meant for use by protocol
 * implementations and should/could not be accessed by bundles that are simply
 * using the telephony functionalities.
 *
 * @param <T> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 *
 * @author Emil Ivov
 *
 */
public abstract class CallPeerMediaHandler<
                                        T extends MediaAwareCallPeer<?, ?, ?>>
    extends PropertyChangeNotifier
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandler</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(CallPeerMediaHandler.class.getName());

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the local SSRC of its audio <tt>MediaStream</tt>.
     */
    public static final String AUDIO_LOCAL_SSRC = "AUDIO_LOCAL_SSRC";

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the remote SSRC of its audio <tt>MediaStream</tt>.
     */
    public static final String AUDIO_REMOTE_SSRC = "AUDIO_REMOTE_SSRC";

    /**
     * The constant which signals that a SSRC value is unknown.
     */
    public static final long SSRC_UNKNOWN = -1;

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the local SSRC of its video <tt>MediaStream</tt>.
     */
    public static final String VIDEO_LOCAL_SSRC = "VIDEO_LOCAL_SSRC";

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the remote SSRC of its video <tt>MediaStream</tt>.
     */
    public static final String VIDEO_REMOTE_SSRC = "VIDEO_REMOTE_SSRC";

    /**
     * Determines whether or not streaming local video is currently enabled.
     * Default is RECVONLY. We tried to have INACTIVE at one point but it was
     * breaking incoming reINVITEs for video calls..
     */
    private MediaDirection videoDirectionUserPreference
        = MediaDirection.RECVONLY;

    /**
     * Determines whether or not streaming local audio is currently enabled.
     */
    private MediaDirection audioDirectionUserPreference
        = MediaDirection.SENDRECV;

    /**
     * A reference to the CallPeer instance that this handler is managing media
     * streams for.
     */
    private final T peer;

    /**
     * A reference to the object that would be responsible for ZRTP control
     * and which most often would be the peer itself.
     */
    public final ZrtpListener zrtpController;

    /**
     * The RTP stream that this media handler uses to send audio.
     */
    private AudioMediaStream audioStream = null;

    /**
     * The last-known local SSRC of {@link #audioStream}.
     */
    private long audioLocalSSRC = SSRC_UNKNOWN;

    /**
     * The last-known remote SSRC of {@link #audioStream}.
     */
    private long audioRemoteSSRC = SSRC_UNKNOWN;

    /**
     * The RTP stream that this media handler uses to send video.
     */
    private VideoMediaStream videoStream = null;

    /**
     * The last-known local SSRC of {@link #videoStream}.
     */
    private long videoLocalSSRC = SSRC_UNKNOWN;

    /**
     * The last-known remote SSRC of {@link #videoStream}.
     */
    private long videoRemoteSSRC = SSRC_UNKNOWN;

    /**
     * The listener that the <tt>CallPeer</tt> registered for local user audio
     * level events.
     */
    private SimpleAudioLevelListener localAudioLevelListener = null;

    /**
     * The object that we are using to sync operations on
     * <tt>localAudioLevelListener</tt>.
     */
    private final Object localAudioLevelListenerLock = new Object();

    /**
     * The listener that our <tt>CallPeer</tt> registered for stream audio
     * level events.
     */
    private SimpleAudioLevelListener streamAudioLevelListener = null;

    /**
     * The object that we are using to sync operations on
     * <tt>streamAudioLevelListener</tt>.
     */
    private final Object streamAudioLevelListenerLock = new Object();

    /**
     * The listener that our <tt>CallPeer</tt> registers for CSRC audio level
     * events.
     */
    private CsrcAudioLevelListener csrcAudioLevelListener = null;

    /**
     * The object that we are using to sync operations on
     * <tt>csrcAudioLevelListener</tt>.
     */
    private final Object csrcAudioLevelListenerLock = new Object();

    /**
     * Determines whether we have placed the call on hold locally.
     */
    private boolean locallyOnHold = false;

    /**
     * Indicates whether this handler has already started at least one of its
     * streams, at least once.
     */
    private boolean isStarted = false;

    /**
     * Contains all dynamic payload type mappings that have been made for this
     * call.
     */
    private final DynamicPayloadTypeRegistry dynamicPayloadTypes
        = new DynamicPayloadTypeRegistry();

    /**
     * Contains all RTP extension mappings (those made through the extmap
     * attribute) that have been bound during this call.
     */
    private final DynamicRTPExtensionsRegistry rtpExtensionsRegistry
        = new DynamicRTPExtensionsRegistry();

    /**
     * Holds the ZRTP controls used for the current call.
     */
    private Map<MediaType, ZrtpControl> zrtpControls =
        new Hashtable<MediaType, ZrtpControl>();

    /**
     * The <tt>List</tt> of <tt>VideoListener</tt>s interested in
     * <tt>VideoEvent</tt>s fired by this instance or rather its
     * <tt>VideoMediaStream</tt>.
     */
    private final List<VideoListener> videoListeners
        = new LinkedList<VideoListener>();

    /**
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of {@link #audioStream} and
     * {@link #videoStream}.
     */
    private final PropertyChangeListener streamPropertyChangeListener
        = new PropertyChangeListener()
    {

        /**
         * Notifies this <tt>PropertyChangeListener</tt> that the value of
         * a specific property of the notifier it is registered with has
         * changed.
         *
         * @param evt a <tt>PropertyChangeEvent</tt> which describes the
         * source of the event, the name of the property which has changed
         * its value and the old and new values of the property
         * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
         */
        public void propertyChange(PropertyChangeEvent evt)
        {
            String propertyName = evt.getPropertyName();

            if (MediaStream.PNAME_LOCAL_SSRC.equals(propertyName))
            {
                Object source = evt.getSource();

                if (source == audioStream)
                    setAudioLocalSSRC(audioStream.getLocalSourceID());
                else if (source == videoStream)
                    setVideoLocalSSRC(videoStream.getLocalSourceID());
            }
            else if (MediaStream.PNAME_REMOTE_SSRC.equals(propertyName))
            {
                Object source = evt.getSource();

                if (source == audioStream)
                    setAudioRemoteSSRC(audioStream.getRemoteSourceID());
                else if (source == videoStream)
                    setVideoRemoteSSRC(videoStream.getRemoteSourceID());
            }
        }
    };

    /**
     * The neomedia <tt>VideoListener</tt> which listens to {@link #videoStream}
     * for changes in the availability of visual <tt>Component</tt>s displaying
     * remote video and re-fires them as
     * <tt>net.java.sip.communicator.service.protocol.event.VideoEvent</tt>s
     * originating from this instance.
     */
    private final net.java.sip.communicator.service.neomedia.event.VideoListener
        videoStreamVideoListener = new net.java.sip.communicator.service
            .neomedia.event.VideoListener()
    {
        /**
         * Notifies this neomedia <tt>VideoListener</tt> that a new visual
         * <tt>Component</tt> displaying remote video has been added in
         * {@link CallPeerMediaHandler#videoStream}.
         *
         * @param event the neomedia <tt>VideoEvent</tt> which specifies the
         * newly-added visual <tt>Component</tt> displaying remote video
         */
        public void videoAdded(
                net.java.sip.communicator.service.neomedia.event.VideoEvent
                    event)
        {
            if (fireVideoEvent(
                    event.getType(),
                    event.getVisualComponent(),
                    event.getOrigin()))
                event.consume();
        }

        /**
         * Notifies this neomedia <tt>VideoListener</tt> that a visual
         * <tt>Component</tt> displaying remote video has been removed from
         * {@link CallPeerMediaHandler#videoStream}.
         *
         * @param event the neomedia <tt>VideoEvent</tt> which specifies the
         * removed visual <tt>Component</tt> displaying remote video
         */
        public void videoRemoved(
                net.java.sip.communicator.service.neomedia.event.VideoEvent
                    event)
        {
            // VIDEO_REMOVED is forwarded the same way as VIDEO_ADDED is.
            videoAdded(event);
        }

        public void videoUpdate(
                net.java.sip.communicator.service.neomedia.event.VideoEvent
                    event)
        {
            fireVideoEvent(
                neomedia2protocol(event, CallPeerMediaHandler.this));
        }
    };

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeer</tt> instance that we will be managing
     * media for.
     * @param zrtpController the object that would be responsible for
     * controlling zrtp, and which most often would be the peer itself.
     */
    public CallPeerMediaHandler(T            peer,
                                ZrtpListener zrtpController)
    {
        this.peer = peer;
        this.zrtpController = zrtpController;
    }

    /**
     * Puts all <tt>MediaStream</tt>s in this handler locally on or off hold
     * (according to the value of <tt>locallyOnHold</tt>). This would also be
     * taken into account when the next update offer is generated.
     *
     * @param locallyOnHold <tt>true</tt> if we are to make our audio stream
     * stop transmitting and <tt>false</tt> if we are to start transmitting
     * again.
     */
    public void setLocallyOnHold(boolean locallyOnHold)
    {
        this.locallyOnHold = locallyOnHold;

        if(locallyOnHold)
        {
            if(audioStream != null)
            {
                audioStream.setDirection(audioStream.getDirection()
                            .and(MediaDirection.SENDONLY));
                audioStream.setMute(locallyOnHold);
            }
            if(videoStream != null)
            {
                videoStream.setDirection(videoStream.getDirection()
                            .and(MediaDirection.SENDONLY));
                videoStream.setMute(locallyOnHold);
            }
        }
        else
        {
            //off hold - make sure that we re-enable sending
            if(audioStream != null)
            {
                audioStream.setDirection(audioStream.getDirection()
                            .or(MediaDirection.SENDONLY));
                audioStream.setMute(locallyOnHold);
            }
            if(videoStream != null
                && videoStream.getDirection() != MediaDirection.INACTIVE)
            {
                videoStream.setDirection(videoStream.getDirection()
                            .or(MediaDirection.SENDONLY));
                videoStream.setMute(locallyOnHold);
            }
        }
    }


    /**
     * Closes and null-ifies all streams and connectors and readies this media
     * handler for garbage collection (or reuse). Synchronized if any other
     * stream operations are in process we won't interrupt them.
     */
    public synchronized void close()
    {
        closeStream(MediaType.AUDIO);
        closeStream(MediaType.VIDEO);

        locallyOnHold = false;
    }

    /**
     * Closes the <tt>MediaStream</tt> that this <tt>MediaHandler</tt> uses for
     * specified media <tt>type</tt> and prepares it for garbage collection.
     *
     * @param type the <tt>MediaType</tt> that we'd like to stop a stream for.
     */
    protected void closeStream(MediaType type)
    {
        if( type == MediaType.AUDIO)
        {
            setAudioStream(null);

        }
        else
        {
            setVideoStream(null);
        }

        this.getTransportManager().closeStreamConnector(type);

        // clears the zrtp controls used for current call.
        ZrtpControl zrtpCtrl = zrtpControls.get(type);
        if (zrtpCtrl != null)
        {
            zrtpCtrl.cleanup();
            zrtpControls.remove(type);
        }
    }

    /**
     * Determines whether this handler's streams have been placed on hold.
     *
     * @return <tt>true</tt> if this handler's streams have been placed on hold
     * and <tt>false</tt> otherwise.
     */
    public boolean isLocallyOnHold()
    {
        return locallyOnHold;
        //no need to actually check stream directions because we only update
        //them through the setLocallyOnHold() method so if the value of the
        //locallyOnHold field has changed, so have stream directions.
    }

    /**
     * Causes this handler's <tt>AudioMediaStream</tt> to stop transmitting the
     * audio being fed from this stream's <tt>MediaDevice</tt> and transmit
     * silence instead.
     *
     * @param mute <tt>true</tt> if we are to make our audio stream start
     * transmitting silence and <tt>false</tt> if we are to end the transmission
     * of silence and use our stream's <tt>MediaDevice</tt> again.
     */
    public void setMute(boolean mute)
    {
        if (audioStream != null)
            audioStream.setMute(mute);
    }

    /**
     * Creates a new
     * <tt>net.java.sip.communicator.service.protocol.event.VideoEvent</tt>
     * instance which represents the same notification/information as a specific
     * <tt>net.java.sip.communicator.service.neomedia.event.VideoEvent</tt>.
     *
     * @param neomediaEvent the
     * <tt>net.java.sip.communicator.service.neomedia.event.VideoEvent</tt> to
     * represent as a
     * <tt>net.java.sip.communicator.service.protocol.event.VideoEvent</tt>
     * @param sender the <tt>Object</tt> to be reported as the source of the
     * new <tt>VideoEvent</tt>
     * @return a new
     * <tt>net.java.sip.communicator.service.protocol.event.VideoEvent</tt>
     * which represents the same notification/information as the specified
     * <tt>neomediaEvent</tt>
     */
    private static VideoEvent neomedia2protocol(
            net.java.sip.communicator.service.neomedia.event.VideoEvent
                neomediaEvent,
            Object sender)
    {
        if (neomediaEvent instanceof net.java.sip.communicator.service
                                          .neomedia.event.SizeChangeVideoEvent)
        {
            net.java.sip.communicator.service.neomedia.event.SizeChangeVideoEvent
                neomediaSizeChangeEvent
                    = (net.java.sip.communicator.service.neomedia.event
                                    .SizeChangeVideoEvent)neomediaEvent;

            return
                new SizeChangeVideoEvent(
                        sender,
                        neomediaEvent.getVisualComponent(),
                        neomediaEvent.getOrigin(),
                        neomediaSizeChangeEvent.getWidth(),
                        neomediaSizeChangeEvent.getHeight());
        }
        else
            throw new IllegalArgumentException("neomediaEvent");
    }

    /**
     * Determines whether the audio stream of this media handler is currently
     * on mute.
     *
     * @return <tt>true</tt> if local audio transmission is currently on mute
     * and <tt>false</tt> otherwise.
     */
    public boolean isMute()
    {
        return (audioStream != null) && audioStream.isMute();
    }

    /**
     * Gets the <tt>MediaDirection</tt> value which represents the preference of
     * the user with respect to streaming media of the specified
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> to retrieve the user preference
     * for
     * @return a <tt>MediaDirection</tt> value which represents the preference
     * of the user with respect to streaming media of the specified
     * <tt>mediaType</tt>
     */
    protected MediaDirection getDirectionUserPreference(MediaType mediaType)
    {
        switch (mediaType)
        {
        case AUDIO:
            return audioDirectionUserPreference;
        case VIDEO:
            return videoDirectionUserPreference;
        default:
            throw new IllegalArgumentException("mediaType");
        }
    }

    /**
     * Specifies whether this media handler should be allowed to transmit
     * local video.
     *
     * @param enabled  <tt>true</tt> if the media handler should transmit local
     * video and <tt>false</tt> otherwise.
     */
    public void setLocalVideoTransmissionEnabled(boolean enabled)
    {
        MediaDirection oldValue = videoDirectionUserPreference;
        MediaDirection newValue = null;

        videoDirectionUserPreference
            = enabled ? MediaDirection.SENDRECV : MediaDirection.RECVONLY;

        newValue = videoDirectionUserPreference;

        firePropertyChange(OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING,
                oldValue, newValue);
    }

    /**
     * Determines whether this media handler is currently set to transmit local
     * video.
     *
     * @return <tt>true</tt> if the media handler is set to transmit local video
     * and false otherwise.
     */
    public boolean isLocalVideoTransmissionEnabled()
    {
        return videoDirectionUserPreference.allowsSending();
    }

    /**
     * Specifies whether this media handler should be allowed to transmit
     * local audio.
     *
     * @param enabled  <tt>true</tt> if the media handler should transmit local
     * audio and <tt>false</tt> otherwise.
     */
    public void setLocalAudioTransmissionEnabled(boolean enabled)
    {
        audioDirectionUserPreference
            = enabled ? MediaDirection.SENDRECV : MediaDirection.RECVONLY;
    }

    /**
     * Determines whether this media handler is currently set to transmit local
     * audio.
     *
     * @return <tt>true</tt> if the media handler is set to transmit local audio
     * and <tt>false</tt> otherwise.
     */
    public boolean isLocalAudioTransmissionEnabled()
    {
        return audioDirectionUserPreference.allowsSending();
    }

    /**
     * Sets the last-known local SSRC of {@link #audioStream}.
     *
     * @param audioLocalSSRC the last-known local SSRC of {@link #audioStream}
     */
    private void setAudioLocalSSRC(long audioLocalSSRC)
    {
        if (this.audioLocalSSRC != audioLocalSSRC)
        {
            long oldValue = this.audioLocalSSRC;

            this.audioLocalSSRC = audioLocalSSRC;

            firePropertyChange(AUDIO_LOCAL_SSRC, oldValue, this.audioLocalSSRC);
        }
    }

    /**
     * Sets the last-known remote SSRC of {@link #audioStream}.
     *
     * @param audioRemoteSSRC the last-known remote SSRC of {@link #audioStream}
     */
    private void setAudioRemoteSSRC(long audioRemoteSSRC)
    {
        if (this.audioRemoteSSRC != audioRemoteSSRC)
        {
            long oldValue = this.audioRemoteSSRC;

            this.audioRemoteSSRC = audioRemoteSSRC;

            firePropertyChange(
                AUDIO_REMOTE_SSRC,
                oldValue,
                this.audioRemoteSSRC);
        }
    }

    /**
     * Sets the SAS verifications state of the call.
     *
     * @param isVerified the new SAS verification status
     */
    public void setSasVerified(boolean isVerified )
    {
        if(audioStream != null)
            audioStream.getZrtpControl().setSASVerification(isVerified);

        if(videoStream != null)
            videoStream.getZrtpControl().setSASVerification(isVerified);
    }

    /**
     * Returns the secure state of the call. If both audio and video is secured.
     *
     * @return the call secure state
     */
    public boolean isSecure()
    {
        boolean isAudioSecured = false;
        if(audioStream != null)
            isAudioSecured = audioStream.getZrtpControl()
                .getSecureCommunicationStatus();
        else
            isAudioSecured = true; // as we don't use audio, we don't mind it

        boolean isVideoSecured = false;
        if(videoStream != null)
            isVideoSecured = videoStream.getZrtpControl()
                .getSecureCommunicationStatus();
        else
            isVideoSecured = true; // as we don't use video, we don't mind it

        return isAudioSecured && isVideoSecured;
    }

    /**
     * Passes <tt>multiStreamData</tt> to the video stream that we are using
     * in this media handler (if any) so that the underlying ZRTP lib could
     * properly handle stream security.
     *
     * @param multiStreamData the data that we are supposed to pass to our
     * video stream.
     */
    public void startZrtpMultistream(byte[] multiStreamData)
    {
        if(videoStream != null)
            videoStream.getZrtpControl().setMultistream(multiStreamData);
    }

    /**
     * Gets the last-known remote SSRC of the audio <tt>MediaStream</tt> of this
     * instance.
     *
     * @return the last-known remote SSRC of the audio <tt>MediaStream</tt> of
     * this instance
     */
    public long getAudioRemoteSSRC()
    {
        return audioRemoteSSRC;
    }

    /**
     * Gets a <tt>MediaDevice</tt> which is capable of capture and/or playback
     * of media of the specified <tt>MediaType</tt>, is the default choice of
     * the user for a <tt>MediaDevice</tt> with the specified <tt>MediaType</tt>
     * and is appropriate for the current states of the associated
     * <tt>CallPeer</tt> and <tt>Call</tt>.
     * <p>
     * For example, when the local peer is acting as a conference focus in the
     * <tt>Call</tt> of the associated <tt>CallPeer</tt>, the audio device must
     * be a mixer.
     * </p>
     *
     * @param mediaType the <tt>MediaType</tt> in which the retrieved
     * <tt>MediaDevice</tt> is to capture and/or play back media
     * @return a <tt>MediaDevice</tt> which is capable of capture and/or
     * playback of media of the specified <tt>mediaType</tt>, is the default
     * choice of the user for a <tt>MediaDevice</tt> with the specified
     * <tt>mediaType</tt> and is appropriate for the current states of the
     * associated <tt>CallPeer</tt> and <tt>Call</tt>
     */
    protected MediaDevice getDefaultDevice(MediaType mediaType)
    {
        return peer.getCall().getDefaultDevice(mediaType);
    }

    /**
     * Registers a specific <tt>VideoListener</tt> with this instance so that it
     * starts receiving notifications from it about changes in the availability
     * of visual <tt>Component</tt>s displaying video.
     *
     * @param listener the <tt>VideoListener</tt> to be registered with this
     * instance and to start receiving notifications from it about changes in
     * the availability of visual <tt>Component</tt>s displaying video
     */
    public void addVideoListener(VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (videoListeners)
        {
            if (!videoListeners.contains(listener))
                videoListeners.add(listener);
        }
    }

    /**
     * Sets the RTP media stream that this instance uses to stream audio to a
     * specific <tt>AudioMediaStream</tt>.
     *
     * @param audioStream the <tt>AudioMediaStream</tt> to be set as the RTP
     * media stream that this instance uses to stream audio
     */
    protected void setAudioStream(AudioMediaStream audioStream)
    {
        if (this.audioStream != audioStream)
        {
            if (this.audioStream != null)
            {
                this.audioStream
                        .removePropertyChangeListener(
                            streamPropertyChangeListener);

                this.audioStream.close();
            }

            this.audioStream = audioStream;

            long audioLocalSSRC;
            long audioRemoteSSRC;

            if (this.audioStream != null)
            {
                this.audioStream
                        .addPropertyChangeListener(
                            streamPropertyChangeListener);
                audioLocalSSRC = this.audioStream.getLocalSourceID();
                audioRemoteSSRC = this.audioStream.getRemoteSourceID();
            }
            else
                audioLocalSSRC = audioRemoteSSRC = SSRC_UNKNOWN;

            setAudioLocalSSRC(audioLocalSSRC);
            setAudioRemoteSSRC(audioRemoteSSRC);
        }
    }

    /**
     * Sets the last-known local SSRC of {@link #videoStream}.
     *
     * @param videoLocalSSRC the last-known local SSRC of {@link #videoStream}
     */
    private void setVideoLocalSSRC(long videoLocalSSRC)
    {
        if (this.videoLocalSSRC != videoLocalSSRC)
        {
            long oldValue = this.videoLocalSSRC;

            this.videoLocalSSRC = videoLocalSSRC;

            firePropertyChange(VIDEO_LOCAL_SSRC, oldValue, this.videoLocalSSRC);
        }
    }

    /**
     * Sets the last-known remote SSRC of {@link #videoStream}.
     *
     * @param videoRemoteSSRC the last-known remote SSRC of {@link #videoStream}
     */
    private void setVideoRemoteSSRC(long videoRemoteSSRC)
    {
        if (this.videoRemoteSSRC != videoRemoteSSRC)
        {
            long oldValue = this.videoRemoteSSRC;

            this.videoRemoteSSRC = videoRemoteSSRC;

            firePropertyChange(
                VIDEO_REMOTE_SSRC,
                oldValue,
                this.videoRemoteSSRC);
        }
    }

    /**
     * Sets the RTP media stream that this instance uses to stream video to a
     * specific <tt>VideoMediaStream</tt>.
     *
     * @param videoStream the <tt>VideoMediaStream</tt> to be set as the RTP
     * media stream that this instance uses to stream video
     */
    private void setVideoStream(VideoMediaStream videoStream)
    {
        if (this.videoStream != videoStream)
        {
            /*
             * Make sure we will no longer notify the registered VideoListeners
             * about changes in the availability of video in the old
             * videoStream.
             */
            Component oldVisualComponent = null;

            if (this.videoStream != null)
            {
                this.videoStream
                        .removePropertyChangeListener(
                            streamPropertyChangeListener);

                this.videoStream.removeVideoListener(videoStreamVideoListener);
                oldVisualComponent = this.videoStream.getVisualComponent();

                this.videoStream.close();
            }

            this.videoStream = videoStream;

            long videoLocalSSRC;
            long videoRemoteSSRC;
            /*
             * Make sure we will notify the registered VideoListeners about
             * changes in the availability of video in the new videoStream.
             */
            Component newVisualComponent = null;

            if (this.videoStream != null)
            {
                this.videoStream
                        .addPropertyChangeListener(
                            streamPropertyChangeListener);
                videoLocalSSRC = this.videoStream.getLocalSourceID();
                videoRemoteSSRC = this.videoStream.getRemoteSourceID();

                this.videoStream.addVideoListener(videoStreamVideoListener);
                newVisualComponent = this.videoStream.getVisualComponent();
            }
            else
                videoLocalSSRC = videoRemoteSSRC = SSRC_UNKNOWN;

            setVideoLocalSSRC(videoLocalSSRC);
            setVideoRemoteSSRC(videoRemoteSSRC);

            /*
             * Notify the VideoListeners in case there was a change in the
             * availability of the visual <tt>Component</tt>s displaying remote
             * video.
             */
            if (oldVisualComponent != null)
                fireVideoEvent(
                    VideoEvent.VIDEO_REMOVED,
                    oldVisualComponent,
                    VideoEvent.REMOTE);
            if (newVisualComponent != null)
                fireVideoEvent(
                    VideoEvent.VIDEO_ADDED,
                    newVisualComponent,
                    VideoEvent.REMOTE);
        }
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>CallPeerMediaHandler</tt> about a specific type of change in the
     * availability of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this <tt>CallPeerMediaHandler</tt>
     * @param origin {@link VideoEvent#LOCAL} if the origin of the video is
     * local (e.g. it is being locally captured); {@link VideoEvent#REMOTE} if
     * the origin of the video is remote (e.g. a remote peer is streaming it)
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    protected boolean fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        VideoListener[] listeners;

        synchronized (videoListeners)
        {
            listeners
                = videoListeners
                    .toArray(new VideoListener[videoListeners.size()]);
        }

        boolean consumed;

        if (listeners.length > 0)
        {
            VideoEvent event
                = new VideoEvent(this, type, visualComponent, origin);

            for (VideoListener listener : listeners)
                switch (type)
                {
                    case VideoEvent.VIDEO_ADDED:
                        listener.videoAdded(event);
                        break;
                    case VideoEvent.VIDEO_REMOVED:
                        listener.videoRemoved(event);
                        break;
                    default:
                        throw new IllegalArgumentException("type");
                }

            consumed = event.isConsumed();
        }
        else
            consumed = false;
        return consumed;
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>CallPeerMediaHandler</tt> about a specific <tt>VideoEvent</tt>.
     *
     * @param event the <tt>VideoEvent</tt> to fire to the
     * <tt>VideoListener</tt>s registered with this
     * <tt>CallPeerMediaHandler</tt>
     */
    public void fireVideoEvent(VideoEvent event)
    {
        VideoListener[] listeners;

        synchronized (videoListeners)
        {
            listeners
                = videoListeners
                    .toArray(new VideoListener[videoListeners.size()]);
        }

        for (VideoListener listener : listeners)
            switch (event.getType())
            {
                case VideoEvent.VIDEO_ADDED:
                    listener.videoAdded(event);
                    break;
                case VideoEvent.VIDEO_REMOVED:
                    listener.videoRemoved(event);
                    break;
                default:
                    listener.videoUpdate(event);
                    break;
            }
    }

    /**
     * Gets local visual <tt>Component</tt> of the local peer.
     *
     * @return visual <tt>Component</tt>
     */
    public Component createLocalVisualComponent()
    {
        return (videoStream == null || !isLocalVideoTransmissionEnabled())
            ? null : videoStream.createLocalVisualComponent();
    }

    /**
     * Dispose local visual <tt>Component</tt> of the local peer.
     */
    public void disposeLocalVisualComponent()
    {
        if(videoStream != null)
        {
            videoStream.disposeLocalVisualComponent();
        }
    }

    /**
     * Gets the visual <tt>Component</tt> in which video from the remote peer is
     * currently being rendered or <tt>null</tt> if there is currently no video
     * streaming from the remote peer.
     *
     * @return the visual <tt>Component</tt> in which video from the remote peer
     * is currently being rendered or <tt>null</tt> if there is currently no
     * video streaming from the remote peer
     */
    public Component getVisualComponent()
    {
        return (videoStream == null) ? null : videoStream.getVisualComponent();
    }

    /**
     * Unregisters a specific <tt>VideoListener</tt> from this instance so that
     * it stops receiving notifications from it about changes in the
     * availability of visual <tt>Component</tt>s displaying video.
     *
     * @param listener the <tt>VideoListener</tt> to be unregistered from this
     * instance and to stop receiving notifications from it about changes in the
     * availability of visual <tt>Component</tt>s displaying video
     */
    public void removeVideoListener(VideoListener listener)
    {
        if (listener != null)
            synchronized (videoListeners)
            {
                videoListeners.remove(listener);
            }
    }

    /**
     * If the local <tt>AudioMediaStream</tt> has already been created, sets
     * <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt> that it should
     * notify for local user level events. Otherwise stores a reference to
     * <tt>listener</tt> so that we could add it once we create the stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add or
     * <tt>null</tt> if we are trying to remove it.
     */
    public void setLocalUserAudioLevelListener(
                                            SimpleAudioLevelListener listener)
    {
        synchronized(localAudioLevelListenerLock)
        {
            this.localAudioLevelListener = listener;

            if(audioStream != null)
                audioStream.setLocalUserAudioLevelListener(listener);
        }
    }

    /**
     * If the local <tt>AudioMediaStream</tt> has already been created, sets
     * <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt> that it should
     * notify for stream user level events. Otherwise stores a reference to
     * <tt>listener</tt> so that we could add it once we create the stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add or
     * <tt>null</tt> if we are trying to remove it.
     *
     */
    public void setStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        synchronized(streamAudioLevelListenerLock)
        {
            this.streamAudioLevelListener = listener;

            if(audioStream != null)
                audioStream.setStreamAudioLevelListener(listener);
        }
    }

    /**
     * Sets <tt>csrcAudioLevelListener</tt> as the listener that will be
     * receiving notifications for changes in the audio levels of the remote
     * participants that our peer is mixing.
     *
     * @param csrcAudioLevelListener the <tt>CsrcAudioLevelListener</tt> to set
     * to our audio streams.
     */
    public void setCsrcAudioLevelListener(
                                CsrcAudioLevelListener csrcAudioLevelListener)
    {
        synchronized(csrcAudioLevelListenerLock)
        {
            this.csrcAudioLevelListener = csrcAudioLevelListener;

            if(audioStream != null)
                audioStream.setCsrcAudioLevelListener(csrcAudioLevelListener);
        }
    }

    /**
     * Returns the currently valid <tt>ZrtpControls</tt> map.
     *
     * @return the currently valid <tt>ZrtpControls</tt> map.
     */
    protected Map<MediaType, ZrtpControl> getZrtpControls()
    {
        return this.zrtpControls;
    }

    /**
     * Creates if necessary, and configures the stream that this
     * <tt>MediaHandler</tt> is using for the <tt>MediaType</tt> matching the
     * one of the <tt>MediaDevice</tt>.
     *
     * @param connector the <tt>MediaConnector</tt> that we'd like to bind the
     * newly created stream to.
     * @param device the <tt>MediaDevice</tt> that we'd like to attach the newly
     * created <tt>MediaStream</tt> to.
     * @param format the <tt>MediaFormat</tt> that we'd like the new
     * <tt>MediaStream</tt> to be set to transmit in.
     * @param target the <tt>MediaStreamTarget</tt> containing the RTP and RTCP
     * address:port couples that the new stream would be sending packets to.
     * @param direction the <tt>MediaDirection</tt> that we'd like the new
     * stream to use (i.e. sendonly, sendrecv, recvonly, or inactive).
     * @param rtpExtensions the list of <tt>RTPExtension</tt>s that should be
     * enabled for this stream.
     *
     * @return the newly created <tt>MediaStream</tt>.
     *
     * @throws OperationFailedException if creating the stream fails for any
     * reason (like for example accessing the device or setting the format).
     */
    protected MediaStream initStream(StreamConnector      connector,
                                     MediaDevice          device,
                                     MediaFormat          format,
                                     MediaStreamTarget    target,
                                     MediaDirection       direction,
                                     List<RTPExtension>   rtpExtensions)
        throws OperationFailedException
    {
        MediaStream stream = null;

        if (device.getMediaType() == MediaType.AUDIO)
            stream = this.audioStream;
        else
        {
            stream = this.videoStream;
        }

        if (stream == null)
        {
            // check whether a control already exists
            ZrtpControl control = zrtpControls.get(format.getMediaType());
            MediaService mediaService
                                = ProtocolMediaActivator.getMediaService();

            if(control == null)
                stream = mediaService.createMediaStream(connector, device);
            else
                stream = mediaService.createMediaStream(
                                            connector, device, control);
        }
        else
        {
            //this is a reinit
        }

        return  configureStream(
                    device, format, target, direction, rtpExtensions, stream);
    }

    /**
     * Configures <tt>stream</tt> to use the specified <tt>format</tt>,
     * <tt>target</tt>, <tt>target</tt>, and <tt>direction</tt>.
     *
     * @param device the <tt>MediaDevice</tt> to be used by <tt>stream</tt>
     * for capture and playback
     * @param format the <tt>MediaFormat</tt> that we'd like the new stream
     * to transmit in.
     * @param target the <tt>MediaStreamTarget</tt> containing the RTP and
     * RTCP address:port couples that the new stream would be sending
     * packets to.
     * @param direction the <tt>MediaDirection</tt> that we'd like the new
     * stream to use (i.e. sendonly, sendrecv, recvonly, or inactive).
     * @param rtpExtensions the list of <tt>RTPExtension</tt>s that should be
     * enabled for this stream.
     * @param stream the <tt>MediaStream</tt> that we'd like to configure.
     *
     * @return the <tt>MediaStream</tt> that we received as a parameter (for
     * convenience reasons).
     *
     * @throws OperationFailedException if setting the <tt>MediaFormat</tt>
     * or connecting to the specified <tt>MediaDevice</tt> fails for some
     * reason.
     */
    protected MediaStream configureStream( MediaDevice          device,
                                           MediaFormat          format,
                                           MediaStreamTarget    target,
                                           MediaDirection       direction,
                                           List<RTPExtension>   rtpExtensions,
                                           MediaStream          stream)
           throws OperationFailedException
    {
        registerDynamicPTsWithStream(stream);
        registerRTPExtensionsWithStream(rtpExtensions, stream);

        stream.setDevice(device);
        stream.setTarget(target);
        stream.setDirection(direction);
        stream.setFormat(format);

        if( stream instanceof AudioMediaStream)
        {
            setAudioStream((AudioMediaStream) stream);

            registerAudioLevelListeners(audioStream);
        }
        else
            setVideoStream((VideoMediaStream) stream);

        if(peer.getCall().isDefaultEncrypted())
        {
            // we use the audio stream for master stream
            // when using zrtp multistreams
            ZrtpControl zrtpControl = stream.getZrtpControl();

            zrtpControl.setZrtpListener(zrtpController);
            zrtpControl.start(stream instanceof AudioMediaStream);
        }

        return stream;
    }

    /**
     * Send empty UDP packet to target destination data/control ports
     * in order to open port on NAT or RTP proxy if any. In order to be really
     * efficient, this method should be called after we send our offer or answer
     *
     * @param target <tt>MediaStreamTarget</tt>
     */
    protected void sendHolePunchPacket(MediaStreamTarget target)
    {
        getTransportManager().sendHolePunchPacket(target, MediaType.VIDEO);
    }

    /**
     * Registers all audio level listeners currently known to this media handler
     * with the specified <tt>audioStream</tt>.
     *
     * @param audioStream the <tt>AudioMediaStream</tt> that we'd like to
     * register our audio level listeners with.
     */
    private void registerAudioLevelListeners(AudioMediaStream audioStream)
    {
        // if we already have a local level listener - register it now.
        synchronized (localAudioLevelListenerLock)
        {
            if (localAudioLevelListener != null)
                audioStream
                    .setLocalUserAudioLevelListener(localAudioLevelListener);
        }

        // if we already have a stream level listener - register it now.
        synchronized (streamAudioLevelListenerLock)
        {
            if (streamAudioLevelListener != null)
                audioStream
                    .setStreamAudioLevelListener(streamAudioLevelListener);
        }

        // if we already have a csrc level listener - register it now.
        synchronized (csrcAudioLevelListenerLock)
        {
            if (csrcAudioLevelListener != null)
                audioStream.setCsrcAudioLevelListener(csrcAudioLevelListener);
        }
    }

    /**
     * Registers all dynamic payload mappings known to this
     * <tt>MediaHandler</tt> with the specified <tt>MediaStream</tt>.
     *
     * @param stream the <tt>MediaStream</tt> that we'd like to register our
     * dynamic payload mappings with.
     */
    private void registerDynamicPTsWithStream(MediaStream stream)
    {
        for ( Map.Entry<MediaFormat, Byte> mapEntry
                        : dynamicPayloadTypes.getMappings().entrySet())
        {
            byte pt = mapEntry.getValue();
            MediaFormat fmt = mapEntry.getKey();

            stream.addDynamicRTPPayloadType(pt, fmt);
        }
    }

    /**
     * Registers with the specified <tt>MediaStream</tt> all RTP extensions
     * negotiated by this <tt>MediaHandler</tt>.
     *
     * @param stream the <tt>MediaStream</tt> that we'd like to register our
     * <tt>RTPExtension</tt>s with.
     * @param rtpExtensions the list of <tt>RTPExtension</tt>s that should be
     * enabled for <tt>stream</tt>.
     */
    private void registerRTPExtensionsWithStream(
                                          List<RTPExtension> rtpExtensions,
                                          MediaStream        stream)
    {
        for ( RTPExtension rtpExtension : rtpExtensions)
        {
            byte extensionID
                = rtpExtensionsRegistry.getExtensionMapping(rtpExtension);

            stream.addRTPExtension(extensionID, rtpExtension);
        }
    }

    /**
     * Gets the <tt>MediaStream</tt> of this <tt>CallPeerMediaHandler</tt> which
     * is of a specific <tt>MediaType</tt>. If this instance doesn't have such a
     * <tt>MediaStream</tt>, returns <tt>null</tt>
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> to
     * retrieve
     * @return the <tt>MediaStream</tt> of this <tt>CallPeerMediaHandler</tt>
     * which is of the specified <tt>mediaType</tt> if this instance has such a
     * <tt>MediaStream</tt>; otherwise, <tt>null</tt>
     */
    public MediaStream getStream(MediaType mediaType)
    {
        switch (mediaType)
        {
        case AUDIO:
            return audioStream;
        case VIDEO:
            return videoStream;
        default:
            throw new IllegalArgumentException("mediaType");
        }
    }

    /**
     * Determines whether the remote party has placed all our streams on hold.
     *
     * @return <tt>true</tt> if all our streams have been placed on hold (i.e.
     * if none of them is currently sending and <tt>false</tt> otherwise.
     */
    public boolean isRemotelyOnHold()
    {
        if(audioStream != null && audioStream.getDirection().allowsSending())
            return false;

        if(videoStream != null && videoStream.getDirection().allowsSending())
            return false;

        return true;
    }

    /**
     * Gets the advanced format parameters list of our device that match
     * remote supported ones.
     *
     * @param remoteFormats remote advanced format parameters found in the
     * SDP message
     * @param localFormats local advanced format parameters of our device
     * @return intersection between our local advanced parameters and remote
     * advanced parameters
     */
    protected List<MediaFormat> intersectFormats(
                                            List<MediaFormat> remoteFormats,
                                            List<MediaFormat> localFormats)
    {
        List<MediaFormat> ret = new ArrayList<MediaFormat>();

        for(MediaFormat remoteFormat : remoteFormats)
        {
            MediaFormat localFormat
                = findMediaFormat(localFormats, remoteFormat);

            if(localFormat != null)
                ret.add(localFormat);
        }
        return ret;
    }

    /**
     * Finds a <tt>MediaFormat</tt> in a specific list of <tt>MediaFormat</tt>s
     * which matches a specific <tt>MediaFormat</tt>.
     *
     * @param formats the list of <tt>MediaFormat</tt>s to find the specified
     * matching <tt>MediaFormat</tt> into
     * @param format encoding of the <tt>MediaFormat</tt> to find
     * @return the <tt>MediaFormat</tt> from <tt>formats</tt> which matches
     * <tt>format</tt> if such a match exists in <tt>formats</tt>; otherwise,
     * <tt>null</tt>
     */
    private MediaFormat findMediaFormat(
            List<MediaFormat> formats, MediaFormat format)
    {
        MediaType mediaType = format.getMediaType();
        String encoding = format.getEncoding();
        double clockRate = format.getClockRate();
        int channels
            = MediaType.AUDIO.equals(mediaType)
                ? ((AudioMediaFormat) format).getChannels()
                : MediaFormatFactory.CHANNELS_NOT_SPECIFIED;

        for(MediaFormat match : formats)
        {
            if (AbstractMediaStream.matches(
                        match,
                        mediaType, encoding, clockRate, channels))
                return match;
        }
        return null;
    }

    /**
     * Compares a list of <tt>RTPExtensoin</tt>s offered by a remote party
     * to the list of locally supported <tt>RTPExtension</tt>s as returned
     * by one of our local <tt>MediaDevice</tt>s and returns a third
     * <tt>List</tt> that contains their intersection. The returned
     * <tt>List</tt> contains extensions supported by both the remote party and
     * the local device that we are dealing with. Direction attributes of both
     * lists are also intersected and the returned <tt>RTPExtension</tt>s have
     * directions valid from a local perspective. In other words, if
     * <tt>remoteExtensions</tt> contains an extension that the remote party
     * supports in a <tt>SENDONLY</tt> mode, and we support that extension in a
     * <tt>SENDRECV</tt> mode, the corresponding entry in the returned list will
     * have a <tt>RECVONLY</tt> direction.
     *
     * @param remoteExtensions the <tt>List</tt> of <tt>RTPExtension</tt>s as
     * advertised by the remote party.
     * @param supportedExtensions the <tt>List</tt> of <tt>RTPExtension</tt>s
     * that a local <tt>MediaDevice</tt> returned as supported.
     *
     * @return the (possibly empty) intersection of both of the extensions lists
     * in a form that can be used for generating an SDP media description or
     * for configuring a stream.
     */
    protected List<RTPExtension> intersectRTPExtensions(
                                    List<RTPExtension> remoteExtensions,
                                    List<RTPExtension> supportedExtensions)
    {
        if(remoteExtensions == null || supportedExtensions == null)
            return new ArrayList<RTPExtension>();

        List<RTPExtension> intersection = new ArrayList<RTPExtension>(
                Math.min(remoteExtensions.size(), supportedExtensions.size()));

        //loop through the list that the remote party sent
        for(RTPExtension remoteExtension : remoteExtensions)
        {
            RTPExtension localExtension = findExtension(
                    supportedExtensions, remoteExtension.getURI().toString());

            if(localExtension == null)
                continue;

            MediaDirection localDir  = localExtension.getDirection();
            MediaDirection remoteDir = remoteExtension.getDirection();

            RTPExtension intersected = new RTPExtension(
                            localExtension.getURI(),
                            localDir.getDirectionForAnswer(remoteDir),
                            remoteExtension.getExtensionAttributes());

            intersection.add(intersected);
        }

        return intersection;
    }

    /**
     * Returns the first <tt>RTPExtension</tt> in <tt>extList</tt> that uses
     * the specified <tt>extensionURN</tt> or <tt>null</tt> if <tt>extList</tt>
     * did not contain such an extension.
     *
     * @param extList the <tt>List</tt> that we will be looking through.
     * @param extensionURN the URN of the <tt>RTPExtension</tt> that we are
     * looking for.
     *
     * @return the first <tt>RTPExtension</tt> in <tt>extList</tt> that uses
     * the specified <tt>extensionURN</tt> or <tt>null</tt> if <tt>extList</tt>
     * did not contain such an extension.
     */
    private RTPExtension findExtension(List<RTPExtension> extList,
                                       String extensionURN)
    {
        for(RTPExtension rtpExt : extList)
            if (rtpExt.getURI().toASCIIString().equals(extensionURN))
                return rtpExt;
        return null;
    }

    /**
     * Returns a (possibly empty) <tt>List</tt> of <tt>RTPExtension</tt>s
     * supported by the device that this media handler uses to handle media of
     * the specified <tt>type</tt>.
     *
     * @param type the <tt>MediaType</tt> of the device whose
     * <tt>RTPExtension</tt>s we are interested in.
     *
     * @return a (possibly empty) <tt>List</tt> of <tt>RTPExtension</tt>s
     * supported by the device that this media handler uses to handle media of
     * the specified <tt>type</tt>.
     */
    protected List<RTPExtension> getExtensionsForType(MediaType type)
    {
        MediaDevice dev = getDefaultDevice(type);

        return dev.getSupportedExtensions();
    }

    /**
     * Returns the {@link DynamicPayloadTypeRegistry} instance we are currently
     * using.
     *
     * @return the {@link DynamicPayloadTypeRegistry} instance we are currently
     * using.
     */
    protected DynamicPayloadTypeRegistry getDynamicPayloadTypes()
    {
        return this.dynamicPayloadTypes;
    }

    /**
     * Returns the {@link DynamicRTPExtensionsRegistry} instance we are
     * currently using.
     *
     * @return the {@link DynamicRTPExtensionsRegistry} instance we are
     * currently using.
     */
    protected DynamicRTPExtensionsRegistry getRtpExtensionsRegistry()
    {
        return this.rtpExtensionsRegistry;
    }

    /**
     * Returns <tt>true</tt> if this handler has already started at least one
     * of its streams, at least once, and <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if this handler has already started at least one
     * of its streams, at least once, and <tt>false</tt> otherwise.
     */
    public boolean isStarted()
    {
        return isStarted;
    }

    /**
     * Returns <tt>true</tt> if this handler has already started at least one
     * of its streams, at least once, and <tt>false</tt> otherwise. If the
     * handler is already started, this method has no effect.
     *
     * @throws IllegalStateException if this method is called without this
     * handler having first seen a media description or having generate an
     * offer.
     */
    public void start()
        throws IllegalStateException
    {
        if(isStarted())
            return;

        MediaStream stream = getStream(MediaType.AUDIO);
        if ( stream != null && !stream.isStarted()
             && isLocalAudioTransmissionEnabled())
        {
            stream.start();
        }

        stream = getStream(MediaType.VIDEO);
        if ( stream != null && !stream.isStarted())
        {
            stream.start();

             // send empty packet to deblock some kind of RTP proxy to let just
             // one user sends its video
            if(stream instanceof VideoMediaStream
               && !isLocalVideoTransmissionEnabled())
            {
                sendHolePunchPacket(stream.getTarget());
            }
        }
    }

    /**
     * Returns the peer that is this media handler's "raison d'etre".
     *
     * @return the {@link MediaAwareCallPeer} that this handler is servicing.
     */
    public T getPeer()
    {
        return peer;
    }

    /**
     * Lets the underlying implementation take note of this error and only
     * then throws it to the using bundles.
     *
     * @param message the message to be logged and then wrapped in a new
     * <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     * <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     * an error and have a new <tt>OperationFailedException</tt> thrown
     *
     * @throws OperationFailedException the exception that we wanted this method
     * to throw.
     */
    protected abstract void throwOperationFailedException( String    message,
                                                           int       errorCode,
                                                           Throwable cause)
        throws OperationFailedException;

    /**
     * Returns the transport manager that is handling our address management.
     *
     * @return the transport manager that is handling our address management.
     */
    public abstract TransportManager<T> getTransportManager();
}
