/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.imgstreaming.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming.*;

/**
 * Add ImageStreaming capture device.
 * 
 * @author Sebastien Vincent
 */
public class ImageStreamingAuto
{
    /**
     * Add capture devices.
     */
    public ImageStreamingAuto()
    {
        String name = "DesktopStreaming";
        CaptureDeviceInfo devInfo = new CaptureDeviceInfo(name, 
            new MediaLocator(ImageStreamingUtils.LOCATOR_PREFIX + name),
            DataSource.getFormats());
            
        /* add to JMF device manager */
        CaptureDeviceManager.addDevice(devInfo);
    }
}

