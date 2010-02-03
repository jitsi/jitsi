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
     *
     * @throws Exception if problem when adding capture devices
     */
    public ImageStreamingAuto() throws Exception
    {
        String name = "Experimental desktop streaming";
        CaptureDeviceInfo devInfo
            = new CaptureDeviceInfo(
                    name,
                    new MediaLocator(
                            ImageStreamingUtils.LOCATOR_PROTOCOL + ":" + name),
                    DataSource.getFormats());
            
        /* add to JMF device manager */
        CaptureDeviceManager.addDevice(devInfo);
        CaptureDeviceManager.commit();
    }
}
