/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
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
    implements MediaDevice
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
    private DataSource captureDevice;

    /**
     * The <tt>CaptureDeviceInfo</tt> of {@link #captureDevice}.
     */
    private final CaptureDeviceInfo captureDeviceInfo;

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
     * The JMF <tt>Processor</tt> which transcodes {@link #captureDevice} into
     * the format of this instance.
     */
    private Processor processor;

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

        this.captureDevice = (DataSource) captureDevice;
        this.captureDeviceInfo = captureDevice.getCaptureDeviceInfo();
        this.mediaType = mediaType;
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
     * For JPEG and H263, we know that they only work for particular
     * sizes.  So we'll perform extra checking here to make sure they
     * are of the right sizes.
     *
     * @param sourceFormat the original format that we'd like to check for
     * size.
     * @return the modified <tt>VideoFormat</tt> set to the size we support.
     */
    private VideoFormat assertSize(VideoFormat sourceFormat)
    {
        int width, height;

        // JPEG
        if (sourceFormat.matches(new Format(VideoFormat.JPEG_RTP)))
        {
            Dimension size = sourceFormat.getSize();

            // For JPEG, make sure width and height are divisible by 8.
            width = (size.width % 8 == 0)
                ? size.width
                : ( ( (size.width / 8)) * 8);
            height = (size.height % 8 == 0)
                ? size.height
                : (size.height / 8) * 8;
        }
        // H.263
        else if (sourceFormat.matches(new Format(VideoFormat.H263_RTP)))
        {
            // For H.263, we only support some specific sizes.
            //if (size.width < 128)
//            {
//                width = 128;
//                height = 96;
//            }
            //else if (size.width < 176)
//            {
//                width = 176;
//                height = 144;
//            }
            //else
//            {
                width = 352;
                height = 288;

//            }
        }
        else
        {
            // We don't know this particular format.  We'll just
            // leave it alone then.
            return sourceFormat;
        }

        VideoFormat result = new VideoFormat(null,
                                             new Dimension(width, height),
                                             Format.NOT_SPECIFIED,
                                             null,
                                             Format.NOT_SPECIFIED);
        return (VideoFormat) result.intersects(sourceFormat);
    }

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected.
     */
    public void close()
    {
        if (captureDevice != null)
        {
            /*
             * As reported by Carlos Alexandre, stopping before disconnecting
             * resolves a slow disconnect on Linux.
             */
            try
            {
                captureDevice.stop();
            }
            catch (IOException ex)
            {
                /*
                 * We cannot do much about the exception because we're not
                 * really interested in the stopping but rather in calling
                 * DataSource#disconnect() anyway.
                 */
                logger.error("Failed to properly stop avDataSource.", ex);
            }

            captureDevice.disconnect();
        }
        if (processor != null)
        {
            processor.stop();
            if (processor.getState() == Processor.Realized)
            {
                DataSource dataOutput = processor.getDataOutput();

                if (dataOutput != null)
                    dataOutput.disconnect();
            }
            processor.deallocate();
            processor.close();
        }
    }

    /**
     * Finds the first <tt>Format</tt> instance in a specific list of
     * <tt>Format</tt>s which matches a specific <tt>Format</tt>. The
     * implementation considers a pair of <tt>Format</tt>s matching if they have
     * the same encoding.
     *
     * @param formats the array of <tt>Format</tt>s to be searched for a match
     * to the specified <tt>format</tt>
     * @param format the <tt>Format</tt> to search for a match in the specified
     * <tt>formats</tt>
     * @return the first element of <tt>formats</tt> which matches
     * <tt>format</tt> i.e. is of the same encoding
     */
    private Format findFirstMatchingFormat(Format[] formats, Format format)
    {
        for (Format match : formats)
            if (match.isSameEncoding(format))
                return match;
        return null;
    }

    /**
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for
     */
    private DataSource getCaptureDevice()
    {
        if (captureDevice == null)
        {
            try
            {
                captureDevice
                    = Manager.createDataSource(captureDeviceInfo.getLocator());
            }
            catch (IOException ioe)
            {
                // TODO
            }
            catch (NoDataSourceException ndse)
            {
                // TODO
            }
        }
        return captureDevice;
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
    private DataSource getConnectedCaptureDevice()
    {
        DataSource captureDevice = getCaptureDevice();

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
                            captureDevice
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
     * Gets the output <tt>DataSource</tt> of this instance which provides the
     * captured (RTP) data to be sent by <tt>MediaStream</tt> to
     * <tt>MediaStreamTarget</tt>.
     *
     * @return the output <tt>DataSource</tt> of this instance which provides
     * the captured (RTP) data to be sent by <tt>MediaStream</tt> to
     * <tt>MediaStreamTarget</tt>
     */
    public DataSource getDataSource()
    {
        Processor processor = getProcessor();

        return (processor == null) ? null : processor.getDataOutput();
    }

    /*
     * Implements MediaDevice#getDirection().
     */
    public MediaDirection getDirection()
    {
        return MediaDirection.SENDRECV;
    }

    /*
     * Implements MediaDevice#getFormat().
     */
    public MediaFormat getFormat()
    {
        Processor processor = getProcessor();

        if (processor != null)
        {
            MediaType mediaType = getMediaType();

            for (TrackControl trackControl : processor.getTrackControls())
            {
                if (!trackControl.isEnabled())
                    continue;

                MediaFormat format
                    = MediaFormatImpl.createInstance(trackControl.getFormat());

                if ((format != null) && format.getMediaType().equals(mediaType))
                    return format;
            }
        }
        return null;
    }

    /*
     * Implements MediaDevice#getMediaType().
     */
    public MediaType getMediaType()
    {
        return mediaType;
    }

    /**
     * Gets the JMF <tt>Processor</tt> which transcodes the
     * <tt>CaptureDevice</tt> wrapped by this instance into the format of this
     * instance.
     *
     * @return the JMF <tt>Processor</tt> which transcodes the
     * <tt>CaptureDevice</tt> wrapped by this instance into the format of this
     * instance
     */
    private Processor getProcessor()
    {
        if (processor == null)
        {
            DataSource captureDevice = getConnectedCaptureDevice();

            if (captureDevice != null)
            {
                Processor processor = null;

                try
                {
                    processor = Manager.createProcessor(captureDevice);
                }
                catch (IOException ioe)
                {
                    // TODO
                }
                catch (NoProcessorException npe)
                {
                    // TODO
                }

                if (waitForState(processor, Processor.Configured))
                {
                    try
                    {
                        processor
                            .setContentDescriptor(
                                new ContentDescriptor(
                                        ContentDescriptor.RAW_RTP));
                    }
                    catch (NotConfiguredError nce)
                    {
                        // TODO
                        processor = null;
                    }

                    if (processor != null)
                        this.processor = processor;
                }
                else
                    processor = null;
            }
        }
        return processor;
    }

    /*
     * Implements MediaDevice#getSupportedFormats().
     */
    public List<MediaFormat> getSupportedFormats()
    {
        Processor processor = getProcessor();
        Set<Format> supportedFormats = new HashSet<Format>();

        if (processor != null)
        {
            MediaType mediaType = getMediaType();

            for (TrackControl trackControl : processor.getTrackControls())
            {
                if (!trackControl.isEnabled())
                    continue;

                for (Format supportedFormat : trackControl.getSupportedFormats())
                    switch (mediaType)
                    {
                    case AUDIO:
                        if (supportedFormat instanceof AudioFormat)
                            supportedFormats.add(supportedFormat);
                        break;
                    case VIDEO:
                        if (supportedFormat instanceof VideoFormat)
                            supportedFormats.add(supportedFormat);
                        break;
                    }
            }
        }

        List<MediaFormat> supportedMediaFormats
            = new ArrayList<MediaFormat>(supportedFormats.size());

        for (Format format : supportedFormats)
            supportedMediaFormats.add(MediaFormatImpl.createInstance(format));
        return supportedMediaFormats;
    }

    /**
     * Sets the <tt>MediaFormat</tt> in which this <tt>MediaDevice</tt> is to
     * capture data.
     *
     * @param format the <tt>MediaFormat</tt> in which this <tt>MediaDevice</tt>
     * is to capture data
     */
    public void setFormat(MediaFormat format)
    {
        MediaType mediaType = getMediaType();

        if (!mediaType.equals(format.getMediaType()))
            throw new IllegalArgumentException("format");

        /*
         * We need javax.media.Format and we know how to convert MediaFormat to
         * it only for MediaFormatImpl so assert early.
         */
        MediaFormatImpl<? extends Format> mediaFormatImpl
            = (MediaFormatImpl<? extends Format>) format;

        Processor processor = getProcessor();

        if (processor != null)
        {
            if ((processor.getState() < Processor.Configured)
                    && !waitForState(processor, Processor.Configured))
            {
                // TODO
                return;
            }

            for (TrackControl trackControl : processor.getTrackControls())
            {
                if (!trackControl.isEnabled())
                    continue;

                Format[] supportedFormats = trackControl.getSupportedFormats();

                if ((supportedFormats == null) || (supportedFormats.length < 1))
                {
                    trackControl.setEnabled(false);
                    continue;
                }

                Format supportedFormat = null;

                switch (mediaType)
                {
                case AUDIO:
                    if (supportedFormats[0] instanceof AudioFormat)
                    {
                        if (FMJConditionals.FORCE_AUDIO_FORMAT != null)
                            trackControl
                                .setFormat(FMJConditionals.FORCE_AUDIO_FORMAT);
                        else
                        {
                            supportedFormat
                                = findFirstMatchingFormat(
                                    supportedFormats,
                                    mediaFormatImpl.getFormat());
                        }
                    }
                    break;
                case VIDEO:
                    if (supportedFormats[0] instanceof VideoFormat)
                    {
                        supportedFormat
                            = findFirstMatchingFormat(
                                supportedFormats,
                                mediaFormatImpl.getFormat());

                        if (supportedFormat != null)
                            supportedFormat
                                = assertSize((VideoFormat) supportedFormat);
                    }
                    break;
                }

                if (supportedFormat == null)
                    trackControl.setEnabled(false);
                else
                    trackControl.setFormat(supportedFormat);
            }
        }
    }

    /**
     * Starts the processing of media in this instance in a specific direction.
     *
     * @param direction a <tt>MediaDirection</tt> value which represents the
     * direction of the processing of media to be started. For example,
     * {@link MediaDirection#SENDRECV} to start both capture and playback of
     * media in this instance or {@link MediaDirection#SENDONLY} to only start
     * the capture of media in this instance
     */
    public void start(MediaDirection direction)
    {
        if (direction == null)
            throw new IllegalArgumentException("direction");

        if (MediaDirection.SENDRECV.equals(direction)
                || MediaDirection.SENDONLY.equals(direction))
        {
            Processor processor = getProcessor();

            if ((processor != null)
                    && (processor.getState() != Processor.Started))
                processor.start();
        }
    }

    /**
     * Stops the processing of media in this instance in a specific direction.
     *
     * @param direction a <tt>MediaDirection</tt> value which represents the
     * direction of the processing of media to be stopped. For example,
     * {@link MediaDirection#SENDRECV} to stop both capture and playback of
     * media in this instance or {@link MediaDirection#SENDONLY} to only stop
     * the capture of media in this instance
     */
    public void stop(MediaDirection direction)
    {
        if (direction == null)
            throw new IllegalArgumentException("direction");

        if (MediaDirection.SENDRECV.equals(direction)
                || MediaDirection.SENDONLY.equals(direction))
            if ((processor != null)
                    && (processor.getState() == Processor.Started))
                processor.start();
    }

    /**
     * Waits for the specified JMF <tt>Processor</tt> to enter the specified
     * <tt>state</tt> and returns <tt>true</tt> if <tt>processor</tt> has
     * successfully entered <tt>state</tt> or <tt>false</tt> if <tt>process</tt>
     * has failed to enter <tt>state</tt>.
     *
     * @param processor the JMF <tt>Processor</tt> to wait on
     * @param state the state as defined by the respective <tt>Processor</tt>
     * state constants to wait <tt>processor</tt> to enter
     * @return <tt>true</tt> if <tt>processor</tt> has successfully entered
     * <tt>state</tt>; otherwise, <tt>false</tt>
     */
    private static boolean waitForState(Processor processor, int state)
    {
        return new ProcessorUtility().waitForState(processor, state);
    }
}
