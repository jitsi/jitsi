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
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Implementation of the configuration form.
 *
 * @author Timur Masar
 */
public class XMPPConfigForm
extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The name of the property used to control whether to use
     * all resources to show capabilities
     */
    public static final String PROP_XMPP_USE_ALL_RESOURCES_FOR_CAPABILITIES =
        "net.java.sip.communicator.XMPP_USE_ALL_RESOURCES_FOR_CAPABILITIES";

    /**
     * The default value for the capabilities setting
     */
    public static final boolean USE_ALL_RESOURCES_FOR_CAPABILITIES_DEFAULT =
        true;

    /**
     * The <tt>ConfigurationService</tt> to be used to access configuration
     */
    private final ConfigurationService configurationService
    = GeneralConfigPluginActivator.getConfigurationService();

    /**
     * Creates the form.
     */
    public XMPPConfigForm()
    {
        super(new BorderLayout());
        Box box = Box.createVerticalBox();
        add(box, BorderLayout.NORTH);

        TransparentPanel contentPanel = new TransparentPanel();
        contentPanel.setLayout(new BorderLayout(10, 10));

        box.add(contentPanel);

        TransparentPanel labelPanel
        = new TransparentPanel(new GridLayout(0, 1, 2, 2));
        TransparentPanel valuePanel
        = new TransparentPanel(new GridLayout(0, 1, 2, 2));

        contentPanel.add(labelPanel, BorderLayout.CENTER);
        contentPanel.add(valuePanel, BorderLayout.WEST);

        final JCheckBox useAllResourcesForCapabilitiesCheckbox =
            new SIPCommCheckBox(Resources.getString(
                "plugin.generalconfig.XMPP_USE_ALL_RESOURCES"));

        useAllResourcesForCapabilitiesCheckbox.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    configurationService.setProperty(
                        PROP_XMPP_USE_ALL_RESOURCES_FOR_CAPABILITIES,
                        useAllResourcesForCapabilitiesCheckbox.isSelected());
                }
            });
        useAllResourcesForCapabilitiesCheckbox.setSelected(
            configurationService.getBoolean(
                PROP_XMPP_USE_ALL_RESOURCES_FOR_CAPABILITIES,
                USE_ALL_RESOURCES_FOR_CAPABILITIES_DEFAULT));
        valuePanel.add(useAllResourcesForCapabilitiesCheckbox);
    }
}
