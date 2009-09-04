/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)JMFInit.java 1.14 03/04/30
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package net.java.sip.communicator.impl.media.device;

import java.io.*;
import java.util.*;
import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.util.*;


public class JMFInit
    implements Runnable {

    private static final Logger logger = Logger.getLogger(JMFInit.class);

    public JMFInit() {

//        try {
//            Registry.commit();
//        }
//        catch (Exception e) {
//            logger.error("Failed to commit to JMFRegistry!", e );
//        }


        Thread detectThread = new Thread(this);
        detectThread.run();

/*
        int slept = 0;
        while (!done && slept < 60 * 1000 * 2) {
            try {
                Thread.currentThread().sleep(500);
            }
            catch (InterruptedException ie) {
            }
            slept += 500;
        }

        if (!done) {
            console.error("Detection is taking too long! Aborting!");
            message("Detection is taking too long! Aborting!");
        }

        try {
            Thread.currentThread().sleep(2000);
        }
        catch (InterruptedException ie) {
        }
*/
    }

    /**
     * Detect all capture devices
     */
    public void run() {
        detectDirectAudio();
        detectS8DirectAudio();
        detectCaptureDevices();
    }

    private void detectCaptureDevices() {
        // check if JavaSound capture is available
        logger.info("Looking for Audio capturer");
        Class<?> dsauto = null;
        try {
            dsauto = Class.forName(
                    "net.java.sip.communicator.impl.media.device.DirectSoundAuto");
            dsauto.newInstance();
            logger.info("Finished detecting DirectSound capturer");
        }
        catch (ThreadDeath td) {
            throw td;
        }
        catch (Throwable t) {
            logger.warn("DirectSound capturer detection failed!", t);
        }

        Class<?> jsauto = null;
        try {
            jsauto = Class.forName(
                    "net.java.sip.communicator.impl.media.device.JavaSoundAuto");
            jsauto.newInstance();
            logger.info("Finished detecting JavaSound capturer");
        }
        catch (ThreadDeath td) {
            throw td;
        }
        catch (Throwable t) {
            logger.warn("JavaSound capturer detection failed!", t);
        }

        // Check if VFWAuto or SunVideoAuto is available
        logger.info("Looking for video capture devices");
        Class<?> auto = null;
        Class<?> autoPlus = null;
        try {
            auto = Class.forName(
                    "net.java.sip.communicator.impl.media.device.VFWAuto");
        }
        catch (Exception e) {
            logger.warn("VFWAuto capturer detection failed!", e);
        }
        if (auto == null) {
            try {
                auto = Class.forName(
                        "net.java.sip.communicator.impl.media.device.SunVideoAuto");
            }
            catch (Exception ee) {
              logger.warn("SunVideoAuto capturer detection failed!", ee);
            }
            try {
                autoPlus = Class.forName(
                        "net.java.sip.communicator.impl.media.device.SunVideoPlusAuto");
            }
            catch (Exception ee) {
              logger.warn("SunVideoPlusAuto capturer detection failed!", ee);
            }
        }
        if (auto == null) {
            try {
                auto = Class.forName(
                        "net.java.sip.communicator.impl.media.device.V4LAuto");
            }
            catch (Exception ee) {
                logger.warn("V4lAuto capturer detection failed!", ee);
            }
        }
        try {
            auto.newInstance();
            if (autoPlus != null) {
                autoPlus.newInstance();
            }
            logger.info("Finished detecting video capture devices");
        }
        catch (ThreadDeath td) {
            throw td;
        }
        catch (Throwable t) {
            logger.error("Capture device detection failed!", t);
        }
    }

    @SuppressWarnings("unchecked") //legacy JMF code.
    private void detectDirectAudio() {
        Class<?> cls;
        int plType = PlugInManager.RENDERER;
        String dar = "com.sun.media.renderer.audio.DirectAudioRenderer";
        try {
            // Check if this is the Windows Performance Pack - hack
            cls = Class.forName(
                    "net.java.sip.communicator.impl.media.device.VFWAuto");
            // Check if DS capture is supported, otherwise fail DS renderer
            // since NT doesn't have capture
            cls = Class.forName("com.sun.media.protocol.dsound.DSound");
            // Find the renderer class and instantiate it.
            cls = Class.forName(dar);

            Renderer rend = (Renderer) cls.newInstance();
            try {
                // Set the format and open the device
                AudioFormat af = new AudioFormat(AudioFormat.LINEAR,
                                                 44100, 16, 2);
                rend.setInputFormat(af);
                rend.open();
                Format[] inputFormats = rend.getSupportedInputFormats();
                // Register the device
                PlugInManager.addPlugIn(dar, inputFormats, new Format[0],
                                        plType);
                // Move it to the top of the list
                Vector<String> rendList =
                    PlugInManager.getPlugInList(null, null, plType);
                int listSize = rendList.size();
                if (rendList.elementAt(listSize - 1).equals(dar)) {
                    rendList.removeElementAt(listSize - 1);
                    rendList.insertElementAt(dar, 0);
                    PlugInManager.setPlugInList(rendList, plType);
                    PlugInManager.commit();
                    //System.err.println("registered");
                }
                rend.close();
            }
            catch (Throwable t) {
                //System.err.println("Error " + t);
            }
        }
        catch (Throwable tt) {
        }
    }

    private void detectS8DirectAudio() {
        try
        {
            new S8DirectAudioAuto();
        }
        catch (Throwable tt)
        {
        }
    }

    /**
     * Runs JMFInit the first time the application is started so that capture
     * devices are properly detected and initialized by JMF.
     */
    public static void setupJMF()
    {
        try
        {
            logger.logEntry();

            // .jmf is the place where we store the jmf.properties file used
            // by JMF. if the directory does not exist or it does not contain
            // a jmf.properties file, or if the jmf.properties file has 0 length
            // then this is the first time we're running and should detect capture
            // devices
            String homeDir = System.getProperty("user.home");
            File jmfDir = new File(homeDir, ".jmf");
            String classpath = System.getProperty("java.class.path");
            classpath += System.getProperty("path.separator") +
                jmfDir.getAbsolutePath();
            System.setProperty("java.class.path", classpath);

            if (!jmfDir.exists())
                jmfDir.mkdir();

            File jmfProperties = new File(jmfDir, "jmf.properties");

            if (!jmfProperties.exists()) {
                try {
                    jmfProperties.createNewFile();
                }
                catch (IOException ex) {
                    logger.error(
                        "Failed to create jmf.properties - " +
                        jmfProperties.getAbsolutePath());
                }
            }

            //if we're running on linux checkout that libjmutil.so is where it
            //should be and put it there.
//            runLinuxPreInstall();

            if (jmfProperties.length() == 0) {
                new JMFInit();
            }
        }
        finally
        {
            logger.logExit();
        }

    }


//    private static void runLinuxPreInstall()
//    {
//        try {
//            logger.logEntry();
//
//            if (Utils.getProperty("os.name") == null
//               || !Utils.getProperty("os.name").equalsIgnoreCase("Linux"))
//                 return;
//
//            try {
//                System.loadLibrary("jmv4l");
//                console.debug("Successfully loaded libjmv4l.so");
//            }
//            catch (UnsatisfiedLinkError err) {
//                console.debug("Failed to load libjmv4l.so. Will try and copy libjmutil.so", err);
//
//                String destinationPathStr = Utils.getProperty("java.home")
//                                              + File.separator + "lib"
//                                              + File.separator + "i386";
//                String libjmutilFileStr   = "libjmutil.so";
//
//                try {
//                    InputStream libIS =
//                        MediaManager.class.getClassLoader().
//                                          getResourceAsStream(libjmutilFileStr);
//                     File outFile = new File(destinationPathStr
//                                             +File.separator + libjmutilFileStr);
//
//                     //Check if file is already there - Ben Asselstine
//                     if (outFile.exists()) {
//                         //if we're here then libjmutil is already where it should be
//                         // but yet we failed to load libjmv4l.
//                         //so notify log and bail out
//                         console.error(
//                             "An error occurred while trying to load JMF. This "
//                             +"error is probably due to a JMF installation problem. "
//                             +"Please copy libjmutil.so to a location contained by "
//                             + "$LD_LIBRARY_PATH and try again!",
//                             err);
//                         return;
//
//                     }
//
//                     outFile.createNewFile();
//
//                     console.debug("jmutil");
//
//                    FileOutputStream fileOS = new FileOutputStream(outFile);
//                    int available = libIS.available();
//                    byte[] bytes = new byte[libIS.available()];
//                    int read = 0;
//                    int i = 0;
//                    for (i = 0; i<available ; i++)
//                    {
//                        bytes[i] = (byte)libIS.read();
//                    }
//
//                    console.debug("Read " + i + " bytes out of " + available );
//
//                    fileOS.write(bytes, 0, bytes.length);
//                    console.debug("Wrote " + available + " bytes.");
//                    bytes = null;
//                    libIS.close();
//                    fileOS.close();
//                }
//                catch (IOException exc) {
//                    if(   exc.getMessage() != null
//                       && exc.getMessage().toLowerCase().indexOf("permission denied") != -1)
//                         console.showError("Permission denied!",
//                                         "Because of insufficient permissions SIP Communicator has failed "
//                                         + "to copy a required library to\n\n\t"
//                                         + destinationPathStr + "!\n\nPlease run the application as root or "
//                                         + "manually copy the " +libjmutilFileStr
//                                         + " file to the above location!\n");
//                    exc.printStackTrace();
//                }
//            }
//                /** @todo check whether we have a permissions problem and alert the
//             * user that they should be running as root */
//            catch(Throwable t)
//            {
//                console.debug("Error while loading");
//            }
//        }
//        finally {
//            console.logExit();
//        }
//    }

    public static void start() {
        setupJMF();
    }
}
