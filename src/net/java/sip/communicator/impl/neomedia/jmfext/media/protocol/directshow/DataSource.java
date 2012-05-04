/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.directshow;

import java.io.*;
import java.util.*;
import java.awt.Dimension;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.directshow.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>CaptureDevice</tt> and a <tt>DataSource</tt> using
 * DirectShow.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class DataSource
    extends AbstractVideoPushBufferCaptureDevice
{
    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * DirectShow capture device.
     */
    private DSCaptureDevice device = null;

    /**
     * Delegate grabber. Each frame captured by device will be pass through this
     * grabber.
     */
    private DSCaptureDevice.GrabberDelegate grabber = null;


    /**
     * DirectShow manager.
     */
    private DSManager manager = null;

    /**
     * The map of DirectShow pixel formats to FFmpeg
     * pixel formats which allows converting between the two.
     */
    private static final long[] DS_TO_FFMPEG_PIX_FMT
        = new long[]
                {
                    DSFormat.RGB24,
                    FFmpeg.PIX_FMT_RGB24,
                    DSFormat.RGB32,
                    FFmpeg.PIX_FMT_RGB32,
                    DSFormat.ARGB32,
                    FFmpeg.PIX_FMT_ARGB,
                    DSFormat.YUY2,
                    FFmpeg.PIX_FMT_YUYV422,
                    DSFormat.UYVY,
                    FFmpeg.PIX_FMT_UYVY422,
                    DSFormat.Y411,
                    FFmpeg.PIX_FMT_UYYVYY411,
                    DSFormat.Y41P,
                    FFmpeg.PIX_FMT_UYYVYY411,
                    DSFormat.NV12,
                    FFmpeg.PIX_FMT_NV12,
                    DSFormat.I420,
                    FFmpeg.PIX_FMT_YUV420P,
                };

    /**
     * Last known native DirectShow format.
     */
    private DSFormat nativeFormat = null;

    /**
     * Constructor.
     */
    public DataSource()
    {
        manager = DSManager.getInstance();
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance from a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);
        manager = DSManager.getInstance();
    }

    /**
     * Sets the <tt>MediaLocator</tt> which specifies the media source of this
     * <tt>DataSource</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> which specifies the media source
     * of this <tt>DataSource</tt>
     * @see DataSource#setLocator(MediaLocator)
     */
    @Override
    public void setLocator(MediaLocator locator)
    {
        DSCaptureDevice device = null;
        logger.info("set locator to " + locator);

        if(getLocator() == null)
            super.setLocator(locator);
        locator = getLocator();
        logger.info("getLocator() returns " + locator);

        if((locator != null) &&
                DirectShowSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                        locator.getProtocol()))
        {
            DSCaptureDevice[] devices = manager.getCaptureDevices();

            logger.info("Search directshow device...");

            /* find device */
            for(int i = 0 ; i < devices.length ; i++)
            {
                if(devices[i].getName().equals(locator.getRemainder()))
                {
                    device = devices[i];
                    logger.info("Set directshow device: " + device);
                    break;
                }
            }

            if(device == null)
            {
                logger.info("No devices matches locator's remainder: " +
                        locator.getRemainder());
            }
        }
        else
        {
            logger.info(
                    "MediaLocator either null or does not have right protocol");
            device = null;
        }
        setDevice(device);
    }

    /**
     * Sets the <tt>DSCaptureDevice</tt> which represents the media source of
     * this <tt>DataSource</tt>.
     *
     * @param device the <tt>DSCaptureDevice</tt> which represents the media
     * source of this <tt>DataSource</tt>
     */
    private void setDevice(DSCaptureDevice device)
    {
        if(this.device != device)
        {
            this.device = device;
        }
    }

    /**
     * Creates a new <tt>FrameRateControl</tt> instance which is to allow the
     * getting and setting of the frame rate of this
     * <tt>AbstractVideoPushBufferCaptureDevice</tt>.
     *
     * @return a new <tt>FrameRateControl</tt> instance which is to allow the
     * getting and setting of the frame rate of this
     * <tt>AbstractVideoPushBufferCaptureDevice</tt>
     * @see AbstractPushBufferCaptureDevice#createFrameRateControl()
     */
    @Override
    protected FrameRateControl createFrameRateControl()
    {
        return
            new FrameRateControlAdapter()
            {
                /**
                 * The output frame rate of this
                 * <tt>AbstractVideoPullBufferCaptureDevice</tt>.
                 */
                private float frameRate = -1;

                @Override
                public float getFrameRate()
                {
                    return frameRate;
                }

                @Override
                public float setFrameRate(float frameRate)
                {
                    this.frameRate = frameRate;
                    return this.frameRate;
                }
            };
    }

    /**
     * Create a new <tt>PushBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PushBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PushBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPushBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPushBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        DirectShowStream stream = new DirectShowStream(this, formatControl);

        if (logger.isTraceEnabled())
        {
            DSFormat deviceFmts[] = device.getSupportedFormats();

            for (DSFormat deviceFmt : deviceFmts)
            {
                logger.trace(
                        "width= " + deviceFmt.getWidth()
                            + ", height= " + deviceFmt.getHeight()
                            + ", pixelFormat= " + deviceFmt.getPixelFormat());
            }
        }

        grabber = stream.grabber;

        return stream;
    }

    /**
     * Gets the <tt>Format</tt>s which are to be reported by a
     * <tt>FormatControl</tt> as supported formats for a
     * <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * for which the specified <tt>FormatControl</tt> is to report the list of
     * supported <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by a
     * <tt>FormatControl</tt> as the supported formats for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in the
     * list of streams of this <tt>PushBufferDataSource</tt>
     * @see AbstractPushBufferCaptureDevice#getSupportedFormats(int)
     */
    @Override
    protected Format[] getSupportedFormats(int streamIndex)
    {
        if(device == null)
            return new Format[0];

        DSFormat[] deviceFmts = device.getSupportedFormats();
        List<Format> fmts = new ArrayList<Format>(deviceFmts.length);

        for (DSFormat deviceFmt : deviceFmts)
        {
            Dimension size
                = new Dimension(deviceFmt.getWidth(), deviceFmt.getHeight());
            long devicePixFmt = deviceFmt.getPixelFormat();
            int pixFmt = (int) getFFmpegPixFmt(devicePixFmt);

            fmts.add(
                    new AVFrameFormat(
                            size,
                            Format.NOT_SPECIFIED,
                            pixFmt, (int) devicePixFmt));
        }
        return fmts.toArray(new Format[fmts.size()]);
    }

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PushBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>PushBufferStream</tt> does not
     * exist at the time of the attempt to set its <tt>Format</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * in the list of streams of this <tt>PushBufferStream</tt> or <tt>null</tt>
     * if the attempt to set the <tt>Format</tt> did not success and any
     * last-known <tt>Format</tt> is to be left in effect
     * @see AbstractPushBufferCaptureDevice#setFormat(int, Format, Format)
     */
    @Override
    protected Format setFormat(
            int streamIndex,
            Format oldValue, Format newValue)
    {
        if(newValue instanceof VideoFormat)
        {
            if(newValue instanceof AVFrameFormat)
            {
                AVFrameFormat newAVFrameFormatValue
                    = (AVFrameFormat) newValue;
                long pixelFormat = newAVFrameFormatValue.getDevicePixFmt();

                if(pixelFormat != -1)
                {
                    Dimension size = newAVFrameFormatValue.getSize();

                    /*
                     * We will set the native format in doStart() because a
                     * connect-disconnect-connect sequence of the native
                     * capture device may reorder its formats in a different
                     * way. Consequently, in the absence of further calls to
                     * setFormat() by JMF, a crash may occur later (typically,
                     * during scaling) because of a wrong format.
                     */
                    nativeFormat
                        = new DSFormat(
                                size.width, size.height,
                                pixelFormat);
                }
            }

            // This DataSource supports setFormat.
            return newValue;
        }
        else
            return super.setFormat(streamIndex, oldValue, newValue);
    }

    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doConnect()
     */
    @Override
    protected void doConnect() throws IOException
    {
        if (logger.isInfoEnabled())
            logger.info("doConnect");

        if(manager == null)
            manager = DSManager.getInstance();

        setLocator(getLocator());

        super.doConnect();
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @see AbstractPushBufferCaptureDevice#doDisconnect()
     */
    @Override
    protected void doDisconnect()
    {
        if (logger.isInfoEnabled())
            logger.info("doDisconnect");

        super.doDisconnect();

        if(manager != null)
        {
            device.setDelegate(null);
            device = null;

            DSManager.dispose();
            manager = null;
        }
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doStart()
     */
    @Override
    protected void doStart() throws IOException
    {
        if (logger.isInfoEnabled())
            logger.info("start");

        /* open and start capture */
        device.open();
        if(nativeFormat != null)
            device.setFormat(nativeFormat);

        device.setDelegate(grabber);

        super.doStart();
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doStop()
     */
    @Override
    protected void doStop() throws IOException
    {
        if (logger.isInfoEnabled())
            logger.info("stop");

        /* close capture */
        super.doStop();
        device.close();
    }

    /**
     * Gets the DirectShow pixel format matching a specific FFmpeg pixel
     * format.
     *
     * @param dsPixFmt the DirectShow pixel format to get the matching
     * FFmpeg pixel format of
     * @return the FFmpeg pixel format matching the specified DirectShow pixel
     */
    public static long getFFmpegPixFmt(long dsPixFmt)
    {
        for (int i = 0; i < DS_TO_FFMPEG_PIX_FMT.length; i += 2)
            if (DS_TO_FFMPEG_PIX_FMT[i] == dsPixFmt)
                return DS_TO_FFMPEG_PIX_FMT[i + 1];
        return FFmpeg.PIX_FMT_NONE;
    }

    /**
     * Gets the FFmpeg pixel format matching a specific DirectShow
     * Specification pixel format.
     *
     * @param ffmpegPixFmt FFmpeg format
     * @return the DirectShow pixel format matching the specified FFmpeg format
     */
    public static long getDSPixFmt(int ffmpegPixFmt)
    {
        for (int i = 0; i < DS_TO_FFMPEG_PIX_FMT.length; i += 2)
            if (DS_TO_FFMPEG_PIX_FMT[i + 1] == ffmpegPixFmt)
                return DS_TO_FFMPEG_PIX_FMT[i];
        return -1;
    }
}
