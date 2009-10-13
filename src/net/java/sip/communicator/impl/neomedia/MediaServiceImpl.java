/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;

/**
 * @author Lubomir Marinov
 */
public class MediaServiceImpl
    implements MediaService
{

    /**
     * With this property video support can be disabled (enabled by default).
     */
    public static final String DISABLE_VIDEO_SUPPORT_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.DISABLE_VIDEO_SUPPORT";

    /**
     * The value of the <tt>devices</tt> property of <tt>MediaServiceImpl</tt>
     * when no <tt>MediaDevice</tt>s are available. Explicitly defined in order
     * to reduce unnecessary allocations.
     */
    private static final List<MediaDevice> EMPTY_DEVICES
        = Collections.emptyList();

    /**
     * The <tt>CaptureDevice</tt> user choices such as the default audio and
     * video capture devices.
     */
    private final DeviceConfiguration deviceConfiguration
        = new DeviceConfiguration();

    /**
     * The format-related user choices such as the enabled and disabled codecs
     * and the order of their preference.
     */
    private final EncodingConfiguration encodingConfiguration
        = new EncodingConfiguration();

    /*
     * Implements MediaService#createMediaStream(StreamConnector, MediaDevice).
     */
    public MediaStream createMediaStream(
            StreamConnector connector,
            MediaDevice device)
    {
        switch (device.getMediaType())
        {
        case AUDIO:
            return new AudioMediaStreamImpl(connector, device);
        case VIDEO:
            return new VideoMediaStreamImpl(connector, device);
        default:
            return null;
        }
    }

    /*
     * Implements MediaService#getDefaultDevice(MediaType).
     */
    public MediaDevice getDefaultDevice(MediaType mediaType)
    {
        CaptureDeviceInfo captureDeviceInfo;

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfo
                = getDeviceConfiguration().getAudioCaptureDevice();
            break;
        case VIDEO:
            captureDeviceInfo
                = getDeviceConfiguration().getVideoCaptureDevice();
            break;
        default:
            captureDeviceInfo = null;
            break;
        }

        return
            (captureDeviceInfo == null)
                ? null
                : new CaptureMediaDevice(captureDeviceInfo, mediaType);
    }

    /**
     * Gets the <tt>CaptureDevice</tt> user choices such as the default audio
     * and video capture devices.
     *
     * @return the <tt>CaptureDevice</tt> user choices such as the default audio
     * and video capture devices.
     */
    DeviceConfiguration getDeviceConfiguration()
    {
        return deviceConfiguration;
    }

    /*
     * Implements MediaService#getDevices(MediaType).
     */
    public List<MediaDevice> getDevices(MediaType mediaType)
    {
        CaptureDeviceInfo[] captureDeviceInfos;

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfos
                = getDeviceConfiguration().getAvailableAudioCaptureDevices();
            break;
        case VIDEO:
            captureDeviceInfos
                = getDeviceConfiguration().getAvailableVideoCaptureDevices();
            break;
        default:
            captureDeviceInfos = null;
            break;
        }

        List<MediaDevice> captureDevices;

        if ((captureDeviceInfos == null) || (captureDeviceInfos.length == 0))
            captureDevices = EMPTY_DEVICES;
        else
        {
            captureDevices
                = new ArrayList<MediaDevice>(captureDeviceInfos.length);
            for (CaptureDeviceInfo captureDeviceInfo : captureDeviceInfos)
                captureDevices
                    .add(new CaptureMediaDevice(captureDeviceInfo, mediaType));
        }
        return captureDevices;
    }

    /**
     * Gets the format-related user choices such as the enabled and disabled
     * codecs and the order of their preference.
     *
     * @return the format-related user choices such as the enabled and disabled
     * codecs and the order of their preference
     */
    EncodingConfiguration getEncodingConfiguration()
    {
        return encodingConfiguration;
    }

    /**
     * Starts this <tt>MediaService</tt> implementation and thus makes it
     * operational.
     */
    void start()
    {
        deviceConfiguration.initialize();
        encodingConfiguration.initializeFormatPreferences();
        encodingConfiguration.registerCustomPackages();
    }

    /**
     * Stops this <tt>MediaService</tt> implementation and thus signals that its
     * utilization should cease.
     */
    void stop()
    {
    }
}
