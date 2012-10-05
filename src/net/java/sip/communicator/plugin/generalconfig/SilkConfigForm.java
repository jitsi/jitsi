/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;


import net.java.sip.communicator.util.swing.*;
import org.jitsi.service.configuration.ConfigurationService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;


/**
 * Implements the Silk configuration panel.
 *
 * @author Boris Grozev
 */
public class SilkConfigForm
        extends TransparentPanel
{
    /**
     * The property name associated with the 'use fec' setting
     */
    private static final String FEC_PROP
            = "net.java.sip.communicator.impl.neomedia.codec.audio.silk." +
              "encoder.USE_FEC";

    /**
     * The property name associated with the 'force packet loss' setting
     */
    private static final String FEC_FORCE_PL_PROP
            = "net.java.sip.communicator.impl.neomedia.codec.audio.silk." +
            "encoder.AWLAYS_ASSUME_PACKET_LOSS";

    /**
     * The property name associated with the 'speech activity threshold' setting
     */
    private static final String FEC_SAT_PROP
            = "net.java.sip.communicator.impl.neomedia.codec.audio.silk." +
            "encoder.SPEECH_ACTIVITY_THRESHOLD";

    /**
     * The property name associated with the 'advertise fec' setting
     */
    private static final String FEC_ADVERTISE_PROP
            = "net.java.sip.communicator.impl.neomedia.codec.audio.silk." +
            "ADVERTISE_FEC";

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
    private final JCheckBox fecCheckbox = new JCheckBox();

    /**
     * The "force packet loss" checkbox
     */
    private final JCheckBox fecForcePLCheckbox = new JCheckBox();

    /**
     * The " advertise FEC" checkbox
     */
    private final JCheckBox advertiseFECCheckbox = new JCheckBox();

    /**
     * The "speech activity threshold" field
     */
    private final JTextField SATField = new JTextField(6);

    /**
     * The <tt>ConfigurationService</tt> to be used to access configuration
     */
    ConfigurationService configurationService
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
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                configurationService.setProperty(FEC_PROP,
                        fecCheckbox.isSelected());
            }
        });
        fecCheckbox.setSelected(configurationService.getBoolean(
                FEC_PROP, FEC_DEFAULT));
        valuePanel.add(fecCheckbox);

        fecForcePLCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                configurationService.setProperty(FEC_FORCE_PL_PROP,
                        fecForcePLCheckbox.isSelected());
            }
        });
        fecForcePLCheckbox.setSelected(configurationService.getBoolean(
                FEC_FORCE_PL_PROP, FEC_FORCE_PL_DEFAULT));
        valuePanel.add(fecForcePLCheckbox);

        SATField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent){}

            @Override
            public void focusLost(FocusEvent focusEvent)
            {
                configurationService.setProperty(FEC_SAT_PROP,
                        SATField.getText());
            }
        });
        SATField.setText(configurationService.getString(
                FEC_SAT_PROP, FEC_SAT_DEFAULT));
        valuePanel.add(SATField);

        advertiseFECCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                configurationService.setProperty(FEC_ADVERTISE_PROP,
                        advertiseFECCheckbox.isSelected());
            }
        });
        advertiseFECCheckbox.setSelected(configurationService.getBoolean(
                FEC_ADVERTISE_PROP, FEC_ADVERTISE_DEFAULT));
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
        configurationService.setProperty(FEC_PROP, FEC_DEFAULT);

        fecForcePLCheckbox.setSelected(FEC_FORCE_PL_DEFAULT);
        configurationService.setProperty(
                FEC_FORCE_PL_PROP, FEC_FORCE_PL_DEFAULT);

        SATField.setText(FEC_SAT_DEFAULT);
        configurationService.setProperty(FEC_SAT_PROP, FEC_SAT_DEFAULT);

        advertiseFECCheckbox.setSelected(FEC_ADVERTISE_DEFAULT);
        configurationService.setProperty(
                FEC_ADVERTISE_PROP, FEC_ADVERTISE_DEFAULT);
    }
}