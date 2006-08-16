/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)VFWAuto.java 1.2 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.configuration;

import java.util.*;
import javax.media.*;

public class VFWAuto {

    public VFWAuto() {
        Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration enumeration = devices.elements();

        while (enumeration.hasMoreElements()) {
            CaptureDeviceInfo cdi = (CaptureDeviceInfo) enumeration.nextElement();
            String name = cdi.getName();
            if (name.startsWith("vfw:"))
                CaptureDeviceManager.removeDevice(cdi);
        }

        int nDevices = 0;
//        for (int i = 0; i < 10; i++) {
//            String name = VFWCapture.capGetDriverDescriptionName(i);
//            if (name != null && name.length() > 1) {
//                System.err.println("Found device " + name);
//                System.err.println("Querying device. Please wait...");
//                com.sun.media.protocol.vfw.VFWSourceStream.autoDetect(i);
//                nDevices++;
//            }
//        }
    }

    public static void main(String [] args) {
        VFWAuto a = new VFWAuto();
        System.exit(0);
    }
}

