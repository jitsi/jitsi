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
package net.java.sip.communicator.impl.neomedia.audio;

import java.beans.*;
import java.util.*;

import javax.media.*;
import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;

/**
 * Implements <tt>ComboBoxModel</tt> for a specific <tt>DeviceConfiguration</tt>
 * so that the latter may be displayed and manipulated in the user interface as
 * a combo box.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
class AudioDeviceComboBoxModel
    extends DefaultComboBoxModel<CaptureDeviceViewModel>
    implements PropertyChangeListener
{
    /**
     * The current device configuration.
     */
    private final DeviceConfiguration deviceConfiguration;

    /**
     * All the devices.
     */
    private List<CaptureDeviceViewModel> devices;

    /**
     * The type of the media for this combo.
     */
    private final AudioSystem.DataFlow type;

    private boolean initializing = true;

    /**
     * Creates device combobox model
     *
     * @param deviceConfiguration the current device configuration
     * @param type                the device - audio/video
     */
    public AudioDeviceComboBoxModel(
        DeviceConfiguration deviceConfiguration,
        AudioSystem.DataFlow type)
    {
        this.deviceConfiguration = deviceConfiguration;
        this.type = type;
        getDevices().forEach(this::addElement);
        initializing = false;
        super.setSelectedItem(getSelectedDevice());
        deviceConfiguration.addPropertyChangeListener(this);
    }

    /**
     * Extracts the devices for the current type.
     *
     * @return the devices.
     */
    private List<CaptureDeviceViewModel> getDevices()
    {
        if (devices != null)
        {
            return devices;
        }

        AudioSystem audioSystem = deviceConfiguration.getAudioSystem();
        if (audioSystem == null)
        {
            return Collections.emptyList();
        }

        List<? extends CaptureDeviceInfo> infos = audioSystem.getDevices(type);
        devices = new ArrayList<>(infos.size() + 1);
        infos.forEach(i -> devices.add(new CaptureDeviceViewModel(i)));
        devices.sort(Comparator.comparing(CaptureDeviceViewModel::toString));
        devices.add(new CaptureDeviceViewModel(null));
        return devices;
    }

    /**
     * Extracts the devices selected by the configuration.
     *
     * @return <tt>CaptureDevice</tt> selected
     */
    private CaptureDeviceViewModel getSelectedDevice()
    {
        AudioSystem audioSystem = deviceConfiguration.getAudioSystem();
        if (audioSystem == null)
        {
            return null;
        }

        CaptureDeviceInfo info = audioSystem.getSelectedDevice(type);
        for (CaptureDeviceViewModel device : getDevices())
        {
            if (device.equals(info))
            {
                return device;
            }
        }

        return null;
    }

    /**
     * Notifies this instance about changes in the values of the properties of
     * {@link #deviceConfiguration} so that this instance keeps itself
     * up-to-date with respect to the list of devices.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     *           property whose value has changed and the old and new values of
     *           that property
     */
    @Override
    public void propertyChange(final PropertyChangeEvent ev)
    {
        if (DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES.equals(
            ev.getPropertyName()))
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                devices = null;
                initializing = true;
                super.removeAllElements();
                getDevices().forEach(this::addElement);
                initializing = false;
            }
            else
            {
                SwingUtilities.invokeLater(() -> propertyChange(ev));
            }
        }
    }

    @Override
    public void setSelectedItem(Object item)
    {
        // We cannot clear the selection of DeviceConfiguration.
        if (item == null || initializing)
        {
            return;
        }

        CaptureDeviceViewModel device = (CaptureDeviceViewModel) item;
        CaptureDeviceViewModel selectedDevice = getSelectedDevice();
        AudioSystem audioSystem = deviceConfiguration.getAudioSystem();
        if (selectedDevice != device && audioSystem != null)
        {
            audioSystem.setDevice(
                type,
                ((CaptureDeviceInfo2) device.info),
                true);
        }
        super.setSelectedItem(item);
    }
}
