/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.*;
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
     * A temporarily single transport manager that we use for generating
     * addresses until we properly implement both ICE and Raw UDP managers.
     */
    private RawUdpTransportManager transportManager
                                                = new RawUdpTransportManager();

    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandlerJabberImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(CallPeerMediaHandlerJabberImpl.class.getName());

    /**
     * The last ( and maybe only ) session description that we generated for
     * our own media.
     */
    private List<ContentPacketExtension> localContentList;

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
     * Returns the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers.
     *
     * @param peer the CallPeer that we would contact.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    @Override
    protected InetAddress getIntendedDestination(CallPeerJabberImpl peer)
    {
        //XXX returning a dummy address until we implement real transport
        //management so that we keep the Jingle Nodes guys happy!
        try
        {
            return InetAddress.getByAddress(new byte []{8,8,8,8});
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            return null;
        }
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
            RtpDescriptionPacketExtension description
                                    = JingleUtils.getRtpDescription(content);
            MediaType mediaType
                            = MediaType.parseString( description.getMedia() );

            List<MediaFormat> supportedFormats = JingleUtils.extractFormats(
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
            MediaDirection remoteDirection = JingleUtils.getDirection(content);
            MediaDirection direction = devDirection
                            .getDirectionForAnswer(remoteDirection);

            // intersect the MediaFormats of our device with remote ones
            List<MediaFormat> mutuallySupportedFormats
                = intersectFormats(supportedFormats, dev.getSupportedFormats());

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

            if (supportedFormats.isEmpty()
                || (devDirection == MediaDirection.INACTIVE)
                || (targetDataPort == 0))
            {
                // skip stream and continue. contrary to sip we don't seem to
                // need to send per-stream disabling answer and only one at the
                //end.

                //close the stream in case it already exists
                closeStream(mediaType);
                continue;
            }

            StreamConnector connector = getStreamConnector(mediaType);

            // create the corresponding stream...
            initStream(connector, dev, supportedFormats.get(0), target,
                      direction, rtpExtensions);

            // create the answer description
            answerContentList.add(JingleUtils.createDescription(
                content.getCreator(), content.getName(), content.getSenders(),
                mutuallySupportedFormats, rtpExtensions,
                getDynamicPayloadTypes(), getRtpExtensionsRegistry()));

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
            ProtocolProviderServiceJabberImpl
                .throwOperationFailedException("Offer contained no media "
                       + " formats or no valid media descriptions.",
                       OperationFailedException.ILLEGAL_ARGUMENT, null, logger);

        this.localContentList = answerContentList;
    }

    /**
     * Wraps up any ongoing candidate harvests and returns our response to the
     * last offer we've received, so that the peer could use it to send a
     * <tt>session-accept</tt>.
     *
     * @return  the last generated list of {@link ContentPacketExtension}s that
     * the call peer could use to send a <tt>session-accept</tt>.
     */
    protected List<ContentPacketExtension> generateSessionAccept()
    {
        //wrap up transport candidate harvesting and insert the final
        //transports in the content list.

        //XXX this is a temporary fix that would only work for Raw UDP as we
        //are trying to finish it to make Thiago happy

        return localContentList;
    }

    /**
     * Creates a <tt>List</tt> containing the {@link ContentPacketExtension}s of
     * the streams that this handler is prepared to initiate depending on
     * available <tt>MediaDevice</tt>s and local on-hold and video transmission
     * preferences.
     *
     * @param initiator indicates whether we are the party that's establishing
     * the call that the result content is going to be used in.
     *
     * @return a {@link List} containing the {@link ContentPacketExtension}s of
     * streams that this handler is prepared to initiate.
     *
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
     */
    public List<ContentPacketExtension> createContentList(boolean initiator)
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
                    ContentPacketExtension content
                        = createContentForOffer( initiator,
                            dev.getSupportedFormats(), direction,
                                dev.getSupportedExtensions());

                    //ZRTP
                    if(peer.getCall().isSipZrtpAttribute())
                    {
                        ZrtpControl control
                                        = getZrtpControls().get(mediaType);
                        if(control == null)
                        {
                            control = JabberActivator.getMediaService()
                                .createZrtpControl();
                            getZrtpControls().put(mediaType, control);
                        }

                        String helloHash = control.getHelloHash();
                        if(helloHash != null && helloHash.length() > 0)
                        {
                            ZrtpHashPacketExtension hash
                                = new ZrtpHashPacketExtension();
                            hash.setValue(helloHash);
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
        transportManager.startCandidateHarvest(mediaDescs, this);

        //XXX ideally we wouldn't wrapup that quickluy. we need to revisit this
        return transportManager.wrapupHarvest();
    }

    /**
     * Generates an SDP {@link ContentPacketExtension} for the specified
     * {@link MediaFormat} list, direction and RTP extensions taking account
     * the local streaming preference for the corresponding media type.
     *
     * @param initiator indicates whether we are the initiating party in the
     * call that this content will be used in.
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
                                        boolean            initiator,
                                        List<MediaFormat>  supportedFormats,
                                        MediaDirection     direction,
                                        List<RTPExtension> supportedExtensions)
    {
        return JingleUtils.createDescription(
            CreatorEnum.initiator,
            supportedFormats.get(0).getMediaType().toString(),
            JingleUtils.getSenders(direction, initiator),
            supportedFormats,
            supportedExtensions,
            getDynamicPayloadTypes(),
            getRtpExtensionsRegistry());
    }


}
