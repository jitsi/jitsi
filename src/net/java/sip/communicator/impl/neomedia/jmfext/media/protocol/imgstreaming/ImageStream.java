/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.imgstreaming.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The stream used by JMF for our image streaming.
 * 
 * This class launches a thread to handle desktop capture interactions.
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class ImageStream
    extends AbstractPushBufferStream
    implements Runnable
{
    /**
     * The <tt>Logger</tt>
     */
    private static final Logger logger = Logger.getLogger(ImageStream.class);

    /**
     * Sequence number.
     */
    private long seqNo = 0;

    /**
     * Capture thread reference.
     */
    private Thread captureThread = null;

    /**
     * If stream is started or not.
     */
    private boolean started = false;

    /**
     * Buffer for the last image.
     */
    private final Buffer buf = new Buffer();

    /**
     * Desktop interaction (screen capture, key press, ...).
     */
    private DesktopInteract desktopInteract = null;

    /**
     * Initializes a new <tt>ImageStream</tt> instance which is to have a
     * specific <tt>FormatControl</tt>
     *
     * @param formatControl the <tt>FormatControl</tt> of the new instance which
     * is to specify the format in which it is to provide its media data
     */
    ImageStream(FormatControl formatControl)
    {
        super(formatControl);
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @param buffer the <tt>Buffer</tt> to read captured media into
     * @throws IOException if an error occurs while reading.
     */
    public void read(Buffer buffer)
        throws IOException
    {
        synchronized(buf)
        {
            try
            {
                Object bufData = buf.getData();
                int bufLength = buf.getLength();

                if ((bufData != null) || (bufLength != 0))
                {
                    buffer.setData(bufData);
                    buffer.setOffset(0);
                    buffer.setLength(bufLength);
                    buffer.setFormat(buf.getFormat());
                    buffer.setHeader(null);
                    buffer.setTimeStamp(buf.getTimeStamp());
                    buffer.setSequenceNumber(buf.getSequenceNumber());
                    buffer.setFlags(buf.getFlags());

                    /* clear buf so JMF will not get twice the same image */
                    buf.setData(null);
                    buf.setLength(0);
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * Start desktop capture stream.
     *
     * @see AbstractPushBufferStream#start()
     */
    @Override
    public void start()
    {
        if(captureThread == null || !captureThread.isAlive())
        {
            logger.info("Start stream");
            captureThread = new Thread(this);

            /*
             * Set the started indicator before calling Thread#start() because
             * the Thread may exist upon start if Thread#run() starts executing
             * before setting the started indicator.
             */
            started = true;
            captureThread.start();
        }
    }

    /**
     * Stop desktop capture stream.
     *
     * @see AbstractPushBufferStream#stop()
     */
    @Override
    public void stop()
    {
        logger.info("Stop stream");
        started = false;
        captureThread = null;
    }

    /**
     * Thread entry point.
     */
    public void run()
    {
        VideoFormat format = (VideoFormat) getFormat();
        Dimension formatSize = format.getSize();
        int width = (int) formatSize.getWidth();
        int height = (int) formatSize.getHeight();

        if(desktopInteract == null)
        {
            try
            {
                desktopInteract = new DesktopInteractImpl();
            }
            catch(Exception e)
            {
                logger.warn("Cannot create DesktopInteract object!");
                started = false;
                return;
            }
        }

        while(started)
        {
            byte data[] = null;
            BufferedImage scaledScreen = null;
            BufferedImage screen = null;

            /* get desktop screen and resize it */
            screen = desktopInteract.captureScreen();

            if(screen.getType() == BufferedImage.TYPE_INT_ARGB)
            {
                /* with our native screencapture we 
                 * automatically create BufferedImage in 
                 * ARGB format so no need to rescale/convert
                 * to ARGB
                 */
                scaledScreen = screen;
            }
            else
            {
                /* convert to ARGB BufferedImage */
                scaledScreen 
                    = ImageStreamingUtils
                        .getScaledImage(
                            screen,
                            width,
                            height,
                            BufferedImage.TYPE_INT_ARGB);
            }

            /* get raw bytes */
            data = ImageStreamingUtils.getImageBytes(scaledScreen);

            /* notify JMF that new data is available */
            synchronized (buf)
            {
                buf.setData(data);
                buf.setOffset(0);
                buf.setLength(data.length);
                buf.setFormat(format);
                buf.setHeader(null);
                buf.setTimeStamp(System.nanoTime());
                buf.setSequenceNumber(seqNo++);
                buf.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            }

            /* pass to JMF handler */
            BufferTransferHandler transferHandler = this.transferHandler;

            if(transferHandler != null)
            {
                transferHandler.transferData(this);
                Thread.yield();
            }

            /* cleanup */
            screen = null;
            scaledScreen = null;
            data = null;

            try
            {
                /* 100 ms */
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {
                /* do nothing */
            }
        }
    }
}
