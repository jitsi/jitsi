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

import net.java.sip.communicator.service.gui.*;

import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.resources.*;

/**
 * A listener to the click on the popup message concerning audio
 * device configuration changes.
 *
 * @author Vincent Lucas
 */
public class AudioDeviceConfigurationListener
    extends AbstractDeviceConfigurationListener
{
    /**
     * The last <tt>PropertyChangeEvent</tt> about an audio capture device which
     * has been received.
     */
    private PropertyChangeEvent capturePropertyChangeEvent;

    /**
     * The last <tt>PropertyChangeEvent</tt> about an audio notification device
     * which has been received.
     */
    private PropertyChangeEvent notifyPropertyChangeEvent;

    /**
     * The last <tt>PropertyChangeEvent</tt> about an audio playback device
     * which has been received.
     */
    private PropertyChangeEvent playbackPropertyChangeEvent;

    /**
     * Creates a listener to the click on the popup message concerning audio
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public AudioDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        super(configurationForm);
    }

    /**
     * Notifies this instance that a property related to the configuration of
     * devices has had its value changed and thus signals that an audio device
     * may have been plugged or unplugged.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     * property whose value has changed and the old and new values of that
     * property
     */
    @Override
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        /*
         * The list of available capture, notification and/or playback devices
         * has changes.
         */
        if (DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES.equals(propertyName))
        {
            @SuppressWarnings("unchecked")
            List<CaptureDeviceInfo> oldDevices
                = (List<CaptureDeviceInfo>) ev.getOldValue();
            @SuppressWarnings("unchecked")
            List<CaptureDeviceInfo> newDevices
                = (List<CaptureDeviceInfo>) ev.getNewValue();

            if (oldDevices.isEmpty())
                oldDevices = null;
            if (newDevices.isEmpty())
                newDevices = null;

            String title;
            ResourceManagementService r = NeomediaActivator.getResources();
            List<CaptureDeviceInfo> devices;
            boolean removal;

            // At least one new device has been connected.
            if(newDevices != null)
            {
                title
                    = r.getI18NString(
                            "impl.media.configform.AUDIO_DEVICE_CONNECTED");
                devices = newDevices;
                removal = false;
            }
            /*
             * At least one old device has been disconnected and no new device
             * has been connected.
             */
            else if(oldDevices != null)
            {
                title
                    = r.getI18NString(
                            "impl.media.configform.AUDIO_DEVICE_DISCONNECTED");
                devices = oldDevices;
                removal = true;
            }
            else
            {
                /*
                 * Neither a new device has been connected nor an old device has
                 * been disconnected. Why are we even here in the first place
                 * anyway?
                 */
                capturePropertyChangeEvent = null;
                notifyPropertyChangeEvent = null;
                playbackPropertyChangeEvent = null;
                return;
            }

            StringBuilder body = new StringBuilder();

            for (CaptureDeviceInfo device : devices)
                body.append(device.getName()).append("\r\n");

            DeviceConfiguration devConf = (DeviceConfiguration) ev.getSource();
            AudioSystem audioSystem = devConf.getAudioSystem();
            boolean selectedHasChanged = false;

            if (audioSystem != null)
            {
                if(capturePropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi
                        = audioSystem.getSelectedDevice(
                                AudioSystem.DataFlow.CAPTURE);

                    if ((cdi != null)
                            && !cdi.equals(
                                    capturePropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".AUDIO_DEVICE_SELECTED_AUDIO_IN"))
                            .append("\r\n\t")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                    }
                }
                if(playbackPropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi
                        = audioSystem.getSelectedDevice(
                                AudioSystem.DataFlow.PLAYBACK);

                    if ((cdi != null)
                            && !cdi.equals(
                                    playbackPropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".AUDIO_DEVICE_SELECTED_AUDIO_OUT"))
                            .append("\r\n\t")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                    }
                }
                if(notifyPropertyChangeEvent != null)
                {
                    CaptureDeviceInfo cdi
                        = audioSystem.getSelectedDevice(
                                AudioSystem.DataFlow.NOTIFY);

                    if ((cdi != null)
                            && !cdi.equals(
                                    notifyPropertyChangeEvent.getOldValue()))
                    {
                        body.append("\r\n")
                            .append(
                                    r.getI18NString(
                                            "impl.media.configform"
                                                + ".AUDIO_DEVICE_SELECTED_AUDIO_NOTIFICATIONS"))
                            .append("\r\n\t")
                            .append(cdi.getName());
                        selectedHasChanged = true;
                    }
                }
            }
            capturePropertyChangeEvent = null;
            notifyPropertyChangeEvent = null;
            playbackPropertyChangeEvent = null;

            /*
             * If an old device has been disconnected and no new device has been
             * connected, show a notification only if any selected device has
             * changed.
             */
            if (!removal || selectedHasChanged)
            {
                showPopUpNotification(
                        title,
                        body.toString(),
                        NeomediaActivator.DEVICE_CONFIGURATION_HAS_CHANGED);
            }
        }
        /*
         * A new capture, notification or playback devices has been selected.
         * We will not show a notification, we will remember to report the
         * change after the batch of changes completes.
         */
        else if(CaptureDevices.PROP_DEVICE.equals(propertyName))
        {
            capturePropertyChangeEvent = ev;
        }
        else if(NotifyDevices.PROP_DEVICE.equals(propertyName))
        {
            notifyPropertyChangeEvent = ev;
        }
        else if(PlaybackDevices.PROP_DEVICE.equals(propertyName))
        {
            playbackPropertyChangeEvent = ev;
        }
    }
}
