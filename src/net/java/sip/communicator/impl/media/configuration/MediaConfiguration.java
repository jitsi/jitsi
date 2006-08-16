/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.configuration;

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
 */
public class MediaConfiguration
{
    private Logger logger = Logger.getLogger(MediaConfiguration.class);
    
    private Object syncRoot_Config = new Object();
    
    /**
     * The configuration service to use when retrieving conf property values
     */
    private ConfigurationService configurationService = null;

    /**
     * Our configuration listener.
     */
    private ConfigurationListener configurationListener =
        new ConfigurationListener();
    
    /**
     * Audio and Video transmission
     */
    private boolean audioTransmission = true;
    private boolean videoTransmission = true;
    private boolean audioReception = true;
    private boolean videoReception = true;
    
    /**
     * Capture devices
     */
    private CaptureDeviceInfo audioCaptureDevice = null;
    private CaptureDeviceInfo videoCaptureDevice = null;
//    private DataSource avDataSource = null;
    
    /**
     * Default constructor.
     */
    public MediaConfiguration() {
        JMFInit.start();
        detectConfiguredCaptureDevices();
    }
    
    /**
     * Set the configuration service.
     *
     * @param configurationService
     */
    public void setConfigurationService(ConfigurationService configurationService) {
        synchronized(this.syncRoot_Config) {
            this.configurationService = configurationService;
            logger.debug("New configuration service registered.");
        }
        // TODO add a list of proporties to listen to
        this.configurationService.addPropertyChangeListener(configurationListener);
    }

    /**
     * Remove a configuration service.
     *
     * @param configurationService
     */
    public void unsetConfigurationService(ConfigurationService configurationService) {
        synchronized(this.syncRoot_Config) {
            if (this.configurationService == configurationService) {
                this.configurationService = null;
                logger.debug("Configuration service unregistered.");
            }
        }
    }
    
    /**
     * Detects capture devices configured through JMF and disable audio
     * and/or video transmission if none were found.
     * Stores found devices in audioCaptureDevice and videoCaptureDevice.
     */
    private void detectConfiguredCaptureDevices()
    {
        logger.info("Scanning for configured Audio Devices.");
        Vector audioCaptureDevices = CaptureDeviceManager.getDeviceList(new
                AudioFormat(AudioFormat.LINEAR, 44100, 16, 1));
        if (audioCaptureDevices.size() < 1) {
            logger.error("No Audio Device was found.");
            audioCaptureDevice = null;
            setAudioTransmission(false);
        }
        else {
            audioCaptureDevice = (CaptureDeviceInfo) audioCaptureDevices.get(0);
            logger.info("Found " + audioCaptureDevice.getName() +" as an audio capture device.");
        }
        
        logger.info("Scanning for configured Video Devices.");
        Vector videoCaptureDevices = CaptureDeviceManager.getDeviceList(new
                VideoFormat(VideoFormat.RGB));
        if (videoCaptureDevices.size() > 0) {
            videoCaptureDevice = (CaptureDeviceInfo) videoCaptureDevices.get(0);
            logger.info("Found " + videoCaptureDevice.getName() + " as an RGB Video Device.");
        }
        // no RGB camera found. And what about YUV ?
        else
        {
            videoCaptureDevices = CaptureDeviceManager.getDeviceList(new
                    VideoFormat(VideoFormat.YUV));
            if (videoCaptureDevices.size() > 0) {
                videoCaptureDevice = (CaptureDeviceInfo) videoCaptureDevices.get(0);
                logger.info("Found " + videoCaptureDevice.getName() + " as an YUV Video Device.");
            }
            else {
                logger.error("No Video Device was found.");
                videoCaptureDevice = null;
                setVideoTransmission(false);
            }
        }
    }
    
    public CaptureDeviceInfo getAudioCaptureDevice() {
        return audioCaptureDevice;
    }
    
    public CaptureDeviceInfo getVideoCaptureDevice() {
        return videoCaptureDevice;
    }

    /**
     * Enable or disable Audio stream transmission.
     * @param enable whereas Audio stream transmission must be enabled or disabled
     */
    protected void setAudioTransmission(boolean enable) {
        logger.info(enable? "Enabling":"Disabling" 
            + " Audio transmission.");
        this.audioTransmission = enable;
    }

    /**
     * Enable or disable Video stream transmission.
     * @param enable whereas Video stream transmission must be enabled or disabled
     */
    protected void setVideoTransmission(boolean enable) {
        logger.info(enable? "Enabling":"Disabling"
            + " Video transmission.");
        this.videoTransmission = enable;
    }

    /**
     * Enable or disable Audio stream reception.
     * @param enable whereas Audio stream reception must be enabled or disabled
     */
    protected void setAudioReception(boolean enable) {
        logger.info(enable? "Enabling":"Disabling"
            + " Audio reception.");
        this.audioReception = enable;
    }

    /**
     * Enable or disable Video stream reception.
     * @param enable whereas Video stream reception must be enabled or disabled
     */
    protected void setVideoReception(boolean enable) {
        logger.info(enable? "Enabling":"Disabling"
            + " Video reception.");
        this.videoReception = enable;
    }
}
