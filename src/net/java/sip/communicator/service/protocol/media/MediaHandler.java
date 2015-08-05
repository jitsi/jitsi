/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.media;

import java.awt.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.event.DTMFListener;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.control.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.service.protocol.*;
import org.jitsi.util.event.*;

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
     * The <tt>CsrcAudioLevelListener</tt> that this instance sets on its
     * {@link #audioStream} if {@link #csrcAudioLevelListeners} is not empty.
     */
    private final CsrcAudioLevelListener csrcAudioLevelListener
        = new CsrcAudioLevelListener()
        {
            public void audioLevelsReceived(long[] audioLevels)
            {
                MediaHandler.this.audioLevelsReceived(audioLevels);
            }
        };

    /**
     * The <tt>Object</tt> which synchronizes the access to
     * {@link #csrcAudioLevelListener} and {@link #csrcAudioLevelListeners}.
     */
    private final Object csrcAudioLevelListenerLock = new Object();

    /**
     * The list of <tt>CsrcAudioLevelListener</tt>s to be notified about audio
     * level-related information received from the remote peer(s).
     */
    private List<CsrcAudioLevelListener> csrcAudioLevelListeners
        = Collections.emptyList();

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
     * The <tt>SimpleAudioLeveListener</tt> that this instance sets on its
     * {@link #audioStream} if {@link #localUserAudioLevelListeners} is not
     * empty in order to listen to changes in the levels of the audio sent from
     * the local user/peer to the remote peer(s).
     */
    private final SimpleAudioLevelListener localUserAudioLevelListener
        = new SimpleAudioLevelListener()
        {
            public void audioLevelChanged(int level)
            {
                MediaHandler.this.audioLevelChanged(
                        localUserAudioLevelListenerLock,
                        localUserAudioLevelListeners,
                        level);
            }
        };

    /**
     * The <tt>Object</tt> which synchronizes the access to
     * {@link #localUserAudioLevelListener} and
     * {@link #localUserAudioLevelListeners}.
     */
    private final Object localUserAudioLevelListenerLock = new Object();

    /**
     * The list of <tt>SimpleAudioLevelListener</tt>s to be notified about
     * changes in the level of the audio sent from the local peer/user to the
     * remote peer(s).
     */
    private List<SimpleAudioLevelListener> localUserAudioLevelListeners
        = Collections.emptyList();

    /**
     * The last-known remote SSRCs of the <tt>MediaStream</tt>s of this instance
     * indexed by <tt>MediaType</tt> ordinal.
     */
    private final long[] remoteSSRCs;

    /**
     * The <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this instance.
     */
    private final SrtpControls srtpControls
        = new SrtpControls();

    private final SrtpListener srtpListener
        = new SrtpListener()
        {
            public void securityMessageReceived(
                    String message, String i18nMessage, int severity)
            {
                for (SrtpListener listener : getSrtpListeners())
                {
                    listener.securityMessageReceived(
                            message, i18nMessage, severity);
                }
            }

            public void securityNegotiationStarted(
                    MediaType mediaType, SrtpControl sender)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityNegotiationStarted(mediaType, sender);
            }

            public void securityTimeout(MediaType mediaType)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityTimeout(mediaType);
            }

            public void securityTurnedOff(MediaType mediaType)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityTurnedOff(mediaType);
            }

            public void securityTurnedOn(
                    MediaType mediaType, String cipher, SrtpControl sender)
            {
                for (SrtpListener listener : getSrtpListeners())
                    listener.securityTurnedOn(mediaType, cipher, sender);
            }
        };

    private final List<SrtpListener> srtpListeners
        = new LinkedList<SrtpListener>();

    /**
     * The set of listeners in the application (<tt>Jitsi</tt>) which are to
     * be notified of DTMF events.
     */
    private final Set<DTMFListener> dtmfListeners
        = new HashSet<DTMFListener>();

    /**
     * The listener registered to receive DTMF events from {@link #audioStream}.
     */
    private final MyDTMFListener dtmfListener = new MyDTMFListener();

    /**
     * The <tt>SimpleAudioLeveListener</tt> that this instance sets on its
     * {@link #audioStream} if {@link #streamAudioLevelListeners} is not empty
     * in order to listen to changes in the levels of the audio received from
     * the remote peer(s) to the local user/peer.
     */
    private final SimpleAudioLevelListener streamAudioLevelListener
        = new SimpleAudioLevelListener()
        {
            public void audioLevelChanged(int level)
            {
                MediaHandler.this.audioLevelChanged(
                        streamAudioLevelListenerLock,
                        streamAudioLevelListeners,
                        level);
            }
        };

    /**
     * The <tt>Object</tt> which synchronizes the access to
     * {@link #streamAudioLevelListener} and {@link #streamAudioLevelListeners}.
     */
    private final Object streamAudioLevelListenerLock = new Object();

    /**
     * The list of <tt>SimpleAudioLevelListener</tt>s to be notified about
     * changes in the level of the audio sent from remote peer(s) to the local
     * peer/user.
     */
    private List<SimpleAudioLevelListener> streamAudioLevelListeners
        = Collections.emptyList();

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
                {
                    setLocalSSRC(
                            MediaType.AUDIO,
                            audioStream.getLocalSourceID());
                }
                else if (source == videoStream)
                {
                    setLocalSSRC(
                            MediaType.VIDEO,
                            videoStream.getLocalSourceID());
                }
            }
            else if (MediaStream.PNAME_REMOTE_SSRC.equals(propertyName))
            {
                Object source = evt.getSource();

                if (source == audioStream)
                {
                    setRemoteSSRC(
                            MediaType.AUDIO,
                            audioStream.getRemoteSourceID());
                }
                else if (source == videoStream)
                {
                    setRemoteSSRC(
                            MediaType.VIDEO,
                            videoStream.getRemoteSourceID());
                }
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

    /**
     * Adds a specific <tt>CsrcAudioLevelListener</tt> to the list of
     * <tt>CsrcAudioLevelListener</tt>s to be notified about audio level-related
     * information received from the remote peer(s).
     *
     * @param listener the <tt>CsrcAudioLevelListener</tt> to add to the list of
     * <tt>CsrcAudioLevelListener</tt>s to be notified about audio level-related
     * information received from the remote peer(s)
     */
    void addCsrcAudioLevelListener(CsrcAudioLevelListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (csrcAudioLevelListenerLock)
        {
            if (!csrcAudioLevelListeners.contains(listener))
            {
                csrcAudioLevelListeners
                    = new ArrayList<CsrcAudioLevelListener>(
                            csrcAudioLevelListeners);
                if (csrcAudioLevelListeners.add(listener)
                        && (csrcAudioLevelListeners.size() == 1))
                {
                    AudioMediaStream audioStream = this.audioStream;

                    if (audioStream != null)
                    {
                        audioStream.setCsrcAudioLevelListener(
                                csrcAudioLevelListener);
                    }
                }
            }
        }
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

    /**
     * Adds a specific <tt>SimpleAudioLevelListener</tt> to the list of
     * <tt>SimpleAudioLevelListener</tt>s to be notified about changes in the
     * level of the audio sent from the local peer/user to the remote peer(s).
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add to the list
     * of <tt>SimpleAudioLevelListener</tt>s to be notified about changes in the
     * level of the audio sent from the local peer/user to the remote peer(s)
     */
    void addLocalUserAudioLevelListener(SimpleAudioLevelListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (localUserAudioLevelListenerLock)
        {
            if (!localUserAudioLevelListeners.contains(listener))
            {
                localUserAudioLevelListeners
                    = new ArrayList<SimpleAudioLevelListener>(
                            localUserAudioLevelListeners);
                if (localUserAudioLevelListeners.add(listener)
                        && (localUserAudioLevelListeners.size() == 1))
                {
                    AudioMediaStream audioStream = this.audioStream;

                    if (audioStream != null)
                    {
                        audioStream.setLocalUserAudioLevelListener(
                                localUserAudioLevelListener);
                    }
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
     * Adds a <tt>DTMFListener</tt> which will be notified when DTMF events
     * are received from the <tt>MediaHandler</tt>'s audio stream.
     * @param listener the listener to add.
     */
    void addDtmfListener(DTMFListener listener)
    {
        if (listener != null)
            dtmfListeners.add(listener);
    }

    /**
     * Removes a <tt>DTMFListener</tt> from the set of listeners to be notified
     * for DTMF events from this <tt>MediaHandler</tt>'s audio steam.
     * @param listener the listener to remove.
     */
    void removeDtmfListener(DTMFListener listener)
    {
        dtmfListeners.remove(listener);
    }

    /**
     * Adds a specific <tt>SimpleAudioLevelListener</tt> to the list of
     * <tt>SimpleAudioLevelListener</tt>s to be notified about changes in the
     * level of the audio sent from remote peer(s) to the local peer/user.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add to the list
     * of <tt>SimpleAudioLevelListener</tt>s to be notified about changes in the
     * level of the audio sent from the remote peer(s) to the local peer/user
     */
    void addStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (streamAudioLevelListenerLock)
        {
            if (!streamAudioLevelListeners.contains(listener))
            {
                streamAudioLevelListeners
                    = new ArrayList<SimpleAudioLevelListener>(
                            streamAudioLevelListeners);
                if (streamAudioLevelListeners.add(listener)
                        && (streamAudioLevelListeners.size() == 1))
                {
                    AudioMediaStream audioStream = this.audioStream;

                    if (audioStream != null)
                    {
                        audioStream.setStreamAudioLevelListener(
                                streamAudioLevelListener);
                    }
                }
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
     * Notifies this instance that a <tt>SimpleAudioLevelListener</tt> has been
     * invoked. Forwards the notification to a specific list of
     * <tt>SimpleAudioLevelListener</tt>s.
     *
     * @param lock the <tt>Object</tt> which is to be used to synchronize the
     * access to <tt>listeners</tt>.
     * @param listeners the list of <tt>SimpleAudioLevelListener</tt>s to
     * forward the notification to
     * @param level the value of the audio level to notify <tt>listeners</tt>
     * about
     */
    private void audioLevelChanged(
            Object lock,
            List<SimpleAudioLevelListener> listeners,
            int level)
    {
        List<SimpleAudioLevelListener> ls;

        synchronized (lock)
        {
            if (listeners.isEmpty())
                return;
            else
                ls = listeners;
        }
        for (int i = 0, count = ls.size();
                i < count;
                i++)
        {
            ls.get(i).audioLevelChanged(level);
        }
    }

    /**
     * Notifies this instance that audio level-related information has been
     * received from the remote peer(s). The method forwards the notification to
     * {@link #csrcAudioLevelListeners}.
     *
     * @param audioLevels the audio level-related information received from the
     * remote peer(s)
     */
    private void audioLevelsReceived(long[] audioLevels)
    {
        List<CsrcAudioLevelListener> listeners;

        synchronized (csrcAudioLevelListenerLock)
        {
            if (csrcAudioLevelListeners.isEmpty())
                return;
            else
                listeners = csrcAudioLevelListeners;
        }
        for (int i = 0, count = listeners.size();
                i < count;
                i++)
        {
            listeners.get(i).audioLevelsReceived(audioLevels);
        }
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
        callPeerMediaHandler.removeAndCleanupOtherSrtpControls(mediaType, null);
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

        MediaAwareCall<?,?,?> call = callPeerMediaHandler.getPeer().getCall();
        MediaType mediaType
            = (stream instanceof AudioMediaStream)
                ? MediaType.AUDIO
                : MediaType.VIDEO;

        stream.setRTPTranslator(call.getRTPTranslator(mediaType));

        switch (mediaType)
        {
        case AUDIO:
            AudioMediaStream audioStream = (AudioMediaStream) stream;

            /*
             * The volume (level) of the audio played back in calls should be
             * call-specific i.e. it should be able to change the volume (level)
             * of a call without affecting any other simultaneous calls.
             */
            setOutputVolumeControl(audioStream, call);

            setAudioStream(audioStream);
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
    SrtpControl getEncryptionMethod(
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
                = getSrtpControls(callPeerMediaHandler)
                    .get(mediaType, srtpControlType);

            if((srtpControl != null)
                    && srtpControl.getSecureCommunicationStatus())
            {
                return srtpControl;
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
    SrtpControls getSrtpControls(CallPeerMediaHandler<?> callPeerMediaHandler)
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
            SrtpControl srtpControl = srtpControls.findFirst(mediaType);

            // If a SrtpControl does not exist yet, create a default one.
            if (srtpControl == null)
            {
                /*
                 * The default SrtpControl is currently ZRTP without the
                 * hello-hash. It is created by the MediaStream implementation.
                 * Consequently, it needs to be linked to the srtpControls Map.
                 */
                stream = mediaService.createMediaStream(connector, device);
                srtpControl = stream.getSrtpControl();
                if (srtpControl != null)
                    srtpControls.set(mediaType, srtpControl);
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
            if (logger.isDebugEnabled())
                logger.debug("Reinitializing stream: " + stream);
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
     * Registers all dynamic payload mappings and any payload type overrides
     * that are known to this <tt>MediaHandler</tt> with the specified
     * <tt>MediaStream</tt>.
     *
     * @param stream the <tt>MediaStream</tt> that we'd like to register our
     * dynamic payload mappings with.
     */
    private void registerDynamicPTsWithStream(
            CallPeerMediaHandler<?> callPeerMediaHandler,
            MediaStream stream)
    {
        DynamicPayloadTypeRegistry dynamicPayloadTypes
                = callPeerMediaHandler.getDynamicPayloadTypes();

        StringBuffer dbgMessage = new StringBuffer("Dynamic PT map: ");

        //first register the mappings
        for (Map.Entry<MediaFormat, Byte> mapEntry
                : dynamicPayloadTypes.getMappings().entrySet())
        {
            byte pt = mapEntry.getValue();
            MediaFormat fmt = mapEntry.getKey();

            dbgMessage.append(pt).append("=").append(fmt).append("; ");
            stream.addDynamicRTPPayloadType(pt, fmt);
        }
        logger.info(dbgMessage);

        dbgMessage = new StringBuffer("PT overrides [");
        //now register whatever overrides we have for the above mappings
        for (Map.Entry<Byte, Byte> overrideEntry
                : dynamicPayloadTypes.getMappingOverrides().entrySet())
        {
            byte originalPt = overrideEntry.getKey();
            byte overridePt = overrideEntry.getValue();


            dbgMessage.append(originalPt).append("->")
                        .append(overridePt).append(" ");
            stream.addDynamicRTPPayloadTypeOverride(originalPt, overridePt);
        }

        dbgMessage.append("]");
        logger.info(dbgMessage);


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

    /**
     * Removes a specific <tt>CsrcAudioLevelListener</tt> to the list of
     * <tt>CsrcAudioLevelListener</tt>s to be notified about audio level-related
     * information received from the remote peer(s).
     *
     * @param listener the <tt>CsrcAudioLevelListener</tt> to remove from the
     * list of <tt>CsrcAudioLevelListener</tt>s to be notified about audio
     * level-related information received from the remote peer(s)
     */
    void removeCsrcAudioLevelListener(CsrcAudioLevelListener listener)
    {
        if (listener == null)
            return;

        synchronized (csrcAudioLevelListenerLock)
        {
            if (csrcAudioLevelListeners.contains(listener))
            {
                csrcAudioLevelListeners
                    = new ArrayList<CsrcAudioLevelListener>(
                            csrcAudioLevelListeners);
                if (csrcAudioLevelListeners.remove(listener)
                        && csrcAudioLevelListeners.isEmpty())
                {
                    AudioMediaStream audioStream = this.audioStream;

                    if (audioStream != null)
                        audioStream.setCsrcAudioLevelListener(null);
                }
            }
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

    /**
     * Removes a specific <tt>SimpleAudioLevelListener</tt> to the list of
     * <tt>SimpleAudioLevelListener</tt>s to be notified about changes in the
     * level of the audio sent from the local peer/user to the remote peer(s).
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to remove from the
     * list of <tt>SimpleAudioLevelListener</tt>s to be notified about changes
     * in the level of the audio sent from the local peer/user to the remote
     * peer(s)
     */
    void removeLocalUserAudioLevelListener(SimpleAudioLevelListener listener)
    {
        if (listener == null)
            return;

        synchronized (localUserAudioLevelListenerLock)
        {
            if (localUserAudioLevelListeners.contains(listener))
            {
                localUserAudioLevelListeners
                    = new ArrayList<SimpleAudioLevelListener>(
                            localUserAudioLevelListeners);
                if (localUserAudioLevelListeners.remove(listener)
                        && localUserAudioLevelListeners.isEmpty())
                {
                    AudioMediaStream audioStream = this.audioStream;

                    if (audioStream != null)
                        audioStream.setLocalUserAudioLevelListener(null);
                }
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
     * Removes a specific <tt>SimpleAudioLevelListener</tt> to the list of
     * <tt>SimpleAudioLevelListener</tt>s to be notified about changes in the
     * level of the audio sent from remote peer(s) to the local peer/user.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to remote from the
     * list of <tt>SimpleAudioLevelListener</tt>s to be notified about changes
     * in the level of the audio sent from the remote peer(s) to the local
     * peer/user
     */
    void removeStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        if (listener == null)
            return;

        synchronized (streamAudioLevelListenerLock)
        {
            if (streamAudioLevelListeners.contains(listener))
            {
                streamAudioLevelListeners
                    = new ArrayList<SimpleAudioLevelListener>(
                            streamAudioLevelListeners);
                if (streamAudioLevelListeners.remove(listener)
                        && streamAudioLevelListeners.isEmpty())
                {
                    AudioMediaStream audioStream = this.audioStream;

                    if (audioStream != null)
                        audioStream.setStreamAudioLevelListener(null);
                }
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
                synchronized (csrcAudioLevelListenerLock)
                {
                    if (!csrcAudioLevelListeners.isEmpty())
                        this.audioStream.setCsrcAudioLevelListener(null);
                }
                synchronized (localUserAudioLevelListenerLock)
                {
                    if (!localUserAudioLevelListeners.isEmpty())
                        this.audioStream.setLocalUserAudioLevelListener(null);
                }
                synchronized (streamAudioLevelListenerLock)
                {
                    if (!streamAudioLevelListeners.isEmpty())
                        this.audioStream.setStreamAudioLevelListener(null);
                }

                this.audioStream.removePropertyChangeListener(
                        streamPropertyChangeListener);
                this.audioStream.removeDTMFListener(dtmfListener);

                this.audioStream.close();
            }

            this.audioStream = audioStream;

            long audioLocalSSRC;
            long audioRemoteSSRC;

            if (this.audioStream != null)
            {
                this.audioStream.addPropertyChangeListener(
                        streamPropertyChangeListener);
                audioLocalSSRC = this.audioStream.getLocalSourceID();
                audioRemoteSSRC = this.audioStream.getRemoteSourceID();

                synchronized (csrcAudioLevelListenerLock)
                {
                    if (!csrcAudioLevelListeners.isEmpty())
                    {
                        this.audioStream.setCsrcAudioLevelListener(
                                csrcAudioLevelListener);
                    }
                }
                synchronized (localUserAudioLevelListenerLock)
                {
                    if (!localUserAudioLevelListeners.isEmpty())
                    {
                        this.audioStream.setLocalUserAudioLevelListener(
                                localUserAudioLevelListener);
                    }
                }
                synchronized (streamAudioLevelListenerLock)
                {
                    if (!streamAudioLevelListeners.isEmpty())
                    {
                        this.audioStream.setStreamAudioLevelListener(
                                streamAudioLevelListener);
                    }
                }

                this.audioStream.addDTMFListener(dtmfListener);
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
     * Sets the <tt>VolumeControl</tt> which is to control the volume (level) of
     * the audio received in/by a specific <tt>AudioMediaStream</tt> and played
     * back in order to achieve call-specific volume (level).
     * <p>
     * <b>Note</b>: The implementation makes the volume (level) telephony
     * conference-specific.
     * </p>
     *
     * @param audioStream the <tt>AudioMediaStream</tt> on which a
     * <tt>VolumeControl</tt> from the specified <tt>call</tt> is to be set
     * @param call the <tt>MediaAwareCall</tt> which provides the
     * <tt>VolumeControl</tt> to be set on the specified <tt>audioStream</tt>
     */
    private void setOutputVolumeControl(
            AudioMediaStream audioStream,
            MediaAwareCall<?,?,?> call)
    {
        /*
         * The volume (level) of the audio played back in calls should be
         * call-specific i.e. it should be able to change the volume (level) of
         * a call without affecting any other simultaneous calls. The
         * implementation makes the volume (level) telephony
         * conference-specific.
         */
        MediaAwareCallConference conference = call.getConference();

        if (conference != null)
        {
            VolumeControl outputVolumeControl
                = conference.getOutputVolumeControl();

            if (outputVolumeControl != null)
                audioStream.setOutputVolumeControl(outputVolumeControl);
        }
    }

    /**
     * Sets the last-known remote SSRC of the <tt>MediaStream</tt> of a specific
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> to
     * set the last-known remote SSRC of
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

    /**
     * Implements a <tt>libjitsi</tt> <tt>DTMFListener</tt>, which receives
     * events from an <tt>AudioMediaStream</tt>, translate them into
     * <tt>Jitsi</tt> events (<tt>DTMFReceivedEvent</tt>s) and forward them to
     * any registered listeners.
     */
    private class MyDTMFListener
        implements org.jitsi.service.neomedia.event.DTMFListener
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void dtmfToneReceptionStarted(DTMFToneEvent dtmfToneEvent)
        {
            fireEvent(
                new DTMFReceivedEvent(
                    this,
                    DTMFTone.getDTMFTone(dtmfToneEvent.getDtmfTone().getValue()),
                    true));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dtmfToneReceptionEnded(DTMFToneEvent dtmfToneEvent)
        {
            fireEvent(
                    new DTMFReceivedEvent(
                            this,
                            DTMFTone.getDTMFTone(dtmfToneEvent.getDtmfTone().getValue()),
                            false));
        }

        /**
         * Sends an <tt>DTMFReceivedEvent</tt> to all listeners.
         * @param event the event to send.
         */
        private void fireEvent(DTMFReceivedEvent event)
        {
            for (net.java.sip.communicator.service.protocol.event.DTMFListener
                    listener : dtmfListeners)
            {
                listener.toneReceived(event);
            }
        }
    }
}
