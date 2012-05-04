/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;

/**
 * @author Lubomir Marinov
 */
public class DeviceConfigurationComboBoxModel
    implements ComboBoxModel
{
    /**
     * Encapsulates CaptureDeviceInfo
     */
    public static class CaptureDevice
    {
        /**
         * Compares two CaptureDeviceInfo
         * @param a first <tt>CaptureDeviceInfo</tt> to compare
         * @param b second <tt>CaptureDeviceInfo</tt> to compare
         * @return whether a is equal to b
         */
        public static boolean equals(CaptureDeviceInfo a, CaptureDeviceInfo b)
        {
            return (a == null) ? (b == null) : a.equals(b);
        }

        /**
         * The encapsulated info.
         */
        public final CaptureDeviceInfo info;

        /**
         * Creates the wrapper.
         * @param info the info object we wrap.
         */
        public CaptureDevice(CaptureDeviceInfo info)
        {
            this.info = info;
        }

        /**
         * Gets a human-readable <tt>String</tt> representation of this
         * instance.
         *
         * @return a <tt>String</tt> value which is a human-readable
         * representation of this instance
         */
        @Override
        public String toString()
        {
            return
                (info == null)
                    ? NeomediaActivator.getResources().getI18NString(
                            "impl.media.configform.NO_DEVICE")
                    : info.getName();
        }
    }

    /**
     * Type of the model - audio.
     */
    public static final int AUDIO = 1;

    /**
     * Type of the model - video.
     */
    public static final int VIDEO = 2;

    /**
     * Audio Capture Device.
     */
    public static final int AUDIO_CAPTURE = 3;

    /**
     * Audio playback device.
     */
    public static final int AUDIO_PLAYBACK = 4;

    /**
     * Audio device for notification sounds.
     */
    public static final int AUDIO_NOTIFY = 5;

    private AudioSystem[] audioSystems;

    /**
     * The current device configuration.
     */
    private final DeviceConfiguration deviceConfiguration;

    /**
     * All the devices.
     */
    private CaptureDevice[] devices;

    /**
     * The <tt>ListDataListener</tt>s registered with this instance.
     */
    private final List<ListDataListener> listeners
        = new ArrayList<ListDataListener>();

    /**
     * The type of the media for this combo.
     */
    private final int type;

    /**
     * Creates device combobox model
     * @param deviceConfiguration the current device configuration
     * @param type the device - audio/video
     */
    public DeviceConfigurationComboBoxModel(
            DeviceConfiguration deviceConfiguration,
            int type)
    {
        if (deviceConfiguration == null)
            throw new IllegalArgumentException("deviceConfiguration");
        if ((type != AUDIO)
                && (type != AUDIO_CAPTURE)
                && (type != AUDIO_NOTIFY)
                && (type != AUDIO_PLAYBACK)
                && (type != VIDEO))
            throw new IllegalArgumentException("type");

        this.deviceConfiguration = deviceConfiguration;
        this.type = type;
    }

    public void addListDataListener(ListDataListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("listener");

        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Change of the content.
     * @param index0 from index.
     * @param index1 to index.
     */
    protected void fireContentsChanged(int index0, int index1)
    {
        ListDataListener[] listeners
            = this.listeners.toArray(
                    new ListDataListener[this.listeners.size()]);
        ListDataEvent event
            = new ListDataEvent(
                    this,
                    ListDataEvent.CONTENTS_CHANGED,
                    index0,
                    index1);

        for (ListDataListener listener : listeners)
            listener.contentsChanged(event);
    }

    private AudioSystem[] getAudioSystems()
    {
        if (type != AUDIO)
            throw new IllegalStateException("type");

        if (audioSystems == null)
            audioSystems = deviceConfiguration.getAvailableAudioSystems();
        return audioSystems;
    }

    /**
     * Extracts the devices for the current type.
     * @return the devices.
     */
    private CaptureDevice[] getDevices()
    {
        if (type == AUDIO)
            throw new IllegalStateException("type");

        if (devices != null)
            return devices;

        AudioSystem audioSystem;
        List<CaptureDeviceInfo> infos;

        switch (type)
        {
        case AUDIO_CAPTURE:
            audioSystem = deviceConfiguration.getAudioSystem();
            infos
                = (audioSystem == null)
                    ? null
                    : audioSystem.getCaptureDevices();
            break;
        case AUDIO_NOTIFY:
            audioSystem = deviceConfiguration.getAudioSystem();
            infos
                = (audioSystem == null) ? null : audioSystem.getNotifyDevices();
            break;
        case AUDIO_PLAYBACK:
            audioSystem = deviceConfiguration.getAudioSystem();
            infos
                = (audioSystem == null)
                    ? null
                    : audioSystem.getPlaybackDevices();
            break;
        case VIDEO:
            infos
                = deviceConfiguration.getAvailableVideoCaptureDevices(
                        MediaUseCase.CALL);
            break;
        default:
            throw new IllegalStateException("type");
        }

        final int deviceCount = (infos == null) ? 0 : infos.size();
        devices = new CaptureDevice[deviceCount + 1];

        if (deviceCount > 0)
            for (int i = 0; i < deviceCount; i++)
                devices[i] = new CaptureDevice(infos.get(i));
        devices[deviceCount] = new CaptureDevice(null);

        return devices;
    }

    /**
     * Extracts the devices selected by the configuration.
     * @return <tt>CaptureDevice</tt> selected
     */
    private CaptureDevice getSelectedDevice()
    {
        AudioSystem audioSystem;
        CaptureDeviceInfo info;

        switch (type)
        {
        case AUDIO_CAPTURE:
            audioSystem = deviceConfiguration.getAudioSystem();
            info
                = (audioSystem == null) ? null : audioSystem.getCaptureDevice();
            break;
        case AUDIO_NOTIFY:
            audioSystem = deviceConfiguration.getAudioSystem();
            info
                = (audioSystem == null) ? null : audioSystem.getNotifyDevice();
            break;
        case AUDIO_PLAYBACK:
            audioSystem = deviceConfiguration.getAudioSystem();
            info
                = (audioSystem == null)
                    ? null
                    : audioSystem.getPlaybackDevice();
            break;
        case VIDEO:
            info = deviceConfiguration.getVideoCaptureDevice(MediaUseCase.ANY);
            break;
        default:
            throw new IllegalStateException("type");
        }

        for (CaptureDevice device : getDevices())
            if (CaptureDevice.equals(device.info, info))
                return device;
        return null;
    }

    public Object getElementAt(int index)
    {
        if (type == AUDIO)
            return getAudioSystems()[index];
        else
            return getDevices()[index];
    }

    public Object getSelectedItem()
    {
        if (type == AUDIO)
            return deviceConfiguration.getAudioSystem();
        else
            return getSelectedDevice();
    }

    public int getSize()
    {
        if (type == AUDIO)
            return getAudioSystems().length;
        else
            return getDevices().length;
    }

    public void removeListDataListener(ListDataListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("listener");

        listeners.remove(listener);
    }

    /**
     * Selects and saves the new choice.
     * @param device the device we choose.
     */
    private void setSelectedDevice(CaptureDevice device)
    {
        // We cannot clear the selection of DeviceConfiguration.
        if (device == null)
            return;

        CaptureDevice selectedDevice = getSelectedDevice();

        if (selectedDevice != device)
        {
            AudioSystem audioSystem;

            switch (type)
            {
            case AUDIO_CAPTURE:
                audioSystem = deviceConfiguration.getAudioSystem();
                if (audioSystem != null)
                    audioSystem.setCaptureDevice(device.info, true);
                break;
            case AUDIO_NOTIFY:
                audioSystem = deviceConfiguration.getAudioSystem();
                if (audioSystem != null)
                    audioSystem.setNotifyDevice(device.info, true);
                break;
            case AUDIO_PLAYBACK:
                audioSystem = deviceConfiguration.getAudioSystem();
                if (audioSystem != null)
                    audioSystem.setPlaybackDevice(device.info, true);
                break;
            case VIDEO:
                deviceConfiguration.setVideoCaptureDevice(device.info, true);
                break;
            }

            fireContentsChanged(-1, -1);
        }
    }

    public void setSelectedItem(Object item)
    {
        if (type == AUDIO)
        {
            AudioSystem audioSystem = (AudioSystem) item;

            if(!audioSystem.equals(deviceConfiguration.getAudioSystem()))
            {
                deviceConfiguration.setAudioSystem(audioSystem, true);
                fireContentsChanged(-1, -1);
            }
        }
        else
            setSelectedDevice((CaptureDevice) item);
    }
}
