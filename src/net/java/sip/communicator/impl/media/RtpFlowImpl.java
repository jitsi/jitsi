/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.rtp.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.rtp.event.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.MediaException;
import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.media.event.MediaEvent;
import net.java.sip.communicator.util.*;

/**
 * Implementation of a <tt>RtpFlow</tt> which is a bridge for data transmission
 * between two end points. Data which transit on this flow are encoded with
 * a specified format.
 *
 * @author Symphorien Wanko
 * @author KenLarson
 */
public class RtpFlowImpl
        implements RtpFlow,
        ReceiveStreamListener,
        SessionListener,
        ControllerListener
{
    /**
     * Logger for this class
     */
    private static final Logger logger
        = Logger.getLogger(RtpFlowImpl.class);

    /**
     * Our IP address
     */
    private String localAddress = null;

    /**
     * IP to which this <tt>flow send media</tt>
     */
    private String remoteAddress = null;

    /**
     * The local port used by this <tt>RtpFlow</tt>
     */
    private int localPort = -1;

    /**
     * Port for the remote endpoint
     */
    private int remotePort = -1;

    /**
     * Collection of <tt>RtpManager</tt>s used by this <tt>RtpFlow</tt>
     */
    private RTPManager rtpMgrs[] = null;

    /**
     * Data source used for media capture
     */
    private DataSource dataSource = null;

    /**
     * Collection of send streams used by this <tt>RtpFlow</tt>
     */
    private final List<SendStream> sendStreams = new ArrayList<SendStream>();

    /**
     * The media on which this <tt>RtpFlow</tt> depend,
     * the one which created us
     */
    private MediaServiceImpl mediaService = null;

    /**
     * Used for controlling media
     */
    private MediaControl mediaControl = null;

    /**
     * Media encoding passed to JMF via the media control
     */
    private final Hashtable<String, List<String>> mediaEncoding
            = new Hashtable<String, List<String>>();

    /**
     * A list of listeners registered for media events.
     */
    private final List<MediaListener> mediaListeners
            = new Vector<MediaListener>();

    /**
     * Creates an instance of <tt>RtpFlowImpl</tt> for media transmission.
     * A flow do not care about session or anything other than transmitting
     * media data between two end points, using the given media encoding.
     *
     * @param mediaServie the media service which created us
     * @param localIpAddress local IP address
     * @param localPort local port number
     * @param remoteIpAddress remote IP address
     * @param remotePort remote port number
     * @param mediaEncoding media encoding used for data
     *
     * @throws MediaException if initializing the flow fails.
     */
    public RtpFlowImpl(MediaServiceImpl mediaServie,
                       String localIpAddress,
                       String remoteIpAddress,
                       int localPort,
                       int remotePort,
                       Hashtable<String, List<String>> mediaEncoding)
            throws MediaException
    {
        this.localAddress = localIpAddress;
        this.remoteAddress = remoteIpAddress;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.mediaService = mediaServie;
        this.mediaControl = mediaService.getMediaControl();
        this.mediaEncoding.putAll(mediaEncoding);

        initialize();
    }

    /**
     * Returns the local port used by this flow.
     *
     * @return localPort the local port used by this flow.
     */
    public int getLocalPort()
    {
        return localPort;
    }

    /**
     * Returns the local address used by this flow.
     *
     * @return localAddress the local address port used by this flow.
     */
    public String getLocalAddress()
    {
        return localAddress;
    }

    /**
     * Returns the remote port used by this flow.
     *
     * @return remotePort the remote port used by this flow.
     */
    public int getRemotePort()
    {
        return remotePort;
    }

    /**
     * Returns the remote address used by this flow.
     *
     * @return remoteAddress the remote address port used by this flow.
     */
    public String getRemoteAddress()
    {
        return remoteAddress;
    }

    /**
     * This method initializes the <tt>RtpFlow</tt> by creating
     * datasource and associated transmitter. We also create session for
     * each JMF track here.
     *
     * @throws MediaException if initialization fails
     */
    private void initialize() throws MediaException
    {
        dataSource = mediaControl.createDataSourceForEncodings(mediaEncoding);

        PushBufferDataSource pbds = (PushBufferDataSource) dataSource;
        PushBufferStream pbss[] = pbds.getStreams();

        rtpMgrs = new RTPManager[pbss.length];
        SessionAddress localAddr, destAddr;
        InetAddress ipAddr;
        SendStream sendStream;

        int port;

        for (int i = 0; i < pbss.length; i++)
        {
            try
            {
                rtpMgrs[i] = RTPManager.newInstance();

                CallSessionImpl.registerCustomCodecFormats(rtpMgrs[i]);
                port = remotePort + 2 * i;
                ipAddr = InetAddress.getByName(remoteAddress);

                localAddr = new SessionAddress(InetAddress.
                        getByName(this.localAddress), localPort);

                destAddr = new SessionAddress(ipAddr, port);

                rtpMgrs[i].addReceiveStreamListener(this);
                rtpMgrs[i].addSessionListener(this);

                BufferControl bc = (BufferControl) rtpMgrs[i]
                        .getControl("javax.media.control.BufferControl");
                if (bc != null)
                {
                    int bl = 160;
                    bc.setBufferLength(bl);
                }

                try
                {
                    rtpMgrs[i].initialize(localAddr);
                }
                catch (InvalidSessionAddressException e)
                {
                    // In case the local address is not allowed to read,
                    // we user another local address
                    SessionAddress sessAddr = new SessionAddress();
                    localAddr = new SessionAddress(sessAddr.getDataAddress(),
                            localPort);
                    rtpMgrs[i].initialize(localAddr);
                }

                rtpMgrs[i].addTarget(destAddr);
                if (logger.isInfoEnabled())
                    logger.info("Created RTP session at " + localPort +
                        " to: " + remoteAddress + " " + port);
                sendStream = rtpMgrs[i].createSendStream(dataSource, i);
                sendStreams.add(sendStream);
                sendStream.start();
            }
            catch (Exception e)
            {
                throw new MediaException("Failed to create transmitter"
                        , MediaException.INTERNAL_ERROR, e);
            }
        }
    }

    /**
     * Implementation of <tt>start</tt> to send media data.
     */
    public void start()
    {
        mediaControl.startProcessingMedia(this);
    }

    /**
     * Stops the transmission if already started.
     * Stops receiving also.
     */
    public void stop()
    {
        RTPManager rtpMgr;
        for (int i = 0; i < rtpMgrs.length; i++)
        {
            rtpMgr = rtpMgrs[i];
            rtpMgr.removeReceiveStreamListener(this);
            rtpMgr.removeSessionListener(this);
            rtpMgr.removeTargets("Session ended.");
            rtpMgr.dispose();
        }
        sendStreams.clear();
        mediaControl.stopProcessingMedia(this);
    }

    /**
     * Resume media transmission on this flow
     */
     public void resume()
     {
        if (logger.isInfoEnabled())
            logger.info("resuming transmission... ");

        for (SendStream sendStream : sendStreams)
        {
            try
            {
                sendStream.start();
            }
            catch (IOException ex)
            {
                logger.warn("Exception when resuming transmission ", ex);
            }
        }
     }

    /**
     * Pause media transmission on this flow
     */
     public void pause()
     {
        if (logger.isInfoEnabled())
            logger.info("pausing transmission... ");

        for (SendStream sendStream : sendStreams)
        {
            try
            {
                sendStream.stop();
            }
            catch (IOException ex)
            {
                logger.warn("Exception when pausing transmission ", ex);
            }
        }
     }

    /**
     * Implements update from javax.media.rtp.SessionListener
     *
     * @param evt received event
     */
    public synchronized void update(SessionEvent evt)
    {
        if (evt instanceof NewParticipantEvent)
        {
            Participant p = ((NewParticipantEvent) evt).getParticipant();
            if (logger.isInfoEnabled())
                logger.info("A new participant had just joined: " + p.getCNAME());
        }
    }

    /**
     * Implements update from javax.media.rtp.ReceiveStreamListener
     *
     * @param evt received event
     */
    public synchronized void update(ReceiveStreamEvent evt)
    {
        Participant participant = evt.getParticipant();    // could be null.
        ReceiveStream stream = evt.getReceiveStream();  // could be null.

        if (evt instanceof RemotePayloadChangeEvent)
        {
            logger.warn("Received an RTP PayloadChangeEvent," +
                    " not supported cannot handle payload change.");
        }
        else if (evt instanceof NewReceiveStreamEvent)
        {

            try
            {
                stream = evt.getReceiveStream();
                DataSource ds = stream.getDataSource();

                // Find out the formats.
                RTPControl ctl =
                        (RTPControl) ds.getControl("javax.jmf.rtp.RTPControl");
                if (ctl != null)
                {
                    if (logger.isInfoEnabled())
                        logger.info("Recevied new RTP stream: " + ctl.getFormat());
                }
                else
                    if (logger.isInfoEnabled())
                        logger.info("Recevied new RTP stream");

                if (participant == null)
                    if (logger.isInfoEnabled())
                        logger.info("The sender of this stream" +
                            "had yet to be identified.");
                else
                {
                    if (logger.isInfoEnabled())
                        logger.info("The stream comes from: " +
                            participant.getCNAME());
                }

                // create a player by passing datasource to the Media Manager
                Player p = javax.media.Manager.createPlayer(ds);
                if (p == null)
                    return;

                p.addControllerListener(this);
                p.realize();
                
                fireMediaEvent((participant != null) ? participant.getCNAME() : "");
            }
            catch (Exception e)
            {
                logger.warn("NewReceiveStreamEvent exception ", e);
                return;
            }

        }
        else if (evt instanceof StreamMappedEvent)
        {

            if (stream != null && stream.getDataSource() != null)
            {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl =
                        (RTPControl) ds.getControl("javax.jmf.rtp.RTPControl");
                if (logger.isInfoEnabled())
                    logger.info("The previously unidentified stream ");
                if (ctl != null)
                    if (logger.isInfoEnabled())
                        logger.info(": " + ctl.getFormat());
                if (logger.isInfoEnabled())
                    logger.info(" had now been identified as sent by: "
                        + participant.getCNAME());
            }
        }
        else if (evt instanceof ByeEvent)
        {
            if (logger.isInfoEnabled())
                logger.info("Got \"bye\" from: " + participant.getCNAME());
        }

    }

    /**
     * Implements controllerUpdate from javax.media.rtp.ControllerListener
     *
     * @param ce received event
     */
    public synchronized void controllerUpdate(ControllerEvent ce)
    {

        Player p = (Player) ce.getSourceController();

        if (p == null)
            return;

        // Get this when the internal players are realized.
        if (ce instanceof RealizeCompleteEvent)
        {
            p.start();
        }
        else if (ce instanceof ControllerErrorEvent)
        {
            p.removeControllerListener(this);
            logger.warn("Receiver internal error " + ce);
        }
    }

    /**
     * Add a listener to be informed of media events hapening
     * on this flow.
     */
    public void addMediaListener(MediaListener listener)
    {
        synchronized(mediaListeners)
        {
            if (!mediaListeners.contains(listener))
                mediaListeners.add(listener);
        }
    }

    /**
     * Notify listeners that we have received media.
     *
     * @param from origin of the media
     */
    private void fireMediaEvent(String from)
    {
        MediaEvent mediaEvent = new MediaEvent(this, from);

        MediaListener[] listeners;
        synchronized(mediaListeners)
        {
            listeners =
                mediaListeners.toArray(new MediaListener[mediaListeners.size()]);
        }

        for (MediaListener listener : listeners)
        {
            listener.receivedMediaStream(mediaEvent);
        }
    }
}
