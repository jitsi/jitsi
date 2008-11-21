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

import net.java.sip.communicator.util.*;

/**
 * This class aims to provide a simple configuration interface for JMF. It
 * retrieves stored configuration when started or listens to ConfigurationEvent
 * for property changes and configures the JMF accordingly.
 * 
 * @author Martin Andre
 * @author Emil Ivov
 * @authod Lubomir Marinov
 */
public class DeviceConfiguration
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
    private CaptureDeviceInfo videoCaptureDevice = null;

    /**
     * Default constructor.
     */
    public DeviceConfiguration()
    {
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
        logger.info("Scanning for configured Audio Devices.");
        CaptureDeviceInfo[] audioCaptureDevices =
            getAvailableAudioCaptureDevices();
        if (audioCaptureDevices.length < 1)
        {
            logger.warn("No Audio Device was found.");
            audioCaptureDevice = null;
        }
        else
        {
            logger.debug("Found " + audioCaptureDevices.length
                + " capture devices: " + audioCaptureDevices);
            audioCaptureDevice = audioCaptureDevices[0];
            logger.info("Found " + audioCaptureDevice.getName()
                + " as an audio capture device.");
        }

        logger.info("Scanning for configured Video Devices.");
        Vector videoCaptureDevices =
            CaptureDeviceManager
                .getDeviceList(new VideoFormat(VideoFormat.RGB));
        if (videoCaptureDevices.size() > 0)
        {
            videoCaptureDevice = (CaptureDeviceInfo) videoCaptureDevices.get(0);
            logger.info("Found " + videoCaptureDevice.getName()
                + " as an RGB Video Device.");
        }
        // no RGB camera found. And what about YUV ?
        else
        {
            videoCaptureDevices =
                CaptureDeviceManager.getDeviceList(new VideoFormat(
                    VideoFormat.YUV));
            if (videoCaptureDevices.size() > 0)
            {
                videoCaptureDevice =
                    (CaptureDeviceInfo) videoCaptureDevices.get(0);
                logger.info("Found " + videoCaptureDevice.getName()
                    + " as an YUV Video Device.");
            }
            else
            {
                logger.info("No Video Device was found.");
                videoCaptureDevice = null;
            }
        }
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
     * <code>DeviceConfiguration</code>, amongst which are
     * {@link #getAudioCaptureDevice()} and
     * {@link #getDefaultAudioCaptureDevice()} and represent acceptable values
     * for {@link #setDefaultAudioCaptureDevice()}
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
     * <code>DeviceConfiguration</code>, amongst which are
     * {@link #getVideoCaptureDevice()} and
     * {@link #getDefaultVideoCaptureDevice()} and represent acceptable values
     * for {@link #setDefaultVideoCaptureDevice()}
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

            firePropertyChange(AUDIO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    protected void firePropertyChange(String property, Object oldValue,
        Object newValue)
    {
        // TODO Auto-generated method stub
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
