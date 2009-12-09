/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.util.*;

import com.sun.media.util.*;

/**
 * Probes for available capture and playback devices and initializes the
 * jmf.properties accordingly.
 *
 * @author Emil Ivov
 * @author Ken Larson
 * @author Lubomir Marinov
 */
public class JmfDeviceDetector
{
    /**
     * The <tt>Logger</tt> used by the <tt>JmfDeviceDetector</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(JmfDeviceDetector.class);

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
        if (FMJConditionals.USE_JMF_INTERNAL_REGISTRY)
        {
            // This uses JMF internals:
            // see if the registry has already been "tagged" by us, 
            // skip auto-detection if it has.
            // This was probably done because JMF auto-detection is very slow, 
            // especially for video devices.  FMJ does this quickly, 
            // so there is no need for this kind of workaround
            // (besides the fact that these internal functions are not
            // implemented in FMJ).
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
        }

        detectCaptureDevices();
    }

    /**
     * Detect all existing capture devices and record them into the JMF
     * repository.
     */
    private void detectCaptureDevices()
    {
        logger.info("Looking for Audio capturer");

        // check if JavaSound capture is available
        try
        {
            new JavaSoundAuto();
        }
        catch (Throwable exc)
        {
            logger.debug("No JMF javasound detected: " + exc.getMessage());
        }

        // check if we have FMJJavaSoundAuto capture is available
        try
        {
            new FMJJavaSoundAuto();
        }
        catch (Throwable exc)
        {
            logger.debug("No FMJ javasound detected: " + exc.getMessage());
        }
        try
        {
            new PortAudioAuto();
        }
        catch (Throwable exc)
        {
            logger.info("No portaudio detected: " + exc.getMessage());
        }

        // after javasound and portaudio eventually add them to available
        // audio systems, lets add option None, in order to be able to
        // disable audio
        DeviceConfiguration.addAudioSystem(
            DeviceConfiguration.AUDIO_SYSTEM_NONE);

        // video is enabled by default
        // if video is disabled skip device detection
        if (NeomediaActivator
                .getConfigurationService()
                    .getBoolean(
                        MediaServiceImpl.DISABLE_VIDEO_SUPPORT_PROPERTY_NAME,
                        false))
            return;

        // Try to configgure capture devices for any operating system.
        // those that do not apply will silently fail.
        logger.info("Looking for video capture devices");

        //FMJ
        try
        {
            if(isFMJVideoAvailable())
                new FMJCivilVideoAuto();
        }
        catch (Throwable exc)
        {
            logger.debug("No FMJ CIVIL video detected: " + exc.getMessage(), exc);
        }
    }

    /**
     * Currently fmj video under macosx using java version 1.6 is not supported.
     * As macosx video support is using libQTJNative.jnilib which supports
     * only java 1.5 and is deprecated.
     * @return is fmj video supported under current OS and environment.
     */
    private boolean isFMJVideoAvailable()
    {
        return
            !(OSUtils.IS_MAC
                && System.getProperty("java.version").startsWith("1.6"));
    }

    /**
     * Runs JMFInit the first time the application is started so that capture
     * devices are properly detected and initialized by JMF.
     */
    public static void setupJMF()
    {
        logger.logEntry();
        try
        {

            // we'll be storing our jmf.properties file inside the
            //sip-communicator directory. If it does not exist - we created it.
            //If the jmf.properties file has 0 length then this is the first
            //time we're running and should detect capture devices
            File jmfPropsFile = null;
            try
            {
                FileAccessService faService
                    = NeomediaActivator.getFileAccessService();
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

        setupRenderers();
    }

    /**
     * Sets the renderers appropriate for the current platform.
     */
    @SuppressWarnings("unchecked") //legacy JMF code.
    private static void setupRenderers()
    {
        Vector<String> renderers =
            PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);

        if(OSUtils.IS_WINDOWS)
        {
            if (OSUtils.IS_WINDOWS_VISTA)
            {
                /*
                 * DDRenderer will cause Windows Vista to switch its theme from
                 * Aero to Vista Basic so try to pick up a different Renderer.
                 */
                if (renderers
                        .contains("com.sun.media.renderer.video.GDIRenderer"))
                {
                    PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.DDRenderer",
                        PlugInManager.RENDERER);
                }
            }
            else if (OSUtils.IS_WINDOWS64)
            {
                /*
                 * Remove native renderers for Windows x64 because native JMF libs
                 * are not available for 64 bit machines
                 */
                PlugInManager.removePlugIn(
                    "com.sun.media.renderer.video.GDIRenderer",
                    PlugInManager.RENDERER);
                PlugInManager.removePlugIn(
                    "com.sun.media.renderer.video.DDRenderer",
                    PlugInManager.RENDERER);
            }
        }
        else if(!OSUtils.IS_LINUX32)
        {
            if (renderers.contains("com.sun.media.renderer.video.LightWeightRenderer") ||
                renderers.contains("com.sun.media.renderer.video.AWTRenderer"))
            {
                // remove xlib renderer cause its native one and jmf is supported
                // only on 32bit machines
                PlugInManager.removePlugIn(
                    "com.sun.media.renderer.video.XLibRenderer",
                    PlugInManager.RENDERER);
            }
        }
    }

    /**
     * Detect all devices and complete
     */
    public static void detectAndConfigureCaptureDevices()
    {
        setupJMF();
    }
}
