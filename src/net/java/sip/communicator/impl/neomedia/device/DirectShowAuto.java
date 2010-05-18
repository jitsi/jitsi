/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.directshow.*;

/**
 * Discovers and registers DirectShow video capture devices with JMF.
 *
 * @author Sebastien Vincent
 */
public class DirectShowAuto
{
    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying QuickTime/QTKit
     * capture devices.
     */
    public static final String LOCATOR_PROTOCOL = "directshow";

    /**
     * Constructor. Discover and register DirectShow capture devices
     * with JMF.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering DirectShow capture defines with JMF
     */
    public DirectShowAuto() throws Exception
    {
        DSManager manager = DSManager.getInstance();
        DSCaptureDevice devices[] = null;
        boolean captureDeviceInfoIsAdded = false;

        /* get devices */
        devices = manager.getCaptureDevices();

        if(devices == null || devices.length == 0)
        {
            throw new Exception("no devices!");
        }

        for(int i = 0 ; i < devices.length ; i++)
        {
            CaptureDeviceInfo device
                = new CaptureDeviceInfo(devices[i].getName(),
                        new MediaLocator(LOCATOR_PROTOCOL + ':' + devices[i].
                            getName()),
                        new Format[]
                                {
                                    new AVFrameFormat(FFmpeg.PIX_FMT_RGB24),
                                    new AVFrameFormat(FFmpeg.PIX_FMT_RGB32)
                                });

            CaptureDeviceManager.addDevice(device);
            captureDeviceInfoIsAdded = true;
        }

        if (captureDeviceInfoIsAdded)
            CaptureDeviceManager.commit();

        devices = null;
        DSManager.dispose();
    }
}

