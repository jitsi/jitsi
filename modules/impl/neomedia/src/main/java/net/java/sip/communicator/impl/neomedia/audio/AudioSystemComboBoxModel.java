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

import java.util.*;
import javax.swing.*;
import org.jitsi.impl.neomedia.device.*;

/**
 * ComboBox model for the {@link AudioSystem}.
 */
public class AudioSystemComboBoxModel
    extends DefaultComboBoxModel<AudioSystem>
{
    /**
     * The current device configuration.
     */
    private final DeviceConfiguration deviceConfiguration;

    /**
     * Creates device combobox model
     *
     * @param deviceConfiguration the current device configuration
     */
    public AudioSystemComboBoxModel(
        DeviceConfiguration deviceConfiguration)
    {
        super(deviceConfiguration.getAvailableAudioSystems());
        super.setSelectedItem(deviceConfiguration.getAudioSystem());
        this.deviceConfiguration = deviceConfiguration;
    }

    @Override
    public void setSelectedItem(Object item)
    {
        if (!Objects.equals(item, deviceConfiguration.getAudioSystem()))
        {
            super.setSelectedItem(item);
            deviceConfiguration.setAudioSystem((AudioSystem) item, true);
        }
    }
}
