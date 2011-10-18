/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window.
 * 
 * @author Damien Roth
 */
public class AutoAwayConfigurationPanel
    extends TransparentPanel
{
    /**
     * The default value to be displayed in {@link #timer} and to be considered
     * for {@link Preferences#TIMER}.
     */
    private static final int DEFAULT_TIMER = 15;

    private JCheckBox enable;
    private JSpinner timer;

    /**
     * Create an instance of <tt>StatusConfigForm</tt>
     */
    public AutoAwayConfigurationPanel()
    {
        super(new BorderLayout());

        add(GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString("service.gui.STATUS")),
            BorderLayout.WEST);
        add(createMainPanel());

        initValues();
    }

    /**
     * Init the main panel.
     * @return the created component
     */
    private Component createMainPanel()
    {
        JPanel mainPanel = new TransparentPanel(new BorderLayout(5, 5));

        enable = new SIPCommCheckBox(GeneralConfigPluginActivator.getResources()
                .getI18NString("plugin.autoaway.ENABLE_CHANGE_STATUS"));

        mainPanel.add(enable, BorderLayout.NORTH);

        enable.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                timer.setEnabled(enable.isSelected());
                saveData();
            }
        });

        JPanel timerPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        // Text
        timerPanel.add(new JLabel(
                GeneralConfigPluginActivator.getResources()
                    .getI18NString("plugin.autoaway.AWAY_MINUTES")));
        // Spinner
        timer = new JSpinner(new SpinnerNumberModel(DEFAULT_TIMER, 1, 180, 1));
        timerPanel.add(timer);
        timer.addChangeListener(new ChangeListener()
        {

            public void stateChanged(ChangeEvent e)
            {
                saveData();
            }
        });

        mainPanel.add(timerPanel, BorderLayout.WEST);

        return mainPanel;
    }

    /**
     * Init the values of the widgets
     */
    private void initValues()
    {
        ConfigurationService configService 
            = GeneralConfigPluginActivator.getConfigurationService();

        boolean e = configService.getBoolean(Preferences.ENABLE, false);
        this.enable.setSelected(e);
        this.timer.setEnabled(e);

        int t = configService.getInt(Preferences.TIMER, DEFAULT_TIMER);
        this.timer.setValue(t);
    }

    /**
     * Save data in the configuration file
     */
    private void saveData()
    {
        ConfigurationService configService 
            = GeneralConfigPluginActivator.getConfigurationService();

        configService.setProperty(Preferences.ENABLE, 
                                  Boolean.toString(enable.isSelected()));
        configService.setProperty(Preferences.TIMER,
                                  timer.getValue().toString());
    }
}
