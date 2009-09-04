/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)SunVideoAuto.java    1.8 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.device;

import java.io.*;
import java.util.*;
import javax.media.*;
import javax.media.format.*;

import java.awt.*;

import net.java.sip.communicator.util.*;
import com.sun.media.protocol.sunvideo.*;


public class SunVideoAuto
{

    private static final Logger logger = Logger.getLogger(SunVideoAuto.class);

    private static String DEVICE_PREFIX = "/dev/rtvc";
    private static String PROTOCOL = "sunvideo";
    private static String LOCATOR_PREFIX = PROTOCOL + "://";
    CaptureDeviceInfo [] devices = null;
    int currentID = -1;

    XILCapture xilCap;

    Vector<Format> formats = null;

    int [] ports = { 1, 2, 0 };     // most likely ports for a device

    int [] scales = { 2, 4, 1 };    // supported scales / sizes

    /**
     * Default constructor. Does nothing.
     */
    public SunVideoAuto()
    {
    }

    /**
     * Removes from the CaptureDeviceManager all currently detected vfw devices
     * and  runs a new detection loop to rediscover those that are currently
     * available.
     *
     * @return the number of devices detected.
     */
    @SuppressWarnings("unchecked") //legacy JMF code.
    public int autoDetectDevices()
    {
        Vector<CaptureDeviceInfo> devices
            = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration<CaptureDeviceInfo> enumeration = devices.elements();
        while (enumeration.hasMoreElements()) {
            CaptureDeviceInfo cdi = enumeration.nextElement();
            String devName = cdi.getLocator().getProtocol();
            if (devName.equals(PROTOCOL))
                CaptureDeviceManager.removeDevice(cdi);
        }

        int nDevices = 0;
        for (int i = 0; i < 7; i++) {
            File fl = new File(DEVICE_PREFIX + i);
            if (fl.exists()) {
                doDevice(i);
                nDevices++;
            }
        }
        try {
            CaptureDeviceManager.commit();
        } catch (java.io.IOException ioe) {
            logger.error("SunVideoAuto: error committing cdm", ioe);
            return 0;
        }

        return nDevices;
    }

    private void addFormat(Format fin)
    {
        Enumeration<Format> enumeration = formats.elements();
        while (enumeration.hasMoreElements()) {
            Format format = enumeration.nextElement();
            if (format.equals(fin))
                return;
        }

        //System.err.println("New format = " + fin);
        formats.addElement(fin);
    }

    private void doDevice(int index)
    {

        xilCap = new XILCapture(null);
        formats = new Vector<Format>();
        //boolean gotPort = false;

        if (!xilCap.connect(index)) {
            dummyDevice(index);
            return;
        }


        for (int i = 0; i < ports.length; i++) {
            if (xilCap.setPort(ports[i])) {
                getJpegFormats();
                getRGBFormats();
            }
        }
        xilCap.disconnect();

        if (formats.size() > 0)
            addDevice(index);
        else
            dummyDevice(index);
    }


    private void getRGBFormats()
    {
        if (!xilCap.setCompress("RGB"))
            return;
        for (int i = 0; i < scales.length; i++) {
            xilCap.setScale(scales[i]);
            // To get the real values, start the device
            if (xilCap.start()) {
                Dimension size = new Dimension(xilCap.getWidth(),
                        xilCap.getHeight());
                int stride = xilCap.getLineStride();
                int maxbuf = stride * size.width;
                addFormat(new RGBFormat(size, maxbuf, byte[].class,
                        15f,
                        24,
                        3, 2, 1, 3, stride,
                        Format.FALSE,
                        Format.NOT_SPECIFIED));
            }
            xilCap.stop();
        }
    }

    private void getJpegFormats() {
        if (!xilCap.setCompress("Jpeg"))
            return;
        for (int i = 0; i < scales.length; i++) {
            xilCap.setScale(scales[i]);
            // To get the real values, start the device
            if (xilCap.start()) {
                Dimension size = new Dimension(xilCap.getWidth(),
                        xilCap.getHeight());
                // approximate the max for high quality
                int maxbuf = 3 * size.width * size.height;
                addFormat(new VideoFormat(VideoFormat.JPEG, size, maxbuf,
                        byte[].class, 15f));
            }
            xilCap.stop();
        }
    }

    private void dummyDevice(int index)
    {
        // Can't get to the device, use the barest formats
        addFormat(new VideoFormat(VideoFormat.JPEG));
        addFormat(new RGBFormat());
        addDevice(index);
    }


    private void addDevice(int index)
    {

        String name = "SunVideo device " + index;
        String locator = LOCATOR_PREFIX + index;

        Format [] farray = new Format[formats.size()];
        Enumeration<Format> enumeration = formats.elements();

        int i = 0;
        while (enumeration.hasMoreElements()) {
            Format format = enumeration.nextElement();
            farray[i++] = format;
        }

        CaptureDeviceInfo cdi = new CaptureDeviceInfo(name,
                new MediaLocator(locator), farray);
        CaptureDeviceManager.addDevice(cdi);
    }

    public static void main(String [] args)
    {
        SunVideoAuto sunVideoAuto = new SunVideoAuto();
        sunVideoAuto.autoDetectDevices();
        System.exit(0);
    }
}

