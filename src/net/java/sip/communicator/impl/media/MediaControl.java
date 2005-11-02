/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.io.IOException;

import javax.media.CaptureDeviceInfo;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.protocol.CaptureDevice;
import javax.media.protocol.DataSource;

import net.java.sip.communicator.impl.media.configuration.MediaConfiguration;
import net.java.sip.communicator.util.Logger;


/**
 * This class is intended to provide a generic way to control media package.
 *
 * @author Martin Andre
 */
public class MediaControl
{
    private Logger logger = Logger.getLogger(MediaConfiguration.class);
    
    /**
     * Our configuration helper.
     */
    private MediaConfiguration mediaConfiguration = null;
    
    /**
     * Capture devices
     */
    private CaptureDevice audioCaptureDevice = null;
    private CaptureDevice videoCaptureDevice = null;
    private DataSource avDataSource = null;
    
    public MediaControl(MediaConfiguration mediaConfiguration) 
    {
        this.mediaConfiguration = mediaConfiguration;
    }
    
    protected void openCaptureDevices()
    {
        // Init Capture devices
        DataSource audioDataSource = null;
        DataSource videoDataSource = null;
        CaptureDeviceInfo audioDeviceInfo = null;
        CaptureDeviceInfo videoDeviceInfo = null;
        
        // audio device
        audioDeviceInfo = mediaConfiguration.getAudioCaptureDevice();
        if (audioDeviceInfo != null) {
            audioDataSource = createDataSource(audioDeviceInfo.getLocator());
            audioCaptureDevice = (CaptureDevice) audioDataSource;
        }
        
        // video device
        videoDeviceInfo = mediaConfiguration.getVideoCaptureDevice();
        if (videoDeviceInfo != null) {
            videoDataSource = createDataSource(videoDeviceInfo.getLocator());
            videoCaptureDevice = (CaptureDevice) videoDataSource;
        }
                
        // Create the av data source
        if (audioDataSource != null && videoDataSource != null) {
            DataSource[] allDS = new DataSource[] {
                    audioDataSource,
                    videoDataSource
            };
            try {
                avDataSource = Manager.createMergingDataSource(allDS);
            }
            catch (IncompatibleSourceException exc) {
                System.out.println(
                        "Failed to create a media data source!"
                        + "Media transmission won't be enabled!");
            }
        }
        else {
            if (audioDataSource != null) {
                avDataSource = audioDataSource;
            }
            if (videoDataSource != null) {
                avDataSource = videoDataSource;
            }
        }
        
        // avDataSource may be null
    }
    
    protected void closeCaptureDevices()
    {
        try {
            avDataSource.stop();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected DataSource getDataSource() {
        return avDataSource;
    }
    
    protected DataSource createDataSource(MediaLocator locator)
    {
        try {
            logger.info("Creating datasource for:"
                    + locator != null
                    ? locator.toExternalForm()
                            : "null");
            return Manager.createDataSource(locator);
        }
        catch (NoDataSourceException ex) {
            // The failure only concens us
            logger.error("Could not create data source for " +
                    locator.toExternalForm());
            return null;
        }
        catch (IOException ex) {
            // The failure only concens us
            logger.error("Could not create data source for " +
                    locator.toExternalForm());
            return null;
        }
    }
    
    protected void startCapture(CaptureDevice captureDevice)
    {
        
    }
    
    protected void stopCapture(CaptureDevice captureDevice)
    {
        
    }
}
