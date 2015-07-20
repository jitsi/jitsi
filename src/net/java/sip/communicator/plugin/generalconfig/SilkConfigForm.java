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
 * Implements the Silk configuration panel.
 *
 * @author Boris Grozev
 */
public class SilkConfigForm
        extends TransparentPanel
{
    /**
     * The default value for the SAT setting
     */
    private static final String FEC_SAT_DEFAULT = "0.5";

    /**
     * The default value for the FEC setting
     */
    private static final boolean FEC_DEFAULT = true;

    /**
     * The default value for the FEC force packet loss setting
     */
    private static final boolean FEC_FORCE_PL_DEFAULT = true;

    /**
     * The default value for the 'advertise FEC' setting
     */
    private static final boolean FEC_ADVERTISE_DEFAULT = false;

    /**
     * The "restore defaults" button
     */
    private final JButton restoreButton = new JButton(Resources.getString(
            "plugin.generalconfig.RESTORE"));

    /**
     * The "use fec" checkbox
     */
    private final JCheckBox fecCheckbox = new SIPCommCheckBox();

    /**
     * The "force packet loss" checkbox
     */
    private final JCheckBox assumePLCheckbox = new SIPCommCheckBox();

    /**
     * The " advertise FEC" checkbox
     */
    private final JCheckBox advertiseFECCheckbox = new SIPCommCheckBox();

    /**
     * The "speech activity threshold" field
     */
    private final JTextField SATField = new JTextField(6);

    /**
     * The <tt>ConfigurationService</tt> to be used to access configuration
     */
    private final ConfigurationService configurationService
            = GeneralConfigPluginActivator.getConfigurationService();


    /**
     * Initialize a new <tt>OpusConfigForm</tt> instance.
     */
    public SilkConfigForm()
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

        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.SILK_USE_FEC")));
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.SILK_ALWAYS_ASSUME_PACKET_LOSS")));
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.SILK_SAT")));
        labelPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.SILK_ADVERTISE_FEC")));


        fecCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                configurationService.setProperty(Constants.PROP_SILK_FEC,
                        fecCheckbox.isSelected());
            }
        });
        fecCheckbox.setSelected(configurationService.getBoolean(
                Constants.PROP_SILK_FEC, FEC_DEFAULT));
        valuePanel.add(fecCheckbox);

        assumePLCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                configurationService.setProperty(Constants.PROP_SILK_ASSUME_PL,
                        assumePLCheckbox.isSelected());
            }
        });
        assumePLCheckbox.setSelected(configurationService.getBoolean(
                Constants.PROP_SILK_ASSUME_PL, FEC_FORCE_PL_DEFAULT));
        valuePanel.add(assumePLCheckbox);

        SATField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent focusEvent){}

            public void focusLost(FocusEvent focusEvent)
            {
                configurationService.setProperty(Constants.PROP_SILK_FEC_SAT,
                        SATField.getText());
            }
        });
        SATField.setText(configurationService.getString(
                Constants.PROP_SILK_FEC_SAT, FEC_SAT_DEFAULT));
        valuePanel.add(SATField);

        advertiseFECCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                configurationService.setProperty(
                        Constants.PROP_SILK_ADVERSISE_FEC,
                        advertiseFECCheckbox.isSelected());
            }
        });
        advertiseFECCheckbox.setSelected(configurationService.getBoolean(
                Constants.PROP_SILK_ADVERSISE_FEC, FEC_ADVERTISE_DEFAULT));
        valuePanel.add(advertiseFECCheckbox);


        southPanel.add(restoreButton);
        restoreButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                restoreDefaults();
            }
        });
        southPanel.add(new JLabel(Resources.getString(
                        "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN")));
    }

    /**
     * Restores the UI components and the configuration to their default state
     */
    private void restoreDefaults()
    {
        fecCheckbox.setSelected(FEC_DEFAULT);
        configurationService.setProperty(Constants.PROP_SILK_FEC, FEC_DEFAULT);

        assumePLCheckbox.setSelected(FEC_FORCE_PL_DEFAULT);
        configurationService.setProperty(
                Constants.PROP_SILK_ASSUME_PL, FEC_FORCE_PL_DEFAULT);

        SATField.setText(FEC_SAT_DEFAULT);
        configurationService.setProperty(
                Constants.PROP_SILK_FEC_SAT, FEC_SAT_DEFAULT);

        advertiseFECCheckbox.setSelected(FEC_ADVERTISE_DEFAULT);
        configurationService.setProperty(
                Constants.PROP_SILK_ADVERSISE_FEC, FEC_ADVERTISE_DEFAULT);
    }
}
