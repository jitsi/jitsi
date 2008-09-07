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

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;



/**
 * This class aims to provide a simple configuration interface for JMF.
 * It retrieves stored configuration when started or listens to
 * ConfigurationEvent for property changes and configures the JMF accordingly.
 *
 * @author Martin Andre
 * @author Emil Ivov
 */
public class DeviceConfiguration
{
    private Logger logger = Logger.getLogger(DeviceConfiguration.class);

    private Object syncRoot_Config = new Object();

    /**
     * Our configuration listener.
     */
    private ConfigurationListener configurationListener =
        new ConfigurationListener();

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
        //these seem to be throwing exceptions every now and then so we'll
        //blindly catch them for now
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
     * Detects capture devices configured through JMF and disable audio
     * and/or video transmission if none were found.
     * Stores found devices in audioCaptureDevice and videoCaptureDevice.
     */
    private void extractConfiguredCaptureDevices()
    {
        logger.info("Scanning for configured Audio Devices.");
        Vector audioCaptureDevices = CaptureDeviceManager.getDeviceList(new
                AudioFormat(AudioFormat.LINEAR, 44100, 16,
                1));//1 means 1 channel for mono
        if (audioCaptureDevices.size() < 1) {
            logger.warn("No Audio Device was found.");
            audioCaptureDevice = null;
        }
        else {
            logger.debug("Found " + audioCaptureDevices.size()
                         + " capture devices: " + audioCaptureDevices);
            audioCaptureDevice = (CaptureDeviceInfo) audioCaptureDevices.get(0);
            logger.info("Found " + audioCaptureDevice.getName()
                        +" as an audio capture device.");
        }

        logger.info("Scanning for configured Video Devices.");
        Vector videoCaptureDevices = CaptureDeviceManager.getDeviceList(new
                VideoFormat(VideoFormat.RGB));
        if (videoCaptureDevices.size() > 0) {
            videoCaptureDevice = (CaptureDeviceInfo) videoCaptureDevices.get(0);
            logger.info("Found " + videoCaptureDevice.getName()
                        + " as an RGB Video Device.");
        }
        // no RGB camera found. And what about YUV ?
        else
        {
            videoCaptureDevices = CaptureDeviceManager.getDeviceList(new
                    VideoFormat(VideoFormat.YUV));
            if (videoCaptureDevices.size() > 0) {
                videoCaptureDevice
                    = (CaptureDeviceInfo) videoCaptureDevices.get(0);
                logger.info("Found " + videoCaptureDevice.getName()
                            + " as an YUV Video Device.");
            }
            else {
                logger.info("No Video Device was found.");
                videoCaptureDevice = null;
            }
        }
    }

    /**
     * Returns a device that we could use for audio capture.
     * @return the CaptureDeviceInfo of a device that we could use for audio
     * capture.
     */
    public CaptureDeviceInfo getAudioCaptureDevice()
    {
        return audioCaptureDevice;
    }

    /**
     * Returns a device that we could use for video capture.
     * @return the CaptureDeviceInfo of a device that we could use for video
     * capture.
     */
    public CaptureDeviceInfo getVideoCaptureDevice()
    {
        return videoCaptureDevice;
    }

    /**
     * Enable or disable Audio stream transmission.
     * @return true if audio capture is supported and false otherwise.
     */
    public boolean isAudioCaptureSupported() {
        return this.audioCaptureDevice != null;
    }

    /**
     * Enable or disable Video stream transmission.
     * @return true if audio capture is supported and false otherwise.
     */
    public boolean isVideoCaptureSupported() {
        return this.videoCaptureDevice != null;
    }

}
