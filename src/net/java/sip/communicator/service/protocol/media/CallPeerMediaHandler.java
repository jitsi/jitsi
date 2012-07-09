/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.awt.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.control.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.util.event.*;

/**
 * A utility class implementing media control code shared between current
 * telephony implementations. This class is only meant for use by protocol
 * implementations and should not be accessed by bundles that are simply using
 * the telephony functionalities.
 *
 * @param <T> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public abstract class CallPeerMediaHandler
        <T extends MediaAwareCallPeer<?, ?, ?>>
    extends PropertyChangeNotifier
{
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
     * The <tt>VideoMediaStream</tt> which this instance uses to send and
     * receive video.
     */
    private VideoMediaStream videoStream;

    /**
     * Determines whether or not streaming local audio is currently enabled.
     */
    private MediaDirection audioDirectionUserPreference
        = MediaDirection.SENDRECV;

    /**
     * The <tt>AudioMediaStream</tt> which this instance uses to send and
     * receive audio.
     */
    private AudioMediaStream audioStream;

    /**
     * List of advertised encryption methods. Indicated before establishing the
     * call.
     */
    private List<SrtpControlType> advertisedEncryptionMethods =
        new ArrayList<SrtpControlType>();

    /**
     * A reference to the CallPeer instance that this handler is managing media
     * streams for.
     */
    private final T peer;

    /**
     * The <tt>SrtpListener</tt> which is responsible for the SRTP control. Most
     * often than not, it is the <tt>peer</tt> itself.
     */
    private final SrtpListener srtpListener;

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
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of the <tt>Call</tt> of {@link #peer}.
     */
    private final CallPropertyChangeListener callPropertyChangeListener;

    /**
     * The <tt>KeyFrameRequester</tt> implemented by this
     * <tt>CallPeerMediaHandler</tt>.
     */
    private final KeyFrameControl.KeyFrameRequester keyFrameRequester
        = new KeyFrameControl.KeyFrameRequester()
        {
            public boolean requestKeyFrame()
            {
                return CallPeerMediaHandler.this.requestKeyFrame();
            }
        };

    /**
     * The <tt>Object</tt> to synchronize the access to
     * {@link #localUserAudioLevelListeners}.
     */
    private final Object visualComponentResolveSyncRoot = new Object();

    /**
     * The list of <tt>VisualComponentResolveListener</tt>s interested in
     * visual component resolve events.
     */
    private List<VisualComponentResolveListener> visualComponentResolveListeners;

    /**
     * The state of this instance which may be shared with multiple other
     * <tt>CallPeerMediaHandler</tt>s.
     */
    private MediaHandler mediaHandler;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of the <tt>MediaStream</tt>s of this instance.
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
                firePropertyChange(
                        evt.getPropertyName(),
                        evt.getOldValue(),
                        evt.getNewValue());
            }
        };

    /**
     * The aid which implements the boilerplate related to adding and removing
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them on behalf
     * of this instance.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this, true);

    /**
     * The <tt>VideoListener</tt> which listens to the video
     * <tt>MediaStream</tt> of this instance for changes in the availability of
     * visual <tt>Component</tt>s displaying remote video and re-fires them as
     * originating from this instance.
     */
    private final VideoListener videoStreamVideoListener
        = new VideoListener()
        {
            public void videoAdded(VideoEvent event)
            {
                VideoEvent clone = event.clone(CallPeerMediaHandler.this);

                fireVideoEvent(clone);
                if (clone.isConsumed())
                    event.consume();
            }

            public void videoRemoved(VideoEvent event)
            {
                // Forwarded in the same way as VIDEO_ADDED.
                videoAdded(event);
            }

            public void videoUpdate(VideoEvent event)
            {
                // Forwarded in the same way as VIDEO_ADDED.
                videoAdded(event);
            }
        };

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeer</tt> instance that we will be managing
     * media for.
     * @param srtpListener the object that receives SRTP security events.
     */
    public CallPeerMediaHandler(T            peer,
                                SrtpListener srtpListener)
    {
        this.peer = peer;
        this.srtpListener = srtpListener;

        setMediaHandler(new MediaHandler());

        /*
         * Listen to the call of peer in order to track the user's choice with
         * respect to the default audio device.
         */
        MediaAwareCall<?, ?, ?> call = this.peer.getCall();

        if (call == null)
            callPropertyChangeListener = null;
        else
        {
            callPropertyChangeListener = new CallPropertyChangeListener(call);
            call.addPropertyChangeListener(callPropertyChangeListener);
        }
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

        // On hold.
        if(locallyOnHold)
        {
            MediaStream audioStream = getStream(MediaType.AUDIO);

            if(audioStream != null)
            {
                audioStream.setDirection(
                        audioStream.getDirection().and(
                                MediaDirection.SENDONLY));
                audioStream.setMute(locallyOnHold);
            }

            MediaStream videoStream = getStream(MediaType.VIDEO);

            if(videoStream != null)
            {
                videoStream.setDirection(
                        videoStream.getDirection().and(
                                MediaDirection.SENDONLY));
                videoStream.setMute(locallyOnHold);
            }
        }
        /*
         * Off hold. Make sure that we re-enable sending only if other party is
         * not on hold.
         */
        else if (!CallPeerState.ON_HOLD_MUTUALLY.equals(getPeer().getState()))
        {
            MediaStream audioStream = getStream(MediaType.AUDIO);

            if(audioStream != null)
            {
                audioStream.setDirection(
                        audioStream.getDirection().or(MediaDirection.SENDONLY));
                audioStream.setMute(locallyOnHold);
            }

            MediaStream videoStream = getStream(MediaType.VIDEO);

            if((videoStream != null)
                && (videoStream.getDirection() != MediaDirection.INACTIVE))
            {
                videoStream.setDirection(
                        videoStream.getDirection().or(MediaDirection.SENDONLY));
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

        if (callPropertyChangeListener != null)
            callPropertyChangeListener.call.removePropertyChangeListener(
                    callPropertyChangeListener);

        setMediaHandler(null);
    }

    /**
     * Closes the <tt>MediaStream</tt> that this instance uses for a specific
     * <tt>MediaType</tt> and prepares it for garbage collection.
     *
     * @param mediaType the <tt>MediaType</tt> that we'd like to stop a stream
     * for.
     */
    protected void closeStream(MediaType mediaType)
    {
        /*
         * This CallPeerMediaHandler releases its reference to the MediaStream
         * it has initialized via #initStream().
         */
        boolean mediaHandlerCloseStream = false;

        switch (mediaType)
        {
        case AUDIO:
            if (audioStream != null)
            {
                audioStream = null;
                mediaHandlerCloseStream = true;
            }
            break;
        case VIDEO:
            if (videoStream != null)
            {
                videoStream = null;
                mediaHandlerCloseStream = true;
            }
            break;
        }
        if (mediaHandlerCloseStream)
            mediaHandler.closeStream(this, mediaType);

        getTransportManager().closeStreamConnector(mediaType);
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
        MediaStream audioStream = getStream(MediaType.AUDIO);

        if (audioStream != null)
            audioStream.setMute(mute);
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
        MediaStream audioStream = getStream(MediaType.AUDIO);

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

        videoDirectionUserPreference
            = enabled ? MediaDirection.SENDRECV : MediaDirection.RECVONLY;

        MediaDirection newValue = videoDirectionUserPreference;

        /* we do not send an event here if video is enabled because we have to
         * wait video stream starts to have correct MediaDevice set in
         * VideoMediaDeviceSession
         */
        if(!enabled)
        {
            firePropertyChange(
                    OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING,
                    oldValue, newValue);
        }
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
     * Returns the secure state of the call. If both audio and video is secured.
     *
     * @return the call secure state
     */
    public boolean isSecure()
    {
        for (MediaType mediaType : MediaType.values())
        {
            MediaStream stream = getStream(mediaType);

            /*
             * If a stream for a specific MediaType does not exist, it's
             * considered secure.
             */
            if ((stream != null)
                    && !stream.getSrtpControl().getSecureCommunicationStatus())
                return false;
        }
        return true;
    }

    /**
     * Returns the advertised methods for securing the call,
     * this are the methods like SDES, ZRTP that are
     * indicated in the initial session initialization. Missing here doesn't
     * mean the other party don't support it.
     * @return the advertised encryption methods.
     */
    public SrtpControlType[] getAdvertisedEncryptionMethods()
    {
        return
            advertisedEncryptionMethods.toArray(
                    new SrtpControlType[advertisedEncryptionMethods.size()]);
    }

    /**
     * Adds encryption method to the list of advertised secure methods.
     * @param encryptionMethod the method to add.
     */
    public void addAdvertisedEncryptionMethod(SrtpControlType encryptionMethod)
    {
        if(!advertisedEncryptionMethods.contains(encryptionMethod))
            advertisedEncryptionMethods.add(encryptionMethod);
    }

    /**
     * Passes <tt>multiStreamData</tt> to the video stream that we are using
     * in this media handler (if any) so that the underlying SRTP lib could
     * properly handle stream security.
     *
     * @param master the data that we are supposed to pass to our
     * video stream.
     */
    public void startSrtpMultistream(SrtpControl master)
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);

        if (videoStream != null)
            videoStream.getSrtpControl().setMultistream(master);
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
        return mediaHandler.getRemoteSSRC(this, MediaType.AUDIO);
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
        videoNotifierSupport.addVideoListener(listener);
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
        return
            videoNotifierSupport.fireVideoEvent(
                    type, visualComponent, origin,
                    true);
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
        videoNotifierSupport.fireVideoEvent(event, true);
    }

    /**
     * Gets local visual <tt>Component</tt> of the local peer.
     *
     * @return visual <tt>Component</tt>
     */
    public Component createLocalVisualComponent()
    {
        boolean flipLocalVideoDisplay = true;
        OperationSetDesktopSharingServer desktopOpSet
            = peer.getCall().getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingServer.class);
        // If the call video is a desktop sharing stream, then do not flip the
        // local video display.
        if (desktopOpSet != null
            && desktopOpSet.isLocalVideoAllowed(peer.getCall()))
        {
            flipLocalVideoDisplay = false;
        }

        MediaStream videoStream = getStream(MediaType.VIDEO);

        return
            ((videoStream == null) || !isLocalVideoTransmissionEnabled())
                ? null
                : ((VideoMediaStream) videoStream).createLocalVisualComponent(
                        flipLocalVideoDisplay);
    }

    /**
     * Disposes of a specific local visual <tt>Component</tt> of the local peer.
     *
     * @param component the local visual <tt>Component</tt> of the local peer to
     * dispose of
     */
    public void disposeLocalVisualComponent(Component component)
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);

        if (videoStream != null)
        {
            ((VideoMediaStream) videoStream).disposeLocalVisualComponent(
                    component);
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
    @Deprecated
    public Component getVisualComponent()
    {
        List<Component> visualComponents = getVisualComponents();

        return visualComponents.isEmpty() ? null : visualComponents.get(0);
    }

    /**
     * Gets the visual <tt>Component</tt>s in which videos from the remote peer
     * are currently being rendered.
     *
     * @return the visual <tt>Component</tt>s in which videos from the remote
     * peer are currently being rendered
     */
    public List<Component> getVisualComponents()
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);
        List<Component> visualComponents;

        if (videoStream == null)
            visualComponents = Collections.emptyList();
        else
        {
            visualComponents
                = ((VideoMediaStream) videoStream).getVisualComponents();
        }
        return visualComponents;
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
        videoNotifierSupport.removeVideoListener(listener);
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

            MediaStream audioStream = getStream(MediaType.AUDIO);

            if (audioStream != null)
            {
                ((AudioMediaStream) audioStream).setLocalUserAudioLevelListener(
                        listener);
            }
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

            MediaStream audioStream = getStream(MediaType.AUDIO);

            if (audioStream != null)
            {
                ((AudioMediaStream) audioStream).setStreamAudioLevelListener(
                        listener);
            }
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

            MediaStream audioStream = getStream(MediaType.AUDIO);

            if (audioStream != null)
            {
                ((AudioMediaStream) audioStream).setCsrcAudioLevelListener(
                        csrcAudioLevelListener);
            }
        }
    }

    /**
     * Gets the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance.
     *
     * @return the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance
     */
    protected Map<MediaTypeSrtpControl, SrtpControl> getSrtpControls()
    {
        return mediaHandler.getSrtpControls(this);
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
     * @param masterStream whether the stream to be used as master if secured
     *
     * @return the newly created <tt>MediaStream</tt>.
     *
     * @throws OperationFailedException if creating the stream fails for any
     * reason (like, for example, accessing the device or setting the format).
     */
    protected MediaStream initStream(StreamConnector      connector,
                                     MediaDevice          device,
                                     MediaFormat          format,
                                     MediaStreamTarget    target,
                                     MediaDirection       direction,
                                     List<RTPExtension>   rtpExtensions,
                                     boolean masterStream)
        throws OperationFailedException
    {
        MediaStream stream
            = mediaHandler.initStream(
                    this,
                    connector,
                    device,
                    format,
                    target,
                    direction,
                    rtpExtensions,
                    masterStream);

        switch (device.getMediaType())
        {
        case AUDIO:
            audioStream = (AudioMediaStream) stream;
            break;
        case VIDEO:
            videoStream = (VideoMediaStream) stream;
            break;
        }

        return stream;
    }

    /**
     * Requests a key frame from the remote peer of the associated
     * <tt>VideoMediaStream</tt> of this <tt>CallPeerMediaHandler</tt>. The
     * default implementation provided by <tt>CallPeerMediaHandler</tt> always
     * returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if this <tt>CallPeerMediaHandler</tt> has indeed
     * requested a key frame from the remote peer of its associated
     * <tt>VideoMediaStream</tt> in response to the call; otherwise,
     * <tt>false</tt>
     */
    protected boolean requestKeyFrame()
    {
        return false;
    }

    /**
     * Sends empty UDP packets to target destination data/control ports in order
     * to open port on NAT or RTP proxy if any. In order to be really efficient,
     * this method should be called after we send our offer or answer.
     *
     * @param target <tt>MediaStreamTarget</tt>
     */
    protected void sendHolePunchPacket(MediaStreamTarget target)
    {
        getTransportManager().sendHolePunchPacket(target, MediaType.VIDEO);
    }

    /**
     * Processes a request for a (video) key frame from the remote peer to the
     * local peer.
     *
     * @return <tt>true</tt> if the request for a (video) key frame has been
     * honored by the local peer; otherwise, <tt>false</tt>
     */
    public boolean processKeyFrameRequest()
    {
        return mediaHandler.processKeyFrameRequest(this);
    }

    /**
     * Notifies this instance that a value of a specific property of the
     * <tt>Call</tt> of {@link #peer} has changed from a specific old value to a
     * specific new value.
     *
     * @param event a <tt>PropertyChangeEvent</tt> which specified the property
     * which had its value changed and the old and new values of that property
     */
    private void callPropertyChange(PropertyChangeEvent event)
    {
        if (MediaAwareCall.DEFAULT_DEVICE.equals(event.getPropertyName()))
        {
            /*
             * XXX We support changing the default audio device only at the time
             * of this writing.
             */
            MediaStream stream = getStream(MediaType.AUDIO);
            MediaDevice oldValue = stream.getDevice();

            if (oldValue != null)
            {
                MediaDevice newValue = getDefaultDevice(MediaType.AUDIO);

                if (oldValue != newValue)
                {
                    stream.setDevice(newValue);
                    registerAudioLevelListeners((AudioMediaStream) stream);
                }
            }
        }
    }

    /**
     * Registers all audio level listeners currently known to this media handler
     * with the specified <tt>audioStream</tt>.
     *
     * @param audioStream the <tt>AudioMediaStream</tt> that we'd like to
     * register our audio level listeners with.
     */
    void registerAudioLevelListeners(AudioMediaStream audioStream)
    {
        synchronized (localAudioLevelListenerLock)
        {
            if (localAudioLevelListener != null)
                audioStream.setLocalUserAudioLevelListener(
                        localAudioLevelListener);
        }
        synchronized (streamAudioLevelListenerLock)
        {
            if (streamAudioLevelListener != null)
                audioStream.setStreamAudioLevelListener(
                        streamAudioLevelListener);
        }
        synchronized (csrcAudioLevelListenerLock)
        {
            if (csrcAudioLevelListener != null)
                audioStream.setCsrcAudioLevelListener(csrcAudioLevelListener);
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
        for (MediaType mediaType : MediaType.values())
        {
            MediaStream stream = getStream(mediaType);

            if ((stream != null) && stream.getDirection().allowsSending())
                return false;
        }
        return true;
    }

    /**
     * Compares a list of <tt>MediaFormat</tt>s offered by a remote party
     * to the list of locally supported <tt>RTPExtension</tt>s as returned
     * by one of our local <tt>MediaDevice</tt>s and returns a third
     * <tt>List</tt> that contains their intersection.
     *
     * @param remoteFormats remote <tt>MediaFormat</tt> found in the
     * SDP message
     * @param localFormats local supported <tt>MediaFormat</tt> of our device
     * @return intersection between our local and remote <tt>MediaFormat</tt>
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
    protected MediaFormat findMediaFormat(
            List<MediaFormat> formats, MediaFormat format)
    {
        MediaType mediaType = format.getMediaType();
        String encoding = format.getEncoding();
        double clockRate = format.getClockRate();
        int channels
            = MediaType.AUDIO.equals(mediaType)
                ? ((AudioMediaFormat) format).getChannels()
                : MediaFormatFactory.CHANNELS_NOT_SPECIFIED;
        Map<String, String> formatParameters = format.getFormatParameters();

        for(MediaFormat match : formats)
        {
            if (AbstractMediaStream.matches(
                        match,
                        mediaType,
                        encoding, clockRate, channels, formatParameters))
                return match;
        }
        return null;
    }

    /**
     * Compares a list of <tt>RTPExtension</tt>s offered by a remote party
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
        return getDefaultDevice(type).getSupportedExtensions();
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
     * Starts this <tt>CallPeerMediaHandler</tt>. If it has already been
     * started, does nothing.
     *
     * @throws IllegalStateException if this method is called without this
     * handler having first seen a media description or having generated an
     * offer.
     */
    public void start()
        throws IllegalStateException
    {
        MediaStream stream;

        stream = getStream(MediaType.AUDIO);
        if ((stream != null)
                && !stream.isStarted()
                && isLocalAudioTransmissionEnabled())
        {
            getTransportManager().setTrafficClass(
                    stream.getTarget(),
                    MediaType.AUDIO);
            stream.start();
        }

        stream = getStream(MediaType.VIDEO);
        if (stream != null)
        {
            /*
             * Inform listener of LOCAL_VIDEO_STREAMING only once the video
             * starts so that VideoMediaDeviceSession has correct MediaDevice
             * set (switch from desktop streaming to webcam video or vice-versa
             * issue)
             */
            firePropertyChange(
                    OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING,
                    null, this.videoDirectionUserPreference);

            if(!stream.isStarted())
            {
                getTransportManager().setTrafficClass(
                        stream.getTarget(),
                        MediaType.VIDEO);
                stream.start();

                // send empty packet to deblock some kind of RTP proxy to let
                // just one user sends its video
                if ((stream instanceof VideoMediaStream)
                        /* && !isLocalVideoTransmissionEnabled() */)
                {
                    sendHolePunchPacket(stream.getTarget());
                }
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
     * Gets the <tt>TransportManager</tt> implementation handling our address
     * management.
     *
     * @return the <tt>TransportManager</tt> implementation handling our address
     * management
     */
    protected abstract TransportManager<T> getTransportManager();

    /**
     * Represents the <tt>PropertyChangeListener</tt> which listens to changes
     * in the values of the properties of the <tt>Call</tt> of {@link #peer}.
     * Remembers the <tt>Call</tt> it has been added to because <tt>peer</tt>
     * does not have a <tt>call</tt> anymore at the time {@link #close()} is
     * called.
     */
    private class CallPropertyChangeListener
        implements PropertyChangeListener
    {
        /**
         * The <tt>Call</tt> this <tt>PropertyChangeListener</tt> will be or is
         * already added to.
         */
        public final MediaAwareCall<?, ?, ?> call;

        /**
         * Initializes a new <tt>CallPropertyChangeListener</tt> which is to be
         * added to a specific <tt>Call</tt>.
         *
         * @param call the <tt>Call</tt> the new instance is to be added to
         */
        public CallPropertyChangeListener(MediaAwareCall<?, ?, ?> call)
        {
            this.call = call;
        }

        /**
         * Notifies this instance that the value of a specific property of
         * {@link #call} has changed from a specific old value to a specific
         * new value.
         *
         * @param event a <tt>PropertyChangeEvent</tt> which specifies the name
         * of the property which had its value changed and the old and new
         * values
         */
        public void propertyChange(PropertyChangeEvent event)
        {
            callPropertyChange(event);
        }
    }

    /**
     * Gets the SRTP control type used for a given media type.
     *
     * @param mediaType the <tt>MediaType</tt> to get the SRTP control type for
     * @return the SRTP control type (MIKEY, SDES, ZRTP) used for the given
     * media type or <tt>null</tt> if SRTP is not enabled for the given media
     * type
     */
    public SrtpControl getEncryptionMethod(MediaType mediaType)
    {
        return mediaHandler.getEncryptionMethod(this, mediaType);
    }

    /**
     * Returns the extended type of the candidate selected if this transport
     * manager is using ICE.
     *
     * @return The extended type of the candidate selected if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public String getICECandidateExtendedType()
    {
        TransportManager<?> transportManager = getTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICECandidateExtendedType();
    }

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public String getICEState()
    {
        TransportManager<?> transportManager = getTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICEState();
    }

    public MediaHandler getMediaHandler()
    {
        return mediaHandler;
    }

    public void setMediaHandler(MediaHandler mediaHandler)
    {
        if (this.mediaHandler != mediaHandler)
        {
            if (this.mediaHandler != null)
            {
                this.mediaHandler.removeKeyFrameRequester(keyFrameRequester);
                this.mediaHandler.removePropertyChangeListener(
                        streamPropertyChangeListener);
                if (srtpListener != null)
                    this.mediaHandler.removeSrtpListener(srtpListener);
                this.mediaHandler.removeVideoListener(videoStreamVideoListener);
            }

            this.mediaHandler = mediaHandler;

            if (this.mediaHandler != null)
            {
                this.mediaHandler.addKeyFrameRequester(-1, keyFrameRequester);
                this.mediaHandler.addPropertyChangeListener(
                        streamPropertyChangeListener);
                if (srtpListener != null)
                    this.mediaHandler.addSrtpListener(srtpListener);
                this.mediaHandler.addVideoListener(videoStreamVideoListener);
            }
        }
    }

    /**
     * Adds a specific <tt>VisualComponentResolveListener</tt> to this telephony
     * in order to receive notifications when visual/video <tt>Component</tt>s
     * are being resolved to correspond to a particular
     * <tt>ConferenceMember</tt>.
     *
     * @param listener the <tt>VisualComponentResolveListener</tt> to be
     * notified when visual/video <tt>Component</tt>s are being resolved to
     * correspond to a particular <tt>ConferenceMember</tt>.
     */
    public void addVisualComponentResolveListener(
        VisualComponentResolveListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (visualComponentResolveSyncRoot)
        {
            if ((visualComponentResolveListeners == null)
                    || visualComponentResolveListeners.isEmpty())
            {
                visualComponentResolveListeners
                    = new ArrayList<VisualComponentResolveListener>();
            }

            visualComponentResolveListeners.add(listener);
        }
    }

    /**
     * Removes a <tt>VisualComponentResolveListener</tt> from this video
     * telephony operation set, which was previously added in order to receive
     * notifications when visual/video <tt>Component</tt>s are being resolved to
     * be corresponding to a particular <tt>ConferenceMember</tt>.
     *
     * @param listener the <tt>VisualComponentResolveListener</tt> to be
     * removed
     */
    public void removeVisualComponentResolveListener(
        VisualComponentResolveListener listener)
    {
        synchronized (visualComponentResolveSyncRoot)
        {
            if ((visualComponentResolveListeners != null)
                    && !visualComponentResolveListeners.isEmpty())
            {
                visualComponentResolveListeners.remove(listener);
            }
        }
    }

    /**
     * Notifies all registered <tt>VisualComponentResolveListener</tt> that a
     * visual <tt>Component</tt> has been resolved.
     *
     * @param visualComponent the visual <tt>Component</tt> that has been
     * resolved
     * @param conferenceMember the resolved <tt>ConferenceMember</tt>
     */
    public void fireVisualComponentResolveEvent(
                                            Component visualComponent,
                                            ConferenceMember conferenceMember)
    {
        List<VisualComponentResolveListener> visualComponentResolveListeners;

        synchronized (visualComponentResolveSyncRoot)
        {
            /*
            * Since the localUserAudioLevelListeners field of this
            * MediaAwareCall is implemented as a copy-on-write storage, just
            * get a reference to it and it should be safe to iterate over it
            * without ConcurrentModificationExceptions.
            */
            visualComponentResolveListeners
            = this.visualComponentResolveListeners;
        }

        if (visualComponentResolveListeners != null)
        {
        /*
        * Iterate over localUserAudioLevelListeners using an index rather
        * than an Iterator in order to try to reduce the number of
        * allocations (as the number of audio level changes is expected to
        * be very large).
        */
        int visualComponentResolveListenerCount
        = visualComponentResolveListeners.size();

        for(int i = 0; i < visualComponentResolveListenerCount; i++)
        visualComponentResolveListeners.get(i).visualComponentResolved(
        new VisualComponentResolveEvent(this,
                        visualComponent,
                        conferenceMember));
        }
    }
}
