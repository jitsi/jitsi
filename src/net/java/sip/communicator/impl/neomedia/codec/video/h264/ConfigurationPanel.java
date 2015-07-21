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
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.impl.neomedia.codec.video.h264.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.control.*;
import org.jitsi.service.resources.*;

/**
 * Implements the H.264 configuration form/panel.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class ConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private final static long serialVersionUID = 0L;

    /**
     * Initializer a new <tt>ConfigurationPanel</tt> instance.
     */
    public ConfigurationPanel()
    {
        // Create the UI components.
        super(new BorderLayout());

        TransparentPanel contentPanel
            = new TransparentPanel(new GridBagLayout());
        add(contentPanel, BorderLayout.NORTH);

        ResourceManagementService r = NeomediaActivator.getResources();
        GridBagConstraints cnstrnts = new GridBagConstraints();

        cnstrnts.anchor = GridBagConstraints.FIRST_LINE_START;
        cnstrnts.fill = GridBagConstraints.HORIZONTAL;

        Component defaultProfileLabel
            = createLineWrapLabel(
                    r.getI18NString(
                            "impl.neomedia.configform.H264.defaultProfile"));
        cnstrnts.gridx = 0;
        cnstrnts.gridy = 0;
        cnstrnts.weightx = 1;
        contentPanel.add(defaultProfileLabel, cnstrnts);

        JComboBox defaultProfileComboBox = new JComboBox();
        defaultProfileComboBox.setEditable(false);
        defaultProfileComboBox.addItem(
                new NameValuePair(
                        r.getI18NString(
                                "impl.neomedia.configform.H264.defaultProfile."
                                    + JNIEncoder.BASELINE_PROFILE),
                        JNIEncoder.BASELINE_PROFILE));
        defaultProfileComboBox.addItem(
                new NameValuePair(
                        r.getI18NString(
                                "impl.neomedia.configform.H264.defaultProfile."
                                    + JNIEncoder.MAIN_PROFILE),
                        JNIEncoder.MAIN_PROFILE));
        defaultProfileComboBox.addItem(
                new NameValuePair(
                        r.getI18NString(
                                "impl.neomedia.configform.H264.defaultProfile."
                                    + JNIEncoder.HIGH_PROFILE),
                        JNIEncoder.HIGH_PROFILE));
        cnstrnts.gridx = 1;
        cnstrnts.gridy = 0;
        cnstrnts.weightx = 0;
        contentPanel.add(defaultProfileComboBox, cnstrnts);

        Component preferredKeyFrameRequesterLabel
            = createLineWrapLabel(
                    r.getI18NString(
                            "impl.neomedia.configform.H264"
                                + ".preferredKeyFrameRequester"));
        cnstrnts.gridx = 0;
        cnstrnts.gridy = 1;
        cnstrnts.weightx = 1;
        contentPanel.add(preferredKeyFrameRequesterLabel, cnstrnts);

        JComboBox preferredKeyFrameRequesterComboBox = new JComboBox();
        preferredKeyFrameRequesterComboBox.setEditable(false);
        preferredKeyFrameRequesterComboBox.addItem(
                new NameValuePair(
                        r.getI18NString(
                                "impl.neomedia.configform.H264"
                                    + ".preferredKeyFrameRequester."
                                    + KeyFrameControl.KeyFrameRequester.RTCP),
                        KeyFrameControl.KeyFrameRequester.RTCP));
        preferredKeyFrameRequesterComboBox.addItem(
                new NameValuePair(
                        r.getI18NString(
                                "impl.neomedia.configform.H264"
                                    + ".preferredKeyFrameRequester."
                                    + KeyFrameControl.KeyFrameRequester.SIGNALING),
                        KeyFrameControl.KeyFrameRequester.SIGNALING));
        cnstrnts.gridx = 1;
        cnstrnts.gridy = 1;
        cnstrnts.weightx = 0;
        contentPanel.add(
                preferredKeyFrameRequesterComboBox,
                cnstrnts);

        Component presetLabel
            = createLineWrapLabel(
                    r.getI18NString("impl.neomedia.configform.H264.preset"));
        cnstrnts.gridx = 0;
        cnstrnts.gridy = 2;
        cnstrnts.weightx = 1;
        contentPanel.add(presetLabel, cnstrnts);

        JComboBox presetComboBox = new JComboBox();
        presetComboBox.setEditable(false);
        for (String preset : JNIEncoder.AVAILABLE_PRESETS)
            presetComboBox.addItem(new NameValuePair(preset, preset));
        cnstrnts.gridx = 1;
        cnstrnts.gridy = 2;
        cnstrnts.weightx = 0;
        contentPanel.add(presetComboBox, cnstrnts);

        JCheckBox defaultIntraRefreshCheckBox
            = new SIPCommCheckBox(
                    r.getI18NString(
                            "impl.neomedia.configform.H264"
                                + ".defaultIntraRefresh"));
        cnstrnts.gridwidth = GridBagConstraints.REMAINDER;
        cnstrnts.gridx = 0;
        cnstrnts.gridy = 3;
        cnstrnts.weightx = 1;
        contentPanel.add(defaultIntraRefreshCheckBox, cnstrnts);
        cnstrnts.gridwidth = 1;

        Component keyintLabel
            = createLineWrapLabel(
                    r.getI18NString("impl.neomedia.configform.H264.keyint"));
        cnstrnts.gridx = 0;
        cnstrnts.gridy = 4;
        cnstrnts.weightx = 1;
        contentPanel.add(keyintLabel, cnstrnts);

        JSpinner keyintSpinner
            = new JSpinner(
                    new SpinnerNumberModel(
                            JNIEncoder.DEFAULT_KEYINT,
                            1, JNIEncoder.X264_KEYINT_MAX_INFINITE,
                            JNIEncoder.DEFAULT_FRAME_RATE));
        cnstrnts.gridx = 1;
        cnstrnts.gridy = 4;
        cnstrnts.weightx = 0;
        contentPanel.add(keyintSpinner, cnstrnts);

        // Load the values from the ConfigurationService into the UI components.
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        setSelectedNameValuePair(
                defaultProfileComboBox,
                cfg.getString(
                        JNIEncoder.DEFAULT_PROFILE_PNAME,
                        JNIEncoder.DEFAULT_DEFAULT_PROFILE));
        addActionListener(
                defaultProfileComboBox,
                JNIEncoder.DEFAULT_PROFILE_PNAME);

        setSelectedNameValuePair(
                preferredKeyFrameRequesterComboBox,
                cfg.getString(
                        KeyFrameControl.KeyFrameRequester.PREFERRED_PNAME,
                        KeyFrameControl.KeyFrameRequester.DEFAULT_PREFERRED));
        addActionListener(
                preferredKeyFrameRequesterComboBox,
                KeyFrameControl.KeyFrameRequester.PREFERRED_PNAME);

        setSelectedNameValuePair(
                presetComboBox,
                cfg.getString(
                        JNIEncoder.PRESET_PNAME,
                        JNIEncoder.DEFAULT_PRESET));
        addActionListener(presetComboBox, JNIEncoder.PRESET_PNAME);

        defaultIntraRefreshCheckBox.setSelected(
                cfg.getBoolean(
                        JNIEncoder.DEFAULT_INTRA_REFRESH_PNAME,
                        JNIEncoder.DEFAULT_DEFAULT_INTRA_REFRESH));
        defaultIntraRefreshCheckBox.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        JCheckBox checkBox = (JCheckBox) e.getSource();

                        NeomediaActivator
                            .getConfigurationService()
                                .setProperty(
                                        JNIEncoder.DEFAULT_INTRA_REFRESH_PNAME,
                                        Boolean.toString(
                                                checkBox.isSelected()));
                    }
                });

        keyintSpinner.setValue(
                cfg.getInt(JNIEncoder.KEYINT_PNAME, JNIEncoder.DEFAULT_KEYINT));
        keyintSpinner.addChangeListener(
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        JSpinner spinner = (JSpinner) e.getSource();
                        SpinnerNumberModel model
                            = (SpinnerNumberModel) spinner.getModel();
                        int value = model.getNumber().intValue();

                        NeomediaActivator
                            .getConfigurationService()
                                .setProperty(
                                        JNIEncoder.KEYINT_PNAME,
                                        Integer.toString(value));                        
                    }
                });
    }

    /**
     * Adds an <tt>ActionListener</tt> to a specific <tt>JComboBox</tt>
     * populated with <tt>NameValuePair</tt>s which sets the value of a specific
     * <tt>ConfigurationService</tt> property to the <tt>value</tt> of the
     * selected <tt>NameValuePair</tt> of the <tt>comboBox</tt>.
     *
     * @param comboBox the <tt>JComboBox</tt> to add an <tt>ActionListener</tt>
     * to
     * @param property the name of the <tt>ConfigurationService</tt> property
     * to set the value of
     */
    private void addActionListener(
            final JComboBox comboBox,
            final String property)
    {
        comboBox.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        NameValuePair nameValuePair
                            = (NameValuePair) comboBox.getSelectedItem();

                        if (nameValuePair != null)
                        {
                            NeomediaActivator.getConfigurationService()
                                    .setProperty(property, nameValuePair.value);
                        }
                    }
                });
    }

    /**
     * Initializes a new <tt>Component</tt> instance which is to display a
     * specific text in the fashion of <tt>JLabel</tt> and with line wrapping.
     *
     * @param text the text to be displayed by the new instance
     * @return a new <tt>Component</tt> instance which displays the specified
     * <tt>text</tt> in the fashion of <tt>JLabel</tt> and with line wrapping
     */
    private Component createLineWrapLabel(String text)
    {
        JTextArea textArea = new JTextArea();

        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setOpaque(false);
        textArea.setWrapStyleWord(true);

        textArea.setText(text);

        return textArea;
    }

    /**
     * Sets the selected item in a specific <tt>JComboBox</tt> populated with
     * <tt>NameValuePair</tt>s to the one which has a specific <tt>value</tt>.
     *
     * @param comboBox the <tt>JComboBox</tt> to set the selected item of
     * @param value the value of the <tt>NameValuePair</tt> to set as the
     * selected item of <tt>comboBox</tt>
     */
    private void setSelectedNameValuePair(JComboBox comboBox, String value)
    {
        for (int i = 0, count = comboBox.getItemCount(); i < count; i++)
        {
            NameValuePair nameValuePair = (NameValuePair) comboBox.getItemAt(i);

            if (nameValuePair.value.equals(value))
            {
                comboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Represents a <tt>String</tt> value which has a human-readable name
     * associated with it for display purposes.
     */
    private static class NameValuePair
    {
        /**
         * The human-readable name of this <tt>NameValuePair</tt>.
         */
        public final String name;

        /**
         * The <tt>String</tt> value represented by this <tt>NameValuePair</tt>.
         */
        public final String value;

        /**
         * Initializes a new <tt>NameValuePair</tt> which is to represent a
         * specific <tt>String</tt> <tt>value</tt> which is to be displayed to
         * the user as <tt>name</tt>.
         *
         * @param name the human-readable name of the new instance
         * @param value the <tt>String</tt> value to be represented by the new
         * instance
         */
        public NameValuePair(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns a human-readable representation of this <tt>Object</tt> i.e.
         * the name of this <tt>NameValuePair</tt>.
         *
         * @return a human-readable representation of this <tt>Object</tt> i.e.
         * the name of this <tt>NameValuePair</tt>
         */
        @Override
        public String toString()
        {
            return name;
        }
    }
}
