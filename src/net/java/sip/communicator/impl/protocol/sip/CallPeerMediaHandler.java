/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.Component;
import java.net.*;
import java.util.*;
import java.beans.*;

import javax.sdp.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.SimpleAudioLevelListener; // disambiguation
import net.java.sip.communicator.service.neomedia.event.CsrcAudioLevelListener; // disambiguation
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The media handler class handles all media management for a single
 * <tt>CallPeer</tt>. This includes initializing and configuring streams,
 * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt> always
 * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
 * both classes are only separated for reasons of readability.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class CallPeerMediaHandler
    extends PropertyChangeNotifier
{
    /**
     * Our class logger.
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
    static final long SSRC_UNKNOWN = -1;

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
     * A reference to the CallPeerSipImpl instance that this handler is
     * managing media streams for.
     */
    public final CallPeerSipImpl peer;

    /**
     * The last ( and maybe only ) session description that we generated for
     * our own media.
     */
    private SessionDescription localSess = null;

    /**
     * The last ( and maybe only ) session description that we received from
     * the remote party.
     */
    private SessionDescription remoteSess = null;

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
     * The minimum port number that we'd like our RTP sockets to bind upon.
     */
    private static int minMediaPort = 5000;

    /**
     * The maximum port number that we'd like our RTP sockets to bind upon.
     */
    private static int maxMediaPort = 6000;

    /**
     * The port that we should try to bind our next media stream's RTP socket
     * to.
     */
    private static int nextMediaPortToTry = minMediaPort;

    /**
     * Determines whether we have placed the call on hold locally.
     */
    private boolean locallyOnHold = false;

    /**
     * The RTP/RTCP socket couple that this media handler should use to send
     * and receive audio flows through.
     */
    private StreamConnector audioStreamConnector = null;

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
     * The RTP/RTCP socket couple that this media handler should use to send
     * and receive video flows through.
     */
    private StreamConnector videoStreamConnector = null;

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
     * The <tt>List</tt> of <tt>VideoListener</tt>s interested in
     * <tt>VideoEvent</tt>s fired by this instance or rather its
     * <tt>VideoMediaStream</tt>.
     */
    private final List<VideoListener> videoListeners
        = new LinkedList<VideoListener>();

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
     * A <tt>URL</tt> pointing to a location with call information or a call
     * control web interface related to the <tt>CallPeer</tt> that we are
     * associated with.
     */
    private URL callInfoURL = null;

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
     * Holds the ZRTP controls used for the current call.
     */
    private Map<MediaType, ZrtpControl> zrtpControls =
        new Hashtable<MediaType, ZrtpControl>();

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerSipImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandler(CallPeerSipImpl peer)
    {
        this.peer = peer;
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
    private MediaDevice getDefaultDevice(MediaType mediaType)
    {
        return peer.getCall().getDefaultDevice(mediaType);
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
    private MediaDirection getDirectionUserPreference(MediaType mediaType)
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
            if(videoStream != null)
            {
                videoStream.setDirection(videoStream.getDirection()
                            .or(MediaDirection.SENDONLY));
                videoStream.setMute(locallyOnHold);
            }
        }
    }


    /**
     * Closes and null-ifies all streams and connectors and readies this media
     * handler for garbage collection (or reuse).
     */
    public void close()
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
    private void closeStream(MediaType type)
    {
        if( type == MediaType.AUDIO)
        {
            setAudioStream(null);
            if (this.audioStreamConnector != null)
            {
                audioStreamConnector.getDataSocket().close();
                audioStreamConnector.getControlSocket().close();
                audioStreamConnector = null;
            }
        }
        else
        {
            setVideoStream(null);

            if (this.videoStreamConnector != null)
            {
                videoStreamConnector.getDataSocket().close();
                videoStreamConnector.getControlSocket().close();
                videoStreamConnector = null;
            }
        }

        // clears the zrtp controls used for current call.
        zrtpControls.remove(type);
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
     * Creates a session description <tt>String</tt> representing the
     * <tt>MediaStream</tt>s that this <tt>MediaHandler</tt> is prepare to
     * exchange. The offer takes into account user preferences such as whether
     * or not local user would be transmitting video, whether any or all streams
     * are put on hold, etc. The method is also taking into account any previous
     * offers that this handler may have previously issues hence making the
     * newly generated <tt>String</tt> an session creation or a session update
     * offer accordingly.
     *
     * @return an SDP description <tt>String</tt> representing the streams that
     * this handler is prepared to initiate.
     *
     * @throws OperationFailedException if creating the SDP fails for some
     * reason.
     */
    public String createOffer()
        throws OperationFailedException
    {
        if (localSess == null)
            return createFirstOffer().toString();
        else
            return createUpdateOffer(localSess).toString();
    }

    /**
     * Allocates ports, retrieves supported formats and creates a
     * <tt>SessionDescription</tt>.
     *
     * @return the <tt>String</tt> representation of the newly created
     * <tt>SessionDescription</tt>.
     *
     * @throws OperationFailedException if generating the SDP fails for whatever
     * reason.
     */
    private SessionDescription createFirstOffer()
        throws OperationFailedException
    {
        //Audio Media Description
        Vector<MediaDescription> mediaDescs = createMediaDescriptions();

        //wrap everything up in a session description
        String userName = peer.getProtocolProvider().getAccountID().getUserID();

        SessionDescription sDes = SdpUtils.createSessionDescription(
            getLastUsedLocalHost(), userName, mediaDescs);

        this.localSess = sDes;
        return localSess;
    }

    /**
     * Creates a <tt>Vector</tt> containing the <tt>MediaSescription</tt> of
     * streams that this handler is prepared to initiate depending on available
     * <tt>MediaDevice</tt>s and local on-hold and video transmission
     * preferences.
     *
     * @return a <tt>Vector</tt> containing the <tt>MediaSescription</tt> of
     * streams that this handler is prepared to initiate.
     *
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
     */
    private Vector<MediaDescription> createMediaDescriptions()
        throws OperationFailedException
    {
        //Audio Media Description
        Vector<MediaDescription> mediaDescs = new Vector<MediaDescription>();

        for (MediaType mediaType : MediaType.values())
        {
            MediaDevice dev = getDefaultDevice(mediaType);

            if (dev != null)
            {
                MediaDirection direction = dev.getDirection().and(
                                getDirectionUserPreference(mediaType));

                if(locallyOnHold)
                    direction = direction.and(MediaDirection.SENDONLY);

                if(direction != MediaDirection.INACTIVE)
                {
                    MediaDescription md =
                        createMediaDescription(
                                        dev.getFormat(),
                                        dev.getSupportedFormats(),
                                        getStreamConnector(mediaType),
                                        direction,
                                        dev.getSupportedExtensions());

                    if(peer.getCall().isSipZrtpAttribute())
                    {
                        try
                        {
                            ZrtpControl control = zrtpControls.get(mediaType);
                            if(control == null)
                            {
                                control = SipActivator.getMediaService()
                                    .createZrtpControl();
                                zrtpControls.put(mediaType, control);
                            }

                            String helloHash = control.getHelloHash();
                            if(helloHash != null && helloHash.length() > 0)
                                md.setAttribute("zrtp-hash", helloHash);

                        } catch (SdpException ex)
                        {
                            logger.error("Cannot add zrtp-hash to sdp", ex);
                        }
                    }

                    mediaDescs.add(md);
                }
            }
        }

        //fail if all devices were inactive
        if(mediaDescs.isEmpty())
        {
            ProtocolProviderServiceSipImpl
                .throwOperationFailedException(
                    "We couldn't find any active Audio/Video devices and "
                        + "couldn't create a call",
                    OperationFailedException.GENERAL_ERROR,
                    null,
                    logger);
        }

        return mediaDescs;
    }

    /**
     * Creates a <tt>SessionDescription</tt> meant to update a previous offer
     * (<tt>sdescToUpdate</tt>) so that it would reflect the current state (e.g.
     * on-hold and local video transmission preferences) of this
     * <tt>MediaHandler</tt>.
     *
     * @param sdescToUpdate the <tt>SessionDescription</tt> that we are going to
     * update.
     *
     * @return the newly created <tt>SessionDescription</tt> meant to update
     * <tt>sdescToUpdate</tt>.
     *
     * @throws OperationFailedException in case creating the new description
     * fails for some reason.
     */
    private SessionDescription createUpdateOffer(
                                        SessionDescription sdescToUpdate)
        throws OperationFailedException

    {
        //create the media descriptions reflecting our current state.
        Vector<MediaDescription> newMediaDescs = createMediaDescriptions();

        SessionDescription newOffer = SdpUtils.createSessionUpdateDescription(
                        sdescToUpdate, getLastUsedLocalHost(), newMediaDescs);

        this.localSess = newOffer;
        return newOffer;
    }

    /**
     * Returns the <tt>InetAddress</tt> that we are using in one of our
     * <tt>StreamConnector</tt>s or, in case we don't have any connectors yet
     * the address returned by the our network address manager as the best local
     * address to use when contacting the <tt>CallPeer</tt> associated with this
     * <tt>MediaHandler</tt>. This method is primarily meant for use with the
     * o= and c= fields of a newly created session description. The point is
     * that we create our <tt>StreamConnector</tt>s when constructing the media
     * descriptions so we already have a specific local address assigned to them
     * at the time we get ready to create the c= and o= fields. It is therefore
     * better to try and return one of these addresses before trying the net
     * address manager again ang running the slight risk of getting a different
     * address.
     *
     * @return an <tt>InetAddress</tt> that we use in one of the
     * <tt>StreamConnector</tt>s in this class.
     */
    private InetAddress getLastUsedLocalHost()
    {
        if (audioStreamConnector != null)
            return audioStreamConnector.getDataSocket().getLocalAddress();

        if (videoStreamConnector != null)
            return videoStreamConnector.getDataSocket().getLocalAddress();

        NetworkAddressManagerService nam
            = SipActivator.getNetworkAddressManagerService();
        InetAddress intendedDestination = peer.getProtocolProvider()
            .getIntendedDestination(peer.getPeerAddress());

        return nam.getLocalHost(intendedDestination);
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
     *
     * @return the newly created <tt>MediaStream</tt>.
     *
     * @throws OperationFailedException if creating the stream fails for any
     * reason (like for example accessing the device or setting the format).
     */
    private MediaStream initStream(StreamConnector      connector,
                                   MediaDevice          device,
                                   MediaFormat          format,
                                   MediaStreamTarget    target,
                                   MediaDirection       direction)
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
            MediaService mediaService = SipActivator.getMediaService();

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

        stream.setAdvancedAttributes(format.getAdvancedParameters());

        return  configureAndStartStream(
                        device, format, target, direction, stream);
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
     * @param stream the <tt>MediaStream</tt> that we'd like to configure.
     *
     * @return the <tt>MediaStream</tt> that we received as a parameter (for
     * convenience reasons).
     *
     * @throws OperationFailedException if setting the <tt>MediaFormat</tt>
     * or connecting to the specified <tt>MediaDevice</tt> fails for some
     * reason.
     */
    private MediaStream configureAndStartStream(
                                            MediaDevice          device,
                                            MediaFormat          format,
                                            MediaStreamTarget    target,
                                            MediaDirection       direction,
                                            MediaStream          stream)
           throws OperationFailedException
    {
        registerDynamicPTsWithStream(stream);
        registerRTPExtensionsWithStream(stream);

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
            //when using zrtp multistreams
            ZrtpControl zrtpControl = stream.getZrtpControl();

            zrtpControl.setZrtpListener(peer);
            zrtpControl.start(stream instanceof AudioMediaStream);
        }

        if ( ! stream.isStarted())
            stream.start();


        /* send empty packet to deblock some kind of RTP proxy to let just
         * one user sends its video
         */
        if(stream instanceof VideoMediaStream &&
                videoDirectionUserPreference == MediaDirection.RECVONLY)
        {
            sendHolePunchPacket(target);
        }
        return stream;
    }

    /**
     * Send empty UDP packet to target destination data/control ports
     * in order to open port on NAT or RTP proxy if any.
     *
     * @param target <tt>MediaStreamTarget</tt>
     */
    private void sendHolePunchPacket(MediaStreamTarget target)
    {
        logger.info("Try to open port on NAT if any");
        try
        {
            /* data port (RTP) */
            videoStreamConnector.getDataSocket().send(new DatagramPacket(
                    new byte[0], 0, target.getDataAddress().getAddress(),
                    target.getDataAddress().getPort()));

            /* control port (RTCP) */
            videoStreamConnector.getControlSocket().send(new DatagramPacket(
                    new byte[0], 0, target.getControlAddress().getAddress(),
                    target.getControlAddress().getPort()));
        }
        catch(Exception e)
        {
            logger.error("Error cannot send to remote peer", e);
        }
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
     */
    private void registerRTPExtensionsWithStream(MediaStream stream)
    {
        for ( Map.Entry<RTPExtension, Byte> mapEntry
                        : rtpExtensionsRegistry.getMappings().entrySet())
        {
            byte extensionID = mapEntry.getValue();
            RTPExtension rtpExtension = mapEntry.getKey();

            stream.addRTPExtension(extensionID, rtpExtension);
        }
    }

    /**
     * Parses <tt>offerString</tt>, creates the <tt>MediaStream</tt>s that it
     * describes and constructs a response representing the state of this
     * <tt>MediaHandler</tt>. The method takes into account the presence or
     * absence of previous negotiations and interprets the <tt>offerString</tt>
     * as an initial offer or a session update accordingly.
     *
     * @param offerString The SDP offer that we'd like to parse, handle and get
     * a response for.
     *
     * @return A <tt>String</tt> containing the SDP response representing the
     * current state of this <tt>MediaHandler</tt>.
     *
     * @throws OperationFailedException if parsing or handling
     * <tt>offerString</tt> fails or we have a problem while creating the
     * response.
     * @throws IllegalArgumentException if there's a problem with the format
     * or semantics of the <tt>offerString</tt>.
     */
    public String processOffer(String offerString)
        throws OperationFailedException,
               IllegalArgumentException
    {
        SessionDescription offer = SdpUtils.parseSdpString(offerString);
        if (localSess == null)
            return processFirstOffer(offer).toString();
        else
            return processUpdateOffer(offer, localSess).toString();
    }

    /**
     * Parses and handles the specified <tt>SessionDescription offer</tt> and
     * returns and SDP answer representing the current state of this media
     * handler. This method MUST only be called when <tt>offer</tt> is the
     * first session description that this <tt>MediaHandler</tt> is seeing.
     *
     * @param offer the offer that we'd like to parse, handle and get an SDP
     * answer for.
     * @return the SDP answer reflecting the current state of this
     * <tt>MediaHandler</tt>
     *
     * @throws OperationFailedException if we have a problem satisfying the
     * description received in <tt>offer</tt> (e.g. failed to open a device or
     * initialize a stream ...).
     * @throws IllegalArgumentException if there's a problem with
     * <tt>offer</tt>'s format or semantics.
     */
    private SessionDescription processFirstOffer(SessionDescription offer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        this.remoteSess = offer;

        Vector<MediaDescription> answerDescriptions
            = createMediaDescriptionsForAnswer(offer);

        //wrap everything up in a session description
        SessionDescription answer = SdpUtils.createSessionDescription(
            getLastUsedLocalHost(), getUserName(), answerDescriptions);

        this.localSess = answer;
        return localSess;
    }

    /**
     * Parses, handles <tt>newOffer</tt>, and produces an update answer
     * representing the current state of this <tt>MediaHandler</tt>.
     *
     * @param newOffer the new SDP description that we are receiving as an
     * update to our current state.
     * @param previousAnswer the <tt>SessionDescripiton</tt> that we last sent
     * as an answer to the previous offer currently updated by
     * <tt>newOffer</tt> is updating.
     *
     * @return an answer that updates <tt>previousAnswer</tt>.
     *
     * @throws OperationFailedException if we have a problem initializing the
     * <tt>MediaStream</tt>s as suggested by <tt>newOffer</tt>
     * @throws IllegalArgumentException if there's a problem with the syntax
     * or semantics of <tt>newOffer</tt>.
     */
    private SessionDescription processUpdateOffer(
                                           SessionDescription newOffer,
                                           SessionDescription previousAnswer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        this.remoteSess = newOffer;

        Vector<MediaDescription> answerDescriptions
            = createMediaDescriptionsForAnswer(newOffer);

        // wrap everything up in a session description
        SessionDescription newAnswer = SdpUtils.createSessionUpdateDescription(
                    previousAnswer, getLastUsedLocalHost(), answerDescriptions);

        this.localSess = newAnswer;
        return localSess;
    }

    /**
     * Creates a number of <tt>MediaDescription</tt>s answering the descriptions
     * offered by the specified <tt>offer</tt> and reflecting the state of this
     * <tt>MediaHandler</tt>.
     *
     * @param offer the offer that we'd like the newly generated session
     * descriptions to answer.
     *
     * @return a <tt>Vector</tt> containing the <tt>MediaDescription</tt>s
     * answering those provided in the <tt>offer</tt>.
     * @throws OperationFailedException if there's a problem handling the
     * <tt>offer</tt>
     * @throws IllegalArgumentException if there's a problem with the syntax
     * or semantics of <tt>newOffer</tt>.
     */
    private Vector<MediaDescription> createMediaDescriptionsForAnswer(
                    SessionDescription offer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        List<MediaDescription> remoteDescriptions = SdpUtils
                        .extractMediaDescriptions(offer);

        // prepare to generate answers to all the incoming descriptions
        Vector<MediaDescription> answerDescriptions
            = new Vector<MediaDescription>( remoteDescriptions.size() );

        this.setCallInfoURL(SdpUtils.getCallInfoURL(offer));

        boolean atLeastOneValidDescription = false;

        for (MediaDescription mediaDescription : remoteDescriptions)
        {
            MediaType mediaType = SdpUtils.getMediaType(mediaDescription);

            List<MediaFormat> supportedFormats = SdpUtils.extractFormats(
                            mediaDescription, dynamicPayloadTypes);

            MediaDevice dev = getDefaultDevice(mediaType);
            MediaDirection devDirection
                = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            devDirection
                = devDirection.and(getDirectionUserPreference(mediaType));

            // stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, offer);
            int targetDataPort = target.getDataAddress().getPort();

            if (supportedFormats.isEmpty()
                    || (devDirection == MediaDirection.INACTIVE)
                    || (targetDataPort == 0))
            {
                // mark stream as dead and go on bravely
                answerDescriptions.add(SdpUtils
                                .createDisablingAnswer(mediaDescription));

                //close the stream in case it already exists
                closeStream(mediaType);
                continue;
            }

            StreamConnector connector = getStreamConnector(mediaType);

            // determine the direction that we need to announce.
            MediaDirection remoteDirection = SdpUtils
                            .getDirection(mediaDescription);
            MediaDirection direction = devDirection
                            .getDirectionForAnswer(remoteDirection);

            // check whether we will be exchanging any RTP extensions.
            List<RTPExtension> offeredRTPExtensions
                    = SdpUtils.extractRTPExtensions(
                            mediaDescription, this.rtpExtensionsRegistry);

            List<RTPExtension> supportedExtensions
                    = getExtensionsForType(mediaType);

            List<RTPExtension> rtpExtensions = intersectRTPExtensions(
                            offeredRTPExtensions, supportedExtensions);

            // create the corresponding stream...
            initStream(connector, dev, supportedFormats.get(0), target,
                      direction);

            // create the answer description
            answerDescriptions.add(createMediaDescription(
                dev.getFormat(),
                supportedFormats, connector,
                direction, rtpExtensions));

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
            throw new OperationFailedException("Offer contained no valid "
                            + "media descriptions.",
                            OperationFailedException.ILLEGAL_ARGUMENT);

        return answerDescriptions;
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
    private List<RTPExtension> intersectRTPExtensions(
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
    private List<RTPExtension> getExtensionsForType(MediaType type)
    {
        MediaDevice dev = getDefaultDevice(type);

        return dev.getSupportedExtensions();
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
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s.
     *
     * @param answer the SDP answer that we'd like to handle.
     *
     * @throws OperationFailedException if we fail to handle <tt>answer</tt> for
     * reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>answer</tt>.
     */
    public void processAnswer(String answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        processAnswer(SdpUtils.parseSdpString(answer));
    }

    /**
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s.
     *
     * @param answer the SDP <tt>SessionDescription</tt>.
     *
     * @throws OperationFailedException if we fail to handle <tt>answer</tt> for
     * reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>answer</tt>.
     */
    private void processAnswer(SessionDescription answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        this.remoteSess = answer;

        List<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(answer);

        this.setCallInfoURL(SdpUtils.getCallInfoURL(answer));

        for ( MediaDescription mediaDescription : remoteDescriptions)
        {
            MediaType mediaType = SdpUtils.getMediaType(mediaDescription);
            //stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, answer);
            // not target port - try next media description
            if(target.getDataAddress().getPort() == 0)
            {
                    closeStream(mediaType);
                    continue;
            }
            List<MediaFormat> supportedFormats = SdpUtils.extractFormats(
                            mediaDescription, dynamicPayloadTypes);

            MediaDevice dev = getDefaultDevice(mediaType);
            MediaDirection devDirection
                = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            devDirection
                = devDirection.and(getDirectionUserPreference(mediaType));

            if (supportedFormats.isEmpty())
            {
                //remote party must have messed up our SDP. throw an exception.
                ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "Remote party sent an invalid SDP answer.",
                     OperationFailedException.ILLEGAL_ARGUMENT, null, logger);
            }

            StreamConnector connector = getStreamConnector(mediaType);

            //determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);

            MediaDirection direction
                = devDirection.getDirectionForAnswer(remoteDirection);

            // create the corresponding stream...
            initStream(connector, dev, supportedFormats.get(0), target,
                                direction);
        }
    }

    /**
     * Returns our own user name so that we could use it when generating SDP o=
     * fields.
     *
     * @return our own user name so that we could use it when generating SDP o=
     * fields.
     */
    private String getUserName()
    {
        return peer.getProtocolProvider().getAccountID().getUserID();
    }


    /**
     * Generates an SDP <tt>MediaDescription</tt> for <tt>MediaDevice</tt>
     * taking account the local streaming preference for the corresponding
     * media type.
     *
     * @param captureFormat capture <tt>MediaFormat</tt> of the device.
     * @param formats the list of <tt>MediaFormats</tt> that we'd like to
     * advertise.
     * @param connector the <tt>StreamConnector</tt> that we will be using
     * for the stream represented by the description we are creating.
     * @param direction the <tt>MediaDirection</tt> that we'd like to establish
     * the stream in.
     * @param extensions the list of <tt>RTPExtension</tt>s that we'd like to
     * advertise in the <tt>MediaDescription</tt>.
     *
     * @return a newly created <tt>MediaDescription</tt> representing streams
     * that we'd be able to handle.
     *
     * @throws OperationFailedException if generating the
     * <tt>MediaDescription</tt> fails for some reason.
     */
    private MediaDescription createMediaDescription(
                                             MediaFormat captureFormat,
                                             List<MediaFormat>  formats,
                                             StreamConnector    connector,
                                             MediaDirection     direction,
                                             List<RTPExtension> extensions )
        throws OperationFailedException
    {
        return SdpUtils.createMediaDescription(
           captureFormat, formats, connector,
           direction, extensions,
           dynamicPayloadTypes, rtpExtensionsRegistry);
    }

    /**
     * Creates a media <tt>StreamConnector</tt>. The method takes into account
     * the minimum and maximum media port boundaries.
     *
     * @return a new <tt>StreamConnector</tt>.
     *
     * @throws OperationFailedException if we fail binding the the sockets.
     */
    private StreamConnector createStreamConnector()
        throws OperationFailedException
    {
        NetworkAddressManagerService nam
                            = SipActivator.getNetworkAddressManagerService();

        InetAddress intendedDestination = peer.getProtocolProvider()
            .getIntendedDestination(peer.getPeerAddress());

        InetAddress localHostForPeer = nam.getLocalHost(intendedDestination);

        //make sure our port numbers reflect the configuration service settings
        initializePortNumbers();

        //create the RTP socket.
        DatagramSocket rtpSocket = null;
        try
        {
            rtpSocket = nam.createDatagramSocket( localHostForPeer,
                            nextMediaPortToTry, minMediaPort, maxMediaPort);
        }
        catch (Exception exc)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to allocate the network ports necessary for the call.",
                OperationFailedException.INTERNAL_ERROR, exc, logger);
        }

        //make sure that next time we don't try to bind on occupied ports
        nextMediaPortToTry = rtpSocket.getLocalPort() + 1;

        //create the RTCP socket, preferably on the port following our RTP one.
        DatagramSocket rtcpSocket = null;
        try
        {
            rtcpSocket = nam.createDatagramSocket(localHostForPeer,
                            nextMediaPortToTry, minMediaPort, maxMediaPort);
        }
        catch (Exception exc)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to allocate the network ports necessary for the call.",
                OperationFailedException.INTERNAL_ERROR, exc, logger);
        }

        //make sure that next time we don't try to bind on occupied ports
        nextMediaPortToTry = rtcpSocket.getLocalPort() + 1;

        if (nextMediaPortToTry > maxMediaPort -1)// take RTCP into account.
            nextMediaPortToTry = minMediaPort;

        //create the RTCP socket
        DefaultStreamConnector connector = new DefaultStreamConnector(
                        rtpSocket, rtcpSocket);

        return connector;
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
    MediaStream getStream(MediaType mediaType)
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
     * Returns the <tt>StreamConnector</tt> instance that this media handler
     * should use for streams of the specified <tt>mediaType</tt>. The method
     * would also create a new <tt>StreamConnector</tt> if no connector has
     * been initialized for this <tt>mediaType</tt> yet or in case one
     * of its underlying sockets has been closed.
     *
     * @param mediaType the MediaType that we'd like to create a connector for.
     *
     * @return this media handler's <tt>StreamConnector</tt> for the specified
     * <tt>mediaType</tt>.
     *
     * @throws OperationFailedException in case we failed to initialize our
     * connector.
     */
    private StreamConnector getStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        if (mediaType == MediaType.AUDIO)
        {
            if ( audioStreamConnector == null
                 || audioStreamConnector.getDataSocket().isClosed()
                 || audioStreamConnector.getControlSocket().isClosed())
            {
                audioStreamConnector = createStreamConnector();
            }

            return audioStreamConnector;
        }
        else
        {
            if ( videoStreamConnector == null
                 || videoStreamConnector.getDataSocket().isClosed()
                 || videoStreamConnector.getControlSocket().isClosed())
            {
                videoStreamConnector = createStreamConnector();
            }

            return videoStreamConnector;
        }
    }

    /**
     * (Re)Sets the <tt>minPortNumber</tt> and <tt>maxPortNumber</tt> to their
     * defaults or to the values specified in the <tt>ConfigurationService</tt>.
     */
    private void initializePortNumbers()
    {
        //first reset to default values
        minMediaPort = 5000;
        maxMediaPort = 6000;

        //then set to anything the user might have specified.
        String minPortNumberStr = SipActivator.getConfigurationService()
            .getString(OperationSetBasicTelephony
                            .MIN_MEDIA_PORT_NUMBER_PROPERTY_NAME);

        if (minPortNumberStr != null)
        {
            try
            {
                minMediaPort = Integer.parseInt(minPortNumberStr);
            }
            catch (NumberFormatException ex)
            {
                logger.warn(minPortNumberStr
                            + " is not a valid min port number value. "
                            + "using min port " + minMediaPort);
            }
        }

        String maxPortNumberStr = SipActivator.getConfigurationService()
            .getString(OperationSetBasicTelephony
                            .MAX_MEDIA_PORT_NUMBER_PROPERTY_NAME);

        if (maxPortNumberStr != null)
        {
            try
            {
                maxMediaPort = Integer.parseInt(maxPortNumberStr);
            }
            catch (NumberFormatException ex)
            {
                logger.warn(maxPortNumberStr
                            + " is not a valid max port number value. "
                            +"using max port " + maxMediaPort,
                            ex);
            }
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
     * Returns a <tt>URL</tt> pointing ta a location with call control
     * information for this peer or <tt>null</tt> if no such <tt>URL</tt> is
     * available for the <tt>CallPeer</tt> associated with this handler..
     *
     * @return a <tt>URL</tt> link to a location with call information or a
     * call control web interface related to our <tt>CallPeer</tt> or
     * <tt>null</tt> if no such <tt>URL</tt>.
     */
    public URL getCallInfoURL()
    {
        return callInfoURL;
    }

    /**
     * Specifies a <tt>URL</tt> pointing to a location with call control
     * information for this peer.
     *
     * @param callInfolURL a <tt>URL</tt> link to a location with call
     * information or a call control web interface related to the
     * <tt>CallPeer</tt> that we are associated with.
     */
    private void setCallInfoURL(URL callInfolURL)
    {
        this.callInfoURL = callInfolURL;
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
     * Gets the last-known remote SSRC of the audio <tt>MediaStream</tt> of this
     * instance.
     *
     * @return the last-known remote SSRC of the audio <tt>MediaStream</tt> of
     * this instance
     */
    long getAudioRemoteSSRC()
    {
        return audioRemoteSSRC;
    }

    /**
     * Sets the RTP media stream that this instance uses to stream audio to a
     * specific <tt>AudioMediaStream</tt>.
     *
     * @param audioStream the <tt>AudioMediaStream</tt> to be set as the RTP
     * media stream that this instance uses to stream audio
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
     * Sets the SAS verifications state of the call.
     *
     * @param isVerified the new SAS verification status
     */
    void setSasVerified(boolean isVerified )
    {
        if(audioStream != null)
            audioStream.getZrtpControl().setSASVerification(isVerified);

        if(videoStream != null)
            videoStream.getZrtpControl().setSASVerification(isVerified);
    }

    /**
     * Gets the secure state of the call. If both audio and video is secured.
     *
     * @return the call secure state
     */
    boolean isSecure()
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
    void startZrtpMultistream(byte[] multiStreamData)
    {
        if(videoStream != null)
            videoStream.getZrtpControl().setMultistream(multiStreamData);
    }
}
