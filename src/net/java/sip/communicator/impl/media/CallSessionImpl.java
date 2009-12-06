/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import gnu.java.zrtp.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.sdp.*;

import net.java.sip.communicator.impl.media.codec.*;
import net.java.sip.communicator.impl.media.keyshare.*;
import net.java.sip.communicator.impl.media.transform.*;
import net.java.sip.communicator.impl.media.transform.zrtp.*;
import net.java.sip.communicator.plugin.zrtpconfigure.ZrtpConfigureUtils;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.MediaException;
import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Contains parameters associated with a particular Call such as media (audio
 * video), a reference to the call itself, RTPManagers and others.
 * <p>
 * Currently the class works in the following way:
 * <p>
 * We create 2 rtp managers (one for video and one for audio) upon
 * initialization of this call session and initialize/bind them on local
 * addresses.
 * <p>
 * When we are asked to create an SDP offer we ask the <tt>MediaControl</tt>
 * for the Formats/Encodings that we support and create a media description that
 * would advertise these formats as well as the ports that our RTP managers are
 * bound upon.
 * <p>
 * When we need to process an incoming offer we ask the <tt>MediaControl</tt>
 * for the Formats/Encodings that we support, intersect them with those that
 * were sent by the offerer and make <tt>MediaControl</tt> configure our source
 * processor so that it would transmit in the format that it is expected to
 * according to the format set that resulted from the intersection. We also
 * prepare our <tt>RTPManager</tt>-s to send streams for every media type
 * requested in the offer. (Note that these streams are not started until
 * the associated call enters the CONNECTED state).
 * <p>
 * Processing an SDP answer is quite similar to processing an offer with the
 * exception that the intersection of all supported formats has been performed
 * bye the remote party and we only need to configure our processor and
 * <tt>RTPManager</tt>s.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 * @author Ryan Ricard
 * @author Ken Larson
 * @author Dudek Przemyslaw
 * @author Lubomir Marinov
 * @author Emanuel Onica
 */
