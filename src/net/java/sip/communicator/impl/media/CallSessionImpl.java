/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.sdp.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.MediaException;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import javax.media.control.*;

/**
 * Contains parameters associated with a particular Call such as media (audio
 * video), a reference to the call itself, RTPManagers and others.
 * <p>
 * Currently the class works the following way:<p>
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
 * @todo implement SendStreamListener.
 * @todo implement ReceiveStreamListener.
 *
 * @author Emil Ivov
 */
public class CallSessionImpl
        implements   CallSession
                   , CallParticipantListener
                   , CallChangeListener
                   , ReceiveStreamListener
                   , SendStreamListener
                   , SessionListener
                   , ControllerListener

{
    private static final Logger logger
        = Logger.getLogger(CallSessionImpl.class);

    /**
     * The call associated with this session.
     */
    private Call call = null;

    /**
     * The session address that is used for audio communication in this call.
     */
    private SessionAddress audioSessionAddress = null;

    /**
     * The session address that is used for video communication in this call.
     */
    private SessionAddress videoSessionAddress = null;

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
    private MediaServiceImpl mediaServCallback = null;

    /**
     * The minimum port number that we'd like our rtp managers to bind upon.
     */
    private int minPortNumber = 5000;

    /**
     * The maximum port number that we'd like our rtp managers to bind upon.
     */
    private int maxPortNumber = 6000;

    /**
     * Creates a new session for the specified <tt>call</tt>.
     *
     * @param call The call associated with this session.
     * @param mediaServCallback the media service instance that created us.
     */
    public CallSessionImpl(Call call, MediaServiceImpl mediaServCallback)
    {
        this.call = call;
        this.mediaServCallback = mediaServCallback;
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
    private void startStreaming()
        throws MediaException
    {
        //start all audio streams
        boolean startedAtLeastOneStream = false;
        RTPManager rtpManager = getAudioRtpManager();

        Vector sendStreams = rtpManager.getSendStreams();
        if(sendStreams != null && sendStreams.size() > 0)
        {
            logger.trace("Will be starting " + sendStreams.size()
                         + " audio send streams.");
            Iterator ssIter = sendStreams.iterator();

            while (ssIter.hasNext())
            {
                SendStream stream = (SendStream) ssIter.next();
                try
                {
                    stream.start();
                    startedAtLeastOneStream = true;
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
        rtpManager = getVideoRtpManager();
        sendStreams = rtpManager.getSendStreams();
        if(sendStreams != null && sendStreams.size() > 0)
        {
            logger.trace("Will be starting " + sendStreams.size()
                         + " video send streams.");
            Iterator ssIter = sendStreams.iterator();

            while (ssIter.hasNext())
            {
                SendStream stream = (SendStream) ssIter.next();
                try
                {
                    stream.start();
                    startedAtLeastOneStream = true;
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


        if(!startedAtLeastOneStream && sendStreams.size() > 0)
            throw new MediaException("Failed to start streaming"
                , MediaException.INTERNAL_ERROR);
    }

    /**
     * Stops and closes all streams that have been initialized for local
     * RTP managers.
     */
    private void stopStreaming()
    {
        stopStreaming(getAudioRtpManager());
        this.audioRtpManager = null;
        stopStreaming(getVideoRtpManager());
        this.videoRtpManager = null;
    }

    /**
     * Stops and closes all streams currently handled by <tt>rtpManager</tt>.
     *
     * @param rtpManager the rtpManager whose streams we'll be stopping.
     */
    private void stopStreaming(RTPManager rtpManager)
    {
        Vector sendStreams = rtpManager.getSendStreams();
        Iterator ssIter = sendStreams.iterator();

        while(ssIter.hasNext())
        {
            SendStream stream = (SendStream) ssIter.next();
            try
            {
                stream.getDataSource().stop();
                stream.getDataSource().disconnect();
                stream.stop();
                stream.close();
            }
            catch (IOException ex)
            {
                logger.warn("Failed to stop stream.", ex);
            }
        }

        Vector receiveStreams = rtpManager.getReceiveStreams();
        Iterator rsIter = receiveStreams.iterator();
        while(rsIter.hasNext())
        {
            ReceiveStream stream = (ReceiveStream) rsIter.next();
            try
            {
                stream.getDataSource().stop();
                stream.getDataSource().disconnect();
            }
            catch (IOException ex)
            {
                logger.warn("Failed to stop stream.", ex);
            }
        }


        //remove targets
        rtpManager.removeTargets("Session ended.");

        //stop listening
        rtpManager.removeReceiveStreamListener(this);
        rtpManager.removeSendStreamListener(this);
        rtpManager.removeSessionListener(this);
        rtpManager.dispose();
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. The
     * resources (address and port) allocated for the <tt>callParticipant</tt>
     * should be kept by the media service implementation until the originating
     * <tt>callParticipant</tt> enters the DISCONNECTED state. Subsequent sdp
     * offers/answers requested for the <tt>Call</tt> that the original
     * <tt>callParticipant</tt> belonged to MUST receive the same IP/port couple
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
        return createSessionDescription(null).toString();
    }

    /**
     * The method is meant for use by protocol service implementations upon
     * reception of an SDP answer in response to an offer sent by us earlier.
     *
     * @param sdpAnswerStr the SDP answer that we'd like to handle.
     * @param responder the participant that has sent the answer.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     * @throws ParseException if sdpAnswerStr does not contain a valid sdp
     * String.
     */
    public void processSdpAnswer(CallParticipant responder, String sdpAnswerStr)
        throws MediaException, ParseException
    {
        logger.trace("Parsing sdp answer: " + sdpAnswerStr);
        //first parse the answer
        SessionDescription sdpAnswer = null;
        try
        {
            sdpAnswer = mediaServCallback.getSdpFactory()
                .createSessionDescription(sdpAnswerStr);
        }
        catch (SdpParseException ex)
        {
            throw new ParseException("Failed to parse SDPOffer: "
                                     + ex.getMessage()
                                     , ex.getCharOffset());
        }

        //extract media descriptions
        Vector mediaDescriptions = null;
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


        //add the RTP targets
        this.initStreamTargets(sdpAnswer.getConnection(), mediaDescriptions);

        //create and init the streams (don't start streaming just yet but wait
        //for the call to enter the connected state).
        createSendStreams(mediaDescriptions);
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. Apart
     * from simply generating an SDP response description, the method records
     * details
     *
     * @param sdpOfferStr the SDP offer that we'd like to create an answer for.
     * @param offerer the participant that has sent the offer.
     *
     * @return a String containing an SDP answer descibing parameters of the
     * <tt>Call</tt> associated with this session and matching those advertised
     * by the caller in their <tt>sdpOffer</tt>.
     *
     * @throws MediaException code INTERNAL_ERROR if processing the offer and/or
     * generating the anser fail for some reason.
     * @throws ParseException if <tt>sdpOfferStr</tt> does not contain a valid
     * sdp string.
     */
    public String processSdpOffer(CallParticipant offerer, String sdpOfferStr)
        throws MediaException, ParseException
    {
        //first parse the offer
        SessionDescription sdpOffer = null;
        try
        {
            sdpOffer = mediaServCallback.getSdpFactory()
                .createSessionDescription(sdpOfferStr);
        }
        catch (SdpParseException ex)
        {
            throw new ParseException("Failed to parse SDPOffer: "
                                     + ex.getMessage()
                                     , ex.getCharOffset());
        }

        //create an sdp answer.
        SessionDescription sdpAnswer = createSessionDescription(sdpOffer);

        //extract the remote addresses.
        Vector mediaDescriptions = null;
        try
        {
            mediaDescriptions = sdpOffer.getMediaDescriptions(true);
        }
        catch (SdpException exc)
        {
            logger.error("failed to extract media descriptions", exc);
            throw new MediaException("failed to extract media descriptions"
                                    , MediaException.INTERNAL_ERROR
                                    , exc);
        }


        //add the RTP targets
        this.initStreamTargets(sdpOffer.getConnection(), mediaDescriptions);

        //create and init the streams (don't start streaming just yet but wait
        //for the call to enter the connected state).
        createSendStreams(mediaDescriptions);

        return sdpAnswer.toString();
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
     * remote participant and the local controller.
     */
    private void createSendStreams(Vector mediaDescriptions)
        throws MediaException
    {
        //extract the encodings these media descriptions specify
        Hashtable mediaEncodings
            = extractMediaEncodings(mediaDescriptions);

        //intersect offered media encodings with those supported by the local
        //media controller
        mediaEncodings = intersectMediaEncodings(mediaEncodings);

        //make our processor output in these encodings.
        DataSource dataSource = mediaServCallback.getMediaControl()
            .createDataSourceForEncodings(mediaEncodings);

        //get all the steams that our processor creates as output.
        PushBufferStream[] streams
            = ((PushBufferDataSource)dataSource).getStreams();

        //for each stream - determine whether it is a video or an audio
        //stream and assign it to the corresponding rtpmanager
        for (int i = 0; i < streams.length; i++)
        {
            RTPManager rtpManager = null;
            if(streams[i].getFormat() instanceof VideoFormat)
            {
                rtpManager = getVideoRtpManager();
            }
            else if (streams[i].getFormat() instanceof AudioFormat)
            {
                rtpManager = getAudioRtpManager();
            }
            else
            {
                logger.warn("We are apparently capable of sending a format "
                            +" that is neither videro nor audio. Is "
                            +"this really possible?:"
                            +streams[i].getFormat());
                continue;
            }

            try
            {
                SendStream stream = rtpManager.createSendStream(dataSource, i);

                logger.trace("Created a send stream for format "
                             + streams[i].getFormat());
            }
            catch (Exception exc)
            {
                throw new MediaException(
                    "Failed to create an RTP send stream for format "
                    + streams[i].getFormat()
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
                                   Vector mediaDescriptions)
        throws MediaException, ParseException
    {
        try
        {
            String globalConnectionAddress = globalConnParam.getAddress();

            Iterator mediaDescsIter = mediaDescriptions.iterator();
            while (mediaDescsIter.hasNext())
            {
                SessionAddress target = null;
                MediaDescription mediaDescription
                    = (MediaDescription) mediaDescsIter.next();

                int port = mediaDescription.getMedia().getMediaPort();
                String type = mediaDescription.getMedia().getMediaType();

                String address = globalConnectionAddress;

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
                    inetAddress = InetAddress.getByName(address);
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
                target = new SessionAddress(inetAddress, port);

                /** @todo the following line assumes that we have a single rtp
                 * manager per media type which is not necessarily true (e.g. we
                 * may two distinct video streams: 1 for a webcam video and another
                 * one desktop capture stream) */
                RTPManager rtpManager = type.equals("video")
                    ? getVideoRtpManager()
                    : getAudioRtpManager();

                try
                {
                    rtpManager.addTarget(target);
                    logger.trace("added target " + target
                                 + " for type " + type);
                }
                catch (Exception exc)
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
     * Creates an SDP description of this session using the offer descirption
     * (if not null) for limiting
     *
     * @param offer the call participant meant to receive the offer or null if
     * we are to construct our own offer.
     * @return a SessionDescription of this CallSession.
     *
     * @throws MediaException code INTERNAL_ERROR if we get an SDP exception
     * while creating and/or parsing the sdp description.
     */
    private SessionDescription createSessionDescription(
                                                    SessionDescription offer)
        throws MediaException
    {
        try
        {
            SessionDescription sessDescr
                = mediaServCallback.getSdpFactory().createSessionDescription();

            //"v=0"
            Version v = mediaServCallback.getSdpFactory().createVersion(0);

            sessDescr.setVersion(v);

            //we don't yet implement ice so just try to choose a local address
            //that corresponds to the address of our participant.
            NetworkAddressManagerService netAddressManager
                = MediaActivator.getNetworkAddressManagerService();

            InetSocketAddress publicVideoAddress
                = netAddressManager.getPublicAddressFor(getVideoPort());

            InetSocketAddress publicAudioAddress
                = netAddressManager.getPublicAddressFor(getAudioPort());

            InetAddress publicIpAddress = publicAudioAddress.getAddress();

            String addrType
                = publicIpAddress instanceof Inet6Address
                ? Connection.IP6
                : Connection.IP4;

            //spaces in the user name mess everything up.
            //bug report - Alessandro Melzi
            Origin o = mediaServCallback.getSdpFactory().createOrigin(
                call.getProtocolProvider().getAccountID().getUserID()
                , 0
                , 0
                , "IN"
                , addrType
                , publicIpAddress.getHostAddress());

            sessDescr.setOrigin(o);
            //c=
            Connection c = mediaServCallback.getSdpFactory().createConnection(
                "IN"
                , addrType
                , publicIpAddress.getHostAddress());

            sessDescr.setConnection(c);

            //"s=-"
            SessionName s
                = mediaServCallback.getSdpFactory().createSessionName("-");
            sessDescr.setSessionName(s);

            //"t=0 0"
            TimeDescription t
                = mediaServCallback.getSdpFactory().createTimeDescription();
            Vector timeDescs = new Vector();
            timeDescs.add(t);

            sessDescr.setTimeDescriptions(timeDescs);

            //media descriptions.
            Vector offeredMediaDescriptions  = null;
            if(offer != null)
                offeredMediaDescriptions = offer.getMediaDescriptions(false);

            Vector mediaDescs
                = createMediaDescriptions(offeredMediaDescriptions
                                        , publicAudioAddress
                                        , publicVideoAddress);

            sessDescr.setMediaDescriptions(mediaDescs);

            if (logger.isTraceEnabled())
            {
                logger.trace("Generated SDP - " + sessDescr.toString());
            }

            return sessDescr;
        }
        catch (SdpException exc)
        {
            throw new MediaException(
                "An SDP exception occurred while generating local "
                + "sdp description"
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
     * <tt>CallParticipant</tt> supports as well.
     *
     * @throws SdpException we fail creating the media descriptions
     * @throws MediaException with code UNSUPPORTED_FORMAT_SET_ERROR if we don't
     * support any of the offered media formats.
     */
    private Vector createMediaDescriptions(
                                          Vector            offerMediaDescs,
                                          InetSocketAddress publicAudioAddress,
                                          InetSocketAddress publicVideoAddress)
        throws SdpException
              ,MediaException
    {
        //supported audio formats.
        String[] supportedAudioEncodings = mediaServCallback.getMediaControl()
            .getSupportedAudioEncodings();

        //supported video formats
        String[] supportedVideoEncodings = mediaServCallback.getMediaControl()
            .getSupportedVideoEncodings();

        //if there was an offer extract the offered media formats and use
        //the intersection between the formats we support and those in the
        //offer.
        if (offerMediaDescs != null && offerMediaDescs.size() > 0)
        {
            Vector offeredVideoEncodings = new Vector();
            Vector offeredAudioEncodings = new Vector();
            Iterator offerDescsIter = offerMediaDescs.iterator();

            while (offerDescsIter.hasNext())
            {
                MediaDescription desc
                    = (MediaDescription) offerDescsIter.next();
                Media media = desc.getMedia();
                String mediaType = media.getMediaType();

                if (mediaType.equalsIgnoreCase("video"))
                {
                    offeredVideoEncodings = media.getMediaFormats(true);
                    continue;
                }

                if (mediaType.equalsIgnoreCase("audio"))
                {
                    offeredAudioEncodings = media.getMediaFormats(true);
                    continue;
                }
            }

            //now intersect the offered encodings with what we support
            Hashtable encodings = new Hashtable(2);
            encodings.put("audio", offeredAudioEncodings);
            encodings.put("video", offeredVideoEncodings);
            encodings = intersectMediaEncodings(encodings);
            List intersectedAudioEncsList = (List)encodings.get("audio");
            List intersectedVideoEncsList = (List)encodings.get("video");

            //now replace the encodings arrays with the intersection
            supportedAudioEncodings
                = new String[intersectedAudioEncsList.size()];
            supportedVideoEncodings
                = new String[intersectedVideoEncsList.size()];

            for (int i = 0; i < supportedAudioEncodings.length; i++)
                supportedAudioEncodings[i]
                    = (String)intersectedAudioEncsList.get(i);

            for (int i = 0; i < supportedVideoEncodings.length; i++)
                supportedVideoEncodings[i]
                    = (String)intersectedVideoEncsList.get(i);

        }

        Vector mediaDescs = new Vector();

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

            if (!mediaServCallback.getDeviceConfiguration()
                .isAudioCaptureSupported())
            {
                am.setAttribute("recvonly", null);
            }
            mediaDescs.add(am);
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

            if (!mediaServCallback.getDeviceConfiguration()
                .isVideoCaptureSupported())
            {
                vm.setAttribute("recvonly", null);
            }
            mediaDescs.add(vm);
        }




        /** @todo record formats for participant. */

        return mediaDescs;
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
    private Hashtable intersectMediaEncodings(Hashtable offeredEncodings)
        throws MediaException
    {
        //audio encodings supported by the media controller
        String[] supportedAudioEncodings = mediaServCallback.getMediaControl()
            .getSupportedAudioEncodings();

        //video encodings supported by the media controller
        String[] supportedVideoEncodings = mediaServCallback.getMediaControl()
            .getSupportedVideoEncodings();

        //audio encodings offered by the remote party
        List offeredAudioEncodings = (List)offeredEncodings.get("audio");

        //video encodings offered by the remote party
        List offeredVideoEncodings = (List)offeredEncodings.get("video");

        //recreate the formats we create according to what the other party
        //offered.
        List supportedAudioEncsList = Arrays.asList(supportedAudioEncodings);
        List intersectedAudioEncsList = new LinkedList();
        List supportedVideoEncsList = Arrays.asList(supportedVideoEncodings);
        List intersectedVideoEncsList = new LinkedList();

        //intersect supported audio formats with offered audio formats
        if (offeredAudioEncodings != null
            && offeredAudioEncodings.size() > 0)
        {
            Iterator offeredAudioEncsIter
                = offeredAudioEncodings.iterator();

            while (offeredAudioEncsIter.hasNext())
            {
                String format = (String) offeredAudioEncsIter.next();
                if (supportedAudioEncsList.contains(format))
                    intersectedAudioEncsList.add(format);
            }
        }

        if (offeredVideoEncodings != null
            && offeredVideoEncodings.size() > 0)
        {
            //intersect supported video formats with offered video formats
            Iterator offeredVideoEncsIter = offeredVideoEncodings.iterator();

            while (offeredVideoEncsIter.hasNext())
            {
                String format = (String) offeredVideoEncsIter.next();
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

        Hashtable intersection = new Hashtable(2);
        intersection.put("audio", intersectedAudioEncsList);
        intersection.put("video", intersectedAudioEncsList);

        return intersection;
    }
    /**
     * Returns a <tt>Hashtable</tt> mapping media types (e.g. audio or video)
     * to lists of JMF encoding strings corresponding to the SDP formats
     * specified in the <tt>mediaDescriptions</tt> vector.
     * @param mediaDescriptions a <tt>Vector</tt> containing
     * <tt>MediaDescription</tt> instances extracted from an SDP offer or
     * answer.
     * @return a <tt>Hashtable</tt> mapping media types (e.g. audio or video)
     * to lists of JMF encoding strings corresponding to the SDP formats
     * specified in the <tt>mediaDescriptions</tt> vector.

     */
    private Hashtable extractMediaEncodings(Vector mediaDescriptions)
    {
        Hashtable mediaEncodings = new Hashtable(2);

        Iterator descriptionsIter = mediaDescriptions.iterator();

        while(descriptionsIter.hasNext())
        {
            MediaDescription mediaDescription
                = (MediaDescription)descriptionsIter.next();
            Media media = mediaDescription.getMedia();
            Vector mediaFormats = null;
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
                List jmfEncodings = MediaUtils.sdpToJmfEncodings(mediaFormats);
                if(jmfEncodings.size() > 0)
                    mediaEncodings.put(mediaType, jmfEncodings);
            }
        }

        return mediaEncodings;
    }

    /**
     * Create the RTP managers and bind them on some ports.
     *
     * @throws MediaException if we fail to initialize rtp manager.
     */
    public void initialize()
        throws MediaException
    {
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
                            +"using max port " + maxPortNumber);
            }
        }

        audioSessionAddress = initializeRtpManager(audioRtpManager);
        logger.debug("Bound audio rtp manager on port "
                     + audioSessionAddress.getDataPort());

        videoSessionAddress = initializeRtpManager(videoRtpManager);
        logger.debug("Bound video rtp manager on port "
                     + videoSessionAddress.getDataPort());

        call.addCallChangeListener(this);
    }

    /**
     * Allocates a local port for the RTP manager and calls its initialize
     * method.
     *
     * @param rtpManager the RTPManager to initialize.
     * @return the SessionAddress address that <tt>rtpManager</tt> was bound
     * upon.
     * @throws MediaException if we fail to initialize rtp manager.
     */
    private SessionAddress initializeRtpManager(RTPManager rtpManager)
        throws MediaException
    {
        int port = minPortNumber;
        //augment min port number so that no one else tries to bind here.
        minPortNumber += 2;

        InetAddress inAddrAny = null;

        try
        {
            //create an ipv4 any address since it also works when accepting
            //ipv6 connections.
            inAddrAny = InetAddress.getByName(NetworkUtils.IN_ADDR_ANY);


            /** @todo temp hack */
            //inAddrAny = InetAddress.getLocalHost();
        }
        catch (UnknownHostException ex)
        {
            //this shouldn't happen.
            throw new MediaException("Failed to create the ANY inet address."
                                     , MediaException.INTERNAL_ERROR
                                     , ex);
        }

        //check the number of times that we'd have to rety binding to local
        //ports before giving up.
        String bindRetriesStr
            = MediaActivator.getConfigurationService().getString(
                MediaService.BIND_RETRIES_PROPERTY_NAME);

        int bindRetries = MediaService.BIND_RETRIES_DEFAULT_VALUE;
        try
        {
            if(bindRetriesStr != null && bindRetriesStr.length() > 0)
                bindRetries = Integer.parseInt(bindRetriesStr);
        }
        catch (NumberFormatException ex)
        {
            logger.warn(bindRetriesStr
                        + " is not a valid value for number of bind retries."
                        , ex);
        }

        //try to initialize the rtp manager.
        boolean initialized = false;
        SessionAddress address = null;
        for (int i = bindRetries; i > 0; i--)
        {
            address = new SessionAddress(inAddrAny, port);
            try
            {
                rtpManager.initialize(address);
                initialized = true;
                break;
            }
            catch (InvalidSessionAddressException exc)
            {
                logger.info("port " + port + " seemed busy. "
                            +"Will retry with another port", exc);
                //get a pair port number between minPortNumber and
                //maxPortNumber.
                port +=2;
            }
            catch (IOException exc)
            {
                logger.error("Failed to init an RTP manager.", exc);
                throw new MediaException("Failed to init an RTP manager."
                                         , MediaException.IO_ERROR
                                         , exc);
            }
        }

        if(!initialized)
            throw new MediaException("Failed to bind to a local port in "
                                     + bindRetriesStr + " tries."
                                     , MediaException.INTERNAL_ERROR);

        //it appears that if we don't do this managers don't play
        // You can try out some other buffer size to see
        // if you can get better smoothness.
        BufferControl bc = (BufferControl)rtpManager
            .getControl(BufferControl.class.getName());
        if (bc != null)
        {
            long buff = bc.setBufferLength(500);
            logger.trace("set receiver buffer len to=" + buff);
            bc.setEnabledThreshold(false);
        }


        //set max packet size
        PacketSizeControl psc = (PacketSizeControl)rtpManager.getControl(
            PacketSizeControl.class.getName());

        if(psc != null)
        {
            logger.debug("Default packets size seems to be="
                + psc.getPacketSize());

            int ps = psc.setPacketSize(20);

            logger.debug("Set packet size to: " + Integer.toString(ps));

        }

        //add listeners
        rtpManager.addReceiveStreamListener(this);
        rtpManager.addSendStreamListener(this);
        rtpManager.addSessionListener(this);

        return address;

    }

    /**
     * Indicates that a change has occurred in the state of the source call.
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
        if( evt.getNewValue() == CallState.CALL_IN_PROGRESS
            && evt.getNewValue() != evt.getOldValue())
        {
            try
            {
                startStreaming();
                mediaServCallback.getMediaControl().startProcessingMedia(this);
            }
            catch (MediaException ex)
            {
                /** @todo need to notify someone */
                logger.error("Failed to start streaming.", ex);
            }
        }
        else if( evt.getNewValue() == CallState.CALL_ENDED
                 && evt.getNewValue() != evt.getOldValue())
        {
            stopStreaming();
            mediaServCallback.getMediaControl().stopProcessingMedia(this);
            /** @todo need to clean all the garbage and get rid of the session */
            /** @todo remove ourselves as listeners from the call and
             * call participant */
        }
    }


    /**
     * Indicates that a change has occurred in the status of the source
     * CallParticipant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
        /** @todo implement participantStateChanged() */
        /** @todo remove target for participant. */
    }

    /**
     * Indicates that a new call participant has joined the source call.
     * @param evt the <tt>CallParticipantEvent</tt> containing the source call
     * and call participant.
     */
    public void callParticipantAdded(CallParticipantEvent evt)
    {
        CallParticipant sourceParticipant = evt.getSourceCallParticipant();
        sourceParticipant.addCallParticipantListener(this);
    }

    /**
     * Indicates that a call participant has left the source call.
     * @param evt the <tt>CallParticipantEvent</tt> containing the source call
     * and call participant.
     */
    public void callParticipantRemoved(CallParticipantEvent evt)
    {

    }

    //-------- dummy implementations of listener methods that we don't need
    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantImageChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantTransportAddressChanged(
                                CallParticipantChangeEvent evt)
    {
        /** @todo i am not sure we should be ignoring this one ... */
    }

    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {
    }

    //implementation of jmf listener methods
    /**
     * Method called back in the SessionListener to notify
     * listener of all Session Events.SessionEvents could be one
     * of NewParticipantEvent or LocalCollisionEvent.
     *
     * @param event the newly received SessionEvent
     */
    public void update(SessionEvent event)
    {
        if (event instanceof NewParticipantEvent)
        {
            Participant participant
                = ( (NewParticipantEvent) event).getParticipant();
            if (logger.isDebugEnabled())
            {
                logger.debug("A new participant had just joined: "
                             + participant.getCNAME());
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                    "Received the following JMF Session event - "
                    + event.getClass().getName() + "=" + event);
            }
        }

    }
    /**
     * Method called back in the RTPSessionListener to notify
     * listener of all SendStream Events.
     *
     * @param event the newly received SendStreamEvent
     */
    public void update(SendStreamEvent event)
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
    public void update(ReceiveStreamEvent evt)
    {
        logger.debug("received a new incoming stream. " + evt);
        RTPManager mgr = (RTPManager) evt.getSource();
        Participant participant = evt.getParticipant(); // could be null.
        ReceiveStream stream = evt.getReceiveStream(); // could be null.
        if (evt instanceof NewReceiveStreamEvent)
        {
            try
            {
                stream = ( (NewReceiveStreamEvent) evt).getReceiveStream();
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
                Player player = Manager.createPlayer(ds);
                player.addControllerListener(this);
                player.realize();
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
    public void controllerUpdate(ControllerEvent ce)
    {
        logger.debug("Received a ControllerEvent: " + ce);
        Player player = (Player) ce.getSourceController();
        if (player == null) {
            return;
        }
        // Get this when the internal players are realized.
        if (ce instanceof RealizeCompleteEvent) {

            //set the volume as it is not on max by default.
            GainControl gc
                = (GainControl)player.getControl(GainControl.class.getName());
            if (gc != null)
            {
                logger.debug("Setting volume to max");
                gc.setLevel(1);
            }
            else
                logger.debug("Player does not have gain control.");


            logger.debug("A player was realized and will be started.");
            player.start();
/** @todo very ugly test code
  * please don't forget to remove */
java.awt.Component vc = player.getVisualComponent();
if(vc != null)
{
    javax.swing.JFrame frame = new javax.swing.JFrame();
    frame.getContentPane().add(vc);
    frame.pack();
    frame.setVisible(true);
}
        }
        if (ce instanceof StartEvent) {
            logger.debug("Received a StartEvent");
//            mediaManager.firePlayerStarting(p);
        }
        if (ce instanceof ControllerErrorEvent) {
            logger.error(
                "The following error was reported while starting a player"
                + ce);
        }
        if (ce instanceof ControllerClosedEvent) {
            logger.debug("Received a ControllerClosedEvent");
//            mediaManager.firePlayerStopped();
        }
    }
}
