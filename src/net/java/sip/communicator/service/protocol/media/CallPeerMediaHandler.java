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
import java.net.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.neomedia.control.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.util.event.*;

/**
 * A utility class implementing media control code shared between current
 * telephony implementations. This class is only meant for use by protocol
 * implementations and should not be accessed by bundles that are simply using
 * the telephony functionality.
 *
 * @param <T> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public abstract class CallPeerMediaHandler<T extends MediaAwareCallPeer<?,?,?>>
    extends PropertyChangeNotifier
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandler</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
                = Logger.getLogger(CallPeerMediaHandler.class);

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
     * List of advertised encryption methods. Indicated before establishing the
     * call.
     */
    private List<SrtpControlType> advertisedEncryptionMethods =
        new ArrayList<SrtpControlType>();

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
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of the <tt>Call</tt> of {@link #peer}.
     */
    private final CallPropertyChangeListener callPropertyChangeListener;

    /**
     * The listener that our <tt>CallPeer</tt> registers for CSRC audio level
     * events.
     */
    private CsrcAudioLevelListener csrcAudioLevelListener;

    /**
     * The object that we are using to sync operations on
     * <tt>csrcAudioLevelListener</tt>.
     */
    private final Object csrcAudioLevelListenerLock = new Object();

    /**
     * Contains all dynamic payload type mappings that have been made for this
     * call.
     */
    private final DynamicPayloadTypeRegistry dynamicPayloadTypes
        = new DynamicPayloadTypeRegistry();

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
     * Determines whether we have placed the call on hold locally.
     */
    protected boolean locallyOnHold = false;

    /**
     * The listener that the <tt>CallPeer</tt> registered for local user audio
     * level events.
     */
    private SimpleAudioLevelListener localUserAudioLevelListener;

    /**
     * The object that we are using to sync operations on
     * <tt>localAudioLevelListener</tt>.
     */
    private final Object localUserAudioLevelListenerLock = new Object();

    /**
     * The state of this instance which may be shared with multiple other
     * <tt>CallPeerMediaHandler</tt>s.
     */
    private MediaHandler mediaHandler;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of the <tt>MediaStream</tt>s of this instance.
     * Since <tt>CallPeerMediaHandler</tt> wraps around/shares a
     * <tt>MediaHandler</tt>, <tt>mediaHandlerPropertyChangeListener</tt>
     * actually listens to <tt>PropertyChangeEvent</tt>s fired by the
     * <tt>MediaHandler</tt> in question and forwards them as its own.
     */
    private final PropertyChangeListener mediaHandlerPropertyChangeListener
        = new PropertyChangeListener()
        {
            /**
             * Notifies this <tt>PropertyChangeListener</tt> that the value of
             * a specific property of the notifier it is registered with has
             * changed.
             *
             * @param ev a <tt>PropertyChangeEvent</tt> which describes the
             * source of the event, the name of the property which has changed
             * its value and the old and new values of the property
             * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
             */
            public void propertyChange(PropertyChangeEvent ev)
            {
                mediaHandlerPropertyChange(ev);
            }
        };

    /**
     * A reference to the CallPeer instance that this handler is managing media
     * streams for.
     */
    private final T peer;

    /**
     * Contains all RTP extension mappings (those made through the extmap
     * attribute) that have been bound during this call.
     */
    private final DynamicRTPExtensionsRegistry rtpExtensionsRegistry
        = new DynamicRTPExtensionsRegistry();

    /**
     * The <tt>SrtpListener</tt> which is responsible for the SRTP control. Most
     * often than not, it is the <tt>peer</tt> itself.
     */
    private final SrtpListener srtpListener;

    /**
     * The listener that our <tt>CallPeer</tt> registered for stream audio
     * level events.
     */
    private SimpleAudioLevelListener streamAudioLevelListener;

    /**
     * The object that we are using to sync operations on
     * <tt>streamAudioLevelListener</tt>.
     */
    private final Object streamAudioLevelListenerLock = new Object();

    /**
     * Determines whether or not streaming local video is currently enabled.
     * Default is RECVONLY. We tried to have INACTIVE at one point but it was
     * breaking incoming reINVITEs for video calls..
     */
    private MediaDirection videoDirectionUserPreference
        = MediaDirection.RECVONLY;

    /**
     * The aid which implements the boilerplate related to adding and removing
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them on behalf
     * of this instance.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this, true);

    /**
     * The <tt>VideoMediaStream</tt> which this instance uses to send and
     * receive video.
     */
    private VideoMediaStream videoStream;

    /**
     * Identifier used to group the audio stream and video stream towards
     * the <tt>CallPeer</tt> in SDP.
     */
    private String msLabel = UUID.randomUUID().toString();

    /**
     * The <tt>VideoListener</tt> which listens to the video
     * <tt>MediaStream</tt> of this instance for changes in the availability of
     * visual <tt>Component</tt>s displaying remote video and re-fires them as
     * originating from this instance.
     */
    private final VideoListener videoStreamVideoListener
        = new VideoListener()
        {
            /**
             * Notifies this <tt>VideoListener</tt> about a specific
             * <tt>VideoEvent</tt>. Fires a new <tt>VideoEvent</tt> which has
             * this <tt>CallPeerMediaHandler</tt> as its source and carries the
             * same information as the specified <tt>ev</tt> i.e. translates the
             * specified <tt>ev</tt> into a <tt>VideoEvent</tt> fired by this
             * <tt>CallPeerMediaHandler</tt>.
             *
             * @param ev the <tt>VideoEvent</tt> to notify this
             * <tt>VideoListener</tt> about
             */
            private void onVideoEvent(VideoEvent ev)
            {
                VideoEvent clone = ev.clone(CallPeerMediaHandler.this);

                fireVideoEvent(clone);
                if (clone.isConsumed())
                    ev.consume();
            }

            public void videoAdded(VideoEvent ev)
            {
                onVideoEvent(ev);
            }

            public void videoRemoved(VideoEvent ev)
            {
                onVideoEvent(ev);
            }

            public void videoUpdate(VideoEvent ev)
            {
                onVideoEvent(ev);
            }
        };

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> instance that we will be managing
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
     * Adds encryption method to the list of advertised secure methods.
     * @param encryptionMethod the method to add.
     */
    public void addAdvertisedEncryptionMethod(SrtpControlType encryptionMethod)
    {
        if(!advertisedEncryptionMethods.contains(encryptionMethod))
            advertisedEncryptionMethods.add(encryptionMethod);
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
     * Notifies this instance that a value of a specific property of the
     * <tt>Call</tt> of {@link #peer} has changed from a specific old value to a
     * specific new value.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which specified the property
     * which had its value changed and the old and new values of that property
     */
    private void callPropertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();
        boolean callConferenceChange
            = MediaAwareCall.CONFERENCE.equals(propertyName);

        if (callConferenceChange
                || MediaAwareCall.DEFAULT_DEVICE.equals(propertyName))
        {
            MediaAwareCall<?,?,?> call = getPeer().getCall();

            if (call == null)
                return;

            for (MediaType mediaType : MediaType.values())
            {
                MediaStream stream = getStream(mediaType);

                if (stream == null)
                    continue;

                // Update the stream device, if necessary.
                MediaDevice oldDevice = stream.getDevice();

                if (oldDevice != null)
                {
                    /*
                     * DEFAULT_DEVICE signals that the actual/hardware device
                     * has been changed and we will make sure that is the case
                     * in order to avoid unnecessary changes. CONFERENCE signals
                     * that the associated Call has been moved to a new
                     * telephony conference and we have to move its MediaStreams
                     * to the respective mixers.
                     */
                    MediaDevice oldValue
                        = (!callConferenceChange
                                && (oldDevice instanceof MediaDeviceWrapper))
                            ? ((MediaDeviceWrapper) oldDevice)
                                .getWrappedDevice()
                            : oldDevice;
                    MediaDevice newDevice = getDefaultDevice(mediaType);
                    MediaDevice newValue
                        = (!callConferenceChange
                                && (newDevice instanceof MediaDeviceWrapper))
                            ? ((MediaDeviceWrapper) newDevice)
                                .getWrappedDevice()
                            : newDevice;

                    if (oldValue != newValue)
                        stream.setDevice(newDevice);
                }

                stream.setRTPTranslator(call.getRTPTranslator(mediaType));
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
            callPropertyChangeListener.removePropertyChangeListener();

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
        if (logger.isDebugEnabled())
            logger.debug("Closing " + mediaType + " stream for " + getPeer());
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

        TransportManager<?> transportManager = queryTransportManager();

        if (transportManager != null)
            transportManager.closeStreamConnector(mediaType);
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
        for(MediaFormat match : formats)
        {
            if (match.matches(format))
                return match;
        }
        return null;
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
        return getPeer().getCall().getDefaultDevice(mediaType);
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
        case DATA:
            return MediaDirection.INACTIVE;
        default:
            throw new IllegalArgumentException("mediaType");
        }
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
        MediaDevice device = getDefaultDevice(type);
        return device != null
            ? device.getSupportedExtensions()
            : new ArrayList<RTPExtension>();
    }

    /**
     * Returns the harvesting time (in ms) for the harvester given in parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The harvesting time (in ms) for the harvester given in parameter.
     * 0 if this harvester does not exists, if the ICE agent is null, or if the
     * agent has never harvested with this harvester.
     */
    public long getHarvestingTime(String harvesterName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getHarvestingTime(harvesterName);
    }

    /**
     * Returns the extended type of the candidate selected if this transport
     * manager is using ICE.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return The extended type of the candidate selected if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public String getICECandidateExtendedType(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICECandidateExtendedType(streamName);
    }

    /**
     * Returns the ICE local host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public InetSocketAddress getICELocalHostAddress(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICELocalHostAddress(streamName);
    }

    /**
     * Returns the ICE local reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * local candidate used.
     */
    public InetSocketAddress getICELocalReflexiveAddress(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICELocalReflexiveAddress(streamName);
    }

    /**
     * Returns the ICE local relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * local candidate used.
     */
    public InetSocketAddress getICELocalRelayedAddress(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICELocalRelayedAddress(streamName);
    }

    /**
     * Returns the ICE remote host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public InetSocketAddress getICERemoteHostAddress(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICERemoteHostAddress(streamName);
    }

    /**
     * Returns the ICE remote reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * remote candidate used.
     */
    public InetSocketAddress getICERemoteReflexiveAddress(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICERemoteReflexiveAddress(streamName);
    }

    /**
     * Returns the ICE remote relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * remote candidate used.
     */
    public InetSocketAddress getICERemoteRelayedAddress(String streamName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICERemoteRelayedAddress(streamName);
    }

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public String getICEState()
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getICEState();
    }

    /**
     * Returns a list of locally supported <tt>MediaFormat</tt>s for the
     * given <tt>MediaDevice</tt>, ordered in descending priority. Takes into
     * account the configuration obtained from the <tt>ProtocolProvider</tt>
     * instance associated this media handler -- if its set up to override the
     * global encoding settings, uses that configuration, otherwise uses the
     * global configuration.
     *
     * @param mediaDevice the <tt>MediaDevice</tt>.
     *
     * @return a non-null list of locally supported <tt>MediaFormat</tt>s for
     * <tt>mediaDevice</tt>, in decreasing order of priority.
     *
     * @see CallPeerMediaHandler#getLocallySupportedFormats(MediaDevice,
     * QualityPreset, QualityPreset)
     */
    public List<MediaFormat> getLocallySupportedFormats(MediaDevice mediaDevice)
    {
        return getLocallySupportedFormats(mediaDevice, null, null);
    }

    /**
     * Returns a list of locally supported <tt>MediaFormat</tt>s for the
     * given <tt>MediaDevice</tt>, ordered in descending priority. Takes into
     * account the configuration obtained from the <tt>ProtocolProvider</tt>
     * instance associated this media handler -- if its set up to override the
     * global encoding settings, uses that configuration, otherwise uses the
     * global configuration.
     *
     * @param mediaDevice the <tt>MediaDevice</tt>.
     * @param sendPreset the preset used to set some of the format parameters,
     * used for video and settings.
     * @param receivePreset the preset used to set the receive format
     * parameters, used for video and settings.
     *
     * @return a non-null list of locally supported <tt>MediaFormat</tt>s for
     * <tt>mediaDevice</tt>, in decreasing order of priority.
     */
    public List<MediaFormat> getLocallySupportedFormats(
            MediaDevice mediaDevice,
            QualityPreset sendPreset, QualityPreset receivePreset)
    {
        if(mediaDevice == null)
            return Collections.emptyList();

        Map<String, String> accountProperties
            = getPeer().getProtocolProvider().getAccountID()
                    .getAccountProperties();
        String overrideEncodings
            = accountProperties.get(ProtocolProviderFactory.OVERRIDE_ENCODINGS);

        if(Boolean.parseBoolean(overrideEncodings))
        {
            /*
             * The account properties associated with the CallPeer of this
             * CallPeerMediaHandler override the global EncodingConfiguration.
             */

            EncodingConfiguration encodingConfiguration
                = ProtocolMediaActivator.getMediaService()
                        .createEmptyEncodingConfiguration();

            encodingConfiguration.loadProperties(
                    accountProperties,
                    ProtocolProviderFactory.ENCODING_PROP_PREFIX);
            return
                mediaDevice.getSupportedFormats(
                        sendPreset, receivePreset,
                        encodingConfiguration);
        }
        else /* The global EncodingConfiguration is in effect. */
        {
            return mediaDevice.getSupportedFormats(sendPreset, receivePreset);
        }
    }

    /**
     * Gets the visual <tt>Component</tt>, if any, depicting the video streamed
     * from the local peer to the remote peer.
     *
     * @return the visual <tt>Component</tt> depicting the local video if local
     * video is actually being streamed from the local peer to the remote peer;
     * otherwise, <tt>null</tt>
     */
    public Component getLocalVisualComponent()
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);

        return
            ((videoStream == null) || !isLocalVideoTransmissionEnabled())
                ? null
                : ((VideoMediaStream) videoStream).getLocalVisualComponent();
    }

    public MediaHandler getMediaHandler()
    {
        return mediaHandler;
    }

    /**
     * Returns the number of harvesting for this agent.
     *
     * @return The number of harvesting for this agent.
     */
    public int getNbHarvesting()
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getNbHarvesting();
    }

    /**
     * Returns the number of harvesting time for the harvester given in
     * parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The number of harvesting time for the harvester given in
     * parameter.
     */
    public int getNbHarvesting(String harvesterName)
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getNbHarvesting(harvesterName);
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
     * Gets the last-known SSRC of an RTP stream with a specific
     * <tt>MediaType</tt> received by a <tt>MediaStream</tt> of this instance.
     *
     * @return the last-known SSRC of an RTP stream with a specific
     * <tt>MediaType</tt> received by a <tt>MediaStream</tt> of this instance
     */
    public long getRemoteSSRC(MediaType mediaType)
    {
        return mediaHandler.getRemoteSSRC(this, mediaType);
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
     * Gets the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance.
     *
     * @return the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance
     */
    public SrtpControls getSrtpControls()
    {
        return mediaHandler.getSrtpControls(this);
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
        case DATA:
            /*
             * DATA is a valid MediaType value and CallPeerMediaHandler does not
             * utilize it at this time so no IllegalArgumentException is thrown
             * and null is returned (as documented).
             */
            return null;
        case VIDEO:
            return videoStream;
        default:
            throw new IllegalArgumentException("mediaType");
        }
    }

    /**
     * Returns the total harvesting time (in ms) for all harvesters.
     *
     * @return The total harvesting time (in ms) for all the harvesters. 0 if
     * the ICE agent is null, or if the agent has nevers harvested.
     */
    public long getTotalHarvestingTime()
    {
        TransportManager<?> transportManager = queryTransportManager();

        return
            (transportManager == null)
                ? null
                : transportManager.getTotalHarvestingTime();
    }

    /**
     * Gets the <tt>TransportManager</tt> implementation handling our address
     * management. If the <tt>TransportManager</tt> does not exist yet, it is
     * created.
     *
     * @return the <tt>TransportManager</tt> implementation handling our address
     * management
     */
    protected abstract TransportManager<T> getTransportManager();

    /**
     * Gets the <tt>TransportManager</tt> implementation handling our address
     * management. If the <tt>TransportManager</tt> does not exist yet, it is
     * not created.
     *
     * @return the <tt>TransportManager</tt> implementation handling our address
     * management
     */
    protected abstract TransportManager<T> queryTransportManager();

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
    protected MediaStream initStream(StreamConnector    connector,
                                     MediaDevice        device,
                                     MediaFormat        format,
                                     MediaStreamTarget  target,
                                     MediaDirection     direction,
                                     List<RTPExtension> rtpExtensions,
                                     boolean            masterStream)
        throws OperationFailedException
    {
        MediaType mediaType = device.getMediaType();
        if (logger.isDebugEnabled())
            logger.debug("Initializing " + mediaType + " stream for "
                    +getPeer());

        /*
         * Do make sure that no unintentional streaming of media generated by
         * the user without prior consent will happen.
         */
        direction = direction.and(getDirectionUserPreference(mediaType));
        /*
         * If the device does not support a direction, there is really nothing
         * to be done at this point to make it use it.
         */
        direction = direction.and(device.getDirection());

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

        switch (mediaType)
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
     * Compares a list of <tt>MediaFormat</tt>s offered by a remote party
     * to the list of locally supported <tt>RTPExtension</tt>s as returned
     * by one of our local <tt>MediaDevice</tt>s and returns a third
     * <tt>List</tt> that contains their intersection.
     *
     * Note that it also treats telephone-event as a special case and puts it
     * to the end of the intersection, if there is any intersection.
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
        MediaFormat telephoneEvents = null;
        MediaFormat red = null;
        MediaFormat ulpfec = null;

        for(MediaFormat remoteFormat : remoteFormats)
        {
            MediaFormat localFormat
                = findMediaFormat(localFormats, remoteFormat);

            if (localFormat != null)
            {
                // We ignore telephone-event, red and ulpfec here as they are
                // not real media formats. Therefore we don't want to decide to
                // use any of them as our preferred format. We'll add them back
                // later if we find a common media format.
                //
                // Note if there are multiple telephone-event (or red, or
                // ulpfec) formats, we'll lose all but the last one.  That's
                // fine because it's meaningless to have multiple repeated
                // formats.
                String encoding = localFormat.getEncoding();
                if (Constants.TELEPHONE_EVENT.equals(encoding))
                {
                    telephoneEvents = localFormat;
                    continue;
                }
                else if (Constants.RED.equals(encoding))
                {
                    red = localFormat;
                    continue;
                }
                else if (Constants.ULPFEC.equals(encoding))
                {
                    ulpfec = localFormat;
                    continue;
                }

                ret.add(localFormat);
            }
        }

        // If we've found some compatible formats, add telephone-event, red
        // and ulpfec back in to the end of the list (if we removed any of them)
        // above.  If we didn't find any compatible formats, we don't want to
        // add any of these formats as the only entries in the list because
        // there'd be no media.
        if (!ret.isEmpty())
        {
            if (telephoneEvents != null)
                ret.add(telephoneEvents);
            if (red != null)
                ret.add(red);
            if (ulpfec != null)
                ret.add(ulpfec);
        }

        return ret;
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
     * Checks whether <tt>dev</tt> can be used for a call.
     *
     * @return <tt>true</tt> if the device is not null, and it has at least
     * one enabled format. Otherwise <tt>false</tt>
     */
    public boolean isDeviceActive(MediaDevice dev)
    {
        return (dev != null) && !getLocallySupportedFormats(dev).isEmpty();
    }

    /**
     * Checks whether <tt>dev</tt> can be used for a call, using
     * <tt>sendPreset</tt> and <tt>reveicePreset</tt>
     *
     * @return <tt>true</tt> if the device is not null, and it has at least
     * one enabled format. Otherwise <tt>false</tt>
     */
    public boolean isDeviceActive(
            MediaDevice dev,
            QualityPreset sendPreset, QualityPreset receivePreset)
    {
        return
            (dev != null)
                && !getLocallySupportedFormats(dev, sendPreset, receivePreset)
                        .isEmpty();
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
     * Determines whether RTP translation is enabled for the <tt>CallPeer</tt>
     * represented by this <tt>CallPeerMediaHandler</tt> and for a specific
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> for which it is to be determined
     * whether RTP translation is enabled for the <tT>CallPeeer</tt> represented
     * by this <tt>CallPeerMediaHandler</tt>
     * @return <tt>true</tt> if RTP translation is enabled for the
     * <tt>CallPeer</tt> represented by this <tt>CallPeerMediaHandler</tt> and
     * for the specified <tt>mediaType; otherwise, <tt>false</tt>
     */
    public boolean isRTPTranslationEnabled(MediaType mediaType)
    {
        T peer = getPeer();
        MediaAwareCall<?,?,?> call = peer.getCall();

        if ((call != null)
                && call.isConferenceFocus()
                && !call.isLocalVideoStreaming())
        {
            Iterator<?> callPeerIt = call.getCallPeers();

            while (callPeerIt.hasNext())
            {
                MediaAwareCallPeer<?,?,?> callPeer
                    = (MediaAwareCallPeer<?,?,?>) callPeerIt.next();
                MediaStream stream
                    = callPeer.getMediaHandler().getStream(mediaType);

                if (stream != null)
                    return true;
            }
        }
        return false;
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
     * Notifies this instance about a <tt>PropertyChangeEvent</tt> fired by the
     * associated {@link MediaHandler}. Since this instance wraps around the
     * associated <tt>MediaHandler</tt>, it forwards the property changes as its
     * own. Allows extenders to override.
     *
     * @param ev the <tt>PropertyChangeEvent</tt> fired by the associated
     * <tt>MediaHandler</tt>
     */
    protected void mediaHandlerPropertyChange(PropertyChangeEvent ev)
    {
        firePropertyChange(
                ev.getPropertyName(),
                ev.getOldValue(), ev.getNewValue());
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
     * Removes from this instance and cleans up the <tt>SrtpControl</tt> which
     * are not of a specific <tt>SrtpControlType</tt>.
     * 
     * @param mediaType the <tt>MediaType</tt> of the <tt>SrtpControl</tt> to be
     * examined
     * @param srtpControlType the <tt>SrtpControlType</tt> of the
     * <tt>SrtpControl</tt>s to not be removed from this instance and cleaned
     * up. If <tt>null</tt>, all <tt>SrtpControl</tt>s are removed from this
     * instance and cleaned up
     */
    protected void removeAndCleanupOtherSrtpControls(
            MediaType mediaType,
            SrtpControlType srtpControlType)
    {
        SrtpControls srtpControls = getSrtpControls();

        for (SrtpControlType i : SrtpControlType.values())
        {
            if (!i.equals(srtpControlType))
            {
                SrtpControl e = srtpControls.remove(mediaType, i);

                if (e != null)
                    e.cleanup(null);
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
    public void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
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
     * Sets <tt>csrcAudioLevelListener</tt> as the listener that will be
     * receiving notifications for changes in the audio levels of the remote
     * participants that our peer is mixing.
     *
     * @param listener the <tt>CsrcAudioLevelListener</tt> to set to our audio
     * stream.
     */
    public void setCsrcAudioLevelListener(CsrcAudioLevelListener listener)
    {
        synchronized (csrcAudioLevelListenerLock)
        {
            if (this.csrcAudioLevelListener != listener)
            {
                MediaHandler mediaHandler = getMediaHandler();

                if ((mediaHandler != null)
                        && (this.csrcAudioLevelListener != null))
                {
                    mediaHandler.removeCsrcAudioLevelListener(
                            this.csrcAudioLevelListener);
                }

                this.csrcAudioLevelListener = listener;

                if ((mediaHandler != null)
                        && (this.csrcAudioLevelListener != null))
                {
                    mediaHandler.addCsrcAudioLevelListener(
                            this.csrcAudioLevelListener);
                }
            }
        }
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
     * Puts all <tt>MediaStream</tt>s in this handler locally on or off hold
     * (according to the value of <tt>locallyOnHold</tt>). This would also be
     * taken into account when the next update offer is generated.
     *
     * @param locallyOnHold <tt>true</tt> if we are to make our streams
     * stop transmitting and <tt>false</tt> if we are to start transmitting
     * again.
     */
    public void setLocallyOnHold(boolean locallyOnHold)
    {
        if (logger.isDebugEnabled())
            logger.debug("Setting locally on hold: " + locallyOnHold);

        this.locallyOnHold = locallyOnHold;

        // On hold.
        if(locallyOnHold)
        {
            MediaStream audioStream = getStream(MediaType.AUDIO);
            MediaDirection direction
                    = (getPeer().getCall().isConferenceFocus()
                        || audioStream == null)
                    ? MediaDirection.INACTIVE
                    : audioStream.getDirection().and(MediaDirection.SENDONLY);
            // the direction in situation where audioStream is
            // null is ignored (just avoiding NPE)

            if(audioStream != null)
            {
                audioStream.setDirection(direction);
                audioStream.setMute(true);
            }

            MediaStream videoStream = getStream(MediaType.VIDEO);
            if(videoStream != null)
            {
                direction = getPeer().getCall().isConferenceFocus()
                        ? MediaDirection.INACTIVE
                        : videoStream.getDirection().and(MediaDirection.SENDONLY);
                /*
                 * Set the video direction to INACTIVE, because currently we
                 * cannot mute video streams.
                 */
                videoStream.setDirection(MediaDirection.INACTIVE);
                //videoStream.setDirection(direction);
                //videoStream.setMute(true);
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
                audioStream.setMute(false);
            }

            MediaStream videoStream = getStream(MediaType.VIDEO);

            if((videoStream != null)
                && (videoStream.getDirection() != MediaDirection.INACTIVE))
            {
                videoStream.setDirection(
                        videoStream.getDirection().or(MediaDirection.SENDONLY));
                videoStream.setMute(false);
            }
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
        synchronized (localUserAudioLevelListenerLock)
        {
            if (this.localUserAudioLevelListener != listener)
            {
                MediaHandler mediaHandler = getMediaHandler();

                if ((mediaHandler != null)
                        && (this.localUserAudioLevelListener != null))
                {
                    mediaHandler.removeLocalUserAudioLevelListener(
                            this.localUserAudioLevelListener);
                }

                this.localUserAudioLevelListener = listener;

                if ((mediaHandler != null)
                        && (this.localUserAudioLevelListener != null))
                {
                    mediaHandler.addLocalUserAudioLevelListener(
                            this.localUserAudioLevelListener);
                }
            }
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
        if (logger.isDebugEnabled())
            logger.debug("Setting local video transmission enabled: " + enabled);
        MediaDirection oldValue = videoDirectionUserPreference;

        videoDirectionUserPreference
            = enabled ? MediaDirection.SENDRECV : MediaDirection.RECVONLY;

        MediaDirection newValue = videoDirectionUserPreference;

        /*
         * Do not send an event here if the local video is enabled because the
         * video stream needs to start before the correct MediaDevice is set in
         * VideoMediaDeviceSession.
         */
        if (!enabled)
        {
            firePropertyChange(
                    OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING,
                    oldValue, newValue);
        }
    }

    public void setMediaHandler(MediaHandler mediaHandler)
    {
        if (this.mediaHandler != mediaHandler)
        {
            if (this.mediaHandler != null)
            {
                synchronized (csrcAudioLevelListenerLock)
                {
                    if (csrcAudioLevelListener != null)
                    {
                        this.mediaHandler.removeCsrcAudioLevelListener(
                                csrcAudioLevelListener);
                    }
                }
                synchronized (localUserAudioLevelListenerLock)
                {
                    if (localUserAudioLevelListener != null)
                    {
                        this.mediaHandler.removeLocalUserAudioLevelListener(
                                localUserAudioLevelListener);
                    }
                }
                synchronized (streamAudioLevelListenerLock)
                {
                    if (streamAudioLevelListener != null)
                    {
                        this.mediaHandler.removeStreamAudioLevelListener(
                                streamAudioLevelListener);
                    }
                }

                this.mediaHandler.removeKeyFrameRequester(keyFrameRequester);
                this.mediaHandler.removePropertyChangeListener(
                        mediaHandlerPropertyChangeListener);
                if (srtpListener != null)
                    this.mediaHandler.removeSrtpListener(srtpListener);
                this.mediaHandler.removeVideoListener(videoStreamVideoListener);

                // We intentionally do not remove our Call from the list of
                // DTMF listeners. It should stay there as long as the
                // MediaHandler is used by at least one CallPeer/CPMH.
                //this.mediaHandler.removeDtmfListener(getPeer().getCall());
            }

            this.mediaHandler = mediaHandler;

            if (this.mediaHandler != null)
            {
                synchronized (csrcAudioLevelListenerLock)
                {
                    if (csrcAudioLevelListener != null)
                    {
                        this.mediaHandler.addCsrcAudioLevelListener(
                                csrcAudioLevelListener);
                    }
                }
                synchronized (localUserAudioLevelListenerLock)
                {
                    if (localUserAudioLevelListener != null)
                    {
                        this.mediaHandler.addLocalUserAudioLevelListener(
                                localUserAudioLevelListener);
                    }
                }
                synchronized (streamAudioLevelListenerLock)
                {
                    if (streamAudioLevelListener != null)
                    {
                        this.mediaHandler.addStreamAudioLevelListener(
                                streamAudioLevelListener);
                    }
                }

                this.mediaHandler.addKeyFrameRequester(-1, keyFrameRequester);
                this.mediaHandler.addPropertyChangeListener(
                        mediaHandlerPropertyChangeListener);
                if (srtpListener != null)
                    this.mediaHandler.addSrtpListener(srtpListener);
                this.mediaHandler.addVideoListener(videoStreamVideoListener);
                this.mediaHandler.addDtmfListener(getPeer().getCall());
            }
        }
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
     * If the local <tt>AudioMediaStream</tt> has already been created, sets
     * <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt> that it should
     * notify for stream user level events. Otherwise stores a reference to
     * <tt>listener</tt> so that we could add it once we create the stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add or
     * <tt>null</tt> if we are trying to remove it.
     */
    public void setStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        synchronized (streamAudioLevelListenerLock)
        {
            if (this.streamAudioLevelListener != listener)
            {
                MediaHandler mediaHandler = getMediaHandler();

                if ((mediaHandler != null)
                        && (this.streamAudioLevelListener != null))
                {
                    mediaHandler.removeStreamAudioLevelListener(
                            this.streamAudioLevelListener);
                }

                this.streamAudioLevelListener = listener;

                if ((mediaHandler != null)
                        && (this.streamAudioLevelListener != null))
                {
                    mediaHandler.addStreamAudioLevelListener(
                            this.streamAudioLevelListener);
                }
            }
        }
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
        if (logger.isInfoEnabled())
            logger.info("Starting");

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
                    null, videoDirectionUserPreference);

            if(!stream.isStarted())
            {
                getTransportManager().setTrafficClass(
                        stream.getTarget(),
                        MediaType.VIDEO);
                stream.start();

                /*
                 * Send an empty packet to unblock some kinds of RTP proxies. Do
                 * not consult whether the local video should be streamed and
                 * send the hole-punch packet anyway to let the remote video
                 * reach this local peer.
                 */
                sendHolePunchPacket(stream.getTarget());
            }
        }
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
    protected abstract void throwOperationFailedException(
            String message,
            int errorCode,
            Throwable cause)
        throws OperationFailedException;

    /**
     * Returns the value to use for the 'msid' source-specific SDP media
     * attribute (RFC5576) for the stream of type <tt>mediaType</tt> towards
     * the <tt>CallPeer</tt>. It consist of a group identifier (shared between
     * the local audio and video streams towards the <tt>CallPeer</tt>) and an
     * identifier for the particular stream, separated by a space.
     *
     * {@see http://tools.ietf.org/html/draft-ietf-mmusic-msid}
     *
     * @param mediaType the media type of the stream for which to return the
     * value for 'msid'
     * @return the value to use for the 'msid' source-specific SDP media
     * attribute (RFC5576) for the stream of type <tt>mediaType</tt> towards
     * the <tt>CallPeer</tt>.
     */
    public String getMsid(MediaType mediaType)
    {
        return msLabel + " " + getLabel(mediaType);
    }

    /**
     * Returns the value to use for the 'label' source-specific SDP media
     * attribute (RFC5576) for the stream of type <tt>mediaType</tt> towards
     * the <tt>CallPeer</tt>.
     *
     * @param mediaType the media type of the stream for which to return the
     * value for 'label'
     * @return the value to use for the 'label' source-specific SDP media
     * attribute (RFC5576) for the stream of type <tt>mediaType</tt> towards
     * the <tt>CallPeer</tt>.
     */
     public String getLabel(MediaType mediaType)
    {
        return mediaType.toString() + hashCode();
    }

    /**
     * Returns the value to use for the 'mslabel' source-specific SDP media
     * attribute (RFC5576).
     * @return the value to use for the 'mslabel' source-specific SDP media
     * attribute (RFC5576).
     */
    public String getMsLabel()
    {
        return msLabel;
    }

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
        private final MediaAwareCall<?, ?, ?> call;

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

        /**
         * Removes this <tt>PropertyChangeListener</tt> from its associated
         * <tt>Call</tt>.
         */
        public void removePropertyChangeListener()
        {
            call.removePropertyChangeListener(this);
        }
    }
}