public class CallSessionImpl
    extends PropertyChangeNotifier
    implements CallSession,
               CallChangeListener,
               ReceiveStreamListener,
               SendStreamListener,
               SessionListener,
               ControllerListener
{
    private static final Logger logger
        = Logger.getLogger(CallSessionImpl.class);

    /**
     * The call associated with this session.
     */
    private final Call call;

    /**
     * The session address that is used for audio communication in this call.
     */
    private SessionAddress audioSessionAddress = null;

    /**
     * The public address returned by the net address manager for the audio
     * session address.
     */
    private InetSocketAddress audioPublicAddress = null;

    /**
     * The session address that is used for video communication in this call.
     */
    private SessionAddress videoSessionAddress = null;

    /**
     * The public address returned by the net address manager for the video
     * session address.
     */
    private InetSocketAddress videoPublicAddress = null;

    /**
     * The rtpManager that handles audio streams in this session.
     */
    private RTPManager audioRtpManager = RTPManager.newInstance();

    /**
     * The rtpManager that handles video streams in this session.
     */
    private RTPManager videoRtpManager = RTPManager.newInstance();

    /**
     * The media service instance that created us.
     */
    private final MediaServiceImpl mediaServCallback;

    private SessionCreatorCallback securityCallback;

    /**
     * The minimum port number that we'd like our rtp managers to bind upon.
     */
    private static int minPortNumber = 5000;

    /**
     * The maximum port number that we'd like our rtp managers to bind upon.
     */
    private static int maxPortNumber = 6000;

    /**
     * The name of the property indicating the length of our receive buffer.
     */
    private static final String PROPERTY_NAME_RECEIVE_BUFFER_LENGTH
        = "net.java.sip.communicator.impl.media.RECEIVE_BUFFER_LENGTH";

    /**
     * The list of currently active players that we have created during this
     * session.
     */
    private final List<Player> players = new ArrayList<Player>();

    private final List<VideoListener> videoListeners =
        new ArrayList<VideoListener>();

    /**
     * SRTP TransformConnectors corresponding to each RTPManager when SRTP
     * feature is enabled.
     */
    private final Hashtable<RTPManager, TransformConnector> transConnectors =
        new Hashtable<RTPManager, TransformConnector>();

    /**
    * Toggles default (from the call start) activation
    * of secure communication
    */
    private boolean usingZRTP = true;

    /**
     * Vector used to hold references of the various key management solutions
     * that are implemented. For now only ZRTP and dummy (hardcoded keys) are
     * present.
     */
    private Vector<KeyProviderAlgorithm> keySharingAlgorithms = null;

    /**
     * The key management solution type used for the current session
     */
    private KeyProviderAlgorithm selectedKeyProviderAlgorithm = null;

    /**
     * The Custom Data Destination used for this call session.
     */
    private final URL dataSink;

    /**
     * RFC 4566 specifies that an SDP description may contain a URI with
     * additional call information. Some servers, such as SEMS use this URI to
     * deliver a link to a call control page, so in case it is there we better
     * store it and show it to the user.
     */
    private URL callURL = null;

    /**
     * The flag which signals that this side of the call has put the other on
     * hold.
     */
    private static final byte ON_HOLD_LOCALLY = 1 << 0;

    /**
     * Indicates an audio session type.
     */
    public static final String AUDIO_SESSION = "AUDIO_SESSION";

    /**
     * Indicates a video session type.
     */
    public static final String VIDEO_SESSION = "VIDEO_SESSION";

    /**
     * The flags which determine whether this side of the call has put the other
     * on hold and whether the other side of the call has put this on hold.
     */
    private byte onHold;

    private final Map<Component, LocalVisualComponentData> localVisualComponents =
        new HashMap<Component, LocalVisualComponentData>();

    /**
     * List of RTP format strings which are supported by SIP Communicator in addition
     * to the JMF standard formats.
     *
     * @see #registerCustomCodecFormats(RTPManager)
     * @see MediaControl#registerCustomCodecs()
     */
    private static final javax.media.Format[] CUSTOM_CODEC_FORMATS
        = new javax.media.Format[]
    {
        // these formats are specific, since RTP uses format numbers with no parameters.
        new AudioFormat(Constants.ILBC_RTP,
                8000.0,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED),
        new AudioFormat(Constants.ALAW_RTP,
                8000,
                8,
                1,
                -1,
                AudioFormat.SIGNED),
        new AudioFormat(Constants.SPEEX_RTP,
                8000,
                8,
                1,
                -1,
                AudioFormat.SIGNED)
    };

    /**
     * Additional info codes for and data to support ZRTP4J.
     * These could be added to the library. However they are specific for this
     * implementation, needing them for various GUI changes.
     */
    public static enum ZRTPCustomInfoCodes
    {
        ZRTPNotEnabledByUser,
        ZRTPDisabledByCallEnd,
        ZRTPEngineInitFailure,
        ZRTPEnabledByDefault
    }

    /**
     * Holds the "Master" ZRTP session.
     *
     * This session must be started first and must have negotiated the key material
     * before any other media session to the same client can be started. See the
     * ZRTP specification, topic multi-streaming mode.
     */
    private TransformConnector zrtpDHSession = null;
    /**
     * JMF stores CUSTOM_CODEC_FORMATS statically, so they only need to be
     * registered once. FMJ does this dynamically (per instance), so it needs
     * to be done for every time we instantiate an RTP manager.
     */
    private static boolean formatsRegisteredOnce = false;
    private static boolean formatsRegisteredOnceVideo = false;

    /**
     * The last <code>intendedDestination</code> to which
     * {@link #allocateMediaPorts(InetAddress)} was applied (and which is
     * supposedly currently in effect).
     */
    private InetAddress lastIntendedDestination;

    /**
     * The indicator which determines whether this instance is currently
     * streaming the local video (to remote destinations).
     */
    private boolean localVideoStreaming = false;

    /**
     * Creates a new session for the specified <tt>call</tt> with a custom
     * destination for incoming data.
     *
     * @param call The call associated with this session.
     * @param mediaServCallback the media service instance that created us.
     * @param dataSink the place to send incoming data.
     */
    public CallSessionImpl(Call call,
                           MediaServiceImpl mediaServCallback,
                           URL dataSink )
    {
        this.call = call;
        this.mediaServCallback = mediaServCallback;
        this.dataSink = dataSink;

        registerCustomCodecFormats(audioRtpManager);

        // not currently needed, we don't have any custom video formats.
        registerCustomVideoCodecFormats(videoRtpManager);

        call.addCallChangeListener(this);
        initializePortNumbers();

        initializeSupportedKeyProviders();
    }

    /**
     * Creates a new session for the specified <tt>call</tt>.
     *
     * @param call The call associated with this session.
     * @param mediaServCallback the media service instance that created us.
     */
    public CallSessionImpl(Call call, MediaServiceImpl mediaServCallback)
    {
        this(call, mediaServCallback, null);
    }

    /**
     * Returns the call associated with this Session.
     *
     * @return the Call associated with this session.
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Returns the port that we are using for receiving video data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving video data in this
     * <tt>CallSession</tt>.
     */
    public int getVideoPort()
    {
        return videoSessionAddress.getDataPort();
    }

    /**
     * Returns the port that we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     */
    public int getAudioPort()
    {
        return audioSessionAddress.getDataPort();
    }

    /**
     * Returns the rtp manager that we are using for audio streams.
     * @return the RTPManager instance that we are using for audio streams.
     */
    public RTPManager getAudioRtpManager()
    {
        return this.audioRtpManager;
    }

    /**
     * Returns the rtp manager that we are using for video streams.
     * @return the RTPManager instance that we are using for audio streams.
     */
    public RTPManager getVideoRtpManager()
    {
        return this.videoRtpManager;
    }

    /**
     * Opens all streams that have been initialized for local RTP managers.
     *
     * @throws MediaException if start() fails for all send streams.
     */
    @SuppressWarnings("unchecked") //JMF legacy code
    public void startStreaming()
        throws MediaException
    {
        //start all audio streams
        boolean startedAtLeastOneAudioStream = false;
        RTPManager rtpManager = getAudioRtpManager();

        List<SendStream> sendStreams = rtpManager.getSendStreams();
        if((sendStreams != null) && (sendStreams.size() > 0))
        {
            logger.trace("Will be starting " + sendStreams.size()
                         + " audio send streams.");

            for (SendStream stream : sendStreams)
            {
                try
                {
                    /** @todo are we sure we want to connect here? */
                    stream.getDataSource().connect();
                    stream.start();
                    startedAtLeastOneAudioStream = true;
                }
                catch (IOException ex)
                {
                    logger.warn("Failed to start stream.", ex);
                }
            }
        }
        else
        {
            logger.trace("No audio send streams will be started.");
        }

        //start video streams
        boolean startedAtLeastOneVideoStream = false;

        rtpManager = getVideoRtpManager();

        if(mediaServCallback.getMediaControl(getCall()).isLocalVideoAllowed()
                && ((sendStreams = rtpManager.getSendStreams()) != null)
                && (sendStreams.size() > 0))
        {
            logger.trace("Will be starting " + sendStreams.size()
                         + " video send streams.");

            for (SendStream stream : sendStreams)
            {
                try
                {
                    stream.start();
                    startedAtLeastOneVideoStream = true;
                }
                catch (IOException ex)
                {
                    logger.warn("Failed to start stream.", ex);
                }
            }
        }
        else
        {
            logger.trace("No video send streams will be started.");
        }
        setLocalVideoStreaming(startedAtLeastOneVideoStream);


        if(!startedAtLeastOneAudioStream
                && !startedAtLeastOneVideoStream
                && sendStreams != null
                && (sendStreams.size() > 0))
        {
            stopStreaming();
            throw new MediaException("Failed to start streaming"
                                     , MediaException.INTERNAL_ERROR);
        }

        applyOnHold();
    }

    /* Implements CallSession#startStreamingAndProcessingMedia(). */
    public void startStreamingAndProcessingMedia()
        throws MediaException
    {
        startStreaming();
        mediaServCallback.getMediaControl(getCall()).startProcessingMedia(this);
    }

    /**
     * Stops and closes all streams that have been initialized for local
     * RTP managers and disposes of the local RTP managers.
     */
    public boolean stopStreaming()
    {
        return stopStreaming(true);
    }

    /**
     * Stops and closes all streams that have been initialized for local
     * RTP managers and, optionally, disposes of the local RTP managers.
     *
     * @param dispose <tt>true</tt> to dispose of the local RTP managers once
     *            their streams have been stopped and closed; <tt>false</tt> to
     *            only stop and close the initialized streams but not dispose of
     *            the local RTP managers
     * @return <tt>true</tt> if at least one stream has been closed and stopped;
     *         otherwise, <tt>false</tt>
     */
    private boolean stopStreaming(boolean dispose)
    {
        boolean stoppedStreaming = false;

        RTPManager audioRtpManager = getAudioRtpManager();
        if (audioRtpManager != null)
        {
            stoppedStreaming = stopStreaming(audioRtpManager, dispose);
            if (dispose)
            {
                transConnectors.remove(this.audioRtpManager); // clean up
                this.audioRtpManager = null;
            }
        }
        RTPManager videoRtpManager = getVideoRtpManager();
        if (videoRtpManager != null)
        {
            stoppedStreaming =
                stopStreaming(videoRtpManager, dispose) || stoppedStreaming;
            if (dispose)
            {
                transConnectors.remove(this.videoRtpManager); // clean up
                this.videoRtpManager = null;
            }
        }

        lastIntendedDestination = null;
        return stoppedStreaming;
    }

    /**
     * Stops and closes the <code>SendStream</code>s initialized for the local
     * RTP managers.
     *
     * @return <tt>true</tt> if at least one stream has been closed and stopped;
     *         otherwise, <tt>false</tt>
     */
    private boolean stopSendStreaming()
    {
        boolean stoppedAtLeastOneStream = false;

        RTPManager audioRtpManager = getAudioRtpManager();
        if ((audioRtpManager != null)
                && stopSendStreaming(audioRtpManager))
            stoppedAtLeastOneStream = true;

        RTPManager videoRtpManager = getVideoRtpManager();
        if ((videoRtpManager != null)
                && stopSendStreaming(videoRtpManager))
            stoppedAtLeastOneStream = true;

        return stoppedAtLeastOneStream;
    }

    /**
     * Stops and closes the <code>SendStream</code>s initialized for a specific
     * <code>RTPManager</code>.
     *
     * @param rtpManager the <code>RTPManager</code> whose
     *            <code>SendStream</code>s are to be stopped and closed
     * @return <tt>true</tt> if at least one stream has been closed and stopped;
     *         otherwise, <tt>false</tt>
     */
    private boolean stopSendStreaming(RTPManager rtpManager)
    {
        @SuppressWarnings("unchecked") //JMF legacy code
            Iterable<SendStream> sendStreams = rtpManager.getSendStreams();
        boolean stoppedAtLeastOneStream = false;

        for (SendStream stream : sendStreams)
        {
            try
            {
                stream.getDataSource().stop();
                stream.stop();

                try
                {
                    stream.close();
                }
                catch (NullPointerException npe)
                {
                    /*
                     * Sometimes com.sun.media.rtp.RTCPTransmitter#bye() may
                     * throw NullPointerException but it does not seem to be
                     * guaranteed because it does not happen while debugging and
                     * stopping at a breakpoint on SendStream#close(). One of
                     * the cases in which it appears upon call hang-up is if we
                     * do not close the "old" SendStreams upon reinvite(s).
                     * Though we are now closing such SendStreams, ignore the
                     * exception here just in case because we already ignore
                     * IOExceptions.
                     */
                    logger.error("Failed to close stream " + stream, npe);
                }
            }
            catch (IOException ioe)
            {
                logger.warn("Failed to stop stream.", ioe);
            }
            stoppedAtLeastOneStream = true;
        }
        return stoppedAtLeastOneStream;
    }

    /**
     * Stops and closes all streams currently handled by <tt>rtpManager</tt>.
     *
     * @param rtpManager the <tt>RTPManager</tt> to stop the streams of
     * @param dispose <tt>true</tt> to dispose of <tt>rtpManager</tt> after
     * stopping its streams; otherwise, <tt>false</tt>
     * @return <tt>true</tt> if there was an actual change in the streaming i.e.
     *         the streaming wasn't already stopped before this request;
     *         <tt>false</tt>, otherwise
     */
    @SuppressWarnings("unchecked") //JMF legacy code
    private boolean stopStreaming(RTPManager rtpManager, boolean dispose)
    {
        boolean stoppedAtLeastOneStream = stopSendStreaming(rtpManager);

        List<ReceiveStream> receiveStreams;
        try
        {
            receiveStreams = rtpManager.getReceiveStreams();
        }
        //it appears that in early call states, when there are no streams
        //this method could throw a null pointer exception. Make sure we handle
        //it gracefully
        catch (Exception e)
        {
            logger.trace("Failed to retrieve receive streams", e);
            receiveStreams = new Vector<ReceiveStream>();
        }
        for (ReceiveStream stream : receiveStreams)
        {
            try
            {
                DataSource streamDataSource = stream.getDataSource();

                /*
                 * For an unknown reason, the stream DataSource can be null at
                 * the end of the Call after re-INVITEs have been handled.
                 */
                if (streamDataSource != null)
                    streamDataSource.stop();
            }
            catch (IOException ex)
            {
                logger.warn("Failed to stop stream.", ex);
            }
            stoppedAtLeastOneStream = true;
        }

        //remove targets
        if (selectedKeyProviderAlgorithm != null
            && selectedKeyProviderAlgorithm.getProviderType()
                == KeyProviderAlgorithm.ProviderType.ZRTP_PROVIDER)
        {
            TransformConnector transConnector =
                this.transConnectors.get(rtpManager);

            if (transConnector != null)
            {
                if (dispose && usingZRTP)
                {
                    ZRTPTransformEngine engine
                        = (ZRTPTransformEngine) transConnector.getEngine();
                    engine.stopZrtp();
                    engine.cleanup();
                }

                transConnector.removeTargets();
            }
        }
        else
        {
            rtpManager.removeTargets("Session ended.");
        }

        printFlowStatistics(rtpManager);

        if (dispose)
        {
            //stop listening
            rtpManager.removeReceiveStreamListener(this);
            rtpManager.removeSendStreamListener(this);
            rtpManager.removeSessionListener(this);
            rtpManager.dispose();
        }

        return stoppedAtLeastOneStream;
    }

    /**
     * Prints all statistics available for rtpManager. (Method contributed by
     * Michael Koch).
     *
     * @param rtpManager the RTP manager that we'd like to print statistics for.
     */
    private void printFlowStatistics(RTPManager rtpManager)
    {
        if(! logger.isDebugEnabled())
            return;

        String rtpManagerDescription = (rtpManager == getAudioRtpManager())
            ? "(for audio flows)"
            : "(for video flows)";

        //print flow statistics.
        GlobalTransmissionStats s = rtpManager.getGlobalTransmissionStats();

        logger.debug(
            "global transmission stats (" + rtpManagerDescription + "): \n" +
            "bytes sent: " + s.getBytesSent() + "\n" +
            "local colls: " + s.getLocalColls() + "\n" +
            "remote colls: " + s.getRemoteColls() + "\n" +
            "RTCP sent: " + s.getRTCPSent() + "\n" +
            "RTP sent: " + s.getRTPSent() + "\n" +
            "transmit failed: " + s.getTransmitFailed()
        );

        GlobalReceptionStats rs = rtpManager.getGlobalReceptionStats();

        logger.debug(
            "global reception stats (" + rtpManagerDescription + "): \n" +
            "bad RTCP packets: " + rs.getBadRTCPPkts() + "\n" +
            "bad RTP packets: " + rs.getBadRTPkts() + "\n" +
            "bytes received: " + rs.getBytesRecd() + "\n" +
            "local collisions: " + rs.getLocalColls() + "\n" +
            "malformed BYEs: " + rs.getMalformedBye() + "\n" +
            "malformed RRs: " + rs.getMalformedRR() + "\n" +
            "malformed SDESs: " + rs.getMalformedSDES() + "\n" +
            "malformed SRs: " + rs.getMalformedSR() + "\n" +
            "packets looped: " + rs.getPacketsLooped() + "\n" +
            "packets received: " + rs.getPacketsRecd() + "\n" +
            "remote collisions: " + rs.getRemoteColls() + "\n" +
            "RTCPs received: " + rs.getRTCPRecd() + "\n" +
            "SRRs received: " + rs.getSRRecd() + "\n" +
            "transmit failed: " + rs.getTransmitFailed() + "\n" +
            "unknown types: " + rs.getUnknownTypes()
        );
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. The
     * resources (address and port) allocated for the <tt>callPeer</tt>
     * should be kept by the media service implementation until the originating
     * <tt>callPeer</tt> enters the DISCONNECTED state. Subsequent sdp
     * offers/answers requested for the <tt>Call</tt> that the original
     * <tt>callPeer</tt> belonged to MUST receive the same IP/port couple
     * as the first one in order to allow for conferencing. The associated port
     * will be released once the call has ended.
     *
     * @todo implement ice.
     *
     * @return a new SDP description String advertising all params of
     * <tt>callSession</tt>.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     */
    public String createSdpOffer()
        throws net.java.sip.communicator.service.media.MediaException
    {
        return createSessionDescription(null, null).toString();
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. The intendedDestination
     * parameter, may contain the address that the offer is to be sent to. In
     * case it is null we'll try our best to determine a default local address.
     *
     * @param intendedDestination the address of the call peer that the
     * descriptions is to be sent to.
     * @return a new SDP description String advertising all params of
     * <tt>callSession</tt>.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     */
    public String createSdpOffer(InetAddress intendedDestination)
        throws net.java.sip.communicator.service.media.MediaException
    {
        return createSessionDescription(null, intendedDestination).toString();
    }

    public String createSdpOffer(String peerSdpDescription)
        throws MediaException
    {
        SessionDescription peerDescription = null;
        try
        {
            peerDescription =
                mediaServCallback.getSdpFactory().createSessionDescription(
                    peerSdpDescription);
        }
        catch (SdpParseException ex)
        {
            throw new MediaException(
                "Failed to parse the SDP description of the peer.",
                MediaException.INTERNAL_ERROR, ex);
        }

        return createSessionDescription(peerDescription, null).toString();
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an in-dialog invitation to a remote callee to put her
     * on/off hold or to send an answer to an offer to be put on/off hold.
     *
     * @param peerSdpDescription the last SDP description of the remote
     *            callee
     * @param on <tt>true</tt> if the SDP description should offer the remote
     *            callee to be put on hold or answer an offer from the remote
     *            callee to be put on hold; <tt>false</tt> to work in the
     *            context of a put-off-hold offer
     * @return a SDP description <tt>String</tt> which offers the remote
     *         callee to be put her on/off hold or answers an offer from the
     *         remote callee to be put on/off hold
     * @throws MediaException
     */
    public String createSdpDescriptionForHold(String peerSdpDescription,
                                              boolean on)
        throws MediaException
    {
        return
            createSessionDescriptionForHold(peerSdpDescription, on)
                .toString();
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an in-dialog invitation to a remote callee to put her
     * on/off hold or to send an answer to an offer to be put on/off hold.
     *
     * @param peerSdpDescription the last SDP description of the remote
     *            callee
     * @param on <tt>true</tt> if the SDP description should offer the remote
     *            callee to be put on hold or answer an offer from the remote
     *            callee to be put on hold; <tt>false</tt> to work in the
     *            context of a put-off-hold offer
     * @return a <code>SessionDescription</code> which offers the remote
     *         callee to be put her on/off hold or answers an offer from the
     *         remote callee to be put on/off hold
     * @throws MediaException
     */
    @SuppressWarnings("unchecked") //javax.sdp - legacy code.
    private SessionDescription createSessionDescriptionForHold(
            String peerSdpDescription,
            boolean on)
        throws MediaException
    {
        SessionDescription peerDescription = null;
        try
        {
            peerDescription =
                mediaServCallback.getSdpFactory().createSessionDescription(
                    peerSdpDescription);
        }
        catch (SdpParseException ex)
        {
            throw new MediaException(
                "Failed to parse the SDP description of the peer.",
                MediaException.INTERNAL_ERROR, ex);
        }

        SessionDescription sdpOffer =
            createSessionDescription(peerDescription, null);

        Vector<MediaDescription> mediaDescriptions;
        try
        {
            mediaDescriptions = sdpOffer.getMediaDescriptions(true);
        }
        catch (SdpException ex)
        {
            throw new MediaException(
                "Failed to get media descriptions from SDP offer.",
                MediaException.INTERNAL_ERROR, ex);
        }

        for (MediaDescription mediaDescription : mediaDescriptions)
        {
            Vector<Attribute> attributes  = mediaDescription.getAttributes(false);

            try
            {
                modifyMediaDescriptionForHold(on, mediaDescription, attributes);
            }
            catch (SdpException ex)

            {
                throw new MediaException(
                    "Failed to modify media description for hold.",
                    MediaException.INTERNAL_ERROR, ex);
            }
        }

        try
        {
            sdpOffer.setMediaDescriptions(mediaDescriptions);
        }
        catch (SdpException ex)
        {
            throw new MediaException(
                "Failed to set media descriptions to SDP offer.",
                MediaException.INTERNAL_ERROR, ex);
        }

        return sdpOffer;
    }

    /**
     * Modifies the attributes of a specific <tt>MediaDescription</tt> in
     * order to make them reflect the state of being on/off hold.
     *
     * @param onHold <tt>true</tt> if the state described by the modified
     *            <tt>MediaDescription</tt> should reflect being put on hold;
     *            <tt>false</tt> for being put off hold
     * @param mediaDescription the <tt>MediaDescription</tt> to modify the
     *            attributes of
     * @param attributes the attributes of <tt>mediaDescription</tt>
     * @throws SdpException
     */
    private void modifyMediaDescriptionForHold(
                            boolean onHold,
                            MediaDescription mediaDescription,
                            Vector<Attribute> attributes)
        throws SdpException
    {

        /*
         * The SDP offer to be put on hold represents a transition between
         * sendrecv and sendonly or between recvonly and inactive depending on
         * the current state.
         */
        String oldAttribute = onHold ? "recvonly" : "inactive";
        String newAttribute = null;
        if (attributes != null)
            for (Attribute attribute : attributes)
            {
                if (oldAttribute.equalsIgnoreCase(attribute.getName()))
                    newAttribute = onHold ? "inactive" : "recvonly";
            }
        if (newAttribute == null)
            newAttribute = onHold ? "sendonly" : "sendrecv";

        mediaDescription.removeAttribute("inactive");
        mediaDescription.removeAttribute("recvonly");
        mediaDescription.removeAttribute("sendonly");
        mediaDescription.removeAttribute("sendrecv");
        mediaDescription.setAttribute(newAttribute, null);
    }

    /*
     * Implements CallSession#getSdpOfferMediaFlags(String).
     */
    @SuppressWarnings("unchecked") //JMF legacy code
    public int getSdpOfferMediaFlags(String sdpOffer) throws MediaException
    {
        SessionDescription description = null;
        try
        {
            description =
                mediaServCallback.getSdpFactory().createSessionDescription(
                    sdpOffer);
        }
        catch (SdpParseException ex)
        {
            throw new MediaException("Failed to parse SDP offer.",
                MediaException.INTERNAL_ERROR, ex);
        }

        Vector<MediaDescription> mediaDescriptions = null;
        try
        {
            mediaDescriptions = description.getMediaDescriptions(true);
        }
        catch (SdpException ex)
        {
            throw new MediaException(
                "Failed to get media descriptions from SDP offer.",
                MediaException.INTERNAL_ERROR, ex);
        }

        int mediaFlags = 0;

        /* Determine whether we're being put on hold. */
        boolean isOfferToHold = true;
        for (Iterator<MediaDescription> mediaDescriptionIter
                        = mediaDescriptions.iterator();
             mediaDescriptionIter.hasNext() && isOfferToHold;)
        {
            MediaDescription mediaDescription = mediaDescriptionIter.next();
            Collection<Attribute> attributes
                                = mediaDescription.getAttributes(false);

            isOfferToHold = false;
            if (attributes != null)
            {
                for (Iterator<Attribute> attributeIter = attributes.iterator();
                     attributeIter.hasNext() && !isOfferToHold;)
                {
                    try
                    {
                        String attribute = attributeIter.next().getName();

                        if ("sendonly".equalsIgnoreCase(attribute)
                                || "inactive".equalsIgnoreCase(attribute))
                            isOfferToHold = true;
                    }
                    catch (SdpParseException ex)
                    {
                        throw new MediaException(
                            "Failed to get SDP media description attribute name",
                            MediaException.INTERNAL_ERROR,
                            ex);
                    }
                }
            }
        }
        if (isOfferToHold)
            mediaFlags |= ON_HOLD_REMOTELY;

        /* Determine which of the media is to be received. */
        for (MediaDescription mediaDescription : mediaDescriptions)
        {
            String mediaType;

            try
            {
                mediaType = mediaDescription.getMedia().getMediaType();
            }
            catch (SdpParseException ex)
            {
                throw new MediaException(
                    "Failed to determine SDP description media type",
                    MediaException.INTERNAL_ERROR,
                    ex);
            }

            int mediaFlag = 0;

            if ("audio".equalsIgnoreCase(mediaType))
                mediaFlag = RECEIVE_AUDIO;
            else if ("video".equalsIgnoreCase(mediaType))
                mediaFlag = RECEIVE_VIDEO;

            mediaFlags |= mediaFlag;

            Collection<Attribute> attributes
                = mediaDescription.getAttributes(false);

            if (attributes != null)
                for (Attribute attribute : attributes)
                {
                    try
                    {
                        String name = attribute.getName();

                        if ("recvonly".equalsIgnoreCase(name)
                                || "inactive".equalsIgnoreCase(name))
                            mediaFlags &= ~mediaFlag;
                    }
                    catch (SdpParseException ex)
                    {
                        throw new MediaException(
                            "Failed to get SDP media description attribute name",
                            MediaException.INTERNAL_ERROR,
                            ex);
                    }
                }
        }

        return mediaFlags;
    }

    /**
     * Puts the media of this <tt>CallSession</tt> on/off hold depending on
     * the origin of the request.
     * <p>
     * For example, a remote request to have this party put off hold cannot
     * override an earlier local request to put the remote party on hold.
     * </p>
     *
     * @param on <tt>true</tt> to request the media of this
     *            <tt>CallSession</tt> be put on hold; <tt>false</tt>,
     *            otherwise
     * @param here <tt>true</tt> if the request comes from this side of the
     *            call; <tt>false</tt> if the remote party is the issuer of
     *            the request i.e. it's the result of a remote offer
     */
    public void putOnHold(boolean on, boolean here)
    {
        if (on)
        {
            onHold |= (here ? ON_HOLD_LOCALLY : ON_HOLD_REMOTELY);
        }
        else
        {
            onHold &= ~ (here ? ON_HOLD_LOCALLY : ON_HOLD_REMOTELY);
        }

        applyOnHold();
    }

    /**
     * Applies the current <code>onHold</code> state to the current streaming.
     */
    private void applyOnHold()
    {
        // Put the send on/off hold.
        boolean sendOnHold =
            (0 != (onHold & (ON_HOLD_LOCALLY | ON_HOLD_REMOTELY)));
        putOnHold(getAudioRtpManager(), sendOnHold);
        putOnHold(getVideoRtpManager(), sendOnHold);

        // Put the receive on/off hold.
        boolean receiveOnHold = (0 != (onHold & ON_HOLD_LOCALLY));
        for (Player player : players)
        {
            if (receiveOnHold)
                player.stop();
            else
                player.start();
        }
    }

    /**
     * Puts a the <tt>SendSteam</tt>s of a specific <tt>RTPManager</tt>
     * on/off hold i.e. stops/starts them.
     *
     * @param rtpManager the <tt>RTPManager</tt> to have its
     *            <tt>SendStream</tt>s on/off hold i.e. stopped/started
     * @param on <tt>true</tt> to have the <tt>SendStream</tt>s of
     *            <tt>rtpManager</tt> put on hold i.e. stopped; <tt>false</tt>,
     *            otherwise
     */
    @SuppressWarnings("unchecked") //JMF legacy code
    private void putOnHold(RTPManager rtpManager, boolean on)
    {
        List<SendStream> sendStreams = rtpManager.getSendStreams();
        for (SendStream sendStream : sendStreams)
        {
            if (on)
            {
                try
                {
                    sendStream.getDataSource().stop();
                    sendStream.stop();
                }
                catch (IOException ex)
                {
                    logger.warn("Failed to stop SendStream.", ex);
                }
            }
            else
            {
                try
                {
                    sendStream.getDataSource().start();
                    sendStream.start();
                }
                catch (IOException ex)
                {
                    logger.warn("Failed to start SendStream.", ex);
                }
            }
        }
    }

    /**
     * The method is meant for use by protocol service implementations upon
     * reception of an SDP answer in response to an offer sent by us earlier.
     *
     * @param sdpAnswerStr the SDP answer that we'd like to handle.
     * @param responder the peer that has sent the answer.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     * @throws ParseException if sdpAnswerStr does not contain a valid sdp
     * String.
     */
    public void processSdpAnswer(CallPeer responder,
                                 String sdpAnswerStr)
        throws MediaException, ParseException
    {
        processSdpStr(responder, sdpAnswerStr, true);
    }

    @SuppressWarnings("unchecked") //javax.sdp legacy code
    private String processSdpStr(CallPeer peer,
                                 String sdpStr,
                                 boolean answer)
        throws MediaException, ParseException
    {
        String answerStr = answer ? "answer" : "offer";

        logger.trace("Parsing SDP " + answerStr + ": " + sdpStr);

        //first parse the answer
        SessionDescription sdp;
        try
        {
            sdp = mediaServCallback.getSdpFactory()
                    .createSessionDescription(sdpStr);
        }
        catch (SdpParseException ex)
        {
            throw new ParseException("Failed to parse SDP "
                                         + answerStr
                                         + ": "
                                         + ex.getMessage(),
                                     ex.getCharOffset());
        }
        //it appears that in some cases parsing could also fail with
        //other exceptions such as a NullPointerException for example so make
        //sure we get those too.
        catch (Exception ex)
        {
            throw new ParseException("Failed to parse SDP "
                                         + answerStr
                                         + ": "
                                         + ex.getMessage(),
                                     0);
        }

        SessionDescription sdpAnswer;

        if (answer)
        {
            /*
             * extract URI (rfc4566 says that if present it should be before the
             * media description so let's start with it)
             */
            setCallURL(sdp.getURI());

            sdpAnswer = sdp;
        }
        else
        {
            //create the SDP answer.
            CallPeerState peerState = peer.getState();

            if (CallPeerState.CONNECTED.equals(peerState)
                    || CallPeerState.isOnHold(peerState))
            {
                //if the call is already connected then this is a
                //reinitialization (e.g. placing the call on/off hold)
                sdpAnswer =
                    createSessionDescriptionForHold(
                        sdpStr,
                        (getSdpOfferMediaFlags(sdpStr)
                            & CallSession.ON_HOLD_REMOTELY) != 0);
            }
            else
            {
                sdpAnswer = createSessionDescription(sdp, null);
            }
        }

        //extract media descriptions
        Vector<MediaDescription> mediaDescriptions;
        try
        {
            mediaDescriptions = sdp.getMediaDescriptions(true);
        }
        catch (SdpException exc)
        {
            logger.error("failed to extract media descriptions", exc);
            throw new MediaException("failed to extract media descriptions"
                                    , MediaException.INTERNAL_ERROR
                                    , exc);
        }
        //add the RTP targets
        initStreamTargets(sdp.getConnection(), mediaDescriptions);

        /*
         * The media descriptions of the offer may list encodings which are not
         * in our answer i.e. we may end up streaming in an encoding disabled by
         * the user in the Media configuration form.
         */
        try
        {
            mediaDescriptions = sdpAnswer.getMediaDescriptions(true);
        }
        catch (SdpException exc)
        {
            logger.error("failed to extract media descriptions", exc);
            throw new MediaException("failed to extract media descriptions"
                                    , MediaException.INTERNAL_ERROR
                                    , exc);
        }

        /*
         * Close the existing streams because we're about to create new ones.
         * Otherwise, a NullPointerException may be thrown in SendStream#close()
         * upon call hang-up for some of the streams.
         */
        stopSendStreaming();
        //create and init the streams (don't start streaming just yet but wait
        //for the call to enter the connected state).
        createSendStreams(mediaDescriptions);

        if (answer)
        {
            CallPeerState peerState = peer.getState();

            if (CallPeerState.CONNECTED.equals(peerState)
                    || CallPeerState.isOnHold(peerState))
                startStreamingAndProcessingMedia();
        }

        return sdpAnswer.toString();
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. Apart
     * from simply generating an SDP response description, the method records
     * details
     *
     * @param sdpOfferStr the SDP offer that we'd like to create an answer for.
     * @param offerer the peer that has sent the offer.
     *
     * @return a String containing an SDP answer describing parameters of the
     * <tt>Call</tt> associated with this session and matching those advertised
     * by the caller in their <tt>sdpOffer</tt>.
     *
     * @throws MediaException code INTERNAL_ERROR if processing the offer and/or
     * generating the answer fail for some reason.
     * @throws ParseException if <tt>sdpOfferStr</tt> does not contain a valid
     * sdp string.
     */
    public String processSdpOffer(CallPeer offerer, String sdpOfferStr)
        throws MediaException, ParseException
    {
        return processSdpStr(offerer, sdpOfferStr, false);
    }

    /**
     * Tries to extract a java.net.URL from the specified sdpURI param and sets
     * it as the default call info URL for this call session.
     *
     * @param sdpURI the sdp uri as extracted from the call session description.
     */
    private void setCallURL(javax.sdp.URI sdpURI)
    {
        if (sdpURI == null)
        {
            logger.trace("Call URI was null.");
            return;
        }

        try
        {
            this.callURL = sdpURI.get();
        }
        catch (SdpParseException exc)
        {
            logger.warn("Failed to parse SDP URI.", exc);
        }
    }

    /**
     * RFC 4566 specifies that an SDP description may contain a URI (i.r. a
     * "u=" param ) with additional call information. Some servers, such as
     * SEMS use this URI to deliver a link to a call control page. This method
     * returns this call URL or <tt>null</tt> if the call session description
     * did not contain a "u=" parameter.
     *
     * @return a call URL as indicated by the "u=" parameter of the call
     * session description or null if there was no such parameter.
     */
    public URL getCallInfoURL()
    {
        return this.callURL;
    }

    /**
     * Creates a DataSource for all encodings in the mediaDescriptions vector
     * and initializes send streams in our rtp managers for every stream in the
     * data source.
     * @param mediaDescriptions a <tt>Vector</tt> containing
     * <tt>MediaDescription</tt> instances as sent by the remote side with their
     * SDP description.
     * @throws MediaException if we fail to create our data source with the
     * proper encodings and/or fail to initialize the RTP managers with the
     * necessary streams and/or don't find encodings supported by both the
     * remote peer and the local controller.
     */
    private void createSendStreams(Vector<MediaDescription> mediaDescriptions)
        throws MediaException
    {
        //extract the encodings these media descriptions specify
        Hashtable<String, List<String>> mediaEncodings
            = extractMediaEncodings(mediaDescriptions);

        //make our processor output in these encodings.
        DataSource dataSource = mediaServCallback.getMediaControl(getCall())
            .createDataSourceForEncodings(mediaEncodings);

        //get all the steams that our processor creates as output.
        PushBufferStream[] streams
            = ((PushBufferDataSource)dataSource).getStreams();

        //for each stream - determine whether it is a video or an audio
        //stream and assign it to the corresponding rtpmanager
        for (int i = 0; i < streams.length; i++)
        {
            PushBufferStream stream = streams[i];
            javax.media.Format format = stream.getFormat();
            RTPManager rtpManager = null;

            if(format instanceof VideoFormat)
            {
                rtpManager = getVideoRtpManager();
            }
            else if (format instanceof AudioFormat)
            {
                rtpManager = getAudioRtpManager();
            }
            else
            {
                logger.warn("We are apparently capable of sending a format that"
                            + " is neither video nor audio. Is this really"
                            + " possible?: "
                            + format);
                continue;
            }

            try
            {
                SendStream sendStream = rtpManager.createSendStream(dataSource, i);

                TransformConnector transConnector =
                    this.transConnectors.get(rtpManager);

                // If a ZRTP engine is availabe then set the SSRC of this stream
                // currently ZRTP supports only one SSRC per engine
                if (transConnector != null)
                {
                    long ssrc = sendStream.getSSRC();

                    ZRTPTransformEngine engine
                        = (ZRTPTransformEngine) transConnector.getEngine();
                    engine.setOwnSSRC(ssrc);
                }
                logger.trace("Created a send stream for format " + format);
            }
            catch (Exception exc)
            {
                throw new MediaException(
                    "Failed to create an RTP send stream for format "
                    + format
                    , MediaException.IO_ERROR
                    , exc);
            }
        }
    }

    /**
     * Extracts the addresses that our interlocutor has sent for receiving media
     * and adds them as targets to our RTP manager.
     *
     * @param globalConnParam the global <tt>Connection</tt> (if there was one)
     * specified by our interlocutor outside any media description.
     * @param mediaDescriptions a Vector containing all media descriptions sent
     * by our interlocutor, that we'd use to verify whether connection level
     * parameters have been specified.
     *
     * @throws ParseException if there was a problem with the sdp
     * @throws MediaException if we simply fail to initialize the remote
     * addresses or set them as targets on our RTPManagers.
     */
    private void initStreamTargets(Connection globalConnParam,
                                   Vector<MediaDescription> mediaDescriptions)
        throws MediaException, ParseException
    {
        try
        {
            String globalConnectionAddress = null;

            if (globalConnParam != null)
                  globalConnectionAddress = globalConnParam.getAddress();

            for (MediaDescription mediaDescription : mediaDescriptions)
            {
                int port = mediaDescription.getMedia().getMediaPort();
                String type = mediaDescription.getMedia().getMediaType();

                // If there's a global address, we use it.
                // If there is no global address, we get the address from
                // the media Description
                // Fix by Pablo L. - Telefonica
                String address;
                if (globalConnectionAddress != null)
                {
                    address = globalConnectionAddress;
                }
                else
                {
                    address = mediaDescription.getConnection().getAddress();
                }

                //check if we have a media level address
                Connection mediaLevelConnection = mediaDescription.
                    getConnection();

                if (mediaLevelConnection != null)
                {
                    address = mediaLevelConnection.getAddress();
                }

                InetAddress inetAddress = null;
                try
                {
                    inetAddress = NetworkUtils.getInetAddress(address);
                }
                catch (UnknownHostException exc)
                {
                    throw new MediaException(
                        "Failed to resolve address " + address
                        , MediaException.NETWORK_ERROR
                        , exc);
                }

                //create the session address for this media type and add it to
                //the RTPManager.
                SessionAddress target = new SessionAddress(inetAddress, port);

                /** @todo the following line assumes that we have a single rtp
                 * manager per media type which is not necessarily true (e.g. we
                 * may have two distinct video streams: 1 for a webcam video and
                 * another one desktop capture stream) */
                RTPManager rtpManager = type.equals("video")
                    ? getVideoRtpManager()
                    : getAudioRtpManager();

                try
                {
                    if (selectedKeyProviderAlgorithm != null)
                    {
                        TransformConnector transConnector =
                            this.transConnectors.get(rtpManager);

                        transConnector.removeTargets();
                        transConnector.addTarget(target);
                    }
                    else
                    {
                        rtpManager.removeTargets("Session ended.");
                        rtpManager.addTarget(target);
                    }

                    logger.trace("added target " + target
                                 + " for type " + type);
                }
                catch (Throwable exc)
                {
                    throw new MediaException("Failed to add RTPManager target."
                        , MediaException.INTERNAL_ERROR
                        , exc);
                }
            }
        }
        catch(SdpParseException exc)
        {
            throw new ParseException("Failed to parse SDP data. Error on line "
                                     + exc.getLineNumber() + " "
                                     + exc.getMessage()
                                     , exc.getCharOffset());
        }
    }

    /**
     * Creates an SDP description of this session using the offer description
     * (if not null) for limiting. The intendedDestination parameter, which may
     * contain the address that the offer is to be sent to, will only be used if
     * the <tt>offer</tt> or its connection parameter are <tt>null</tt>. In the
     * opposite case we are using the address provided in the connection param
     * as an intended destination.
     *
     * @param offer the call peer meant to receive the offer or null if
     * we are to construct our own offer.
     * @param intendedDestination the address of the call peer that the
     * descriptions is to be sent to.
     * @return a SessionDescription of this CallSession.
     *
     * @throws MediaException code INTERNAL_ERROR if we get an SDP exception
     * while creating and/or parsing the sdp description.
     */
    @SuppressWarnings("unchecked") //javax.sdp legacy code
    private SessionDescription createSessionDescription(
                                            SessionDescription offer,
                                            InetAddress intendedDestination)
        throws MediaException
    {
        SdpFactory sdpFactory = mediaServCallback.getSdpFactory();

        try
        {
            SessionDescription sessDescr =
                sdpFactory.createSessionDescription();

            //"v=0"
            Version v = sdpFactory.createVersion(0);

            sessDescr.setVersion(v);

            //we don't yet implement ice so just try to choose a local address
            //that corresponds to the address provided by the offer or as an
            //intended destination.

            if(offer != null)
            {
                Connection c = offer.getConnection();
                if(c != null)
                {
                    try
                    {
                        intendedDestination = NetworkUtils.getInetAddress(
                                        c.getAddress());
                    }
                    catch (SdpParseException ex)
                    {
                        logger.warn("error reading remote sdp. "
                                    + c.toString()
                                    + " is not a valid connection parameter.",
                                    ex);
                    }
                    catch (UnknownHostException ex)
                    {
                        logger.warn("error reading remote sdp. "
                                    + c.toString()
                                    + " does not contain a valid address.",
                                    ex);
                    }
                }

                //in case the offer contains a media level connection param, it
                // needs to override the connection one.
                List<MediaDescription> mediaDescriptions =
                    offer.getMediaDescriptions(true);

                for (MediaDescription mediaDescription : mediaDescriptions)
                {
                    Connection conn = mediaDescription.getConnection();

                    if (conn != null)
                    {
                        try
                        {
                            intendedDestination =
                                NetworkUtils.getInetAddress(conn.getAddress());
                            break;
                        }
                        catch (UnknownHostException e)
                        {
                            logger.debug("Couldn't determine indtended "
                                + "destination from address"
                                + conn.getAddress(), e);
                        }
                    }
                }
            }

            /*
             * Only allocate ports if this is a call establishing event. The
             * opposite could happen for example, when issuing a reINVITE
             * that would put a CallPeer on hold.
             */
            boolean allocateMediaPorts = false;

            if ((audioSessionAddress == null) || (videoSessionAddress == null))
            {
                allocateMediaPorts = true;
            }
            else if ((intendedDestination != null)
                        && !intendedDestination.equals(lastIntendedDestination))
            {
                //we already have ongoing streams so we kill them
                stopStreaming(false);
            }
            if (allocateMediaPorts)
            {
                allocateMediaPorts(intendedDestination);
                lastIntendedDestination = intendedDestination;
            }

            InetAddress publicIpAddress = audioPublicAddress.getAddress();

            String addrType
                = publicIpAddress instanceof Inet6Address
                ? Connection.IP6
                : Connection.IP4;

            //spaces in the user name mess everything up.
            //bug report - Alessandro Melzi
            Origin o = sdpFactory.createOrigin(
                call.getProtocolProvider().getAccountID().getUserID()
                , 0
                , 0
                , "IN"
                , addrType
                , publicIpAddress.getHostAddress());

            sessDescr.setOrigin(o);
            //c=
            Connection c = sdpFactory.createConnection(
                "IN"
                , addrType
                , publicIpAddress.getHostAddress());

            sessDescr.setConnection(c);

            //"s=-"
            sessDescr.setSessionName(sdpFactory.createSessionName("-"));

            //"t=0 0"
            TimeDescription t = sdpFactory.createTimeDescription();
            Vector<TimeDescription> timeDescs = new Vector<TimeDescription>();
            timeDescs.add(t);

            sessDescr.setTimeDescriptions(timeDescs);

            //media descriptions.
            Vector<MediaDescription> offeredMediaDescriptions
                = (offer == null)
                        ? null
                        : offer.getMediaDescriptions(false);

            logger.debug("Will create media descs with: audio public address="
                         + audioPublicAddress
                         + " and video public address="
                         + videoPublicAddress);

            Vector<MediaDescription> mediaDescs
                = createMediaDescriptions(offeredMediaDescriptions
                                        , audioPublicAddress
                                        , videoPublicAddress);

            sessDescr.setMediaDescriptions(mediaDescs);

            if (logger.isTraceEnabled())
                logger.trace("Generated SDP - " + sessDescr.toString());

            return sessDescr;
        }
        catch (SdpException exc)
        {
            throw new MediaException(
                "An SDP exception occurred while generating local "
                    + "SDP description"
                , MediaException.INTERNAL_ERROR
                , exc);
        }
    }

    /**
     * Creates a vector containing SDP descriptions of media types and formats
     * that we support. If the offerVector is non null
     * @param offerMediaDescs the media descriptions sent by the offerer (could
     * be null).
     *
     * @param publicAudioAddress the <tt>InetSocketAddress</tt> that we should
     * be using for sending audio.
     * @param publicVideoAddress the <tt>InetSocketAddress</tt> that we should
     * be using for sending video.
     *
     * @return a <tt>Vector</tt> containing media descriptions that we support
     * and (if this is an answer to an offer) that the offering
     * <tt>CallPeer</tt> supports as well.
     *
     * @throws SdpException we fail creating the media descriptions
     * @throws MediaException with code UNSUPPORTED_FORMAT_SET_ERROR if we don't
     * support any of the offered media formats.
     */
    @SuppressWarnings("unchecked") //javax.sdp legacy code
    private Vector<MediaDescription> createMediaDescriptions(
                                      Vector<MediaDescription> offerMediaDescs,
                                      InetSocketAddress publicAudioAddress,
                                      InetSocketAddress publicVideoAddress)
        throws SdpException
              ,MediaException
    {
        MediaControl mediaControl =
            mediaServCallback.getMediaControl(getCall());

        // supported audio formats.
        String[] supportedAudioEncodings =
            mediaControl.getSupportedAudioEncodings();

        // supported video formats
        String[] supportedVideoEncodings =
            mediaControl.getSupportedVideoEncodings();

        //if there was an offer extract the offered media formats and use
        //the intersection between the formats we support and those in the
        //offer.
        if (offerMediaDescs != null && offerMediaDescs.size() > 0)
        {
            Vector<String> offeredVideoEncodings = new Vector<String>();
            Vector<String> offeredAudioEncodings = new Vector<String>();

            for (MediaDescription desc : offerMediaDescs)
            {
                Media media = desc.getMedia();
                String mediaType = media.getMediaType();

                if (mediaType.equalsIgnoreCase("video"))
                {
                    offeredVideoEncodings = media.getMediaFormats(true);
                }
                else if (mediaType.equalsIgnoreCase("audio"))
                {
                    offeredAudioEncodings = media.getMediaFormats(true);
                }
            }

            //now intersect the offered encodings with what we support
            Hashtable<String, List<String>> encodings
                                = new Hashtable<String, List<String>>(2);
            encodings.put("audio", offeredAudioEncodings);
            encodings.put("video", offeredVideoEncodings);
            encodings = intersectMediaEncodings(encodings);
            List<String> intersectedAudioEncsList = encodings.get("audio");
            List<String> intersectedVideoEncsList = encodings.get("video");

            //now replace the encodings arrays with the intersection
            supportedAudioEncodings
                = intersectedAudioEncsList.toArray(new String[0]);
            supportedVideoEncodings
                = intersectedVideoEncsList.toArray(new String[0]);
        }
        Vector<MediaDescription> mediaDescs = new Vector<MediaDescription>();

        if(supportedAudioEncodings.length > 0)
        {
            //--------Audio media description
            //make sure preferred formats come first
            MediaDescription am
                = mediaServCallback.getSdpFactory().createMediaDescription(
                    "audio"
                    , publicAudioAddress.getPort()
                    , 1
                    , "RTP/AVP"
                    , supportedAudioEncodings);

            // if we support g723 it is in 6.3 bitrate
            String g723Str = String.valueOf(SdpConstants.G723);
            for (String supportedAudioEncoding : supportedAudioEncodings)
            {
                if(supportedAudioEncoding.equals(g723Str))
                {
                    am.setAttribute("rtpmap", "4 G723/8000");
                    am.setAttribute("fmtp", "4 annexa=no;bitrate=6.3");
                }
            }

            byte onHold = this.onHold;

            // add audio line to the sdp only when we support audio/audio-capture
            if (mediaServCallback.getDeviceConfiguration()
                .isAudioCaptureSupported())
            {
                setAttributeOnHold(am, onHold);

                // check if ZRTP engine is used and set SDP attribute if enabled
                TransformConnector transConnector = this.transConnectors
                        .get(audioRtpManager);
                if (transConnector != null)
                {
                    TransformEngine engine = transConnector.getEngine();
                    if (engine instanceof ZRTPTransformEngine && call.isSipZrtpAttribute())
                    {
                        ZRTPTransformEngine ze = (ZRTPTransformEngine) engine;

                        String helloHash = ze.getHelloHash();

                        if( helloHash != null && helloHash.length() > 0)
                            am.setAttribute("zrtp-hash", helloHash);
                    }
                }
                mediaDescs.add(am);
            }
        }
        //--------Video media description
        if(supportedVideoEncodings.length> 0)
        {
            //"m=video 22222 RTP/AVP 34";
            MediaDescription vm
                = mediaServCallback.getSdpFactory().createMediaDescription(
                    "video"
                    , publicVideoAddress.getPort()
                    , 1
                    , "RTP/AVP"
                    , supportedVideoEncodings);

            String h264Str = String.valueOf(Constants.H264_RTP_SDP);
            for (String supportedVideoEncoding : supportedVideoEncodings)
            {
                if(supportedVideoEncoding.equals(h264Str))
                {
                    vm.setAttribute("rtpmap",
                        Constants.H264_RTP_SDP + " H264/90000");
                    vm.setAttribute("fmtp",
                        Constants.H264_RTP_SDP + " packetization-mode=1");
                }
            }

            byte onHold = this.onHold;

            if (!mediaServCallback.getDeviceConfiguration()
                    .isVideoCaptureSupported() ||
                 !mediaControl.isLocalVideoAllowed())
            {
                /* We don't have anything to send. */
                onHold |= ON_HOLD_REMOTELY;
            }
            setAttributeOnHold(vm, onHold);

            // check if ZRTP engine is used and set SDP attribute if enabled
            TransformConnector transConnector = this.transConnectors
                    .get(videoRtpManager);
            if (transConnector != null)
            {
                TransformEngine engine = transConnector.getEngine();
                if (engine instanceof ZRTPTransformEngine && call.isSipZrtpAttribute())
                {
                    ZRTPTransformEngine ze = (ZRTPTransformEngine) engine;

                    String helloHash = ze.getHelloHash();

                    if( helloHash != null && helloHash.length() > 0)
                        vm.setAttribute("zrtp-hash", helloHash);
                }
            }
            mediaDescs.add(vm);
        }




        /** @todo record formats for peer. */

        return mediaDescs;
    }

    /**
     * Sets the call-hold related attribute of a specific
     * <tt>MediaDescription</tt> to a specific value depending on the type of
     * hold this <tt>CallSession</tt> is currently in.
     *
     * @param mediaDescription the <tt>MediaDescription</tt> to set the
     *            call-hold related attribute of
     * @param onHold the call-hold state of this <tt>CallSession</tt> which is
     *            a combination of {@link #ON_HOLD_LOCALLY} and
     *            {@link #ON_HOLD_REMOTELY}
     * @throws SdpException
     */
    private void setAttributeOnHold(MediaDescription mediaDescription,
        byte onHold) throws SdpException
    {
        String attribute;

        if (ON_HOLD_LOCALLY == (onHold & ON_HOLD_LOCALLY))
            attribute =
                (ON_HOLD_REMOTELY == (onHold & ON_HOLD_REMOTELY)) ? "inactive"
                        : "sendonly";
        else
            attribute =
                (ON_HOLD_REMOTELY == (onHold & ON_HOLD_REMOTELY)) ? "recvonly"
                        : null;

        if (attribute != null)
            mediaDescription.setAttribute(attribute, null);
    }

    /**
     * Compares audio/video encodings in the <tt>offeredEncodings</tt>
     * hashtable with those supported by the currently valid media controller
     * and returns the set of those that were present in both. The hashtable
     * a maps "audio"/"video" specifier to a list of encodings present in both
     * the source <tt>offeredEncodings</tt> hashtable and the list of supported
     * encodings.
     *
     * @param offeredEncodings a Hashtable containing sets of encodings that an
     * interlocutor has sent to us.
     * @return a <tt>Hashtable</tt> mapping an "audio"/"video" specifier to a
     * list of encodings present in both the source <tt>offeredEncodings</tt>
     * hashtable and the list of encodings supported by the local media
     * controller.
     * @throws MediaException code UNSUPPORTED_FORMAT_SET_ERROR if the
     * intersection of both encoding sets does not contain any elements.
     */
    private Hashtable<String, List<String>> intersectMediaEncodings(
                            Hashtable<String, List<String>> offeredEncodings)
        throws MediaException
    {
        MediaControl mediaControl =
            mediaServCallback.getMediaControl(getCall());

        // audio encodings supported by the media controller
        String[] supportedAudioEncodings =
            mediaControl.getSupportedAudioEncodings();

        // video encodings supported by the media controller
        String[] supportedVideoEncodings =
            mediaControl.getSupportedVideoEncodings();

        //audio encodings offered by the remote party
        List<String> offeredAudioEncodings = offeredEncodings.get("audio");

        //video encodings offered by the remote party
        List<String> offeredVideoEncodings = offeredEncodings.get("video");

        //recreate the formats we create according to what the other party
        // offered.
        List<String> supportedAudioEncsList =
            Arrays.asList(supportedAudioEncodings);
        List<String> intersectedAudioEncsList = new LinkedList<String>();
        List<String> supportedVideoEncsList =
            Arrays.asList(supportedVideoEncodings);
        List<String> intersectedVideoEncsList = new LinkedList<String>();

        //intersect supported audio formats with offered audio formats
        if (offeredAudioEncodings != null
            && offeredAudioEncodings.size() > 0)
        {
            for (String format : offeredAudioEncodings)
            {
                if (supportedAudioEncsList.contains(format))
                    intersectedAudioEncsList.add(format);
            }
        }

        if (offeredVideoEncodings != null
            && offeredVideoEncodings.size() > 0)
        {
            // intersect supported video formats with offered video formats
            for (String format : offeredVideoEncodings)
            {
                if (supportedVideoEncsList.contains(format))
                    intersectedVideoEncsList.add(format);
            }
        }

        //if the intersection contains no common formats then we need to
        //bail.
        if (intersectedAudioEncsList.size() == 0
            && intersectedVideoEncsList.size() == 0)
        {
            throw new MediaException(
                "None of the offered formats was supported by this "
                + "media implementation"
                , MediaException.UNSUPPORTED_FORMAT_SET_ERROR);

        }

        Hashtable<String, List<String>> intersection =
            new Hashtable<String, List<String>>(2);
        intersection.put("audio", intersectedAudioEncsList);
        intersection.put("video", intersectedVideoEncsList);

        return intersection;
    }

    /**
     * Returns a <tt>Hashtable</tt> mapping media types (e.g. audio or video) to
     * lists of JMF encoding strings corresponding to the SDP formats specified
     * in the <tt>mediaDescriptions</tt> vector.
     *
     * @param mediaDescriptions a <tt>Vector</tt> containing
     *            <tt>MediaDescription</tt> instances extracted from an SDP
     *            offer or answer.
     * @return a <tt>Hashtable</tt> mapping media types (e.g. audio or video) to
     *         lists of JMF encoding strings corresponding to the SDP formats
     *         specified in the <tt>mediaDescriptions</tt> vector.
     */
    @SuppressWarnings("unchecked") //javax.sdp legacy code
    private Hashtable<String, List<String>> extractMediaEncodings(
        Vector<MediaDescription> mediaDescriptions)
    {
        Hashtable<String, List<String>> mediaEncodings =
            new Hashtable<String, List<String>>(2);

        for (MediaDescription mediaDescription : mediaDescriptions)
        {
            Media media = mediaDescription.getMedia();

            Vector<String> mediaFormats = null;
            String mediaType = null;
            try
            {
                mediaFormats = media.getMediaFormats(true);
                mediaType    = media.getMediaType();
            }
            catch (SdpParseException ex)
            {
                //this shouldn't happen since nist-sdp is not doing
                //lasy parsing but log anyway
                logger.warn("Error parsing sdp.",ex);
                continue;
            }

            if(mediaFormats.size() > 0)
            {
                List<String> jmfEncodings =
                    MediaUtils.sdpToJmfEncodings(mediaFormats);
                if(jmfEncodings.size() > 0)
                    mediaEncodings.put(mediaType, jmfEncodings);
            }
        }
        logger.trace("Possible media encodings="+mediaEncodings);
        return mediaEncodings;
    }

    /**
     * Create the RTP managers and bind them on some ports.
     */
    private void initializePortNumbers()
    {
        //first reset to default values
        minPortNumber = 5000;
        maxPortNumber = 6000;

        //then set to anything the user might have specified.
        String minPortNumberStr = MediaActivator.getConfigurationService()
            .getString(MediaService.MIN_PORT_NUMBER_PROPERTY_NAME);

        if (minPortNumberStr != null)
        {
            try{
                minPortNumber = Integer.parseInt(minPortNumberStr);
            }catch (NumberFormatException ex){
                logger.warn(minPortNumberStr
                            + " is not a valid min port number value. "
                            +"using min port " + minPortNumber);
            }
        }

        String maxPortNumberStr = MediaActivator.getConfigurationService()
            .getString(MediaService.MAX_PORT_NUMBER_PROPERTY_NAME);

        if (maxPortNumberStr != null)
        {
            try{
                maxPortNumber = Integer.parseInt(maxPortNumberStr);
            }catch (NumberFormatException ex){
                logger.warn(maxPortNumberStr
                            + " is not a valid max port number value. "
                            +"using max port " + maxPortNumber,
                            ex);
            }
        }
    }

    /**
     * Allocates a local port for the RTP manager, tries to obtain a public
     * address for it and after succeeding makes the network address manager
     * protect the address until we are ready to bind on it.
     *
     * @param intendedDestination a destination that the rtp manager would be
     * communicating with.
     * @param sessionAddress the sessionAddress that we're locally bound on.
     * @param bindRetries the number of times that we need to retry a bind.
     *
     * @return the SocketAddress the public address that the network address
     * manager returned for the session address that we're bound on.
     *
     * @throws MediaException if we fail to initialize rtp manager.
     */
    private InetSocketAddress allocatePort(InetAddress intendedDestination,
                                           SessionAddress sessionAddress,
                                           int bindRetries)
        throws MediaException
    {
        InetSocketAddress publicAddress = null;
        boolean initialized = false;

        NetworkAddressManagerService netAddressManager
                    = MediaActivator.getNetworkAddressManagerService();

        //try to initialize a public address for the rtp manager.
        for (int i = bindRetries; i > 0; i--)
        {
            // first try to obtain a binding for the address.
            // check both ports - data and control port
            try
            {
                publicAddress = netAddressManager.getPublicAddressFor(
                        intendedDestination, sessionAddress.getDataPort());

                // Just check control port, not used here any further
                netAddressManager.getPublicAddressFor(
                        intendedDestination, sessionAddress.getControlPort());

                initialized =true;
                break;
            }
            catch (IOException ex)
            {
                logger.warn("Retrying a bind because of a failure. "
                            + "Failed Address is: "
                            + sessionAddress.toString(), ex);

                //reinit the session address we tried with and prepare to retry.
                sessionAddress
                    .setDataPort(sessionAddress.getDataPort()+2);
                sessionAddress
                    .setControlPort(sessionAddress.getControlPort()+2);
            }

        }

        if(!initialized)
            throw new MediaException("Failed to bind to a local port in "
                                     + Integer.toString(bindRetries)
                                     + " tries."
                                     , MediaException.INTERNAL_ERROR);

        return publicAddress;
    }

    /**
     * Looks for free ports and initializes the RTP manager according toe the
     * specified <tt>intendedDestination</tt>.
     *
     * @param intendedDestination the InetAddress that we will be transmitting
     * to.
     * @throws MediaException if we fail initializing the RTP managers.
     */
    private void allocateMediaPorts(InetAddress intendedDestination)
        throws MediaException
    {
        // Get our local address for the intended destination.
        // Use this address to bind our local RTP sockets
        InetAddress inAddrLocal
            = MediaActivator.getNetworkAddressManagerService()
                    .getLocalHost(intendedDestination);

        //check the number of times that we'd have to retry binding to local
        //ports before giving up.
        int bindRetries
            = MediaActivator.getConfigurationService()
                    .getInt(
                            MediaService.BIND_RETRIES_PROPERTY_NAME,
                            MediaService.BIND_RETRIES_DEFAULT_VALUE);

        //initialize audio rtp manager.

        /*
         * XXX If the ports have to be relocated, keep in mind that when X-Lite
         * sends a re-INVITE and our port change is reported as part of the OK
         * response to the re-INVITE, X-Lite continues to stream to the old
         * ports and not to the new ones.
         */
        if (audioSessionAddress == null)
        {
            audioSessionAddress
                = new SessionAddress(inAddrLocal, minPortNumber);
            audioPublicAddress
                = allocatePort(
                        intendedDestination, audioSessionAddress, bindRetries);

            //augment min port number so that no one else tries to bind here.
            minPortNumber = audioSessionAddress.getDataPort() + 2;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("AudioSessionAddress=" + audioSessionAddress);
            logger.debug("AudioPublicAddress=" + audioPublicAddress);
        }

        //initialize video rtp manager.
        if (videoSessionAddress == null)
        {
            videoSessionAddress
                = new SessionAddress(inAddrLocal, minPortNumber);
            videoPublicAddress
                = allocatePort(
                        intendedDestination, videoSessionAddress, bindRetries);

            //augment min port number so that no one else tries to bind here.
            minPortNumber = videoSessionAddress.getDataPort() + 2;
        }

        //if we have reached the max port number - reinit.
        if (minPortNumber > maxPortNumber - 2)
            initializePortNumbers();

        //now init the rtp managers and make them bind
        initializeRtpManager(audioRtpManager, audioSessionAddress);
        initializeRtpManager(videoRtpManager, videoSessionAddress);
    }

    /**
     * Initializes the RTP manager so that it would start listening on the
     * <tt>address</tt> session address. The method also initializes the RTP
     * manager buffer control.
     *
     * @param rtpManager the <tt>RTPManager</tt> to initialize.
     * @param bindAddress the <tt>SessionAddress</tt> to use when initializing
     * the RTPManager.
     *
     * @throws MediaException if we fail to initialize the RTP manager.
     */
    private void initializeRtpManager(RTPManager rtpManager,
                                      SessionAddress bindAddress)
        throws MediaException
    {

        /* Select a key management type from the present ones to use
         * for now using the zero - top priority solution (ZRTP);
         * TODO: should be extended to a selection algorithm to choose the
         * key management type
         */
        selectedKeyProviderAlgorithm = selectKeyProviderAlgorithm(0);

        try
        {
            // Selected key management type == ZRTP branch
            if (selectedKeyProviderAlgorithm != null &&
                selectedKeyProviderAlgorithm.getProviderType()
                    == KeyProviderAlgorithm.ProviderType.ZRTP_PROVIDER)
            {
                // The connector is created based also on the crypto services
                // The crypto provider solution should be queried somehow
                // or taken from a resources file
                TransformConnector transConnector
                    = TransformManager.createZRTPConnector(bindAddress);

                rtpManager.initialize(transConnector);
                this.transConnectors.put(rtpManager, transConnector);

                // Create security user callback for each peer.
                SecurityEventManager securityEventManager
                    = new SecurityEventManager(this);

                boolean zrtpAutoStart = false;

                // Decide if this will become the ZRTP Master session:
                // - Statement: audio media session will be started before video
                //   media session
                // - if no other audio session was started before then this will
                //   become
                //   ZRTP Master session
                // - only the ZRTP master sessions start in "auto-sensing" mode
                //   to immediately catch ZRTP communication from other client
                // - after the master session has completed its key negotiation
                //   it will start other media sessions (see SCCallback)
                if (rtpManager.equals(audioRtpManager))
                {
                    if (zrtpDHSession == null)
                    {
                        zrtpDHSession = transConnector;
                        zrtpAutoStart = true;

                        securityEventManager.setDHSession(true);
                    }

                    securityEventManager.setSessionType(AUDIO_SESSION);
                }
                else if (rtpManager.equals(videoRtpManager))
                {
                    securityEventManager.setSessionType(VIDEO_SESSION);
                }

                // ZRTP engine initialization
                ZRTPTransformEngine engine
                    = (ZRTPTransformEngine) transConnector.getEngine();

                engine.setUserCallback(securityEventManager);

                // Case 1: user toggled secure communication prior to the call
                // call is encrypted by default due to the option set in
                // the account registration wizard
                if (this.getCall().isDefaultEncrypted())
                {
                    ZrtpConfigure config = ZrtpConfigureUtils.getZrtpConfiguration();
                    if (engine.initialize("GNUZRTP4J.zid", zrtpAutoStart, config))
                    {
                        usingZRTP = true;
                        engine.sendInfo(
                            ZrtpCodes.MessageSeverity.Info,
                            EnumSet.of(
                                    ZRTPCustomInfoCodes.ZRTPEnabledByDefault));
                    }
                    else
                    {
                        engine.sendInfo(ZrtpCodes.MessageSeverity.Info,
                           EnumSet.of(ZRTPCustomInfoCodes.ZRTPEngineInitFailure));
                    }
                }
                // Case 2: user will toggle secure communication during the call
                // (it's not set on at this point)
                else
                {
                    engine.sendInfo(
                        ZrtpCodes.MessageSeverity.Info,
                        EnumSet.of(ZRTPCustomInfoCodes.ZRTPNotEnabledByUser));
                }

                logger.trace(
                    "RTP"
                    + (rtpManager.equals(audioRtpManager)?" audio ":"video")
                    + "manager initialized through connector");
            }
//            else
//            // Selected key management type == Dummy branch - hardcoded keys
//            if (selectedKeyProviderAlgorithm != null &&
//                selectedKeyProviderAlgorithm.getProviderType() ==
//                KeyProviderAlgorithm.ProviderType.DUMMY_PROVIDER
//                /* TODO: Video securing related code
//                 * remove the next condition as part of enabling video securing
//                 * (see comments in secureStatusChanged method for more info)
//                 */
//                && rtpManager.equals(audioRtpManager))
//            {
//                SRTPPolicy srtpPolicy = new SRTPPolicy(
//                            SRTPPolicy.AESF8_ENCRYPTION, 16,
//                            SRTPPolicy.HMACSHA1_AUTHENTICATION, 20, 10, 14);
//
//                // Master key and master salt are hardcoded
//                byte[] masterKey  =
//                    {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
//                     0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
//
//                byte[] masterSalt =
//                    {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
//                    0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d};
//
//                TransformConnector transConnector = null;
//
//                TransformManager.initializeProviders();
//
//                // The connector is created based also on the crypto services
//                // provider type;
//                // The crypto provider solution should be queried somehow
//                // or taken from a resources file
//                transConnector =
//                    TransformManager.createSRTPConnector(bindAddress,
//                                                        masterKey,
//                                                        masterSalt,
//                                                        srtpPolicy,
//                                                        srtpPolicy,
//                                                        "BouncyCastle");
//
//                rtpManager.initialize(transConnector);
//                this.transConnectors.put(rtpManager, transConnector);
//
//                logger.trace("RTP"+
//                        (rtpManager.equals(audioRtpManager)?" audio ":"video")+
//                        "manager initialized through connector");
//
//            }
//            // No key management solution - unsecure communication branch
            else
            {
                rtpManager.initialize(bindAddress);

                logger.trace("RTP"+
                        (rtpManager.equals(audioRtpManager)?" audio ":"video")+
                        "manager initialized normally");
            }

        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            logger.error("Failed to init an RTP manager.", exc);
            throw new MediaException("Failed to init an RTP manager."
                                     , MediaException.IO_ERROR
                                     , exc);
        }


        //it appears that if we don't do this managers don't play
        // You can try out some other buffer size to see
        // if you can get better smoothness.
        BufferControl bc = (BufferControl)rtpManager
            .getControl(BufferControl.class.getName());
        if (bc != null)
        {
            long buff = 100;
            String buffStr = MediaActivator.getConfigurationService()
                    .getString(PROPERTY_NAME_RECEIVE_BUFFER_LENGTH);
            try
            {
                if(buffStr != null && buffStr.length() > 0)
                    buff = Long.parseLong(buffStr);
            }
            catch (NumberFormatException exc)
            {
                logger.warn(buffStr
                            + " is not a valid receive buffer value (integer)."
                            , exc);
            }

            buff = bc.setBufferLength(buff);
            logger.trace("set receiver buffer len to=" + buff);
            bc.setEnabledThreshold(true);
            bc.setMinimumThreshold(100);
        }

        //add listeners
        rtpManager.addReceiveStreamListener(this);
        rtpManager.addSendStreamListener(this);
        rtpManager.addSessionListener(this);
    }

    /**
     * Registers the RTP formats which are supported by SIP Communicator in
     * addition to the JMF standard formats. This has to be done for every RTP
     * Manager instance.
     * <p>
     * JMF stores this statically, so it only has to be done once.  FMJ does it
     * dynamically (per instance, so it needs to be done for each instance.
     *
     * @param rtpManager The manager with which to register the formats.
     * @see EncodingConfiguration#registerCustomCodecs()
     */
    static void registerCustomCodecFormats(RTPManager rtpManager)
    {
        // if we have already registered custom formats and we are running JMF
        // we bail out.
        if (!FMJConditionals.REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
            && formatsRegisteredOnce)
        {
            return;
        }

        for (int i=0; i<CUSTOM_CODEC_FORMATS.length; i++)
        {
            javax.media.Format format = CUSTOM_CODEC_FORMATS[i];
            logger.debug("registering format " + format + " with RTP manager");
            /*
             * NOTE (mkoch@rowa.de): com.sun.media.rtp.RtpSessionMgr.addFormat
             * leaks memory, since it stores the Format in a static Vector.
             * AFAIK there is no easy way around it, but the memory impact
             * should not be too bad.
             */
            rtpManager.addFormat(
                format, MediaUtils.jmfToSdpEncoding(format.getEncoding()));
        }

        formatsRegisteredOnce = true;
    }

    static void registerCustomVideoCodecFormats(RTPManager rtpManager)
    {
        // if we have already registered custom formats and we are running JMF
        // we bail out.
        if (!FMJConditionals.REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
            && formatsRegisteredOnceVideo)
        {
            return;
        }

        javax.media.Format format = new VideoFormat(Constants.H264_RTP);
        logger.debug("registering format " + format + " with RTP manager");
            /*
             * NOTE (mkoch@rowa.de): com.sun.media.rtp.RtpSessionMgr.addFormat
             * leaks memory, since it stores the Format in a static Vector.
             * AFAIK there is no easy way around it, but the memory impact
             * should not be too bad.
             */
            rtpManager.addFormat(
                format, MediaUtils.jmfToSdpEncoding(format.getEncoding()));

        formatsRegisteredOnceVideo = true;
    }

    /**
     * Indicates that a change has occurred in the state of the source call.
     *
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     *            calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
        // we are interested only in CALL_STATE_CHANGEs
        if(!evt.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE))
            return;

        Object newValue = evt.getNewValue();

        if (newValue == evt.getOldValue())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Ignoring call state change from " + newValue
                    + " to the same state.");
            }
            return;
        }

        if (newValue == CallState.CALL_IN_PROGRESS)
        {
            try
            {
                logger.debug("call connected. starting streaming");
                startStreamingAndProcessingMedia();
            }
            catch (MediaException ex)
            {
                /** @todo need to notify someone */
                logger.error("Failed to start streaming.", ex);
            }
        }
        else if (newValue == CallState.CALL_ENDED)
        {
            logger.warn("Stopping streaming.");
            stopStreaming();
            mediaServCallback.getMediaControl(getCall()).stopProcessingMedia(
                this);

            // close all players that we have created in this session
            Player[] players
                = this.players.toArray(new Player[this.players.size()]);

            for (Player player : players)
                disposePlayer(player);

            // remove ourselves as listeners from the call
            evt.getSourceCall().removeCallChangeListener(this);

            /*
             * The disposal of the audio and video RTPManager which used to be
             * here shouldn't be necessary because #stopStreaming() should've
             * already take care of it.
             */

            // Clean up after the support for viewing the local video.
            Component[] localVisualComponents
                = this.localVisualComponents.keySet().toArray(
                        new Component[this.localVisualComponents.size()]);
            for (Component localVisualComponent : localVisualComponents)
                disposeLocalVisualComponent(localVisualComponent);
        }
    }

    private void disposePlayer(Player player)
    {
        player.stop();

        /*
         * The player is being disposed so let the (interested) listeners know
         * its Player#getVisualComponent() (if any) should be released.
         */
        Component visualComponent = getVisualComponent(player);

        player.deallocate();
        player.close();

        players.remove(player);

        if (visualComponent != null)
            fireVideoEvent(VideoEvent.VIDEO_REMOVED, visualComponent,
                VideoEvent.REMOTE);
    }

    /**
     * Indicates that a new call peer has joined the source call.
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public synchronized void callPeerAdded(CallPeerEvent evt)
    {
        //dummy implementation
    }

    /**
     * Indicates that a call peer has left the source call.
     * @param evt the <tt>CallPeerEvent</tt> containing the source call
     * and call peer.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        //dummy implementation
    }

    //implementation of jmf listener methods
    /**
     * Method called back in the SessionListener to notify
     * listener of all Session Events.SessionEvents could be one
     * of NewParticipantEvent or LocalCollisionEvent.
     *
     * @param event the newly received SessionEvent
     */
    public synchronized void update(SessionEvent event)
    {
        if (logger.isDebugEnabled())
        {
            if (event instanceof NewParticipantEvent)
            {
                Participant participant
                    = ((NewParticipantEvent) event).getParticipant();

                logger.debug("A new participant had just joined: "
                                 + participant.getCNAME());
            }
            else
            {
                logger.debug("Received the following JMF Session event - "
                                 + event.getClass().getName()
                                 + "="
                                 + event);
            }
        }
    }

    /**
     * Method called back in the RTPSessionListener to notify
     * listener of all SendStream Events.
     *
     * @param event the newly received SendStreamEvent
     */
    public synchronized void update(SendStreamEvent event)
    {
        logger.debug(
            "received the following JMF SendStreamEvent - "
            + event.getClass().getName() + "="+ event);
    }

    /**
     * Method called back in the RTPSessionListener to notify
     * listener of all ReceiveStream Events.
     *
     * @param evt the newly received ReceiveStreamEvent
     */
    public synchronized void update(ReceiveStreamEvent evt)
    {
        Participant participant = evt.getParticipant(); // could be null.
        ReceiveStream stream = evt.getReceiveStream(); // could be null.

        if (evt instanceof NewReceiveStreamEvent)
        {
            try
            {
                logger.debug("received a new incoming stream. " + evt);
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl(
                    "javax.media.rtp.RTPControl");
                if (logger.isDebugEnabled())
                {
                    if (ctl != null)
                    {
                        logger.debug("Received new RTP stream: "
                                     + ctl.getFormat());
                    }
                    else
                    {
                        logger.debug("Received new RTP stream");
                    }
                }

                Player player = null;
                //if we are using a custom destination, create a processor
                //if not, a player will suffice
                if (dataSink != null)
                {
                    player = Manager.createProcessor(ds);
                }
                else
                {
                    player = Manager.createPlayer(ds);
                }
                player.addControllerListener(this);

                //a processor needs to be configured then realized.
                if (dataSink !=  null)
                {
                    ((Processor)player).configure();
                }
                else
                {
                    player.realize();
                }

                players.add(player);
            }
            catch (Exception e)
            {
                logger.error("NewReceiveStreamEvent exception ", e);
                return;
            }
        }
        else if (evt instanceof StreamMappedEvent)
        {
            if (stream != null && stream.getDataSource() != null)
            {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl(
                    "javax.media.rtp.RTPControl");
                if (logger.isDebugEnabled())
                {
                    String msg = "The previously unidentified stream ";
                    if (ctl != null)
                    {
                        msg += ctl.getFormat();
                    }
                    msg += " had now been identified as sent by: "
                        + participant.getCNAME();
                    logger.debug(msg);
                }
            }
        }
        else if (evt instanceof ByeEvent)
        {
            logger.debug("Got \"bye\" from: " + participant.getCNAME());
        }
    }

    /**
     * This method is called when an event is generated by a
     * <code>Controller</code> that this listener is registered with.
     *
     * @param ce The event generated.
     */
    public synchronized void controllerUpdate(ControllerEvent ce)
    {
        logger.debug("Received a ControllerEvent: " + ce);
        Player player = (Player) ce.getSourceController();

        if (player == null)
        {
            return;
        }

        // if configuration is completed and this is a processor
        // we need to set file format and explicitly call realize().
        if (ce instanceof ConfigureCompleteEvent)
        {
            try
            {
                ((Processor) player)
                    .setContentDescriptor(new FileTypeDescriptor(
                        FileTypeDescriptor.WAVE));
                player.realize();
            }
            catch (Exception exc)
            {
                logger.error("failed to record to file", exc);
            }
        }
        // Get this when the internal players are realized.
        else if (ce instanceof RealizeCompleteEvent)
        {

            // set the volume as it is not on max by default.
            // XXX: I am commenting this since apparently it is causing some
            // problems on windows.
            // GainControl gc
            // = (GainControl)player.getControl(GainControl.class.getName());
            // if (gc != null)
            // {
            // logger.debug("Setting volume to max");
            // gc.setLevel(1);
            // }
            // else
            // logger.debug("Player does not have gain control.");

            logger.debug("A player was realized and will be started.");

            if (dataSink != null)
            {
                try
                {
                    logger.info("starting recording to file: " + dataSink);
                    MediaLocator dest = new MediaLocator(dataSink);
                    DataSink sink =
                        Manager.createDataSink(((Processor) player)
                            .getDataOutput(), dest);
                    player.start();
                    // do we know the output file's duration
                    RecordInitiator record = new RecordInitiator(sink);
                    record.start();
                }
                catch (Exception e)
                {
                    logger.error("failed while trying to record to file", e);
                }
            }
            else
            {
                player.start();
            }

            Component visualComponent = player.getVisualComponent();
            if (visualComponent != null)
            {
                fireVideoEvent(VideoEvent.VIDEO_ADDED, visualComponent,
                    VideoEvent.REMOTE);
            }
        }
        else if (ce instanceof StartEvent)
        {
            logger.debug("Received a StartEvent");
        }
        else if (ce instanceof ControllerErrorEvent)
        {
            logger.error(
                "The following error was reported while starting a player"
                    + ce);
        }
        else if (ce instanceof ControllerClosedEvent)
        {

            /*
             * XXX ControllerClosedEvent is the super of ControllerErrorEvent so
             * the check for the latter should be kept in front of the check for
             * the former.
             */
            logger.debug("Received a ControllerClosedEvent");
        }
    }

    /**
     * The record initiator is started after taking a call that is supposed to
     * be answered by a mailbox plug-in. It waits for the outgoing message to
     * stop transmitting and starts recording whatever comes after that.
     */
    private class RecordInitiator extends Thread
    {
        private DataSink sink;

        public RecordInitiator(DataSink sink)
        {
            this.sink = sink;
        }

        public void run()
        {
            //determine how long to wait for the outgoing
            //message to stop playing
            javax.media.Time timeToWait = mediaServCallback
                                    .getMediaControl(call)
                                    .getOutputDuration();

            //if the time is unknown, we will start recording immediately
            if (timeToWait != javax.media.Time.TIME_UNKNOWN)
            {
                double millisToWait = timeToWait.getSeconds() * 1000;
                long timeStartedPlaying = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeStartedPlaying
                         + millisToWait)
                {
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        logger.error("Interrupted while waiting to start "
                            + "recording incoming message",e);
                    }
                }
            }

            //open the dataSink and start recording
            try
            {
                sink.open();
                sink.start();
            }
            catch (IOException e)
            {
                logger.error("IO Exception while attempting to start "
                             + "recording incoming message",e);
            }
        }
    }

    /**
     * Method for getting the default secure status value for communication
     *
     * @return the default enabled/disabled status value for secure
     * communication
     */
    public boolean getSecureCommunicationStatus()
    {
        return usingZRTP;
    }

    /*
     * The following methods are specific to ZRTP key management implementation.
     */
    /*
     * (non-Javadoc)
     * @see net.java.sip.communicator.service.media.CallSession#setZrtpSASVerification(boolean)
     */
    public boolean setZrtpSASVerification(boolean verified)
    {
        ZRTPTransformEngine engine =
            (ZRTPTransformEngine) zrtpDHSession.getEngine();

        if (verified)
        {
            engine.SASVerified();
        } else
        {
            engine.resetSASVerified();
        }

        return true;
    }

    /**
     * Start multi-stream ZRTP sessions.
     *
     * After the ZRTP Master (DH) session reached secure state the SCCallback calls
     * this method to start the multi-stream ZRTP sessions.
     *
     * First get the multi-stream data from the ZRTP DH session. Then iterate over
     * all known connectors, set multi-stream mode data, and enable auto-start
     * mode (auto-sensing).
     *
     * @return Number of started ZRTP multi-stream mode sessions
     */
    public int startZrtpMultiStreams()
    {
        ZRTPTransformEngine engine
            = (ZRTPTransformEngine) zrtpDHSession.getEngine();

        int counter = 0;
        byte[] multiStreamData = engine.getMultiStrParams();

        Enumeration<TransformConnector> tcs = transConnectors.elements();

        while (tcs.hasMoreElements())
        {
            TransformConnector tc = tcs.nextElement();

            if (tc.equals(zrtpDHSession))
                continue;

            engine = (ZRTPTransformEngine) tc.getEngine();
            engine.setMultiStrParams(multiStreamData);
            engine.setEnableZrtp(true);
            counter++;
        }
        return counter;
    }
    /**
     * Initializes the supported key management types and establishes
     * default usage priorities for them.
     * This part should be further developed (by adding a more detailed
     * priority setting mechanism in case of addition of other security
     * providers).
     */
    public void initializeSupportedKeyProviders()
    {
        if (keySharingAlgorithms == null)
            keySharingAlgorithms = new Vector<KeyProviderAlgorithm>();

        DummyKeyProvider dummyProvider = new DummyKeyProvider(1);
        ZRTPKeyProvider zrtpKeyProvider = new ZRTPKeyProvider(0);

        keySharingAlgorithms.add(
                        zrtpKeyProvider.getPriority(), zrtpKeyProvider);
        keySharingAlgorithms.add(dummyProvider.getPriority(), dummyProvider);
    }

    /**
     * Selects a default key management type to use in securing based
     * on which the actual implementation for that solution will be started
     * This part should be further developed (by adding a more detailed
     * priority choosing mechanism in case of addition of other security
     * providers).
     *
     * @return the default keymanagement type used in securing
     */
    public KeyProviderAlgorithm selectDefaultKeyProviderAlgorithm()
    {
        KeyProviderAlgorithm defaultProvider = keySharingAlgorithms.get(0);

        return (defaultProvider == null) ? new DummyKeyProvider(0)
            : defaultProvider;
    }

    /**
     * Selects a key management type to use in securing based on priority
     * For now the priorities are equal with the position in the Vector
     * holding the keymanagement types.
     * This part should be further developed (by adding a more detailed
     * priority choosing mechanism in case of addition of other security
     * providers).
     *
     * @param priority the priority of the selected key management type with
     * 0 indicating top priority
     * @return the selected key management type
     */
    public KeyProviderAlgorithm selectKeyProviderAlgorithm(int priority)
    {
        return keySharingAlgorithms.get(priority);
    }

    /**
     * Determines whether the audio of this session is (set to) mute.
     *
     * @return <tt>true</tt> if the audio of this session is (set to) mute;
     *         otherwise, <tt>false</tt>
     */
    public boolean isMute()
    {
        return mediaServCallback.getMediaControl(getCall()).isMute();
    }

    /**
     * Sets the mute state of the audio of this session.
     *
     * @param mute <tt>true</tt> to mute the audio of this session; otherwise,
     *            <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
        mediaServCallback.getMediaControl(getCall()).setMute(mute);
    }

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

    public Component createLocalVisualComponent(final VideoListener listener)
        throws MediaException
    {
        if (!isLocalVideoStreaming())
            return null;

        final MediaControl mediaControl
            = mediaServCallback.getMediaControl(getCall());
        DataSource dataSource = mediaControl.createLocalVideoDataSource();

        if (dataSource != null)
        {
            Player player;

            try
            {
                player = Manager.createPlayer(dataSource);
            }
            catch (IOException ex)
            {
                throw new MediaException(
                    "Failed to create Player for local video DataSource.",
                    MediaException.IO_ERROR, ex);
            }
            catch (NoPlayerException ex)
            {
                throw new MediaException(
                    "Failed to create Player for local video DataSource.",
                    MediaException.INTERNAL_ERROR, ex);
            }

            player.addControllerListener(new ControllerListener()
            {
                public void controllerUpdate(ControllerEvent event)
                {
                    controllerUpdateForCreateLocalVisualComponent(
                            event, listener, mediaControl);
                }
            });
            player.start();
        }
        return null;
    }

    private void controllerUpdateForCreateLocalVisualComponent(
        ControllerEvent controllerEvent, VideoListener listener,
        MediaControl mediaControl)
    {
        if (controllerEvent instanceof RealizeCompleteEvent)
        {
            Player player = (Player) controllerEvent.getSourceController();
            Component visualComponent = player.getVisualComponent();

            if (visualComponent != null)
            {
                VideoEvent videoEvent = new VideoEvent(
                        this,
                        VideoEvent.VIDEO_ADDED,
                        visualComponent,
                        VideoEvent.LOCAL);

                listener.videoAdded(videoEvent);

                if (videoEvent.isConsumed())
                {
                    localVisualComponents.put(
                            visualComponent,
                            new LocalVisualComponentData(
                                    player, listener, mediaControl));
                }
                else
                {
                    player.stop();
                    player.deallocate();
                    player.close();
                }
            }
        }
    }

    public void disposeLocalVisualComponent(Component component)
    {
        if (component == null)
            throw new IllegalArgumentException("component");

        LocalVisualComponentData data = localVisualComponents.remove(component);
        if (data != null)
        {
            VideoListener listener = data.listener;

            data.dispose();

            if (listener != null)
            {
                VideoEvent videoEvent = new VideoEvent(
                        this,
                        VideoEvent.VIDEO_REMOVED,
                        component,
                        VideoEvent.LOCAL);

                listener.videoRemoved(videoEvent);
            }
        }
    }

    /*
     * Gets the visual Components of the #players of this CallSession by calling
     * Player#getVisualComponent(). Ignores the failures to access the visual
     * Components of unrealized Players.
     */
    public Component[] getVisualComponents()
    {
        List<Component> visualComponents = new ArrayList<Component>();

        for (Player player : players)
        {
            Component visualComponent = getVisualComponent(player);

            if (visualComponent != null)
                visualComponents.add(visualComponent);
        }
        return visualComponents.toArray(new Component[visualComponents.size()]);
    }

    /**
     * Gets the visual <code>Component</code> of a specific <code>Player</code>
     * if it has one and ignores the failure to access it if the specified
     * <code>Player</code> is unrealized.
     *
     * @param player the <code>Player</code> to get the visual
     *            <code>Component</code> of if it has one
     * @return the visual <code>Component</code> of the specified
     *         <code>Player</code> if it has one; <tt>null</tt> if the specified
     *         <code>Player</code> does not have a visual <code>Component</code>
     *         or the <code>Player</code> is unrealized
     */
    private Component getVisualComponent(Player player)
    {
        Component visualComponent;

        try
        {
            visualComponent = player.getVisualComponent();
        }
        catch (NotRealizedError e)
        {
            visualComponent = null;
            if (logger.isDebugEnabled())
            {
                logger
                    .debug("Called Player#getVisualComponent() on Unrealized player "
                        + player);
            }
        }
        return visualComponent;
    }

    public void removeVideoListener(VideoListener listener)
    {
        videoListeners.remove(listener);
    }

    /**
     * Notifies the <code>VideoListener</code>s registered with this
     * <code>CallSession</code> about a specific type of change in the
     * availability of a specific visual <code>Component</code> depicting video.
     *
     * @param type the type of change as defined by <code>VideoEvent</code> in
     *            the availability of the specified visual
     *            <code>Component</code> depciting video
     * @param visualComponent the visual <code>Component</code> depicting video
     *            which has been added or removed in this
     *            <code>CallSession</code>
     * @param origin
     */
    protected void fireVideoEvent(int type, Component visualComponent,
        int origin)
    {
        VideoListener[] listeners;

        synchronized (videoListeners)
        {
            listeners =
                videoListeners
                    .toArray(new VideoListener[videoListeners.size()]);
        }

        if (listeners.length > 0)
        {
            VideoEvent event =
                new VideoEvent(this, type, visualComponent, origin);

            for (VideoListener listener : listeners)
            {
                switch (type)
                {
                    case VideoEvent.VIDEO_ADDED:
                        listener.videoAdded(event);
                        break;
                    case VideoEvent.VIDEO_REMOVED:
                        listener.videoRemoved(event);
                        break;
                }
            }
        }
    }

    /*
     * Implements CallSession#setLocalVideoAllowed(boolean).
     */
    public void setLocalVideoAllowed(boolean allowed)
        throws MediaException
    {
        stopSendStreaming();
        mediaServCallback.getMediaControl(getCall())
            .setLocalVideoAllowed(allowed);
    }

    /*
     * Implements CallSession#isLocalVideoAllowed().
     */
    public boolean isLocalVideoAllowed()
    {
        return mediaServCallback.getMediaControl(getCall())
                .isLocalVideoAllowed();
    }

    /**
     * Sets the indicator which determine whether this <code>CallSession</code>
     * is currently streaming the local video (to remote destinations). If the
     * setting causes a change in the current state, registered
     * <code>PropertyChangeListener</code>s are notified about a change in the
     * value of the property {@link CallSession#LOCAL_VIDEO_STREAMING}.
     *
     * @param streaming <tt>true</tt> to indicate that this instance is
     *            currently streaming the local video (to remote destinations);
     *            otherwise, <tt>false</tt>
     */
    private void setLocalVideoStreaming(boolean streaming)
    {
        if (localVideoStreaming != streaming)
        {
            boolean oldValue = localVideoStreaming;

            localVideoStreaming = streaming;

            firePropertyChange(
                LOCAL_VIDEO_STREAMING, oldValue, localVideoStreaming);
        }
    }

    /*
     * Implements CallSession#isLocalVideoStreaming().
     */
    public boolean isLocalVideoStreaming()
    {
        return localVideoStreaming;
    }

    /*
     * Implements CallSession#setReceiveStreaming(int).
     */
    public void setReceiveStreaming(int mediaFlags)
    {
        if ((mediaFlags & RECEIVE_VIDEO) == 0)
        {
            Player[] players
                = this.players.toArray(new Player[this.players.size()]);

            for (Player player : players)
            {
                Component visualComponent = getVisualComponent(player);

                if (visualComponent != null)
                    disposePlayer(player);
            }
        }
    }

    private class LocalVisualComponentData
    {
        public final VideoListener listener;

        private final MediaControl mediaControl;

        private PropertyChangeListener mediaControlListener;

        public final Player player;

        public LocalVisualComponentData(Player player, VideoListener listener,
                MediaControl mediaControl)
        {
            this.player = player;
            this.listener = listener;

            this.mediaControl = mediaControl;
            this.mediaControlListener = new PropertyChangeListener()
            {
                private final Component component
                    = LocalVisualComponentData.this.player.getVisualComponent();

                public void propertyChange(PropertyChangeEvent event)
                {
                    if (MediaControl.VIDEO_DATA_SOURCE.equals(event.getPropertyName()))
                        disposeLocalVisualComponent(component);
                }
            };
            this.mediaControl.addPropertyChangeListener(
                    this.mediaControlListener);
        }

        public void dispose()
        {
            if (mediaControlListener != null)
            {
                mediaControl.removePropertyChangeListener(mediaControlListener);
                mediaControlListener = null;
            }

            player.stop();
            player.deallocate();
            player.close();
        }
    }

    /**
     * Sets a <tt>SessionCreatorCallback</tt> that will listen for
     * security events.
     *
     * @param securityCallback the <tt>SessionCreatorCallback</tt> to
     * set
     */
    public void setSessionCreatorCallback(
        SessionCreatorCallback securityCallback)
    {
        this.securityCallback = securityCallback;
    }

    /**
     * Returns the <tt>SessionCreatorCallback</tt> which listens for
     * security events.
     *
     * @return the <tt>SessionCreatorCallback</tt> which listens for
     * security events
     */
    public SessionCreatorCallback getSessionCreatorCallback()
    {
        return securityCallback;
    }
}
