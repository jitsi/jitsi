/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.List;
import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.*;

/**
 * Add ImageStreaming capture device.
 *
 * @author Sebastien Vincent
 */
public class ImgStreamingSystem
    extends DeviceSystem
{
    /**
     * The locator protocol used when creating or parsing
     * <tt>MediaLocator</tt>s.
     */
    public static final String LOCATOR_PROTOCOL = "imgstreaming";

    /**
     * Add capture devices.
     *
     * @throws Exception if problem when adding capture devices
     */
    public ImgStreamingSystem()
        throws Exception
    {
        super(MediaType.VIDEO, LOCATOR_PROTOCOL, FEATURE_REINITIALIZE);
    }

    protected void doInitialize()
        throws Exception
    {
        String name = "Desktop Streaming";
        List<ScreenDevice> screens
            = NeomediaActivator.getMediaServiceImpl()
                    .getAvailableScreenDevices();
        int i = 0;
        boolean multipleMonitorOneScreen = false;
        Dimension screenSize = null;

        /*
         * On Linux, multiple monitors may result in a single X display (:0.0)
         * which combines them.
         */
        if(OSUtils.IS_LINUX)
        {
            Dimension size = new Dimension(0, 0);

            for(ScreenDevice screen : screens)
            {
                Dimension s = screen.getSize();

                size.width += s.width;
                size.height += s.height;
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
            Format formats[]
                = new Format[]
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
                        new MediaLocator(LOCATOR_PROTOCOL + ":" + i),
                        formats);

            CaptureDeviceManager.addDevice(devInfo);
            i++;

            if(multipleMonitorOneScreen)
                break;
        }

        if (!NeomediaActivator.isJmfRegistryDisableLoad())
            CaptureDeviceManager.commit();
    }
}
