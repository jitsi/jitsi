/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.quicktime.*;
import net.java.sip.communicator.util.*;

/**
 * Discovers and registers QuickTime/QTKit capture devices with JMF.
 *
 * @author Lyubomir Marinov
 */
public class QuickTimeAuto
{

    /**
     * The <tt>Logger</tt> used by the <tt>QuickTimeAuto</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(QuickTimeAuto.class);

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying QuickTime/QTKit
     * capture devices.
     */
    public static final String LOCATOR_PROTOCOL = "quicktime";

    /**
     * Initializes a new <tt>QuickTimeAuto</tt> instance which discovers and
     * registers QuickTime/QTKit capture devices with JMF.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering QuickTime/QTKit capture defines with JMF
     */
    public QuickTimeAuto()
        throws Exception
    {
        QTCaptureDevice[] inputDevices
            = QTCaptureDevice.inputDevicesWithMediaType(QTMediaType.Video);
        boolean captureDeviceInfoIsAdded = false;

        for (QTCaptureDevice inputDevice : inputDevices)
        {
            CaptureDeviceInfo device
                = new CaptureDeviceInfo(
                        inputDevice.localizedDisplayName(),
                        new MediaLocator(
                                LOCATOR_PROTOCOL
                                    + ':'
                                    + inputDevice.uniqueID()),
                        new Format[]
                                {
                                    new AVFrameFormat(FFmpeg.PIX_FMT_ARGB),
                                    new RGBFormat()
                                });

            if(logger.isInfoEnabled())
            {
                QTFormatDescription[] fs = inputDevice.formatDescriptions();
                for(QTFormatDescription f : fs)
                {
                    logger.info("Webcam available resolution for " +
                        inputDevice.localizedDisplayName()
                        + ":" + f.sizeForKey(
                        QTFormatDescription.VideoEncodedPixelsSizeAttribute));
                }
            }

            CaptureDeviceManager.addDevice(device);
            captureDeviceInfoIsAdded = true;
            if (logger.isDebugEnabled())
                logger.debug("Added CaptureDeviceInfo " + device);
        }
        if (captureDeviceInfoIsAdded)
            CaptureDeviceManager.commit();
    }
}
