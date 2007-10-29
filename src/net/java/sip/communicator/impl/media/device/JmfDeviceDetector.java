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

import com.sun.media.*;
import net.java.sip.communicator.util.*;
import com.sun.media.util.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.impl.media.*;

/**
 * Probes for available capture and playback devices and initializes the
 * jmf.properties accordingly.
 *
 * @author Emil Ivov
 */
public class JmfDeviceDetector
{

    private static final Logger logger = Logger.getLogger(JmfDeviceDetector.class);

    /**
     * The JMF property that specifies whether we'd have the right to capture
     * when run from webstart or an applet.
     */
    private static final String PROP_ALLOW_CAPTURE_FROM_APPLETS
        = "secure.allowCaptureFromApplets";

    /**
     * The JMF property that specifies whether we'd have the right to save
     * files when run from webstart or an applet.
     */
    private static final String PROP_ALLOW_SAVE_FILE_FROM_APPLETS
        = "secure.allowSaveFileFromApplets";

    /**
     * The JMF registry property that specifies that have initilized the
     * currently valid repository.
     */
    private static final String PROP_REGISTRY_AUTHOR
        = "registry.author";

    /**
     * The value of the JMF registry property that determines whether we have
     * initilized the currentl repository.
     */
    private static final String PROP_REGISTRY_AUTHOR_VALUE
        = "sip-communicator.org";

    /**
     * The name of the file that the JMF registry uses for storing and loading
     * jmf properties.
     */
    private static final String JMF_PROPERTIES_FILE_NAME = "jmf.properties";

    /**
     * Default constructor - does nothing.
     */
    public JmfDeviceDetector()
    {
    }

    /**
     * Detect all capture devices
     */
    private void initialize()
    {
        String author = (String)Registry.get(PROP_REGISTRY_AUTHOR);

        if(author != null)
        {
            return;
        }

        Registry.set(PROP_ALLOW_CAPTURE_FROM_APPLETS, new Boolean(true));
        Registry.set(PROP_ALLOW_SAVE_FILE_FROM_APPLETS, new Boolean(true));

        Registry.set(PROP_REGISTRY_AUTHOR, PROP_REGISTRY_AUTHOR_VALUE);

        try
        {
            Registry.commit();
        }
        catch (Exception exc)
        {
            logger.error(
                "Failed to initially commit JMFRegistry. Ignoring err."
                , exc);
        }

        detectDirectAudio();
        detectS8DirectAudio();
        detectCaptureDevices();
    }

    /**
     * Detect all existing capture devices and record them into the jmf
     * repository.
     */
    private void detectCaptureDevices()
    {
        // check if JavaSound capture is available
        logger.info("Looking for Audio capturer");
        DirectSoundAuto directSoundAuto = new DirectSoundAuto();
        JavaSoundAuto javaSoundAuto = new JavaSoundAuto();

        // Try to configgure capture devices for any operating system.
        //those that do not apply will silently fail.
        logger.info("Looking for video capture devices");
        int nDevices = 0;
        //Windows
        try
        {
            VFWAuto vfwAuto = new VFWAuto();
            vfwAuto.autoDetectDevices();
            logger.info("Detected "
                        + nDevices
                        +" VFW video capture device(s).");
        }
        catch (Throwable exc)
        {
            logger.debug("No VFW video detected: " + exc.getMessage());
        }

        //SunVideo
        try
        {
            SunVideoAuto sunVideoAuto = new SunVideoAuto();
            nDevices = sunVideoAuto.autoDetectDevices();

            logger.info("Detected "
                        + nDevices
                        +" SUN Video capture device(s).");
        }
        catch (Exception exc)
        {
            logger.debug("No SUN Video detected: " + exc.getMessage());
        }

        //SunVideoPlus
        try
        {
            SunVideoPlusAuto sunVideoAutoPlus = new SunVideoPlusAuto();
            nDevices = sunVideoAutoPlus.autoDetectDevices();

            logger.info("Detected "
                        + nDevices
                        + " SUN Video Plus device(s).");
        }
        catch (Exception exc)
        {
            logger.debug("No SUN Video Plus detected: " + exc.getMessage());
        }

        //Linux
//        try
//        {
//            V4LAuto v4lAuto = new V4LAuto();
//            nDevices = v4lAuto.autoDetectDevices();
//            logger.info("Detected "
//                        + nDevices
//                        +" V4L video capture device.");
//        }
//        catch (Throwable exc)
//        {
//            logger.debug("No V4l video detected: " + exc.getMessage());
//        }
        
//        try
//        {
//            new net.sf.fmj.media.cdp.civil.CaptureDevicePlugger().
//                    addCaptureDevices();
//            CaptureDeviceManager.commit();
//        }
//        catch (Throwable exc)
//        {
//            logger.debug("No civil video detected: ", exc);
//        }
    }

