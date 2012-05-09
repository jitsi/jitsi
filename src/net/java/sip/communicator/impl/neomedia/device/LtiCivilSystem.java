/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.lang.reflect.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * FMJ auto-detect of CIVIL video capture devices.
 * 
 * @author Ken Larson
 */
public class LtiCivilSystem
    extends DeviceSystem
{
    private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_CIVIL;

    /**
     * Creates an instance of LtiCivilSystem and auto-detects CIVIL video 
     * capture devices.
     * 
     * @throws java.lang.Exception if FMJ is not present in the classpath or if 
     * detection fails for some other reason.
     */
    public LtiCivilSystem()
        throws Exception
    {
        super(MediaType.VIDEO, LOCATOR_PROTOCOL);
    }

    protected void doInitialize()
        throws Exception
    {
        // Done using reflection to avoid compile-time dependency on FMJ.
        Class<?> clazz 
            = Class.forName("net.sf.fmj.media.cdp.civil.CaptureDevicePlugger");
        Method addCaptureDevices = clazz.getMethod("addCaptureDevices");
        Object captureDevicePlugger = clazz.newInstance();

        addCaptureDevices.invoke(captureDevicePlugger);
    }
}
