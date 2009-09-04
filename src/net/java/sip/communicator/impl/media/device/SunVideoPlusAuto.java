/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)SunVideoPlusAuto.java 1.6 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.device;

import java.io.*;
import java.util.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import java.awt.*;

import com.sun.media.protocol.sunvideoplus.*;
import net.java.sip.communicator.util.*;

/**
 * SIP Communicator modifications.
 * @author Emil Ivov
 */
public class SunVideoPlusAuto
{
    private static final Logger logger
        = Logger.getLogger(SunVideoPlusAuto.class);

    private static String DEVICE_PREFIX = "/dev/o1k";
    private static String PROTOCOL = "sunvideoplus";
    private static String LOCATOR_PREFIX = PROTOCOL + "://";

    private static boolean DO_PAL = false;

    int currentID = -1;

    /**
     * Removes from the CaptureDeviceManager all currently detected sun video
     * devices and  runs a new detection loop to rediscover those that are
     * currently available.
     *
     * @return the number of devices detected.
     */
    @SuppressWarnings("unchecked") //legacy JMF code.
    public int autoDetectDevices()
    {
        /*
         * First remove any old entries
         */
        Vector<CaptureDeviceInfo> devices = (Vector) CaptureDeviceManager.
            getDeviceList(null).clone();
        Enumeration<CaptureDeviceInfo> enumeration = devices.elements();
        while (enumeration.hasMoreElements())
        {
            CaptureDeviceInfo cdi = enumeration.nextElement();
            String devName = cdi.getLocator().getProtocol();
            if (devName.equals(PROTOCOL))
                CaptureDeviceManager.removeDevice(cdi);
        }

        int nDevices = 0;
        for (int i = 0; i < 10; i++)
        {
            File fl = new File(DEVICE_PREFIX + i);
            if (fl.exists())
            {
                if (DO_PAL)
                {
                    generalDevice(i, "PAL");
                    // If generating PAL, do both
                    // Garbage collect to release the PAL datasource
                    // otherwise it sometimes hangs before completing NTSC
                    System.gc();
                    generalDevice(i, "NTSC");
                }
                else
                {
                    generalDevice(i, null);
                }
                // No longer generate specific configurations,
                // let capture preview handle selection.
                // doDevice(i);
                nDevices++;
            }
        }

        try
        {
            CaptureDeviceManager.commit();
        }
        catch (java.io.IOException ioe)
        {
            logger.error("SunVideoPlusAuto: error committing cdm", ioe);
            return 0;
        }

        return nDevices;
    }

    protected void generalDevice(int id, String signal)
    {
        // Add the general device
        javax.media.protocol.DataSource dsource = null;
        String url = LOCATOR_PREFIX + id;
        if (signal != null)
            url += "////" + signal.toLowerCase();
        try
        {
            dsource = Manager.createDataSource(new MediaLocator(url));
        }
        catch (Exception ex)
        {
        }
        if (dsource != null && dsource instanceof
            com.sun.media.protocol.sunvideoplus.DataSource)
        {
            CaptureDeviceInfo cdi = ( (CaptureDevice) dsource).
                getCaptureDeviceInfo();
            if (cdi != null)
            {
                String name = cdi.getName();
                if (signal == null)
                {
                    CaptureDeviceManager.addDevice(cdi);
                }
                else
                {
                    name = cdi.getName() + " (" + signal + ")";
                    CaptureDeviceManager.addDevice(new CaptureDeviceInfo(name,
                        cdi.getLocator(), cdi.getFormats()));
                }
                System.err.println("CaptureDeviceInfo = "
                                   + name + " "
                                   + cdi.getLocator());
            }
            dsource.disconnect();
        }
    }

    protected void doDevice(int id)
    {
        currentID = id;
        FormatSetup fd = new FormatSetup(currentID);
        Vector<CaptureDeviceInfo> cdiv = fd.getDeviceInfo();
        if (cdiv != null && cdiv.size() > 0)
        {
            for (int i = 0; i < cdiv.size(); i++)
            {
                CaptureDeviceInfo cdi = cdiv.elementAt(i);
                // At the moment, the name and locator are identical
                System.err.println("CaptureDeviceInfo = "
                                   + cdi.getName());
//              System.err.println("CaptureDeviceInfo = "
//              + cdi.getName() + " "
//              + cdi.getLocator());
            }
        }
    }

