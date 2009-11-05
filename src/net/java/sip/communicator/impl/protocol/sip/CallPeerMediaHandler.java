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
    private boolean localVideoTransmissionEnabled = false;

    /**
     * Determines whether or not streaming local audio is currently enabled.
     */
    private boolean localAudioTransmissionEnabled = true;

    /**
     * The minimum port number that we'd like our RTP sockets to bind upon.
     */
    private static int minPortNumber = 5000;

    /**
     * The maximum port number that we'd like our RTP sockets to bind upon.
     */
    private static int maxPortNumber = 6000;


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
     * The method creates . The
     * resources (address and port) allocated for the <tt>callPeer</tt>
     * should be kept by the media service implementation until the originating
     * <tt>callPeer</tt> enters the DISCONNECTED state. Subsequent sdp
     * offers/answers requested for the <tt>Call</tt> that the original
     * <tt>callPeer</tt> belonged to MUST receive the same IP/port couple
     * as the first one in order to allow for conferencing. The associated port
     * will be released once the call has ended.
     *
     * @return a new SDP description String advertising all params of
     * <tt>callSession</tt>.
     */
    public synchronized String createSdpOffer()
    {
        SessionDescription sess = SdpUtils.createSessionDescription();

        return sess.toString();
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
        return localVideoTransmissionEnabled;
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
        return localAudioTransmissionEnabled;
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

    /**
     *
     */
    public void init()
    {
        MediaService mediaService = SipActivator.getMediaService();

        MediaDevice aDev = mediaService.getDefaultDevice(MediaType.AUDIO);
        MediaDevice vDev = mediaService.getDefaultDevice(MediaType.VIDEO);

        Iterator<MediaFormat> aFmtIter = aDev.getSupportedFormats().iterator();
        initFormats(aFmtIter);

        Iterator<MediaFormat> vFmtIter = vDev.getSupportedFormats().iterator();
        initFormats(vFmtIter);
    }

    private void initFormats(Iterator<MediaFormat> fmtsIter)
    {
        while(fmtsIter.hasNext())
        {
            logger.error("Foramt="+fmtsIter.next());
        }
    }

    private void createStreamConnector()
    {
        NetworkAddressManagerService nam
                            = SipActivator.getNetworkAddressManagerService();

        InetAddress intendedDestination = peer.getProtocolProvider()
            .getIntendedDestination(peer.getPeerAddress());

        InetAddress localHostForPeer = nam.getLocalHost(intendedDestination);

        DatagramSocket rtpSocket = nam.createDatagramSocket(localHostForPeer, minPort, maxPort)
        DefaultStreamConnector audioConnector = new DefaultStreamConnector();

        DefaultStreamConnector videoConnector = new DefaultStreamConnector();


    }

    /**
     * (Re)Sets the <tt>minPortNumber</tt> and <tt>maxPortNumber</tt> to their
     * defaults or to the values specified in the <tt>ConfigurationService</tt>.
     */
    private void initializePortNumbers()
    {
        //first reset to default values
        minPortNumber = 5000;
        maxPortNumber = 6000;

        //then set to anything the user might have specified.
        String minPortNumberStr = SipActivator.getConfigurationService()
            .getString(OperationSetBasicTelephony
                            .MIN_MEDIA_PORT_NUMBER_PROPERTY_NAME);

        if (minPortNumberStr != null)
        {
            try
            {
                minPortNumber = Integer.parseInt(minPortNumberStr);
            }
            catch (NumberFormatException ex)
            {
                logger.warn(minPortNumberStr
                            + " is not a valid min port number value. "
                            +"using min port " + minPortNumber);
            }
        }

        String maxPortNumberStr = SipActivator.getConfigurationService()
            .getString(OperationSetBasicTelephony
                            .MAX_MEDIA_PORT_NUMBER_PROPERTY_NAME);

        if (maxPortNumberStr != null)
        {
            try
            {
                maxPortNumber = Integer.parseInt(maxPortNumberStr);
            }
            catch (NumberFormatException ex)
            {
                logger.warn(maxPortNumberStr
                            + " is not a valid max port number value. "
                            +"using max port " + maxPortNumber,
                            ex);
            }
        }
    }
}
