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
import org.jitsi.service.neomedia.rtp.*;

import ch.imvs.sdes4j.srtp.*;

/**
 * The media handler class handles all media management for a single
 * <tt>CallPeer</tt>. This includes initializing and configuring streams,
 * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt> always
 * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
 * both classes are only separated for reasons of readability.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class CallPeerMediaHandlerSipImpl
    extends CallPeerMediaHandler<CallPeerSipImpl>
{
    /**
     * The name of the SDP attribute which specifies the fingerprint and hash
     * function which has computed it of the certificate to validate a DTLS
     * flow.
     */
    private static final String DTLS_SRTP_FINGERPRINT_ATTR = "fingerprint";

    private static final String DTLS_SRTP_SETUP_ATTR = "setup";

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
     * <tt>MediaStream</tt>s that this <tt>MediaHandler</tt> is prepared to
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
        SessionDescription offer
            = (localSess == null)
                ? createFirstOffer()
                : createUpdateOffer(localSess);

        if (getConfigurationService().getBoolean(
                ProtocolProviderServiceSipImpl
                        .USE_SESSION_LEVEL_DIRECTION_IN_SDP,
                false))
        {
            SdpUtils.setSessionDirection(offer);
        }

        return offer.toString();
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

        SessionDescription sDes
            = SdpUtils.createSessionDescription(
                    getTransportManager().getLastUsedLocalHost(),
                    userName,
                    mediaDescs);

        //ICE HACK - please fix
        //new IceTransportManagerSipImpl(getPeer()).startCandidateHarvest(
        //    sDes, null, false, false, false, false, false );

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
                for (String proto : getRtpTransports())
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
                                proto,
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
                        switch (mediaType)
                        {
                        case AUDIO:
                            /*
                             * Let the remote peer know that we support RTCP XR
                             * in general and VoIP Metrics Report Block in
                             * particular.
                             */
                            String rtcpxr
                                = md.getAttribute(
                                        RTCPExtendedReport.SDP_ATTRIBUTE);

                            if (rtcpxr == null)
                            {
                                md.setAttribute(
                                        RTCPExtendedReport.SDP_ATTRIBUTE,
                                        RTCPExtendedReport
                                            .VoIPMetricsReportBlock
                                                .SDP_PARAMETER);
                            }

                            int ptimeSetting
                                = SipActivator.getConfigurationService().getInt(
                                    "net.java.sip.communicator.impl.protocol" +
                                        ".sip.PTIME_VALUE",
                                    20);
                            // the default value is 20ms
                            if(ptimeSetting != 20)
                            {
                                md.setAttribute(
                                    "ptime",
                                    String.valueOf(ptimeSetting));
                            }

                            break;
                        case VIDEO:
                            // If we have a video preset, let's send info about
                            // the desired frame rate.
                            if (receiveQualityPreset != null)
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
                            break;
                        default:
                            break;
                        }
                    }
                    catch(SdpException e)
                    {
                        // do nothing in case of error.
                    }

                    if (DtlsControl.UDP_TLS_RTP_SAVP.equals(proto)
                            || DtlsControl.UDP_TLS_RTP_SAVPF.equals(proto))
                    {
                        /*
                         * RFC 5764 "Datagram Transport Layer Security (DTLS)
                         * Extension to Establish Keys for the Secure Real-time
                         * Transport Protocol (SRTP)"
                         */
                        updateMediaDescriptionForDtls(mediaType, md, null);
                    }
                    else
                    {
                        /*
                         * According to RFC 6189 "ZRTP: Media Path Key Agreement
                         * for Unicast Secure RTP", "ZRTP utilizes normal
                         * RTP/AVP (Audio-Visual Profile) profiles", "[t]he
                         * Secure RTP/AVP (SAVP) profile MAY be used in
                         * subsequent offer/answer exchanges after a successful
                         * ZRTP exchange has resulted in an SRTP session, or if
                         * it is known that the other endpoint supports this
                         * profile" and "[o]ther profiles MAY also be used."
                         */
                        updateMediaDescriptionForZrtp(mediaType, md, null);
                        if (SrtpControl.RTP_SAVP.equals(proto)
                                || SrtpControl.RTP_SAVPF.equals(proto))
                        {
                            /*
                             * According to Ingo Bauersachs, SDES "[b]asically
                             * requires SAVP per RFC."
                             */
                            updateMediaDescriptionForSDes(mediaType, md, null);
                        }
                        if (SrtpControl.RTP_SAVPF.equals(proto))
                        {
                            /*
                             * draft-ietf-rtcweb-rtp-usage-09 "Web Real-Time
                             * Communication (WebRTC): Media Transport and Use
                             * of RTP"
                             */
                            updateMediaDescriptionForDtls(mediaType, md, null);
                        }
                    }

                    mediaDescs.add(md);
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
            SessionDescription answer = (localSess == null)
                    ? processFirstOffer(offer)
                    : processUpdateOffer(offer, localSess);

            if (getConfigurationService().getBoolean(
                    ProtocolProviderServiceSipImpl
                            .USE_SESSION_LEVEL_DIRECTION_IN_SDP,
                    false))
            {
                SdpUtils.setSessionDirection(answer);
            }

            return answer.toString();
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
        SessionDescription answer
            = SdpUtils.createSessionDescription(
                    getTransportManager().getLastUsedLocalHost(),
                    getUserName(),
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
        SessionDescription newAnswer
            = SdpUtils.createSessionUpdateDescription(
                    previousAnswer,
                    getTransportManager().getLastUsedLocalHost(),
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
        List<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(offer);
        // prepare to generate answers to all the incoming descriptions
        Vector<MediaDescription> answerDescriptions
            = new Vector<MediaDescription>(remoteDescriptions.size());

        this.setCallInfoURL(SdpUtils.getCallInfoURL(offer));

        boolean atLeastOneValidDescription = false;
        boolean rejectedAvpOfferDueToSavpMandatory = false;

        AccountID accountID = getPeer().getProtocolProvider().getAccountID();
        int savpOption
            = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                ? accountID.getAccountPropertyInt(
                        ProtocolProviderFactory.SAVP_OPTION,
                        ProtocolProviderFactory.SAVP_OFF)
                : ProtocolProviderFactory.SAVP_OFF;

        boolean masterStreamSet = false;
        List<MediaType> seenMediaTypes = new ArrayList<MediaType>();

        for (MediaDescription mediaDescription : remoteDescriptions)
        {
            String proto;
            try
            {
                proto = mediaDescription.getMedia().getProtocol();
            }
            catch (SdpParseException e)
            {
                throw new OperationFailedException(
                        "Unable to create the media description",
                        OperationFailedException.ILLEGAL_ARGUMENT,
                        e);
            }

            // Disable and ignore a RTP/AVP(F) stream when RTP/SAVP(F) is
            // mandatory. Set the flag that we had such a stream to fail the
            // complete offer if it was the only stream.
            if ((savpOption == ProtocolProviderFactory.SAVP_MANDATORY)
                    && !(proto.endsWith(SrtpControl.RTP_SAVP)
                            || proto.endsWith(SrtpControl.RTP_SAVPF)))
            {
                rejectedAvpOfferDueToSavpMandatory = true;
                answerDescriptions.add(
                        SdpUtils.createDisablingAnswer(mediaDescription));
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

            List<MediaFormat> remoteFormats
                = SdpUtils.extractFormats(
                        mediaDescription,
                        getDynamicPayloadTypes());

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

            MediaDescription md
                = createMediaDescription(
                        proto,
                        mutuallySupportedFormats,
                        connector,
                        direction,
                        rtpExtensions);

            // Sets ZRTP or SDES, depending on the preferences for this account.
            setAndAddPreferredEncryptionProtocol(
                    mediaType,
                    md,
                    mediaDescription);

            // RTCP XR
            String rtcpxr;
            
            try
            {
                /*
                 * We support the receiving of RTCP XR so we will answer the
                 * offer of the remote peer.
                 */
                rtcpxr
                    = mediaDescription.getAttribute(
                            RTCPExtendedReport.SDP_ATTRIBUTE);
                if (rtcpxr != null)
                {
                    /*
                     * However, we support the receiving and sending of VoIP
                     * Metrics Report Block only. 
                     */
                    if (rtcpxr.contains(
                            RTCPExtendedReport.VoIPMetricsReportBlock
                                .SDP_PARAMETER))
                    {
                        rtcpxr
                            = RTCPExtendedReport.VoIPMetricsReportBlock
                                .SDP_PARAMETER;
                    }
                    else
                    {
                        rtcpxr = "";
                    }
                    md.setAttribute(RTCPExtendedReport.SDP_ATTRIBUTE, rtcpxr);
                }
            }
            catch (SdpException se)
            {
                rtcpxr = null;
            }

            // create the corresponding stream...
            MediaFormat fmt
                = findMediaFormat(
                        remoteFormats,
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

            MediaStream stream
                = initStream(
                        connector,
                        dev,
                        fmt,
                        target,
                        direction,
                        rtpExtensions,
                        masterStream);

            // RTCP XR
            if (stream != null)
                stream.setProperty(RTCPExtendedReport.SDP_ATTRIBUTE, rtcpxr);

            // create the answer description
            answerDescriptions.add(md);

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
        {
            if (rejectedAvpOfferDueToSavpMandatory)
            {
                throw new OperationFailedException(
                        "Offer contained no valid media descriptions. Insecure"
                            + " media was rejected (only RTP/AVP instead of"
                            + " RTP/SAVP).",
                        OperationFailedException.ILLEGAL_ARGUMENT);
            }
            else
            {
                throw new OperationFailedException(
                        "Offer contained no valid media descriptions.",
                        OperationFailedException.ILLEGAL_ARGUMENT);
            }
        }

        return answerDescriptions;
    }

    /**
     * Updates a specific local <tt>MediaDescription</tt> and the state of this
     * instance for the purposes of DTLS-SRTP.
     *
     * @param mediaType the <tt>MediaType</tt> of the media described by
     * <tt>localMd</tt> and <tt>remoteMd</tt>
     * @param localMd the local <tt>MediaDescription</tt> to be updated
     * @param remoteMd the remote <tt>MediaDescription</tt>, if any, associated
     * with <tt>localMd</tt>
     * @return <tt>true</tt> if the specified <tt>localMd</tt> and/or the state
     * of this instance was updated for the purposes of DTLS-SRTP or
     * <tt>false</tt> if the specified <tt>localMd</tt> (and <tt>remoteMd</tt>)
     * did not concern DTLS-SRTP
     */
    private boolean updateMediaDescriptionForDtls(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        AccountID accountID = getPeer().getProtocolProvider().getAccountID();
        boolean b = false;

        if (accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && accountID.isEncryptionProtocolEnabled(
                        SrtpControlType.DTLS_SRTP))
        {
            /*
             * The transport protocol of the media described by localMd should
             * be DTLS-SRTP in order to be of any concern here.
             */
            Media localMedia = localMd.getMedia();

            if (localMedia != null)
            {
                String proto;

                try
                {
                    proto = localMedia.getProtocol();
                }
                catch (SdpParseException e)
                {
                    /*
                     * Well, if the protocol of the Media cannot be parsed, then
                     * surely we do not want to have anything to do with it.
                     */
                    proto = null;
                }

                boolean dtls
                    = DtlsControl.UDP_TLS_RTP_SAVP.equals(proto)
                        || DtlsControl.UDP_TLS_RTP_SAVPF.equals(proto)
                        || SrtpControl.RTP_SAVPF.equals(proto);

                if (dtls && (remoteMd != null))
                    dtls = isDtlsMediaDescription(remoteMd);
                SrtpControls srtpControls = getSrtpControls();

                if (dtls)
                {
                    DtlsControl dtlsControl
                        = (DtlsControl)
                            srtpControls.getOrCreate(
                                    mediaType,
                                    SrtpControlType.DTLS_SRTP);

                    // SDP attributes
                    @SuppressWarnings("unchecked")
                    Vector<Attribute> attrs = localMd.getAttributes(true);

                    // setup
                    DtlsControl.Setup setup
                        = (remoteMd == null)
                            ? DtlsControl.Setup.ACTPASS
                            : DtlsControl.Setup.ACTIVE;
                    Attribute setupAttr
                        = SdpUtils.createAttribute(
                                DTLS_SRTP_SETUP_ATTR,
                                setup.toString());

                    attrs.add(setupAttr);

                    // fingerprint
                    String hashFunction
                        = dtlsControl.getLocalFingerprintHashFunction();
                    String fingerprint = dtlsControl.getLocalFingerprint();
                    Attribute fingerprintAttr
                        = SdpUtils.createAttribute(
                                DTLS_SRTP_FINGERPRINT_ATTR,
                                hashFunction + " " + fingerprint);

                    attrs.add(fingerprintAttr);

                    dtlsControl.setSetup(setup);

                    if (remoteMd != null) // answer
                        updateSrtpControlsForDtls(mediaType, localMd, remoteMd);

                    b = true;
                }
                else if (remoteMd != null) // answer
                {
                    /*
                     * If DTLS-SRTP has been rejected as the transport protocol,
                     * then halt the operation of DTLS-SRTP.
                     */
                    SrtpControl dtlsControl
                        = srtpControls.remove(
                                mediaType,
                                SrtpControlType.DTLS_SRTP);

                    if (dtlsControl != null)
                        dtlsControl.cleanup(null);
                }
            }
        }
        return b;
    }

    /**
     * Updates the <tt>SrtpControls</tt> of this instance in accord with a
     * specific <tt>MediaDescription</tt> presented by a remote peer.
     *
     * @param mediaType the <tt>MediaType</tt> of the specified
     * <tt>MediaDescription</tt> to be analyzed
     * @param localMd the <tt>MediaDescription</tt> of the local peer that is
     * the answer to the offer presented by a remote peer represented by
     * <tt>remoteMd</tt> or <tt>null</tt> if the specified <tt>remoteMd</tt> is
     * an answer to an offer of the local peer
     * @param remoteMd the <tt>MediaDescription</tt> presented by a remote peer
     * to be analyzed
     */
    private void updateSrtpControlsForDtls(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        SrtpControls srtpControls = getSrtpControls();
        DtlsControl dtlsControl
            = (DtlsControl)
                srtpControls.get(mediaType, SrtpControlType.DTLS_SRTP);

        if (dtlsControl == null)
            return;

        boolean dtls = isDtlsMediaDescription(remoteMd);

        if (dtls)
        {
            if (localMd == null) // answer
            {
                // setup
                /*
                 * RFC 5763 requires setup:actpass from the offerer i.e. the
                 * offerer is the DTLS server and recommends setup:active to the
                 * answerer i.e the answerer is the DTLS client. If the answerer
                 * chooses setup:passive i.e. the answerer is the DTLS server,
                 * the offerer has to become the DTLS client. 
                 */
                String setup;

                try
                {
                    setup = remoteMd.getAttribute(DTLS_SRTP_SETUP_ATTR);
                }
                catch (SdpParseException spe)
                {
                    setup = null;
                }
                if (DtlsControl.Setup.PASSIVE.toString().equals(setup))
                    dtlsControl.setSetup(DtlsControl.Setup.ACTIVE);
            }

            // fingerprint
            @SuppressWarnings("unchecked")
            Vector<Attribute> attrs = remoteMd.getAttributes(false);
            Map<String, String> remoteFingerprints
                = new LinkedHashMap<String, String>();

            if (attrs != null)
            {
                for (Attribute attr : attrs)
                {
                    String fingerprint;

                    try
                    {
                        if (DTLS_SRTP_FINGERPRINT_ATTR.equals(attr.getName()))
                        {
                            fingerprint = attr.getValue();
                            if (fingerprint == null)
                                continue;
                            else
                                fingerprint = fingerprint.trim();
                        }
                        else
                        {
                            continue;
                        }
                    }
                    catch (SdpParseException spe)
                    {
                        /*
                         * Whatever part of the SDP failed to parse, we would
                         * better not try to recover from it.
                         */
                        continue;
                    }

                    int spIndex = fingerprint.indexOf(' ');

                    if ((spIndex > 0) && (spIndex < fingerprint.length() - 1))
                    {
                        String hashFunction = fingerprint.substring(0, spIndex);

                        fingerprint = fingerprint.substring(spIndex + 1);
                        remoteFingerprints.put(hashFunction, fingerprint);
                    }
                }
            }
            dtlsControl.setRemoteFingerprints(remoteFingerprints);

            removeAndCleanupOtherSrtpControls(
                    mediaType,
                    SrtpControlType.DTLS_SRTP);
        }
        else
        {
            srtpControls.remove(mediaType, SrtpControlType.DTLS_SRTP);
            dtlsControl.cleanup(null);
        }
    }

    /**
     * Updates the supplied media description with SDES attributes if necessary.
     *
     * @param mediaType the media type.
     * @param localMd the description of the local peer.
     * @param remoteMd the description of the remote peer.
     * @return <tt>true</tt> if SDES has been added to the media description;
     * <tt>false</tt>, otherwise.
     */
    private boolean updateMediaDescriptionForSDes(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        AccountID accountID = getPeer().getProtocolProvider().getAccountID();

        // Check if encryption and SDES are enabled at all.
        if(!accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                || !accountID.isEncryptionProtocolEnabled(
                        SrtpControlType.SDES))
        {
            return false;
        }

        // get or create the control
        SrtpControls srtpControls = getSrtpControls();
        SDesControl sdesControl
            = (SDesControl)
                srtpControls.getOrCreate(mediaType, SrtpControlType.SDES);
        // set the enabled ciphers suites
        String ciphers
            = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SDES_CIPHER_SUITES);

        if (ciphers == null)
        {
            ciphers
                = SipActivator.getResources().getSettingsString(
                        SDesControl.SDES_CIPHER_SUITES);
        }
        sdesControl.setEnabledCiphers(Arrays.asList(ciphers.split(",")));

        if (remoteMd == null) // act as initiator
        {
            @SuppressWarnings("unchecked")
            Vector<Attribute> atts = localMd.getAttributes(true);

            for (SrtpCryptoAttribute ca
                    : sdesControl.getInitiatorCryptoAttributes())
            {
                atts.add(SdpUtils.createAttribute("crypto", ca.encode()));
            }
            return true;
        }
        else // act as responder
        {
            SrtpCryptoAttribute localAttr
                = selectSdesCryptoSuite(false, sdesControl, remoteMd);

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
                // None of the offered suites match, destroy the SDES control.
                sdesControl.cleanup(null);
                srtpControls.remove(mediaType, SrtpControlType.SDES);
                logger.warn("Received unsupported sdes crypto attribute.");
            }
            return false;
        }
    }

    /**
     * Updates the supplied media description with ZRTP hello hash if necessary.
     *
     * @param mediaType the media type.
     * @param localMd the media description to update.
     * @return <tt>true</tt> if ZRTP is added to the media description;
     * <tt>false</tt>, otherwise.
     */
    private boolean updateMediaDescriptionForZrtp(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        MediaAwareCallPeer<?, ?, ?> peer = getPeer();
        AccountID accountID = peer.getProtocolProvider().getAccountID();
        boolean b = false;

        if(accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && accountID.isEncryptionProtocolEnabled(SrtpControlType.ZRTP)
                && peer.getCall().isSipZrtpAttribute())
        {
            ZrtpControl zrtpControl
                = (ZrtpControl)
                    getSrtpControls().getOrCreate(
                            mediaType,
                            SrtpControlType.ZRTP);
            int numberSupportedVersions
                = zrtpControl.getNumberSupportedVersions();

            try
            {
                for (int i = 0; i < numberSupportedVersions; i++)
                {
                    String helloHash = zrtpControl.getHelloHash(i);

                    if ((helloHash != null) && helloHash.length() > 0)
                    {
                        localMd.setAttribute(
                                SdpUtils.ZRTP_HASH_ATTR,
                                helloHash);
                        /*
                         * Will return true if at least one zrtp-hash has been
                         * set.
                         */
                        b = true;
                    }
                }
            }
            catch (SdpException ex)
            {
                logger.error("Cannot add zrtp-hash to sdp", ex);
            }
        }
        return b;
    }

    /**
     * Gets a list of (RTP) transport protocols (i.e. <tt>&lt;proto&gt;</tt>) to
     * be announced in a SDP media description (i.e. <tt>m=</tt> line).
     *
     * @return a <tt>List</tt> of (RTP) transport protocols to be announced in a
     * SDP media description
     * @throws OperationFailedException if the value of the <tt>AccountID</tt>
     * property {@link ProtocolProviderFactory#SAVP_OPTION} is invalid
     */
    private List<String> getRtpTransports()
        throws OperationFailedException
    {
        AccountID accountID = getPeer().getProtocolProvider().getAccountID();
        int savpOption
            = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                ? accountID.getAccountPropertyInt(
                        ProtocolProviderFactory.SAVP_OPTION,
                        ProtocolProviderFactory.SAVP_OFF)
                : ProtocolProviderFactory.SAVP_OFF;
        List<String> result = new ArrayList<String>(3);

        if (savpOption == ProtocolProviderFactory.SAVP_OFF)
        {
            result.add(SdpConstants.RTP_AVP);
        }
        else
        {
            /*
             * List the secure transports in the result according to the order
             * of preference of their respective encryption protocols.
             */
            List<SrtpControlType> encryptionProtocols
                = accountID.getSortedEnabledEncryptionProtocolList();

            for (int epi = encryptionProtocols.size() - 1; epi >= 0; epi--)
            {
                SrtpControlType srtpControlType = encryptionProtocols.get(epi);
                String[] protos;

                if (srtpControlType == SrtpControlType.DTLS_SRTP)
                {
                    protos
                        = new String[]
                                {
                                    /*
                                     * RFC 5764 "Datagram Transport Layer
                                     * Security (DTLS) Extension to Establish
                                     * Keys for the Secure Real-time Transport
                                     * Protocol (SRTP)"
                                     */
                                    DtlsControl.UDP_TLS_RTP_SAVP,
                                    /*
                                     * draft-ietf-rtcweb-rtp-usage-09 "Web
                                     * Real-Time Communication (WebRTC): Media
                                     * Transport and Use of RTP"
                                     */
                                    SrtpControl.RTP_SAVPF
                                };
                }
                else
                {
                    /*
                     * According to Ingo Bauersachs, SDES "[b]asically requires
                     * SAVP per RFC."
                     */
                    /*
                     * According to RFC 6189 "ZRTP: Media Path Key Agreement for
                     * Unicast Secure RTP", "ZRTP utilizes normal RTP/AVP
                     * (Audio-Visual Profile) profiles", "[t]he Secure RTP/AVP
                     * (SAVP) profile MAY be used in subsequent offer/answer
                     * exchanges after a successful ZRTP exchange has resulted
                     * in an SRTP session, or if it is known that the other
                     * endpoint supports this profile" and "[o]ther profiles MAY
                     * also be used."
                     */
                    protos = new String[] { SrtpControl.RTP_SAVP };
                }

                for (int pi = protos.length - 1; pi >= 0; pi--)
                {
                    String proto = protos[pi];
                    int ri = result.indexOf(proto);

                    if (ri > 0)
                        result.remove(ri);
                    result.add(0, proto);
                }
            }

            if (savpOption == ProtocolProviderFactory.SAVP_OPTIONAL)
                result.add(SdpConstants.RTP_AVP);
        }

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
     * synchronization on top of {@link #doNonSynchronisedProcessAnswer(
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

            List<MediaFormat> supportedFormats
                = SdpUtils.extractFormats(
                        mediaDescription,
                        getDynamicPayloadTypes());
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
                        mediaDescription,
                        getRtpExtensionsRegistry());
            List<RTPExtension> supportedExtensions
                = getExtensionsForType(mediaType);
            List<RTPExtension> rtpExtensions
                = intersectRTPExtensions(
                        remoteRTPExtensions,
                        supportedExtensions);

            // check for options from remote party and set
            // is quality controls supported
            if(mediaType.equals(MediaType.VIDEO))
            {
                supportQualityControls
                    = SdpUtils.containsAttribute(mediaDescription, "imageattr");
            }


            // DTLS-SRTP
            updateSrtpControlsForDtls(mediaType, null, mediaDescription);

            // SDES
            // select the crypto key the peer has chosen from our proposal
            SrtpControls srtpControls = getSrtpControls();
            SDesControl sdesControl
                = (SDesControl)
                    srtpControls.get(mediaType, SrtpControlType.SDES);

            if(sdesControl != null)
            {
                if(selectSdesCryptoSuite(
                        true,
                        sdesControl,
                        mediaDescription) == null)
                {
                    sdesControl.cleanup(null);
                    srtpControls.remove(mediaType, SrtpControlType.SDES);
                    logger.warn("Received unsupported sdes crypto attribute.");
                }
                else
                {
                    //found an SDES answer, remove all other controls
                    removeAndCleanupOtherSrtpControls(
                            mediaType,
                            SrtpControlType.SDES);
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
            MediaStream stream
                = initStream(
                        connector,
                        dev,
                        supportedFormats.get(0),
                        target,
                        direction,
                        rtpExtensions,
                        masterStream);

            // RTCP XR
            if (stream != null)
            {
                String rtcpxr;

                try
                {
                    rtcpxr
                        = mediaDescription.getAttribute(
                                RTCPExtendedReport.SDP_ATTRIBUTE);
                }
                catch (SdpException se)
                {
                    rtcpxr = null;
                }
                stream.setProperty(RTCPExtendedReport.SDP_ATTRIBUTE, rtcpxr);
            }
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
        return
            SdpUtils.createMediaDescription(
                    transport,
                    formats,
                    connector,
                    direction,
                    extensions,
                    getDynamicPayloadTypes(),
                    getRtpExtensionsRegistry());
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
     * Returns the transport manager that is handling our address management.
     *
     * @return the transport manager that is handling our address management.
     */
    @Override
    protected TransportManagerSipImpl queryTransportManager()
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
            catch (Exception e)
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
        List<SrtpControlType> preferredEncryptionProtocols
            = getPeer()
                .getProtocolProvider()
                    .getAccountID()
                        .getSortedEnabledEncryptionProtocolList();

        for(SrtpControlType srtpControlType : preferredEncryptionProtocols)
        {
            // DTLS-SRTP
            if (srtpControlType == SrtpControlType.DTLS_SRTP)
            {
                if(updateMediaDescriptionForDtls(mediaType, localMd, remoteMd))
                {
                    // Stop once an encryption advertisement has been chosen.
                    return;
                }
            }
            // SDES
            else if(srtpControlType == SrtpControlType.SDES)
            {
                if(updateMediaDescriptionForSDes(mediaType, localMd, remoteMd))
                {
                    // Stop once an encryption advertisement has been chosen.
                    return;
                }
            }
            // ZRTP
            else if(srtpControlType == SrtpControlType.ZRTP)
            {
                if(updateMediaDescriptionForZrtp(mediaType, localMd, remoteMd))
                {
                    // Stop once an encryption advertisement has been chosen.
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

    /**
     * Determines whether a specific <tt>MediaDescription</tt> selects DTLS-SRTP
     * as the encryption protocol i.e. checks whether the <tt>proto</tt> of the
     * specified <tt>mediaDescription</tt> is supported by DTLS-SRTP and
     * whether the specified <tt>mediaDescription</tt> assigns non-empty values
     * to the SDP attributes <tt>fingerprint</tt> and <tt>setup</tt>.
     *
     * @param mediaDescription the <tt>MediaDescription</tt> to be analyzed
     * @return <tt>true</tt> if the specified <tt>mediaDescription</tt> selects
     * DTLS-SRTP as the encryption protocol; otherwise, <tt>false</tt>
     */
    private boolean isDtlsMediaDescription(MediaDescription mediaDescription)
    {
        boolean dtls = false;

        if (mediaDescription != null)
        {
            Media media = mediaDescription.getMedia();

            if (media != null)
            {
                try
                {
                    String proto = media.getProtocol();

                    if (DtlsControl.UDP_TLS_RTP_SAVP.equals(proto)
                            || DtlsControl.UDP_TLS_RTP_SAVPF.equals(proto)
                            || SrtpControl.RTP_SAVPF.equals(proto))
                    {
                        String fingerprint
                            = mediaDescription.getAttribute(
                                    DTLS_SRTP_FINGERPRINT_ATTR);

                        if ((fingerprint != null)
                                && (fingerprint.length() != 0))
                        {
                            String setup
                                = mediaDescription.getAttribute(
                                        DTLS_SRTP_SETUP_ATTR);

                            if ((setup != null) && (setup.length() != 0))
                                dtls = true;
                        }
                    }
                }
                catch (SdpParseException e)
                {
                    /*
                     * Well, if the protocol of the Media cannot be parsed, then
                     * surely we do not want to have anything to do with it.
                     */
                }
            }
        }
        return dtls;
    }
}
