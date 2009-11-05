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
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The media handler class handles all media management for a single
 * <tt>CallPeer</tt>. This includes initializing and configuring streams,
 * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt> always
 * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
 * both classes are only separated for reasons of readability.
 *
 * @author Emil Ivov
 */
public class CallPeerMediaHandler
{
    /**
     * Our class logger.
     */
    private Logger logger = Logger.getLogger(CallPeerMediaHandler.class);
    /**
     * A reference to the CallPeerSipImpl instance that this handler is
     * managing media streams for.
     */
    private final CallPeerSipImpl peer;

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
     * The RTP/RTCP socket couple that this media handler should use to send
     * and receive audio flows through.
     */
    private StreamConnector audioStreamConnector = null;

    /**
     * The RTP stream that this media handler uses to send audio.
     */
    private AudioMediaStream audioStream = null;

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
     * A <tt>URL</tt> pointing to a location with call information or a call
     * control web interface related to the <tt>CallPeer</tt> that we are
     * associated with.
     */
    private URL callInfoURL = null;

    /**
     * Contains all dynamic for payload type mappings that have been made for
     * this call.
     */
    private final DynamicPayloadTypeRegistry dynamicPayloadTypes
        = new DynamicPayloadTypeRegistry();

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
        if (enabled)
            videoDirectionUserPreference = MediaDirection.SENDRECV;
        else
            videoDirectionUserPreference = MediaDirection.RECVONLY;
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
        if(enabled)
            audioDirectionUserPreference = MediaDirection.SENDRECV;
        else
            audioDirectionUserPreference = MediaDirection.RECVONLY;
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
     * Determines whether the audio stream of this media handler is currently
     * on mute.
     *
     * @return <tt>true</tt> if local audio transmission is currently on mute
     * and <tt>false</tt> otherwise.
     */
    public boolean isMute()
    {
        return this.audioStream != null && audioStream.isMute();
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
        if (this.audioStream == null)
            return;

        audioStream.setMute(mute);
    }

