/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.quicktime;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List; // disambiguation

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.impl.neomedia.quicktime.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>PushBufferDataSource</tt> and <tt>CaptureDevice</tt> using
 * QuickTime/QTKit.
 *
 * @author Lyubomir Marinov
 */
public class DataSource
    extends AbstractPushBufferCaptureDevice
{

    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * The default width of <tt>DataSource</tt> when the associated
     * <tt>QTCaptureDevice</tt> does not report the actual width.
     */
    static final int DEFAULT_WIDTH = 640;

    /**
     * The default height of <tt>DataSource</tt> when the associated
     * <tt>QTCaptureDevice</tt> does not report the actual height.
     */
    static final int DEFAULT_HEIGHT = 480;

    /**
     * The <tt>QTCaptureSession</tt> which captures from {@link #device} and
     * pushes media data to the <tt>PushBufferStream</tt>s of this
     * <tt>PushBufferDataSource</tt>.
     */
    private QTCaptureSession captureSession;

    /**
     * The <tt>QTCaptureDevice</tt> which represents the media source of this
     * <tt>DataSource</tt>.
     */
    private QTCaptureDevice device;

    /**
     * The list of <tt>Format</tt>s to be reported by <tt>DataSource</tt>
     * instances as supported formats.
     */
    private static Format[] supportedFormats;

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
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
    }

    /**
     * Creates a new <tt>PushBufferStream</tt> which is to be at a specific
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
        QuickTimeStream stream = new QuickTimeStream(formatControl);

        if (captureSession != null)
            try
            {
                captureSession.addOutput(stream.captureOutput);
            }
            catch (NSErrorException nseex)
            {
                logger.error("Failed to addOutput to QTCaptureSession", nseex);
                throw new UndeclaredThrowableException(nseex);
            }
        return stream;
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
    protected void doConnect()
        throws IOException
    {
        super.doConnect();

        boolean deviceIsOpened;

        try
        {
            deviceIsOpened = device.open();
        }
        catch (NSErrorException nseex)
        {
            IOException ioex = new IOException();

            ioex.initCause(nseex);
            throw ioex;
        }
        if (!deviceIsOpened)
            throw new IOException("Failed to open QTCaptureDevice");

        QTCaptureDeviceInput deviceInput
            = QTCaptureDeviceInput.deviceInputWithDevice(device);

        captureSession = new QTCaptureSession();
        try
        {
            captureSession.addInput(deviceInput);
        }
        catch (NSErrorException nseex)
        {
            IOException ioex = new IOException();

            ioex.initCause(nseex);
            throw ioex;
        }

        /*
         * Add the QTCaptureOutputs represented by the QuickTimeStreams (if any)
         * to the QTCaptureSession.
         */
        synchronized (getStreamSyncRoot())
        {
            Object[] streams = streams();

            if (streams != null)
                for (Object stream : streams)
                    if (stream != null)
                        try
                        {
                            captureSession
                                .addOutput(
                                    ((QuickTimeStream) stream).captureOutput);
                        }
                        catch (NSErrorException nseex)
                        {
                            logger
                                .error(
                                    "Failed to addOutput to QTCaptureSession",
                                    nseex);

                            IOException ioex = new IOException();

                            ioex.initCause(nseex);
                            throw ioex;
                        }
        }
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
        super.doDisconnect();

        if (captureSession != null)
        {
            captureSession.close();
            captureSession = null;
        }
        device.close();
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doStart()
     */
    @Override
    protected void doStart()
        throws IOException
    {
        captureSession.startRunning();

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
    protected void doStop()
        throws IOException
    {
        super.doStop();

        captureSession.stopRunning();
    }

    /**
     * Gets the <tt>Format</tt> to be reported by the <tt>FormatControl</tt> of
     * a <tt>PushBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PushBufferDataSource</tt>. The
     * <tt>PushBufferStream</tt> may not exist at the time of requesting its
     * <tt>Format</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be retrieved
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt> in
     * the list of streams of this <tt>PushBufferDataSource</tt>
     * @see AbstractPushBufferCaptureDevice#getFormat(int, Format)
     */
    @Override
    protected Format getFormat(int streamIndex, Format oldValue)
    {
        Format format = super.getFormat(streamIndex, oldValue);

        if (format instanceof VideoFormat)
        {
            VideoFormat videoFormat = (VideoFormat) format;
            Dimension size = videoFormat.getSize();

            if (size == null)
            {
                QTFormatDescription[] formatDescriptions
                    = device.formatDescriptions();

                if ((formatDescriptions != null)
                        && (streamIndex < formatDescriptions.length))
                {
                    size
                        = formatDescriptions[streamIndex]
                            .sizeForKey(
                                QTFormatDescription
                                    .VideoEncodedPixelsSizeAttribute);
                    if (size != null)
                    {
                        /*
                         * If the actual capture device reports too small a
                         * resolution, we end up using it while it can actually
                         * be set to one with better quality. We've decided that
                         * DEFAULT_WIDTH and DEFAULT_HEIGHT make sense as the
                         * minimum resolution to request from the capture
                         * device.
                         */
                        if ((size.width < DEFAULT_WIDTH)
                                && (size.height < DEFAULT_HEIGHT))
                        {
                            double ratio = size.height / (double) size.width;

                            size.width = DEFAULT_WIDTH;
                            size.height = (int) (size.width * ratio);
                        }

                        format
                            = format
                                .intersects(
                                    new VideoFormat(
                                            format.getEncoding(),
                                            size,
                                            videoFormat.getMaxDataLength(),
                                            format.getDataType(),
                                            videoFormat.getFrameRate()));
                    }
                }
            }
        }
        return format;
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
        return getSupportedFormats(super.getSupportedFormats(streamIndex));
    }

    /**
     * Gets a list of <tt>Format</tt>s which are more specific than given
     * <tt>Format</tt>s with respect to video size. The implementation tries to
     * come up with sane video sizes (for example, by looking for codecs which
     * accept the encodings of the specified generic <tt>Format</tt>s and using
     * their sizes if any).
     *
     * @param genericFormats the <tt>Format</tt>s from which more specific are
     * to be derived
     * @return a list of <tt>Format</tt>s which are more specific than the given
     * <tt>Format</tt>s with respect to video size
     */
    private static synchronized Format[] getSupportedFormats(
            Format[] genericFormats)
    {
        if ((supportedFormats != null) && (supportedFormats.length > 0))
            return supportedFormats.clone();

        List<Format> specificFormats = new LinkedList<Format>();

        for (Format genericFormat : genericFormats)
        {
            VideoFormat genericVideoFormat = (VideoFormat) genericFormat;

            if (genericVideoFormat.getSize() == null)
            {
//                specificFormats
//                    .add(
//                        genericFormat
//                            .intersects(
//                                new VideoFormat(
//                                        null,
//                                        new Dimension(
//                                                DEFAULT_WIDTH,
//                                                DEFAULT_HEIGHT),
//                                        Format.NOT_SPECIFIED,
//                                        null,
//                                        Format.NOT_SPECIFIED)));

                @SuppressWarnings("unchecked")
                Vector<String> codecs
                    = PlugInManager
                        .getPlugInList(
                            new VideoFormat(genericVideoFormat.getEncoding()),
                            null,
                            PlugInManager.CODEC);

                for (String codec : codecs)
                {
                    Format[] supportedInputFormats
                        = PlugInManager
                            .getSupportedInputFormats(
                                codec,
                                PlugInManager.CODEC);

                    for (Format supportedInputFormat : supportedInputFormats)
                        if (supportedInputFormat instanceof VideoFormat)
                        {
                            Dimension size
                                = ((VideoFormat) supportedInputFormat)
                                    .getSize();

                            if (size != null)
                                specificFormats
                                    .add(
                                        genericFormat
                                            .intersects(
                                                new VideoFormat(
                                                        null,
                                                        size,
                                                        Format.NOT_SPECIFIED,
                                                        null,
                                                        Format.NOT_SPECIFIED)));
                        }
                }
            }

            specificFormats.add(genericFormat);
        }
        supportedFormats
            = specificFormats.toArray(new Format[specificFormats.size()]);
        return supportedFormats.clone();
    }

    /**
     * Sets the <tt>QTCaptureDevice</tt> which represents the media source of
     * this <tt>DataSource</tt>.
     *
     * @param device the <tt>QTCaptureDevice</tt> which represents the media
     * source of this <tt>DataSource</tt>
     */
    private void setDevice(QTCaptureDevice device)
    {
        if (this.device != device)
            this.device = device;
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
        /*
         * A resolution that is too small will yield bad image quality. We've
         * decided that DEFAULT_WIDTH and DEFAULT_HEIGHT make sense as the
         * minimum resolution to request from the capture device.
         */
        if (newValue instanceof VideoFormat)
        {
            VideoFormat newVideoFormatValue = (VideoFormat) newValue;
            Dimension newSize = newVideoFormatValue.getSize();

            if ((newSize != null)
                    && (newSize.width < DEFAULT_WIDTH)
                    && (newSize.height < DEFAULT_HEIGHT))
            {
                String encoding = newVideoFormatValue.getEncoding();
                Class<?> dataType = newVideoFormatValue.getDataType();
                float frameRate = newVideoFormatValue.getFrameRate();

                newSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
                newValue
                    = ((Format) newVideoFormatValue.clone())
                        .relax()
                            .intersects(
                                new VideoFormat(
                                        encoding,
                                        newSize,
                                        Format.NOT_SPECIFIED,
                                        dataType,
                                        frameRate));
            }
            return newValue;
        }
        else
            return super.setFormat(streamIndex, oldValue, newValue);
    }

    /**
     * Sets the <tt>MediaLocator</tt> which specifies the media source of this
     * <tt>DataSource</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> which specifies the media source
     * of this <tt>DataSource</tt>
     * @see javax.media.protocol.DataSource#setLocator(MediaLocator)
     */
    @Override
    public void setLocator(MediaLocator locator)
    {
        super.setLocator(locator);

        locator = getLocator();

        QTCaptureDevice device;

        if ((locator != null)
                && QuickTimeAuto.LOCATOR_PROTOCOL
                        .equalsIgnoreCase(locator.getProtocol()))
        {
            String deviceUID = locator.getRemainder();

            device = QTCaptureDevice.deviceWithUniqueID(deviceUID);
        }
        else
            device = null;
        setDevice(device);
    }
}
