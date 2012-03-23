/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.util.*;

import com.sun.media.util.*;

/**
 * Probes for available capture and playback devices and initializes the
 * jmf.properties accordingly.
 *
 * @author Emil Ivov
 * @author Ken Larson
 * @author Lyubomir Marinov
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
     * The JMF registry property that specifies that have initialized the
     * currently valid repository.
     */
    private static final String PROP_REGISTRY_AUTHOR
        = "registry.author";

    /**
     * The value of the JMF registry property that determines whether we have
     * initialized the current repository.
     */
    private static final String PROP_REGISTRY_AUTHOR_VALUE
        = "sip-communicator.org";

    /**
     * PortAudioAuto reference.
     */
    private static PortAudioAuto portaudioAuto = null;

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
                return;

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
     * Reinitializes the video capture devices.
     */
    private void reinitializeVideo()
    {
        // Determine which devices are to be reinitialized.
        @SuppressWarnings("unchecked")
        Vector<CaptureDeviceInfo> devices
            = (Vector<CaptureDeviceInfo>)
                CaptureDeviceManager.getDeviceList(null);
        List<CaptureDeviceInfo> devicesToRemove
            = new ArrayList<CaptureDeviceInfo>(devices.size());

        for (CaptureDeviceInfo device : devices)
        {
            Format formats[] = device.getFormats();

            if ((formats.length > 0) && (formats[0] instanceof VideoFormat))
            {
                /*
                 * XXX It seems that problems arise when we reload the webcam
                 * devices so reinitialize the desktop streaming ones for now.
                 */
                MediaLocator locator = device.getLocator();

                if ((locator != null)
                        && ImageStreamingAuto.LOCATOR_PROTOCOL.equals(
                                locator.getProtocol()))
                {
                    devicesToRemove.add(device);
                }
            }
        }

        // Remove the devices which are to be reinitialized.
        for(CaptureDeviceInfo deviceToRemove : devicesToRemove)
            CaptureDeviceManager.removeDevice(deviceToRemove);

        /*
         * Detect the video capture devices unless the configuration explicitly
         * states that they are to not be detected.
         */
        if (!NeomediaActivator.getConfigurationService().getBoolean(
                MediaServiceImpl.DISABLE_VIDEO_SUPPORT_PNAME,
                false))
        {
            /*
             * As stated above, we're reinitializing only the desktop streaming
             * devices for now.
             */
            try
            {
                new ImageStreamingAuto();
            }
            catch(Throwable t)
            {
                logger.debug(
                        "No desktop streaming available: " + t.getMessage(),
                        t);
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }
    }

    /**
     * Detect all existing capture devices and record them into the JMF
     * repository.
     */
    private void detectCaptureDevices()
    {
        /*
         * Detect the audio capture devices unless the configuration explicitly
         * states that they are to not be detected.
         */
        if (!NeomediaActivator.getConfigurationService().getBoolean(
                MediaServiceImpl.DISABLE_AUDIO_SUPPORT_PNAME,
                false))
        {
            initializeAudio();
        }

        /*
         * After all audio systems have been added during their respective
         * detection routine, add the option None which allows the disabling of
         * audio.
         */
        DeviceConfiguration.addAudioSystem(DeviceConfiguration.AUDIO_NONE);

        /*
         * Detect the video capture devices unless the configuration explicitly
         * states that they are to not be detected.
         */
        if (!NeomediaActivator.getConfigurationService().getBoolean(
                MediaServiceImpl.DISABLE_VIDEO_SUPPORT_PNAME,
                false))
        {
            initializeVideo();
        }
    }

    /**
     * Initializes the audio capture devices.
     */
    private void initializeAudio()
    {
        if (logger.isInfoEnabled())
            logger.info("Looking for Audio capturer");

        // check if JavaSound capture is available
        try
        {
            new JavaSoundAuto();
        }
        catch (Throwable exc)
        {
            if (logger.isDebugEnabled())
                logger.debug("No JMF javasound detected: " + exc.getMessage());
        }

        // check if we have FMJJavaSoundAuto capture is available
        try
        {
            new FMJJavaSoundAuto();
        }
        catch (Throwable exc)
        {
            if (logger.isDebugEnabled())
                logger.debug("No FMJ javasound detected: " + exc.getMessage());
        }
        try
        {
            portaudioAuto = new PortAudioAuto();
        }
        catch (Throwable exc)
        {
            if (logger.isInfoEnabled())
                logger.info("No portaudio detected: " + exc.getMessage());
        }
    }

    /**
     * Initializes the video capture devices.
     */
    private void initializeVideo()
    {
        // Try to configure capture devices for any operating system.
        // those that do not apply will silently fail.
        if (logger.isInfoEnabled())
            logger.info("Looking for video capture devices");

        if (OSUtils.IS_MAC) // QuickTime
        {
            try
            {
                new QuickTimeAuto();
            }
            catch (Throwable t)
            {
                if (logger.isDebugEnabled())
                    logger.debug("No QuickTime detected: " + t.getMessage(), t);
            }
        }
        else if (OSUtils.IS_LINUX) // Video4Linux2
        {
            try
            {
                new Video4Linux2Auto();
            }
            catch (Throwable t)
            {
                if (logger.isDebugEnabled())
                    logger.debug("No Video4Linux2 detected: " + t.getMessage(),
                            t);
            }
        }
        else if (OSUtils.IS_WINDOWS) /* DirectShow */
        {
            try
            {
                new DirectShowAuto();
            }
            catch(Throwable t)
            {
                if (logger.isDebugEnabled())
                    logger.debug("No DirectShow detected: " + t.getMessage(),
                            t);
            }
        }

        /* Desktop capture */
        try
        {
            new ImageStreamingAuto();
        }
        catch(Throwable exc)
        {
            logger
                .debug(
                    "No desktop streaming available: " + exc.getMessage(),
                    exc);
        }
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
            if (OSUtils.IS_WINDOWS32 &&
                    (OSUtils.IS_WINDOWS_VISTA || OSUtils.IS_WINDOWS_7))
            {
                /*
                 * DDRenderer will cause Windows Vista/7 32-bit to switch its
                 * theme from Aero to Vista Basic so try to pick up a different
                 * Renderer.
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
        new JmfDeviceDetector().initialize();
        setupRenderers();
    }

    /**
     * Reinitializes the video capture devices.
     */
    public static void reinitializeVideoCaptureDevices()
    {
        new JmfDeviceDetector().reinitializeVideo();
    }

    /**
     * Reinitialize PortAudio devices.
     */
    public static void reinitializePortAudio()
    {
        if(portaudioAuto != null)
        {
            try
            {
                portaudioAuto.reinit();
            }
            catch(Exception e)
            {
            }
        }
    }
}
