/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.impl.neomedia.device.*;

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
                    ? NeomediaActivator
                        .getResources()
                            .getI18NString("impl.media.configform.NO_DEVICE")
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

    /**
     * The current device configuration.
     */
    private final DeviceConfiguration deviceConfiguration;

    /**
     * All the devices.
     */
    private CaptureDevice[] devices;

    /**
     * Listener for data changes.
     */
    private final List<ListDataListener> listeners =
        new ArrayList<ListDataListener>();

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
        DeviceConfiguration deviceConfiguration, int type)
    {
        if (deviceConfiguration == null)
            throw new IllegalArgumentException("deviceConfiguration");
        if ((type != AUDIO_CAPTURE) && (type != AUDIO_NOTIFY) &&
            (type != AUDIO_PLAYBACK) &&
            (type != AUDIO) && (type != VIDEO))
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
        ListDataListener[] listeners =
            this.listeners.toArray(new ListDataListener[this.listeners.size()]);
        ListDataEvent event =
            new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0,
                index1);

        for (ListDataListener listener : listeners)
        {
            listener.contentsChanged(event);
        }
    }

    /**
     * Extracts the devices for the current type.
     * @return the devices.
     */
    private CaptureDevice[] getDevices()
    {
        if (devices != null)
            return devices;


        CaptureDeviceInfo[] infos;
        switch (type)
        {
        case AUDIO_CAPTURE:
            // supply only portaudio devices, as we are in case specifying
            // capture devices available only for portaudio
            infos = deviceConfiguration.getAvailableAudioCaptureDevices(
                DeviceConfiguration.AUDIO_SYSTEM_PORTAUDIO);
            break;
        case AUDIO_NOTIFY:
        case AUDIO_PLAYBACK:
            infos = deviceConfiguration.getAvailableAudioPlaybackDevices();
            break;
        case VIDEO:
            infos = deviceConfiguration.getAvailableVideoCaptureDevices(
                    MediaUseCase.CALL);
            break;
        default:
            throw new IllegalStateException("type");
        }

        final int deviceCount = infos.length;
        devices = new CaptureDevice[deviceCount + 1];
        for (int i = 0; i < deviceCount; i++)
        {
            devices[i] = new CaptureDevice(infos[i]);
        }
        devices[deviceCount] = new CaptureDevice(null);
        return devices;
    }

    /**
     * Extracts the devices selected by the configuration.
     * @return <tt>CaptureDevice</tt> selected
     */
    private CaptureDevice getSelectedDevice()
    {
        CaptureDeviceInfo info;
        switch (type)
        {
        case AUDIO_CAPTURE:
            info = deviceConfiguration.getAudioCaptureDevice();
            break;
        case AUDIO_NOTIFY:
            info = deviceConfiguration.getAudioNotifyDevice();
            break;
        case AUDIO_PLAYBACK:
            info = deviceConfiguration.getAudioPlaybackDevice();
            break;
        case VIDEO:
            info = deviceConfiguration.getVideoCaptureDevice(MediaUseCase.ANY);
            break;
        default:
            throw new IllegalStateException("type");
        }


        for (CaptureDevice device : getDevices())
        {
            if (CaptureDevice.equals(device.info, info))
                return device;
        }

        return null;
    }

    public Object getElementAt(int index)
    {
        if(type == AUDIO)
            return deviceConfiguration.getAvailableAudioSystems()[index];
        else
            return getDevices()[index];
    }

    public Object getSelectedItem()
    {
        if(type == AUDIO)
            return deviceConfiguration.getAudioSystem();
        else
        {
            return getSelectedDevice();
        }
    }

    public int getSize()
    {
        if(type == AUDIO)
            return deviceConfiguration.getAvailableAudioSystems().length;
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
            switch (type)
            {
            case AUDIO_CAPTURE:
                deviceConfiguration.setAudioCaptureDevice(device.info, true);
                break;
            case AUDIO_NOTIFY:
                deviceConfiguration.setAudioNotifyDevice(device.info, true);
                break;
            case AUDIO_PLAYBACK:
                deviceConfiguration.setAudioPlaybackDevice(device.info, true);
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
        if(type == AUDIO)
        {
            String systemName = (String)item;

            if(!systemName.equals(deviceConfiguration.getAudioSystem()))
            {
                deviceConfiguration.setAudioSystem(systemName, null, true);
                fireContentsChanged(-1, -1);
            }
        }
        else
            setSelectedDevice((CaptureDevice) item);
    }

    /**
     * Reinitialize video devices.
     */
    public void reinitVideo()
    {
        if(type == VIDEO)
        {
            devices = null;
        }
    }
}
