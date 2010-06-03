/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.device;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.media.protocol.portaudio.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * This class aims to provide a simple configuration interface for JMF. It
 * retrieves stored configuration when started or listens to ConfigurationEvent
 * for property changes and configures the JMF accordingly.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lubomir Marinov
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

    private static final String PROP_AUDIO_DEVICE =
        "net.java.sip.communicator.impl.media.audiodev";

    private static final String PROP_AUDIO_PLAYBACK_DEVICE =
        "net.java.sip.communicator.impl.media.audio.playbackdev";

    private static final String PROP_AUDIO_NOTIFY_DEVICE =
        "net.java.sip.communicator.impl.media.audio.notifydev";

    private static final String PROP_AUDIO_DEVICE_IS_DISABLED =
        "net.java.sip.communicator.impl.media.audiodevIsDisabled";

    private static final String PROP_VIDEO_DEVICE =
        "net.java.sip.communicator.impl.media.videodev";

    private static final String PROP_VIDEO_DEVICE_IS_DISABLED =
        "net.java.sip.communicator.impl.media.videodevIsDisabled";

    private static final String PROP_AUDIO_ECHOCANCEL_ENABLED =
        "net.java.sip.communicator.impl.media.audio.echocancel";

    private static final String PROP_AUDIO_ECHOCANCEL_TAIL =
        "net.java.sip.communicator.impl.media.audio.echocancel.tail";

    private static final String PROP_AUDIO_DENOISE_ENABLED =
        "net.java.sip.communicator.impl.media.audio.denoise";

    private static final String PROP_AUDIO_LATENCY =
        "net.java.sip.communicator.impl.media.audio.latency";

    private static final CaptureDeviceInfo[] NO_CAPTURE_DEVICES =
        new CaptureDeviceInfo[0];

    private Logger logger = Logger.getLogger(DeviceConfiguration.class);

    /**
     * The device that we'll be using for audio capture.
     */
    private CaptureDeviceInfo audioCaptureDevice = null;

    private CaptureDeviceInfo audioPlaybackDevice = null;

    private CaptureDeviceInfo audioNotifyDevice = null;

    /**
     * The device that we'll be using for video capture.
     */
    private CaptureDeviceInfo videoCaptureDevice;

    private static Vector<String> audioSystems = new Vector<String>();

    private String audioSystem = null;

    /**
     * Default constructor.
     */
    public DeviceConfiguration()
    {
        //dummy ... XXX do we really need it though?
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
            JmfDeviceDetector.detectAndConfigureCaptureDevices();
            extractConfiguredCaptureDevices();
        }
        catch (Exception ex)
        {
            logger.error("Failed to initialize media.", ex);
        }
    }

    /**
     * Detects capture devices configured through JMF and disable audio and/or
     * video transmission if none were found. Stores found devices in
     * audioCaptureDevice and videoCaptureDevice.
     */
    private void extractConfiguredCaptureDevices()
    {
        ConfigurationService config = MediaActivator.getConfigurationService();

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
                // the default behaviour if nothing set is to use javasound
                // this will also choose the capture device
                setAudioSystem(AUDIO_SYSTEM_JAVASOUND, null);
            }
            else
            {
                for (CaptureDeviceInfo captureDeviceInfo : audioCaptureDevices)
                {
                    if (audioDevName.equals(captureDeviceInfo.getName()))
                    {
                        setAudioSystem(getAudioSystem(captureDeviceInfo),
                            captureDeviceInfo);
                        break;
                    }
                }

                if(getAudioSystem() == null)
                {
                    logger.warn("Computer sound config changed or " +
                        "there is a problem since last config was saved, " +
                        "will back to default javasound");
                    setAudioPlaybackDevice(null);
                    setAudioNotifyDevice(null);
                    setAudioCaptureDevice(null);
                    setAudioSystem(AUDIO_SYSTEM_JAVASOUND, null);
                }
            }
            if (audioCaptureDevice != null)
                if (logger.isInfoEnabled())
                    logger.info("Found " + audioCaptureDevice.getName()
                    + " as an audio capture device.");

            // now extract other sound related configs
            try
            {
                boolean echoCancelEnabled =
                    config.getBoolean(PROP_AUDIO_ECHOCANCEL_ENABLED,
                        PortAudioManager.getInstance().isEnabledEchoCancel());
                if(echoCancelEnabled)
                {
                    int echoCancelTail =
                        config.getInt(PROP_AUDIO_ECHOCANCEL_TAIL,
                            PortAudioManager.getInstance().getFilterLength());
                    PortAudioManager.getInstance().setEchoCancel(
                        echoCancelEnabled, 
                        PortAudioManager.getInstance().getFrameSize(),
                        echoCancelTail);
                }

                boolean denoiseEnabled =
                    config.getBoolean(PROP_AUDIO_DENOISE_ENABLED,
                        PortAudioManager.getInstance().isEnabledDeNoise());
                PortAudioManager.getInstance().setDeNoise(denoiseEnabled);

                // suggested latency is saved in configuration as
                // milliseconds but PortAudioManager use it as seconds
                int audioLatency = config.getInt(PROP_AUDIO_LATENCY,
                    (int)(PortAudioManager.getSuggestedLatency()*1000));
                if(audioLatency !=
                    (int)PortAudioManager.getSuggestedLatency()*1000)
                        PortAudioManager.setSuggestedLatency(
                            (double)audioLatency/1000d);
            }
            catch (Exception e)
            {
                logger.error("Error parsing audio config", e);
            }
        }

        if (config.getBoolean(PROP_VIDEO_DEVICE_IS_DISABLED, false))
            videoCaptureDevice = null;
        else
        {
            if (logger.isInfoEnabled())
                logger.info("Scanning for configured Video Devices.");
            videoCaptureDevice =
                extractConfiguredVideoCaptureDevice(VideoFormat.RGB);
            // no RGB camera found. And what about YUV ?
            if (videoCaptureDevice == null)
            {
                videoCaptureDevice =
                    extractConfiguredVideoCaptureDevice(VideoFormat.YUV);
                if (videoCaptureDevice == null)
                    if (logger.isInfoEnabled())
                        logger.info("No Video Device was found.");
            }
        }
    }

    /**
     * Returns the configured video capture device with the specified
     * output format.
     * @param format the output format of the video format.
     * @return CaptureDeviceInfo for the video device.
     */
    private CaptureDeviceInfo extractConfiguredVideoCaptureDevice(String format)
    {
        List<CaptureDeviceInfo> videoCaptureDevices =
            CaptureDeviceManager.getDeviceList(new VideoFormat(format));
        CaptureDeviceInfo videoCaptureDevice = null;

        if (videoCaptureDevices.size() > 0)
        {
            String videoDevName =
                MediaActivator.getConfigurationService().getString(
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

            if (videoCaptureDevice != null)
                if (logger.isInfoEnabled())
                    logger.info("Found " + videoCaptureDevice.getName()
                    + " as an RGB Video Device.");
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
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo)}
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
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo)}
     *
     * @param soundSystem
     *         filter capture devices only from the supplied audio system.
     *
     * @return an array of <code>CaptureDeviceInfo</code> describing the audio
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableAudioCaptureDevices(String soundSystem)
    {
        String protocol = null;
        if(soundSystem.equals(AUDIO_SYSTEM_JAVASOUND))
            protocol = "javasound";
        else if(soundSystem.equals(AUDIO_SYSTEM_PORTAUDIO))
            protocol = "portaudio";

        Vector<CaptureDeviceInfo> res = new Vector<CaptureDeviceInfo>();

        if(protocol != null)
        {
            CaptureDeviceInfo[] all = getAvailableAudioCaptureDevices();
            for(int i = 0; i < all.length; i++)
            {
                CaptureDeviceInfo cDeviceInfo = all[i];
                if(cDeviceInfo.getLocator().getProtocol().equals(protocol))
                {
                    res.add(cDeviceInfo);
                }
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
     * {@link #getVideoCaptureDevice()} and represent acceptable values
     * for {@link #setVideoCaptureDevice(CaptureDeviceInfo)}
     *
     * @return an array of <code>CaptureDeviceInfo</code> describing the video
     *         capture devices available through this
     *         <code>DeviceConfiguration</code>
     */
    public CaptureDeviceInfo[] getAvailableVideoCaptureDevices()
    {
        Set<CaptureDeviceInfo> videoCaptureDevices =
            new HashSet<CaptureDeviceInfo>();

        videoCaptureDevices.addAll(CaptureDeviceManager
            .getDeviceList(new VideoFormat(VideoFormat.RGB)));
        videoCaptureDevices.addAll(CaptureDeviceManager
            .getDeviceList(new VideoFormat(VideoFormat.YUV)));
        return videoCaptureDevices.toArray(NO_CAPTURE_DEVICES);
    }

    /**
     * Returns a device that we could use for video capture.
     *
     * @return the CaptureDeviceInfo of a device that we could use for video
     *         capture.
     */
    public CaptureDeviceInfo getVideoCaptureDevice()
    {
        return videoCaptureDevice;
    }

    /**
     * Sets the device which is to be used by this
     * <code>DeviceConfiguration</code> for video capture.
     *
     * @param device a <code>CaptureDeviceInfo</code> describing device to be
     *            used by this <code>DeviceConfiguration</code> for video
     *            capture
     */
    public void setVideoCaptureDevice(CaptureDeviceInfo device)
    {
        if (videoCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = videoCaptureDevice;

            videoCaptureDevice = device;

            ConfigurationService config =
                MediaActivator.getConfigurationService();
            config.setProperty(PROP_VIDEO_DEVICE_IS_DISABLED,
                videoCaptureDevice == null);
            if (videoCaptureDevice != null)
                config.setProperty(PROP_VIDEO_DEVICE, videoCaptureDevice
                    .getName());

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
     */
    public void setAudioCaptureDevice(CaptureDeviceInfo device)
    {
        if (audioCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = audioCaptureDevice;

            audioCaptureDevice = device;

            ConfigurationService config =
                MediaActivator.getConfigurationService();

            if (audioCaptureDevice != null)
            {
                config.setProperty(PROP_AUDIO_DEVICE, audioCaptureDevice
                    .getName());
            }
            else
                config.setProperty(PROP_AUDIO_DEVICE, null);

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

        if(res == null)
            res = AUDIO_SYSTEM_NONE;

        return res;
    }

    /**
     * Changes the current audio system.
     * When javasound is selected we also change the capture device.
     *
     * @param name the name of the audio system.
     * @param captureDevice the selected capture device, if is null we will
     *        choose a default one. Param used when first time initing and
     *        extracting config.
     */
    public void setAudioSystem(String name, CaptureDeviceInfo captureDevice)
    {
        ConfigurationService config = MediaActivator.getConfigurationService();

        audioSystem = name;

        if(name.equals(AUDIO_SYSTEM_NONE))
        {
            setAudioCaptureDevice(null);
            setAudioNotifyDevice(null);
            setAudioPlaybackDevice(null);
        }
        else if(name.equals(AUDIO_SYSTEM_JAVASOUND))
        {
            setAudioNotifyDevice(null);
            setAudioPlaybackDevice(null);

            // as there is only one device for javasound
            // lets search for it
            if(captureDevice != null)
                setAudioCaptureDevice(captureDevice);
            else
            {
                CaptureDeviceInfo[] audioCaptureDevices =
                    getAvailableAudioCaptureDevices();
                for (CaptureDeviceInfo captureDeviceInfo : audioCaptureDevices)
                {
                    if(captureDeviceInfo.getLocator().getProtocol().
                        equals("javasound"))
                    {
                        setAudioCaptureDevice(captureDeviceInfo);
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
            // firts get anyconfig before we change it
            String audioNotifyDevName =
                config.getString(PROP_AUDIO_NOTIFY_DEVICE);

            String audioPlaybackDevName =
                config.getString(PROP_AUDIO_PLAYBACK_DEVICE);

            // changed to portaudio, so lets first set the default devices
            setAudioPlaybackDevice(PortAudioAuto.defaultPlaybackDevice);
            setAudioNotifyDevice(PortAudioAuto.defaultPlaybackDevice);

            // capture device is not null when we are called for the
            // first time, we will also extract playback devices here
            if(captureDevice != null)
            {
                this.audioCaptureDevice = captureDevice;

                if(audioNotifyDevName != null)
                {
                    for (CaptureDeviceInfo captureDeviceInfo :
                            PortAudioAuto.playbackDevices)
                    {
                        if (audioNotifyDevName.equals(
                                captureDeviceInfo.getName()))
                        {
                            setAudioNotifyDevice(captureDeviceInfo);
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
                            setAudioPlaybackDevice(captureDeviceInfo);
                            break;
                        }
                    }
                }
            }
            else // no capture device specified save default
                setAudioCaptureDevice(PortAudioAuto.defaultCaptureDevice);

            // return here to prevent clearing the last config that was saved
            return;
        }
        else
        {
            // not expected behaviour
            logger.error("Unknown audio system! Name:" + name);
            audioSystem = null;
        }

        config.setProperty(PROP_AUDIO_DEVICE_IS_DISABLED, 
            audioCaptureDevice == null);
    }

    /**
     * Installs the PortAudio Renderer
     */
    protected static void initPortAudioRenderer()
    {
        PlugInManager.addPlugIn(
        "net.java.sip.communicator.impl.media.renderer.audio.PortAudioRenderer",
        net.java.sip.communicator.impl.media.renderer.audio.
            PortAudioRenderer.supportedInputFormats,
        null,
        PlugInManager.RENDERER);
    }

    /**
     * Removes javasound renderer.
     */
    private void removeJavaSoundRenderer()
    {
        PlugInManager.removePlugIn(
            "com.sun.media.renderer.audio.JavaSoundRenderer",
            PlugInManager.RENDERER);
    }

    /**
     * Removed portaudio renderer.
     */
    private void removePortAudioRenderer()
    {
        PlugInManager.removePlugIn(
        "net.java.sip.communicator.impl.media.renderer.audio.PortAudioRenderer",
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
     * Sets the device to be used by portaudio renderer.
     * @param devInfo
     */
    private void setDeviceToRenderer(CaptureDeviceInfo devInfo)
    {
        // no need to change device to renderer it will not be used anyway
        if(devInfo == null)
            return;

        try
        {
            net.java.sip.communicator.impl.media.renderer.audio.
                PortAudioRenderer.setDevice(devInfo.getLocator());
        }
        catch (Exception e)
        {
            logger.error("error setting device to renderer", e);
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
     * @param audioPlaybackDevice the audioPlaybackDevice to set
     */
    public void setAudioPlaybackDevice(CaptureDeviceInfo audioPlaybackDevice)
    {
        if(this.audioPlaybackDevice != audioPlaybackDevice)
        {
            CaptureDeviceInfo oldDev = this.audioPlaybackDevice;
            this.audioPlaybackDevice = audioPlaybackDevice;
            setDeviceToRenderer(audioPlaybackDevice);

            // we changed playback device, so we are using portaudio
            // lets use it, remove javasound renderer to be sure
            // its not used anymore and install the portaudio one
            removeJavaSoundRenderer();
            initPortAudioRenderer();

            ConfigurationService config =
                MediaActivator.getConfigurationService();

            if (audioPlaybackDevice != null)
            {
                config.setProperty(PROP_AUDIO_PLAYBACK_DEVICE,
                    audioPlaybackDevice.getName());

                config.setProperty(PROP_AUDIO_DEVICE_IS_DISABLED, false);
            }
            else
                config.setProperty(PROP_AUDIO_PLAYBACK_DEVICE, null);

            firePropertyChange(AUDIO_PLAYBACK_DEVICE, 
                oldDev, audioPlaybackDevice);
        }
    }

    /**
     * @param audioNotifyDevice the audioNotifyDevice to set
     */
    public void setAudioNotifyDevice(CaptureDeviceInfo audioNotifyDevice)
    {
        if(this.audioNotifyDevice != audioNotifyDevice)
        {
            CaptureDeviceInfo oldDev = this.audioNotifyDevice;
            this.audioNotifyDevice = audioNotifyDevice;

            ConfigurationService config =
                MediaActivator.getConfigurationService();

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

            firePropertyChange(AUDIO_NOTIFY_DEVICE,
                oldDev, audioNotifyDevice);
        }
    }

    /**
     * Change the state of echo cancel configuration
     * @param enabled true if enabled
     */
    public void setEchoCancel(boolean enabled)
    {
        try
        {
            PortAudioManager.getInstance().setEchoCancel(
                enabled,
                PortAudioManager.getInstance().getFrameSize(),
                PortAudioManager.getInstance().getFilterLength());
            MediaActivator.getConfigurationService()
                .setProperty(PROP_AUDIO_ECHOCANCEL_ENABLED, enabled);
        }
        catch (PortAudioException ex)
        {
            logger.error("Error setting echocancel config", ex);
        }
    }

    /**
     * Change the state of noise suppression configuration
     * @param enabled true if enabled
     */
    public void setDenoise(boolean enabled)
    {
        try
        {
            PortAudioManager.getInstance().setDeNoise(enabled);
            MediaActivator.getConfigurationService()
                .setProperty(PROP_AUDIO_DENOISE_ENABLED, enabled);
        }
        catch (PortAudioException ex)
        {
            logger.error("Error setting denoise config", ex);
        }
    }

    /**
     * Returns the state of echo cancel configuration.
     * @return state of echo cancel.
     */
    public boolean isEchoCancelEnabled()
    {
        try
        {
            return PortAudioManager.getInstance().isEnabledEchoCancel();
        }
        catch (PortAudioException e)
        {
            return false;
        }
    }

    /**
     * Returns the state of noise suppression configuration.
     * @return state of noise suppression.
     */
    public boolean isDenoiseEnabled()
    {
        try
        {
            return PortAudioManager.getInstance().isEnabledDeNoise();
        }
        catch (PortAudioException e)
        {
            return false;
        }
    }
}
