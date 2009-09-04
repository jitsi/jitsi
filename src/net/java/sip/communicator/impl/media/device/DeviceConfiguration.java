/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.device;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * This class aims to provide a simple configuration interface for JMF. It
 * retrieves stored configuration when started or listens to ConfigurationEvent
 * for property changes and configures the JMF accordingly.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
@SuppressWarnings("unchecked")
public class DeviceConfiguration
    extends PropertyChangeNotifier
{

    /**
     * The name of the <code>DeviceConfiguration</code> property which
     * represents the device used by <code>DeviceConfiguration</code> for video
     * capture.
     */
    public static final String AUDIO_CAPTURE_DEVICE = "AUDIO_CAPTURE_DEVICE";

    /**
     * The name of the <code>DeviceConfiguration</code> property which
     * represents the device used by <code>DeviceConfiguration</code> for video
     * capture.
     */
    public static final String VIDEO_CAPTURE_DEVICE = "VIDEO_CAPTURE_DEVICE";

    private static final String PROP_AUDIO_DEVICE =
        "net.java.sip.communicator.impl.media.audiodev";

    private static final String PROP_AUDIO_DEVICE_IS_DISABLED =
        "net.java.sip.communicator.impl.media.audiodevIsDisabled";

    private static final String PROP_VIDEO_DEVICE =
        "net.java.sip.communicator.impl.media.videodev";

    private static final String PROP_VIDEO_DEVICE_IS_DISABLED =
        "net.java.sip.communicator.impl.media.videodevIsDisabled";

    private static final CaptureDeviceInfo[] NO_CAPTURE_DEVICES =
        new CaptureDeviceInfo[0];

    private Logger logger = Logger.getLogger(DeviceConfiguration.class);

    /**
     * The device that we'll be using for audio capture.
     */
    private CaptureDeviceInfo audioCaptureDevice = null;

    /**
     * The device that we'll be using for video capture.
     */
    private CaptureDeviceInfo videoCaptureDevice;

    /**
     * Default constructor.
     */
    public DeviceConfiguration()
    {
        //dummy ... XXX do we really need it though?
    }

    /**
     * Initializes capture devices.
     */
    public void initialize()
    {
        // these seem to be throwing exceptions every now and then so we'll
        // blindly catch them for now
        try
        {
            JmfDeviceDetector.detectAndConfigureCaptureDevices();
            extractConfiguredCaptureDevices();
        }
        catch (Exception ex)
        {
            logger.error("Failed to initialize media.", ex);
        }
    }

    /**
     * Detects capture devices configured through JMF and disable audio and/or
     * video transmission if none were found. Stores found devices in
     * audioCaptureDevice and videoCaptureDevice.
     */
    private void extractConfiguredCaptureDevices()
    {
        ConfigurationService config = MediaActivator.getConfigurationService();

        logger.info("Scanning for configured Audio Devices.");
        CaptureDeviceInfo[] audioCaptureDevices =
            getAvailableAudioCaptureDevices();
        if (config.getBoolean(PROP_AUDIO_DEVICE_IS_DISABLED, false))
            audioCaptureDevice = null;
        else if (audioCaptureDevices.length < 1)
        {
            logger.warn("No Audio Device was found.");
            audioCaptureDevice = null;
        }
        else
        {
            logger.debug("Found " + audioCaptureDevices.length
                + " capture devices: " + audioCaptureDevices);

            String audioDevName = config.getString(PROP_AUDIO_DEVICE);

            if(audioDevName == null)
                audioCaptureDevice = audioCaptureDevices[0];
            else
            {
                for (CaptureDeviceInfo captureDeviceInfo : audioCaptureDevices)
                {
                    if (audioDevName.equals(captureDeviceInfo.getName()))
                    {
                        audioCaptureDevice = captureDeviceInfo;
                        break;
                    }
                }
            }
            if (audioCaptureDevice != null)
                logger.info("Found " + audioCaptureDevice.getName()
                    + " as an audio capture device.");
        }

        if (config.getBoolean(PROP_VIDEO_DEVICE_IS_DISABLED, false))
            videoCaptureDevice = null;
        else
        {
            logger.info("Scanning for configured Video Devices.");
            videoCaptureDevice =
                extractConfiguredVideoCaptureDevice(VideoFormat.RGB);
            // no RGB camera found. And what about YUV ?
            if (videoCaptureDevice == null)
            {
                videoCaptureDevice =
                    extractConfiguredVideoCaptureDevice(VideoFormat.YUV);
                if (videoCaptureDevice == null)
                    logger.info("No Video Device was found.");
            }
        }
    }

    private CaptureDeviceInfo extractConfiguredVideoCaptureDevice(String format)
    {
        List<CaptureDeviceInfo> videoCaptureDevices =
            CaptureDeviceManager.getDeviceList(new VideoFormat(format));
        CaptureDeviceInfo videoCaptureDevice = null;

        if (videoCaptureDevices.size() > 0)
        {
            String videoDevName =
                MediaActivator.getConfigurationService().getString(
                    PROP_VIDEO_DEVICE);

            if (videoDevName == null)
                videoCaptureDevice = videoCaptureDevices.get(0);
            else
            {
                for (CaptureDeviceInfo captureDeviceInfo : videoCaptureDevices)
                {
                    if (videoDevName.equals(captureDeviceInfo.getName()))
                    {
                        videoCaptureDevice = captureDeviceInfo;
                        break;
                    }
                }
            }

            if (videoCaptureDevice != null)
                logger.info("Found " + videoCaptureDevice.getName()
                    + " as an RGB Video Device.");
        }
        return videoCaptureDevice;
    }

    /**
     * Returns a device that we could use for audio capture.
     *
     * @return the CaptureDeviceInfo of a device that we could use for audio
     *         capture.
     */
    public CaptureDeviceInfo getAudioCaptureDevice()
    {
        return audioCaptureDevice;
    }

    /**
     * Gets the list of audio capture devices which are available through this
     * <code>DeviceConfiguration</code>, amongst which is
     * {@link #getAudioCaptureDevice()} and represent acceptable values
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo)}
     *
     * @return an array of <code>CaptureDeviceInfo</code> describing the audio
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableAudioCaptureDevices()
    {
        Vector<CaptureDeviceInfo> audioCaptureDevices =
            CaptureDeviceManager.getDeviceList(new AudioFormat(
                AudioFormat.LINEAR, 44100, 16, 1));// 1 means 1 channel for mono

        return audioCaptureDevices.toArray(NO_CAPTURE_DEVICES);
    }

    /**
     * Gets the list of video capture devices which are available through this
     * <code>DeviceConfiguration</code>, amongst which is
     * {@link #getVideoCaptureDevice()} and represent acceptable values
     * for {@link #setVideoCaptureDevice(CaptureDeviceInfo)}
     *
     * @return an array of <code>CaptureDeviceInfo</code> describing the video
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableVideoCaptureDevices()
    {
        Set<CaptureDeviceInfo> videoCaptureDevices =
            new HashSet<CaptureDeviceInfo>();

        videoCaptureDevices.addAll(CaptureDeviceManager
            .getDeviceList(new VideoFormat(VideoFormat.RGB)));
        videoCaptureDevices.addAll(CaptureDeviceManager
            .getDeviceList(new VideoFormat(VideoFormat.YUV)));
        return videoCaptureDevices.toArray(NO_CAPTURE_DEVICES);
    }

    /**
     * Returns a device that we could use for video capture.
     *
     * @return the CaptureDeviceInfo of a device that we could use for video
     *         capture.
     */
    public CaptureDeviceInfo getVideoCaptureDevice()
    {
        return videoCaptureDevice;
    }

    /**
     * Sets the device which is to be used by this
     * <code>DeviceConfiguration</code> for video capture.
     *
     * @param device a <code>CaptureDeviceInfo</code> describing device to be
     *            used by this <code>DeviceConfiguration</code> for video
     *            capture
     */
    public void setVideoCaptureDevice(CaptureDeviceInfo device)
    {
        if (videoCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = videoCaptureDevice;

            videoCaptureDevice = device;

            ConfigurationService config =
                MediaActivator.getConfigurationService();
            config.setProperty(PROP_VIDEO_DEVICE_IS_DISABLED,
                videoCaptureDevice == null);
            if (videoCaptureDevice != null)
                config.setProperty(PROP_VIDEO_DEVICE, videoCaptureDevice
                    .getName());

            firePropertyChange(VIDEO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    /**
     * Sets the device which is to be used by this
     * <code>DeviceConfiguration</code> for audio capture.
     *
     * @param device a <code>CaptureDeviceInfo</code> describing the device to
     *            be used by this <code>DeviceConfiguration</code> for audio
     *            capture
     */
    public void setAudioCaptureDevice(CaptureDeviceInfo device)
    {
        if (audioCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = audioCaptureDevice;

            audioCaptureDevice = device;

            ConfigurationService config =
                MediaActivator.getConfigurationService();
            config.setProperty(PROP_AUDIO_DEVICE_IS_DISABLED,
                audioCaptureDevice == null);
            if (audioCaptureDevice != null)
                config.setProperty(PROP_AUDIO_DEVICE, audioCaptureDevice
                    .getName());

            firePropertyChange(AUDIO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    /**
     * Enable or disable Audio stream transmission.
     *
     * @return true if audio capture is supported and false otherwise.
     */
    public boolean isAudioCaptureSupported()
    {
        return this.audioCaptureDevice != null;
    }

    /**
     * Enable or disable Video stream transmission.
     *
     * @return true if audio capture is supported and false otherwise.
     */
    public boolean isVideoCaptureSupported()
    {
        return this.videoCaptureDevice != null;
    }
}
