/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.lang.reflect.*;
import net.java.sip.communicator.util.*;

/**
 * FMJ auto-detect of CIVIL video capture devices.
 * 
 * @author Ken Larson
 */
public class FMJCivilVideoAuto
{
    /**
     * The <tt>Logger</tt> used by the <tt>FMJCivilVideoAuto</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(FMJCivilVideoAuto.class);

    /**
     * Creates an instance of FMJCivilVideoAuto and auto-detects CIVIL video 
     * capture devices.
     * 
     * @throws java.lang.Exception if FMJ is not present in the classpath or if 
     * detection fails for some other reason.
     */
    public FMJCivilVideoAuto() throws Exception
    {
        if(logger.isInfoEnabled())
            logger.info("Start detecting Civil Capture Devices");

        // Done using reflection to avoid compile-time dependency on FMJ:
        //new net.sf.fmj.media.cdp.civil.CaptureDevicePlugger().addCaptureDevices();
        final Class<?> clazz 
            = Class.forName("net.sf.fmj.media.cdp.civil.CaptureDevicePlugger");
        final Method addCaptureDevices = clazz.getMethod("addCaptureDevices");
        final Object captureDevicePlugger = clazz.newInstance();
        addCaptureDevices.invoke(captureDevicePlugger);

        if(logger.isInfoEnabled())
            logger.info("Finish detecting Civil Capture Devices");
    }
}
