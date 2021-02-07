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
package net.java.sip.communicator.impl.neomedia.video;

import static net.java.sip.communicator.impl.neomedia.NeomediaActivator.VIDEO_MORE_SETTINGS_DISABLED_PROP;
import static net.java.sip.communicator.impl.neomedia.NeomediaActivator.DEVICES_DISABLED_PROP;
import static net.java.sip.communicator.impl.neomedia.NeomediaActivator.ENCODINGS_DISABLED_PROP;

import java.awt.*;
import javax.swing.*;
import net.java.sip.communicator.impl.neomedia.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.utils.*;

public class VideoConfigPanel
    extends JTabbedPane
{
    public VideoConfigPanel()
    {
        setPreferredSize(new Dimension(350, 350));
        ResourceManagementService res = NeomediaActivator.getResources();
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        if (!cfg.getBoolean(DEVICES_DISABLED_PROP, false))
        {
            addTab(
                res.getI18NString("impl.media.configform.DEVICES"),
                new VideoDeviceTab());
        }

        if (!cfg.getBoolean(ENCODINGS_DISABLED_PROP, false))
        {
            addTab(
                res.getI18NString("impl.media.configform.ENCODINGS"),
                new EncodingConfigurationTab(MediaType.VIDEO, null));
        }

        if (!cfg.getBoolean(VIDEO_MORE_SETTINGS_DISABLED_PROP, false))
        {
            addTab(
                res.getI18NString("impl.media.configform.VIDEO_MORE_SETTINGS"),
                new VideoAdvancedSettings());
        }
    }
}
