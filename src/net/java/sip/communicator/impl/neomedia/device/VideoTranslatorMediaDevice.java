/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements a <tt>MediaDevice</tt> which is to be used in video conferencing
 * implemented with an RTP translator.
 *
 * @author Lyubomir Marinov
 */
public class VideoTranslatorMediaDevice
    extends AbstractMediaDevice
{
    /**
     * The <tt>MediaDevice</tt> which this instance enables to be used in a
     * video conference implemented with an RTP translator.
     */
    private final MediaDeviceImpl device;

    /**
     * The <tt>MediaDeviceSession</tt> of {@link #device} the
     * <tt>outputDataSource</tt> of which is the <tt>captureDevice</tt> of
     * {@link #streamDeviceSessions}.
     */
    private MediaDeviceSession deviceSession;

    /**
     * The <tt>MediaStreamMediaDeviceSession</tt>s sharing the
     * <tt>outputDataSource</tt> of {@link #device} as their
     * <tt>captureDevice</tt>.
     */
    private final List<MediaStreamMediaDeviceSession> streamDeviceSessions
        = new LinkedList<MediaStreamMediaDeviceSession>();

    /**
     * Initializes a new <tt>VideoTranslatorMediaDevice</tt> which enables a
     * specific <tt>MediaDevice</tt> to be used in video conferencing
     * implemented with an RTP translator.
     *
     * @param device the <tt>MediaDevice</tt> which the new instance is to
     * enable to be used in video conferencing implemented with an RTP
     * translator
     */
    public VideoTranslatorMediaDevice(MediaDeviceImpl device)
    {
        this.device = device;
    }

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected when all
     * {@link #streamDeviceSessions} have been closed.
     *
     * @param streamDeviceSession the <tt>MediaStreamMediaDeviceSession</tt>
     * which has been closed
     */
    private synchronized void close(
            MediaStreamMediaDeviceSession streamDeviceSession)
    {
        streamDeviceSessions.remove(streamDeviceSession);
        if (streamDeviceSessions.isEmpty())
        {
            deviceSession.close();
            deviceSession = null;
        }
        else
            updateDeviceSessionStartedDirection();
    }

    /**
     * Creates a <tt>DataSource</tt> instance for this <tt>MediaDevice</tt>
     * which gives access to the captured media.
     *
     * @return a <tt>DataSource</tt> instance which gives access to the media
     * captured by this <tt>MediaDevice</tt>
     * @see AbstractMediaDevice#createOutputDataSource()
     */
    synchronized DataSource createOutputDataSource()
    {
        if (deviceSession == null)
        {
            MediaFormatImpl<? extends Format> format = null;
            MediaDirection startedDirection = MediaDirection.INACTIVE;

            for (MediaStreamMediaDeviceSession streamDeviceSession
                    : streamDeviceSessions)
            {
                MediaFormatImpl<? extends Format> streamFormat
                    = streamDeviceSession.getFormat();

                if ((streamFormat != null) && (format == null))
                    format = streamFormat;
                startedDirection
                    = startedDirection.or(
                            streamDeviceSession.getStartedDirection());
            }

            deviceSession = device.createSession();
            if (format != null)
                deviceSession.setFormat(format);
            deviceSession.start(startedDirection);
        }
        return
            (deviceSession == null)
                ? null
                : deviceSession.getOutputDataSource();
    }

    /**
     * Creates a new <tt>MediaDeviceSession</tt> instance which is to represent
     * the use of this <tt>MediaDevice</tt> by a <tt>MediaStream</tt>.
     *
     * @return a new <tt>MediaDeviceSession</tt> instance which is to represent
     * the use of this <tt>MediaDevice</tt> by a <tt>MediaStream</tt>
     * @see AbstractMediaDevice#createSession()
     */
    @Override
    public synchronized MediaDeviceSession createSession()
    {
        MediaStreamMediaDeviceSession streamDeviceSession
            = new MediaStreamMediaDeviceSession();

        streamDeviceSessions.add(streamDeviceSession);
        return streamDeviceSession;
    }

    /**
     * Returns the <tt>MediaDirection</tt> supported by this device.
     *
     * @return <tt>MediaDirection.SENDONLY</tt> if this is a read-only device,
     * <tt>MediaDirection.RECVONLY</tt> if this is a write-only device and
     * <tt>MediaDirection.SENDRECV</tt> if this <tt>MediaDevice</tt> can both
     * capture and render media
     * @see MediaDevice#getDirection()
     */
    public MediaDirection getDirection()
    {
        return device.getDirection();
    }

    /**
     * Returns the <tt>MediaFormat</tt> that this device is currently set to use
     * when capturing data.
     *
     * @return the <tt>MediaFormat</tt> that this device is currently set to
     * provide media in.
     * @see MediaDevice#getFormat()
     */
    public MediaFormat getFormat()
    {
        return device.getFormat();
    }

    /**
     * Returns the <tt>MediaType</tt> that this device supports.
     *
     * @return <tt>MediaType.AUDIO</tt> if this is an audio device or
     * <tt>MediaType.VIDEO</tt> in case of a video device
     * @see MediaDevice#getMediaType()
     */
    public MediaType getMediaType()
    {
        return device.getMediaType();
    }

    /**
     * Returns a list of <tt>MediaFormat</tt> instances representing the media
     * formats supported by this <tt>MediaDevice</tt>.
     *
     * @param localPreset the preset used to set the send format parameters,
     * used for video and settings
     * @param remotePreset the preset used to set the receive format parameters,
     * used for video and settings
     * @return the list of <tt>MediaFormat</tt>s supported by this device
     * @see MediaDevice#getSupportedFormats(QualityPreset, QualityPreset)
     */
    public List<MediaFormat> getSupportedFormats(
            QualityPreset localPreset,
            QualityPreset remotePreset)
    {
        return device.getSupportedFormats(localPreset, remotePreset);
    }

    /**
     * Updates the value of the <tt>startedDirection</tt> property of
     * {@link #deviceSession} to be in accord with the values of the property
     * of {@link #streamDeviceSessions}.
     */
    private synchronized void updateDeviceSessionStartedDirection()
    {
        if (deviceSession == null)
            return;

        MediaDirection startDirection = MediaDirection.INACTIVE;

        for (MediaStreamMediaDeviceSession streamDeviceSession
                : streamDeviceSessions)
        {
            startDirection
                = startDirection.or(streamDeviceSession.getStartedDirection());
        }
        deviceSession.start(startDirection);

        MediaDirection stopDirection = MediaDirection.INACTIVE;

        if (!startDirection.allowsReceiving())
            stopDirection = stopDirection.or(MediaDirection.RECVONLY);
        if (!startDirection.allowsSending())
            stopDirection = stopDirection.or(MediaDirection.SENDONLY);
        deviceSession.stop(stopDirection);
    }

    /**
     * Represents the use of this <tt>VideoTranslatorMediaDevice</tt> by a
     * <tt>MediaStream</tt>.
     */
    private class MediaStreamMediaDeviceSession
        extends VideoMediaDeviceSession
    {
        /**
         * Initializes a new <tt>MediaStreamMediaDeviceSession</tt> which is to
         * represent the use of this <tt>VideoTranslatorMediaDevice</tt> by a
         * <tt>MediaStream</tt>.
         */
        public MediaStreamMediaDeviceSession()
        {
            super(VideoTranslatorMediaDevice.this);
        }

        /**
         * Releases the resources allocated by this instance in the course of
         * its execution and prepares it to be garbage collected.
         */
        @Override
        public void close()
        {
            super.close();

            VideoTranslatorMediaDevice.this.close(this);
        }

        /**
         * Creates the <tt>DataSource</tt> that this instance is to read
         * captured media from.
         *
         * @return the <tt>DataSource</tt> that this instance is to read
         * captured media from
         * @see VideoMediaDeviceSession#createCaptureDevice()
         */
        @Override
        protected DataSource createCaptureDevice()
        {
            return VideoTranslatorMediaDevice.this.createOutputDataSource();
        }

        /**
         * Initializes a new <tt>Player</tt> instance which is to provide the
         * local visual/video <tt>Component</tt>. The new instance is
         * initialized to render the media of a specific <tt>DataSource</tt>.
         *
         * @param captureDevice the <tt>DataSource</tt> which is to have its
         * media rendered by the new instance as the local visual/video
         * <tt>Component</tt>
         * @return a new <tt>Player</tt> instance which is to provide the local
         * visual/video <tt>Component</tt>
         */
        @Override
        protected Player createLocalPlayer(DataSource captureDevice)
        {
            synchronized (VideoTranslatorMediaDevice.this)
            {
                if (deviceSession != null)
                    captureDevice = deviceSession.getCaptureDevice();
            }

            return super.createLocalPlayer(captureDevice);
        }

        /**
         * Initializes a new FMJ <tt>Processor</tt> which is to transcode
         * {@link #captureDevice} into the format of this instance.
         *
         * @return a new FMJ <tt>Processor</tt> which is to transcode
         * <tt>captureDevice</tt> into the format of this instance
         */
        @Override
        protected Processor createProcessor()
        {
            return null;
        }

        /**
         * Gets the output <tt>DataSource</tt> of this instance which provides
         * the captured (RTP) data to be sent by <tt>MediaStream</tt> to
         * <tt>MediaStreamTarget</tt>.
         *
         * @return the output <tt>DataSource</tt> of this instance which
         * provides the captured (RTP) data to be sent by <tt>MediaStream</tt>
         * to <tt>MediaStreamTarget</tt>
         * @see MediaDeviceSession#getOutputDataSource()
         */
        @Override
        public DataSource getOutputDataSource()
        {
            return getConnectedCaptureDevice();
        }

        /**
         * Notifies this instance that the value of its
         * <tt>startedDirection</tt> property has changed from a specific
         * <tt>oldValue</tt> to a specific <tt>newValue</tt>.
         *
         * @param oldValue the <tt>MediaDirection</tt> which used to be the
         * value of the <tt>startedDirection</tt> property of this instance
         * @param newValue the <tt>MediaDirection</tt> which is the value of the
         * <tt>startedDirection</tt> property of this instance
         */
        @Override
        protected void startedDirectionChanged(
                MediaDirection oldValue,
                MediaDirection newValue)
        {
            super.startedDirectionChanged(oldValue, newValue);

            VideoTranslatorMediaDevice.this
                    .updateDeviceSessionStartedDirection();
        }
    }
}
