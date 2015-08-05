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
package net.java.sip.communicator.plugin.desktoputil.wizard;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.resources.*;

/**
 * The encodings configuration panel (used in the account configuration wizards)
 *
 * @author Boris Grozev
 */
public class EncodingsPanel
    extends TransparentPanel
{
    /**
     * The <tt>ResourceManagementService</tt> used by this class
     */
    private static ResourceManagementService resourceService
            = UtilActivator.getResources();

    /**
     * The "override global settings" checkbox.
     */
    private final JCheckBox overrideCheckBox;

    /**
     * The <tt>MediaConfiguration</tt> instance we'll use to obtain most of the
     * <tt>Component</tt>s for the panel
     */
    private final MediaConfigurationService mediaConfiguration;

    /**
     * A panel to hold the audio encodings table
     */
    private JPanel audioPanel;

    /**
     * The audio encodings table (and "up"/"down" buttons)
     */
    private Component audioControls;

    /**
     * A panel to hold the video encodings table
     */
    private JPanel videoPanel;

    /**
     * The video encodings table (and "up"/"down" buttons)
     */
    private Component videoControls;

    /**
     * Holds the properties we need to get/set for the encoding preferences
     */
    private Map<String, String> encodingProperties
            = new HashMap<String, String>();

    /**
     * An <tt>EncodingConfiguration</tt> we'll be using to manage preferences
     * for us
     */
    private EncodingConfiguration encodingConfiguration;

    /**
     * The "reset" button
     */
    private JButton resetButton = new JButton(resourceService.getI18NString(
            "plugin.jabberaccregwizz.RESET"));

    /**
     * Builds an object, loads the tables with the global configuration..
     */
    public EncodingsPanel()
    {
        super(new BorderLayout());

        overrideCheckBox = new SIPCommCheckBox(resourceService.
            getI18NString("plugin.jabberaccregwizz.OVERRIDE_ENCODINGS"),
                false);
        overrideCheckBox.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                updateTableState();
            }
        });

        mediaConfiguration
                = UtilActivator.getMediaConfiguration();

        //by default (on account creation) use an <tt>EncodingConfiguration</tt>
        //loaded with the global preferences. But make a new instance, because
        //we do not want to change the current one
        encodingConfiguration = mediaConfiguration.getMediaService()
                .createEmptyEncodingConfiguration();
        encodingConfiguration.loadEncodingConfiguration(mediaConfiguration
                .getMediaService().getCurrentEncodingConfiguration());

        audioControls = mediaConfiguration.
                createEncodingControls(MediaType.AUDIO, encodingConfiguration);
        videoControls = mediaConfiguration.
                createEncodingControls(MediaType.VIDEO, encodingConfiguration);

        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        add(mainPanel, BorderLayout.NORTH);

        JPanel checkBoxPanel
            = new TransparentPanel(new BorderLayout());
        checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        checkBoxPanel.add(overrideCheckBox,BorderLayout.WEST);
        resetButton.setToolTipText(resourceService.getI18NString(
                "plugin.jabberaccregwizz.RESET_DESCRIPTION"));
        checkBoxPanel.add(resetButton,BorderLayout.EAST);
        resetButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                encodingConfiguration.loadEncodingConfiguration(
                        mediaConfiguration.getMediaService()
                                .getCurrentEncodingConfiguration());
                encodingConfiguration.storeProperties(encodingProperties,
                        ProtocolProviderFactory.ENCODING_PROP_PREFIX+".");
                resetTables();
            }
        });

        audioPanel = new TransparentPanel(new BorderLayout(10, 10));
        audioPanel.setBorder(BorderFactory.createTitledBorder(
               resourceService.getI18NString("plugin.jabberaccregwizz.AUDIO")));
        audioPanel.add(audioControls);

        videoPanel = new TransparentPanel(new BorderLayout(10, 10));
        videoPanel.setBorder(BorderFactory.createTitledBorder(
               resourceService.getI18NString("plugin.jabberaccregwizz.VIDEO")));
        videoPanel.add(videoControls);

        mainPanel.add(checkBoxPanel);
        mainPanel.add(audioPanel);
        mainPanel.add(videoPanel);
    }

    /**
     * Saves the settings we hold in <tt>registration</tt>
     * @param registration the <tt>EncodingsRegistrationUtil</tt> to use
     */
    public void commitPanel(EncodingsRegistrationUtil registration)
    {
        registration.setOverrideEncodings(overrideCheckBox.isSelected());

        encodingConfiguration.storeProperties(encodingProperties,
                ProtocolProviderFactory.ENCODING_PROP_PREFIX+".");

        registration.setEncodingProperties(encodingProperties);
    }

    /**
     * Loads encoding configuration from given <tt>encodingsReg</tt> object.
     *
     * @param encodingsReg the encoding registration object to use.
     */
    public void loadAccount(EncodingsRegistrationUtil encodingsReg)
    {
        overrideCheckBox.setSelected(encodingsReg.isOverrideEncodings());

        encodingConfiguration
                = encodingsReg.createEncodingConfig(
                mediaConfiguration.getMediaService());
        encodingConfiguration.storeProperties(encodingProperties);

        resetTables();

    }

    /**
     * Recreates the audio and video controls. Necessary when
     * our encodingConfiguration reference has changed.
     */
    private void resetTables()
    {
        audioPanel.remove(audioControls);
        videoPanel.remove(videoControls);
        audioControls = mediaConfiguration.
                createEncodingControls(MediaType.AUDIO, encodingConfiguration);
        videoControls = mediaConfiguration.
                createEncodingControls(MediaType.VIDEO, encodingConfiguration);

        audioPanel.add(audioControls);
        videoPanel.add(videoControls);
        updateTableState();
    }

    /**
     * Enables or disables the encodings tables based on the override checkbox.
     */
    private void updateTableState()
    {
        audioControls.setEnabled(overrideCheckBox.isSelected());
        videoControls.setEnabled(overrideCheckBox.isSelected());
    }
}
