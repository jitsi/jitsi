/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
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
     * The RTP/RTCP socket couple that this media handler should use to send
     * and receive video flows through.
     */
    private StreamConnector videoStreamConnector = null;

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
        /** @todo implement */
        return false;
    }

    public void init()
        throws OperationFailedException
    {
        MediaService mediaService = SipActivator.getMediaService();

        //media connectors.
        audioStreamConnector = createStreamConnector();
        videoStreamConnector = createStreamConnector();

        //Audio Media Description
        Vector<MediaDescription> mediaDescs = new Vector<MediaDescription>();

        MediaDevice aDev = mediaService.getDefaultDevice(MediaType.AUDIO);
        MediaDirection audioDirection
            = aDev.getDirection().and(audioDirectionUserPreference);

        if(audioDirection != MediaDirection.INACTIVE);
        {
            mediaDescs.add(createMediaDescription(
                            aDev, this.audioStreamConnector, audioDirection));
        }


        MediaDevice vDev = mediaService.getDefaultDevice(MediaType.VIDEO);
        MediaDirection videoDirection
            = vDev.getDirection().and(videoDirectionUserPreference);

        if(videoDirection != MediaDirection.INACTIVE);
        {
            mediaDescs.add(createMediaDescription(
                            vDev, this.videoStreamConnector, videoDirection));
        }

        String userName = peer.getProtocolProvider().getAccountID().getUserID();

        SessionDescription sDes = SdpUtils.createSessionDescription(
            videoStreamConnector.getDataSocket().getLocalAddress(),
            userName, mediaDescs);

        System.out.println(sDes.toString());

    }

    /**
     * Generates an SDP <tt>MediaDescription</tt> for <tt>MediaDevice</tt>
     * taking account the local streaming preference for the corresponding
     * media type.
     *
     * @param dev the <tt>MediaDevice</tt> that we'd like to generate a media
     * description for.
     *
     * @return a newly created <tt>MediaDescription</tt> representing streams
     * that we'd be able to handle with <tt>dev</tt>.
     *
     * @throws OperationFailedException
     */
    private MediaDescription createMediaDescription(MediaDevice     dev,
                                                    StreamConnector connector,
                                                    MediaDirection  direction)
        throws OperationFailedException
    {

        List<MediaFormat> formats = dev.getSupportedFormats();

        return SdpUtils.createMediaDescription(
           formats, connector, direction, dynamicPayloadTypes);
    }

    private void initFormats(Iterator<MediaFormat> fmtsIter)
    {
        while(fmtsIter.hasNext())
        {

            logger.error("Foramt="+fmtsIter.next());
        }
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
}