    private void detectDirectAudio()
    {
        Class cls;
        int plType = PlugInManager.RENDERER;
        String dar = "com.sun.media.renderer.audio.DirectAudioRenderer";
        try
        {
            // Check if this is the Windows Performance Pack - hack
            cls = Class.forName(
                "net.java.sip.communicator.impl.media.configuration.VFWAuto");
            // Check if DS capture is supported, otherwise fail DS renderer
            // since NT doesn't have capture
            cls = Class.forName("com.sun.media.protocol.dsound.DSound");
            // Find the renderer class and instantiate it.
            cls = Class.forName(dar);

            Renderer rend = (Renderer) cls.newInstance();
            try
            {
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
                Vector rendList =
                    PlugInManager.getPlugInList(null, null, plType);
                int listSize = rendList.size();
                if (rendList.elementAt(listSize - 1).equals(dar))
                {
                    rendList.removeElementAt(listSize - 1);
                    rendList.insertElementAt(dar, 0);
                    PlugInManager.setPlugInList(rendList, plType);
                    PlugInManager.commit();
                    //System.err.println("registered");
                }
                rend.close();
            }
            catch (Throwable throwable)
            {
                //System.err.println("Error " + t);
            }
        }
        catch (Throwable tt)
        {
        }
    }

    private void detectS8DirectAudio()
    {
        Class cls;
        int plType = PlugInManager.RENDERER;
        String dar = "com.sun.media.renderer.audio.DirectAudioRenderer";
        try
        {
            // Check if this is the solaris Performance Pack - hack
            cls = Class.forName(
                "net.java.sip.communicator.impl.media.configuration.SunVideoAuto");

            // Find the renderer class and instantiate it.
            cls = Class.forName(dar);

            Renderer rend = (Renderer) cls.newInstance();

            if (rend instanceof ExclusiveUse &&
                ! ( (ExclusiveUse) rend).isExclusive())
            {
                // sol8+, DAR supports mixing
                Vector rendList = PlugInManager.getPlugInList(null, null,
                    plType);
                int listSize = rendList.size();
                boolean found = false;
                String rname = null;

                for (int i = 0; i < listSize; i++)
                {
                    rname = (String) (rendList.elementAt(i));
                    if (rname.equals(dar))
                    { // DAR is in the registry
                        found = true;
                        rendList.removeElementAt(i);
                        break;
                    }
                }

                if (found)
                {
                    rendList.insertElementAt(dar, 0);
                    PlugInManager.setPlugInList(rendList, plType);
                    PlugInManager.commit();
                }
            }
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

            // we'll be storing our jmf.properties file inside the
            //sip-communicator directory. If it does not exist - we created it.
            //If the jmf.properties file has 0 length then this is the first
            //time we're running and should detect capture devices
            File jmfPropsFile = null;
            try
            {
                FileAccessService faService
                    = MediaActivator.getFileAccessService();
                if(faService != null)
                {
                    jmfPropsFile = faService.
                        getPrivatePersistentFile(JMF_PROPERTIES_FILE_NAME);
                }
                //small hack for when running from outside oscar
                else
                {
                    jmfPropsFile
                    = new File(System.getProperty("user.home")
                               + File.separator
                               + ".sip-communicator/jmf.properties");
                }
                //force reinitialization
                if(jmfPropsFile.exists())
                    jmfPropsFile.delete();
                jmfPropsFile.createNewFile();
            }
            catch (Exception exc)
            {
                throw new RuntimeException(
                    "Failed to create the jmf.properties file.", exc);
            }
            String classpath =  System.getProperty("java.class.path");

            classpath = jmfPropsFile.getParentFile().getAbsolutePath()
                + System.getProperty("path.separator")
                + classpath;

            System.setProperty("java.class.path", classpath);

            /** @todo run this only if necessary and in parallel. Right now
             * we're running detection no matter what. We should be more
             * intelligent and detect somehow whether new devices are present
             * before we run our detection tests.*/
            JmfDeviceDetector detector = new JmfDeviceDetector();
            detector.initialize();
        }
        finally
        {
            logger.logExit();
        }

    }


    /**
     * Detect all devices and complete
     */
    public static void detectAndConfigureCaptureDevices()
    {
        setupJMF();
    }

    public static void main(String[] args)
    {
        detectAndConfigureCaptureDevices();
    }
}
