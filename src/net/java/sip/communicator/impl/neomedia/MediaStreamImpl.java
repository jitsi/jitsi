/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.net.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * @author Lubomir Marinov
 */
public class MediaStreamImpl
    extends AbstractMediaStream
    implements ControllerListener,
               ReceiveStreamListener,
               SendStreamListener,
               SessionListener
{
    private static final Logger logger
        = Logger.getLogger(MediaStreamImpl.class);

    /**
     * The name of the property indicating the length of our receive buffer.
     */
    private static final String PROPERTY_NAME_RECEIVE_BUFFER_LENGTH
        = "net.java.sip.communicator.impl.media.RECEIVE_BUFFER_LENGTH";

    /**
     * The <tt>MediaDevice</tt> this instance uses for both capture and playback
     * of media.
     */
    private CaptureMediaDevice device;

    /**
     * The <tt>RTPConnector</tt> through which this instance sends and receives
     * RTP and RTCP traffic.
     */
    private final RTPConnectorImpl rtpConnector;

    private RTPManager rtpManager;

    private MediaDirection startedDirection;

    /**
     * Initializes a new <tt>MediaStreamImpl</tt> instance which will use the
     * specified <tt>MediaDevice</tt> for both capture and playback of media
     * exchanged via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the <tt>StreamConnector</tt> the new instance is to use
     * for sending and receiving media
     * @param device the <tt>MediaDevice</tt> the new instance is to use for
     * both capture and playback of media exchanged via the specified
     * <tt>StreamConnector</tt>
     */
    public MediaStreamImpl(StreamConnector connector, MediaDevice device)
    {

        /*
         * XXX Set the device early in order to make sure that its of the right
         * type because we do not support just about any MediaDevice yet.
         */
        setDevice(device);

        this.rtpConnector = new RTPConnectorImpl(connector);
    }

    /*
     * Implements MediaStream#close().
     */
    public void close()
    {
        stop();

        rtpConnector.removeTargets();

        if (rtpManager != null)
        {
            rtpManager.removeReceiveStreamListener(this);
            rtpManager.removeSendStreamListener(this);
            rtpManager.removeSessionListener(this);
            rtpManager.dispose();
            rtpManager = null;
        }

        getDevice().close();
    }

    /*
     * Implements ControllerListener#controllerUpdate(ControllerEvent).
     */
    public void controllerUpdate(ControllerEvent event)
    {
        if (event instanceof RealizeCompleteEvent)
        {
            Player player = (Player) event.getSourceController();

            if (player != null)
                player.start();
        }
    }

    private void createSendStreams()
    {
        RTPManager rtpManager = getRTPManager();
        DataSource dataSource = getDevice().getDataSource();
        int streamCount;

        if (dataSource instanceof PushBufferDataSource)
        {
            PushBufferStream[] streams
                = ((PushBufferDataSource) dataSource).getStreams();

            streamCount = (streams == null) ? 0 : streams.length;
        }
        else if (dataSource instanceof PushDataSource)
        {
            PushSourceStream[] streams
                = ((PushDataSource) dataSource).getStreams();

            streamCount = (streams == null) ? 0 : streams.length;
        }
        else if (dataSource instanceof PullBufferDataSource)
        {
            PullBufferStream[] streams
                = ((PullBufferDataSource) dataSource).getStreams();

            streamCount = (streams == null) ? 0 : streams.length;
        }
        else if (dataSource instanceof PullDataSource)
        {
            PullSourceStream[] streams
                = ((PullDataSource) dataSource).getStreams();

            streamCount = (streams == null) ? 0 : streams.length;
        }
        else
            streamCount = 1;

        for (int streamIndex = 0; streamIndex < streamCount; streamIndex++)
        {
            Throwable exception = null;

            try
            {
                rtpManager.createSendStream(dataSource, streamIndex);
            }
            catch (IOException ioe)
            {
                exception = ioe;
            }
            catch (UnsupportedFormatException ufe)
            {
                exception = ufe;
            }

            if (exception != null)
            {
                // TODO
                logger
                    .error(
                        "Failed to create send stream for data source "
                            + dataSource
                            + " and stream index "
                            + streamIndex,
                        exception);
            }
        }
    }

    /*
     * Implements MediaStream#getDevice().
     */
    public CaptureMediaDevice getDevice()
    {
        return device;
    }

    /*
     * Implements MediaStream#getFormat().
     */
    public MediaFormat getFormat()
    {
        return getDevice().getFormat();
    }

    /*
     * Implements MediaStream#getLocalSourceID().
     */
    public String getLocalSourceID()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * Implements MediaStream#getRemoteControlAddress().
     */
    public InetSocketAddress getRemoteControlAddress()
    {
        return
            (InetSocketAddress)
                rtpConnector.getControlSocket().getRemoteSocketAddress();
    }

    /*
     * Implements MediaStream#getRemoteDataAddress().
     */
    public InetSocketAddress getRemoteDataAddress()
    {
        return
            (InetSocketAddress)
                rtpConnector.getDataSocket().getRemoteSocketAddress();
    }

    /*
     * Implements MediaStream#getRemoteSourceID().
     */
    public String getRemoteSourceID()
    {
        // TODO Auto-generated method stub
        return null;
    }

    private RTPManager getRTPManager()
    {
        if (rtpManager == null)
        {
            rtpManager = RTPManager.newInstance();

            registerCustomCodecFormats(rtpManager);

            rtpManager.addReceiveStreamListener(this);
            rtpManager.addSendStreamListener(this);
            rtpManager.addSessionListener(this);

            /*
             * It appears that if we don't do this managers don't play. You can
             * try out some other buffer size to see if you can get better
             * smoothness.
             */
            BufferControl bc
                = (BufferControl)
                    rtpManager.getControl(BufferControl.class.getName());
            if (bc != null)
            {
                String buffStr
                    = NeomediaActivator
                        .getConfigurationService()
                            .getString(PROPERTY_NAME_RECEIVE_BUFFER_LENGTH);
                long buff = 100;

                try
                {
                    if ((buffStr != null) && (buffStr.length() > 0))
                        buff = Long.parseLong(buffStr);
                }
                catch (NumberFormatException nfe)
                {
                    logger
                        .warn(
                            buffStr
                                + " is not a valid receive buffer/long value",
                            nfe);
                }

                buff = bc.setBufferLength(buff);
                logger.trace("set receiver buffer len to " + buff);
                bc.setEnabledThreshold(true);
                bc.setMinimumThreshold(100);
            }

            rtpManager.initialize(rtpConnector);

            createSendStreams();
        }
        return rtpManager;
    }

    protected void registerCustomCodecFormats(RTPManager rtpManager)
    {
    }

    /*
     * Implements MediaStream#setDevice(MediaDevice).
     */
    public void setDevice(MediaDevice device)
    {
        this.device = (CaptureMediaDevice) device;
    }

    /*
     * Implements MediaStream#setFormat(MediaFormat).
     */
    public void setFormat(MediaFormat format)
    {
        getDevice().setFormat(format);
    }

    public void setTarget(MediaStreamTarget target)
    {
        rtpConnector.removeTargets();

        if (target != null)
        {
            InetSocketAddress dataAddr = target.getDataAddress();
            InetSocketAddress controlAddr = target.getControlAddress();

            try
            {
                rtpConnector
                    .addTarget(
                        new SessionAddress(
                                dataAddr.getAddress(),
                                dataAddr.getPort(),
                                controlAddr.getAddress(),
                                controlAddr.getPort()));
            }
            catch (IOException ioe)
            {
                // TODO
                logger.error("Failed to add target " + target, ioe);
            }
        }
    }

    /*
     * Implements MediaStream#start().
     */
    public void start()
    {
        start(MediaDirection.SENDRECV);
    }

    public void start(MediaDirection direction)
    {
        if (direction == null)
            throw new IllegalArgumentException("direction");

        if ((MediaDirection.SENDRECV.equals(direction)
                    || MediaDirection.SENDONLY.equals(direction))
                && (!MediaDirection.SENDRECV.equals(startedDirection)
                        && !MediaDirection.SENDONLY.equals(startedDirection)))
        {
            RTPManager rtpManager = getRTPManager();
            @SuppressWarnings("unchecked")
            Iterable<SendStream> sendStreams = rtpManager.getSendStreams();

            if (sendStreams != null)
                for (SendStream sendStream : sendStreams)
                    try
                    {
                        // TODO Are we sure we want to connect here?
                        sendStream.getDataSource().connect();
                        sendStream.start();
                    }
                    catch (IOException ioe)
                    {
                        logger
                            .warn("Failed to start stream " + sendStream, ioe);
                    }

            getDevice().start(MediaDirection.SENDONLY);

            if (MediaDirection.RECVONLY.equals(startedDirection))
                startedDirection = MediaDirection.SENDRECV;
            else if (startedDirection == null)
                startedDirection = MediaDirection.SENDONLY;
        }

        if ((MediaDirection.SENDRECV.equals(direction)
                    || MediaDirection.RECVONLY.equals(direction))
                && (!MediaDirection.SENDRECV.equals(startedDirection)
                        && !MediaDirection.RECVONLY.equals(startedDirection)))
        {
            RTPManager rtpManager = getRTPManager();
            Iterable<ReceiveStream> receiveStreams;

            try
            {
                receiveStreams = rtpManager.getReceiveStreams();
            }
            catch (Exception e)
            {
                /*
                 * it appears that in early call states, when there are no
                 * streams this method could throw a null pointer exception.
                 * Make sure we handle it gracefully
                 */
                logger.trace("Failed to retrieve receive streams", e);
                receiveStreams = null;
            }

            if (receiveStreams != null)
                for (ReceiveStream receiveStream : receiveStreams)
                    try
                    {
                        DataSource receiveStreamDataSource
                            = receiveStream.getDataSource();

                        /*
                         * For an unknown reason, the stream DataSource can be
                         * null at the end of the Call after re-INVITEs have
                         * been handled.
                         */
                        if (receiveStreamDataSource != null)
                            receiveStreamDataSource.start();
                    }
                    catch (IOException ioe)
                    {
                        logger
                            .warn(
                                "Failed to start stream " + receiveStream,
                                ioe);
                    }

            getDevice().start(MediaDirection.RECVONLY);

            if (MediaDirection.SENDONLY.equals(startedDirection))
                startedDirection = MediaDirection.SENDONLY;
            else if (startedDirection == null)
                startedDirection = MediaDirection.RECVONLY;
        }
    }

    /*
     * Implements MediaStream#stop().
     */
    public void stop()
    {
        stop(MediaDirection.SENDRECV);
    }

    public void stop(MediaDirection direction)
    {
        if (direction == null)
            throw new IllegalArgumentException("direction");

        if (rtpManager == null)
            return;

        if ((MediaDirection.SENDRECV.equals(direction)
                    || MediaDirection.SENDONLY.equals(direction))
                && (MediaDirection.SENDRECV.equals(startedDirection)
                        || MediaDirection.SENDONLY.equals(startedDirection)))
        {
            @SuppressWarnings("unchecked")
            Iterable<SendStream> sendStreams = rtpManager.getSendStreams();

            if (sendStreams != null)
                for (SendStream sendStream : sendStreams)
                    try
                    {
                        sendStream.getDataSource().stop();
                        sendStream.stop();

                        try
                        {
                            sendStream.close();
                        }
                        catch (NullPointerException npe)
                        {
                            /*
                             * Sometimes com.sun.media.rtp.RTCPTransmitter#bye()
                             * may throw NullPointerException but it does not
                             * seem to be guaranteed because it does not happen
                             * while debugging and stopping at a breakpoint on
                             * SendStream#close(). One of the cases in which it
                             * appears upon call hang-up is if we do not close
                             * the "old" SendStreams upon reinvite(s). Though we
                             * are now closing such SendStreams, ignore the
                             * exception here just in case because we already
                             * ignore IOExceptions.
                             */
                            logger
                                .error(
                                    "Failed to close stream " + sendStream,
                                    npe);
                        }
                    }
                    catch (IOException ioe)
                    {
                        logger.warn("Failed to stop stream " + sendStream, ioe);
                    }

            getDevice().stop(MediaDirection.SENDONLY);

            if (MediaDirection.SENDRECV.equals(startedDirection))
                startedDirection = MediaDirection.RECVONLY;
            else if (MediaDirection.SENDONLY.equals(startedDirection))
                startedDirection = null;
        }

        if ((MediaDirection.SENDRECV.equals(direction)
                || MediaDirection.RECVONLY.equals(direction))
            && (MediaDirection.SENDRECV.equals(startedDirection)
                    || MediaDirection.RECVONLY.equals(startedDirection)))
        {
            Iterable<ReceiveStream> receiveStreams;

            try
            {
                receiveStreams = rtpManager.getReceiveStreams();
            }
            catch (Exception e)
            {
                /*
                 * it appears that in early call states, when there are no
                 * streams this method could throw a null pointer exception.
                 * Make sure we handle it gracefully
                 */
                logger.trace("Failed to retrieve receive streams", e);
                receiveStreams = null;
            }

            if (receiveStreams != null)
                for (ReceiveStream receiveStream : receiveStreams)
                    try
                    {
                        DataSource receiveStreamDataSource
                            = receiveStream.getDataSource();

                        /*
                         * For an unknown reason, the stream DataSource can be
                         * null at the end of the Call after re-INVITEs have
                         * been handled.
                         */
                        if (receiveStreamDataSource != null)
                            receiveStreamDataSource.stop();
                    }
                    catch (IOException ioe)
                    {
                        logger
                            .warn(
                                "Failed to stop stream " + receiveStream,
                                ioe);
                    }

            getDevice().stop(MediaDirection.RECVONLY);

            if (MediaDirection.SENDRECV.equals(startedDirection))
                startedDirection = MediaDirection.SENDONLY;
            else if (MediaDirection.RECVONLY.equals(startedDirection))
                startedDirection = null;
        }
    }

    /*
     * Implements ReceiveStreamListener#update(ReceiveStreamEvent).
     */
    public void update(ReceiveStreamEvent event)
    {
        if (event instanceof NewReceiveStreamEvent)
        {
            ReceiveStream receiveStream = event.getReceiveStream();

            if (receiveStream != null)
            {
                DataSource receiveStreamDataSource
                    = receiveStream.getDataSource();

                if (receiveStreamDataSource != null)
                {
                    Player player = null;
                    Throwable exception = null;

                    try
                    {
                        player = Manager.createPlayer(receiveStreamDataSource);
                    }
                    catch (IOException ioe)
                    {
                        exception = ioe;
                    }
                    catch (NoPlayerException npe)
                    {
                        exception = npe;
                    }

                    if (exception != null)
                        logger
                            .error(
                                "Failed to create player for new receive stream "
                                    + receiveStream,
                                exception);
                    else
                    {
                        player.addControllerListener(this);
                        player.realize();
                    }
                }
            }
        }
    }

    /*
     * Implements SendStreamListener#update(SendStreamEvent).
     */
    public void update(SendStreamEvent event)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Implements SessionListener#update(SessionEvent).
     */
    public void update(SessionEvent event)
    {
        // TODO Auto-generated method stub
    }
}
