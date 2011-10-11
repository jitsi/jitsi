/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.directshow.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.directshow.*;
import net.java.sip.communicator.util.*;

/**
 * Discovers and registers DirectShow video capture devices with JMF.
 *
 * @author Sebastien Vincent
 */
public class DirectShowAuto
{
    /**
     * The <tt>Logger</tt>.
     */
    private static final Logger logger = Logger.getLogger(DirectShowAuto.class);

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
        if(logger.isInfoEnabled())
            logger.info("Start detecting DirectShow capture devices");

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
            DSFormat fmt = devices[i].getFormat();
            long pixelFormat = fmt.getPixelFormat();
            Format format = null;
            int ffmpegPixFmt = (int)DataSource.getFFmpegPixFmt(pixelFormat);

            if(ffmpegPixFmt != FFmpeg.PIX_FMT_NONE)
            {
                format = new AVFrameFormat(ffmpegPixFmt, (int)pixelFormat);
            }
            else
            {
                logger.warn("No support for this webcam: " +
                        devices[i].getName() + "(format " + pixelFormat +
                        " not supported)");
                continue;
            }

            if(logger.isInfoEnabled())
            {
                DSFormat[] fs = devices[i].getSupportedFormats();
                for(DSFormat f : fs)
                {
                    if(f.getWidth() != 0 && f.getHeight() != 0)
                        logger.info("Webcam available resolution for " +
                            devices[i].getName()
                                + ":" + f.getWidth() + "x" + f.getHeight());
                }
            }

            CaptureDeviceInfo device
                = new CaptureDeviceInfo(devices[i].getName(),
                        new MediaLocator(LOCATOR_PROTOCOL + ':' + devices[i].
                            getName()),
                        new Format[]
                        {
                            format,
                        });

            if(logger.isInfoEnabled())
                logger.info("Found[" + i + "]: " + device.getName());

            CaptureDeviceManager.addDevice(device);
            captureDeviceInfoIsAdded = true;
        }

        if (captureDeviceInfoIsAdded)
            CaptureDeviceManager.commit();

        devices = null;
        DSManager.dispose();

        if(logger.isInfoEnabled())
            logger.info("Finish detecting DirectShow capture devices");
    }
}

