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

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.codec.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Implements the Opus configuration panel.
 *
 * @author Boris Grozev
 */
public class OpusConfigForm
    extends TransparentPanel
    implements ActionListener
{
    /**
     * Strings for audio bandwidths. Used as the value for the bandwidth
     * property. In sync with <tt>BANDWIDTHS_LONG</tt>
     */
    private static final String[] BANDWIDTHS = new String[]{
            "auto", "fb", "swb", "wb", "mb", "nb"
    };

    /**
     * The strings used in the combobox. In sync with <tt>BANDWIDTHS</tt>
     */
    private static final String[] BANDWIDTHS_LONG = new String[]{
            "Auto",
            "Fullband (48kHz)",
            "Super-wideband (24kHz)",
            "Wideband (16kHz)",
            "Medium-band (12kHz)",
            "Narrowband (8kHz)"
    };

    /**
     * The default value for the "bandwidth" setting.
     */
    private static final String BANDWIDTH_DEFAULT = "auto";

    /**
     * The default value for the "bitrate" setting
     */
    private static final int BITRATE_DEFAULT = 32;

    /**
     * The default value for the "dtx" setting
     */
    private static final boolean DTX_DEFAULT = true;

    /**
     * The default value for the "fec" setting
     */
    private static final boolean FEC_DEFAULT = true;

    /**
     * The default value for the "minimum expected packet loss" setting
     */
    private static final int MIN_EXPECTED_PL_DEFAULT = 1;

    /**
     * The index of the default value for the 'complexity' setting. Index 0
     * corresponds to the default complexity set by the opus_encoder_create
     * function/method.
     */
    private static final int COMPLEXITY_DEFAULT_INDEX = 0;

    /**
     * The "audio bandwidth" combobox
     */
    private final JComboBox bandwidthCombobox = new JComboBox();

    /**
     * The "bitrate" field
     */
    private final JTextField bitrateField = new JTextField(6);

    /**
     * The "use dtx" checkbox
     */
    private final JCheckBox dtxCheckbox = new SIPCommCheckBox();

    /**
     * The "use fec" checkbox
     */
    private final JCheckBox fecCheckbox = new SIPCommCheckBox();

    /**
     * The "minimum expected packet loss" field
     */
    private final JTextField minExpectedPLField = new JTextField(3);

    /**
     * The "complexity" combobox
     */
    private final JComboBox complexityCombobox = new JComboBox();


    /**
     * The <tt>ConfigurationService</tt> to be used to access configuration
     */
    private final ConfigurationService configurationService
            = GeneralConfigPluginActivator.getConfigurationService();

    /**
     * The "restore defaults" button
     */
    private final JButton restoreButton = new JButton(Resources.getString(
            "plugin.generalconfig.RESTORE"));

    /**
     * Initialize a new <tt>OpusConfigForm</tt> instance.
     */
    public OpusConfigForm()
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
        TransparentPanel southPanel
                = new TransparentPanel(new GridLayout(0, 1, 2, 2));

        contentPanel.add(labelPanel, BorderLayout.WEST);
        contentPanel.add(valuePanel, BorderLayout.CENTER);
        contentPanel.add(southPanel, BorderLayout.SOUTH);

        //Audio bandwidth
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.OPUS_AUDIO_BANDWIDTH")));
        for(String str : BANDWIDTHS_LONG)
            bandwidthCombobox.addItem(str);
        bandwidthCombobox.setSelectedIndex(getBandwidthIndex(
                configurationService.getString(
                        Constants.PROP_OPUS_BANDWIDTH, BANDWIDTH_DEFAULT)));
        bandwidthCombobox.addActionListener(this);
        valuePanel.add(bandwidthCombobox);


        //Bitrate
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.OPUS_BITRATE")));
        bitrateField.setText(
                ((Integer)configurationService.getInt(
                        Constants.PROP_OPUS_BITRATE,
                        BITRATE_DEFAULT))
                .toString());
        bitrateField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent focusEvent) {}

            public void focusLost(FocusEvent focusEvent) {
                configurationService.setProperty(
                        Constants.PROP_OPUS_BITRATE, bitrateField.getText());
            }
        });
        valuePanel.add(bitrateField);

        //DTX
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.OPUS_USE_DTX")));
        dtxCheckbox.setSelected(
                configurationService.getBoolean(
                        Constants.PROP_OPUS_DTX, DTX_DEFAULT));
        dtxCheckbox.addActionListener(this);
        valuePanel.add(dtxCheckbox);

        //FEC
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.OPUS_USE_FEC")));
        fecCheckbox.setSelected(
                configurationService.getBoolean(
                        Constants.PROP_OPUS_FEC, FEC_DEFAULT));
        fecCheckbox.addActionListener(this);
        valuePanel.add(fecCheckbox);

        //min expected packet loss
        labelPanel.add(new JLabel(Resources.getString(
                       "plugin.generalconfig.OPUS_MIN_EXPECTED_PACKET_LOSS")));
        minExpectedPLField.setText(
                configurationService.getString(
                        Constants.PROP_OPUS_MIN_EXPECTED_PACKET_LOSS,
                        ((Integer)MIN_EXPECTED_PL_DEFAULT).toString()));
        minExpectedPLField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent focusEvent) {
            }

            public void focusLost(FocusEvent focusEvent) {
                configurationService.setProperty(
                        Constants.PROP_OPUS_MIN_EXPECTED_PACKET_LOSS,
                        minExpectedPLField.getText());
            }
        });
        valuePanel.add(minExpectedPLField);

        //Complexity
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.OPUS_COMPLEXITY")));
        /*
         * Unless the user explicitly assigns a complexity value, let the
         * opus_encoder_create function/method set the default complexity.
         */
        complexityCombobox.addItem("");
        for(int i = 10; i > 0; i--)
            complexityCombobox.addItem(Integer.toString(i));
        complexityCombobox.addActionListener(this);
        valuePanel.add(complexityCombobox);

        restoreButton.addActionListener(this);
        southPanel.add(restoreButton);
    }

    /**
     * Restores the UI components to the default values and writes the default
     * values to the configuration.
     */
    private void restoreDefaults()
    {
        bandwidthCombobox.setSelectedIndex(getBandwidthIndex(BANDWIDTH_DEFAULT));

        bitrateField.setText(((Integer)BITRATE_DEFAULT).toString());
        configurationService.setProperty(
                Constants.PROP_OPUS_BITRATE, BITRATE_DEFAULT);

        dtxCheckbox.setSelected(DTX_DEFAULT);
        configurationService.setProperty(
                Constants.PROP_OPUS_DTX, DTX_DEFAULT);

        fecCheckbox.setSelected(FEC_DEFAULT);
        configurationService.setProperty(
                Constants.PROP_OPUS_FEC, FEC_DEFAULT);

        minExpectedPLField.setText(
                ((Integer)MIN_EXPECTED_PL_DEFAULT).toString());
        configurationService.setProperty(
                Constants.PROP_OPUS_MIN_EXPECTED_PACKET_LOSS,
                MIN_EXPECTED_PL_DEFAULT);

        complexityCombobox.setSelectedIndex(COMPLEXITY_DEFAULT_INDEX);
        configurationService.removeProperty(Constants.PROP_OPUS_COMPLEXITY);
    }

    /**
     * Action listener for the checkboxes, buttons and comboboxes. Updates
     * the configuration service with the appropriate new value.
     * @param actionEvent
     */
    public void actionPerformed(ActionEvent actionEvent)
    {
        Object source = actionEvent.getSource();
        if(source == restoreButton)
        {
            restoreDefaults();
        }
        else if (source == bandwidthCombobox)
        {
            configurationService.setProperty(
                    Constants.PROP_OPUS_BANDWIDTH,
                    BANDWIDTHS[bandwidthCombobox.getSelectedIndex()]);
        }
        else if(source == dtxCheckbox)
        {
            configurationService.setProperty(
                    Constants.PROP_OPUS_DTX, dtxCheckbox.isSelected());
        }
        else if(source == fecCheckbox)
        {
            configurationService.setProperty(
                    Constants.PROP_OPUS_FEC, fecCheckbox.isSelected());
        }
        else if(source == complexityCombobox)
        {
            Object selectedItem = complexityCombobox.getSelectedItem();
            String complexity
                = (selectedItem == null) ? null : selectedItem.toString();

            if ((complexity == null) || (complexity.length() == 0))
            {
                configurationService.removeProperty(
                        Constants.PROP_OPUS_COMPLEXITY);
            }
            else
            {
                configurationService.setProperty(
                        Constants.PROP_OPUS_COMPLEXITY,
                        complexity);
            }
        }
    }

    /**
     * Returns the index of the audio bandwidth with a short name
     * <tt>bandwidthShortName</tt> in <tt>bandwidthCheckbox</tt>, or -1 if it's
     * not found.
     *
     * @param bandwidthShortName the short name of the audio bandwidth to return
     * to return the index of.
     *
     * @return the index of the audio bandwidth with a short name
     * <tt>bandwidthShortName</tt> in <tt>bandwidthCheckbox</tt>, or -1 if it's
     * not found.
     */
    private int getBandwidthIndex(String bandwidthShortName)
    {
        for(int i = 0; i < BANDWIDTHS.length; i++)
            if(BANDWIDTHS[i].equals(bandwidthShortName))
                return i;
        return -1;
    }
}
