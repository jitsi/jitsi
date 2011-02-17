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

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.imgstreaming.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The stream used by JMF for our image streaming.
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
     * Desktop interaction (screen capture, key press, ...).
     */
    private DesktopInteract desktopInteract = null;

    /**
     * Native buffer pointer.
     */
    private ByteBuffer data = null;

    /**
     * If stream has been reinitialized.
     */
    private boolean reinit = false;

    /**
     * Index of display that we will capture from.
     */
    private int displayIndex = -1;

    /**
     * X origin.
     */
    private int x = 0;

    /**
     * Y origin.
     */
    private int y = 0;

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
        long begin = System.currentTimeMillis();
        /* maximum time allowed for a capture to respect frame rate */
        long maxTime = 1000 / 10;
        int wait = 0;

        /*
         * Determine the Format in which we're expected to output. We cannot
         * rely on the Format always being specified in the Buffer because it is
         * not its responsibility, the DataSource of this ImageStream knows the
         * output Format.
         */
        Format bufferFormat = buffer.getFormat();

        if (bufferFormat == null)
        {
            bufferFormat = getFormat();
            if (bufferFormat != null)
                buffer.setFormat(bufferFormat);
        }

        if(bufferFormat instanceof AVFrameFormat)
        {
            /* native transfert: we keep data in native memory rather
             * than Java Heap until we reach SwScaler
             */
            Object dataAv = buffer.getData();
            AVFrame bufferFrame = null;
            long bufferFramePtr = 0;

            if (dataAv instanceof AVFrame)
            {
                bufferFrame = (AVFrame)dataAv;
                bufferFramePtr = bufferFrame.getPtr();
            }
            else
            {
                bufferFrame = new FinalizableAVFrame();
                bufferFramePtr = bufferFrame.getPtr();
            }

            AVFrameFormat bufferFrameFormat = (AVFrameFormat) bufferFormat;
            Dimension bufferFrameSize = bufferFrameFormat.getSize();

            if(readScreenNative(bufferFrameSize))
            {
                FFmpeg.avpicture_fill(
                        bufferFramePtr,
                        data.ptr,
                        bufferFrameFormat.getPixFmt(),
                        bufferFrameSize.width, bufferFrameSize.height);
                buffer.setData(bufferFrame);
            }
            else
            {
                /* this can happen when we disconnect a monitor from computer
                 * before or during grabbing
                 */
                throw new IOException("Failed to grab screen");
            }
        }
        else
        {
            byte dataByte[] = (byte[])buffer.getData();
            int dataLength = (dataByte != null) ? dataByte.length : 0;

            if((dataByte != null) || (dataLength != 0))
            {
                Dimension bufferFrameSize =
                    ((VideoFormat)bufferFormat).getSize();
                byte buf[] = readScreen(dataByte, bufferFrameSize);

                if(buf != dataByte)
                {
                    /* readScreen returns us a different buffer than JMF ones,
                     * it means that JMF's initial buffer was too short.
                     */
                    //System.out.println("use our own buffer");
                    buffer.setData(buf);
                }

                buffer.setOffset(0);
                buffer.setLength(buf.length);
            }
        }

        buffer.setHeader(null);
        buffer.setTimeStamp(System.nanoTime());
        buffer.setSequenceNumber(seqNo);
        buffer.setFlags(Buffer.FLAG_SYSTEM_TIME | Buffer.FLAG_LIVE_DATA);
        seqNo++;

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
     * Set display index.
     *
     * @param index display index
     */
    public void setDisplayIndex(int index)
    {
        displayIndex = index;
    }

    /**
     * Set Origin of capture.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setOrigin(int x, int y)
    {
        this.x = x;
        this.y = y;
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
            }
        }

        reinit = true;
    }

    /**
     * Stop desktop capture stream.
     *
     * @see AbstractPullBufferStream#stop()
     */
    @Override
    public void stop()
    {
        if (logger.isInfoEnabled())
            logger.info("Stop stream");
    }

    /**
     * Read screen and store result in native buffer.
     *
     * @param dim dimension of the video
     * @return true if success, false otherwise
     */
    private boolean readScreenNative(Dimension dim)
    {
        int size = dim.width * dim.height * 4;

        /* pad the buffer */
        size += FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;

        /* allocate native array */
        if(data == null || reinit)
        {
            data = new ByteBuffer(size);
            data.setLength(size);
            reinit = false;
        }
        else if(data.capacity < size)
        {
            /* reallocate native array if capacity is not enough */
            data.setFree(true);
            FFmpeg.av_free(data.ptr);
            data = new ByteBuffer(size);
            data.setLength(size);
        }

        /* get desktop screen via native grabber */
        return desktopInteract.captureScreen(displayIndex, x, y, dim.width,
                dim.height, data.ptr, data.getLength());
    }

    /**
     * Read screen.
     *
     * @param output output buffer for screen bytes
     * @param dim dimension of the screen
     * @return raw bytes, it could be equal to output or not. Take care in the
     * caller to check if output is the returned value.
     */
    public byte[] readScreen(byte output[], Dimension dim)
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
        if(desktopInteract.captureScreen(displayIndex, x, y, dim.width,
                    dim.height, output))
        {
            return output;
        }

        System.out.println("failed to grab with native! " + output.length);

        /* OK native grabber failed or is not available,
         * try with AWT Robot and convert it to the right format
         *
         * Note that it is very memory consuming since memory are allocated
         * to capture screen (via Robot) and then for converting to raw bytes
         * Moreover support for multiple display has not yet been investigated
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
