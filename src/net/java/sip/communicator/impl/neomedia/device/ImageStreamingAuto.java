/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.List;
import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.*;
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
        String name = "Desktop Streaming";
        List<ScreenDevice> screens = NeomediaActivator.getMediaServiceImpl().
            getAvailableScreenDevices();
        int i = 0;
        boolean multipleMonitorOneScreen = false;
        Dimension screenSize = null;

        /* on Linux, multiple monitors may result in only one
         * X display (:0.0) that combine those monitors
         */
        if(OSUtils.IS_LINUX)
        {
            Dimension size = new Dimension(0, 0);

            for(ScreenDevice screen : screens)
            {
                size.width += screen.getSize().width;
                size.height += screen.getSize().height;
            }

            try
            {
                screenSize = Toolkit.getDefaultToolkit().getScreenSize();

                if(screenSize.width == size.width ||
                        screenSize.height == size.height)
                {
                    multipleMonitorOneScreen = true;
                }
            }
            catch(Exception e)
            {
            }
        }

        for(ScreenDevice screen : screens)
        {
            Dimension size = screenSize != null ? screenSize : screen.getSize();

            Format formats[]= new Format[]
                    {
                        new AVFrameFormat(
                                size,
                                Format.NOT_SPECIFIED,
                                FFmpeg.PIX_FMT_ARGB,
                                Format.NOT_SPECIFIED),
                        new RGBFormat(
                                size, // size
                                Format.NOT_SPECIFIED, // maxDataLength
                                Format.byteArray, // dataType
                                Format.NOT_SPECIFIED, // frameRate
                                32, // bitsPerPixel
                                2 /* red */, 3 /* green */,  4 /* blue */)
                    };

            CaptureDeviceInfo devInfo
                = new CaptureDeviceInfo(
                        name + " " + i,
                        new MediaLocator(
                            ImageStreamingUtils.LOCATOR_PROTOCOL + ":" + i),
                        formats);
        
            /* add to JMF device manager */
            CaptureDeviceManager.addDevice(devInfo);
            i++;

            if(multipleMonitorOneScreen)
            {
                break;
            }
        }

        CaptureDeviceManager.commit();
    }
}
