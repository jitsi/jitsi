/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)DirectSoundAuto.java 1.3 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.device;

import java.util.*;
import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.util.*;

public class DirectSoundAuto {

    private static final Logger logger = Logger.getLogger(DirectSoundAuto.class);

    private static final String detectClass = "com.sun.media.protocol.dsound.DSound";
    CaptureDeviceInfo[] devices = null;

    public static void main(String[] args) {
        new DirectSoundAuto();
        System.exit(0);
    }

    private boolean supports(AudioFormat af) {
        try {
            com.sun.media.protocol.dsound.DSound ds;
            ds = new com.sun.media.protocol.dsound.DSound(af, 1024);
            ds.open();
            ds.close();
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked") //legacy JMF code.
    public DirectSoundAuto() {
        boolean supported = false;
        // instance JavaSoundDetector to check is javasound's capture is availabe
        try {
            Class.forName(detectClass);
            supported = true;
        } catch (Throwable t) {
            supported = false;
            // t.printStackTrace();
        }

        logger.info("DirectSound Capture Supported = " + supported);

        if (supported) {
            // It's there, start to register JavaSound with CaptureDeviceManager
            Vector<CaptureDeviceInfo> devices
                = (Vector<CaptureDeviceInfo>)CaptureDeviceManager
                    .getDeviceList(null).clone();

            // remove the old direct sound capturers
            String name;
            Enumeration<CaptureDeviceInfo> enumeration = devices.elements();
            while (enumeration.hasMoreElements()) {
                CaptureDeviceInfo cdi = enumeration.nextElement();
                name = cdi.getName();
                if (name.startsWith(com.sun.media.protocol.dsound.DataSource.NAME))
                    CaptureDeviceManager.removeDevice(cdi);
            }
            int LE = AudioFormat.LITTLE_ENDIAN;
            int SI = AudioFormat.SIGNED;
            int US = AudioFormat.UNSIGNED;
            int UN = AudioFormat.NOT_SPECIFIED;
            float [] Rates = new float[] {
                    48000, 44100, 32000, 22050, 16000, 11025, 8000
            };
            Vector<AudioFormat> formats = new Vector<AudioFormat>(4);
            for (int rateIndex = 0; rateIndex < Rates.length; rateIndex++) {
                float rate = Rates[rateIndex];
                AudioFormat af;
                af = new AudioFormat(AudioFormat.LINEAR, rate, 16, 2, LE, SI);
                if (supports(af)) formats.addElement(af);
                af = new AudioFormat(AudioFormat.LINEAR, rate, 16, 1, LE, SI);
                if (supports(af)) formats.addElement(af);
                af = new AudioFormat(AudioFormat.LINEAR, rate, 8, 2, UN, US);
                if (supports(af)) formats.addElement(af);
                af = new AudioFormat(AudioFormat.LINEAR, rate, 8, 1, UN, US);
                if (supports(af)) formats.addElement(af);
            }

            AudioFormat [] formatArray = new AudioFormat[formats.size()];
            for (int fa = 0; fa < formatArray.length; fa++)
                formatArray[fa] = formats.elementAt(fa);

            CaptureDeviceInfo cdi = new CaptureDeviceInfo(
                    com.sun.media.protocol.dsound.DataSource.NAME,
                    new MediaLocator("dsound://"),
                    formatArray);
            CaptureDeviceManager.addDevice(cdi);
            try {
                CaptureDeviceManager.commit();
                logger.info("DirectSoundAuto: Committed ok");
            } catch (java.io.IOException ioe) {
                logger.error("DirectSoundAuto: error committing cdm");
            }
        }
    }
}