    public String createOffer()
        throws OperationFailedException
    {
        return createFirstOffer().toString();
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

    private Vector<MediaDescription> createMediaDescriptions()
        throws OperationFailedException
    {
        MediaService mediaService = SipActivator.getMediaService();

        //Audio Media Description
        Vector<MediaDescription> mediaDescs = new Vector<MediaDescription>();

        MediaDevice aDev = mediaService.getDefaultDevice(MediaType.AUDIO);

        if (aDev != null)
        {
            MediaDirection audioDirection
                = aDev.getDirection().and(audioDirectionUserPreference);

            if(audioDirection != MediaDirection.INACTIVE);
            {
                mediaDescs.add(createMediaDescription(
                        aDev.getSupportedFormats(),
                        getStreamConnector(MediaType.AUDIO), audioDirection));
            }
        }

        //Video Media Description
        MediaDevice vDev = mediaService.getDefaultDevice(MediaType.VIDEO);

        if(vDev != null)
        {
            MediaDirection videoDirection
                = vDev.getDirection().and(videoDirectionUserPreference);

            if(videoDirection != MediaDirection.INACTIVE);
            {
                mediaDescs.add(createMediaDescription(
                        vDev.getSupportedFormats(),
                        getStreamConnector(MediaType.VIDEO), videoDirection));
            }
        }

        //fail if all devices were inactive
        if(mediaDescs.size() == 0)
        {
             ProtocolProviderServiceSipImpl.throwOperationFailedException(
                 "We couldn't find any active Audio/Video devices and "
                 +"couldn't create a call",
                 OperationFailedException.GENERAL_ERROR,
                 null,
                 logger);
        }

        return mediaDescs;

    }

    private SessionDescription createUpdateOffer(
                                        SessionDescription sdescToUpdate)
        throws OperationFailedException

    {
        //create the media descriptions reflecting our current state.
        Vector<MediaDescription> newMediaDescs = createMediaDescriptions();

        SessionDescription newOffer = SdpUtils.createSessionUpdateDescription(
                        sdescToUpdate, getLastUsedLocalHost(), newMediaDescs);

        return newOffer;
    }



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

    private MediaStream initStream(StreamConnector      connector,
                                   MediaDevice          device,
                                   MediaFormat          format,
                                   MediaStreamTarget    target,
                                   MediaDirection       direction)
        throws OperationFailedException
    {
        MediaService mediaService = SipActivator.getMediaService();

        MediaStream stream = null;

        if (device.getMediaType() == MediaType.AUDIO)
            stream = this.audioStream;
        if (device.getMediaType() == MediaType.AUDIO)
            stream = this.videoStream;

        if (stream == null)
        {
            stream = mediaService.createMediaStream(connector, device);
        }
        else
        {
            //this is a reinitialization so make sure we stop the stream
            stream.stop();
        }

        return  configureStream(connector, device, format,
                        target, direction, stream);
    }

    private MediaStream configureStream(StreamConnector      connector,
                                        MediaDevice          device,
                                        MediaFormat          format,
                                        MediaStreamTarget    target,
                                        MediaDirection       direction,
                                        MediaStream          stream)
       throws OperationFailedException
    {
        registerDynamicPTsWithStream(stream);

        stream.setFormat(format);
        stream.setTarget(target);
        stream.setDirection(direction);

        if( stream instanceof AudioMediaStream)
            this.audioStream = (AudioMediaStream)stream;
        else
            this.videoStream = (VideoMediaStream)stream;

        stream.start();

        return stream;
    }

    private void registerDynamicPTsWithStream(MediaStream stream)
    {
        for ( Map.Entry<MediaFormat, Byte> mapEntry
                        : dynamicPayloadTypes.getMappings().entrySet())
        {
            byte pt = mapEntry.getValue().byteValue();
            MediaFormat fmt = mapEntry.getKey();
            stream.addDynamicRTPPayloadType(pt, fmt);
        }
    }

    public SessionDescription processFirstOffer(SessionDescription offer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        this.remoteSess = offer;

        Vector<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(offer);

        MediaService mediaService = SipActivator.getMediaService();

        //prepare to generate answers to all the incoming descriptions
        Vector<MediaDescription> answerDescriptions
            = new Vector<MediaDescription>(remoteDescriptions.size());

        this.setCallInfoURL(SdpUtils.getCallInfoURL(offer));

        boolean atLeastOneValidDescription = false;

        for ( MediaDescription mediaDescription : remoteDescriptions)
        {
            MediaType mediaType = SdpUtils.getMediaType(mediaDescription);

            List<MediaFormat> supportedFormats = SdpUtils.extractFormats(
                            mediaDescription, dynamicPayloadTypes);

            MediaDevice dev = mediaService.getDefaultDevice(mediaType);
            MediaDirection devDirection = dev.getDirection();

            //stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, offer);

            if (supportedFormats == null || supportedFormats.size() == 0
                || dev == null || devDirection == MediaDirection.INACTIVE
                || target.getDataAddress().getPort() == 0)
            {
                //mark stream as dead and go on bravely
                answerDescriptions.add(
                             SdpUtils.createDisablingAnswer(mediaDescription));
                continue;
            }

            StreamConnector connector = getStreamConnector(mediaType);

            //determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);

            MediaDirection direction
                = devDirection.getDirectionForAnswer(remoteDirection);

            //create the corresponding stream
            initStream( connector, dev, supportedFormats.get(0),
                            target, direction);

            //create the answer description
            answerDescriptions.add( createMediaDescription(
                            supportedFormats, connector, direction));

            atLeastOneValidDescription = true;
        }

        if(!atLeastOneValidDescription)
            throw new OperationFailedException("Offer contained no valid "
                + "media descriptions.",
                OperationFailedException.ILLEGAL_ARGUMENT);

        //wrap everything up in a session description
        SessionDescription answer = SdpUtils.createSessionDescription(
            getLastUsedLocalHost(), getUserName(), answerDescriptions);

        this.localSess = answer;

        return localSess;
    }

    public void processAnswer(SessionDescription answer)
        throws OperationFailedException, IllegalArgumentException
    {
        this.remoteSess = answer;

        Vector<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(answer);

        this.setCallInfoURL(SdpUtils.getCallInfoURL(answer));

        MediaService mediaService = SipActivator.getMediaService();

        for ( MediaDescription mediaDescription : remoteDescriptions)
        {
            MediaType mediaType = SdpUtils.getMediaType(mediaDescription);

            List<MediaFormat> supportedFormats = SdpUtils.extractFormats(
                            mediaDescription, dynamicPayloadTypes);

            MediaDevice dev = mediaService.getDefaultDevice(mediaType);
            MediaDirection devDirection = dev.getDirection();

            if (supportedFormats == null || supportedFormats.size() == 0
                || dev == null || devDirection == MediaDirection.INACTIVE)
            {
                //remote party must have messed up our SDP. throw an exception.
                ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "Remote party sent an invlid SDP answer.",
                     OperationFailedException.ILLEGAL_ARGUMENT, null, logger);
            }

            StreamConnector connector = getStreamConnector(mediaType);

            //determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);

            MediaDirection direction
                = devDirection.getDirectionForAnswer(remoteDirection);

            //stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, answer);

            //create the corresponding stream
            initStream( connector, dev, supportedFormats.get(0),
                            target, direction);
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
     * @param formats the list of <tt>MediaFormats</tt> that we'd like to
     * advertise.
     * @param connector the <tt>StreamConnector</tt> that we will be using
     * for the stream represented by the description we are creating.
     * @param direction the <tt>MediaDirection</tt> that we'd like to establish
     * the stream in.
     *
     * @return a newly created <tt>MediaDescription</tt> representing streams
     * that we'd be able to handle with <tt>dev</tt>.
     *
     * @throws OperationFailedException if generating the
     * <tt>MediaDescription</tt> fails for some reason.
     */
    private MediaDescription createMediaDescription(
                                                  List<MediaFormat> formats,
                                                  StreamConnector   connector,
                                                  MediaDirection    direction)
        throws OperationFailedException
    {
        return SdpUtils.createMediaDescription(
           formats, connector, direction, dynamicPayloadTypes);
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

        //create the RTCP socket
        DefaultStreamConnector connector = new DefaultStreamConnector(
                        rtpSocket, rtcpSocket);

        return connector;
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
}
