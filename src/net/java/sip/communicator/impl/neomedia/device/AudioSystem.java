/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;

public abstract class AudioSystem
    extends DeviceSystem
{
    public static final int FEATURE_DENOISE = 2;

    public static final int FEATURE_ECHO_CANCELLATION = 4;

    public static final int FEATURE_NOTIFY_AND_PLAYBACK_DEVICES = 8;

    private static final int FLAG_CAPTURE_DEVICE_IS_NULL = 1;

    private static final int FLAG_NOTIFY_DEVICE_IS_NULL = 2;

    private static final int FLAG_PLAYBACK_DEVICE_IS_NULL = 4;

    public static final String PROP_CAPTURE_DEVICE = "captureDevice";

    public static final String PROP_NOTIFY_DEVICE = "notifyDevice";

    public static final String PROP_PLAYBACK_DEVICE = "playbackDevice";

    public static AudioSystem getAudioSystem(String locatorProtocol)
    {
        AudioSystem[] audioSystems = getAudioSystems();
        AudioSystem audioSystemWithLocatorProtocol = null;

        if (audioSystems != null)
        {
            for (AudioSystem audioSystem : audioSystems)
            {
                if (audioSystem.getLocatorProtocol().equalsIgnoreCase(
                        locatorProtocol))
                {
                    audioSystemWithLocatorProtocol = audioSystem;
                    break;
                }
            }
        }
        return audioSystemWithLocatorProtocol;
    }

    public static AudioSystem[] getAudioSystems()
    {
        DeviceSystem[] deviceSystems
            = DeviceSystem.getDeviceSystems(MediaType.AUDIO);
        List<AudioSystem> audioSystems;

        if (deviceSystems == null)
            audioSystems = null;
        else
        {
            audioSystems = new ArrayList<AudioSystem>(deviceSystems.length);
            for (DeviceSystem deviceSystem : deviceSystems)
                if (deviceSystem instanceof AudioSystem)
                    audioSystems.add((AudioSystem) deviceSystem);
        }
        return
            (audioSystems == null)
                ? null
                : audioSystems.toArray(new AudioSystem[audioSystems.size()]);
    }

    private CaptureDeviceInfo captureDevice;

    private int flags;

    private CaptureDeviceInfo notifyDevice;

    private CaptureDeviceInfo playbackDevice;

    private List<CaptureDeviceInfo> playbackDevices;

    protected AudioSystem(String locatorProtocol)
        throws Exception
    {
        this(locatorProtocol, 0);
    }

    protected AudioSystem(String locatorProtocol, int features)
        throws Exception
    {
        super(MediaType.AUDIO, locatorProtocol, features);
    }

    public CaptureDeviceInfo getCaptureDevice()
    {
        List<CaptureDeviceInfo> captureDevices = null;

        if (this.captureDevice != null)
        {
            if (captureDevices == null)
                captureDevices = getCaptureDevices();
            if ((captureDevices == null)
                    || !captureDevices.contains(this.captureDevice))
                setCaptureDevice(null, false);
        }

        CaptureDeviceInfo captureDevice = this.captureDevice;

        if ((captureDevice == null)
                && ((flags & FLAG_CAPTURE_DEVICE_IS_NULL) == 0))
        {
            if (captureDevices == null)
                captureDevices = getCaptureDevices();
            if ((captureDevices != null) && (captureDevices.size() > 0))
                captureDevice = captureDevices.get(0);
        }
        return captureDevice;
    }

    public List<CaptureDeviceInfo> getCaptureDevices()
    {
        return
            filterDeviceListByLocatorProtocol(
                    NeomediaActivator
                        .getMediaServiceImpl()
                            .getDeviceConfiguration()
                                .getAvailableAudioCaptureDevices(),
                    getLocatorProtocol());
    }

    public CaptureDeviceInfo getNotifyDevice()
    {
        List<CaptureDeviceInfo> notifyDevices = null;

        if (this.notifyDevice != null)
        {
            if (notifyDevices == null)
                notifyDevices = getNotifyDevices();
            if ((notifyDevices == null)
                    || !notifyDevices.contains(this.notifyDevice))
                setNotifyDevice(null, false);
        }

        CaptureDeviceInfo notifyDevice = this.notifyDevice;

        if ((notifyDevice == null)
                && ((flags & FLAG_NOTIFY_DEVICE_IS_NULL) == 0))
        {
            if (notifyDevices == null)
                notifyDevices = getNotifyDevices();
            if ((notifyDevices != null) && (notifyDevices.size() > 0))
                notifyDevice = notifyDevices.get(0);
        }
        return notifyDevice;
    }

    public List<CaptureDeviceInfo> getNotifyDevices()
    {
        return getPlaybackDevices();
    }

    public CaptureDeviceInfo getPlaybackDevice()
    {
        List<CaptureDeviceInfo> playbackDevices = null;

        if (this.playbackDevice != null)
        {
            if (playbackDevices == null)
                playbackDevices = getPlaybackDevices();
            if ((playbackDevices == null)
                    || !playbackDevices.contains(this.playbackDevice))
                setPlaybackDevice(null, false);
        }

        CaptureDeviceInfo playbackDevice = this.playbackDevice;

        if ((playbackDevice == null)
                && ((flags & FLAG_PLAYBACK_DEVICE_IS_NULL) == 0))
        {
            if (playbackDevices == null)
                playbackDevices = getPlaybackDevices();
            if ((playbackDevices != null) && (playbackDevices.size() > 0))
                playbackDevice = playbackDevices.get(0);
        }
        return playbackDevice;
    }

    public List<CaptureDeviceInfo> getPlaybackDevices()
    {
        List<CaptureDeviceInfo> playbackDevices = this.playbackDevices;

        return
            (playbackDevices == null)
                ? null
                : new ArrayList<CaptureDeviceInfo>(playbackDevices);
    }

    @Override
    protected void postInitialize()
    {
        try
        {
            super.postInitialize();
        }
        finally
        {
            try
            {
                if (captureDevice != null)
                {
                    List<CaptureDeviceInfo> captureDevices = getCaptureDevices();

                    if ((captureDevices == null)
                            || !captureDevices.contains(captureDevice))
                        setCaptureDevice(null, false);
                }
            }
            finally
            {
                if ((FEATURE_NOTIFY_AND_PLAYBACK_DEVICES & getFeatures()) != 0)
                {
                    try
                    {
                        if (notifyDevice != null)
                        {
                            List<CaptureDeviceInfo> notifyDevices
                                = getNotifyDevices();

                            if ((notifyDevices == null)
                                    || !notifyDevices.contains(notifyDevice))
                                setNotifyDevice(null, false);
                        }
                    }
                    finally
                    {
                        if (playbackDevice != null)
                        {
                            List<CaptureDeviceInfo> playbackDevices
                                = getPlaybackDevices();

                            if ((playbackDevices == null)
                                    || !playbackDevices.contains(
                                            playbackDevice))
                                setPlaybackDevice(null, false);
                        }
                    }
                }
            }
        }
    }

    private void saveDevice(
            String property,
            CaptureDeviceInfo device,
            boolean isNull)
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        if (cfg != null)
        {
            property
                = DeviceConfiguration.PROP_AUDIO_SYSTEM
                    + "."
                    + getLocatorProtocol()
                    + "."
                    + property;
            if (device == null)
            {
                if (isNull)
                    cfg.setProperty(property, NoneAudioSystem.LOCATOR_PROTOCOL);
                else
                    cfg.removeProperty(property);
            }
            else
                cfg.setProperty(property, device.getName());
        }
    }

    public void setCaptureDevice(CaptureDeviceInfo captureDevice, boolean save)
    {
        if ((this.captureDevice != captureDevice) || (captureDevice == null))
        {
            CaptureDeviceInfo oldValue = this.captureDevice;

            this.captureDevice = captureDevice;

            if (save)
            {
                boolean isNull = (this.captureDevice == null);

                if (isNull)
                    flags |= FLAG_CAPTURE_DEVICE_IS_NULL;
                else
                    flags &= ~FLAG_CAPTURE_DEVICE_IS_NULL;

                saveDevice(PROP_CAPTURE_DEVICE, this.captureDevice, isNull);
            }

            CaptureDeviceInfo newValue = getCaptureDevice();

            if (oldValue != newValue)
                firePropertyChange(PROP_CAPTURE_DEVICE, oldValue, newValue);
        }
    }

    protected void setCaptureDevices(List<CaptureDeviceInfo> captureDevices)
    {
        if (captureDevices != null)
        {
            boolean commit = false;

            for (CaptureDeviceInfo captureDevice : captureDevices)
            {
                CaptureDeviceManager.addDevice(captureDevice);
                commit = true;
            }
            if (commit)
            {
                try
                {
                    CaptureDeviceManager.commit();
                }
                catch (IOException ioe)
                {
                    // Whatever.
                }
            }
        }
    }

    public void setNotifyDevice(CaptureDeviceInfo notifyDevice, boolean save)
    {
        if ((this.notifyDevice != notifyDevice) || (notifyDevice == null))
        {
            CaptureDeviceInfo oldValue = this.notifyDevice;

            this.notifyDevice = notifyDevice;

            if (save)
            {
                boolean isNull = (this.notifyDevice == null);

                if (isNull)
                    flags |= FLAG_NOTIFY_DEVICE_IS_NULL;
                else
                    flags &= ~FLAG_NOTIFY_DEVICE_IS_NULL;

                saveDevice(PROP_NOTIFY_DEVICE, this.notifyDevice, isNull);
            }

            CaptureDeviceInfo newValue = getNotifyDevice();

            if (oldValue != newValue)
                firePropertyChange(PROP_NOTIFY_DEVICE, oldValue, newValue);
        }
    }

    public void setPlaybackDevice(
            CaptureDeviceInfo playbackDevice,
            boolean save)
    {
        if ((this.playbackDevice != playbackDevice) || (playbackDevice == null))
        {
            CaptureDeviceInfo oldValue = this.playbackDevice;

            this.playbackDevice = playbackDevice;

            if (save)
            {
                boolean isNull = (this.playbackDevice == null);

                if (isNull)
                    flags |= FLAG_PLAYBACK_DEVICE_IS_NULL;
                else
                    flags &= ~FLAG_PLAYBACK_DEVICE_IS_NULL;

                saveDevice(PROP_PLAYBACK_DEVICE, this.playbackDevice, isNull);
            }

            CaptureDeviceInfo newValue = getPlaybackDevice();

            if (oldValue != newValue)
                firePropertyChange(PROP_PLAYBACK_DEVICE, oldValue, newValue);
        }
    }

    protected void setPlaybackDevices(List<CaptureDeviceInfo> playbackDevices)
    {
        this.playbackDevices
            = (playbackDevices == null)
                ? null
                : new ArrayList<CaptureDeviceInfo>(playbackDevices);
    }
}
