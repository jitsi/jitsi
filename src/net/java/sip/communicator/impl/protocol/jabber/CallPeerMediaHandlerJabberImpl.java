/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

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
 */
public class CallPeerMediaHandlerJabberImpl
    extends CallPeerMediaHandler<CallPeerJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandlerJabberImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(CallPeerMediaHandlerJabberImpl.class.getName());

    /**
     * A temporarily single transport manager that we use for generating
     * addresses until we properly implement both ICE and Raw UDP managers.
     */
    private final TransportManagerJabberImpl transportManager;

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
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerSipImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandlerJabberImpl(CallPeerJabberImpl peer)
    {
        super(peer, peer);

        transportManager = new RawUdpTransportManager(peer);
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
    @Override
    protected void throwOperationFailedException(String message, int errorCode,
                    Throwable cause) throws OperationFailedException
    {
        ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                            message, errorCode, cause, logger);
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
            {
                return content;
            }
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
            {
                return content;
            }
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
                                     List<RTPExtension>   rtpExtensions)
        throws OperationFailedException
    {
        MediaStream stream = super.initStream(connector, device, format,
                        target, direction, rtpExtensions);

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
        List<ContentPacketExtension> answerContentList
                        = new ArrayList<ContentPacketExtension>(offer.size());

        boolean atLeastOneValidDescription = false;

        for (ContentPacketExtension content : offer)
        {
            remoteContentMap.put(content.getName(), content);

            RtpDescriptionPacketExtension description
                                    = JingleUtils.getRtpDescription(content);
            MediaType mediaType
                            = MediaType.parseString( description.getMedia() );

            List<MediaFormat> remoteFormats = JingleUtils.extractFormats(
                            description, getDynamicPayloadTypes());

            MediaDevice dev = getDefaultDevice(mediaType);

            MediaDirection devDirection = (dev == null)
                                                      ? MediaDirection.INACTIVE
                                                      : dev.getDirection();

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

            // stream target
            MediaStreamTarget target
                = JingleUtils.extractDefaultTarget(content);

            int targetDataPort = target.getDataAddress().getPort();

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
            ContentPacketExtension ourContent = JingleUtils.createDescription(
                content.getCreator(), content.getName(),
                JingleUtils.getSenders(direction, !getPeer().isInitiator()) ,
                mutuallySupportedFormats, rtpExtensions,
                getDynamicPayloadTypes(), getRtpExtensionsRegistry());

            // ZRTP
            if(getPeer().getCall().isSipZrtpAttribute())
            {
                ZrtpControl control = getZrtpControls().get(mediaType);
                if(control == null)
                {
                    control = JabberActivator.getMediaService()
                        .createZrtpControl();
                    getZrtpControls().put(mediaType, control);
                }

                String helloHash[] = control.getHelloHashSep();

                if(helloHash != null && helloHash[1].length() > 0)
                {
                    ZrtpHashPacketExtension hash
                    = new ZrtpHashPacketExtension();
                    hash.setVersion(helloHash[0]);
                    hash.setValue(helloHash[1]);

                    ourContent.addChildExtension(hash);
                }
            }

            answerContentList.add(ourContent);
            localContentMap.put(content.getName(), ourContent);

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
            ProtocolProviderServiceJabberImpl
                .throwOperationFailedException("Offer contained no media "
                       + " formats or no valid media descriptions.",
                       OperationFailedException.ILLEGAL_ARGUMENT, null, logger);

        //now, before we go, tell the transport manager to start our candidate
        //harvest
        getTransportManager().startCandidateHarvest(offer, answerContentList);
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
    protected List<ContentPacketExtension> generateSessionAccept()
        throws OperationFailedException
    {
        List<ContentPacketExtension> sessAccept
                                    = getTransportManager().wrapupHarvest();

        //user answered an incoming call so we go through whatever content
        //entries we are initializing and init their corresponding streams
        for(ContentPacketExtension ourContent : sessAccept)
        {
            RtpDescriptionPacketExtension description
                            = JingleUtils.getRtpDescription(ourContent);
            MediaType type = MediaType.parseString(description.getMedia());

            //
            StreamConnector connector
                = getTransportManager().getStreamConnector(type);

            //the device this stream would be reading from and writing to.
            MediaDevice dev = getDefaultDevice(type);

            // stream target
            ContentPacketExtension theirContent
                = this.remoteContentMap.get(ourContent.getName());
            MediaStreamTarget target
                = JingleUtils.extractDefaultTarget(theirContent);
            RtpDescriptionPacketExtension theirDescription
                = JingleUtils.getRtpDescription(theirContent);

            //stream direction
            MediaDirection direction = JingleUtils.getDirection(
                                       ourContent, !getPeer().isInitiator());

            //let's now see what was the format we announced as first and
            //configure the stream with it.
            MediaFormat format = null;
            List<PayloadTypePacketExtension> payloadTypes =
                theirDescription.getPayloadTypes();

            for(PayloadTypePacketExtension payload : payloadTypes)
            {
                format = JingleUtils.payloadTypeToMediaFormat(
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

            // create the corresponding stream...
            initStream(ourContent.getName(), connector, dev, format, target,
                            direction, rtpExtensions);
        }
        return sessAccept;
    }

    /**
     * Creates a {@link ContentPacketExtension}s of the streams for a
     * specific <tt>MediaDevice</tt>.
     *
     * @param type <tt>MediaDevice</tt>
     * @return the {@link ContentPacketExtension}s of stream that this
     * handler is prepared to initiate.
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
     */
    private ContentPacketExtension createContent(MediaDevice dev)
    {
        MediaDirection direction = dev.getDirection().and(
                        getDirectionUserPreference(
                            dev.getMediaType()));

        if(isLocallyOnHold())
            direction = direction.and(MediaDirection.SENDONLY);

        if(direction != MediaDirection.INACTIVE)
        {
            ContentPacketExtension content = createContentForOffer(
                    dev.getSupportedFormats(), direction,
                    dev.getSupportedExtensions());

            //ZRTP
            if(getPeer().getCall().isSipZrtpAttribute())
            {
                ZrtpControl control = getZrtpControls().get(dev.getMediaType());
                if(control == null)
                {
                    control = JabberActivator.getMediaService()
                        .createZrtpControl();
                    getZrtpControls().put(dev.getMediaType(), control);
                }

               String helloHash[] = control.getHelloHashSep();

                if(helloHash != null && helloHash[1].length() > 0)
                {
                    ZrtpHashPacketExtension hash
                        = new ZrtpHashPacketExtension();
                    hash.setVersion(helloHash[0]);
                    hash.setValue(helloHash[1]);

                    content.addChildExtension(hash);
                }
            }

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
     * @param type <tt>MediaType</tt> of the content
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

        //now add the transport elements
        getTransportManager().startCandidateHarvest(mediaDescs);

        //XXX ideally we wouldn't wrapup that quickly. we need to revisit this
        return getTransportManager().wrapupHarvest();
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
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
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

                if(direction != MediaDirection.INACTIVE)
                {
                    ContentPacketExtension content = createContentForOffer(
                            dev.getSupportedFormats(), direction,
                            dev.getSupportedExtensions());

                    //ZRTP
                    if(getPeer().getCall().isSipZrtpAttribute())
                    {
                        ZrtpControl control = getZrtpControls().get(mediaType);
                        if(control == null)
                        {
                            control = JabberActivator.getMediaService()
                                .createZrtpControl();
                            getZrtpControls().put(mediaType, control);
                        }

                        String helloHash[] = control.getHelloHashSep();

                        if(helloHash != null && helloHash[1].length() > 0)
                        {
                            ZrtpHashPacketExtension hash
                                = new ZrtpHashPacketExtension();
                            hash.setVersion(helloHash[0]);
                            hash.setValue(helloHash[1]);

                            content.addChildExtension(hash);
                        }
                    }

                    mediaDescs.add(content);
                }
            }
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

        //now add the transport elements
        getTransportManager().startCandidateHarvest(mediaDescs);

        //XXX ideally we wouldn't wrapup that quickly. we need to revisit this
        return getTransportManager().wrapupHarvest();
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
        ContentPacketExtension content = JingleUtils.createDescription(
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
        for(String key : remoteContentMap.keySet())
        {
            ContentPacketExtension ext = remoteContentMap.get(key);

            if(ext != null)
            {
                processContent(ext);
            }
        }
    }

    /**
     * Reinitialize a media content such as video.
     *
     * @param name name of the Jingle content
     * @param senders media direction
     * @throws OperationFailedException if we fail to handle <tt>content</tt>
     * for reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>content</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    public void reinitContent(String name,
            ContentPacketExtension.SendersEnum senders)
            throws OperationFailedException,
            IllegalArgumentException
    {
        ContentPacketExtension ext = remoteContentMap.get(name);

        if(ext != null)
        {
            ext.setSenders(senders);
            processContent(ext);
            remoteContentMap.put(name, ext);
        }
    }

    /**
     * Remove a media content and stop the corresponding stream.
     *
     * @param name of the Jingle content
     */
    public void removeLocalContent(String name)
    {
        ContentPacketExtension content = localContentMap.remove(name);

        if(content == null)
        {
            return;
        }

        RtpDescriptionPacketExtension description
            = JingleUtils.getRtpDescription(content);

        String media = description.getMedia();

        if(media != null )
        {
              MediaStream stream = getStream(MediaType.parseString(media));
              stream.stop();
              stream = null;
        }
    }

    /**
     * Remove a media content and stop the corresponding stream.
     *
     * @param name of the Jingle content
     */
    public void removeRemoteContent(String name)
    {
        ContentPacketExtension content = remoteContentMap.remove(name);

        if(content == null)
        {
            return;
        }

        RtpDescriptionPacketExtension description
            = JingleUtils.getRtpDescription(content);

        String media = description.getMedia();

        if(media != null )
        {
              MediaStream stream = getStream(MediaType.parseString(media));
              stream.stop();
              stream = null;
        }
    }

    /**
     * Process a <tt>ContentPacketExtension</tt> and initialize its
     * corresponding <tt>MediaStream</tt>.
     *
     * @param content a <tt>ContentPacketExtension</tt>
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
    private void processContent(ContentPacketExtension content)
        throws OperationFailedException,
        IllegalArgumentException
    {
        RtpDescriptionPacketExtension description
                                = JingleUtils.getRtpDescription(content);

        MediaType mediaType
                        = MediaType.parseString( description.getMedia() );

        //stream target
        MediaStreamTarget target
            = JingleUtils.extractDefaultTarget(content);

        // no target port - try next media description
        if(target.getDataAddress().getPort() == 0)
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
            = getTransportManager().getStreamConnector(mediaType);

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

        // create the corresponding stream...
        initStream(content.getName(), connector, dev,
                supportedFormats.get(0), target, direction, rtpExtensions);
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
        for ( ContentPacketExtension content : answer)
        {
            remoteContentMap.put(content.getName(), content);

            processContent(content);
        }
    }

    /**
     * Returns the transport manager that is handling our address management.
     *
     * @return the transport manager that is handling our address management.
     */
    @Override
    public TransportManagerJabberImpl getTransportManager()
    {
        return transportManager;
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
            {
                calculatePostHoldDirection(audioStream);
            }
            if(videoStream != null)
            {
                calculatePostHoldDirection(videoStream);
            }
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
        postHoldDir = postHoldDir
            .and(getDirectionUserPreference(device.getMediaType()));

        //3. check our local hold status.
        if(isLocallyOnHold())
            postHoldDir.and(MediaDirection.SENDONLY);

        //4. check the device direction.
        postHoldDir = postHoldDir.and(device.getDirection());

        stream.setDirection(postHoldDir);

        return postHoldDir;
    }
}
