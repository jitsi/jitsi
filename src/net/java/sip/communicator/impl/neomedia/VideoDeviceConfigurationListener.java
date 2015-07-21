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

import javax.media.*;

import net.java.sip.communicator.service.gui.*;

import org.jitsi.impl.neomedia.device.*;

/**
 * Implements a listener which responds to changes in the video device
 * configuration by displaying pop-up notifications summarizing the changes for
 * the user.
 *
 * @author Vincent Lucas
 */
public class VideoDeviceConfigurationListener
    extends AbstractDeviceConfigurationListener
{
    /**
     * Creates a listener to the click on the popup message concerning video
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public VideoDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        super(configurationForm);
    }

    /**
     * Notifies this instance that a property related to the configuration of
     * devices has had its value changed and thus signals that a video device
     * may have been plugged or unplugged.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the name of the
     * property whose value has changed and the old and new values of that
     * property
     */
    @Override
    public void propertyChange(PropertyChangeEvent ev)
    {
        // If a new video device has been selected.
        if(DeviceConfiguration.VIDEO_CAPTURE_DEVICE.equals(
                ev.getPropertyName()))
        {
            Object newValue = ev.getNewValue();

            if(newValue != null)
            {
                String title
                    = NeomediaActivator.getResources().getI18NString(
                            "impl.media.configform.VIDEO_DEVICE_SELECTED");
                String body = ((CaptureDeviceInfo) newValue).getName();
                String popUpEvent = NeomediaActivator.NEW_SELECTED_DEVICE;

                showPopUpNotification(title, body, popUpEvent);
            }
        }
    }
}
