/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.lang.reflect.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * An Google Talk specific extension of the generic media handler.
 *
 * @author Sebastien Vincent
 */
public class CallPeerMediaHandlerGTalkImpl
    extends CallPeerMediaHandler<CallPeerGTalkImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandlerGTalkImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerMediaHandlerGTalkImpl.class);

    /**
     * Google Talk name for the audio part.
     */
    public static final String AUDIO_RTP = "rtp";

    /**
     * Google Talk name for the video part.
     */
    public static final String VIDEO_RTP = "video_rtp";

    /**
     * The current description of the streams that we have going toward the
     * remote side. We use {@link LinkedHashMap}s to make sure that we preserve
     * the order of the individual content extensions.
     */
    private Map<String, List<PayloadTypePacketExtension>> localContentMap
        = new LinkedHashMap<String, List<PayloadTypePacketExtension> >();

    /**
     * The current description of the streams that the remote side has with us.
     * We use {@link LinkedHashMap}s to make sure that we preserve
     * the order of the individual content extensions.
     */
    private Map<String, List<PayloadTypePacketExtension> > remoteContentMap
        = new LinkedHashMap<String, List<PayloadTypePacketExtension> >();

    /**
     * The <tt>TransportManager</tt> implementation handling our address
     * management.
     */
    private TransportManagerGTalkImpl transportManager;

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerGTalkImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandlerGTalkImpl(CallPeerGTalkImpl peer)
    {
        super(peer, peer);
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
    protected void throwOperationFailedException(
            String message,
            int errorCode,
            Throwable cause)
        throws OperationFailedException
    {
        ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                message,
                errorCode,
                cause,
                logger);
    }

    /**
     * Parses and handles the specified <tt>offer</tt> and returns a content
     * extension representing the current state of this media handler. This
     * method MUST only be called when <tt>offer</tt> is the first session
     * description that this <tt>MediaHandler</tt> is seeing.
     *
     * @param offer the offer that we'd like to parse, handle and get an answer
     * for.
     *
     * @throws OperationFailedException if we have a problem satisfying the
     * description received in <tt>offer</tt> (e.g. failed to open a device or
     * initialize a stream ...).
     * @throws IllegalArgumentException if there's a problem with
     * <tt>offer</tt>'s format or semantics.
     */
    public void processOffer(RtpDescriptionPacketExtension offer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        List<PayloadTypePacketExtension> payloadTypes = offer.getPayloadTypes();
        boolean atLeastOneValidDescription = false;
        List<PayloadTypePacketExtension> answer =
            new ArrayList<PayloadTypePacketExtension>();
        List<MediaFormat> remoteFormats = JingleUtils.extractFormats(
                    offer,
                    getDynamicPayloadTypes());
        boolean isAudio = false;
        boolean isVideo = false;

        for(PayloadTypePacketExtension ext : payloadTypes)
        {
            if(ext.getNamespace().equals(
                    SessionIQProvider.GTALK_AUDIO_NAMESPACE))
            {
                isAudio = true;
            }
            else if(ext.getNamespace().equals(
                    SessionIQProvider.GTALK_VIDEO_NAMESPACE))
            {
                isVideo = true;
            }
        }

        EncryptionPacketExtension encryptionPacketExtension
            = offer.getFirstChildOfType(EncryptionPacketExtension.class);
        if(encryptionPacketExtension != null)
        {
            ZrtpHashPacketExtension zrtpHashPacketExtension =
                encryptionPacketExtension.getFirstChildOfType(
                    ZrtpHashPacketExtension.class);

            if(zrtpHashPacketExtension != null
                && zrtpHashPacketExtension.getValue() != null)
            {
                addAdvertisedEncryptionMethod(SrtpControlType.ZRTP);
            }
        }

        for(MediaType mediaType : MediaType.values())
        {
            if(!(isAudio && mediaType == MediaType.AUDIO) &&
                    !(isVideo && mediaType == MediaType.VIDEO))
            {
                continue;
            }

            remoteContentMap.put(mediaType.toString(), payloadTypes);

            MediaDevice dev = getDefaultDevice(mediaType);

            MediaDirection devDirection
                = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            devDirection
                = devDirection.and(getDirectionUserPreference(mediaType));

            // intersect the MediaFormats of our device with remote ones
            List<MediaFormat> mutuallySupportedFormats
                = intersectFormats(remoteFormats, dev.getSupportedFormats());

            List<PayloadTypePacketExtension> contents
                = createPayloadTypesForOffer(
                    getNameForMediaType(mediaType),
                    mutuallySupportedFormats);
            answer.addAll(contents);

            localContentMap.put(mediaType.toString(), answer);

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
        {
            ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                    "Offer contained no media formats"
                        + " or no valid media descriptions.",
                    OperationFailedException.ILLEGAL_ARGUMENT,
                    null,
                    logger);
        }

        /*
         * In order to minimize post-pickup delay, start establishing the
         * connectivity prior to ringing.
         */
        harvestCandidates(
            answer,
            new CandidatesSender()
            {
                public void sendCandidates(
                        Iterable<GTalkCandidatePacketExtension> candidates)
                {
                            getPeer().sendCandidates(candidates);
                }
            });
    }

    /**
     * Wraps up any ongoing candidate harvests and returns our response to the
     * last offer we've received, so that the peer could use it to send a
     * <tt>accept</tt>.
     *
     * @param initStream true to initialize stream, false to do nothing else
     * @return  the last generated list of
     * {@link RtpDescriptionPacketExtension}s that the call peer could use to
     * send a <tt>accept</tt>.
     *
     * @throws OperationFailedException if we fail to configure the media stream
     */
    public RtpDescriptionPacketExtension generateSessionAccept(
        boolean initStream)
        throws OperationFailedException
    {
        RtpDescriptionPacketExtension description =
            new RtpDescriptionPacketExtension();
        List<PayloadTypePacketExtension> lst = localContentMap.get("audio");

        description.setNamespace(SessionIQProvider.GTALK_AUDIO_NAMESPACE);
        
        boolean masterStreamSet = false;
        for(MediaType mediaType : MediaType.values())
        {
            MediaFormat format = null;
            String ns = getNamespaceForMediaType(mediaType);
            String mediaName = getNameForMediaType(mediaType);

            for(PayloadTypePacketExtension ext : lst)
            {
                if(ext.getNamespace().equals(ns))
                {
                    if(mediaType.equals(MediaType.VIDEO))
                    {
                        description.setNamespace(SessionIQProvider.
                                GTALK_VIDEO_NAMESPACE);

                        getPeer().getCall().setLocalVideoAllowed(
                            true, MediaUseCase.CALL);

                        ext.setAttribute("width", 320);
                        ext.setAttribute("height", 200);
                        ext.setAttribute("framerate", 30);
                    }

                    format = JingleUtils.payloadTypeToMediaFormat(
                        ext,
                        getDynamicPayloadTypes());
                    description.addPayloadType(ext);

                    if(format != null)
                        break;
                }
            }

            if(format == null)
                continue;

            if(!initStream)
                continue;

            // stream connector
            StreamConnector connector
                = transportManager.getStreamConnector(mediaType);

            //the device this stream would be reading from and writing to.
            MediaDevice dev = getDefaultDevice(mediaType);

            // stream target
            MediaStreamTarget target = transportManager.getStreamTarget(
                    mediaType);

            List<RTPExtension> rtpExtensions =
                new ArrayList<RTPExtension>();
            MediaDirection direction = MediaDirection.SENDRECV;

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                if(MediaType.values().length > 1)
                {
                    if(mediaType.equals(MediaType.AUDIO))
                    {
                        masterStream = true;
                        masterStreamSet = true;
                    }
                }
                else
                {
                    masterStream = true;
                    masterStreamSet = true;
                }
            }

            initStream(mediaName, connector, dev, format, target,
                    direction, rtpExtensions, masterStream);
        }

        return description;
    }

    /**
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s.
     *
     * @param answer the Google Talk answer
     *
     * @throws OperationFailedException if we fail to handle <tt>answer</tt> for
     * reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>answer</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    public void processAnswer(RtpDescriptionPacketExtension answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        List<PayloadTypePacketExtension> lst = answer.getPayloadTypes();

        EncryptionPacketExtension encryptionPacketExtension
            = answer.getFirstChildOfType(EncryptionPacketExtension.class);
        if(encryptionPacketExtension != null)
        {
            ZrtpHashPacketExtension zrtpHashPacketExtension =
                encryptionPacketExtension.getFirstChildOfType(
                    ZrtpHashPacketExtension.class);

            if(zrtpHashPacketExtension != null
                && zrtpHashPacketExtension.getValue() != null)
            {
                addAdvertisedEncryptionMethod(SrtpControlType.ZRTP);
            }
        }

        boolean masterStreamSet = true;
        for(MediaType mediaType : MediaType.values())
        {
            String ns = getNamespaceForMediaType(mediaType);
            String mediaName = getNameForMediaType(mediaType);
            MediaFormat format = null;

            for(PayloadTypePacketExtension ext : lst)
            {
                if(ext.getNamespace().equals(ns))
                {
                    format = JingleUtils.payloadTypeToMediaFormat(
                            ext,
                            getDynamicPayloadTypes());
                    if(format != null)
                        break;
                }
            }

            if(format == null)
                continue;

            // stream connector
            StreamConnector connector
                = transportManager.getStreamConnector(mediaType);

            //the device this stream would be reading from and writing to.
            MediaDevice dev = getDefaultDevice(mediaType);

            // stream target
            MediaStreamTarget target = transportManager.getStreamTarget(
                    mediaType);

            List<RTPExtension> rtpExtensions = new ArrayList<RTPExtension>();
            MediaDirection direction = MediaDirection.SENDRECV;

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                if(MediaType.values().length > 1)
                {
                    if(mediaType.equals(MediaType.AUDIO))
                    {
                        masterStream = true;
                        masterStreamSet = true;
                    }
                }
                else
                {
                    masterStream = true;
                    masterStreamSet = true;
                }
            }

            initStream(mediaName, connector, dev, format, target,
                    direction, rtpExtensions, masterStream);
        }
    }

    /**
     * Gets the <tt>TransportManager</tt> implementation handling our address
     * management.
     *
     * @return the <tt>TransportManager</tt> implementation handling our address
     * management
     * @see CallPeerMediaHandler#getTransportManager()
     */
    protected synchronized TransportManagerGTalkImpl getTransportManager()
    {
        if (transportManager == null)
        {
            /* Google Talk assumes to use ICE */
            CallPeerGTalkImpl peer = getPeer();

            // support for Google Talk
            transportManager = new TransportManagerGTalkImpl(peer);
        }
        return transportManager;
    }

    /**
     * Processes the transport-related information provided by the remote
     * <tt>peer</tt> in a specific set of <tt>CandidatePacketExtension</tt>s.
     *
     * @param candidates the <tt>CandidatePacketExtenion</tt>s provided by the
     * remote <tt>peer</tt> and containing the candidate-related information to
     * be processed
     * @throws OperationFailedException if anything goes wrong while processing
     * the candidate-related information provided by the remote <tt>peer</tt> in
     * the specified set of <tt>CandidatePacketExtension</tt>s
     */
    public void processCandidates(
            Iterable<GTalkCandidatePacketExtension> candidates)
        throws OperationFailedException
    {
        getTransportManager().startConnectivityEstablishment(candidates);
    }

    /**
     * Creates a <tt>List</tt> containing the {@link ContentPacketExtension}s of
     * the streams that this handler is prepared to initiate depending on
     * available <tt>MediaDevice</tt>s and local on-hold and video transmission
     * preferences.
     *
     * @return a <tt>RtpDescriptionPacketExtension</tt> that contains
     * list of <tt>PayloadTypePacketExtension</tt>
     *
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like problems with device interaction, allocating ports, etc.
     */
    public RtpDescriptionPacketExtension createDescription()
        throws OperationFailedException
    {
        RtpDescriptionPacketExtension description =
            new RtpDescriptionPacketExtension(
                    SessionIQProvider.GTALK_AUDIO_NAMESPACE);
        List<PayloadTypePacketExtension> mediaDescs
            = new ArrayList<PayloadTypePacketExtension>();
        boolean isVideo = false;

        for (MediaType mediaType : MediaType.values())
        {
            MediaDevice dev = getDefaultDevice(mediaType);

            if (dev != null)
            {
                MediaDirection direction = dev.getDirection().and(
                                getDirectionUserPreference(mediaType));

                if(isLocallyOnHold())
                    direction = direction.and(MediaDirection.SENDONLY);

                /*
                 * If we're only able to receive, we don't have to offer it at
                 * all. For example, we have to offer audio and no video when we
                 * start an audio call.
                 */
                if (MediaDirection.RECVONLY.equals(direction))
                    direction = MediaDirection.INACTIVE;

                if(direction != MediaDirection.INACTIVE)
                {
                    List<PayloadTypePacketExtension> contents
                        = createPayloadTypesForOffer(
                                getNameForMediaType(mediaType),
                                dev.getSupportedFormats());

                    for(PayloadTypePacketExtension ext : contents)
                    {
                        /* if we add one "video" payload type, we must
                         * advertise it in the description IQ
                         */
                        if(!isVideo && mediaType.equals(MediaType.VIDEO))
                        {
                            description.setNamespace(SessionIQProvider.
                                    GTALK_VIDEO_NAMESPACE);

                            ext.setAttribute("width", 320);
                            ext.setAttribute("height", 200);
                            ext.setAttribute("framerate", 30);
                            isVideo = true;
                        }
                        description.addChildExtension(ext);
                        mediaDescs.add(ext);
                    }
                }
            }
        }

        //fail if all devices were inactive
        if(mediaDescs.isEmpty())
        {
            ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                    "We couldn't find any active Audio/Video devices"
                        + " and couldn't create a call",
                    OperationFailedException.GENERAL_ERROR,
                    null,
                    logger);
        }

        return description;
    }

    /**
     * Gathers local candidate addresses.
     *
     * @param local the media descriptions sent or to be sent from the local
     * peer to the remote peer.
     * @param candidatesSender the <tt>CandidatesSender</tt> to be used by
     * this <tt>TransportManagerGTalkImpl</tt> to send <tt>candidates</tt>
     * <tt>SessionIQ</tt>s from the local peer to the remote peer if this
     * <tt>TransportManagerGTalkImpl</tt> wishes to utilize <tt>candidates</tt>
     * @throws OperationFailedException if anything goes wrong while starting or
     * wrapping up the gathering of local candidate addresses
     */
    protected void harvestCandidates(
            List<PayloadTypePacketExtension> local,
            CandidatesSender candidatesSender)
        throws OperationFailedException
    {
        getTransportManager().startCandidateHarvest(
                local,
                candidatesSender);

        transportManager.wrapupCandidateHarvest();
    }

    /**
     * Get Google Talk name for the media type.
     *
     * @param mediaType media type
     * @return name for the media type
     * @throws IllegalArgumentException if media type is not audio or video
     */
    private static String getNameForMediaType(MediaType mediaType)
        throws IllegalArgumentException
    {
        if(mediaType == MediaType.AUDIO)
        {
            return AUDIO_RTP;
        }
        else if(mediaType == MediaType.VIDEO)
        {
            return VIDEO_RTP;
        }
        else
        {
            throw new IllegalArgumentException("not a mediatype");
        }
    }

    /**
     * Get Google Talk namespace for the media type.
     *
     * @param mediaType media type
     * @return namespace for the media type
     * @throws IllegalArgumentException if media type is not audio or video
     */
    private static String getNamespaceForMediaType(MediaType mediaType)
    {

        if(mediaType == MediaType.AUDIO)
        {
            return SessionIQProvider.GTALK_AUDIO_NAMESPACE;
        }
        else if(mediaType == MediaType.VIDEO)
        {
            return SessionIQProvider.GTALK_VIDEO_NAMESPACE;
        }
        else
        {
            throw new IllegalArgumentException("not a mediatype");
        }
    }

    /**
     * Create list of payload types for device.
     *
     * @param supportedFormats supported formats of a device
     * @param name name of payload type
     * @return list of payload types for this device
     */
    private List<PayloadTypePacketExtension> createPayloadTypesForOffer(
            String name,
            List<MediaFormat> supportedFormats)
    {
        List<PayloadTypePacketExtension> peList =
            new ArrayList<PayloadTypePacketExtension>();

        for(MediaFormat fmt : supportedFormats)
        {
            PayloadTypePacketExtension ext =
                JingleUtils.formatToPayloadType(fmt, getDynamicPayloadTypes());
            ext.setNamespace(name.equals(AUDIO_RTP) ?
                    SessionIQProvider.GTALK_AUDIO_NAMESPACE :
                    SessionIQProvider.GTALK_VIDEO_NAMESPACE);
            peList.add(ext);
        }

        return peList;
    }

    /**
     * Waits for the associated <tt>TransportManagerJabberImpl</tt> to conclude
     * any started connectivity establishment and then starts this
     * <tt>CallPeerMediaHandler</tt>.
     *
     * @throws IllegalStateException if no offer or answer has been provided or
     * generated earlier
     */
    @Override
    public void start()
        throws IllegalStateException
    {
        try
        {
            wrapupConnectivityEstablishment();
        }
        catch (OperationFailedException ofe)
        {
            throw new UndeclaredThrowableException(ofe);
        }
        super.start();
    }

    /**
     * Notifies the associated <tt>TransportManagerGTalkImpl</tt> that it
     * should conclude any connectivity establishment, waits for it to actually
     * do so and sets the <tt>connector</tt>s and <tt>target</tt>s of the
     * <tt>MediaStream</tt>s managed by this <tt>CallPeerMediaHandler</tt>.
     *
     * @throws OperationFailedException if anything goes wrong while setting the
     * <tt>connector</tt>s and/or <tt>target</tt>s of the <tt>MediaStream</tt>s
     * managed by this <tt>CallPeerMediaHandler</tt>
     */
    private void wrapupConnectivityEstablishment()
        throws OperationFailedException
    {
        TransportManagerGTalkImpl transportManager = getTransportManager();

        transportManager.wrapupConnectivityEstablishment();

        for (MediaType mediaType : MediaType.values())
        {
            MediaStream stream = getStream(mediaType);

            if (stream != null)
            {
                stream.setConnector(
                        transportManager.getStreamConnector(mediaType));
                stream.setTarget(transportManager.getStreamTarget(mediaType));
            }
        }
    }

    /**
     * Creates if necessary, and configures the stream that this
     * <tt>MediaHandler</tt> is using for the <tt>MediaType</tt> matching the
     * one of the <tt>MediaDevice</tt>. This method extends the one already
     * available by adding a stream name, corresponding to a stream's content
     * name.
     *
     * @param streamName the name of the stream as indicated in the XMPP
     * <tt>content</tt> element.
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
    protected MediaStream initStream(String               streamName,
                                     StreamConnector      connector,
                                     MediaDevice          device,
                                     MediaFormat          format,
                                     MediaStreamTarget    target,
                                     MediaDirection       direction,
                                     List<RTPExtension>   rtpExtensions,
                                     boolean masterStream)
        throws OperationFailedException
    {
        if(format instanceof VideoMediaFormat)
        {
            Map<String, String> settings = new HashMap<String, String>();

            // GTalk client has problem decoding H.264 stream with intra refresh
            settings.put("h264.intrarefresh", "false");
            // GTalk client cannot decode H.264 stream with "Main" profile
            settings.put("h264.profile", "baseline");
            format.setAdditionalCodecSettings(settings);
        }

        MediaStream stream
            = super.initStream(
                    connector,
                    device,
                    format,
                    target,
                    direction,
                    rtpExtensions,
                    masterStream);

        if(stream != null)
            stream.setName(streamName);

        return stream;
    }

    /**
     * Returns the {@link DynamicPayloadTypeRegistry} instance we are currently
     * using.
     *
     * @return the {@link DynamicPayloadTypeRegistry} instance we are currently
     * using.
     */
    @Override
    protected DynamicPayloadTypeRegistry getDynamicPayloadTypes()
    {
        DynamicPayloadTypeRegistry registry = super.getDynamicPayloadTypes();
        Map<Byte, String> mappings = new HashMap<Byte, String>();

        // GTalk will send its video content with PT 97 whenever it says
        // something else in codec negociation
        mappings.put(Byte.valueOf((byte)97), new String("H264"));

        registry.setOverridePayloadTypeMappings(mappings);

        return registry;
    }
}
