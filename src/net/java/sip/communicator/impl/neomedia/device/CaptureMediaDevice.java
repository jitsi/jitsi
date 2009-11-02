/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>MediaDevice</tt> for the JMF <tt>CaptureDevice</tt>.
 *
 * @author Lubomir Marinov
 */
public class CaptureMediaDevice
    extends AbstractMediaDevice
{

    /**
     * The <tt>Logger</tt> used by <tt>CaptureMediaDevice</tt> and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CaptureMediaDevice.class);

    /**
     * The JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     */
    private CaptureDevice captureDevice;

    /**
     * The <tt>CaptureDeviceInfo</tt> of {@link #captureDevice}.
     */
    private CaptureDeviceInfo captureDeviceInfo;

    /**
     * The indicator which determines whether {@link DataSource#connect()} has
     * been successfully executed on {@link #captureDevice}.
     */
    private boolean captureDeviceIsConnected;

    /**
     * The <tt>MediaType</tt> of this instance and the <tt>CaptureDevice</tt>
     * that it wraps.
     */
    private final MediaType mediaType;

    /**
     * Initializes a new <tt>CaptureMediaDevice</tt> instance which is to
     * provide an implementation of <tt>MediaDevice</tt> for a specific
     * <tt>CaptureDevice</tt> with a specific <tt>MediaType</tt>.
     *
     * @param captureDevice the JMF <tt>CaptureDevice</tt> the new instance is
     * to provide an implementation of <tt>MediaDevice</tt> for
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public CaptureMediaDevice(CaptureDevice captureDevice, MediaType mediaType)
    {
        if (captureDevice == null)
            throw new NullPointerException("captureDevice");
        if (mediaType == null)
            throw new NullPointerException("mediaType");

        this.mediaType = mediaType;

        setCaptureDevice(captureDevice);
    }

    /**
     * Initializes a new <tt>CaptureMediaDevice</tt> instance which is to
     * provide an implementation of <tt>MediaDevice</tt> for a
     * <tt>CaptureDevice</tt> with a specific <tt>CaptureDeviceInfo</tt> and
     * which is of a specific <tt>MediaType</tt>.
     *
     * @param captureDeviceInfo the <tt>CaptureDeviceInfo</tt> of the JMF
     * <tt>CaptureDevice</tt> the new instance is to provide an implementation
     * of <tt>MediaDevice</tt> for
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public CaptureMediaDevice(
        CaptureDeviceInfo captureDeviceInfo,
        MediaType mediaType)
    {
        if (captureDeviceInfo == null)
            throw new NullPointerException("captureDeviceInfo");
        if (mediaType == null)
            throw new NullPointerException("mediaType");

        this.captureDevice = null;
        this.captureDeviceInfo = captureDeviceInfo;
        this.mediaType = mediaType;
    }

    /**
     * Notifies this instance that its <tt>captureDevice</tt> (the JMF
     * <tt>CaptureDevice</tt> this instance wraps and provides an implementation
     * of <tt>MediaDevice</tt> for) property has changed its value from
     * <tt>oldValue</tt> to <tt>newValue</tt>. Allows extenders to override in
     * order to perform additional processing of the new <tt>captureDevice</tt>
     * once it is clear that it is set into this instance.
     *
     * @param oldValue the JMF <tt>CaptureDevice</tt> which was the value of the
     * <tt>captureDevice</tt> property of this instance before <tt>newValue</tt>
     * was set
     * @param newValue the JMF <tt>CaptureDevice</tt> which is the value of the
     * <tt>captureDevice</tt> property of this instance and which replaced
     * <tt>oldValue</tt>
     */
    protected void captureDeviceChanged(
            CaptureDevice oldValue,
            CaptureDevice newValue)
    {
    }

    /**
     * Creates a <tt>DataSource</tt> instance for this <tt>MediaDevice</tt>
     * which gives access to the captured media.
     *
     * @return a <tt>DataSource</tt> instance which gives access to the media
     * captured by this <tt>MediaDevice</tt>
     * @see AbstractMediaDevice#createOutputDataSource()
     */
    DataSource createOutputDataSource()
    {
        return (DataSource) getConnectedCaptureDevice();
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for
     */
    public CaptureDevice getCaptureDevice()
    {
        return getCaptureDevice(true);
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for and, optionally, creates it if
     * it does not exist yet.
     *
     * @param create <tt>true</tt> to create the <tt>CaptureDevice</tt> this
     * instance provides an implementation of <tt>MediaDevice</tt> for it it
     * does not exist yet; <tt>false</tt> to not create it and return
     * <tt>null</tt> if it does not exist yet
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for if it exists or
     * <tt>create</tt> is <tt>true</tt> and its creation succeeds; <tt>null</tt>
     * if it does not exist yet and <tt>create</tt> is <tt>false</tt> or its
     * creation fails
     */
    protected CaptureDevice getCaptureDevice(boolean create)
    {
        if ((captureDevice == null) && create)
        {
            CaptureDevice captureDevice = null;
            Throwable exception = null;

            try
            {
                captureDevice
                    = (CaptureDevice)
                        Manager
                            .createDataSource(captureDeviceInfo.getLocator());
            }
            catch (IOException ioe)
            {
                // TODO
                exception = ioe;
            }
            catch (NoDataSourceException ndse)
            {
                // TODO
                exception = ndse;
            }

            if (exception != null)
                logger
                    .error(
                        "Failed to create CaptureDevice DataSource "
                            + "from CaptureDeviceInfo "
                            + captureDeviceInfo,
                        exception);
            else
                setCaptureDevice(captureDevice);
        }
        return captureDevice;
    }

    /**
     * Gets the <tt>CaptureDeviceInfo</tt> of the JMF <tt>CaptureDevice</tt>
     * represented by this instance.
     *
     * @return the <tt>CaptureDeviceInfo</tt> of the <tt>CaptureDevice</tt>
     * represented by this instance
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        return captureDeviceInfo;
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for in a connected state. If the
     * <tt>CaptureDevice</tt> is not connected to yet, first tries to connect to
     * it. Returns <tt>null</tt> if this instance has failed to create a
     * <tt>CaptureDevice</tt> instance or to connect to it.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for in a connected state;
     * <tt>null</tt> if this instance has failed to create a
     * <tt>CaptureDevice</tt> instance or to connect to it
     */
    private CaptureDevice getConnectedCaptureDevice()
    {
        CaptureDevice captureDevice = getCaptureDevice();

        if ((captureDevice != null) && !captureDeviceIsConnected)
        {
            Throwable exception = null;

            try
            {
                captureDevice.connect();
            }
            catch (IOException ioe)
            {
                // TODO
                exception = ioe;
            }
            catch (NullPointerException npe)
            {
                /*
                 * TODO The old media says it happens when the operating system
                 * does not support the operation.
                 */
                exception = npe;
            }

            if (exception == null)
            {
                captureDeviceIsConnected = true;

                /*
                 * 1. Changing buffer size. The default buffer size (for
                 * javasound) is 125 milliseconds - 1/8 sec. On MacOS this leads
                 * to an exception and no audio capture. A value of 30 for the
                 * buffer fixes the problem and is OK when using some pstn
                 * gateways.
                 * 
                 * 2. Changing to 60. When it is 30 there are some issues with
                 * asterisk and nat (we don't start to send stream and so
                 * asterisk rtp part doesn't notice that we are behind nat)
                 * 
                 * 3. Do not set buffer length on linux as it completely breaks
                 * audio capture.
                 */
                String osName = System.getProperty("os.name");

                if ((osName == null) || !osName.toLowerCase().contains("linux"))
                {
                    Control bufferControl
                        = (Control)
                            ((DataSource) captureDevice)
                                .getControl(
                                    "javax.media.control.BufferControl");

                    if (bufferControl != null)
                        ((BufferControl) bufferControl)
                            .setBufferLength(60); // in milliseconds
                }
            }
            else
                captureDevice = null;
        }
        return captureDevice;
    }

    /**
     * Returns the <tt>MediaDirection</tt> supported by this device.
     *
     * @return {@link MediaDirection#SENDONLY} if this is a read-only device,
     * {@link MediaDirection#RECVONLY} if this is a write-only device or
     * {@link MediaDirection#SENDRECV} if this <tt>MediaDevice</tt> can both
     * capture and render media
     * @see MediaDevice#getDirection()
     */
    public MediaDirection getDirection()
    {
        return MediaDirection.SENDRECV;
    }

    /**
     * Gets the <tt>MediaFormat</tt> in which this <t>MediaDevice</tt> captures
     * media.
     *
     * @return the <tt>MediaFormat</tt> in which this <tt>MediaDevice</tt>
     * captures media
     * @see MediaDevice#getFormat()
     */
    public MediaFormat getFormat()
    {
        MediaType mediaType = getMediaType();

        for (FormatControl formatControl
                : getCaptureDevice().getFormatControls())
        {
            MediaFormat format
                = MediaFormatImpl.createInstance(formatControl.getFormat());

            if ((format != null) && format.getMediaType().equals(mediaType))
                return format;
        }
        return null;
    }

    /**
     * Gets the <tt>MediaType</tt> that this device supports.
     *
     * @return {@link MediaType#AUDIO} if this is an audio device or
     * {@link MediaType#VIDEO} if this is a video device
     * @see MediaDevice#getMediaType()
     */
    public MediaType getMediaType()
    {
        return mediaType;
    }

    /**
     * Gets a list of <tt>MediaFormat</tt>s supported by this
     * <tt>MediaDevice</tt>.
     *
     * @return the list of <tt>MediaFormat</tt>s supported by this device
     * @see MediaDevice#getSupportedFormats()
     */
    public List<MediaFormat> getSupportedFormats()
    {
        EncodingConfiguration encodingConfiguration
            = NeomediaActivator
                .getMediaServiceImpl().getEncodingConfiguration();
        MediaFormat[] supportedEncodings
            = encodingConfiguration.getSupportedEncodings(getMediaType());
        List<MediaFormat> supportedFormats = new ArrayList<MediaFormat>();

        if (supportedEncodings != null)
            for (MediaFormat supportedEncoding : supportedEncodings)
                supportedFormats.add(supportedEncoding);

        return supportedFormats;
    }

    /**
     * Gets the <tt>MediaFormat</tt>s supported by a specific
     * <tt>CaptureDevice</tt>.
     *
     * @param captureDevice the JMF <tt>CaptureDevice</tt> to retrieve the
     * supported <tt>MediaFormat</tt>s of
     * @return the <tt>MediaFormat</tt>s supported by the specified
     * <tt>CaptureDevice</tt>
     */
    private List<MediaFormat> getSupportedFormats(CaptureDevice captureDevice)
    {
        MediaType mediaType = getMediaType();
        Set<Format> supportedFormats = new HashSet<Format>();

        for (FormatControl formatControl : captureDevice.getFormatControls())
        {
            for (Format format : formatControl.getSupportedFormats())
                switch (mediaType)
                {
                case AUDIO:
                    if (format instanceof AudioFormat)
                        supportedFormats.add(format);
                    break;
                case VIDEO:
                    if (format instanceof VideoFormat)
                        supportedFormats.add(format);
                    break;
                }
        }

        List<MediaFormat> supportedMediaFormats
            = new ArrayList<MediaFormat>(supportedFormats.size());

        for (Format format : supportedFormats)
            supportedMediaFormats.add(MediaFormatImpl.createInstance(format));
        return supportedMediaFormats;
    }

    /**
     * Gets the <tt>MediaFormat</tt>s supported by a <tt>CaptureDevice</tt>
     * judging by its <tt>CaptureDeviceInfo</tt>.
     *
     * @param captureDeviceInfo the <tt>CaptureDeviceInfo</tt> to retrieve the
     * supported <tt>MediaFormat</tt>s of
     * @return the <tt>MediaFormat</tt>s supported by the specified
     * <tt>CaptureDeviceInfo</tt>
     */
    private List<MediaFormat> getSupportedFormats(
            CaptureDeviceInfo captureDeviceInfo)
    {
        Format[] supportedFormats = captureDeviceInfo.getFormats();
        MediaType mediaType = getMediaType();
        List<MediaFormat> supportedMediaFormats
            = new ArrayList<MediaFormat>(supportedFormats.length);

        for (Format format : supportedFormats)
        {
            MediaFormat mediaFormat = MediaFormatImpl.createInstance(format);

            if ((mediaFormat != null)
                    && mediaFormat.getMediaType().equals(mediaType))
                supportedMediaFormats.add(mediaFormat);
        }
        return supportedMediaFormats;
    }

    /**
     * Sets the JMF <tt>CaptureDevice</tt> this instance wraps and provides a
     * <tt>MediaDevice</tt> implementation for. Allows extenders to override in
     * order to customize <tt>captureDevice</tt> including to replace it before
     * it is set into this instance.
     *
     * @param captureDevice the JMF <tt>CaptureDevice</tt> this instance is to
     * wrap and provide a <tt>MediaDevice</tt> implementation for
     */
    protected void setCaptureDevice(CaptureDevice captureDevice)
    {
        if (this.captureDevice != captureDevice)
        {
            CaptureDevice oldValue = this.captureDevice;

            this.captureDevice = captureDevice;
            this.captureDeviceInfo = captureDevice.getCaptureDeviceInfo();

            CaptureDevice newValue = captureDevice;

            captureDeviceChanged(oldValue, newValue);
        }
    }

    /**
     * Gets a human-readable <tt>String</tt> representation of this instance.
     *
     * @return a <tt>String</tt> providing a human-readable representation of
     * this instance
     */
    @Override
    public String toString()
    {
        CaptureDeviceInfo captureDeviceInfo = getCaptureDeviceInfo();

        return
            (captureDeviceInfo == null)
                ? super.toString()
                : captureDeviceInfo.toString();
    }
}
