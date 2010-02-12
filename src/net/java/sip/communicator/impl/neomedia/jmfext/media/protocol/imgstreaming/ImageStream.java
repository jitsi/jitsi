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
    extends AbstractPullBufferStream
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
        //System.out.println(System.currentTimeMillis());
        byte data[] = (byte[])buffer.getData();
        int dataLength = (data != null) ? data.length : 0;
        long begin = System.currentTimeMillis();
        /* maximum time allowed for a capture to respect frame rate */
        long maxTime = 1000 / 10; 
        int wait = 0;

        if((data != null) || (dataLength != 0))
        {
            byte buf[] = readScreen(data);

            if(buf != data)
            {
                /* readScreen returns us a different buffer than JMF ones,
                 * it means that JMF's initial buffer was too short.
                 */
                //System.out.println("use our own buffer");
                buffer.setData(buf);
            }

            buffer.setOffset(0);
            buffer.setLength(buf.length);
            buffer.setFormat(getFormat());
            buffer.setHeader(null);
            buffer.setTimeStamp(System.nanoTime());
            buffer.setSequenceNumber(seqNo);
            buffer.setFlags(Buffer.FLAG_SYSTEM_TIME | Buffer.FLAG_LIVE_DATA);
            seqNo++;
        }
        
        wait = (int)(maxTime - (System.currentTimeMillis() - begin));

        try
        {
            /* sleep to respect as much as possible the 
             * frame rate
             */
            if(wait > 0)
            {
                Thread.sleep(wait);
            }
            else
            {
                /* yield a little bit to not use all the 
                 * CPU
                 */
                Thread.yield();
            }
        }
        catch(Exception e)
        {
        }
    }

    /**
     * Start desktop capture stream.
     *
     * @see AbstractPullBufferStream#start()
     */
    @Override
    public void start()
    {
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

        started = true;
    }

    /**
     * Stop desktop capture stream.
     *
     * @see AbstractPullBufferStream#stop()
     */
    @Override
    public void stop()
    {
        logger.info("Stop stream");
        started = false;
    }

    /**
     * Read screen.
     * 
     * @param output output buffer for screen bytes
     * @return raw bytes, it could be equal to output or not. Take care in the caller 
     * to check if output is the returned value.
     */
    public byte[] readScreen(byte output[])
    {
        VideoFormat format = (VideoFormat) getFormat();
        Dimension formatSize = format.getSize();
        int width = (int) formatSize.getWidth();
        int height = (int) formatSize.getHeight();
        BufferedImage scaledScreen = null;
        BufferedImage screen = null;
        byte data[] = null;
        int size = width * height * 4;

        /* check if output buffer can hold all the screen
         * if not allocate our own buffer
         */
        if(output.length < size)
        {
            output = null;
            output = new byte[size];
        }

        /* get desktop screen via native grabber if available */
        if(desktopInteract.captureScreen(output))
        {
            return output;
        }

        System.out.println("failed to grab with native! " + output.length);

        /* OK native grabber failed or is not available,
         * try with AWT Robot and convert it to the right format
         *
         * Note that it is very memory consuming since memory are allocated
         * to capture screen (via Robot) and then for converting to raw bytes
         *
         * Normally not of our supported platform (Windows (x86, x64), 
         * Linux (x86, x86-64), Mac OS X (i386, x86-64, ppc) and 
         * FreeBSD (x86, x86-64) should go here.
         */
        screen = desktopInteract.captureScreen();

        if(screen != null)
        {
            /* convert to ARGB BufferedImage */
            scaledScreen 
                = ImageStreamingUtils
                    .getScaledImage(
                        screen,
                        width,
                        height,
                        BufferedImage.TYPE_INT_ARGB);

            /* get raw bytes */
            data = ImageStreamingUtils.getImageBytes(scaledScreen, output);
        }

        screen = null;
        scaledScreen = null;
        return data;
    }
}
