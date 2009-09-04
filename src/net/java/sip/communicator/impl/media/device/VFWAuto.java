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
package net.java.sip.communicator.impl.media.device;

import java.util.*;
import javax.media.*;
import com.sun.media.protocol.vfw.*;
import net.java.sip.communicator.util.*;

/**
 * SIP Communicator modifications
 * @author Emil Ivov
 */
public class VFWAuto
{
    private static final Logger logger
        = Logger.getLogger(VFWAuto.class);

    /**
     * Removes from the CaptureDeviceManager all currently detected vfw devices
     * and  runs a new detection loop to rediscover those that are currently
     * available.
     *
     * @return the number of devices detected.
     */
    @SuppressWarnings("unchecked") // jmf legacy code
    public int autoDetectDevices()
    {
        Vector<CaptureDeviceInfo> devices
            = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration<CaptureDeviceInfo> devicesEnum = devices.elements();

        while (devicesEnum.hasMoreElements())
        {
            CaptureDeviceInfo cdi = devicesEnum.nextElement();
            String name = cdi.getName();
            if (name.startsWith("vfw:"))
                CaptureDeviceManager.removeDevice(cdi);
        }

        int nDevices = 0;
        for (int i = 0; i < 10; i++)
        {
            String name = VFWCapture.capGetDriverDescriptionName(i);
            if (name != null && name.length() > 1)
            {
                logger.debug("Found device " + name);
                logger.debug("Querying device. Please wait...");
                com.sun.media.protocol.vfw.VFWSourceStream.autoDetect(i);
                nDevices++;
            }
        }

        return nDevices;
    }

    public static void main(String [] args)
    {
        VFWAuto vfwAuto = new VFWAuto();
        vfwAuto.autoDetectDevices();
        System.exit(0);
    }
}

