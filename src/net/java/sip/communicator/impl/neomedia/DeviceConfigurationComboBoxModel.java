/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.neomedia;

import java.beans.*;
import java.util.*;

import javax.media.*;
import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;

/**
 * Implements <tt>ComboBoxModel</tt> for a specific <tt>DeviceConfiguration</tt>
 * so that the latter may be displayed and manipulated in the user interface as
 * a combo box.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class DeviceConfigurationComboBoxModel
    implements ComboBoxModel,
               ListModel,
               PropertyChangeListener
{
    /**
     * Type of the model - audio.
     */
    public static final int AUDIO = 1;

    /**
     * Audio Capture Device.
     */
    public static final int AUDIO_CAPTURE = 3;

    /**
     * Audio device for notification sounds.
     */
    public static final int AUDIO_NOTIFY = 5;

    /**
     * Audio playback device.
     */
    public static final int AUDIO_PLAYBACK = 4;

    /**
     * Type of the model - video.
     */
    public static final int VIDEO = 2;

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
     * @param parent the parent component
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

        if (type == AUDIO
            || type == AUDIO_CAPTURE
            || type == AUDIO_NOTIFY
            || type == AUDIO_PLAYBACK)
        {
            deviceConfiguration.addPropertyChangeListener(this);
        }
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
        List<? extends CaptureDeviceInfo> infos = null;

        switch (type)
        {
        case AUDIO_CAPTURE:
            audioSystem = deviceConfiguration.getAudioSystem();
            infos = (audioSystem == null)
                    ? null
                    : audioSystem.getDevices(AudioSystem.DataFlow.CAPTURE);
            break;
        case AUDIO_NOTIFY:
            audioSystem = deviceConfiguration.getAudioSystem();
            infos = (audioSystem == null)
                ? null
                : audioSystem.getDevices(AudioSystem.DataFlow.NOTIFY);
            break;
        case AUDIO_PLAYBACK:
            audioSystem = deviceConfiguration.getAudioSystem();
            infos = (audioSystem == null)
                    ? null
                    : audioSystem.getDevices(AudioSystem.DataFlow.PLAYBACK);
            break;
        case VIDEO:
            infos = deviceConfiguration.getAvailableVideoCaptureDevices(
                        MediaUseCase.CALL);
            break;
        default:
            throw new IllegalStateException("type");
        }

        final int deviceCount = (infos == null) ? 0 : infos.size();
        devices = new CaptureDevice[deviceCount + 1];

        if (deviceCount > 0)
        {
            for (int i = 0; i < deviceCount; i++)
                devices[i] = new CaptureDevice(infos.get(i));
        }
        devices[deviceCount] = new CaptureDevice(null);

        return devices;
    }

    public Object getElementAt(int index)
    {
        if (type == AUDIO)
            return getAudioSystems()[index];
        else
            return getDevices()[index];
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
            info = (audioSystem == null)
                ? null
                : audioSystem.getSelectedDevice(AudioSystem.DataFlow.CAPTURE);
            break;
        case AUDIO_NOTIFY:
            audioSystem = deviceConfiguration.getAudioSystem();
            info = (audioSystem == null)
                ? null
                : audioSystem.getSelectedDevice(AudioSystem.DataFlow.NOTIFY);
            break;
        case AUDIO_PLAYBACK:
            audioSystem = deviceConfiguration.getAudioSystem();
            info = (audioSystem == null)
                ? null
                : audioSystem.getSelectedDevice(AudioSystem.DataFlow.PLAYBACK);
            break;
        case VIDEO:
            info = deviceConfiguration.getVideoCaptureDevice(MediaUseCase.ANY);
            break;
        default:
            throw new IllegalStateException("type");
        }

        for (CaptureDevice device : getDevices())
        {
            if (device.equals(info))
                return device;
        }
        return null;
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

    /**
     * Notifies this instance about changes in the values of the properties of
     * {@link #deviceConfiguration} so that this instance keeps itself
     * up-to-date with respect to the list of devices.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     * property whose value has changed and the old and new values of that
     * property
     */
    public void propertyChange(final PropertyChangeEvent ev)
    {
        if (DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES.equals(
                ev.getPropertyName()))
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                audioSystems = null;
                devices = null;
                fireContentsChanged(0, getSize() - 1);
            }
            else
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                propertyChange(ev);
                            }
                        });
            }
        }
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
                {
                    audioSystem.setDevice(
                            AudioSystem.DataFlow.CAPTURE,
                            ((CaptureDeviceInfo2) device.info),
                            true);
                }
                break;
            case AUDIO_NOTIFY:
                audioSystem = deviceConfiguration.getAudioSystem();
                if (audioSystem != null)
                {
                    audioSystem.setDevice(
                            AudioSystem.DataFlow.NOTIFY,
                            ((CaptureDeviceInfo2) device.info),
                            true);
                }
                break;
            case AUDIO_PLAYBACK:
                audioSystem = deviceConfiguration.getAudioSystem();
                if (audioSystem != null)
                {
                    audioSystem.setDevice(
                            AudioSystem.DataFlow.PLAYBACK,
                            ((CaptureDeviceInfo2) device.info),
                            true);
                }
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

    /**
     * Encapsulates a <tt>CaptureDeviceInfo</tt> for the purposes of its display
     * in the user interface.
     */
    public static class CaptureDevice
    {
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
         * Determines whether the <tt>CaptureDeviceInfo</tt> encapsulated by
         * this instance is equal (by value) to a specific
         * <tt>CaptureDeviceInfo</tt>.
         *
         * @param cdi the <tt>CaptureDeviceInfo</tt> to be determined whether it
         * is equal (by value) to the <tt>CaptureDeviceInfo</tt> encapsulated by
         * this instance
         * @return <tt>true</tt> if the <tt>CaptureDeviceInfo</tt> encapsulated
         * by this instance is equal (by value) to the specified <tt>cdi</tt>;
         * otherwise, <tt>false</tt>
         */
        public boolean equals(CaptureDeviceInfo cdi)
        {
            return (info == null) ? (cdi == null) : info.equals(cdi);
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
            String s;

            if(info == null)
            {
                s
                    = NeomediaActivator.getResources().getI18NString(
                            "impl.media.configform.NO_DEVICE");
            }
            else
            {
                s = info.getName();
                if(info instanceof CaptureDeviceInfo2)
                {
                    String transportType
                        = ((CaptureDeviceInfo2) info).getTransportType();

                    if(transportType != null)
                        s += " (" + transportType + ")";
                }
            }
            return s;
        }
    }
}
