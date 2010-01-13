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
 * @author Lubomir Marinov
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
        QuickTimeStream stream = new QuickTimeStream(formatControl);

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
        return format;
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
            Format oldValue,
            Format newValue)
    {
        return newValue;
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
