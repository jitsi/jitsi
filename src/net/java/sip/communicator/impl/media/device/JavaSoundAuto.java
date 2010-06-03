/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)JavaSoundAuto.java   1.2 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.device;

import java.util.*;
import javax.media.*;

import net.java.sip.communicator.util.*;

/**
 * Detects javasound and registers capture devices.
 * @author damencho
 */
public class JavaSoundAuto {

    private static final Logger logger = Logger.getLogger(JavaSoundAuto.class);

    private static final String detectClass =
            "net.java.sip.communicator.impl.media.device.JavaSoundDetector";
    CaptureDeviceInfo[] devices = null;

    public static void main(String[] args) {
        new JavaSoundAuto();
        System.exit(0);
    }

    @SuppressWarnings("unchecked") // JMF legacy code
    public JavaSoundAuto() {
        boolean supported = false;
        // instance JavaSoundDetector to check is javasound's capture is availabe
        try {
            Class<?> cls = Class.forName(detectClass);
            JavaSoundDetector detect = (JavaSoundDetector)cls.newInstance();
            supported = detect.isSupported();
        } catch (Throwable thr) {
            supported = false;
            logger.error("Failed detecting java sound audio", thr);
        }

        if (logger.isInfoEnabled())
            logger.info("JavaSound Capture Supported = " + supported);

        if (supported) {
            // It's there, start to register JavaSound with CaptureDeviceManager
            Vector<CaptureDeviceInfo> devices
                = CaptureDeviceManager.getDeviceList(null);
            devices = (Vector<CaptureDeviceInfo>) devices.clone();

            // remove the old javasound capturers
            String name;
            Enumeration<CaptureDeviceInfo> enumeration = devices.elements();
            while (enumeration.hasMoreElements()) {
                CaptureDeviceInfo cdi = enumeration.nextElement();
                name = cdi.getName();
                if (name.startsWith("JavaSound"))
                    CaptureDeviceManager.removeDevice(cdi);
            }

            // collect javasound capture device info from JavaSoundSourceStream
            // and register them with CaptureDeviceManager
            CaptureDeviceInfo[] cdi
                =  com.sun.media.protocol.javasound.JavaSoundSourceStream
                    .listCaptureDeviceInfo();
            if ( cdi != null ){
                for (int i = 0; i < cdi.length; i++)
                    CaptureDeviceManager.addDevice(cdi[i]);
                try {
                    CaptureDeviceManager.commit();
                    if (logger.isInfoEnabled())
                        logger.info("JavaSoundAuto: Committed ok");
                } catch (java.io.IOException ioe) {
                    logger.error("JavaSoundAuto: error committing cdm");
                }
            }

            // now add it as available audio system to DeviceConfiguration
            DeviceConfiguration.addAudioSystem(
                DeviceConfiguration.AUDIO_SYSTEM_JAVASOUND);
        }
    }
}
