/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaDevice</tt> for the JMF <tt>CaptureDevice</tt>.
 *
 * @author Lubomir Marinov
 */
public class CaptureMediaDevice
    implements MediaDevice
{

    /**
     * The JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     */
    private CaptureDevice captureDevice;

    /**
     * The <tt>CaptureDeviceInfo</tt> of {@link #captureDevice}.
     */
    private final CaptureDeviceInfo captureDeviceInfo;

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

        this.captureDevice = captureDevice;
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
     * Gets the JMF <tt>CaptureDevice</tt> this instance wraps and provides an
     * implementation of <tt>MediaDevice</tt> for.
     *
     * @return the JMF <tt>CaptureDevice</tt> this instance wraps and provides
     * an implementation of <tt>MediaDevice</tt> for
     */
    private CaptureDevice getCaptureDevice()
    {
        if (captureDevice == null)
            captureDevice
                = (CaptureDevice)
                    MediaControl
                        .createDataSource(
                            captureDeviceInfo.getLocator());
        return captureDevice;
    }

    /*
     * Implements MediaDevice#getDirection(). Because CaptureDevice can only be
     * read from, returns MediaDirection#SENDONLY.
     */
    public MediaDirection getDirection()
    {
        return MediaDirection.SENDONLY;
    }

    /*
     * Implements MediaDevice#getFormat().
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

    /*
     * Implements MediaDevice#getMediaType().
     */
    public MediaType getMediaType()
    {
        return mediaType;
    }

    /*
     * Implements MediaDevice#getSupportedFormats().
     */
    public List<MediaFormat> getSupportedFormats()
    {
        return
            (captureDevice == null)
                ? getSupportedFormats(captureDeviceInfo)
                : getSupportedFormats(captureDevice);
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
}
