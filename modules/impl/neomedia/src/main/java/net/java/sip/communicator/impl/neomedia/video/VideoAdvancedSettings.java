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

import java.awt.*;
import javax.swing.*;
import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.resources.*;

public class VideoAdvancedSettings extends TransparentPanel
{
    public VideoAdvancedSettings()
    {
        ResourceManagementService resources = NeomediaActivator.getResources();
        DeviceConfiguration deviceConfig =
            NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration();

        TransparentPanel centerPanel =
            new TransparentPanel(new GridBagLayout());
        centerPanel.setMaximumSize(new Dimension(WIDTH, 150));

        JButton resetDefaultsButton = new JButton(
            resources.getI18NString("impl.media.configform.VIDEO_RESET"));
        JPanel resetButtonPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.RIGHT));
        resetButtonPanel.add(resetDefaultsButton);

        setLayout(new BorderLayout());
        add(centerPanel, BorderLayout.NORTH);
        add(resetButtonPanel, BorderLayout.SOUTH);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridy = 0;

        centerPanel.add(new JLabel(
                resources.getI18NString("impl.media.configform.VIDEO_RESOLUTION")),
            constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        final JCheckBox frameRateCheck = new SIPCommCheckBox(
            resources.getI18NString("impl.media.configform.VIDEO_FRAME_RATE"));
        centerPanel.add(frameRateCheck, constraints);
        constraints.gridy = 2;
        constraints.insets = new Insets(5, 5, 0, 0);
        centerPanel.add(new JLabel(
                resources.getI18NString(
                    "impl.media.configform.VIDEO_PACKETS_POLICY")),
            constraints);
        constraints.gridy = 3;
        centerPanel.add(new JLabel(
                resources.getI18NString(
                    "impl.media.configform.VIDEO_BITRATE")),
            constraints);

        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.insets = new Insets(5, 0, 0, 5);
        Object[] resolutionValues
            = new Object[DeviceConfiguration.SUPPORTED_RESOLUTIONS.length + 1];
        System.arraycopy(DeviceConfiguration.SUPPORTED_RESOLUTIONS, 0,
            resolutionValues, 1,
            DeviceConfiguration.SUPPORTED_RESOLUTIONS.length);
        final JComboBox sizeCombo = new JComboBox(resolutionValues);
        sizeCombo.setRenderer(new ResolutionCellRenderer());
        sizeCombo.setEditable(false);
        centerPanel.add(sizeCombo, constraints);

        // default value is 20
        final JSpinner frameRate = new JSpinner(new SpinnerNumberModel(
            20, 5, 30, 1));
        frameRate.addChangeListener(e -> deviceConfig.setFrameRate(
            ((SpinnerNumberModel)frameRate.getModel())
                .getNumber().intValue()));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 5);
        centerPanel.add(frameRate, constraints);

        frameRateCheck.addActionListener(e -> {
            if(frameRateCheck.isSelected())
            {
                deviceConfig.setFrameRate(
                    ((SpinnerNumberModel)frameRate.getModel())
                        .getNumber().intValue());
            }
            else // unlimited framerate
                deviceConfig.setFrameRate(-1);

            frameRate.setEnabled(frameRateCheck.isSelected());
        });

        int videoMaxBandwith = deviceConfig.getVideoRTPPacingThreshold();
        // Accord the current value with the maximum allowed value. Fixes
        // existing configurations that have been set to a number larger than
        // the advised maximum value.
        videoMaxBandwith = Math.min(videoMaxBandwith, 999);

        final JSpinner videoMaxBandwidth = new JSpinner(new SpinnerNumberModel(
            videoMaxBandwith,
            1, 999, 1));
        videoMaxBandwidth.addChangeListener(e -> deviceConfig.setVideoRTPPacingThreshold(
            ((SpinnerNumberModel) videoMaxBandwidth.getModel())
                .getNumber().intValue()));
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 5, 5);
        centerPanel.add(videoMaxBandwidth, constraints);

        final JSpinner videoBitrate = new JSpinner(new SpinnerNumberModel(
            deviceConfig.getVideoBitrate(),
            1, Integer.MAX_VALUE, 1));
        videoBitrate.addChangeListener(e -> deviceConfig.setVideoBitrate(
            ((SpinnerNumberModel) videoBitrate.getModel())
                .getNumber().intValue()));
        constraints.gridy = 3;
        centerPanel.add(videoBitrate, constraints);

        resetDefaultsButton.addActionListener(e -> {
            // reset to defaults
            sizeCombo.setSelectedIndex(0);
            frameRateCheck.setSelected(false);
            frameRate.setEnabled(false);
            frameRate.setValue(20);
            // unlimited framerate
            deviceConfig.setFrameRate(-1);
            videoMaxBandwidth.setValue(
                DeviceConfiguration.DEFAULT_VIDEO_RTP_PACING_THRESHOLD);
            videoBitrate.setValue(
                DeviceConfiguration.DEFAULT_VIDEO_BITRATE);
        });

        // load selected value or auto
        Dimension videoSize = deviceConfig.getVideoSize();

        if(videoSize.getHeight() != DeviceConfiguration.DEFAULT_VIDEO_HEIGHT
            && videoSize.getWidth()
            != DeviceConfiguration.DEFAULT_VIDEO_WIDTH)
            sizeCombo.setSelectedItem(deviceConfig.getVideoSize());
        else
            sizeCombo.setSelectedIndex(0);
        sizeCombo.addActionListener(e -> {
            Dimension selectedVideoSize
                = (Dimension) sizeCombo.getSelectedItem();

            if(selectedVideoSize == null)
            {
                // the auto value, default one
                selectedVideoSize
                    = new Dimension(
                    DeviceConfiguration.DEFAULT_VIDEO_WIDTH,
                    DeviceConfiguration.DEFAULT_VIDEO_HEIGHT);
            }
            deviceConfig.setVideoSize(selectedVideoSize);
        });

        frameRateCheck.setSelected(
            deviceConfig.getFrameRate()
                != DeviceConfiguration.DEFAULT_VIDEO_FRAMERATE);
        frameRate.setEnabled(frameRateCheck.isSelected());

        if(frameRate.isEnabled())
            frameRate.setValue(deviceConfig.getFrameRate());
    }
}
