/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.util.*;

import javax.sdp.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;

import ch.imvs.sdes4j.srtp.*;

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
    private QualityControlWrapper qualityControls;

    /**
     * The lock we use to make sure that we won't be processing a second
     * offer/answer exchange while a .
     */
    private Object offerAnswerLock = new Object();

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
        qualityControls = new QualityControlWrapper(peer);
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
     * Creates a <tt>Vector</tt> containing the <tt>MediaDescription</tt>s of
     * streams that this handler is prepared to initiate depending on available
     * <tt>MediaDevice</tt>s and local on-hold and video transmission
     * preferences.
     *
     * @return a <tt>Vector</tt> containing the <tt>MediaDescription</tt>s of
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

        QualityPreset sendQualityPreset = null;
        QualityPreset receiveQualityPreset = null;

        if(qualityControls != null)
        {
            // the one we will send is the one the other part has announced as
            // receive
            sendQualityPreset = qualityControls.getRemoteReceivePreset();
            // the one we want to receive is the setting that remote can send
            receiveQualityPreset = qualityControls.getRemoteSendMaxPreset();
        }

        for (MediaType mediaType : MediaType.values())
        {
            MediaDevice dev = getDefaultDevice(mediaType);

            if (!isDeviceActive(dev, sendQualityPreset, receiveQualityPreset))
                continue;

            MediaDirection direction
                = dev.getDirection().and(getDirectionUserPreference(mediaType));

            if(isLocallyOnHold())
                direction = direction.and(MediaDirection.SENDONLY);

            if(direction != MediaDirection.INACTIVE)
            {
                boolean hadSavp = false;
                for (String profileName : getRtpTransports())
                {
                    /*
                     * If we start an audio-only call and re-INVITE the remote
                     * peer for desktop sharing/streaming later on, we will have
                     * effectively switched from the webcam to the
                     * display/screen. It seems beneficial in such a scenario to
                     * not have a send quality preset unless we actually intend
                     * to send video.
                     */
                    QualityPreset effectiveSendQualityPreset
                        = direction.allowsSending() ? sendQualityPreset : null;
                    MediaDescription md
                        = createMediaDescription(
                                profileName,
                                getLocallySupportedFormats(
                                        dev,
                                        effectiveSendQualityPreset,
                                        receiveQualityPreset),
                                getTransportManager().getStreamConnector(
                                        mediaType),
                                direction,
                                dev.getSupportedExtensions());

                    try
                    {
                        // If we have a video preset, let's send info about the
                        // desired frame rate.
                        if (mediaType.equals(MediaType.VIDEO)
                                && (receiveQualityPreset != null))
                        {
                            // doing only int frame rate for now
                            int frameRate
                                = (int) receiveQualityPreset.getFameRate();

                            if (frameRate > 0)
                            {
                                md.setAttribute(
                                        "framerate",
                                        String.valueOf(frameRate));
                            }
                        }
                    }
                    catch(SdpException e)
                    {
                        // do nothing in case of error.
                    }

                    if(!hadSavp)
                    {
                        updateMediaDescriptionForZrtp(mediaType, md);
                        updateMediaDescriptionForSDes(mediaType, md, null);
                    }

                    mediaDescs.add(md);

                    if(!hadSavp && profileName.contains("SAVP"))
                        hadSavp = true;
                }
            }
        }

        //fail if all devices were inactive
        if(mediaDescs.isEmpty())
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
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

        synchronized (offerAnswerLock)
        {
            if (localSess == null)
                return processFirstOffer(offer).toString();
            else
                return processUpdateOffer(offer, localSess).toString();
        }
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
        boolean rejectedAvpOfferDueToSavpRequired = false;

        AccountID accountID = getPeer().getProtocolProvider().getAccountID();
        boolean encryptionEnabled
            = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true);
        int savpOption
            = accountID.getAccountPropertyInt(
                    ProtocolProviderFactory.SAVP_OPTION,
                    ProtocolProviderFactory.SAVP_OFF);

        boolean masterStreamSet = false;
        List<MediaType> seenMediaTypes = new ArrayList<MediaType>();
        for (MediaDescription mediaDescription : remoteDescriptions)
        {
            String transportProtocol;
            try
            {
                transportProtocol = mediaDescription.getMedia().getProtocol();
            }
            catch (SdpParseException e)
            {
                throw new OperationFailedException(
                    "unable to create the media description",
                    OperationFailedException.ILLEGAL_ARGUMENT, e);
            }

            //ignore RTP/AVP(F) stream when RTP/SAVP(F) is mandatory
            if ((savpOption == ProtocolProviderFactory.SAVP_MANDATORY)
                    && !(transportProtocol.equals("RTP/SAVP")
                            || transportProtocol.equals("RTP/SAVPF"))
                    && encryptionEnabled)
            {
                rejectedAvpOfferDueToSavpRequired = true;
                continue;
            }

            MediaType mediaType = null;
            try
            {
                mediaType = SdpUtils.getMediaType(mediaDescription);
                //don't process a second media of the same type
                if(seenMediaTypes.contains(mediaType))
                    continue;
                seenMediaTypes.add(mediaType);
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

            List<MediaFormat> mutuallySupportedFormats;
            if(dev == null)
            {
                mutuallySupportedFormats = null;
            }
            else if(mediaType.equals(MediaType.VIDEO)
                    && (qualityControls != null))
            {
                /*
                 * If we start an audio-only call and re-INVITE the remote peer
                 * for desktop sharing/streaming later on, we will have
                 * effectively switched from the webcam to the display/screen.
                 * It seems beneficial in such a scenario to not have a send
                 * quality preset unless we actually intend to send video.
                 */
                QualityPreset sendQualityPreset
                    = direction.allowsSending()
                        ? qualityControls.getRemoteReceivePreset()
                        : null;
                QualityPreset receiveQualityPreset
                    = qualityControls.getRemoteSendMaxPreset();

                mutuallySupportedFormats
                    = intersectFormats(
                            remoteFormats,
                            getLocallySupportedFormats(
                                    dev,
                                    sendQualityPreset,
                                    receiveQualityPreset));
            }
            else
            {
                mutuallySupportedFormats
                    = intersectFormats(
                            remoteFormats,
                            getLocallySupportedFormats(dev));
            }

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

            // check for options from remote party and set them locally
            if(mediaType.equals(MediaType.VIDEO))
            {
                // update stream
                MediaStream stream = getStream(MediaType.VIDEO);

                if(stream != null && dev != null)
                {
                    List<MediaFormat> fmts = intersectFormats(
                        getLocallySupportedFormats(dev),
                        remoteFormats);

                    if(fmts.size() > 0)
                    {
                        MediaFormat fmt = fmts.get(0);

                        ((VideoMediaStream)stream).updateQualityControl(
                            fmt.getAdvancedAttributes());
                    }
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
                    qualityControls.setMaxFrameRate(frameRate);
            }

            MediaDescription md = createMediaDescription(transportProtocol,
                        mutuallySupportedFormats, connector,
                        direction, rtpExtensions);

            // Sets ZRTP or SDES, depending on the preferences for this account.
            this.setAndAddPreferredEncryptionProtocol(
                    mediaType,
                    md,
                    mediaDescription);

            // create the corresponding stream...
            MediaFormat fmt = findMediaFormat(remoteFormats,
                    mutuallySupportedFormats.get(0));

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                if(remoteDescriptions.size() > 1)
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

            initStream(connector, dev, fmt, target, direction, rtpExtensions,
                masterStream);

            // create the answer description
            answerDescriptions.add(md);

            atLeastOneValidDescription = true;
        }

        if (rejectedAvpOfferDueToSavpRequired && !atLeastOneValidDescription)
            throw new OperationFailedException("Offer contained no valid "
                + "media descriptions. Insecure media was rejected (only "
                + "RTP/AVP instead of RTP/SAVP).",
                OperationFailedException.ILLEGAL_ARGUMENT);

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
     *
     * @return True if ZRTP is added tp the media description. False, otherwise.
     */
    private boolean updateMediaDescriptionForZrtp(
        MediaType mediaType, MediaDescription md)
    {
        MediaAwareCallPeer<?, ?, ?> peer = getPeer();

        if(peer.getProtocolProvider().getAccountID().getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && peer.getProtocolProvider().getAccountID()
                    .isEncryptionProtocolEnabled("ZRTP")
                && peer.getCall().isSipZrtpAttribute())
        {
            try
            {
                Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                    = getSrtpControls();
                MediaTypeSrtpControl key
                    = new MediaTypeSrtpControl(mediaType, SrtpControlType.ZRTP);
                SrtpControl scontrol = srtpControls.get(key);

                if(scontrol == null)
                {
                    scontrol
                        = SipActivator.getMediaService().createZrtpControl();
                    srtpControls.put(key, scontrol);
                }

                ZrtpControl zcontrol = (ZrtpControl) scontrol;
                int versionIndex = zcontrol.getNumberSupportedVersions();
                boolean zrtpHashSet = false;    // will become true if at least one is set

                for (int i = 0; i < versionIndex; i++) {
                    String helloHash = zcontrol.getHelloHash(i);

                    if (helloHash != null && helloHash.length() > 0)
                    {
                        md.setAttribute(SdpUtils.ZRTP_HASH_ATTR, helloHash);
                        zrtpHashSet = true;
                    }
                }
                return zrtpHashSet;
            }
            catch (SdpException ex)
            {
                logger.error("Cannot add zrtp-hash to sdp", ex);
            }
        }
        return false;
    }

    /**
     * Updates the supplied description with SDES attributes if necessary.
     *
     * @param mediaType the media type.
     * @param localMd the description of the local peer.
     * @param remoteMd the description of the remote peer.
     *
     * @return True if SDES is added tp the media description. False, otherwise.
     */
    private boolean updateMediaDescriptionForSDes(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        AccountID accountID = getPeer().getProtocolProvider().getAccountID();

        // check if SDES and encryption is enabled at all
        if(!accountID.isEncryptionProtocolEnabled("SDES")
                || !accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true))
        {
            return false;
        }

        // get or create the control
        Map<MediaTypeSrtpControl, SrtpControl> srtpControls = getSrtpControls();
        MediaTypeSrtpControl key
            = new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
        SrtpControl scontrol = srtpControls.get(key);

        if (scontrol == null)
        {
            scontrol = SipActivator.getMediaService().createSDesControl();
            srtpControls.put(key, scontrol);
        }

        // set the enabled ciphers suites
        SDesControl sdcontrol = (SDesControl) scontrol;
        String ciphers
            = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SDES_CIPHER_SUITES);

        if (ciphers == null)
        {
            ciphers =
                SipActivator.getResources().getSettingsString(
                    SDesControl.SDES_CIPHER_SUITES);
        }
        sdcontrol.setEnabledCiphers(Arrays.asList(ciphers.split(",")));

        // act as initiator
        if (remoteMd == null)
        {
            @SuppressWarnings("unchecked")
            Vector<Attribute> atts = localMd.getAttributes(true);

            for(SrtpCryptoAttribute ca:
                    sdcontrol.getInitiatorCryptoAttributes())
                atts.add(SdpUtils.createAttribute("crypto", ca.encode()));

            return true;
        }
        // act as responder
        else
        {
            SrtpCryptoAttribute localAttr
                = selectSdesCryptoSuite(false, sdcontrol, remoteMd);

            if (localAttr != null)
            {
                try
                {
                    localMd.setAttribute("crypto", localAttr.encode());
                    return true;
                }
                catch (SdpException e)
                {
                    logger.error("unable to add crypto to answer", e);
                }
            }
            else
            {
                // none of the offered suites match, destroy the sdes
                // control
                sdcontrol.cleanup();
                srtpControls.remove(key);
                logger.warn("Received unsupported sdes crypto attribute.");
            }
            return false;
        }
    }

    private List<String> getRtpTransports() throws OperationFailedException
    {
        List<String> result = new ArrayList<String>(2);
        int savpOption = ProtocolProviderFactory.SAVP_OFF;
        if(getPeer()
            .getProtocolProvider()
            .getAccountID()
            .getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_ENCRYPTION, true))
        {
            savpOption = getPeer()
                .getProtocolProvider()
                .getAccountID()
                .getAccountPropertyInt(
                    ProtocolProviderFactory.SAVP_OPTION,
                    ProtocolProviderFactory.SAVP_OFF);
        }
        if(savpOption == ProtocolProviderFactory.SAVP_MANDATORY)
            result.add("RTP/SAVP");
        else if(savpOption == ProtocolProviderFactory.SAVP_OFF)
            result.add(SdpConstants.RTP_AVP);
        else if(savpOption == ProtocolProviderFactory.SAVP_OPTIONAL)
        {
            result.add("RTP/SAVP");
            result.add(SdpConstants.RTP_AVP);
        }
        else
            throw new OperationFailedException("invalid value for SAVP_OPTION",
                OperationFailedException.GENERAL_ERROR);
        return result;
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
     * corresponding <tt>MediaStream</tt>s. This method basically just adds
     * synchronisation on top of {@link #doNonSynchronisedProcessAnswer(
     * SessionDescription)}
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
    private void processAnswer(SessionDescription answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        synchronized (offerAnswerLock)
        {
            doNonSynchronisedProcessAnswer(answer);
        }
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
    private void doNonSynchronisedProcessAnswer(SessionDescription answer)
            throws OperationFailedException,
                   IllegalArgumentException
    {
        List<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(answer);

        this.setCallInfoURL(SdpUtils.getCallInfoURL(answer));

        boolean masterStreamSet = false;
        List<MediaType> seenMediaTypes = new ArrayList<MediaType>();
        for (MediaDescription mediaDescription : remoteDescriptions)
        {
            MediaType mediaType;
            try
            {
                mediaType = SdpUtils.getMediaType(mediaDescription);
                //don't process a second media of the same type
                if(seenMediaTypes.contains(mediaType))
                    continue;
                seenMediaTypes.add(mediaType);
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

            if(!isDeviceActive(dev))
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

            // check for options from remote party and set
            // is quality controls supported
            if(mediaType.equals(MediaType.VIDEO))
            {
                supportQualityControls =
                    SdpUtils.containsAttribute(mediaDescription, "imageattr");
            }

            // select the crypto key the peer has chosen from our proposal
            Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                = getSrtpControls();
            MediaTypeSrtpControl key =
                new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
            SrtpControl scontrol = srtpControls.get(key);

            if(scontrol != null)
            {
                if(selectSdesCryptoSuite(
                            true,
                            (SDesControl) scontrol,
                            mediaDescription) == null)
                {
                    scontrol.cleanup();
                    srtpControls.remove(key);
                    logger.warn("Received unsupported sdes crypto attribute.");
                }
                else
                {
                    //found an SDES answer, remove all other controls
                    Iterator<Map.Entry<MediaTypeSrtpControl, SrtpControl>> iter
                        = srtpControls.entrySet().iterator();

                    while (iter.hasNext())
                    {
                        Map.Entry<MediaTypeSrtpControl, SrtpControl> entry
                            = iter.next();
                        MediaTypeSrtpControl mtsc = entry.getKey();

                        if ((mtsc.mediaType == mediaType)
                                && (mtsc.srtpControlType
                                        != SrtpControlType.SDES))
                        {
                            entry.getValue().cleanup();
                            iter.remove();
                        }
                    }

                    addAdvertisedEncryptionMethod(SrtpControlType.SDES);
                }
            }

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                if(remoteDescriptions.size() > 1)
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

            try
            {
                if(mediaDescription.getAttribute(SdpUtils.ZRTP_HASH_ATTR)
                        != null)
                {
                    addAdvertisedEncryptionMethod(SrtpControlType.ZRTP);
                }
            }
            catch (SdpParseException e)
            {
                logger.error("received an unparsable sdp attribute", e);
            }

            // create the corresponding stream...
            initStream(connector, dev, supportedFormats.get(0), target,
                                direction, rtpExtensions, masterStream);
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
     * @param transport the profile name (RTP/SAVP or RTP/AVP)
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
                                             String             transport,
                                             List<MediaFormat>  formats,
                                             StreamConnector    connector,
                                             MediaDirection     direction,
                                             List<RTPExtension> extensions )
        throws OperationFailedException
    {
        return SdpUtils.createMediaDescription(transport, formats, connector,
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
    @Override
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
    @Override
    protected TransportManagerSipImpl getTransportManager()
    {
        return transportManager;
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
     * Returns the selected crypto suite selected.
     *
     * @param isInitiator True if the local call instance is the initiator of
     * the call. False otherwise.
     * @param sDesControl The SDES based SRTP MediaStream encryption control.
     * @param mediaDescription The description received from the
     * remote peer. This contains the SDES crypto suites available for the
     * remote peer.
     *
     * @return The selected SDES crypto suite supported by both the local and
     * the remote peer. Or null, if there is no crypto suite supported by both
     * of the peers.
     */
    protected SrtpCryptoAttribute selectSdesCryptoSuite(
            boolean isInitiator,
            SDesControl sDesControl,
            MediaDescription mediaDescription)
    {
        @SuppressWarnings("unchecked")
        Vector<Attribute> attrs = mediaDescription.getAttributes(true);
        Vector<SrtpCryptoAttribute> peerAttributes
            = new Vector<SrtpCryptoAttribute>(attrs.size());

        Attribute a;
        for(int i = 0; i < attrs.size(); ++i)
        {
            try
            {
                a = attrs.get(i);
                if (a.getName().equals("crypto"))
                {
                    peerAttributes.add(
                            SrtpCryptoAttribute.create(a.getValue()));
                }
            }
            catch (SdpParseException e)
            {
                logger.error("received an unparsable sdp attribute", e);
            }
        }

        if(isInitiator)
        {
            return sDesControl.initiatorSelectAttribute(peerAttributes);
        }
        else
        {
            return sDesControl.responderSelectAttribute(peerAttributes);
        }
    }

    /**
     * Selects the preferred encryption protocol (only used by the callee).
     *
     * @param mediaType The type of media (AUDIO or VIDEO).
     * @param localMd the description of the local peer.
     * @param remoteMd the description of the remote peer.
     */
    protected void setAndAddPreferredEncryptionProtocol(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        // Sets ZRTP or SDES, depending on the preferences for this account.
        List<String> preferredEncryptionProtocols = getPeer()
            .getProtocolProvider()
            .getAccountID()
            .getSortedEnabledEncryptionProtocolList();

        for(int i = 0; i < preferredEncryptionProtocols.size(); ++i)
        {
            // ZRTP
            if(preferredEncryptionProtocols.get(i).equals(
                        ProtocolProviderFactory.ENCRYPTION_PROTOCOL + ".ZRTP"))
            {
                if(updateMediaDescriptionForZrtp(mediaType, localMd))
                {
                    // Stops once an encryption advertisement has been choosen.
                    return;
                }
            }
            // SDES
            else if(preferredEncryptionProtocols.get(i).equals(
                        ProtocolProviderFactory.ENCRYPTION_PROTOCOL + ".SDES"))
            {
                if(updateMediaDescriptionForSDes(
                            mediaType,
                            localMd,
                            remoteMd))
                {
                    // Stops once an encryption advertisement has been choosen.
                    return;
                }
            }
        }
    }

    /**
     * Starts this <tt>CallPeerMediaHandler</tt>. If it has already been
     * started, does nothing. This method just adds synchronization on top of
     * the one already implemented by {@link CallPeerMediaHandler#start()}.
     *
     * @throws IllegalStateException if this method is called without this
     * handler having first seen a media description or having generated an
     * offer.
     */
    @Override
    public void start()
        throws IllegalStateException
    {
        synchronized (offerAnswerLock)
        {
            super.start();
        }
    }
}