    static class FormatSetup
    {

        int id;

        boolean fullVideo = false;
        boolean anyVideo = true;

        String sAnalog, sPort, sVideoFormat, sSize;

        Hashtable<String, Format> videoFormats = new Hashtable<String, Format>();

        OPICapture opiVidCap = null;

        public FormatSetup(int id)
        {
            this.id = id;
            opiVidCap = new OPICapture(null);
            if (!opiVidCap.connect(id))
            {
                throw new Error("Unable to connect to device");
            }

        }

        private void addVideoFormat(Format fin)
        {
            String sVideo = sPort + "/" + sVideoFormat + "/"
                + sSize + "/"
                + sAnalog;
            System.err.println("New format " + sVideo + " = " + fin);
            videoFormats.put(sVideo, fin);
        }

        public void mydispose()
        {
            opiVidCap.disconnect();
            System.err.println("Disconnected driver");
        }

        public void doFormat()
        {
            if (anyVideo)
            {
                doVideoFormats();
            }
        }

        public void doVideoFormats()
        {
            if (!anyVideo)
            {
                // add a dummy format entry
                videoFormats.put("off", new VideoFormat(VideoFormat.RGB));
            }

            sAnalog = "ntsc";
            if (DO_PAL)
                sAnalog = "pal";
            if (!opiVidCap.setSignal(sAnalog))
            {
                System.err.println("Video analog signal not recognized");
                return;
            }
            int port = 1;
            if (!opiVidCap.setPort(port))
            {
                System.err.println("Video source not recognized on port");
                return;
            }
            sPort = "" + port;
            opiVidCap.setScale(2);
            sSize = "cif";
            getVideoFormats();
        }

        private void getVideoFormats()
        {
            sVideoFormat = "h261";
            getH261Format();
            sVideoFormat = "h263";
            getH263Format();
            sVideoFormat = "jpeg";
            getJpegFormat();
            sVideoFormat = "rgb";
            getRGBFormat();
            sVideoFormat = "yuv";
            getYUVFormat();
        }

        private void getRGBFormat()
        {
            if (!opiVidCap.setCompress("RGB"))
                return;
            /*
             * If sizes are wanted, the only valid sizes are
             *  NTSC
             *      fcif    (640 x 480)
             *      cif     (320 x 240)
             *      qcif    (160 x 120)
             *  PAL
             *      fcif    (768 x 576)
             *      cif     (384 x 288)
             *      qcif    (192 x 144)
             */
            Dimension size = new Dimension(opiVidCap.getWidth(),
                                           opiVidCap.getHeight());
            addVideoFormat(new RGBFormat(size, Format.NOT_SPECIFIED,
                                         Format.byteArray,
                                         Format.NOT_SPECIFIED,
                                         16,
                                         0xF800, 0x7E0, 0x1F, 2,
                                         Format.NOT_SPECIFIED,
                                         Format.FALSE,
                                         Format.NOT_SPECIFIED));
        }

        private void getYUVFormat()
        {
            if (!opiVidCap.setCompress("YUV"))
                return;
            /*
             * If sizes are wanted, the only valid sizes are
             *  NTSC
             *      fcif    (640 x 480)
             *      cif     (320 x 240)
             *      qcif    (160 x 120)
             *  PAL
             *      fcif    (768 x 576)
             *      cif     (384 x 288)
             *      qcif    (192 x 144)
             *
             * The capture stream is actually interleaved YVYU format.
             * This is defined in the offset values below.
             */
            Dimension size = new Dimension(opiVidCap.getWidth(),
                                           opiVidCap.getHeight());
            addVideoFormat(new YUVFormat(size, Format.NOT_SPECIFIED,
                                         Format.byteArray,
                                         Format.NOT_SPECIFIED,
                                         YUVFormat.YUV_YUYV,
                                         Format.NOT_SPECIFIED,
                                         Format.NOT_SPECIFIED,
                                         0, 3, 1));
        }

