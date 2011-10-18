/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.lang.reflect.Method;

/**
 * Auto-detection of FMJ audio capture devices.
 * 
 * @author Ken Larson
 */
public class FMJJavaSoundAuto
{
    /**
     * Creates an instance of FMJJavaSoundAuto and auto-detects FMJ audio
     * capture devices.
     * 
     * @throws java.lang.Exception if FMJ is not present in the classpath or if 
     * detection fails for some other reason.
     */
    public FMJJavaSoundAuto() throws Exception
    {
        // Done using reflection to avoid compile-time dependency on FMJ:
        //new net.sf.fmj.media.cdp.javasound.CaptureDevicePlugger()
        //.addCaptureDevices();
        final Class<?> clazz = Class.forName(
            "net.sf.fmj.media.cdp.javasound.CaptureDevicePlugger");
        final Method addCaptureDevices = clazz.getMethod("addCaptureDevices");
        final Object captureDevicePlugger = clazz.newInstance();
        addCaptureDevices.invoke(captureDevicePlugger);
    }
}
