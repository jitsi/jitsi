/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)V4LAuto.java 1.2 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.device;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.util.*;

import com.sun.media.protocol.v4l.*;

/**
 * Probes for video capture devices present on linux systems.
 * @author Emil Ivov
 */
public class V4LAuto {

    private static final Logger logger = Logger.getLogger(V4LAuto.class);

    /**
     * Removes from the CaptureDeviceManager all currently detected devices and
     * runs a new detection loop to rediscover those that are currently
     * available.
     *
     * @return the number of devices detected.
     */
    @SuppressWarnings("unchecked") //JMF legacy code
    public int autoDetectDevices()
    {
        Vector<CaptureDeviceInfo> devices
            = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration<CaptureDeviceInfo> enumeration = devices.elements();
        while (enumeration.hasMoreElements())
        {
            CaptureDeviceInfo cdi = enumeration.nextElement();
            String name = cdi.getName();
            if (name.startsWith("v4l:"))
                CaptureDeviceManager.removeDevice(cdi);
        }

        int nDevices = 0;
        for (int i = 0; i < 10; i++)
        {
            CaptureDeviceInfo cdi = autoDetect(i);
            if (cdi != null)
                nDevices++;
        }

        return nDevices;
    }
    /**
     * Runs a device query for the capture card with the specified card number
     * and returns its corresponding CaptureDeviceInfo or null if detection
     * failed or nos such card exists.
     * @param cardNo the index of the card to discover.
     * @return the CaptureDeviceInfo corresponsing to the newly discovered
     * device.
     */
    protected CaptureDeviceInfo autoDetect(int cardNo)
    {
        CaptureDeviceInfo cdi = null;
        try
        {
            cdi = new V4LDeviceQuery();

            ((V4LDeviceQuery)cdi).sendQuery(cardNo);
            if ( cdi.getFormats() != null
                 && cdi.getFormats().length > 0)
            {
                // Commit it to disk. Its a new device
                if (CaptureDeviceManager.addDevice(cdi))
                {
                    logger.info("Added device " + cdi);
                    CaptureDeviceManager.commit();
                }
            }
        }
        catch (Throwable thr)
        {
            logger.debug("No device for index "
                         + cardNo + ". "
                         + thr.getMessage());
            if (thr instanceof ThreadDeath)
                throw (ThreadDeath)thr;
        }

        return cdi;
    }

    /**
     * Test method, present for testing only.
     * @param args String[]
     */
    public static void main(String [] args)
    {
        V4LAuto auto = new V4LAuto();
        auto.autoDetectDevices();
        System.exit(0);
    }
}

