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

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.control.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.event.*;

/**
 * Implements media control code which allows state sharing among multiple
 * <tt>CallPeerMediaHandler</tt>s.
 *
 * @author Lyubomir Marinov
 */
public class MediaHandler
    extends PropertyChangeNotifier
{
    /**
     * The <tt>Logger</tt> used by the <tt>MediaHandler</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(MediaHandler.class);

    /**
     * The <tt>AudioMediaStream</tt> which this instance uses to send and
     * receive audio.
     */
    private AudioMediaStream audioStream;

    /**
     * The <tt>KeyFrameControl</tt> currently known to this
     * <tt>MediaHandler</tt> and made available by {@link #videoStream}.
     */
    private KeyFrameControl keyFrameControl;

    /**
     * The <tt>KeyFrameRequester</tt> implemented by this
     * <tt>MediaHandler</tt> and provided to {@link #keyFrameControl}.
     */
    private final KeyFrameControl.KeyFrameRequester keyFrameRequester
        = new KeyFrameControl.KeyFrameRequester()
        {
            public boolean requestKeyFrame()
            {
                return MediaHandler.this.requestKeyFrame();
            }
        };

    private final List<KeyFrameControl.KeyFrameRequester> keyFrameRequesters
        = new LinkedList<KeyFrameControl.KeyFrameRequester>();

    /**
     * The last-known local SSRCs of the <tt>MediaStream</tt>s of this instance
     * indexed by <tt>MediaType</tt> ordinal.
     */
    private final long[] localSSRCs;

    /**
     * The last-known remote SSRCs of the <tt>MediaStream</tt>s of this instance
     * indexed by <tt>MediaType</tt> ordinal.
     */
    private final long[] remoteSSRCs;

    /**
     * The <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this instance.
     */
    private final SortedMap<MediaTypeSrtpControl, SrtpControl> srtpControls
        = new TreeMap<MediaTypeSrtpControl, SrtpControl>();

    private final SrtpListener srtpListener
        = new SrtpListener()
        {
            public void securityMessageReceived(
                    String message, String i18nMessage, int severity)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityMessageReceived(
                            message, i18nMessage, severity);
            }

            public void securityTimeout(int sessionType)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityTimeout(sessionType);
            }

            public void securityTurnedOff(int sessionType)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityTurnedOff(sessionType);
            }

            public void securityTurnedOn(
                    int sessionType, String cipher, SrtpControl sender)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityTurnedOn(sessionType, cipher, sender);
            }

            public void securityNegotiationStarted(int sessionType,
                                                    SrtpControl sender)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityNegotiationStarted(sessionType, sender);
            }
        };

    private final List<SrtpListener> srtpListeners
        = new LinkedList<SrtpListener>();

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
            String propertyName = evt.getPropertyName();

            if (MediaStream.PNAME_LOCAL_SSRC.equals(propertyName))
            {
                Object source = evt.getSource();

                if (source == audioStream)
                    setLocalSSRC(
                            MediaType.AUDIO,
                            audioStream.getLocalSourceID());
                else if (source == videoStream)
                    setLocalSSRC(
                            MediaType.VIDEO,
                            videoStream.getLocalSourceID());
            }
            else if (MediaStream.PNAME_REMOTE_SSRC.equals(propertyName))
            {
                Object source = evt.getSource();

                if (source == audioStream)
                    setRemoteSSRC(
                            MediaType.AUDIO,
                            audioStream.getRemoteSourceID());
                else if (source == videoStream)
                    setRemoteSSRC(
                            MediaType.VIDEO,
                            videoStream.getRemoteSourceID());
            }
        }
    };

    /**
     * The number of references to the <tt>MediaStream</tt>s of this instance
     * returned by {@link #configureStream(CallPeerMediaHandler, MediaDevice,
     * MediaFormat, MediaStreamTarget, MediaDirection, List, MediaStream,
     * boolean)} to {@link CallPeerMediaHandler}s as new instances.
     */
    private final int[] streamReferenceCounts;

    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this, true);

    /**
     * The <tt>VideoMediaStream</tt> which this instance uses to send and
     * receive video.
     */
    private VideoMediaStream videoStream;

    /**
     * The <tt>VideoListener</tt> which listens to {@link #videoStream} for
     * changes in the availability of visual <tt>Component</tt>s displaying
     * remote video and re-fires them as originating from this instance.
     */
    private final VideoListener videoStreamVideoListener
        = new VideoListener()
    {
        public void videoAdded(VideoEvent event)
        {
            VideoEvent clone = event.clone(MediaHandler.this);

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

    public MediaHandler()
    {
        int mediaTypeValueCount = MediaType.values().length;

        localSSRCs = new long[mediaTypeValueCount];
        Arrays.fill(localSSRCs, CallPeerMediaHandler.SSRC_UNKNOWN);
        remoteSSRCs = new long[mediaTypeValueCount];
        Arrays.fill(remoteSSRCs, CallPeerMediaHandler.SSRC_UNKNOWN);

        streamReferenceCounts = new int[mediaTypeValueCount];
    }

    boolean addKeyFrameRequester(
            int index,
            KeyFrameControl.KeyFrameRequester keyFrameRequester)
    {
        if (keyFrameRequester == null)
            throw new NullPointerException("keyFrameRequester");
        else
        {
            synchronized (keyFrameRequesters)
            {
                if (keyFrameRequesters.contains(keyFrameRequester))
                    return false;
                else
                {
                    keyFrameRequesters.add(
                            (index == -1)
                                ? keyFrameRequesters.size()
                                : index,
                            keyFrameRequester);
                    return true;
                }
            }
        }
    }

    void addSrtpListener(SrtpListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");
        else
        {
            synchronized (srtpListeners)
            {
                if (!srtpListeners.contains(listener))
                    srtpListeners.add(listener);
            }
        }
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
    void addVideoListener(VideoListener listener)
    {
        videoNotifierSupport.addVideoListener(listener);
    }

    /**
     * Closes the <tt>MediaStream</tt> that this instance uses for a specific
     * <tt>MediaType</tt> and prepares it for garbage collection.
     *
     * @param mediaType the <tt>MediaType</tt> that we'd like to stop a stream
     * for.
     */
    protected void closeStream(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaType mediaType)
    {
        int index = mediaType.ordinal();
        int streamReferenceCount = streamReferenceCounts[index];

        /*
         * The streamReferenceCounts should not fall into an invalid state but
         * anyway...
         */
        if (streamReferenceCount <= 0)
            return;

        streamReferenceCount--;
        streamReferenceCounts[index] = streamReferenceCount;

        /*
         * The MediaStream of the specified mediaType is still referenced by
         * other CallPeerMediaHandlers so it is not to be closed yet.
         */
        if (streamReferenceCount > 0)
            return;

        switch (mediaType)
        {
        case AUDIO:
            setAudioStream(null);
            break;
        case VIDEO:
            setVideoStream(null);
            break;
        }

        // Clean up the SRTP controls used for the associated Call.
        Iterator<Map.Entry<MediaTypeSrtpControl, SrtpControl>> iter
            = srtpControls.entrySet().iterator();

        while (iter.hasNext())
        {
            Map.Entry<MediaTypeSrtpControl, SrtpControl> entry = iter.next();

            if (entry.getKey().mediaType == mediaType)
            {
                entry.getValue().cleanup();
                iter.remove();
            }
        }
    }

    /**
     * Configures <tt>stream</tt> to use the specified <tt>device</tt>,
     * <tt>format</tt>, <tt>target</tt>, <tt>direction</tt>, etc.
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
     * @param masterStream whether the stream to be used as master if secured
     *
     * @return the <tt>MediaStream</tt> that we received as a parameter (for
     * convenience reasons).
     *
     * @throws OperationFailedException if setting the <tt>MediaFormat</tt>
     * or connecting to the specified <tt>MediaDevice</tt> fails for some
     * reason.
     */
    protected MediaStream configureStream(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaDevice device,
            MediaFormat format,
            MediaStreamTarget target,
            MediaDirection direction,
            List<RTPExtension> rtpExtensions,
            MediaStream stream,
            boolean masterStream)
        throws OperationFailedException
    {
        registerDynamicPTsWithStream(callPeerMediaHandler, stream);
        registerRTPExtensionsWithStream(
                callPeerMediaHandler,
                rtpExtensions, stream);

        stream.setDevice(device);
        stream.setTarget(target);
        stream.setDirection(direction);
        stream.setFormat(format);

        MediaAwareCall<?, ?, ?> call = callPeerMediaHandler.getPeer().getCall();
        MediaType mediaType
            = (stream instanceof AudioMediaStream)
                ? MediaType.AUDIO
                : MediaType.VIDEO;

        stream.setRTPTranslator(call.getRTPTranslator(mediaType));

        switch (mediaType)
        {
        case AUDIO:
            setAudioStream((AudioMediaStream) stream);
            callPeerMediaHandler.registerAudioLevelListeners(audioStream);
            break;

        case VIDEO:
            setVideoStream((VideoMediaStream) stream);
            break;
        }

        if (call.isDefaultEncrypted())
        {
            /*
             * We'll use the audio stream as the master stream when using SRTP
             * multistreams.
             */
            SrtpControl srtpControl = stream.getSrtpControl();

            srtpControl.setMasterSession(masterStream);
            srtpControl.setSrtpListener(srtpListener);
            srtpControl.start(mediaType);
        }

        /*
         * If the specified callPeerMediaHandler is going to see the stream as
         * a new instance, count a new reference to it so that this MediaHandler
         * knows when it really needs to close the stream later on upon calls to
         * #closeStream(CallPeerMediaHandler<?>, MediaType).
         */
        if (stream != callPeerMediaHandler.getStream(mediaType))
            streamReferenceCounts[mediaType.ordinal()]++;

        return stream;
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>MediaHandler</tt> about a specific type of change in the availability
     * of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this <tt>MediaHandler</tt>
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
     * <tt>MediaHandler</tt> about a specific <tt>VideoEvent</tt>.
     *
     * @param event the <tt>VideoEvent</tt> to fire to the
     * <tt>VideoListener</tt>s registered with this <tt>MediaHandler</tt>
     */
    protected void fireVideoEvent(VideoEvent event)
    {
        videoNotifierSupport.fireVideoEvent(event, true);
    }

    /**
     * Gets the SRTP control type used for a given media type.
     *
     * @param mediaType the <tt>MediaType</tt> to get the SRTP control type for
     * @return the SRTP control type (MIKEY, SDES, ZRTP) used for the given
     * media type or <tt>null</tt> if SRTP is not enabled for the given media
     * type
     */
    SrtpControlType getEncryptionMethod(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaType mediaType)
    {
        /*
         * Find the first existing SRTP control type for the specified media
         * type which is active i.e. secures the communication.
         */
        for(SrtpControlType srtpControlType : SrtpControlType.values())
        {
            SrtpControl srtpControl
                = getSrtpControls(callPeerMediaHandler).get(
                        new MediaTypeSrtpControl(mediaType, srtpControlType));

            if((srtpControl != null)
                    && srtpControl.getSecureCommunicationStatus())
            {
                return srtpControlType;
            }
        }

        return null;
    }

    long getRemoteSSRC(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaType mediaType)
    {
        return remoteSSRCs[mediaType.ordinal()];
    }

    /**
     * Gets the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance.
     *
     * @return the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance
     */
    Map<MediaTypeSrtpControl, SrtpControl> getSrtpControls(
            CallPeerMediaHandler<?> callPeerMediaHandler)
    {
        return srtpControls;
    }

    private SrtpListener[] getSrtpListeners()
    {
        synchronized (srtpListeners)
        {
            return
                srtpListeners.toArray(new SrtpListener[srtpListeners.size()]);
        }
    }

    /**
     * Gets the <tt>MediaStream</tt> of this instance which is of a specific
     * <tt>MediaType</tt>. If this instance doesn't have such a
     * <tt>MediaStream</tt>, returns <tt>null</tt>
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> to
     * retrieve
     * @return the <tt>MediaStream</tt> of this <tt>CallPeerMediaHandler</tt>
     * which is of the specified <tt>mediaType</tt> if this instance has such a
     * <tt>MediaStream</tt>; otherwise, <tt>null</tt>
     */
    MediaStream getStream(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaType mediaType)
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
     * reason (like for example accessing the device or setting the format).
     */
    MediaStream initStream(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            StreamConnector connector,
            MediaDevice device,
            MediaFormat format,
            MediaStreamTarget target,
            MediaDirection direction,
            List<RTPExtension> rtpExtensions,
            boolean masterStream)
        throws OperationFailedException
    {
        MediaType mediaType = device.getMediaType();
        MediaStream stream = getStream(callPeerMediaHandler, mediaType);

        if (stream == null)
        {
            if (logger.isTraceEnabled() && (mediaType != format.getMediaType()))
                logger.trace("The media types of device and format differ.");

            MediaService mediaService
                = ProtocolMediaActivator.getMediaService();
            /*
             * The default SrtpControlType is ZRTP. But if a SrtpControl exists
             * already, it determines the SrtpControlType.
             */
            SrtpControlType srtpControlType
                = (srtpControls.size() > 0)
                    ? srtpControls.firstKey().srtpControlType
                    : SrtpControlType.ZRTP;
            MediaTypeSrtpControl mediaTypeSrtpControl
                = new MediaTypeSrtpControl(mediaType, srtpControlType);
            SrtpControl srtpControl = srtpControls.get(mediaTypeSrtpControl);

            // If a SrtpControl does not exist yet, create a default one.
            if (srtpControl == null)
            {
                /*
                 * The default SrtpControl is currently ZRTP without the
                 * hello-hash. It is created by the MediaStream implementation.
                 * Consequently, it needs to be linked to the srtpControls Map.
                 */
                stream = mediaService.createMediaStream(connector, device);
                srtpControls.put(mediaTypeSrtpControl, stream.getSrtpControl());
            }
            else
            {
                stream
                    = mediaService.createMediaStream(
                            connector,
                            device,
                            srtpControl);
            }
        }
        else
        {
            // this is a reinit
        }

        return
            configureStream(
                    callPeerMediaHandler,
                    device, format, target, direction, rtpExtensions, stream,
                    masterStream);
    }

    /**
     * Processes a request for a (video) key frame from a remote peer to the
     * local peer.
     *
     * @return <tt>true</tt> if the request for a (video) key frame has been
     * honored by the local peer; otherwise, <tt>false</tt>
     */
    boolean processKeyFrameRequest(CallPeerMediaHandler<?> callPeerMediaHandler)
    {
        KeyFrameControl keyFrameControl = this.keyFrameControl;

        return
            (keyFrameControl == null)
                ? null
                : keyFrameControl.keyFrameRequest();
    }

    /**
     * Registers all dynamic payload mappings known to this
     * <tt>MediaHandler</tt> with the specified <tt>MediaStream</tt>.
     *
     * @param stream the <tt>MediaStream</tt> that we'd like to register our
     * dynamic payload mappings with.
     */
    private void registerDynamicPTsWithStream(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaStream stream)
    {
        for (Map.Entry<MediaFormat, Byte> mapEntry
                : callPeerMediaHandler.getDynamicPayloadTypes().getMappings()
                        .entrySet())
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
            CallPeerMediaHandler<?> callPeerMediaHandler,
            List<RTPExtension> rtpExtensions,
            MediaStream stream)
    {
        DynamicRTPExtensionsRegistry rtpExtensionsRegistry
            = callPeerMediaHandler.getRtpExtensionsRegistry();

        for (RTPExtension rtpExtension : rtpExtensions)
        {
            byte extensionID
                = rtpExtensionsRegistry.getExtensionMapping(rtpExtension);

            stream.addRTPExtension(extensionID, rtpExtension);
        }
    }

    boolean removeKeyFrameRequester(
            KeyFrameControl.KeyFrameRequester keyFrameRequester)
    {
        if (keyFrameRequester == null)
            return false;
        else
        {
            synchronized (keyFrameRequesters)
            {
                return keyFrameRequesters.remove(keyFrameRequester);
            }
        }
    }

    void removeSrtpListener(SrtpListener listener)
    {
        if (listener != null)
        {
            synchronized (srtpListeners)
            {
                srtpListeners.remove(listener);
            }
        }
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
    void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
    }

    /**
     * Requests a key frame from the remote peer of the associated
     * <tt>VideoMediaStream</tt> of this <tt>MediaHandler</tt>.
     *
     * @return <tt>true</tt> if this <tt>MediaHandler</tt> has indeed requested
     * a key frame from the remote peer of its associated
     * <tt>VideoMediaStream</tt> in response to the call; otherwise,
     * <tt>false</tt>
     */
    protected boolean requestKeyFrame()
    {
        KeyFrameControl.KeyFrameRequester[] keyFrameRequesters;

        synchronized (this.keyFrameRequesters)
        {
            keyFrameRequesters
                = this.keyFrameRequesters.toArray(
                        new KeyFrameControl.KeyFrameRequester[
                                this.keyFrameRequesters.size()]);
        }

        for (KeyFrameControl.KeyFrameRequester keyFrameRequester
                : keyFrameRequesters)
        {
            if (keyFrameRequester.requestKeyFrame())
                return true;
        }
        return false;
    }

    /**
     * Sets the <tt>AudioMediaStream</tt> which this instance is to use to send
     * and receive audio.
     *
     * @param audioStream the <tt>AudioMediaStream</tt> which this instance is
     * to use to send and receive audio
     */
    private void setAudioStream(AudioMediaStream audioStream)
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
            {
                audioLocalSSRC
                    = audioRemoteSSRC
                        = CallPeerMediaHandler.SSRC_UNKNOWN;
            }

            setLocalSSRC(MediaType.AUDIO, audioLocalSSRC);
            setRemoteSSRC(MediaType.AUDIO, audioRemoteSSRC);
        }
    }

    /**
     * Sets the <tt>KeyFrameControl</tt> currently known to this
     * <tt>MediaHandler</tt> made available by a specific
     * <tt>VideoMediaStream</tt>.
     *
     * @param videoStream the <tt>VideoMediaStream</tt> the
     * <tt>KeyFrameControl</tt> of which is to be set as the currently known to
     * this <tt>MediaHandler</tt>
     */
    private void setKeyFrameControlFromVideoStream(VideoMediaStream videoStream)
    {
        KeyFrameControl keyFrameControl
            = (videoStream == null) ? null : videoStream.getKeyFrameControl();

        if (this.keyFrameControl != keyFrameControl)
        {
            if (this.keyFrameControl != null)
                this.keyFrameControl.removeKeyFrameRequester(keyFrameRequester);

            this.keyFrameControl = keyFrameControl;

            if (this.keyFrameControl != null)
                this.keyFrameControl.addKeyFrameRequester(-1, keyFrameRequester);
        }
    }

    /**
     * Sets the last-known local SSRC of the <tt>MediaStream</tt> of a specific
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> to
     * set the last-known local SSRC of
     * @param localSSRC the last-known local SSRC of the <tt>MediaStream</tt> of
     * the specified <tt>mediaType</tt>
     */
    private void setLocalSSRC(MediaType mediaType, long localSSRC)
    {
        int index = mediaType.ordinal();
        long oldValue = localSSRCs[index];

        if (oldValue != localSSRC)
        {
            localSSRCs[index] = localSSRC;

            String property;

            switch (mediaType)
            {
            case AUDIO:
                property = CallPeerMediaHandler.AUDIO_LOCAL_SSRC;
                break;
            case VIDEO:
                property = CallPeerMediaHandler.VIDEO_LOCAL_SSRC;
                break;
            default:
                property = null;
            }
            if (property != null)
                firePropertyChange(property, oldValue, localSSRC);
        }
    }

    /**
     * Sets the last-known local SSRC of the <tt>MediaStream</tt> of a specific
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> to
     * set the last-known local SSRC of
     * @param remoteSSRC the last-known remote SSRC of the <tt>MediaStream</tt>
     * of the specified <tt>mediaType</tt>
     */
    private void setRemoteSSRC(MediaType mediaType, long remoteSSRC)
    {
        int index = mediaType.ordinal();
        long oldValue = remoteSSRCs[index];

        if (oldValue != remoteSSRC)
        {
            remoteSSRCs[index] = remoteSSRC;

            String property;

            switch (mediaType)
            {
            case AUDIO:
                property = CallPeerMediaHandler.AUDIO_REMOTE_SSRC;
                break;
            case VIDEO:
                property = CallPeerMediaHandler.VIDEO_REMOTE_SSRC;
                break;
            default:
                property = null;
            }
            if (property != null)
                firePropertyChange(property, oldValue, remoteSSRC);
        }
    }

    /**
     * Sets the <tt>VideoMediaStream</tt> which this instance is to use to send
     * and receive video.
     *
     * @param videoStream the <tt>VideoMediaStream</tt> which this instance is
     * to use to send and receive video
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
            List<Component> oldVisualComponents = null;

            if (this.videoStream != null)
            {
                this.videoStream.removePropertyChangeListener(
                        streamPropertyChangeListener);

                this.videoStream.removeVideoListener(videoStreamVideoListener);
                oldVisualComponents = this.videoStream.getVisualComponents();

                /*
                 * The current videoStream is going away so this
                 * CallPeerMediaHandler should no longer use its
                 * KeyFrameControl.
                 */
                setKeyFrameControlFromVideoStream(null);

                this.videoStream.close();
            }

            this.videoStream = videoStream;

            /*
             * The videoStream has just changed so this CallPeerMediaHandler
             * should use its KeyFrameControl.
             */
            setKeyFrameControlFromVideoStream(this.videoStream);

            long videoLocalSSRC;
            long videoRemoteSSRC;
            /*
             * Make sure we will notify the registered VideoListeners about
             * changes in the availability of video in the new videoStream.
             */
            List<Component> newVisualComponents = null;

            if (this.videoStream != null)
            {
                this.videoStream.addPropertyChangeListener(
                        streamPropertyChangeListener);
                videoLocalSSRC = this.videoStream.getLocalSourceID();
                videoRemoteSSRC = this.videoStream.getRemoteSourceID();

                this.videoStream.addVideoListener(videoStreamVideoListener);
                newVisualComponents = this.videoStream.getVisualComponents();
            }
            else
            {
                videoLocalSSRC
                    = videoRemoteSSRC
                        = CallPeerMediaHandler.SSRC_UNKNOWN;
            }

            setLocalSSRC(MediaType.VIDEO, videoLocalSSRC);
            setRemoteSSRC(MediaType.VIDEO, videoRemoteSSRC);

            /*
             * Notify the VideoListeners in case there was a change in the
             * availability of the visual Components displaying remote video.
             */
            if ((oldVisualComponents != null) && !oldVisualComponents.isEmpty())
            {
                /*
                 * Discard Components which are present in the old and in the
                 * new Lists.
                 */
                if (newVisualComponents == null)
                    newVisualComponents = Collections.emptyList();
                for (Component oldVisualComponent : oldVisualComponents)
                {
                    if (!newVisualComponents.remove(oldVisualComponent))
                    {
                        fireVideoEvent(
                            VideoEvent.VIDEO_REMOVED,
                            oldVisualComponent,
                            VideoEvent.REMOTE);
                    }
                }
            }
            if ((newVisualComponents != null) && !newVisualComponents.isEmpty())
            {
                for (Component newVisualComponent : newVisualComponents)
                {
                    fireVideoEvent(
                        VideoEvent.VIDEO_ADDED,
                        newVisualComponent,
                        VideoEvent.REMOTE);
                }
            }
        }
    }
}
