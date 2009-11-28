/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>MediaDevice</tt> for the JMF <tt>CaptureDevice</tt>.
 *
 * @author Lubomir Marinov
 * @author Emil Ivov
 */
public class MediaDeviceImpl
    extends AbstractMediaDevice
{

    /**
     * The <tt>Logger</tt> used by <tt>MediaDeviceImpl</tt> and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaDeviceImpl.class);

    /**
     * The <tt>CaptureDeviceInfo</tt> of the device that this instance is
     * representing.
     */
    private CaptureDeviceInfo captureDeviceInfo;

    /**
     * The <tt>MediaType</tt> of this instance and the <tt>CaptureDevice</tt>
     * that it wraps.
     */
    private final MediaType mediaType;

    /**
     * The <tt>List</tt> of RTP extensions supported by this device (at the time
     * of writing this list is only filled for audio devices and is
     * <tt>null</tt> otherwise).
     */
    private List<RTPExtension> rtpExtensions = null;

    /**
     * Initializes a new <tt>MediaDeviceImpl</tt> instance with a specific
     * <tt>MediaType</tt> and with <tt>MediaDirection</tt> which does not allow
     * sending.
     *
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public MediaDeviceImpl(MediaType mediaType)
    {
        this.captureDeviceInfo = null;
        this.mediaType = mediaType;
    }

    /**
     * Initializes a new <tt>MediaDeviceImpl</tt> instance which is to provide
     * an implementation of <tt>MediaDevice</tt> for a <tt>CaptureDevice</tt>
     * with a specific <tt>CaptureDeviceInfo</tt> and which is of a specific
     * <tt>MediaType</tt>.
     *
     * @param captureDeviceInfo the <tt>CaptureDeviceInfo</tt> of the JMF
     * <tt>CaptureDevice</tt> the new instance is to provide an implementation
     * of <tt>MediaDevice</tt> for
     * @param mediaType the <tt>MediaType</tt> of the new instance
     */
    public MediaDeviceImpl(
            CaptureDeviceInfo captureDeviceInfo,
            MediaType mediaType)
    {
        if (captureDeviceInfo == null)
            throw new NullPointerException("captureDeviceInfo");
        if (mediaType == null)
            throw new NullPointerException("mediaType");

        this.captureDeviceInfo = captureDeviceInfo;
        this.mediaType = mediaType;
    }

    /**
     * Connects to a specific <tt>CaptureDevice</tt> given in the form of a
     * <tt>DataSource</tt>.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> to be connected to
     * @throws IOException if anything wrong happens while connecting to the
     * specified <tt>captureDevice</tt>
     * @see AbstractMediaDevice#connect(DataSource)
     */
    @Override
    public void connect(DataSource captureDevice)
        throws IOException
    {
        super.connect(captureDevice);

        /*
         * 1. Changing buffer size. The default buffer size (for JavaSound) is
         * 125 milliseconds - 1/8 sec. On Mac OS X this leads to an exception
         * and no audio capture. A value of 30 for the buffer fixes the problem
         * and is OK when using some PSTN gateways.
         *
         * 2. Changing to 60. When it is 30 there are some issues with Asterisk
         * and NAT (we don't start to send stream and so Asterisk RTP part
         * doesn't notice that we are behind NAT).
         *
         * 3. Do not set buffer length on Linux as it completely breaks audio
         * capture.
         */
        String osName = System.getProperty("os.name");

        if ((osName == null) || !osName.toLowerCase().contains("linux"))
        {
            Control bufferControl
                = (Control)
                    captureDevice
                        .getControl("javax.media.control.BufferControl");

            if (bufferControl != null)
                ((BufferControl) bufferControl)
                    .setBufferLength(60); // in milliseconds
        }
    }

    /**
     * A default implementation for the
     * {@link MediaDevice#getSupportedExtensions()} method returning
     * <tt>null</tt> and hence indicating support for no RTP extensions.
     *
     * @return <tt>null</tt>, indicating that this device does not support any
     * RTP extensions.
     */
    public List<RTPExtension> getSupportedExtensions()
    {
        if ( getMediaType() != MediaType.AUDIO)
            return null;

        if ( rtpExtensions == null)
        {
            rtpExtensions = new ArrayList<RTPExtension>(1);

            URI csrcAudioLevelURN;
            try
            {
                csrcAudioLevelURN = new URI(RTPExtension.CSRC_AUDIO_LEVEL_URN);
            }
            catch (URISyntaxException e)
            {
                // can't happen since CSRC_AUDIO_LEVEL_URN is a valid URI and
                // never changes.
                logger.info("Aha! Someone messed with the source!", e);
                return null;
            }

            rtpExtensions.add(new RTPExtension(
                               csrcAudioLevelURN, MediaDirection.RECVONLY));
        }

        return rtpExtensions;
    }

    /**
     * Creates the JMF <tt>CaptureDevice</tt> this instance represents and
     * provides an implementation of <tt>MediaDevice</tt> for.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance represents and
     * provides an implementation of <tt>MediaDevice</tt> for; <tt>null</tt>
     * if the creation fails
     */
    CaptureDevice createCaptureDevice()
    {
        CaptureDevice captureDevice = null;

        if (getDirection().allowsSending())
        {
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
                        "Failed to create CaptureDevice"
                            + " from CaptureDeviceInfo "
                            + captureDeviceInfo,
                        exception);
            else
            {
                // Try to enable tracing on captureDevice.
                if (logger.isTraceEnabled())
                    captureDevice
                        = createTracingCaptureDevice(captureDevice, logger);
            }
        }
        return captureDevice;
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
        return
            getDirection().allowsSending()
                ? (DataSource) createCaptureDevice()
                : null;
    }

    /**
     * Creates a new <tt>CaptureDevice</tt> which traces calls to a specific
     * <tt>CaptureDevice</tt> for debugging purposes.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> which is to have its
     * calls traced for debugging output
     * @param logger the <tt>Logger</tt> to be used for logging the trace
     * messages
     * @return a new <tt>CaptureDevice</tt> which traces the calls to the
     * specified <tt>captureDevice</tt>
     */
    public static CaptureDevice createTracingCaptureDevice(
            CaptureDevice captureDevice,
            final Logger logger)
    {
        if (captureDevice instanceof PushBufferDataSource)
            captureDevice
                = new CaptureDeviceDelegatePushBufferDataSource(
                        captureDevice)
                {
                    @Override
                    public void connect()
                        throws IOException
                    {
                        super.connect();

                        if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Connected "
                                        + MediaDeviceImpl
                                            .toString(this.captureDevice));
                    }

                    @Override
                    public void disconnect()
                    {
                        super.disconnect();

                        if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Disconnected "
                                        + MediaDeviceImpl
                                            .toString(this.captureDevice));
                    }

                    @Override
                    public void start()
                        throws IOException
                    {
                        super.start();

                        if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Started "
                                        + MediaDeviceImpl
                                            .toString(this.captureDevice));
                    }

                    @Override
                    public void stop()
                        throws IOException
                    {
                        super.stop();

                        if (logger.isTraceEnabled())
                            logger
                                .trace(
                                    "Stopped "
                                        + MediaDeviceImpl
                                            .toString(this.captureDevice));
                    }
                };
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
        if (captureDeviceInfo != null)
            return MediaDirection.SENDRECV;
        else
            return
                MediaType.AUDIO.equals(getMediaType())
                    ? MediaDirection.INACTIVE
                    : MediaDirection.RECVONLY;
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
        CaptureDevice captureDevice = createCaptureDevice();

        if (captureDevice != null)
        {
            MediaType mediaType = getMediaType();

            for (FormatControl formatControl
                    : captureDevice.getFormatControls())
            {
                MediaFormat format
                    = MediaFormatImpl.createInstance(formatControl.getFormat());

                if ((format != null) && format.getMediaType().equals(mediaType))
                    return format;
            }
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

    /**
     * Returns a human-readable representation of a specific
     * <tt>CaptureDevice</tt> in the form of a <tt>String</tt> value.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> to get a human-readable
     * representation of
     * @return a <tt>String</tt> value which gives a human-readable
     * representation of the specified <tt>captureDevice</tt>
     */
    private static String toString(CaptureDevice captureDevice)
    {
        StringBuffer str = new StringBuffer();

        str.append("CaptureDevice with hashCode ");
        str.append(captureDevice.hashCode());
        str.append(" and captureDeviceInfo ");

        CaptureDeviceInfo captureDeviceInfo
            = captureDevice.getCaptureDeviceInfo();
        MediaLocator mediaLocator = captureDeviceInfo.getLocator();

        str.append((mediaLocator == null) ? captureDeviceInfo : mediaLocator);
        return str.toString();
    }
}
