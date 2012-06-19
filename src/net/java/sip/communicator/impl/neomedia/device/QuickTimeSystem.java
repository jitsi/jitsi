/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.quicktime.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * Discovers and registers QuickTime/QTKit capture devices with JMF.
 *
 * @author Lyubomir Marinov
 */
public class QuickTimeSystem
    extends DeviceSystem
{

    /**
     * The <tt>Logger</tt> used by the <tt>QuickTimeSystem</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(QuickTimeSystem.class);

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying QuickTime/QTKit
     * capture devices.
     */
    private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_QUICKTIME;

    /**
     * Initializes a new <tt>QuickTimeSystem</tt> instance which discovers and
     * registers QuickTime/QTKit capture devices with JMF.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering QuickTime/QTKit capture defines with JMF
     */
    public QuickTimeSystem()
        throws Exception
    {
        super(MediaType.VIDEO, LOCATOR_PROTOCOL);
    }

    protected void doInitialize()
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
                for(QTFormatDescription f : inputDevice.formatDescriptions())
                {
                    logger.info(
                            "Webcam available resolution for "
                                + inputDevice.localizedDisplayName()
                                + ":"
                                + f.sizeForKey(
                                        QTFormatDescription
                                            .VideoEncodedPixelsSizeAttribute));
                }
            }

            CaptureDeviceManager.addDevice(device);
            captureDeviceInfoIsAdded = true;
            if (logger.isDebugEnabled())
                logger.debug("Added CaptureDeviceInfo " + device);
        }
        if (captureDeviceInfoIsAdded
                && !MediaServiceImpl.isJmfRegistryDisableLoad())
            CaptureDeviceManager.commit();
    }
}
