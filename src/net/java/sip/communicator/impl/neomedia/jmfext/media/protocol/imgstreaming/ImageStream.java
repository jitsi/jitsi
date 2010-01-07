/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import java.io.*;

import java.awt.*;
import java.awt.image.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.neomedia.imgstreaming.*;

/**
 * The stream used by JMF for our image streaming.
 * 
 * This class launch a thread to handle desktop capture
 * interactions.
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class ImageStream implements PushBufferStream, Runnable
{
    /**
     * The <tt>Logger</tt>
     */
    private static final Logger logger = Logger.getLogger(ImageStream.class);

    /**
     * Content descriptor of this stream which is always RAW.
     */
    private final static ContentDescriptor contentDescriptor =
        new ContentDescriptor(ContentDescriptor.RAW);

    /**
     * Controls associated with the stream.
     */
    private final Control[] controls = new Control[0];

    /**
     * Current format used.
     */
    private Format currentFormat = null;

    /**
     * Callback handler when data from stream is available.
     */
    private BufferTransferHandler transferHandler = null;

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
    private Buffer buf = null;

    /**
     * Destkop interaction (screen capture, key press, ...).
     */
    private DesktopInteract desktopInteract = null;

    /**
     * Constructor.
     */
    public ImageStream()
    {
    }

    /**
     * Constructor.
     *
     * @param locator <tt>MediaLocator</tt> to use
     */
    public ImageStream(MediaLocator locator)
    {
    }

    /**
     * Set format to use.
     *
     * @param format new format to use
     */
    public void setFormat(Format format)
    {
        currentFormat = format;
    }

    /**
     * Returns the supported format by this stream.
     *
     * @return supported formats
     */
    public Format getFormat()
    {
        return currentFormat;
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @param buffer the <tt>Buffer</tt> to read captured media into
     * @throws IOException if an error occurs while reading.
     */
    public void read(Buffer buffer) throws IOException
    {
        try
        {
            buffer.setData(buf.getData());
            buffer.setOffset(0);
            buffer.setLength(buf.getLength());
            buffer.setFormat(buf.getFormat());
            buffer.setHeader(null);
            buffer.setTimeStamp(buf.getTimeStamp());
            buffer.setSequenceNumber(buf.getSequenceNumber());
            buffer.setFlags(buf.getFlags());
        }
        catch(Exception e)
        {
        }
    }

    /**
     * Query if the next read will block.
     *
     * @return true if a read will block.
     */
    public boolean willReadBlock()
    {
        return false;
    }

    /**
     * Register an object to service data transfers to this stream.
     *
     * @param transferHandler handler to transfer data to
     */
    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        synchronized(this)
        {
            this.transferHandler = transferHandler;
            notifyAll();
        }
    }

    /**
     * Indicates whether or not the end of media stream.
     *
     * In case of image streaming it is always false.
     *
     * @return false
     */
    public boolean endOfStream()
    {
        return false;
    }

    /**
     * Get content length of the stream.
     *
     * In case of image streaming it is unknown.
     *
     * @return LENGTH_UNKNOWN
     */
    public long getContentLength()
    {
        return LENGTH_UNKNOWN;
    }

    /**
     * We are providing access to raw data.
     *
     * @return RAW content descriptor.
     */
    public ContentDescriptor getContentDescriptor()
    {
        return contentDescriptor;
    }

    /**
     * Gives control information to the caller.
     *
     * @return the collection of object controls.
     */
    public Object[] getControls()
    {
        /*
         * The field controls represents is private so we cannot directly return
         * it. Otherwise, the caller will be able to modify it.
         */
        return controls.clone();
    }

    /**
     * Return required control from the Control[] array
     * if exists.
     *
     * @param controlType the control we are interested in.
     * @return the object that implements the control, or null.
     */
    public Object getControl(String controlType)
    {
        try
        {
            Class<?> cls = Class.forName(controlType);
            Object cs[] = getControls();
            for(int i = 0; i < cs.length; i++)
            {
                if(cls.isInstance(cs[i]))
                {
                    return cs[i];
                }
            }

            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Start desktop capture stream.
     */
    public void start()
    {
        if(captureThread == null || !captureThread.isAlive())
        {
            logger.info("Start stream");
            captureThread = new Thread(this);
            captureThread.start();
            started = true;
        }
    }

    /**
     * Stop desktop capture stream.
     */
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
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final RGBFormat format = (RGBFormat)currentFormat;
        final int width = (int)format.getSize().getWidth();
        final int height = (int)format.getSize().getHeight();
        Buffer buffer = new Buffer();

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
            
            /*
            long t = System.nanoTime();
            */

            /* get desktop screen and resize it */
            screen = desktopInteract.captureScreen();
            scaledScreen = ImageStreamingUtils.getScaledImage(screen, 
                    width, height, BufferedImage.TYPE_INT_ARGB);

            /* get raw bytes */
            data = ImageStreamingUtils.getImageByte(scaledScreen);

            /* notify JMF that new data is available */
            buffer.setData(data);
            buffer.setOffset(0);
            buffer.setLength(data.length);
            buffer.setFormat(currentFormat);
            buffer.setHeader(null);
            buffer.setTimeStamp(System.nanoTime());
            buffer.setSequenceNumber(seqNo);
            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            seqNo++;

            buf = buffer;

            /* pass to JMF handler */
            if(transferHandler != null)
            {
                transferHandler.transferData(this);
                Thread.yield();
            }
            /*
            t = System.nanoTime() - t;
            logger.info("Desktop capture processing time: " + t);
            */

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

        buffer = null;
    }
}