        private void getJpegFormat()
        {
            if (!opiVidCap.setCompress("Jpeg"))
                return;
            /*
             * If sizes are wanted, the only valid sizes are
             *  NTSC
             *      cif     (320 x 240)
             *      qcif    (160 x 120)
             *  PAL
             *      cif     (384 x 288)
             *      qcif    (192 x 144)
             */
            Dimension size = new Dimension(opiVidCap.getWidth(),
                                           opiVidCap.getHeight());
            addVideoFormat(new VideoFormat(VideoFormat.JPEG, size,
                                           Format.NOT_SPECIFIED,
                                           Format.byteArray,
                                           Format.NOT_SPECIFIED));
        }

        private void getH261Format()
        {
            if (!opiVidCap.setCompress("H261"))
                return;
            /*
             * If sizes are wanted, the only valid sizes are
             *      cif     (352 x 288)
             *      qcif    (176 x 144)
             */
            Dimension size = new Dimension(opiVidCap.getWidth(),
                                           opiVidCap.getHeight());
            addVideoFormat(new VideoFormat(VideoFormat.H261, size,
                                           Format.NOT_SPECIFIED,
                                           Format.byteArray,
                                           Format.NOT_SPECIFIED));
        }

        private void getH263Format()
        {
            if (!opiVidCap.setCompress("H263"))
                return;
            /*
             * If sizes are wanted, the only valid sizes are
             *      cif     (352 x 288)
             *      qcif    (176 x 144)
             */
            Dimension size = new Dimension(opiVidCap.getWidth(),
                                           opiVidCap.getHeight());
            addVideoFormat(new VideoFormat(VideoFormat.H263, size,
                                           Format.NOT_SPECIFIED,
                                           Format.byteArray,
                                           Format.NOT_SPECIFIED));
        }

        public void issueError(String err)
        {
            System.err.println(err);
            Toolkit.getDefaultToolkit().beep();
        }

        public Enumeration<String> sortedFormats(Hashtable<String, Format> formats)
        {
            Vector<String> sorted = new Vector<String>();
            keyloop:for (Enumeration<String> en = formats.keys();
                         en.hasMoreElements(); )
            {
                String key = en.nextElement();
                for (int i = 0; i < sorted.size(); i++)
                {
                    if (key.compareTo( sorted.elementAt(i)) < 0)
                    {
                        sorted.insertElementAt(key, i);
                        continue keyloop;
                    }
                }
                sorted.addElement(key);
            }
            return sorted.elements();
        }

        public Vector<CaptureDeviceInfo> getDeviceInfo()
        {
            doFormat();
            mydispose();

            String locatorPrefix = LOCATOR_PREFIX + id;
            Vector<CaptureDeviceInfo> devices = new Vector<CaptureDeviceInfo>();
            if (anyVideo)
            {

                for (Enumeration<String> ve = sortedFormats(videoFormats);
                     ve.hasMoreElements(); )
                {
                    String vKey = ve.nextElement();
                    Format vForm = videoFormats.get(vKey);
                    Format[] farray = null;
                    farray = new Format[1];
                    farray[0] = vForm;
                    String name = locatorPrefix + "/" + vKey;
                    CaptureDeviceInfo cdi = new CaptureDeviceInfo(name,
                        new MediaLocator(name), farray);
                    CaptureDeviceManager.addDevice(cdi);
                    devices.addElement(cdi);
                }
            }
            return devices;
        }

    }

    public static void setPALSignal(boolean pal)
    {
        DO_PAL = pal;
    }

    public static void main(String[] args)
    {
        if (args.length > 0)
        {
            if (args.length > 1)
            {
                System.err.println(
                    "Usage: java SunVideoPlusAuto [ ntsc | pal ]");
                System.exit(1);
            }
            if (args[0].equalsIgnoreCase("ntsc"))
            {
                SunVideoPlusAuto.setPALSignal(false);
            }
            else if (args[0].equalsIgnoreCase("pal"))
            {
                SunVideoPlusAuto.setPALSignal(true);
            }
            else
            {
                System.err.println(
                    "Usage: java SunVideoPlusAuto [ ntsc | pal ]");
                System.exit(1);
            }
        }
        SunVideoPlusAuto sunVideoPlus = new SunVideoPlusAuto();
        sunVideoPlus.autoDetectDevices();
        System.exit(0);
    }
}

