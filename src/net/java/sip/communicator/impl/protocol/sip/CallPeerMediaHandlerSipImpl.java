/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.util.*;

import javax.sdp.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
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
public class CallPeerMediaHandlerSipImpl
    extends CallPeerMediaHandler<CallPeerSipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerMediaHandlerSipImpl.class);

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
     * A <tt>URL</tt> pointing to a location with call information or a call
     * control web interface related to the <tt>CallPeer</tt> that we are
     * associated with.
     */
    private URL callInfoURL = null;

    /**
     * A temporarily single transport manager that we use for generating
     * addresses until we properly implement both ICE and Raw UDP managers.
     */
    private final TransportManagerSipImpl transportManager;

    /**
     * Whether other party is able to change video quality settings.
     * Normally its whether we have detected existence of imageattr in sdp.
     */
    boolean supportQualityControls;

    /**
     * The current quality controls for this peer media handler if any.
     */
    private QualityControlsWrapper qualityControls;

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerSipImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandlerSipImpl(CallPeerSipImpl peer)
    {
        super(peer, peer);

        transportManager = new TransportManagerSipImpl(peer);
        qualityControls = new QualityControlsWrapper(peer);
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
        String userName
            = getPeer().getProtocolProvider().getAccountID().getUserID();

        SessionDescription sDes = SdpUtils.createSessionDescription(
            getTransportManager().getLastUsedLocalHost(), userName, mediaDescs);

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

        QualityPresets sendQualityPreset = null;
        QualityPresets receiveQualityPreset = null;

        if(qualityControls != null)
        {
            // the one we will send is the one the other part has announced
            // as receive
            sendQualityPreset = qualityControls.getRemoteReceivePreset();
            // the one we want to receive is the setting that remote
            // can send
            receiveQualityPreset = qualityControls.getRemoteSendMaxPreset();
        }

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
                    MediaDescription md =
                        createMediaDescription(
                           dev.getSupportedFormats(
                                   sendQualityPreset,
                                   receiveQualityPreset),
                           getTransportManager().getStreamConnector(mediaType),
                           direction,
                           dev.getSupportedExtensions());

                    try
                    {
                        // if we have setting for video preset lets
                        // send info for the desired framerate
                        if(receiveQualityPreset != null
                           && receiveQualityPreset.getFameRate() > 0)
                            md.setAttribute("framerate",
                                // doing only int frame rate for now
                                String.valueOf(
                                    (int)receiveQualityPreset.getFameRate()));
                    }
                    catch(SdpException e)
                    {
                        // do nothing in case of error.
                    }

                    updateMediaDescriptionForZrtp(mediaType, md);

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
            sdescToUpdate, getTransportManager().getLastUsedLocalHost(),
            newMediaDescs);

        this.localSess = newOffer;
        return newOffer;
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
            getTransportManager().getLastUsedLocalHost(), getUserName(),
            answerDescriptions);

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
                previousAnswer, getTransportManager().getLastUsedLocalHost(),
                answerDescriptions);

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
            MediaType mediaType = null;
            try
            {
                mediaType = SdpUtils.getMediaType(mediaDescription);
            }
            catch (IllegalArgumentException iae)
            {
                //remote party offers a stream of a type that we don't support.
                //we'll disable it and move on.
                answerDescriptions.add(
                        SdpUtils.createDisablingAnswer(mediaDescription));
                continue;
            }

            List<MediaFormat> remoteFormats = SdpUtils.extractFormats(
                            mediaDescription, getDynamicPayloadTypes());

            MediaDevice dev = getDefaultDevice(mediaType);
            MediaDirection devDirection
                = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            devDirection
                = devDirection.and(getDirectionUserPreference(mediaType));

            // determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);
            MediaDirection direction
                = devDirection.getDirectionForAnswer(remoteDirection);

            // intersect the MediaFormats of our device with remote ones
            List<MediaFormat> mutuallySupportedFormats
                = (dev == null)
                    ? null
                    : intersectFormats(
                            remoteFormats,
                            dev.getSupportedFormats());

            // stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, offer);
            int targetDataPort = target.getDataAddress().getPort();

            if ((devDirection == MediaDirection.INACTIVE)
                    || (mutuallySupportedFormats == null)
                    || mutuallySupportedFormats.isEmpty()
                    || (targetDataPort == 0))
            {
                // mark stream as dead and go on bravely
                answerDescriptions.add(
                        SdpUtils.createDisablingAnswer(mediaDescription));

                //close the stream in case it already exists
                closeStream(mediaType);
                continue;
            }

            // check whether we will be exchanging any RTP extensions.
            List<RTPExtension> offeredRTPExtensions
                    = SdpUtils.extractRTPExtensions(
                            mediaDescription, this.getRtpExtensionsRegistry());
            List<RTPExtension> supportedExtensions
                    = getExtensionsForType(mediaType);
            List<RTPExtension> rtpExtensions
                = intersectRTPExtensions(
                        offeredRTPExtensions,
                        supportedExtensions);

            StreamConnector connector
                = getTransportManager().getStreamConnector(mediaType);

            // create the corresponding stream...
            MediaFormat fmt = findMediaFormat(remoteFormats,
                    mutuallySupportedFormats.get(0));
            initStream(connector, dev, fmt, target, direction, rtpExtensions);

            // check for options from remote party and set them locally
            if(mediaType.equals(MediaType.VIDEO))
            {
                QualityPresets sendQualityPreset = null;
                QualityPresets receiveQualityPreset = null;

                if(qualityControls != null)
                {
                    // the one we will send is the other party receive
                    sendQualityPreset =
                            qualityControls.getRemoteReceivePreset();
                    // the one we want to receive
                    receiveQualityPreset =
                            qualityControls.getRemoteSendMaxPreset();

                    mutuallySupportedFormats
                        = (dev == null)
                            ? null
                            : intersectFormats(
                                mutuallySupportedFormats,
                                dev.getSupportedFormats(
                                    sendQualityPreset, receiveQualityPreset));
                }

                supportQualityControls =
                    SdpUtils.containsAttribute(mediaDescription, "imageattr");

                float frameRate = -1;
                // check for frame rate setting
                try
                {
                    String frStr = mediaDescription.getAttribute("framerate");
                    if(frStr != null)
                        frameRate = Integer.parseInt(frStr);
                }
                catch(SdpParseException e)
                {
                    // do nothing
                }

                if(frameRate > 0)
                {
                    qualityControls.setMaxFrameRate(frameRate);
                }
            }

            MediaDescription md = createMediaDescription(
                mutuallySupportedFormats, connector, direction, rtpExtensions);

            updateMediaDescriptionForZrtp(mediaType, md);

            // create the answer description
            answerDescriptions.add(md);

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
            throw new OperationFailedException("Offer contained no valid "
                            + "media descriptions.",
                            OperationFailedException.ILLEGAL_ARGUMENT);

        return answerDescriptions;
    }

    /**
     * Updates the supplied description with zrtp hello hash if necessary.
     *
     * @param mediaType the media type.
     * @param md the description to be updated.
     */
    private void updateMediaDescriptionForZrtp(
        MediaType mediaType, MediaDescription md)
    {
        if(getPeer().getCall().isSipZrtpAttribute())
        {
            try
            {
                ZrtpControl control
                                = getZrtpControls().get(mediaType);
                if(control == null)
                {
                    control = SipActivator.getMediaService()
                        .createZrtpControl();
                    getZrtpControls().put(mediaType, control);
                }

                String helloHash = control.getHelloHash();
                if(helloHash != null && helloHash.length() > 0)
                    md.setAttribute("zrtp-hash", helloHash);

            } catch (SdpException ex)
            {
                logger.error("Cannot add zrtp-hash to sdp", ex);
            }
        }
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
     * the semantics of <tt>answer</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    private synchronized void processAnswer(SessionDescription answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        this.remoteSess = answer;

        List<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(answer);

        this.setCallInfoURL(SdpUtils.getCallInfoURL(answer));

        for ( MediaDescription mediaDescription : remoteDescriptions)
        {
            MediaType mediaType;
            try
            {
                mediaType = SdpUtils.getMediaType(mediaDescription);
            }
            catch(IllegalArgumentException iae)
            {
                logger.info("Remote party added to answer a media type that " +
                        "we don't understand. Ignoring stream.");
                continue;
            }

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
                            mediaDescription, getDynamicPayloadTypes());

            MediaDevice dev = getDefaultDevice(mediaType);

            if(dev == null)
            {
                closeStream(mediaType);
                continue;
            }

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
                    "Remote party sent an invalid SDP answer. The codecs in " +
                            "the answer are either not present or not " +
                            "supported",
                     OperationFailedException.ILLEGAL_ARGUMENT, null, logger);
            }

            StreamConnector connector
                = getTransportManager().getStreamConnector(mediaType);

            //determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);

            MediaDirection direction
                = devDirection.getDirectionForAnswer(remoteDirection);

            // take into account and the direction previously set/sent
            // to change directions properly, this is in case
            // where we set a direction and the other side don't agree with us
            // we need to be in the state we have offered
            if(isLocallyOnHold())
                direction = direction.and(MediaDirection.SENDONLY);

            // update the RTP extensions that we will be exchanging.
            List<RTPExtension> remoteRTPExtensions
                    = SdpUtils.extractRTPExtensions(
                            mediaDescription, getRtpExtensionsRegistry());

            List<RTPExtension> supportedExtensions
                    = getExtensionsForType(mediaType);

            List<RTPExtension> rtpExtensions = intersectRTPExtensions(
                            remoteRTPExtensions, supportedExtensions);

            // create the corresponding stream...
            initStream(connector, dev, supportedFormats.get(0), target,
                                direction, rtpExtensions);
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
        return getPeer().getProtocolProvider().getAccountID().getUserID();
    }

    /**
     * Generates an SDP <tt>MediaDescription</tt> for <tt>MediaDevice</tt>
     * taking account the local streaming preference for the corresponding
     * media type.
     *
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
                                             List<MediaFormat>  formats,
                                             StreamConnector    connector,
                                             MediaDirection     direction,
                                             List<RTPExtension> extensions )
        throws OperationFailedException
    {
        return SdpUtils.createMediaDescription(formats, connector,
           direction, extensions,
           getDynamicPayloadTypes(), getRtpExtensionsRegistry());
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
     * Returns a reference to the currently valid network address manager
     * service for use by this handler's generic ancestor.
     *
     * @return a reference to the currently valid {@link
     * NetworkAddressManagerService}
     */
    protected NetworkAddressManagerService getNetworkAddressManagerService()
    {
        return SipActivator.getNetworkAddressManagerService();
    }

    /**
     * Returns a reference to the currently valid media service for use by this
     * handler's generic ancestor.
     *
     * @return a reference to the currently valid {@link MediaService}
     */
    protected ConfigurationService getConfigurationService()
    {
        return SipActivator.getConfigurationService();
    }

    /**
     * Returns a reference to the currently valid media service for use by this
     * handler's generic ancestor.
     *
     * @return a reference to the currently valid {@link MediaService}
     */
    protected MediaService getMediaService()
    {
        return SipActivator.getMediaService();
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
    protected void throwOperationFailedException( String    message,
                                                  int       errorCode,
                                                  Throwable cause)
        throws OperationFailedException
    {
        ProtocolProviderServiceSipImpl.throwOperationFailedException(
                        message, errorCode, cause, logger);
    }

    /**
     * Returns the transport manager that is handling our address management.
     *
     * @return the transport manager that is handling our address management.
     */
    protected TransportManagerSipImpl getTransportManager()
    {
        return transportManager;
    }

    /**
     * Returns the quality control for video calls if any.
     * @return the implemented quality control.
     */
    public QualityControls getQualityControls()
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
}
