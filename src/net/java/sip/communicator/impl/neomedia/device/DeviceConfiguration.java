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
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.audio.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * This class aims to provide a simple configuration interface for JMF. It
 * retrieves stored configuration when started or listens to ConfigurationEvent
 * for property changes and configures the JMF accordingly.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
@SuppressWarnings("unchecked")
public class DeviceConfiguration
    extends PropertyChangeNotifier
{

    /**
     * The name of the <code>DeviceConfiguration</code> property which
     * represents the device used by <code>DeviceConfiguration</code> for audio
     * capture.
     */
    public static final String AUDIO_CAPTURE_DEVICE = "AUDIO_CAPTURE_DEVICE";

    /**
     * The name of the <code>DeviceConfiguration</code> property which
     * represents the device used by <code>DeviceConfiguration</code> for audio
     * playback.
     */
    public static final String AUDIO_PLAYBACK_DEVICE = "AUDIO_PLAYBACK_DEVICE";

    /**
     * The name of the <code>DeviceConfiguration</code> property which
     * represents the device used by <code>DeviceConfiguration</code> for audio
     * notify.
     */
    public static final String AUDIO_NOTIFY_DEVICE = "AUDIO_NOTIFY_DEVICE";

    /**
     * The name of the <code>DeviceConfiguration</code> property which
     * represents the device used by <code>DeviceConfiguration</code> for video
     * capture.
     */
    public static final String VIDEO_CAPTURE_DEVICE = "VIDEO_CAPTURE_DEVICE";

    /**
     * When audio is disabled the selected audio system is with name None.
     */
    public static final String AUDIO_SYSTEM_NONE = "None";

    /**
     * JavaSound sound system.
     */
    public static final String AUDIO_SYSTEM_JAVASOUND = "JavaSound";

    /**
     * PortAudio sound system.
     */
    public static final String AUDIO_SYSTEM_PORTAUDIO = "PortAudio";

    /**
     * Property used to store the capture device.
     */
    private static final String PROP_AUDIO_DEVICE =
        "net.java.sip.communicator.impl.neomedia.capturedev";

    /**
     * Property used to store the playback device.
     */
    private static final String PROP_AUDIO_PLAYBACK_DEVICE =
        "net.java.sip.communicator.impl.neomedia.playbackdev";

    /**
     * Property used to store the notify device.
     */
    private static final String PROP_AUDIO_NOTIFY_DEVICE =
        "net.java.sip.communicator.impl.neomedia.notifydev";

    /**
     * Property used to store is audio enabled or disabled.
     */
    private static final String PROP_AUDIO_DEVICE_IS_DISABLED =
        "net.java.sip.communicator.impl.neomedia.audiodevIsDisabled";

    /**
     * Property used to store the video device we use.
     */
    private static final String PROP_VIDEO_DEVICE =
        "net.java.sip.communicator.impl.neomedia.videodev";

    /**
     * Property used to store is video enabled or disabled.
     */
    private static final String PROP_VIDEO_DEVICE_IS_DISABLED =
        "net.java.sip.communicator.impl.neomedia.videodevIsDisabled";

    /**
     * The name of the <tt>boolean</tt> property which determines whether echo
     * cancellation is to be performed for captured audio.
     */
    static final String PROP_AUDIO_ECHOCANCEL
        = "net.java.sip.communicator.impl.neomedia.echocancel";

    /**
     * The default value to be used for the {@link #PROP_AUDIO_ECHOCANCEL}
     * property when it does not have a value.
     */
    private static final boolean DEFAULT_AUDIO_ECHOCANCEL = true;

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
     * The name of the <tt>boolean</tt> property which determines whether noise
     * suppression is to be performed for captured audio.
     */
    static final String PROP_AUDIO_DENOISE
        = "net.java.sip.communicator.impl.neomedia.denoise";

    /**
     * The default value to be used for the {@link #PROP_AUDIO_DENOISE} property
     * when it does not have a value.
     */
    private static final boolean DEFAULT_AUDIO_DENOISE = true;

    /**
     * The list of class names of custom <tt>Renderer</tt> implementations to be
     * registered with JMF.
     */
    private static final String[] CUSTOM_RENDERERS
        = new String[]
                {
                    "net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.video.JAWTRenderer"
                };

    /**
     * Used when no capture device is selected.
     */
    private static final CaptureDeviceInfo[] NO_CAPTURE_DEVICES =
        new CaptureDeviceInfo[0];

    /**
     * The logger.
     */
    private Logger logger = Logger.getLogger(DeviceConfiguration.class);

    /**
     * The device that we'll be using for audio capture.
     */
    private CaptureDeviceInfo audioCaptureDevice = null;

    /**
     * The device that we'll be using for audio playback.
     */
    private CaptureDeviceInfo audioPlaybackDevice = null;

    /**
     * The device that we'll be using for audio notifications.
     */
    private CaptureDeviceInfo audioNotifyDevice = null;

    /**
     * The device that we'll be using for video capture.
     */
    private CaptureDeviceInfo videoCaptureDevice;

    /**
     * The currently available audio systems.(JavaSoubnd or PortAudio).
     */
    private static Vector<String> audioSystems = new Vector<String>();

    /**
     * The audio system we use.
     */
    private String audioSystem = null;

    /**
     * Initializes capture devices.
     */
    public void initialize()
    {
        // these seem to be throwing exceptions every now and then so we'll
        // blindly catch them for now
        try
        {
            JmfDeviceDetector.detectAndConfigureCaptureDevices();
            extractConfiguredCaptureDevices();
        }
        catch (Exception ex)
        {
            logger.error("Failed to initialize media.", ex);
        }

        registerCustomRenderers();
    }

    /**
     * Detects capture devices configured through JMF and disable audio and/or
     * video transmission if none were found.
     */
    private void extractConfiguredCaptureDevices()
    {
        ConfigurationService config
            = NeomediaActivator.getConfigurationService();

        if (logger.isInfoEnabled())
            logger.info("Scanning for configured Audio Devices.");
        CaptureDeviceInfo[] audioCaptureDevices =
            getAvailableAudioCaptureDevices();
        if (config.getBoolean(PROP_AUDIO_DEVICE_IS_DISABLED, false))
        {
            audioCaptureDevice = null;
            audioSystem = AUDIO_SYSTEM_NONE;
        }
        else if (audioCaptureDevices.length < 1)
        {
            logger.warn("No Audio Device was found.");
            audioCaptureDevice = null;
            audioSystem = AUDIO_SYSTEM_NONE;
        }
        else
        {
            if (logger.isDebugEnabled())
                logger.debug("Found " + audioCaptureDevices.length
                + " capture devices: " + audioCaptureDevices);

            String audioDevName = config.getString(PROP_AUDIO_DEVICE);

            if(audioDevName == null)
            {
                // the default behaviour if nothing set is to use PortAudio
                // this will also choose the capture device
                if(PortAudioAuto.isSupported())
                {
                    setAudioSystem(AUDIO_SYSTEM_PORTAUDIO, null, false);
                }
                else
                {
                    setAudioPlaybackDevice(null, false);
                    setAudioNotifyDevice(null, false);
                    setAudioCaptureDevice(null, false);
                    setAudioSystem(AUDIO_SYSTEM_JAVASOUND, null, false);
                }
            }
            else
            {
                for (CaptureDeviceInfo captureDeviceInfo : audioCaptureDevices)
                {
                    if (audioDevName.equals(captureDeviceInfo.getName()))
                    {
                        setAudioSystem(getAudioSystem(captureDeviceInfo),
                            captureDeviceInfo, false);
                        break;
                    }
                }

                if(getAudioSystem() == null || !PortAudioAuto.isSupported())
                {
                    logger.warn("Computer sound config changed or " +
                        "there is a problem since last config was saved, " +
                        "will back to default");
                    setAudioPlaybackDevice(null, false);
                    setAudioNotifyDevice(null, false);
                    setAudioCaptureDevice(null, false);
                    setAudioSystem(AUDIO_SYSTEM_PORTAUDIO, null, false);
                }
            }
            if (audioCaptureDevice != null)
                if (logger.isInfoEnabled())
                    logger.info("Found " + audioCaptureDevice.getName()
                    + " as an audio capture device.");
        }

        if (config.getBoolean(PROP_VIDEO_DEVICE_IS_DISABLED, false))
            videoCaptureDevice = null;
        else
        {
            if (logger.isInfoEnabled())
                logger.info("Scanning for configured Video Devices.");

            Format[] formats
                = new Format[]
                        {
                            new AVFrameFormat(),
                            new VideoFormat(VideoFormat.RGB),
                            new VideoFormat(VideoFormat.YUV)
                        };

            for (Format format : formats)
            {
                videoCaptureDevice
                    = extractConfiguredVideoCaptureDevice(format);
                if (videoCaptureDevice != null)
                    break;
            }
            if (videoCaptureDevice == null)
                if (logger.isInfoEnabled())
                    logger.info("No Video Device was found.");
        }
    }

    /**
     * Returns the configured video capture device with the specified
     * output format.
     * @param format the output format of the video format.
     * @return CaptureDeviceInfo for the video device.
     */
    private CaptureDeviceInfo extractConfiguredVideoCaptureDevice(Format format)
    {
        List<CaptureDeviceInfo> videoCaptureDevices
            = CaptureDeviceManager.getDeviceList(format);
        CaptureDeviceInfo videoCaptureDevice = null;

        if (videoCaptureDevices.size() > 0)
        {
            String videoDevName
                = NeomediaActivator.getConfigurationService()
                        .getString(PROP_VIDEO_DEVICE);

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

            if (videoCaptureDevice != null)
            {
                if (logger.isInfoEnabled())
                    logger.info(
                        "Found "
                            + videoCaptureDevice.getName()
                            + " as a "
                            + format
                            + " Video Device.");
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
        return audioCaptureDevice;
    }

    /**
     * Gets the list of audio capture devices which are available through this
     * <code>DeviceConfiguration</code>, amongst which is
     * {@link #getAudioCaptureDevice()} and represent acceptable values
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @return an array of <code>CaptureDeviceInfo</code> describing the audio
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableAudioCaptureDevices()
    {
        Vector<CaptureDeviceInfo> audioCaptureDevices =
            CaptureDeviceManager.getDeviceList(new AudioFormat(
                AudioFormat.LINEAR, -1, 16, -1));

        return audioCaptureDevices.toArray(NO_CAPTURE_DEVICES);
    }

    /**
     * Gets the list of audio capture devices which are available through this
     * <code>DeviceConfiguration</code>, amongst which is
     * {@link #getAudioCaptureDevice()} and represent acceptable values
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @param soundSystem
     *         filter capture devices only from the supplied audio system.
     * @return an array of <code>CaptureDeviceInfo</code> describing the audio
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableAudioCaptureDevices(
            String soundSystem)
    {
        String protocol = null;

        if(soundSystem.equals(AUDIO_SYSTEM_JAVASOUND))
            protocol = "javasound";
        else if(soundSystem.equals(AUDIO_SYSTEM_PORTAUDIO))
            protocol = "portaudio";

        Vector<CaptureDeviceInfo> res = new Vector<CaptureDeviceInfo>();

        if(protocol != null)
        {
            for(CaptureDeviceInfo cDeviceInfo
                    : getAvailableAudioCaptureDevices())
            {
                if(cDeviceInfo.getLocator().getProtocol().equals(protocol))
                    res.add(cDeviceInfo);
            }
        }
        return res.toArray(NO_CAPTURE_DEVICES);
    }

    /**
     * Lists all the playback devices. These are only portaudio devices
     * as we can only set particular device for playback when using portaudio.
     *
     * @return the devices that can be used for playback.
     */
    public CaptureDeviceInfo[] getAvailableAudioPlaybackDevices()
    {
        return PortAudioAuto.playbackDevices;
    }

    /**
     * Gets the list of video capture devices which are available through this
     * <code>DeviceConfiguration</code>, amongst which is
     * {@link #getVideoCaptureDevice(MediaUseCase)} and represent acceptable
     * values for {@link #setVideoCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @param useCase extract video capture devices that correspond to this
     * <tt>MediaUseCase</tt>
     * @return an array of <code>CaptureDeviceInfo</code> describing the video
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableVideoCaptureDevices(
            MediaUseCase useCase)
    {
        Format[] formats
            = new Format[]
                    {
                        new AVFrameFormat(),
                        new VideoFormat(VideoFormat.RGB),
                        new VideoFormat(VideoFormat.YUV)
                    };
        Set<CaptureDeviceInfo> videoCaptureDevices =
            new HashSet<CaptureDeviceInfo>();

        for (Format format : formats)
        {
            Vector<CaptureDeviceInfo> captureDeviceInfos =
                CaptureDeviceManager.getDeviceList(format);

            if(useCase != MediaUseCase.ANY)
            {
                for(CaptureDeviceInfo dev : captureDeviceInfos)
                {
                    if(useCase == MediaUseCase.CALL &&
                            !dev.getLocator().getProtocol().equals(
                                    ImageStreamingAuto.LOCATOR_PROTOCOL))
                    {
                        // add only non-desktop capture device
                        videoCaptureDevices.add(dev);
                    }
                    else if(useCase == MediaUseCase.DESKTOP &&
                            dev.getLocator().getProtocol().equals(
                                    ImageStreamingAuto.LOCATOR_PROTOCOL))
                    {
                        // add only desktop streaming devices
                        videoCaptureDevices.add(dev);
                    }
                }
            }
            else
            {
                videoCaptureDevices.addAll(captureDeviceInfos);
            }
        }

        return videoCaptureDevices.toArray(NO_CAPTURE_DEVICES);
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

        switch(useCase)
        {
        case ANY:
        case CALL:
            dev = videoCaptureDevice;
            break;
        case DESKTOP:
            CaptureDeviceInfo devs[] =
                getAvailableVideoCaptureDevices(MediaUseCase.DESKTOP);

            if(devs.length > 0)
                dev = devs[0];
            break;
        default:
            break;
        }

        return dev;
    }

    /**
     * Sets the device which is to be used by this
     * <code>DeviceConfiguration</code> for video capture.
     *
     * @param device a <code>CaptureDeviceInfo</code> describing device to be
     *            used by this <code>DeviceConfiguration</code> for video
     *            capture.
     * @param save whether we will save this option or not.
     */
    public void setVideoCaptureDevice(CaptureDeviceInfo device, boolean save)
    {
        if (videoCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = videoCaptureDevice;

            videoCaptureDevice = device;

            if(save)
            {
                ConfigurationService config
                    = NeomediaActivator.getConfigurationService();
                config.setProperty(PROP_VIDEO_DEVICE_IS_DISABLED,
                    videoCaptureDevice == null);
                if (videoCaptureDevice != null)
                    config.setProperty(PROP_VIDEO_DEVICE, videoCaptureDevice
                        .getName());
            }

            firePropertyChange(VIDEO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    /**
     * Sets the device which is to be used by this
     * <code>DeviceConfiguration</code> for audio capture.
     *
     * @param device a <code>CaptureDeviceInfo</code> describing the device to
     *            be used by this <code>DeviceConfiguration</code> for audio
     *            capture
     * @param save whether we will save this option or not.
     */
    public void setAudioCaptureDevice(CaptureDeviceInfo device, boolean save)
    {
        if (audioCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = audioCaptureDevice;

            audioCaptureDevice = device;

            if(save)
            {
                NeomediaActivator
                    .getConfigurationService()
                        .setProperty(
                            PROP_AUDIO_DEVICE,
                            (audioCaptureDevice == null)
                                ? null
                                : audioCaptureDevice.getName());
            }

            firePropertyChange(AUDIO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    /**
     * Enable or disable Audio stream transmission.
     *
     * @return true if audio capture is supported and false otherwise.
     */
    public boolean isAudioCaptureSupported()
    {
        return this.audioCaptureDevice != null;
    }

    /**
     * Enable or disable Video stream transmission.
     *
     * @return true if audio capture is supported and false otherwise.
     */
    public boolean isVideoCaptureSupported()
    {
        return this.videoCaptureDevice != null;
    }

    /**
     * Return the installed Audio Systems.
     * @return the audio systems names.
     */
    public String[] getAvailableAudioSystems()
    {
        return audioSystems.toArray(new String[0]);
    }

    /**
     * Adds audio system.
     * @param audioSystemName the name of the audio system.
     */
    public static void addAudioSystem(String audioSystemName)
    {
        audioSystems.add(audioSystemName);
    }

    /**
     * The current selected audio system.
     * @return the name of the current audio system.
     */
    public String getAudioSystem()
    {
        return audioSystem;
    }

    /**
     * Extracts the audio system for the given device info.
     * @param cdi the device
     * @return the audio system used by the device.
     */
    private String getAudioSystem(CaptureDeviceInfo cdi)
    {
        String res = null;
        // Here we iterate over the available audio systems
        // to be sure that the audio system
        // is available and enabled on the system we are running on
        if(cdi.getLocator().getProtocol().equals("javasound"))
        {
            Iterator<String> iter = audioSystems.iterator();
            while (iter.hasNext())
            {
                String asName = iter.next();
                if(asName.equals(AUDIO_SYSTEM_JAVASOUND))
                    res = asName;
            }
        }
        else if(cdi.getLocator().getProtocol().equals("portaudio"))
        {
            Iterator<String> iter = audioSystems.iterator();
            while (iter.hasNext())
            {
                String asName = iter.next();
                if(asName.equals(AUDIO_SYSTEM_PORTAUDIO))
                    res = asName;
            }
        }
        return (res == null) ? AUDIO_SYSTEM_NONE : res;
    }

    /**
     * Changes the current audio system.
     * When javasound is selected we also change the capture device.
     *
     * @param name the name of the audio system.
     * @param captureDevice the selected capture device, if is null we will
     *        choose a default one. Param used when first time initing and
     *        extracting config.
     * @param save whether we will save this new option or not.
     */
    public void setAudioSystem(String name, CaptureDeviceInfo captureDevice,
        boolean save)
    {
        ConfigurationService config
            = NeomediaActivator.getConfigurationService();

        audioSystem = name;

        if(name.equals(AUDIO_SYSTEM_NONE))
        {
            setAudioCaptureDevice(null, save);
            setAudioNotifyDevice(null, save);
            setAudioPlaybackDevice(null, save);
        }
        else if(name.equals(AUDIO_SYSTEM_JAVASOUND))
        {
            setAudioNotifyDevice(null, save);
            setAudioPlaybackDevice(null, save);

            // as there is only one device for javasound
            // lets search for it
            if(captureDevice != null)
                setAudioCaptureDevice(captureDevice, save);
            else
            {
                CaptureDeviceInfo[] audioCaptureDevices =
                    getAvailableAudioCaptureDevices();
                for (CaptureDeviceInfo captureDeviceInfo : audioCaptureDevices)
                {
                    if(captureDeviceInfo.getLocator().getProtocol().
                        equals("javasound"))
                    {
                        setAudioCaptureDevice(captureDeviceInfo, save);
                        break;
                    }
                }
            }

            // if we have inited the audiocaptureDevice, it means javasound is
            // available and everything is ok
            if (audioCaptureDevice != null)
            {
                removePortAudioRenderer();
                initJavaSoundRenderer();
            }
        }
        else if(name.equals(AUDIO_SYSTEM_PORTAUDIO))
        {
            // first get any config before we change it
            String audioNotifyDevName =
                config.getString(PROP_AUDIO_NOTIFY_DEVICE);

            String audioPlaybackDevName =
                config.getString(PROP_AUDIO_PLAYBACK_DEVICE);

            if(logger.isDebugEnabled())
            {
                logger.debug("Portaudio: Found stored notify device: ["
                        + audioNotifyDevName + "]");
                logger.debug("Portaudio: Found stored playback device: ["
                        + audioPlaybackDevName + "]");
            }

            // changed to portaudio, so lets first set the default devices
            setAudioPlaybackDevice(PortAudioAuto.defaultPlaybackDevice, save);
            setAudioNotifyDevice(PortAudioAuto.defaultPlaybackDevice, save);

            // capture device is not null when we are called for the
            // first time, we will also extract playback devices here
            if(captureDevice != null)
            {
                this.audioCaptureDevice = captureDevice;
            }
            else // no capture device specified save default
                setAudioCaptureDevice(PortAudioAuto.defaultCaptureDevice, save);

            if(audioNotifyDevName != null)
            {
                for (CaptureDeviceInfo captureDeviceInfo :
                        PortAudioAuto.playbackDevices)
                {
                    if (audioNotifyDevName.equals(
                            captureDeviceInfo.getName()))
                    {
                        setAudioNotifyDevice(captureDeviceInfo, save);
                        break;
                    }
                }
            }

            if(audioPlaybackDevName != null)
            {
                for (CaptureDeviceInfo captureDeviceInfo :
                        PortAudioAuto.playbackDevices)
                {
                    if (audioPlaybackDevName.equals(
                            captureDeviceInfo.getName()))
                    {
                        setAudioPlaybackDevice(captureDeviceInfo, save);
                        break;
                    }
                }
            }

            // return here to prevent clearing the last config that was saved
            return;
        }
        else
        {
            // not expected behaviour
            logger.error("Unknown audio system! Name:" + name);
            audioSystem = null;
        }

        if(save)
        {
            config.setProperty(
                    PROP_AUDIO_DEVICE_IS_DISABLED,
                    audioCaptureDevice == null);
        }
    }

    /**
     * Removed portaudio renderer.
     */
    private void removePortAudioRenderer()
    {
        PlugInManager.removePlugIn(
        "net.java.sip.communicator.impl.neomedia.jmfext" +
                ".media.renderer.audio.PortAudioRenderer",
        PlugInManager.RENDERER);
    }

    /**
     * Registers javasound renderer.
     */
    private void initJavaSoundRenderer()
    {
        try
        {
            PlugInManager.addPlugIn(
                "com.sun.media.renderer.audio.JavaSoundRenderer",
                new com.sun.media.renderer.audio.JavaSoundRenderer()
                        .getSupportedInputFormats(),
                null,
                PlugInManager.RENDERER);
        }
        catch (Exception e)
        {
            // if class is missing
            logger.error("Problem init javasound renderer", e);
        }
    }

    /**
     * @return the audioPlaybackDevice
     */
    public CaptureDeviceInfo getAudioPlaybackDevice()
    {
        return audioPlaybackDevice;
    }

    /**
     * @return the audioNotifyDevice
     */
    public CaptureDeviceInfo getAudioNotifyDevice()
    {
        return audioNotifyDevice;
    }

    /**
     * Set audio playback device.
     * @param audioPlaybackDevice the audioPlaybackDevice to set.
     * @param save whether we will save this option or not.
     */
    public void setAudioPlaybackDevice(CaptureDeviceInfo audioPlaybackDevice,
                                       boolean save)
    {
        if(this.audioPlaybackDevice != audioPlaybackDevice)
        {
            CaptureDeviceInfo oldDev = this.audioPlaybackDevice;

            this.audioPlaybackDevice = audioPlaybackDevice;

            if (this.audioPlaybackDevice != null)
            {
                /*
                 * The audioPlaybackDevice is non-null only for PortAudio for
                 * now i.e. we currently want to use PortAudio instead of
                 * JavaSound. So we have to disable JavaSound and enable
                 * PortAudio.
                 */
                PlugInManager.removePlugIn(
                    "com.sun.media.renderer.audio.JavaSoundRenderer",
                    PlugInManager.RENDERER);

                PortAudioRenderer.setDefaultLocator(
                        this.audioPlaybackDevice.getLocator());
                PlugInManager.addPlugIn(
                        "net.java.sip.communicator.impl.neomedia.jmfext.media"
                            + ".renderer.audio.PortAudioRenderer",
                        new PortAudioRenderer().getSupportedInputFormats(),
                        null,
                        PlugInManager.RENDERER);
            }

            if(save)
            {
                ConfigurationService config
                    = NeomediaActivator.getConfigurationService();

                if (audioPlaybackDevice != null)
                {
                    config.setProperty(PROP_AUDIO_PLAYBACK_DEVICE,
                        audioPlaybackDevice.getName());

                    config.setProperty(PROP_AUDIO_DEVICE_IS_DISABLED, false);
                }
                else
                    config.setProperty(PROP_AUDIO_PLAYBACK_DEVICE, null);
            }

            firePropertyChange(AUDIO_PLAYBACK_DEVICE,
                oldDev, audioPlaybackDevice);
        }
    }

    /**
     * Sets the notify device.
     * @param audioNotifyDevice the audioNotifyDevice to set
     * @param save whether we will save this option or not.
     */
    public void setAudioNotifyDevice(CaptureDeviceInfo audioNotifyDevice,
                                    boolean save)
    {
        if(this.audioNotifyDevice != audioNotifyDevice)
        {
            CaptureDeviceInfo oldDev = this.audioNotifyDevice;
            this.audioNotifyDevice = audioNotifyDevice;

            if(save)
            {
                ConfigurationService config
                    = NeomediaActivator.getConfigurationService();

                if (audioNotifyDevice != null)
                {
                    config.setProperty(PROP_AUDIO_NOTIFY_DEVICE,
                        audioNotifyDevice.getName());

                    // atleast notify or playback must be set to consider
                    // portaudio for enabled
                    config.setProperty(PROP_AUDIO_DEVICE_IS_DISABLED, false);
                }
                else
                    config.setProperty(PROP_AUDIO_NOTIFY_DEVICE, null);
            }

            firePropertyChange(AUDIO_NOTIFY_DEVICE,
                oldDev, audioNotifyDevice);
        }
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
        Vector<String> renderers
            = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);
        boolean commit = false;

        for (String customRenderer : CUSTOM_RENDERERS)
        {
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
                                 + " with JMF.");
                }
            }
        }

        /*
         * Just in case, bubble our JMF contributions at the top so that they
         * are considered preferred.
         */
        int pluginType = PlugInManager.RENDERER;
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

        if (commit)
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
}
