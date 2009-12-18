/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.mediamgr;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.*;
//import net.java.sip.communicator.service.media.*;
//import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smackx.jingle.*;
import org.jivesoftware.smackx.jingle.media.*;
import org.jivesoftware.smackx.jingle.nat.*;

/**
 * This Class implements a JingleMediaSession. which serve as a wrapper
 * for a RtpFlow, allowing us to receive and transmit audio.
 *
 * based on Thiago Camargo's media manager from jingle
 *
 * @author Symphorien Wanko
 */
public class AudioMediaSession
        extends JingleMediaSession
//        implements MediaListener
{
    /**
     * the logger used by this class
     */
    private static final Logger logger
            = Logger.getLogger(AudioMediaSession.class);

    /**
     * An audio media session encapsulate a RtpFlow which handle media transfer
     */
//    private RtpFlow rtpFlow = null;

    /**
     * Creates an AudioMediaSession with defined payload type,
     * remote and local candidates
     *
     * @param payloadType Payload used by jmf
     * @param remote      the remote information.
     * @param local       the local information.
     * @param jingleSession the session for which we create a media session
     */
    public AudioMediaSession(PayloadType payloadType, TransportCandidate remote
            , TransportCandidate local, JingleSession jingleSession)
    {
        this(payloadType, remote, local, null, null);
        initialize();
    }

    /**
     * This constructor is used only to match the one of our super class.
     * In SC, An audioSession don't need the media locator asked by
     * <tt>jingleMediaSession</tt> because the locator is handled
     * by the media service.
     *
     * @param payloadType Payload used by jmf
     * @param remote      the remote information.
     * @param local       the local information.
     * @param locator     media locator
     * @param jingleSession the session for which we create a media session
     */
    private AudioMediaSession(PayloadType payloadType, TransportCandidate remote
            , TransportCandidate local, String locator, JingleSession jingleSession)
    {
        //super(payloadType, remote, local, locator==null?"dsound://":locator, jingleSession);
        super(payloadType, remote, local, locator, jingleSession);
    }

    /**
     * Initialize the RtpFlow to make us able to send and receive audio media
     */
    public void initialize()
    {
        String remoteIp;
        String localIp;
        int localPort;
        int remotePort;

//        if (getLocal().getSymmetric() != null)
//        {
//            remoteIp = getLocal().getIp();
//            localIp = getLocal().getLocalIp();
//            localPort = getFreePort();
//            remotePort = getLocal().getSymmetric().getPort();
//        }
//        else
//        {
        if (getLocal().getSymmetric() != null)
        {
            logger.warn("oops : unexpected situation ... ");
            // TODO : if this situation happens only one
            // un comment the above code wich is meant to handle this
        }

        remoteIp = getRemote().getIp();
        localIp = getLocal().getLocalIp();
        localPort = getLocal().getPort();
        remotePort = getRemote().getPort();
        logger.info("AudioMediaSession : " + localIp + ":" + localPort + 
                    " <-> " + remoteIp + ":" + remotePort);
//        }

        Map<String, List<String>> mediaEncoding
            = MediaUtils.getAudioEncoding(getPayloadType().getId());
//        try
//        {
//            rtpFlow = JabberActivator.getMediaService().
//                    createRtpFlow(
//                    localIp, localPort,
//                    remoteIp, remotePort,
//                    mediaEncoding);
//        }
//        catch (MediaException ex)
//        {
//            logger.error("failed to create a RtpFlow between " +
//                    localIp + ":" + localPort + " and " +
//                    remoteIp + ":" + remotePort, ex);
//            rtpFlow = null;
//            throw new RuntimeException("failed to create a RtpFlow between "
//                                        + localIp + ":" + localPort + " and "
//                                        + remoteIp + ":" + remotePort,
//                                        ex);
//        }
    }

    /**
     * Implements <tt>startTransmit</tt> from <tt>JingleMediaSession.</tt>
     */
    public void startTrasmit()
    {
//        rtpFlow.start();
    }

    /**
     * Implements <tt>startReceive</tt> from <tt>JingleMediaSession.</tt>
     */
    public void startReceive()
    {}

    /**
     * Implements <tt>setTrasmit</tt> from <tt>JingleMediaSession.</tt>
     *
     * @param active pause the transmission if false, resume otherwise
     */
    public void setTrasmit(boolean active)
    {
//        if (active)
//            rtpFlow.resume();
//        else
//            rtpFlow.pause();
    }

    /**
     * Implements <tt>stopTrasmit</tt> from <tt>JingleMediaSession.</tt>
     */
    public void stopTrasmit()
    {
        //if (rtpFlow != null)
//        rtpFlow.stop();
    }

    /**
     * Implements <tt>stopReceive</tt> from <tt>JingleMediaSession.</tt>
     */
    public void stopReceive()
    {}

//    /**
//     * Obtain a free port we can use.
//     *
//     * @return A free port number.
//     */
//    protected int getFreePort()
//    {
//        //perhaps this method will be better in an utily class
//
//        ServerSocket ss;
//        int freePort = 0;
//        
//        for (int i = 0; i < 10; i++)
//        {
//            freePort = (int) (10000 + Math.round(Math.random() * 10000));
//            freePort = freePort % 2 == 0 ? freePort : freePort + 1;
//            try
//            {
//                ss = new ServerSocket(freePort);
//                freePort = ss.getLocalPort();
//                ss.close();
//                return freePort;
//            }
//            catch (IOException e)
//            {
//                e.printStackTrace();
//            }
//        }
//        try
//        {
//            ss = new ServerSocket(0);
//            freePort = ss.getLocalPort();
//            ss.close();
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//        
//        return freePort;
//    }

    /**
     * Implementation of <tt>receivedMediaStream</tt> from
     * <tt>RtpFlow</tt>.
     */
//    public void receivedMediaStream(MediaEvent evt)
//    {
//        mediaReceived(evt.getFrom());
//    }
//
//    public void mediaServiceStatusChanged()
//    {
//    }
}
