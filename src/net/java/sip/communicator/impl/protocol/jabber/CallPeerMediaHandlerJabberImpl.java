/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.lang.reflect.*;
import java.util.*;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

/**
 * An XMPP specific extension of the generic media handler.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class CallPeerMediaHandlerJabberImpl
    extends CallPeerMediaHandler<CallPeerJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandlerJabberImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerMediaHandlerJabberImpl.class);

    /**
     * The <tt>TransportManager</tt> implementation handling our address
     * management.
     */
    private TransportManagerJabberImpl transportManager;

    /**
     * The current description of the streams that we have going toward the
     * remote side. We use {@link LinkedHashMap}s to make sure that we preserve
     * the order of the individual content extensions.
     */
    private Map<String, ContentPacketExtension> localContentMap
        = new LinkedHashMap<String, ContentPacketExtension>();

    /**
     * The current description of the streams that the remote side has with us.
     * We use {@link LinkedHashMap}s to make sure that we preserve
     * the order of the individual content extensions.
     */
    private Map<String, ContentPacketExtension> remoteContentMap
        = new LinkedHashMap<String, ContentPacketExtension>();

    /**
     * Indicates whether the remote party has placed us on hold.
     */
    private boolean remotelyOnHold = false;

    /**
     * Indicates if the <tt>CallPeer</tt> will support </tt>inputevt</tt>
     * extension (i.e. will be able to be remote-controlled).
     */
    private boolean localInputEvtAware = false;

    /**
     * Whether other party is able to change video quality settings.
     * Normally its whether we have detected existence of imageattr in sdp.
     */
    private boolean supportQualityControls = false;

    /**
     * The current quality controls for this peer media handler if any.
     */
    private QualityControlWrapper qualityControls = null;

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerJabberImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandlerJabberImpl(CallPeerJabberImpl peer)
    {
        super(peer, peer);
        qualityControls = new QualityControlWrapper(peer);
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
     * Enable or disable <tt>inputevt</tt> support (remote-control).
     *
     * @param enable new state of inputevt support
     */
    public void setLocalInputEvtAware(boolean enable)
    {
        localInputEvtAware = enable;
    }

    /**
     * Get the remote content of a specific content type (like audio or video).
     *
     * @param contentType content type name
     * @return remote <tt>ContentPacketExtension</tt> or null if not found
     */
    public ContentPacketExtension getRemoteContent(String contentType)
    {
        for(String key : remoteContentMap.keySet())
        {
            ContentPacketExtension content = remoteContentMap.get(key);
            RtpDescriptionPacketExtension description
                = JingleUtils.getRtpDescription(content);

            if(description.getMedia().equals(contentType))
                return content;
        }
        return null;
    }

    /**
     * Get the local content of a specific content type (like audio or video).
     *
     * @param contentType content type name
     * @return remote <tt>ContentPacketExtension</tt> or null if not found
     */
    public ContentPacketExtension getLocalContent(String contentType)
    {
        for(String key : localContentMap.keySet())
        {
            ContentPacketExtension content = localContentMap.get(key);
            RtpDescriptionPacketExtension description
                = JingleUtils.getRtpDescription(content);

            if(description.getMedia().equals(contentType))
                return content;
        }
        return null;
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
    public void processOffer(List<ContentPacketExtension> offer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        // prepare to generate answers to all the incoming descriptions
        List<ContentPacketExtension> answer
            = new ArrayList<ContentPacketExtension>(offer.size());
        boolean atLeastOneValidDescription = false;

        for (ContentPacketExtension content : offer)
        {
            remoteContentMap.put(content.getName(), content);

            RtpDescriptionPacketExtension description
                = JingleUtils.getRtpDescription(content);
            MediaType mediaType
                = MediaType.parseString( description.getMedia() );

            List<MediaFormat> remoteFormats
                = JingleUtils.extractFormats(
                        description,
                        getDynamicPayloadTypes());

            MediaDevice dev = getDefaultDevice(mediaType);

            MediaDirection devDirection
                = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            devDirection
                = devDirection.and(getDirectionUserPreference(mediaType));

            // determine the direction that we need to announce.
            MediaDirection remoteDirection = JingleUtils.getDirection(
                                            content, getPeer().isInitiator());
            MediaDirection direction = devDirection
                            .getDirectionForAnswer(remoteDirection);

            // intersect the MediaFormats of our device with remote ones
            List<MediaFormat> mutuallySupportedFormats
                = intersectFormats(remoteFormats, dev.getSupportedFormats());

            // check whether we will be exchanging any RTP extensions.
            List<RTPExtension> offeredRTPExtensions
                    = JingleUtils.extractRTPExtensions(
                            description, this.getRtpExtensionsRegistry());

            List<RTPExtension> supportedExtensions
                    = getExtensionsForType(mediaType);

            List<RTPExtension> rtpExtensions = intersectRTPExtensions(
                            offeredRTPExtensions, supportedExtensions);

            // transport
            /*
             * RawUdpTransportPacketExtension extends
             * IceUdpTransportPacketExtension so getting
             * IceUdpTransportPacketExtension should suffice.
             */
            IceUdpTransportPacketExtension transport
                = content.getFirstChildOfType(
                        IceUdpTransportPacketExtension.class);

            // stream target
            MediaStreamTarget target = null;

            try
            {
                target = JingleUtils.extractDefaultTarget(content);
            }
            catch(IllegalArgumentException e)
            {
                logger.warn("Fail to extract default target", e);
            }

            // according to XEP-176, transport element in session-initiate
            // "MAY instead be empty (with each candidate to be sent as the
            // payload of a transport-info message)".
            int targetDataPort = (target == null && transport != null) ? -1 :
                (target != null) ? target.getDataAddress().getPort() : 0;

            /*
             * TODO If the offered transport is not supported, attempt to
             * fall back to a supported one using transport-replace.
             */
            setTransportManager(transport.getNamespace());

            if (mutuallySupportedFormats.isEmpty()
                || (devDirection == MediaDirection.INACTIVE)
                || (targetDataPort == 0))
            {
                // skip stream and continue. contrary to sip we don't seem to
                // need to send per-stream disabling answer and only one at the
                // end.

                //close the stream in case it already exists
                closeStream(mediaType);
                continue;
            }

            // create the answer description
            ContentPacketExtension ourContent
                = JingleUtils.createDescription(
                        content.getCreator(),
                        content.getName(),
                        JingleUtils.getSenders(
                                direction,
                                !getPeer().isInitiator()),
                        mutuallySupportedFormats,
                        rtpExtensions,
                        getDynamicPayloadTypes(),
                        getRtpExtensionsRegistry());

            // ZRTP
            if(getPeer().getCall().isSipZrtpAttribute())
            {
                MediaTypeSrtpControl key =
                    new MediaTypeSrtpControl(mediaType, SrtpControlType.ZRTP);
                SrtpControl control = getSrtpControls().get(key);
                if(control == null)
                {
                    control = JabberActivator.getMediaService()
                        .createZrtpControl();
                    getSrtpControls().put(key, control);
                }

                String helloHash[] = ((ZrtpControl)control).getHelloHashSep();

                if(helloHash != null && helloHash[1].length() > 0)
                {
                    EncryptionPacketExtension encryption = new
                        EncryptionPacketExtension();
                    ZrtpHashPacketExtension hash
                        = new ZrtpHashPacketExtension();
                    hash.setVersion(helloHash[0]);
                    hash.setValue(helloHash[1]);

                    encryption.addChildExtension(hash);
                    RtpDescriptionPacketExtension rtpDescription =
                        JingleUtils.getRtpDescription(ourContent);
                    rtpDescription.setEncryption(encryption);
                }
            }

            // got an content which have inputevt, it means that peer requests
            // a desktop sharing session so tell it we support inputevt
            if(content.getChildExtensionsOfType(InputEvtPacketExtension.class)
                    != null)
            {
                ourContent.addChildExtension(new InputEvtPacketExtension());
            }

            answer.add(ourContent);
            localContentMap.put(content.getName(), ourContent);

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
            offer,
            answer,
            new TransportInfoSender()
                    {
                        public void sendTransportInfo(
                                Iterable<ContentPacketExtension> contents)
                        {
                            getPeer().sendTransportInfo(contents);
                        }
                    });

        /*
         * While it may sound like we can completely eliminate the post-pickup
         * delay by waiting for the connectivity establishment to finish, it may
         * not be possible in all cases. We are the Jingle session responder so,
         * in the case of the ICE UDP transport, we are not the controlling ICE
         * Agent and we cannot be sure when the controlling ICE Agent will
         * perform the nomination. It could, for example, choose to wait for our
         * session-accept to perform the nomination which will deadlock us if we
         * have chosen to wait for the connectivity establishment to finish
         * before we begin ringing and send session-accept.
         */
        getTransportManager().startConnectivityEstablishment(offer);
    }

    /**
     * Wraps up any ongoing candidate harvests and returns our response to the
     * last offer we've received, so that the peer could use it to send a
     * <tt>session-accept</tt>.
     *
     * @return  the last generated list of {@link ContentPacketExtension}s that
     * the call peer could use to send a <tt>session-accept</tt>.
     *
     * @throws OperationFailedException if we fail to configure the media stream
     */
    public Iterable<ContentPacketExtension> generateSessionAccept()
        throws OperationFailedException
    {
        TransportManagerJabberImpl transportManager = getTransportManager();
        Iterable<ContentPacketExtension> sessAccept
            = transportManager.wrapupCandidateHarvest();
        CallPeerJabberImpl peer = getPeer();

        //user answered an incoming call so we go through whatever content
        //entries we are initializing and init their corresponding streams

        // First parse content so we know how may streams,
        // and what type of content we have
        Map<ContentPacketExtension,
            RtpDescriptionPacketExtension> contents
                = new HashMap<ContentPacketExtension,
                              RtpDescriptionPacketExtension>();
        for(ContentPacketExtension ourContent : sessAccept)
        {
            RtpDescriptionPacketExtension description
                            = JingleUtils.getRtpDescription(ourContent);
            contents.put(ourContent, description);
        }

        boolean masterStreamSet = false;
        for(Map.Entry<ContentPacketExtension, RtpDescriptionPacketExtension> en
                : contents.entrySet())
        {
            ContentPacketExtension ourContent = en.getKey();

            RtpDescriptionPacketExtension description = en.getValue();
            MediaType type = MediaType.parseString(description.getMedia());

            // stream connector
            StreamConnector connector
                = transportManager.getStreamConnector(type);

            //the device this stream would be reading from and writing to.
            MediaDevice dev = getDefaultDevice(type);

            // stream target
            MediaStreamTarget target = transportManager.getStreamTarget(type);

            //stream direction
            MediaDirection direction
                = JingleUtils.getDirection(ourContent, !peer.isInitiator());

            // if we answer with video, tell remote peer that video direction is
            // sendrecv, and whether video device can capture(send)
            if(type == MediaType.VIDEO && isLocalVideoTransmissionEnabled()
                && dev.getDirection().allowsSending())
            {
               direction = MediaDirection.SENDRECV;
               ourContent.setSenders(SendersEnum.both);
            }

            //let's now see what was the format we announced as first and
            //configure the stream with it.
            ContentPacketExtension theirContent
                = this.remoteContentMap.get(ourContent.getName());
            RtpDescriptionPacketExtension theirDescription
                = JingleUtils.getRtpDescription(theirContent);
            MediaFormat format = null;

            for(PayloadTypePacketExtension payload
                    : theirDescription.getPayloadTypes())
            {
                format
                    = JingleUtils.payloadTypeToMediaFormat(
                            payload,
                            getDynamicPayloadTypes());

                if(format != null)
                    break;
            }

            if(format == null)
            {
                ProtocolProviderServiceJabberImpl.
                    throwOperationFailedException(
                        "No matching codec.",
                        OperationFailedException.ILLEGAL_ARGUMENT,
                        null,
                        logger);
            }

            //extract the extensions that we are advertising:
            // check whether we will be exchanging any RTP extensions.
            List<RTPExtension> rtpExtensions
                    = JingleUtils.extractRTPExtensions(
                            description, this.getRtpExtensionsRegistry());

            Map<String, String> adv = format.getAdvancedAttributes();
            if(adv != null)
            {
                for(Map.Entry<String, String> f : adv.entrySet())
                {
                    if(f.getKey().equals("imageattr"))
                        supportQualityControls = true;
                }
            }

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                if(contents.size() > 1)
                {
                    if(type.equals(MediaType.AUDIO))
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

            // create the corresponding stream...
            initStream(ourContent.getName(), connector, dev, format, target,
                            direction, rtpExtensions, masterStream);

            // if remote peer requires inputevt, notify UI to capture mouse
            // and keyboard events
            if(ourContent.getChildExtensionsOfType(
                            InputEvtPacketExtension.class)
                    != null)
            {
                OperationSetDesktopSharingClientJabberImpl client
                    = (OperationSetDesktopSharingClientJabberImpl)
                        peer.getProtocolProvider().getOperationSet(
                                OperationSetDesktopSharingClient.class);

                if (client != null)
                    client.fireRemoteControlGranted(peer);
            }
        }
        return sessAccept;
    }

    /**
     * Creates a {@link ContentPacketExtension}s of the streams for a
     * specific <tt>MediaDevice</tt>.
     *
     * @param dev <tt>MediaDevice</tt>
     * @return the {@link ContentPacketExtension}s of stream that this
     * handler is prepared to initiate.
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like problems with device interaction, allocating ports, etc.
     */
    private ContentPacketExtension createContent(MediaDevice dev)
        throws OperationFailedException
    {
        MediaDirection direction
            = dev.getDirection().and(
                    getDirectionUserPreference(dev.getMediaType()));

        if(isLocallyOnHold())
            direction = direction.and(MediaDirection.SENDONLY);

        QualityPreset sendQualityPreset = null;
        QualityPreset receiveQualityPreset = null;

        if(qualityControls != null)
        {
            // the one we will send is the one the other part has announced
            // as receive
            sendQualityPreset = qualityControls.getRemoteReceivePreset();
            // the one we want to receive is the setting that remote
            // can send
            receiveQualityPreset = qualityControls.getRemoteSendMaxPreset();
        }
        if(direction != MediaDirection.INACTIVE)
        {
            ContentPacketExtension content = createContentForOffer(
                    dev.getSupportedFormats(sendQualityPreset,
                        receiveQualityPreset), direction,
                    dev.getSupportedExtensions());

            //ZRTP
            if(getPeer().getCall().isSipZrtpAttribute())
            {
                MediaTypeSrtpControl key =
                    new MediaTypeSrtpControl(dev.getMediaType(),
                        SrtpControlType.ZRTP);
                SrtpControl control = getSrtpControls().get(key);
                if(control == null)
                {
                    control
                        = JabberActivator.getMediaService().createZrtpControl();
                    getSrtpControls().put(key, control);
                }

                String helloHash[] = ((ZrtpControl)control).getHelloHashSep();

                if(helloHash != null && helloHash[1].length() > 0)
                {
                    EncryptionPacketExtension encryption = new
                        EncryptionPacketExtension();
                    ZrtpHashPacketExtension hash
                        = new ZrtpHashPacketExtension();

                    hash.setVersion(helloHash[0]);
                    hash.setValue(helloHash[1]);

                    encryption.addChildExtension(hash);
                    RtpDescriptionPacketExtension description =
                        JingleUtils.getRtpDescription(content);
                    description.setEncryption(encryption);
                }
            }

            return content;
        }
        return null;
    }

    /**
     * Creates a {@link ContentPacketExtension} for a particular stream.
     *
     * @param mediaType <tt>MediaType</tt> of the content
     * @return a {@link ContentPacketExtension}
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
     */
    public ContentPacketExtension createContentForMedia(
        MediaType mediaType)
        throws OperationFailedException
    {
        MediaDevice dev = getDefaultDevice(mediaType);

        if (dev != null)
        {
            ContentPacketExtension content = createContent(dev);

            return content;
        }

        return null;
    }

    /**
     * Creates a <tt>List</tt> containing the {@link ContentPacketExtension}s of
     * the streams of a specific <tt>MediaType</tt> that this handler is
     * prepared to initiate depending on available <tt>MediaDevice</tt>s and
     * local on-hold and video transmission preferences.
     *
     * @param mediaType <tt>MediaType</tt> of the content
     * @return a {@link List} containing the {@link ContentPacketExtension}s of
     * streams that this handler is prepared to initiate.
     *
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
     */
    public List<ContentPacketExtension> createContentList(MediaType mediaType)
        throws OperationFailedException
    {
        MediaDevice dev = getDefaultDevice(mediaType);
        List<ContentPacketExtension> mediaDescs
                                    = new ArrayList<ContentPacketExtension>();

        if (dev != null)
        {
            ContentPacketExtension content = createContent(dev);

            if(content != null)
                mediaDescs.add(content);
        }

        //fail if all devices were inactive
        if(mediaDescs.isEmpty())
        {
            ProtocolProviderServiceJabberImpl
                .throwOperationFailedException(
                    "We couldn't find any active Audio/Video devices and "
                        + "couldn't create a call",
                    OperationFailedException.GENERAL_ERROR, null, logger);
        }

        TransportInfoSender transportInfoSender =
            getTransportManager().getXmlNamespace().equals(
                ProtocolProviderServiceJabberImpl.URN_GOOGLE_TRANSPORT_P2P)
                ? new TransportInfoSender()
                  {
                      public void sendTransportInfo(
                          Iterable<ContentPacketExtension> contents)
                      {
                          getPeer().sendTransportInfo(contents);
                      }
                  }
                : null;

        //now add the transport elements
        return harvestCandidates(null, mediaDescs, transportInfoSender);
    }

    /**
     * Creates a <tt>List</tt> containing the {@link ContentPacketExtension}s of
     * the streams that this handler is prepared to initiate depending on
     * available <tt>MediaDevice</tt>s and local on-hold and video transmission
     * preferences.
     *
     * @return a {@link List} containing the {@link ContentPacketExtension}s of
     * streams that this handler is prepared to initiate.
     *
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like problems with device interaction, allocating ports, etc.
     */
    public List<ContentPacketExtension> createContentList()
        throws OperationFailedException
    {
        //Audio Media Description
        List<ContentPacketExtension> mediaDescs
            = new ArrayList<ContentPacketExtension>();

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
                    ContentPacketExtension content
                        = createContentForOffer(
                                dev.getSupportedFormats(),
                                direction,
                                dev.getSupportedExtensions());

                    //ZRTP
                    if(getPeer().getCall().isSipZrtpAttribute())
                    {
                        MediaTypeSrtpControl key =
                            new MediaTypeSrtpControl(mediaType,
                                SrtpControlType.ZRTP);
                        SrtpControl control = getSrtpControls().get(key);
                        if(control == null)
                        {
                            control = JabberActivator.getMediaService()
                                .createZrtpControl();
                            getSrtpControls().put(key, control);
                        }

                        String helloHash[] =
                            ((ZrtpControl) control).getHelloHashSep();

                        if(helloHash != null && helloHash[1].length() > 0)
                        {
                            EncryptionPacketExtension encryption = new
                                EncryptionPacketExtension();
                            ZrtpHashPacketExtension hash
                                = new ZrtpHashPacketExtension();
                            hash.setVersion(helloHash[0]);
                            hash.setValue(helloHash[1]);

                            encryption.addChildExtension(hash);
                            RtpDescriptionPacketExtension description =
                                JingleUtils.getRtpDescription(content);
                            description.setEncryption(encryption);
                        }
                    }

                    /* we request a desktop sharing session so add the inputevt
                     * extension in the "video" content
                     */
                    RtpDescriptionPacketExtension description
                        = JingleUtils.getRtpDescription(content);
                    if(description.getMedia().equals(MediaType.VIDEO.toString())
                            && localInputEvtAware)
                    {
                        content.addChildExtension(
                                new InputEvtPacketExtension());
                    }

                    mediaDescs.add(content);
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

        TransportInfoSender transportInfoSender =
            getTransportManager().getXmlNamespace().equals(
                ProtocolProviderServiceJabberImpl.URN_GOOGLE_TRANSPORT_P2P)
                ? new TransportInfoSender()
                  {
                      public void sendTransportInfo(
                          Iterable<ContentPacketExtension> contents)
                      {
                          getPeer().sendTransportInfo(contents);
                      }
                  }
                : null;

        //now add the transport elements
        return harvestCandidates(null, mediaDescs, transportInfoSender);
    }

    /**
     * Generates an Jingle {@link ContentPacketExtension} for the specified
     * {@link MediaFormat} list, direction and RTP extensions taking account
     * the local streaming preference for the corresponding media type.
     *
     * @param supportedFormats the list of <tt>MediaFormats</tt> that we'd
     * like to advertise.
     * @param direction the <tt>MediaDirection</tt> that we'd like to establish
     * the stream in.
     * @param supportedExtensions the list of <tt>RTPExtension</tt>s that we'd
     * like to advertise in the <tt>MediaDescription</tt>.
     *
     * @return a newly created {@link ContentPacketExtension} representing
     * streams that we'd be able to handle.
     */
    private ContentPacketExtension createContentForOffer(
                                        List<MediaFormat>  supportedFormats,
                                        MediaDirection     direction,
                                        List<RTPExtension> supportedExtensions)
    {
        ContentPacketExtension content
            = JingleUtils.createDescription(
                    CreatorEnum.initiator,
                    supportedFormats.get(0).getMediaType().toString(),
                    JingleUtils.getSenders(direction, !getPeer().isInitiator()),
                    supportedFormats,
                    supportedExtensions,
                    getDynamicPayloadTypes(),
                    getRtpExtensionsRegistry());

        this.localContentMap.put(content.getName(), content);
        return content;
    }

    /**
     * Reinitialize all media contents.
     *
     * @throws OperationFailedException if we fail to handle <tt>content</tt>
     * for reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>content</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    public void reinitAllContents()
            throws OperationFailedException,
            IllegalArgumentException
    {
        boolean masterStreamSet = false;
        for(String key : remoteContentMap.keySet())
        {
            ContentPacketExtension ext = remoteContentMap.get(key);

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                RtpDescriptionPacketExtension description
                    = JingleUtils.getRtpDescription(ext);
                MediaType mediaType
                    = MediaType.parseString( description.getMedia() );

                if(remoteContentMap.size() > 1)
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

            if(ext != null)
                processContent(ext, false, masterStream);
        }
    }

    /**
     * Reinitialize a media content such as video.
     *
     * @param name name of the Jingle content
     * @param content media content
     * @param modify if it correspond to a content-modify for resolution change
     * @throws OperationFailedException if we fail to handle <tt>content</tt>
     * for reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>content</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    public void reinitContent(
            String name,
            ContentPacketExtension content,
            boolean modify)
        throws OperationFailedException,
               IllegalArgumentException
    {
        ContentPacketExtension ext = remoteContentMap.get(name);

        if(ext != null)
        {
            if(modify)
            {
                processContent(content, modify, false);
                remoteContentMap.put(name, content);
            }
            else
            {
                ext.setSenders(content.getSenders());
                processContent(ext, modify, false);
                remoteContentMap.put(name, ext);
            }
        }
    }

    /**
     * Removes a media content with a specific name from the session represented
     * by this <tt>CallPeerMediaHandlerJabberImpl</tt> and closes its associated
     * media stream.
     *
     * @param name the name of the media content to be removed from this session
     */
    public void removeContent(String name)
    {
        removeContent(localContentMap, name);
        removeContent(remoteContentMap, name);
        getTransportManager().removeContent(name);
    }

    /**
     * Removes a media content with a specific name from the session represented
     * by this <tt>CallPeerMediaHandlerJabberImpl</tt> and closes its associated
     * media stream.
     *
     * @param contentMap the <tt>Map</tt> in which the specified <tt>name</tt>
     * has an association with the media content to be removed
     * @param name the name of the media content to be removed from this session
     */
    private void removeContent(
            Map<String, ContentPacketExtension> contentMap,
            String name)
    {
        ContentPacketExtension content = contentMap.remove(name);

        if (content != null)
        {
            RtpDescriptionPacketExtension description
                = JingleUtils.getRtpDescription(content);
            String media = description.getMedia();

            if (media != null)
                closeStream(MediaType.parseString(media));
        }
    }

    /**
     * Process a <tt>ContentPacketExtension</tt> and initialize its
     * corresponding <tt>MediaStream</tt>.
     *
     * @param content a <tt>ContentPacketExtension</tt>
     * @param modify if it correspond to a content-modify for resolution change
     * @param masterStream whether the stream to be used as master
     * @throws OperationFailedException if we fail to handle <tt>content</tt>
     * for reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>content</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    private void processContent(ContentPacketExtension content, boolean modify,
                                boolean masterStream)
        throws OperationFailedException,
               IllegalArgumentException
    {
        RtpDescriptionPacketExtension description
            = JingleUtils.getRtpDescription(content);
        MediaType mediaType
            = MediaType.parseString( description.getMedia() );

        //stream target
        TransportManagerJabberImpl transportManager = getTransportManager();
        MediaStreamTarget target = transportManager.getStreamTarget(mediaType);

        if (target == null)
            target = JingleUtils.extractDefaultTarget(content);

        // no target port - try next media description
        if((target == null) || (target.getDataAddress().getPort() == 0))
        {
            closeStream(mediaType);
            return;
        }

        List<MediaFormat> supportedFormats = JingleUtils.extractFormats(
                        description, getDynamicPayloadTypes());

        MediaDevice dev = getDefaultDevice(mediaType);

        if(dev == null)
        {
            closeStream(mediaType);
            return;
        }

        MediaDirection devDirection
            = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

        // Take the preference of the user with respect to streaming
        // mediaType into account.
        devDirection
            = devDirection.and(getDirectionUserPreference(mediaType));

        if (supportedFormats.isEmpty())
        {
            //remote party must have messed up our Jingle description.
            //throw an exception.
            ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                "Remote party sent an invalid Jingle answer.",
                 OperationFailedException.ILLEGAL_ARGUMENT, null, logger);
        }

        StreamConnector connector
            = transportManager.getStreamConnector(mediaType);

        //determine the direction that we need to announce.
        MediaDirection remoteDirection
            = JingleUtils.getDirection(content, getPeer().isInitiator());

        MediaDirection direction
            = devDirection.getDirectionForAnswer(remoteDirection);

        // update the RTP extensions that we will be exchanging.
        List<RTPExtension> remoteRTPExtensions
                = JingleUtils.extractRTPExtensions(
                        description, getRtpExtensionsRegistry());

        List<RTPExtension> supportedExtensions
                = getExtensionsForType(mediaType);

        List<RTPExtension> rtpExtensions = intersectRTPExtensions(
                        remoteRTPExtensions, supportedExtensions);

        Map<String, String> adv = supportedFormats.get(0).getAdvancedAttributes();
        if(adv != null)
        {
            for(Map.Entry<String, String> f : adv.entrySet())
            {
                if(f.getKey().equals("imageattr"))
                {
                    supportQualityControls = true;
                }
            }
        }

        // check for options from remote party and set them locally
        if(mediaType.equals(MediaType.VIDEO) && modify)
        {
            QualityPreset sendQualityPreset = null;
            QualityPreset receiveQualityPreset = null;

            // update stream
            MediaStream stream = getStream(MediaType.VIDEO);

            if(stream != null && dev != null)
            {
                List<MediaFormat> fmts = supportedFormats;

                if(fmts.size() > 0)
                {
                    MediaFormat fmt = fmts.get(0);

                    ((VideoMediaStream)stream).updateQualityControl(
                        fmt.getAdvancedAttributes());
                }
            }

            if(qualityControls != null)
            {
                receiveQualityPreset = qualityControls.getRemoteReceivePreset();
                sendQualityPreset = qualityControls.getRemoteSendMaxPreset();

                supportedFormats
                    = (dev == null)
                        ? null
                        : intersectFormats(
                            supportedFormats,
                            dev.getSupportedFormats(
                                sendQualityPreset, receiveQualityPreset));
            }
        }

        // create the corresponding stream...
        initStream(content.getName(), connector, dev,
                supportedFormats.get(0), target, direction, rtpExtensions,
                masterStream);
    }

    /**
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s.
     *
     * @param answer the Jingle answer
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
    public void processAnswer(List<ContentPacketExtension> answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        /*
         * The answer given in session-accept may contain transport-related
         * information compatible with that carried in transport-info.
         */
        processTransportInfo(answer);
        
        boolean masterStreamSet = false;
        for (ContentPacketExtension content : answer)
        {
            remoteContentMap.put(content.getName(), content);

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                RtpDescriptionPacketExtension description
                    = JingleUtils.getRtpDescription(content);
                MediaType mediaType
                    = MediaType.parseString( description.getMedia() );

                if(answer.size() > 1)
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

            processContent(content, false, masterStream);
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
    protected TransportManagerJabberImpl getTransportManager()
    {
        if (transportManager == null)
        {
            CallPeerJabberImpl peer = getPeer();

            if (peer.isInitiator())
            {
                throw new IllegalStateException(
                        "The initiator is expected to specify the transport"
                            + " in their offer.");
            }
            else
            {
                ScServiceDiscoveryManager discoveryManager
                    = peer.getProtocolProvider().getDiscoveryManager();
                DiscoverInfo peerDiscoverInfo = peer.getDiscoverInfo();

                String domain = StringUtils.parseServer(
                    peer.getAddress());

                // We use Google P2P transport if both conditions are satisfied:
                // - both peers have Google P2P transport in their features;
                // - at least one peer is a Gmail or Google Apps account.
                //
                // Otherwise we go for an ICE-UDP transport
                boolean isGoogle =
                    peer.getProtocolProvider().isGmailOrGoogleAppsAccount() ||
                        ProtocolProviderServiceJabberImpl.
                            isGmailOrGoogleAppsAccount(domain);

                // Put Google P2P transport first. We will take it
                // for a node that support both ICE-UDP and Google P2P to use
                // Google relay.
                if (discoveryManager.includesFeature(
                    ProtocolProviderServiceJabberImpl
                            .URN_GOOGLE_TRANSPORT_P2P)
                        && ((peerDiscoverInfo == null)
                                || peerDiscoverInfo.containsFeature(
                                ProtocolProviderServiceJabberImpl
                                    .URN_GOOGLE_TRANSPORT_P2P)) && isGoogle)
                {
                    transportManager = new P2PTransportManager(peer);
                }
                else if (discoveryManager.includesFeature(
                            ProtocolProviderServiceJabberImpl
                                .URN_XMPP_JINGLE_ICE_UDP_1)
                        && ((peerDiscoverInfo == null)
                                || peerDiscoverInfo.containsFeature(
                                        ProtocolProviderServiceJabberImpl
                                            .URN_XMPP_JINGLE_ICE_UDP_1)))
                {
                    transportManager = new IceUdpTransportManager(peer);
                }
                else if (discoveryManager.includesFeature(
                            ProtocolProviderServiceJabberImpl
                                .URN_XMPP_JINGLE_RAW_UDP_0)
                        && ((peerDiscoverInfo == null)
                                || peerDiscoverInfo.containsFeature(
                                        ProtocolProviderServiceJabberImpl
                                            .URN_XMPP_JINGLE_RAW_UDP_0)))
                {
                    transportManager = new RawUdpTransportManager(peer);
                }
                else if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "No known Jingle transport supported"
                                + " by Jabber call peer "
                                + peer);
                }
            }
        }
        return transportManager;
    }

    /**
     * Sets the <tt>TransportManager</tt> implementation to handle our address
     * management by Jingle transport XML namespace.
     *
     * @param xmlns the Jingle transport XML namespace specifying the
     * <tt>TransportManager</tt> implementation type to be set on this instance
     * to handle our address management
     * @throws IllegalArgumentException if the specified <tt>xmlns</tt> does not
     * specify a (supported) <tt>TransportManager</tt> implementation type
     */
    private void setTransportManager(String xmlns)
        throws IllegalArgumentException
    {
        // Is this really going to be an actual change?
        if ((transportManager != null)
                && transportManager.getXmlNamespace().equals(xmlns))
        {
            return;
        }

        CallPeerJabberImpl peer = getPeer();

        if (!peer
                .getProtocolProvider()
                    .getDiscoveryManager().includesFeature(xmlns))
        {
            throw new IllegalArgumentException(
                    "Unsupported Jingle transport " + xmlns);
        }

        /*
         * TODO The transportManager is going to be changed so it may need to be
         * disposed prior to the change.
         */

        if (xmlns.equals(
                ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_ICE_UDP_1))
        {
            transportManager = new IceUdpTransportManager(peer);
        }
        else if (xmlns.equals(
                ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_RAW_UDP_0))
        {
            transportManager = new RawUdpTransportManager(peer);
        }
        else if (xmlns.equals(
            ProtocolProviderServiceJabberImpl.URN_GOOGLE_TRANSPORT_P2P))
        {
            transportManager = new P2PTransportManager(peer);
        }
        else
        {
            throw new IllegalArgumentException(
                    "Unsupported Jingle transport " + xmlns);
        }
    }

    /**
     * Acts upon a notification received from the remote party indicating that
     * they've put us on/off hold.
     *
     * @param onHold <tt>true</tt> if the remote party has put us on hold
     * and <tt>false</tt> if they've just put us off hold.
     */
    public void setRemotelyOnHold(boolean onHold)
    {
        this.remotelyOnHold = onHold;

        MediaStream audioStream = getStream(MediaType.AUDIO);
        MediaStream videoStream = getStream(MediaType.VIDEO);

        if(remotelyOnHold)
        {
            if(audioStream != null)
            {
                audioStream.setDirection(audioStream.getDirection()
                            .and(MediaDirection.RECVONLY));
            }
            if(videoStream != null)
            {
                videoStream.setDirection(videoStream.getDirection()
                            .and(MediaDirection.RECVONLY));
            }
        }
        else
        {
            //off hold - make sure that we re-enable sending if that's
            if(audioStream != null)
                calculatePostHoldDirection(audioStream);
            if(videoStream != null)
                calculatePostHoldDirection(videoStream);
        }
    }

    /**
     * Determines and sets the direction that a stream, which has been place on
     * hold by the remote party, would need to go back to after being
     * re-activated. If the stream is not currently on hold (i.e. it is still
     * sending media), this method simply returns its current direction.
     *
     * @param stream the {@link MediaStreamTarget} whose post-hold direction
     * we'd like to determine.
     *
     * @return the {@link MediaDirection} that we need to set on <tt>stream</tt>
     * once it is reactivate.
     */
    private MediaDirection calculatePostHoldDirection(MediaStream stream)
    {
        MediaDirection streamDirection = stream.getDirection();

        if(streamDirection.allowsSending())
            return streamDirection;

        //when calculating a direction we need to take into account 1) what
        //direction the remote party had asked for before putting us on hold,
        //2) what the user preference is for the stream's media type, 3) our
        //local hold status, 4) the direction supported by the device this
        //stream is reading from.

        //1. check what the remote party originally told us (from our persp.)
        ContentPacketExtension content = remoteContentMap.get(stream.getName());

        MediaDirection postHoldDir = JingleUtils.getDirection(content,
                        !getPeer().isInitiator());

        //2. check the user preference.
        MediaDevice device = stream.getDevice();
        postHoldDir
            = postHoldDir.and(
                    getDirectionUserPreference(device.getMediaType()));

        //3. check our local hold status.
        if(isLocallyOnHold())
            postHoldDir.and(MediaDirection.SENDONLY);

        //4. check the device direction.
        postHoldDir = postHoldDir.and(device.getDirection());

        stream.setDirection(postHoldDir);

        return postHoldDir;
    }

    /**
     * Gathers local candidate addresses.
     *
     * @param remote the media descriptions received from the remote peer if any
     * or <tt>null</tt> if <tt>local</tt> represents an offer from the local
     * peer to be sent to the remote peer
     * @param local the media descriptions sent or to be sent from the local
     * peer to the remote peer. If <tt>remote</tt> is <tt>null</tt>,
     * <tt>local</tt> represents an offer from the local peer to be sent to the
     * remote peer
     * @param transportInfoSender the <tt>TransportInfoSender</tt> to be used by
     * this <tt>TransportManagerJabberImpl</tt> to send <tt>transport-info</tt>
     * <tt>JingleIQ</tt>s from the local peer to the remote peer if this
     * <tt>TransportManagerJabberImpl</tt> wishes to utilize
     * <tt>transport-info</tt>
     * @return the media descriptions of the local peer after the local
     * candidate addresses have been gathered as returned by
     * {@link TransportManagerJabberImpl#wrapupCandidateHarvest()}
     * @throws OperationFailedException if anything goes wrong while starting or
     * wrapping up the gathering of local candidate addresses
     */
    private List<ContentPacketExtension> harvestCandidates(
            List<ContentPacketExtension> remote,
            List<ContentPacketExtension> local,
            TransportInfoSender transportInfoSender)
        throws OperationFailedException
    {
        TransportManagerJabberImpl transportManager = getTransportManager();

        if (remote == null)
        {
            /*
             * We'll be harvesting candidates in order to make an offer so it
             * doesn't make sense to send them in transport-info.
             */
            if (!transportManager.getXmlNamespace().equals(
                ProtocolProviderServiceJabberImpl.URN_GOOGLE_TRANSPORT_P2P) &&
                transportInfoSender != null)
                throw new IllegalArgumentException("transportInfoSender");

            transportManager.startCandidateHarvest(local, transportInfoSender);
        }
        else
        {
            transportManager.startCandidateHarvest(
                    remote,
                    local,
                    transportInfoSender);
        }

        /*
         * XXX Ideally, we wouldn't wrap up that quickly. We need to revisit
         * this.
         */
        return transportManager.wrapupCandidateHarvest();
    }

    /**
     * Processes the transport-related information provided by the remote
     * <tt>peer</tt> in a specific set of <tt>ContentPacketExtension</tt>s.
     *
     * @param contents the <tt>ContentPacketExtenion</tt>s provided by the
     * remote <tt>peer</tt> and containing the transport-related information to
     * be processed
     * @throws OperationFailedException if anything goes wrong while processing
     * the transport-related information provided by the remote <tt>peer</tt> in
     * the specified set of <tt>ContentPacketExtension</tt>s
     */
    public void processTransportInfo(Iterable<ContentPacketExtension> contents)
        throws OperationFailedException
    {
        if (getTransportManager().startConnectivityEstablishment(contents))
        {
            //wrapupConnectivityEstablishment();
        }
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
     * Notifies the associated <tt>TransportManagerJabberImpl</tt> that it
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
        TransportManagerJabberImpl transportManager = getTransportManager();

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
     * Returns the quality control for video calls if any.
     * @return the implemented quality control.
     */
    public QualityControl getQualityControl()
    {
        if(supportQualityControls)
        {
            return qualityControls;
        }
        else
        {
            // we have detected that its not supported and return null
            // and control ui won't be visible
            return null;
        }
    }

    /**
     * Sometimes as initing a call with custom preset can set and we force
     * that quality controls is supported.
     * @param value whether quality controls is supported..
     */
    public void setSupportQualityControls(boolean value)
    {
        this.supportQualityControls = value;
    }

    /**
     * Closes the <tt>CallPeerMediaHandler</tt>.
     */
    public synchronized void close()
    {
        super.close();

        OperationSetDesktopSharingClientJabberImpl client
            = (OperationSetDesktopSharingClientJabberImpl)
                getPeer().getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingClient.class);

        if (client != null)
            client.fireRemoteControlRevoked(getPeer());
    }
}
