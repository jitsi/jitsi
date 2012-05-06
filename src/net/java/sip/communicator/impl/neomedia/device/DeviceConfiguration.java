/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.event.*;

/**
 * This class aims to provide a simple configuration interface for JMF. It
 * retrieves stored configuration when started or listens to ConfigurationEvent
 * for property changes and configures the JMF accordingly.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class DeviceConfiguration
    extends PropertyChangeNotifier
    implements PropertyChangeListener
{

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which represents
     * the device used by <tt>DeviceConfiguration</tt> for audio capture.
     */
    public static final String AUDIO_CAPTURE_DEVICE
        = AudioSystem.PROP_CAPTURE_DEVICE;

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which represents
     * the device used by <tt>DeviceConfiguration</tt> for audio notify.
     */
    public static final String AUDIO_NOTIFY_DEVICE
        = AudioSystem.PROP_NOTIFY_DEVICE;

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which represents
     * the device used by <tt>DeviceConfiguration</tt> for audio playback.
     */
    public static final String AUDIO_PLAYBACK_DEVICE
        = AudioSystem.PROP_PLAYBACK_DEVICE;

    /**
     * The list of class names of custom <tt>Renderer</tt> implementations to be
     * registered with JMF.
     */
    private static final String[] CUSTOM_RENDERERS
        = new String[]
        {
            ".audio.PulseAudioRenderer",
            ".audio.PortAudioRenderer",
            ".video.JAWTRenderer"
        };

    /**
     * The default value to be used for the {@link #PROP_AUDIO_DENOISE} property
     * when it does not have a value.
     */
    private static final boolean DEFAULT_AUDIO_DENOISE = true;

    /**
     * The default value to be used for the {@link #PROP_AUDIO_ECHOCANCEL}
     * property when it does not have a value.
     */
    private static final boolean DEFAULT_AUDIO_ECHOCANCEL = true;

    /**
     * The default value to be used for the
     * {@link #PROP_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS} property when it
     * does not have a value. The recommended filter length is approximately the
     * third of the room reverberation time. For example, in a small room,
     * reverberation time is in the order of 300 ms, so a filter length of 100
     * ms is a good choice (800 samples at 8000 Hz sampling rate).
     */
    private static final long DEFAULT_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS
        = 100;

    /**
     * The default frame rate, <tt>-1</tt> unlimited.
     */
    public static final int DEFAULT_VIDEO_FRAMERATE = -1;

    /**
     * The default video height.
     */
    public static final int DEFAULT_VIDEO_HEIGHT = 480;

    /**
     * The default value for video maximum bandwidth.
     */
    public static final int DEFAULT_VIDEO_MAX_BANDWIDTH = 100;

    /**
     * The default video width.
     */
    public static final int DEFAULT_VIDEO_WIDTH = 640;

    /**
     * The name of the <tt>boolean</tt> property which determines whether noise
     * suppression is to be performed for captured audio.
     */
    static final String PROP_AUDIO_DENOISE
        = "net.java.sip.communicator.impl.neomedia.denoise";

    /**
     * The name of the <tt>boolean</tt> property which determines whether echo
     * cancellation is to be performed for captured audio.
     */
    static final String PROP_AUDIO_ECHOCANCEL
        = "net.java.sip.communicator.impl.neomedia.echocancel";

    /**
     * The name of the <tt>long</tt> property which determines the filter length
     * in milliseconds to be used by the echo cancellation implementation. The
     * recommended filter length is approximately the third of the room
     * reverberation time. For example, in a small room, reverberation time is
     * in the order of 300 ms, so a filter length of 100 ms is a good choice
     * (800 samples at 8000 Hz sampling rate).
     */
    static final String PROP_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS
        = "net.java.sip.communicator.impl.neomedia.echocancel.filterLengthInMillis";

    public static final String PROP_AUDIO_SYSTEM
        = "net.java.sip.communicator.impl.neomedia.audioSystem";

    public static final String PROP_AUDIO_SYSTEM_DEVICES
        = PROP_AUDIO_SYSTEM + "." + DeviceSystem.PROP_DEVICES;

    /**
     * The <tt>ConfigurationService</tt> property which stores the device used
     * by <tt>DeviceConfiguration</tt> for video capture.
     */
    private static final String PROP_VIDEO_DEVICE
        = "net.java.sip.communicator.impl.neomedia.videoDevice";

    /**
     * The property we use to store the video framerate settings.
     */
    private static final String PROP_VIDEO_FRAMERATE
        = "net.java.sip.communicator.impl.neomedia.video.framerate";

    /**
     * The name of the property which specifies the height of the video.
     */
    private static final String PROP_VIDEO_HEIGHT
        = "net.java.sip.communicator.impl.neomedia.video.height";

    /**
     * The property we use to store the settings for maximum allowed video
     * bandwidth.
     */
    private static final String PROP_VIDEO_MAX_BANDWIDTH
        = "net.java.sip.communicator.impl.neomedia.video.maxbandwidth";

    /**
     * The name of the property which specifies the width of the video.
     */
    private static final String PROP_VIDEO_WIDTH
        = "net.java.sip.communicator.impl.neomedia.video.width";

    /**
     * The currently supported resolutions we will show as option
     * and user can select.
     */
    public static final Dimension[] SUPPORTED_RESOLUTIONS
        = new Dimension[]
            {
                // QVGA
                new Dimension(160, 100),
                //QCIF
                new Dimension(176, 144),
                // QVGA
                new Dimension(320, 200),
                // QVGA
                new Dimension(320, 240),
                //CIF
                new Dimension(352, 288),
                // VGA
                new Dimension(640, 480),
                // HD 720
                new Dimension(1280, 720)
            };

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which
     * represents the device used by <tt>DeviceConfiguration</tt> for video
     * capture.
     */
    public static final String VIDEO_CAPTURE_DEVICE = "VIDEO_CAPTURE_DEVICE";

    private AudioSystem audioSystem;

    /**
     * The frame rate.
     */
    private int frameRate = DEFAULT_VIDEO_FRAMERATE;

    /**
     * The <tt>Logger</tt> used by this instance for logging output.
     */
    private Logger logger = Logger.getLogger(DeviceConfiguration.class);

    /**
     * The device that we'll be using for video capture.
     */
    private CaptureDeviceInfo videoCaptureDevice;

    /**
     * Current setting for video maximum bandwidth.
     */
    private int videoMaxBandwidth = -1;

    /**
     * The current resolution settings.
     */
    private Dimension videoSize;

    /**
     * Fixes the list of <tt>Renderer</tt>s registered with FMJ in order to
     * resolve operating system-specific issues.
     */
    private static void fixRenderers()
    {
        @SuppressWarnings("unchecked")
        Vector<String> renderers
            = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);

        /*
         * JMF is no longer in use, FMJ is used in its place. FMJ has its own
         * JavaSoundRenderer which is also extended into a JMF-compatible one.
         */
        PlugInManager.removePlugIn(
                "com.sun.media.renderer.audio.JavaSoundRenderer",
                PlugInManager.RENDERER);

        if (OSUtils.IS_WINDOWS)
        {
            if (OSUtils.IS_WINDOWS32 &&
                    (OSUtils.IS_WINDOWS_VISTA || OSUtils.IS_WINDOWS_7))
            {
                /*
                 * DDRenderer will cause 32-bit Windows Vista/7 to switch its
                 * theme from Aero to Vista Basic so try to pick up a different
                 * Renderer.
                 */
                if (renderers.contains(
                        "com.sun.media.renderer.video.GDIRenderer"))
                {
                    PlugInManager.removePlugIn(
                            "com.sun.media.renderer.video.DDRenderer",
                            PlugInManager.RENDERER);
                }
            }
            else if (OSUtils.IS_WINDOWS64)
            {
                /*
                 * Remove the native Renderers for 64-bit Windows because native
                 * JMF libs are not available for 64-bit machines.
                 */
                PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.GDIRenderer",
                        PlugInManager.RENDERER);
                PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.DDRenderer",
                        PlugInManager.RENDERER);
            }
        }
        else if (!OSUtils.IS_LINUX32)
        {
            if (renderers.contains(
                        "com.sun.media.renderer.video.LightWeightRenderer")
                    || renderers.contains(
                            "com.sun.media.renderer.video.AWTRenderer"))
            {
                // Remove XLibRenderer because it is native and JMF is supported
                // on 32-bit machines only.
                PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.XLibRenderer",
                        PlugInManager.RENDERER);
            }
        }
    }

    /**
     * Initializes capture devices.
     */
    public void initialize()
    {
        // these seem to be throwing exceptions every now and then so we'll
        // blindly catch them for now
        try
        {
            DeviceSystem.initializeDeviceSystems();
            extractConfiguredCaptureDevices();

            ConfigurationService cfg
                = NeomediaActivator.getConfigurationService();

            cfg.addPropertyChangeListener(PROP_VIDEO_HEIGHT, this);
            cfg.addPropertyChangeListener(PROP_VIDEO_WIDTH, this);
            cfg.addPropertyChangeListener(PROP_VIDEO_FRAMERATE, this);
            cfg.addPropertyChangeListener(PROP_VIDEO_MAX_BANDWIDTH, this);
        }
        catch (Exception ex)
        {
            logger.error("Failed to initialize media.", ex);
        }

        registerCustomRenderers();
        fixRenderers();
    }

    /**
     * Detects capture devices configured through JMF and disable audio and/or
     * video transmission if none were found.
     */
    private void extractConfiguredCaptureDevices()
    {
        extractConfiguredAudioCaptureDevices();
        extractConfiguredVideoCaptureDevices();
    }

    /**
     * Returns the configured video capture device with the specified
     * output format.
     * @param format the output format of the video format.
     * @return CaptureDeviceInfo for the video device.
     */
    private CaptureDeviceInfo extractConfiguredVideoCaptureDevice(Format format)
    {
        @SuppressWarnings("unchecked")
        List<CaptureDeviceInfo> videoCaptureDevices
            = CaptureDeviceManager.getDeviceList(format);
        CaptureDeviceInfo videoCaptureDevice = null;

        if (videoCaptureDevices.size() > 0)
        {
            String videoDevName
                = NeomediaActivator.getConfigurationService().getString(
                        PROP_VIDEO_DEVICE);

            if (videoDevName == null)
                videoCaptureDevice = videoCaptureDevices.get(0);
            else
            {
                for (CaptureDeviceInfo captureDeviceInfo : videoCaptureDevices)
                {
                    if (videoDevName.equals(captureDeviceInfo.getName()))
                    {
                        videoCaptureDevice = captureDeviceInfo;
                        break;
                    }
                }
            }

            if ((videoCaptureDevice != null) && logger.isInfoEnabled())
            {
                logger.info(
                        "Found "
                            + videoCaptureDevice.getName()
                            + " as a "
                            + format
                            + " video capture device.");
            }
        }
        return videoCaptureDevice;
    }

    /**
     * Returns a device that we could use for audio capture.
     *
     * @return the CaptureDeviceInfo of a device that we could use for audio
     *         capture.
     */
    public CaptureDeviceInfo getAudioCaptureDevice()
    {
        AudioSystem audioSystem = getAudioSystem();

        return (audioSystem == null) ? null : audioSystem.getCaptureDevice();
    }

    /**
     * Gets the list of audio capture devices which are available through this
     * <tt>DeviceConfiguration</tt>, amongst which is
     * {@link #getAudioCaptureDevice()} and represent acceptable values
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @return an array of <tt>CaptureDeviceInfo</tt> describing the audio
     *         capture devices available through this
     *         <tt>DeviceConfiguration</tt>
     */
    public List<CaptureDeviceInfo> getAvailableAudioCaptureDevices()
    {
        @SuppressWarnings("unchecked")
        Vector<CaptureDeviceInfo> audioCaptureDevices
            = CaptureDeviceManager.getDeviceList(
                    new AudioFormat(AudioFormat.LINEAR, -1, 16, -1));

        return audioCaptureDevices;
    }

    public AudioSystem getAudioSystem()
    {
        return audioSystem;
    }

    public AudioSystem[] getAvailableAudioSystems()
    {
        AudioSystem[] audioSystems =  AudioSystem.getAudioSystems();

        if ((audioSystems == null) || (audioSystems.length == 0))
            return audioSystems;
        else
        {
            List<AudioSystem> audioSystemsWithDevices
                = new ArrayList<AudioSystem>();

            for (AudioSystem audioSystem : audioSystems)
            {
                if (!NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                        audioSystem.getLocatorProtocol()))
                {
                    List<CaptureDeviceInfo> captureDevices
                        = audioSystem.getCaptureDevices();

                    if ((captureDevices == null)
                            || (captureDevices.size() <= 0))
                    {
                        if ((AudioSystem.FEATURE_NOTIFY_AND_PLAYBACK_DEVICES
                                    & audioSystem.getFeatures())
                                == 0)
                        {
                            continue;
                        }
                        else
                        {
                            List<CaptureDeviceInfo> notifyDevices
                                = audioSystem.getNotifyDevices();

                            if ((notifyDevices == null)
                                    || (notifyDevices.size() <= 0))
                            {
                                List<CaptureDeviceInfo> playbackDevices
                                    = audioSystem.getPlaybackDevices();
    
                                if ((playbackDevices == null)
                                        || (playbackDevices.size() <= 0))
                                {
                                    continue;
                                }
                            }
                        }
                    }
                }
                audioSystemsWithDevices.add(audioSystem);
            }

            int audioSystemsWithDevicesCount = audioSystemsWithDevices.size();

            return
                (audioSystemsWithDevicesCount == audioSystems.length)
                    ? audioSystems
                    : audioSystemsWithDevices.toArray(
                            new AudioSystem[audioSystemsWithDevicesCount]);
        }
    }

    public void setAudioSystem(AudioSystem audioSystem, boolean save)
    {
        if (this.audioSystem != audioSystem)
        {
            if (this.audioSystem != null)
                this.audioSystem.removePropertyChangeListener(this);

            AudioSystem oldValue = this.audioSystem;

            this.audioSystem = audioSystem;

            if (this.audioSystem != null)
                this.audioSystem.addPropertyChangeListener(this);

            if (save)
            {
                ConfigurationService cfg
                    = NeomediaActivator.getConfigurationService();

                if (cfg != null)
                {
                    if (this.audioSystem == null)
                        cfg.removeProperty(PROP_AUDIO_SYSTEM);
                    else
                        cfg.setProperty(
                                PROP_AUDIO_SYSTEM,
                                this.audioSystem.getLocatorProtocol());
                }
            }

            firePropertyChange(PROP_AUDIO_SYSTEM, oldValue, this.audioSystem);
        }
    }

    /**
     * Gets the list of video capture devices which are available through this
     * <tt>DeviceConfiguration</tt>, amongst which is
     * {@link #getVideoCaptureDevice(MediaUseCase)} and represent acceptable
     * values for {@link #setVideoCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @param useCase extract video capture devices that correspond to this
     * <tt>MediaUseCase</tt>
     * @return an array of <tt>CaptureDeviceInfo</tt> describing the video
     *         capture devices available through this
     *         <tt>DeviceConfiguration</tt>
     */
    public List<CaptureDeviceInfo> getAvailableVideoCaptureDevices(
            MediaUseCase useCase)
    {
        Format[] formats
            = new Format[]
                    {
                        new AVFrameFormat(),
                        new VideoFormat(VideoFormat.RGB),
                        new VideoFormat(VideoFormat.YUV),
                        new VideoFormat(Constants.H264)
                    };
        Set<CaptureDeviceInfo> videoCaptureDevices
            = new HashSet<CaptureDeviceInfo>();

        for (Format format : formats)
        {
            @SuppressWarnings("unchecked")
            Vector<CaptureDeviceInfo> cdis
                = CaptureDeviceManager.getDeviceList(format);

            if (useCase != MediaUseCase.ANY)
            {
                for (CaptureDeviceInfo cdi : cdis)
                {
                    MediaUseCase cdiUseCase
                        = ImgStreamingSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                                    cdi.getLocator().getProtocol())
                            ? MediaUseCase.DESKTOP
                            : MediaUseCase.CALL;

                    if (cdiUseCase.equals(useCase))
                        videoCaptureDevices.add(cdi);
                }
            }
            else
            {
                videoCaptureDevices.addAll(cdis);
            }
        }

        return new ArrayList<CaptureDeviceInfo>(videoCaptureDevices);
    }

    /**
     * Returns a device that we could use for video capture.
     *
     * @param useCase <tt>MediaUseCase</tt> that will determined device
     * we will use
     * @return the CaptureDeviceInfo of a device that we could use for video
     *         capture.
     */
    public CaptureDeviceInfo getVideoCaptureDevice(MediaUseCase useCase)
    {
        CaptureDeviceInfo dev = null;

        switch (useCase)
        {
        case ANY:
        case CALL:
            dev = videoCaptureDevice;
            break;
        case DESKTOP:
            List<CaptureDeviceInfo> devs
                = getAvailableVideoCaptureDevices(MediaUseCase.DESKTOP);

            if (devs.size() > 0)
                dev = devs.get(0);
            break;
        default:
            break;
        }

        return dev;
    }

    /**
     * Sets the device which is to be used by this
     * <tt>DeviceConfiguration</tt> for video capture.
     *
     * @param device a <tt>CaptureDeviceInfo</tt> describing device to be
     *            used by this <tt>DeviceConfiguration</tt> for video
     *            capture.
     * @param save whether we will save this option or not.
     */
    public void setVideoCaptureDevice(CaptureDeviceInfo device, boolean save)
    {
        if (videoCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = videoCaptureDevice;

            videoCaptureDevice = device;

            if (save)
            {
                ConfigurationService cfg
                    = NeomediaActivator.getConfigurationService();

                cfg.setProperty(
                        PROP_VIDEO_DEVICE,
                        (videoCaptureDevice == null)
                            ? NoneAudioSystem.LOCATOR_PROTOCOL
                            : videoCaptureDevice.getName());
            }

            firePropertyChange(VIDEO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    /**
     * @return the audioNotifyDevice
     */
    public CaptureDeviceInfo getAudioNotifyDevice()
    {
        AudioSystem audioSystem = getAudioSystem();

        return (audioSystem == null) ? null : audioSystem.getNotifyDevice();
    }

    /**
     * Sets the indicator which determines whether echo cancellation is to be
     * performed for captured audio.
     *
     * @param echoCancel <tt>true</tt> if echo cancellation is to be performed
     * for captured audio; otherwise, <tt>false</tt>
     */
    public void setEchoCancel(boolean echoCancel)
    {
        NeomediaActivator.getConfigurationService().setProperty(
                PROP_AUDIO_ECHOCANCEL,
                echoCancel);
    }

    /**
     * Sets the indicator which determines whether noise suppression is to be
     * performed for captured audio.
     *
     * @param denoise <tt>true</tt> if noise suppression is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public void setDenoise(boolean denoise)
    {
        NeomediaActivator.getConfigurationService().setProperty(
                PROP_AUDIO_DENOISE,
                denoise);
    }

    /**
     * Gets the indicator which determines whether echo cancellation is to be
     * performed for captured audio.
     *
     * @return <tt>true</tt> if echo cancellation is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public boolean isEchoCancel()
    {
        return
            NeomediaActivator.getConfigurationService().getBoolean(
                    PROP_AUDIO_ECHOCANCEL,
                    DEFAULT_AUDIO_ECHOCANCEL);
    }

    /**
     * Get the echo cancellation filter length (in milliseconds).
     *
     * @return echo cancel filter length in milliseconds
     */
    public long getEchoCancelFilterLengthInMillis()
    {
        return
            NeomediaActivator.getConfigurationService().getLong(
                    PROP_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS,
                    DEFAULT_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS);
    }

    /**
     * Gets the indicator which determines whether noise suppression is to be
     * performed for captured audio
     *
     * @return <tt>true</tt> if noise suppression is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public boolean isDenoise()
    {
        return
            NeomediaActivator.getConfigurationService().getBoolean(
                    PROP_AUDIO_DENOISE,
                    DEFAULT_AUDIO_DENOISE);
    }

    /**
     * Registers the custom <tt>Renderer</tt> implementations defined by class
     * name in {@link #CUSTOM_RENDERERS} with JMF.
     */
    private void registerCustomRenderers()
    {
        @SuppressWarnings("unchecked")
        Vector<String> renderers
            = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);
        boolean commit = false;

        for (String customRenderer : CUSTOM_RENDERERS)
        {
            if (customRenderer.startsWith("."))
            {
                customRenderer
                    = "net.java.sip.communicator.impl.neomedia"
                        + ".jmfext.media.renderer"
                        + customRenderer;
            }
            if ((renderers == null) || !renderers.contains(customRenderer))
            {
                try
                {
                    Renderer customRendererInstance
                        = (Renderer)
                            Class.forName(customRenderer).newInstance();

                    PlugInManager.addPlugIn(
                            customRenderer,
                            customRendererInstance.getSupportedInputFormats(),
                            null,
                            PlugInManager.RENDERER);
                    commit = true;
                }
                catch (Throwable t)
                {
                    logger.error(
                            "Failed to register custom Renderer "
                                 + customRenderer
                                 + " with JMF.",
                             t);
                }
            }
        }

        /*
         * Just in case, bubble our JMF contributions at the top so that they
         * are considered preferred.
         */
        int pluginType = PlugInManager.RENDERER;
        @SuppressWarnings("unchecked")
        Vector<String> plugins
            = PlugInManager.getPlugInList(null, null, pluginType);

        if (plugins != null)
        {
            int pluginCount = plugins.size();
            int pluginBeginIndex = 0;
            String preferred = "net.java.sip.communicator.impl.neomedia.";

            for (int pluginIndex = pluginCount - 1;
                 pluginIndex >= pluginBeginIndex;)
            {
                String plugin = plugins.get(pluginIndex);

                if (plugin.startsWith(preferred))
                {
                    plugins.remove(pluginIndex);
                    plugins.add(0, plugin);
                    pluginBeginIndex++;
                    commit = true;
                }
                else
                    pluginIndex--;
            }
            PlugInManager.setPlugInList(plugins, pluginType);
            if (logger.isTraceEnabled())
                logger.trace("Reordered plug-in list:" + plugins);
        }

        if (commit && !NeomediaActivator.isJmfRegistryDisableLoad())
        {
            try
            {
                PlugInManager.commit();
            }
            catch (IOException ioex)
            {
                logger.warn(
                        "Failed to commit changes to the JMF plug-in list.");
            }
        }
    }

    /**
     * Gets the maximum allowed video bandwidth.
     *
     * @return the maximum allowed video bandwidth. The default value is
     * {@link #DEFAULT_VIDEO_MAX_BANDWIDTH}.
     */
    public int getVideoMaxBandwidth()
    {
        if(videoMaxBandwidth == -1)
        {
            videoMaxBandwidth
                = NeomediaActivator.getConfigurationService().getInt(
                        PROP_VIDEO_MAX_BANDWIDTH,
                        DEFAULT_VIDEO_MAX_BANDWIDTH);
        }
        return videoMaxBandwidth;
    }

    /**
     * Sets and stores the maximum allowed video bandwidth.
     *
     * @param videoMaxBandwidth the maximum allowed video bandwidth
     */
    public void setVideoMaxBandwidth(int videoMaxBandwidth)
    {
        this.videoMaxBandwidth = videoMaxBandwidth;

        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        if(videoMaxBandwidth != DEFAULT_VIDEO_MAX_BANDWIDTH)
            cfg.setProperty(PROP_VIDEO_MAX_BANDWIDTH, videoMaxBandwidth);
        else
            cfg.removeProperty(PROP_VIDEO_MAX_BANDWIDTH);
    }

    /**
     * Gets the frame rate set on this <tt>DeviceConfiguration</tt>.
     *
     * @return the frame rate set on this <tt>DeviceConfiguration</tt>. The
     * default value is {@link #DEFAULT_FRAME_RATE}
     */
    public int getFrameRate()
    {
        if(frameRate == -1)
        {
            frameRate
                = NeomediaActivator.getConfigurationService().getInt(
                        PROP_VIDEO_FRAMERATE,
                        DEFAULT_VIDEO_FRAMERATE);
        }
        return frameRate;
    }

    /**
     * Sets and stores the frame rate.
     *
     * @param frameRate the frame rate to be set on this
     * <tt>DeviceConfiguration</tt>
     */
    public void setFrameRate(int frameRate)
    {
        this.frameRate = frameRate;

        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        if(frameRate != DEFAULT_VIDEO_FRAMERATE)
            cfg.setProperty(PROP_VIDEO_FRAMERATE, frameRate);
        else
            cfg.removeProperty(PROP_VIDEO_FRAMERATE);
    }

    /**
     * Gets the video size set on this <tt>DeviceConfiguration</tt>.
     *
     * @return the video size set on this <tt>DeviceConfiguration</tt>
     */
    public Dimension getVideoSize()
    {
        if(videoSize == null)
        {
            ConfigurationService cfg
                = NeomediaActivator.getConfigurationService();
            int height = cfg.getInt(PROP_VIDEO_HEIGHT, DEFAULT_VIDEO_HEIGHT);
            int width = cfg.getInt(PROP_VIDEO_WIDTH, DEFAULT_VIDEO_WIDTH);

            videoSize = new Dimension(width, height);
        }
        return videoSize;
    }

    /**
     * Sets and stores the video size.
     *
     * @param videoSize the video size to be set on this
     * <tt>DeviceConfiguration</tt>
     */
    public void setVideoSize(Dimension videoSize)
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        if((videoSize.getHeight() != DEFAULT_VIDEO_HEIGHT)
                || (videoSize.getWidth() != DEFAULT_VIDEO_WIDTH))
        {
            cfg.setProperty(PROP_VIDEO_HEIGHT, videoSize.height);
            cfg.setProperty(PROP_VIDEO_WIDTH, videoSize.width);
        }
        else
        {
            cfg.removeProperty(PROP_VIDEO_HEIGHT);
            cfg.removeProperty(PROP_VIDEO_WIDTH);
        }

        this.videoSize = videoSize;

        firePropertyChange(
                VIDEO_CAPTURE_DEVICE,
                videoCaptureDevice, videoCaptureDevice);
    }

    /**
     * Listens for changes in the configuration and if such happen
     * we reset local values so next time we will update from
     * the configuration.
     *
     * @param event the property change event
     */
    public void propertyChange(PropertyChangeEvent event)
    {
        String propertyName = event.getPropertyName();

        if (AudioSystem.PROP_CAPTURE_DEVICE.equals(propertyName)
                || AudioSystem.PROP_NOTIFY_DEVICE.equals(propertyName)
                || AudioSystem.PROP_PLAYBACK_DEVICE.equals(propertyName))
        {
            firePropertyChange(
                    propertyName,
                    event.getOldValue(),
                    event.getNewValue());
        }
        else if (DeviceSystem.PROP_DEVICES.equals(propertyName))
        {
            if (event.getSource() instanceof AudioSystem)
                firePropertyChange(
                        PROP_AUDIO_SYSTEM_DEVICES,
                        event.getOldValue(),
                        event.getNewValue());
        }
        else if (PROP_VIDEO_FRAMERATE.equals(propertyName))
        {
            frameRate = -1;
        }
        else if (PROP_VIDEO_HEIGHT.equals(propertyName)
                || PROP_VIDEO_WIDTH.equals(propertyName))
        {
            videoSize = null;
        }
        else if (PROP_VIDEO_MAX_BANDWIDTH.equals(propertyName))
        {
            videoMaxBandwidth = -1;
        }
    }

    /**
     * Detects audio capture devices configured through JMF and disable audio if
     * none was found.
     */
    private void extractConfiguredAudioCaptureDevices()
    {
        if (logger.isInfoEnabled())
            logger.info("Looking for configured audio devices.");

        AudioSystem[] availableAudioSystems = getAvailableAudioSystems();

        if ((availableAudioSystems != null)
                && (availableAudioSystems.length != 0))
        {
            AudioSystem audioSystem = getAudioSystem();

            if (audioSystem != null)
            {
                boolean audioSystemIsAvailable = false;

                for (AudioSystem availableAudioSystem : availableAudioSystems)
                {
                    if (availableAudioSystem.equals(audioSystem))
                    {
                        audioSystemIsAvailable = true;
                        break;
                    }
                }
                if (!audioSystemIsAvailable)
                    audioSystem = null;
            }

            if (audioSystem == null)
            {
                ConfigurationService cfg
                    = NeomediaActivator.getConfigurationService();

                if (cfg != null)
                {
                    String locatorProtocol = cfg.getString(PROP_AUDIO_SYSTEM);

                    if (locatorProtocol != null)
                    {
                        for (AudioSystem availableAudioSystem
                                : availableAudioSystems)
                        {
                            if (locatorProtocol.equalsIgnoreCase(
                                    availableAudioSystem.getLocatorProtocol()))
                            {
                                audioSystem = availableAudioSystem;
                                break;
                            }
                        }
                    }
                }

                if (audioSystem == null)
                    audioSystem = availableAudioSystems[0];

                setAudioSystem(audioSystem, false);
            }
        }
    }

    /**
     * Detects video capture devices configured through JMF and disable video if
     * none was found.
     */
    private void extractConfiguredVideoCaptureDevices()
    {
        if (NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                NeomediaActivator.getConfigurationService().getString(
                        PROP_VIDEO_DEVICE)))
        {
            videoCaptureDevice = null;
        }
        else
        {
            if (logger.isInfoEnabled())
                logger.info("Scanning for configured Video Devices.");

            Format[] formats
                = new Format[]
                        {
                            new AVFrameFormat(),
                            new VideoFormat(VideoFormat.RGB),
                            new VideoFormat(VideoFormat.YUV),
                            new VideoFormat(Constants.H264)
                        };

            for (Format format : formats)
            {
                videoCaptureDevice
                    = extractConfiguredVideoCaptureDevice(format);
                if (videoCaptureDevice != null)
                    break;
            }
            if ((videoCaptureDevice == null) && logger.isInfoEnabled())
                logger.info("No Video Device was found.");
        }
    }
}
