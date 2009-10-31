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
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * Implements <tt>MediaService</tt> for JMF.
 *
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
     * The list of audio <tt>MediaDevice</tt>s reported by this instance when
     * its {@link MediaService#getDevices(MediaType)} method is called with an
     * argument {@link MediaType#AUDIO}.
     */
    private final List<CaptureMediaDevice> audioDevices
        = new ArrayList<CaptureMediaDevice>();

    /**
     * The format-related user choices such as the enabled and disabled codecs
     * and the order of their preference.
     */
    private final EncodingConfiguration encodingConfiguration
        = new EncodingConfiguration();

    /**
     * The <tt>MediaFormatFactory</tt> through which <tt>MediaFormat</tt>
     * instances may be created for the purposes of working with the
     * <tt>MediaStream</tt>s created by this <tt>MediaService</tt>.
     */
    private MediaFormatFactory formatFactory;

    /**
     * The list of video <tt>MediaDevice</tt>s reported by this instance when
     * its {@link MediaService#getDevices(MediaType)} method is called with an
     * argument {@link MediaType#VIDEO}.
     */
    private final List<CaptureMediaDevice> videoDevices
        = new ArrayList<CaptureMediaDevice>();

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
            return
                (captureDeviceInfo == null)
                    ? null
                    : new AudioCaptureMediaDevice(captureDeviceInfo);
        case VIDEO:
            captureDeviceInfo
                = getDeviceConfiguration().getVideoCaptureDevice();
            return
                (captureDeviceInfo == null)
                    ? null
                    : new CaptureMediaDevice(captureDeviceInfo, mediaType);
        default:
            return null;
        }
    }

    /**
     * Gets the <tt>CaptureDevice</tt> user choices such as the default audio
     * and video capture devices.
     *
     * @return the <tt>CaptureDevice</tt> user choices such as the default audio
     * and video capture devices.
     */
    public DeviceConfiguration getDeviceConfiguration()
    {
        return deviceConfiguration;
    }

    /**
     * Gets a list of the <tt>MediaDevice</tt>s known to this
     * <tt>MediaService</tt> and handling the specified <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> to obtain the
     * <tt>MediaDevice</tt> list for
     * @return a new <tt>List</tt> of <tt>MediaDevice</tt>s known to this
     * <tt>MediaService</tt> and handling the specified <tt>MediaType</tt>. The
     * returned <tt>List</tt> is a copy of the internal storage and,
     * consequently, modifications to it do not affect this instance. Despite
     * the fact that a new <tt>List</tt> instance is returned by each call to
     * this method, the <tt>MediaDevice</tt> instances are the same if they are
     * still known to this <tt>MediaService</tt> to be available.
     * @see MediaService#getDevices(MediaType)
     */
    public List<MediaDevice> getDevices(MediaType mediaType)
    {
        CaptureDeviceInfo[] captureDeviceInfos;
        List<CaptureMediaDevice> devices;

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfos
                = getDeviceConfiguration().getAvailableAudioCaptureDevices();
            devices = audioDevices;
            break;
        case VIDEO:
            captureDeviceInfos
                = getDeviceConfiguration().getAvailableVideoCaptureDevices();
            devices = videoDevices;
            break;
        default:
            captureDeviceInfos = null;
            devices = null;
            break;
        }

        synchronized (devices)
        {
            if ((captureDeviceInfos == null) || (captureDeviceInfos.length == 0))
            {
                devices.clear();
                return EMPTY_DEVICES;
            }

            Iterator<CaptureMediaDevice> deviceIter = devices.iterator();

            while (deviceIter.hasNext())
            {
                CaptureDeviceInfo captureDeviceInfo
                    = deviceIter.next().getCaptureDeviceInfo();
                boolean deviceIsFound = false;

                for (int i = 0; i < captureDeviceInfos.length; i++)
                    if (captureDeviceInfo.equals(captureDeviceInfos[i]))
                    {
                        deviceIsFound = true;
                        captureDeviceInfos[i] = null;
                        break;
                    }
                if (!deviceIsFound)
                    deviceIter.remove();
            }

            for (CaptureDeviceInfo captureDeviceInfo : captureDeviceInfos)
            {
                if (captureDeviceInfo == null)
                    continue;

                CaptureMediaDevice device;

                switch (mediaType)
                {
                case AUDIO:
                    device = new AudioCaptureMediaDevice(captureDeviceInfo);
                    break;
                case VIDEO:
                default:
                    device
                        = new CaptureMediaDevice(captureDeviceInfo, mediaType);
                    break;
                }
                devices.add(device);
            }

            return new ArrayList<MediaDevice>(devices);
        }
    }

    /**
     * Gets the format-related user choices such as the enabled and disabled
     * codecs and the order of their preference.
     *
     * @return the format-related user choices such as the enabled and disabled
     * codecs and the order of their preference
     */
    public EncodingConfiguration getEncodingConfiguration()
    {
        return encodingConfiguration;
    }

    /**
     * Gets the <tt>MediaFormatFactory</tt> through which <tt>MediaFormat</tt>
     * instances may be created for the purposes of working with the
     * <tt>MediaStream</tt>s created by this <tt>MediaService</tt>.
     *
     * @return the <tt>MediaFormatFactory</tt> through which
     * <tt>MediaFormat</tt> instances may be created for the purposes of working
     * with the <tt>MediaStream</tt>s created by this <tt>MediaService</tt>
     * @see MediaService#getFormatFactory()
     */
    public MediaFormatFactory getFormatFactory()
    {
        if (formatFactory == null)
            formatFactory = new MediaFormatFactoryImpl();
        return formatFactory;
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
