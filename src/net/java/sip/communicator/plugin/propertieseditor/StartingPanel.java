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
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;

/**
 * The <tt>StartingPanel</tt> wraps the <tt>WarningPanel</tt> and
 * <tt>PropertiesEditorPanel</tt>. Its main purpose is to choose which one of
 * the panels to set visible.
 * 
 * @author Marin Dzhigarov
 */
public class StartingPanel
    extends TransparentPanel
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    private ConfigurationService confService = PropertiesEditorActivator
        .getConfigurationService();

    /*
     * The WarningPanel
     */
    WarningPanel warningPanel = new WarningPanel(this);

    /**
     * The PropertiesEditorPanel
     */
    PropertiesEditorPanel propertiesEditorPanel = new PropertiesEditorPanel();

    /**
     * Creates an instance <tt>StartingPanel</tt>
     */
    public StartingPanel()
    {
        super(new BorderLayout());
        boolean showWarning =
            confService.getBoolean(Constants.SHOW_WARNING_MSG_PROP, true);

        if (showWarning)
        {
            add(warningPanel, BorderLayout.CENTER);
        }
        else
        {
            add(propertiesEditorPanel, BorderLayout.CENTER);
        }
    }
}
